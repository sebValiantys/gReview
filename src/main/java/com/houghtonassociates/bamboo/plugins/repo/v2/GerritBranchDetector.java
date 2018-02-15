package com.houghtonassociates.bamboo.plugins.repo.v2;

import com.atlassian.bamboo.build.fileserver.BuildDirectoryManager;
import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.build.logger.NullBuildLogger;
import com.atlassian.bamboo.commit.CommitContext;
import com.atlassian.bamboo.credentials.CredentialsAccessor;
import com.atlassian.bamboo.plan.branch.VcsBranch;
import com.atlassian.bamboo.repository.RepositoryException;
import com.atlassian.bamboo.security.TrustedKeyHelper;
import com.atlassian.bamboo.ssh.SshProxyService;
import com.atlassian.bamboo.utils.i18n.I18nBeanFactory;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.bamboo.vcs.configuration.VcsRepositoryData;
import com.atlassian.bamboo.vcs.runtime.ContextualVcsId;
import com.atlassian.bamboo.vcs.runtime.VcsBranchDetector;
import com.houghtonassociates.bamboo.plugins.GerritHelper;
import com.houghtonassociates.bamboo.plugins.dao.GerritChangeVO;
import com.houghtonassociates.bamboo.plugins.dao.GerritConfig;
import com.houghtonassociates.bamboo.plugins.dao.GerritService;
import com.opensymphony.xwork2.TextProvider;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author www.valiantys.com
 * Date : 19/02/2018
 */
public class GerritBranchDetector extends AbstractGerritExecutor implements VcsBranchDetector {

    private static final Logger log = Logger.getLogger(GerritBranchDetector.class);

    public GerritBranchDetector(SshProxyService sshProxyService,
                                CapabilityContext capabilityContext,
                                CredentialsAccessor credentialsAccessor,
                                CustomVariableContext customVariableContext,
                                BuildDirectoryManager buildDirectoryManager,
                                TrustedKeyHelper trustedKeyHelper,
                                I18nBeanFactory i18nBeanFactory) {
        super(sshProxyService, capabilityContext, credentialsAccessor, customVariableContext, buildDirectoryManager, trustedKeyHelper, i18nBeanFactory);
    }

    @NotNull
    @Override
    public List<VcsBranch> getOpenBranches(@NotNull final VcsRepositoryData repositoryData) throws RepositoryException {
        final GerritConfig substitutedAccessData = getSubstitutedAccessData(repositoryData);
        try {
            this.prepareSSHKeyFile(substitutedAccessData);
            GerritService gerrit = new GerritService(substitutedAccessData);
            return gerrit.getOpenBranches(substitutedAccessData, getWorkingDirectory());
         } catch (Exception e) {
            log.error("Unable to get branches", e);
            throw new RepositoryException(e, repositoryData.getId());
        } finally {
            this.cleanUpSSHFile();
        }
    }

    @NotNull
    @Override
    /**
     * Copied and adapted from GitBranchDetector
     */
    public CommitContext getFirstCommitApproximation(@NotNull final VcsRepositoryData repositoryData) throws RepositoryException {
        final GerritConfig substitutedAccessData = getSubstitutedAccessData(repositoryData);
        BuildLogger buildLogger = new NullBuildLogger();
        final File directory = this.getWorkingDirectory();
        try {
            this.prepareSSHKeyFile(substitutedAccessData);
            GerritService gerrit = new GerritService(substitutedAccessData);
            String shaOfRefIfExists = gerrit.getRevision(directory, substitutedAccessData.getBranch().getName());
            if (shaOfRefIfExists == null) {
                return getLastCommit(repositoryData);
            }
            return gerrit.getCommit(directory, shaOfRefIfExists);
        } catch (RepositoryException e) {
            log.warn(buildLogger.addBuildLogEntry(this.textProvider.getText("repository.git.messages.cannotDetermineRevision", directory.getName()) + " " + e.getMessage()), e);
            throw e;
        } finally {
            this.cleanUpSSHFile();
        }
    }

    @NotNull
    @Override
    public CommitContext getLastCommit(@NotNull final VcsRepositoryData repositoryData) throws RepositoryException {
        final BuildLogger buildLogger = new NullBuildLogger();
        final GerritConfig substitutedAccessData = getSubstitutedAccessData(repositoryData);
        try {
            GerritChangeVO change = null;
            this.prepareSSHKeyFile(substitutedAccessData);
            GerritService gerrit = new GerritService(substitutedAccessData);
            GerritHelper helper = new GerritHelper(buildDirectoryManager, repositoryData, substitutedAccessData, textProvider);
            if (substitutedAccessData.getBranch().equals(GerritConstants.ALL_BRANCH)) {
                change = gerrit.getLastUnverifiedChange(substitutedAccessData.getProject());
                if (change == null) {
                    change = gerrit.getLastChange(substitutedAccessData.getProject());
                }
            } else {
                change = gerrit.getLastUnverifiedChange(substitutedAccessData.getProject(),substitutedAccessData.getBranch().getName());
                if (change == null) {
                    change = gerrit.getLastChange(substitutedAccessData.getProject(),substitutedAccessData.getBranch().getName());
                }
            }

            if (change == null) {
                buildLogger.addBuildLogEntry(this.textProvider.getText("processor.gerrit.messages.build.error.nochanges"));
            }
            buildLogger.addBuildLogEntry(this.textProvider.getText("repository.gerrit.messages.ccRecover.completed"));
            CommitContext commit = helper.convertChangeToCommit(change, true);
            return commit;
        } catch (IOException e) {
            log.error("Unable to get last commit for " + substitutedAccessData.getRepositoryUrl(), e);
            throw new RepositoryException(this.textProvider.getText("repository.gerrit.messages.error.retrieve.commit"), repositoryData.getId());
        } finally {
            this.cleanUpSSHFile();
        }
    }

    @Nullable
    @Override
    public ContextualVcsId<VcsBranchDetector> getVcsIdForExecutor(@NotNull final VcsRepositoryData vcsRepositoryData) {
        final GerritConfig substitutedAccessData = getSubstitutedAccessData(vcsRepositoryData);
        return new ContextualVcsId<>(this,
                vcsRepositoryData,
                substitutedAccessData.getRepositoryUrl(),
                substitutedAccessData.getUsername(),
                substitutedAccessData.getSshKey());
    }
}
