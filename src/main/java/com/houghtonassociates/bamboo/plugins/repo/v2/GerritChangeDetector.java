package com.houghtonassociates.bamboo.plugins.repo.v2;

import com.atlassian.bamboo.author.Author;
import com.atlassian.bamboo.bandana.PlanAwareBandanaContext;
import com.atlassian.bamboo.build.BuildLoggerManager;
import com.atlassian.bamboo.build.fileserver.BuildDirectoryManager;
import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.commit.Commit;
import com.atlassian.bamboo.commit.CommitContext;
import com.atlassian.bamboo.commit.CommitContextImpl;
import com.atlassian.bamboo.credentials.CredentialsAccessor;
import com.atlassian.bamboo.plan.PlanKey;
import com.atlassian.bamboo.plan.PlanKeys;
import com.atlassian.bamboo.plan.vcsRevision.PlanVcsRevisionData;
import com.atlassian.bamboo.repository.RepositoryException;
import com.atlassian.bamboo.security.TrustedKeyHelper;
import com.atlassian.bamboo.ssh.SshProxyService;
import com.atlassian.bamboo.util.Narrow;
import com.atlassian.bamboo.utils.i18n.I18nBeanFactory;
import com.atlassian.bamboo.v2.build.BuildRepositoryChanges;
import com.atlassian.bamboo.v2.build.BuildRepositoryChangesImpl;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.bamboo.vcs.configuration.VcsRepositoryData;
import com.atlassian.bamboo.vcs.runtime.CommitsIsolatingVcsChangeDetector;
import com.atlassian.bamboo.vcs.runtime.ContextualVcsId;
import com.atlassian.bamboo.vcs.runtime.VcsChangeDetector;
import com.atlassian.bandana.BandanaManager;
import com.google.common.collect.Lists;
import com.houghtonassociates.bamboo.plugins.GerritHelper;
import com.houghtonassociates.bamboo.plugins.dao.GerritChangeVO;
import com.houghtonassociates.bamboo.plugins.dao.GerritConfig;
import com.houghtonassociates.bamboo.plugins.dao.GerritService;
import com.opensymphony.xwork2.TextProvider;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class GerritChangeDetector extends AbstractGerritExecutor implements CommitsIsolatingVcsChangeDetector {
    private static final Logger log = Logger.getLogger(GerritChangeDetector.class);

    private BuildLoggerManager buildLoggerManager;
    private BandanaManager bandanaManager;

    public GerritChangeDetector(SshProxyService sshProxyService,
                                CapabilityContext capabilityContext,
                                CredentialsAccessor credentialsAccessor,
                                CustomVariableContext customVariableContext,
                                BuildDirectoryManager buildDirectoryManager,
                                TextProvider textProvider,
                                TrustedKeyHelper trustedKeyHelper,
                                BuildLoggerManager buildLoggerManager,
                                BandanaManager bandanaManager,
                                I18nBeanFactory i18nBeanFactory) {
        super(sshProxyService, capabilityContext, credentialsAccessor, customVariableContext, buildDirectoryManager, trustedKeyHelper, i18nBeanFactory);
        this.buildLoggerManager = buildLoggerManager;
        this.bandanaManager = bandanaManager;
    }

    @NotNull
    @Override
    public BuildRepositoryChanges collectChangesSinceRevision(@NotNull final PlanKey planKey,
                                                              @NotNull final VcsRepositoryData repositoryData,
                                                              @NotNull final PlanVcsRevisionData lastRevisionData) throws RepositoryException {
        return collectChanges(planKey, repositoryData, lastRevisionData.getVcsRevisionKey(), null);
    }

    @NotNull
    @Override
    public BuildRepositoryChanges collectChangesForRevision(@NotNull final PlanKey planKey,
                                                            @NotNull final VcsRepositoryData repositoryData,
                                                            @NotNull final String customRevision) throws RepositoryException {
        return collectChanges(planKey, repositoryData, customRevision, customRevision);
    }

    @NotNull
    private BuildRepositoryChanges collectChanges(@NotNull PlanKey planKey,
                                                   @NotNull VcsRepositoryData vcsRepositoryData,
                                                   @Nullable final String lastVcsRevisionKey,
                                                   @Nullable final String customRevision) throws RepositoryException {
        // TODO : what to do with customRevision ?
        final GerritConfig substitutedAccessData = getSubstitutedAccessData(vcsRepositoryData);
        PlanKey actualKey = PlanKeys.getPlanKey(planKey.getKey());
        final BuildLogger buildLogger = buildLoggerManager.getLogger(actualKey);
        List<Commit> commits = new ArrayList<Commit>();
        GerritChangeVO change = null;
        GerritService gerrit = null;
        GerritHelper helper = null;
        try {
            this.prepareSSHKeyFile(substitutedAccessData);
            helper = new GerritHelper(buildDirectoryManager, vcsRepositoryData, substitutedAccessData, textProvider);
            gerrit = new GerritService(substitutedAccessData);

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
                BuildRepositoryChanges buildChanges = helper.getBuildChangesFromJGit(actualKey, lastVcsRevisionKey);
                return buildChanges;
            } else {
                log.debug(String.format("collectChangesSinceLastBuild: %s, %s, %s",
                        change.getBranch(), change.getId(), change.getCurrentPatchSet().getRef()));

                buildLogger.addBuildLogEntry(this.textProvider.getText("repository.gerrit.messages.ccRecover.completed"));

                if (lastVcsRevisionKey == null) {
                    buildLogger.addBuildLogEntry(this.textProvider.getText("repository.gerrit.messages.ccRepositoryNeverChecked",change.getLastRevision()));
                } else if (change.getLastRevision().equals(lastVcsRevisionKey)) {
                    // Time is unreliable as comments change the last update field.
                    // We need to track by last patchset revision
                    Object lastRevForChange = bandanaManager.getValue(PlanAwareBandanaContext.GLOBAL_CONTEXT, change.getId());
                    if ((lastRevForChange != null) && lastRevForChange.equals(change.getLastRevision())) {
                        return new BuildRepositoryChangesImpl(vcsRepositoryData.getId(), change.getLastRevision(),
                                null, substitutedAccessData.getBranch());
                    }
                }
            }

            commits.add(helper.convertChangeToCommit(change, true));

        } catch (IOException e) {
            log.error("Unable to collect changes for " + planKey, e);
            throw new RepositoryException(this.textProvider.getText("repository.gerrit.messages.error.retrieve"), vcsRepositoryData.getId());
        } finally {
            this.cleanUpSSHFile();
        }

        BuildRepositoryChanges buildChanges = new BuildRepositoryChangesImpl(vcsRepositoryData.getId(),
                change.getLastRevision(), commits, substitutedAccessData.getBranch());

        // TODO : check if values are correctly passed
        if (!substitutedAccessData.getBranch().isEqualToBranchWith(change.getBranch())) {
            buildChanges.setActualBranch(substitutedAccessData.getBranch());
        }

        bandanaManager.setValue(PlanAwareBandanaContext.GLOBAL_CONTEXT, change.getId(), change.getLastRevision());

        return buildChanges;

    }

    @NotNull
    @Override
    public List<BuildRepositoryChanges> isolateCommits(@NotNull final BuildRepositoryChanges changes, @NotNull final VcsRepositoryData vcsRepositoryData) {
        final List<CommitContext> commits = changes.getChanges();
        if (commits.size() > 1) {
            List<BuildRepositoryChanges> isolatedChanges = new ArrayList<>();
            for (CommitContext commit : commits) {
                CommitContextImpl commitContextWithBranch = Narrow.downTo(commit, CommitContextImpl.class);
                String commitBranch = commitContextWithBranch != null ? commitContextWithBranch.getBranch() : null;
                if (commitBranch == null || commitBranch.equals(vcsRepositoryData.getBranch().getVcsBranch().getName())) {
                    BuildRepositoryChangesImpl change = new BuildRepositoryChangesImpl(changes.getRepositoryId(), commit.getChangeSetId(), Collections.singletonList(commit));
                    isolatedChanges.add(change);
                }
            }

            // Reverse so that's the oldest is at the top
            return Lists.reverse(isolatedChanges);
        } else {
            return Lists.newArrayList(changes);
        }
    }

    public static CommitContext createUnknownChangesEntry(final TextProvider textProvider, @Nullable final String startRevision, final String endRevision) {
        return CommitContextImpl.builder()
                .changesetId(endRevision)
                .author(Author.UNKNOWN_AUTHOR)
                .comment(textProvider.getText("repository.git.messages.unknownChanges", startRevision, endRevision))
                .date(new Date())
                .build();
    }

    @Nullable
    @Override
    public ContextualVcsId getVcsIdForExecutor(@NotNull final VcsRepositoryData vcsRepositoryData) {
        GerritConfig accessData = getSubstitutedAccessData(vcsRepositoryData);
        return new ContextualVcsId<VcsChangeDetector>(this,
                vcsRepositoryData,
                accessData.getRepositoryUrl(),
                accessData.getBranch().getName(),
                accessData.getUsername(),
                accessData.getSshKey());
    }

    public void setBuildLoggerManager(BuildLoggerManager blm) {
        this.buildLoggerManager = blm;
    }
}
