package com.houghtonassociates.bamboo.plugins.repo.v2;

import com.atlassian.bamboo.agent.AgentType;
import com.atlassian.bamboo.agent.AgentTypeHolder;
import com.atlassian.bamboo.build.BuildLoggerManager;
import com.atlassian.bamboo.build.fileserver.BuildDirectoryManager;
import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.build.logger.NullBuildLogger;
import com.atlassian.bamboo.credentials.CredentialsAccessor;
import com.atlassian.bamboo.executor.CancelException;
import com.atlassian.bamboo.plan.branch.VcsBranch;
import com.atlassian.bamboo.plan.branch.VcsBranchImpl;
import com.atlassian.bamboo.plan.branch.VcsBranchIntegrationHelper;
import com.atlassian.bamboo.plan.vcsRevision.PlanVcsRevisionData;
import com.atlassian.bamboo.plugins.git.GitCapabilityTypeModule;
import com.atlassian.bamboo.repository.RepositoryException;
import com.atlassian.bamboo.security.TrustedKeyHelper;
import com.atlassian.bamboo.ssh.SshProxyService;
import com.atlassian.bamboo.utils.SystemProperty;
import com.atlassian.bamboo.utils.i18n.I18nBean;
import com.atlassian.bamboo.utils.i18n.I18nBeanFactory;
import com.atlassian.bamboo.v2.build.CommonContext;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext;
import com.atlassian.bamboo.v2.build.agent.capability.Requirement;
import com.atlassian.bamboo.v2.build.agent.capability.RequirementImpl;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.bamboo.vcs.configuration.VcsRepositoryData;
import com.atlassian.bamboo.vcs.runtime.MergingVcsWorkingCopyManager;
import com.atlassian.bamboo.vcs.runtime.NoContextVcsWorkingCopyManager;
import com.atlassian.bamboo.vcs.runtime.UpdatingVcsWorkingCopyManager;
import com.atlassian.bamboo.vcs.runtime.VcsWorkingCopy;
import com.google.common.collect.Sets;
import com.houghtonassociates.bamboo.plugins.GerritHelper;
import com.houghtonassociates.bamboo.plugins.dao.GerritChangeVO;
import com.houghtonassociates.bamboo.plugins.dao.GerritConfig;
import com.houghtonassociates.bamboo.plugins.dao.GerritService;
import com.houghtonassociates.bamboo.plugins.dao.jgit.JGitRepository;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.Status;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Set;

public class GerritRepositoryManager extends AbstractGerritExecutor
        implements MergingVcsWorkingCopyManager, UpdatingVcsWorkingCopyManager,
        NoContextVcsWorkingCopyManager/*, GerritProcessListener*/
{
    private static final Logger log = Logger.getLogger(GerritRepositoryManager.class);

    private VcsBranchIntegrationHelper branchIntegrationHelper;
    protected transient I18nBean textProvider;
    private BuildLoggerManager buildLoggerManager;

    private String project = "";
    private VcsBranch vcsBranch = GerritConstants.MASTER_BRANCH;

    protected static boolean USE_SHALLOW_CLONES = new SystemProperty(false, "atlassian.bamboo.git.useShallowClones", "ATLASSIAN_BAMBOO_GIT_USE_SHALLOW_CLONES").getValue(true);

    public GerritRepositoryManager(SshProxyService sshProxyService,
                                   CapabilityContext capabilityContext,
                                   CredentialsAccessor credentialsAccessor,
                                   CustomVariableContext customVariableContext,
                                   BuildDirectoryManager buildDirectoryManager,
                                   TrustedKeyHelper trustedKeyHelper,
                                   I18nBeanFactory i18nBeanFactory,
                                   BuildLoggerManager buildLoggerManager) {
        super(sshProxyService, capabilityContext, credentialsAccessor, customVariableContext, buildDirectoryManager, trustedKeyHelper, i18nBeanFactory);
        this.buildLoggerManager = buildLoggerManager;
        this.textProvider = i18nBeanFactory.getI18nBean();
    }

    @NotNull
    @Override
    public Set<Requirement> getMergingRequirements() {
        return Sets.<Requirement>newHashSet(new RequirementImpl(GitCapabilityTypeModule.GIT_CAPABILITY, true, ".*", true));
    }

    /*
        Replace GerritOperationHelper with GerritService
     */
    @NotNull
    @Override
    public VcsWorkingCopy checkoutAndMerge(@NotNull CommonContext commonContext,
                                           @NotNull final VcsRepositoryData vcsRepositoryData,
                                           @NotNull final VcsBranch targetBranch,
                                           @NotNull final PlanVcsRevisionData revisionOnTarget,
                                           @NotNull final VcsBranch sourceBranch,
                                           @NotNull final PlanVcsRevisionData sourceRevision,
                                           @NotNull final File targetPath) throws RepositoryException {
        this.updateBranch(vcsRepositoryData);
        //todo garmar: PlanVcsRevision contain branch. Use it! (it's not so simple because we don't store it)
        PlanVcsRevisionData tmp = new PlanVcsRevisionData(revisionOnTarget.getVcsRevisionKey(), revisionOnTarget.getCustomXmlData(), targetBranch);
        VcsWorkingCopy wcAfterCheckout = checkout(commonContext, vcsRepositoryData, tmp, targetPath, true);

        final BuildLogger buildLogger = buildLoggerManager.getLogger(commonContext.getResultKey());
        final GerritConfig substitutedAccessData = getSubstitutedAccessDataBuilder(vcsRepositoryData)
                        .branch(new VcsBranchImpl(substituteString(sourceBranch.getName())))
                        .useShallowClones(false).build();

        GerritService gerrit = null;
        GerritHelper gerritHelper = null;
        try {
            gerritHelper = new GerritHelper(buildDirectoryManager, vcsRepositoryData, substitutedAccessData, textProvider);
            this.prepareSSHKeyFile(substitutedAccessData);
            gerrit = new GerritService(substitutedAccessData);
            //gerrit.fetch(wcAfterCheckout.getPath(), sourceRevision.getVcsRevisionKey());
            String headShaBeforeMerge = gerrit.getHead(substitutedAccessData, wcAfterCheckout.getPath());
            MergeResult mergeResult = gerrit.mergeWorkspaceWith(wcAfterCheckout.getPath(), sourceRevision.getVcsRevisionKey());
            Status workspaceStatus = gerrit.status(wcAfterCheckout.getPath());
            String headShaAfterMerge = mergeResult.getNewHead().getName();
            if (log.isDebugEnabled()) {
                log.debug("Checkout and Merge. Head before merge: " + headShaBeforeMerge + ", after merge: " + headShaAfterMerge);
                log.debug("Status of merge : " + mergeResult.getMergeStatus().toString());
            }
            if (workspaceStatus.isClean()) {
                buildLogger.addBuildLogEntry(textProvider.getText("repository.gerrit.messages.merge.error", sourceBranch.getName(), targetBranch.getName()));
                return new VcsWorkingCopy(vcsRepositoryData.getId(), targetPath, headShaAfterMerge, false);
            } else {
                return new VcsWorkingCopy(vcsRepositoryData.getId(), targetPath, headShaBeforeMerge, true);
            }
        } catch (Exception e) {
            throw new RepositoryException(e, vcsRepositoryData.getId());
        } finally {
            this.cleanUpSSHFile();
        }
    }

    private void updateBranch(VcsRepositoryData repositoryData) {
        this.vcsBranch = repositoryData.getBranch().getVcsBranch();
    }

    @NotNull
    @Override
    public VcsWorkingCopy updateToLatestRevision(@NotNull VcsRepositoryData vcsRepositoryData, @NotNull File file) throws RepositoryException {
        return checkout(null, vcsRepositoryData, null, file, false);
    }

    @NotNull
    @Override
    public VcsWorkingCopy commitLocal(@NotNull VcsWorkingCopy vcsWorkingCopy, @NotNull VcsRepositoryData vcsRepositoryData, @NotNull String s) throws RepositoryException {
        this.updateBranch(vcsRepositoryData);
        final GerritConfig substitutedAccessData = getSubstitutedAccessData(vcsRepositoryData);
        GerritService gerrit = null;
        try {
            this.prepareSSHKeyFile(substitutedAccessData);
            gerrit = new GerritService(substitutedAccessData);
            String revision = gerrit.commit(vcsWorkingCopy.getPath(), s, branchIntegrationHelper.getCommitterName(), branchIntegrationHelper.getCommitterEmail());
            return new VcsWorkingCopy(vcsWorkingCopy.getRepositoryId(), vcsWorkingCopy.getPath(), revision);
        } catch (Exception e) {
            throw new RepositoryException(e.getMessage(), vcsRepositoryData.getId());
        } finally {
            this.cleanUpSSHFile();
        }
    }

    @NotNull
    @Override
    public VcsWorkingCopy updateRemote(@NotNull VcsWorkingCopy workingCopy, @NotNull VcsRepositoryData vcsRepositoryData, @NotNull String s) throws RepositoryException {
        this.updateBranch(vcsRepositoryData);
        final GerritConfig substitutedAccessData = getSubstitutedAccessData(vcsRepositoryData);
        GerritService gerrit = null;
        try {
            this.prepareSSHKeyFile(substitutedAccessData);
            gerrit = new GerritService(substitutedAccessData);
            gerrit.pushRevision(workingCopy.getPath(), workingCopy.getCurrentRevisionKey());
            return workingCopy;
        } catch (Exception e) {
            throw new RepositoryException(e.getMessage(), vcsRepositoryData.getId());
        } finally {
            this.cleanUpSSHFile();
        }
    }

    @NotNull
    @Override
    public VcsWorkingCopy retrieveSourceCode(@NotNull CommonContext commonContext,
                                             @NotNull VcsRepositoryData vcsRepositoryData,
                                             @NotNull PlanVcsRevisionData planVcsRevisionData,
                                             @NotNull File file) throws RepositoryException {
        return checkout(commonContext, vcsRepositoryData, planVcsRevisionData, file, false);
    }

    @NotNull
    @Override
    public VcsWorkingCopy updateToLatestRevision(@NotNull CommonContext commonContext,
                                                 @NotNull VcsRepositoryData vcsRepositoryData,
                                                 @NotNull File file) throws RepositoryException {
        return checkout(commonContext, vcsRepositoryData, null, file, false);
    }

    /*
     * See how to handle local vs remote agent execution
     */
    @NotNull
    private VcsWorkingCopy checkout(@Nullable final CommonContext commonContext,
                                    @NotNull final VcsRepositoryData repositoryData,
                                    @Nullable final PlanVcsRevisionData planVcsRevisionData,
                                    @NotNull final File sourceDirectory,
                                    boolean needsDeepClone) throws RepositoryException {
        this.updateBranch(repositoryData);
        String vcsRevisionKey = null;
        VcsBranch effectiveBranch = null;
        if (planVcsRevisionData != null) {
            vcsRevisionKey = planVcsRevisionData.getVcsRevisionKey();
            effectiveBranch = planVcsRevisionData.getActualBranch();
        }
        final GerritConfig accessData = getAccessData(repositoryData);
        final GerritConfig.Builder substitutedAccessDataBuilder = getSubstitutedAccessDataBuilder(repositoryData);
        final boolean doShallowFetch = USE_SHALLOW_CLONES && accessData.isUseShallowClones() && !isOnLocalAgent() && effectiveBranch == null && !needsDeepClone;
        if (effectiveBranch != null) {
            substitutedAccessDataBuilder.branch(effectiveBranch);
        }
        substitutedAccessDataBuilder.useShallowClones(doShallowFetch);
        final GerritConfig substitutedAccessData = substitutedAccessDataBuilder.build();
        try {
            this.prepareSSHKeyFile(substitutedAccessData);
            final BuildLogger buildLogger = getBuildLogger(commonContext);
            String revisionKey = this.retrieveSourceCode(buildLogger, vcsRevisionKey, sourceDirectory, substitutedAccessData, repositoryData);
            return new VcsWorkingCopy(repositoryData.getId(), sourceDirectory, revisionKey);
        } catch (RepositoryException | CancelException e) {
            throw e;
        } finally {
            this.cleanUpSSHFile();
        }
    }

    @NotNull
    private String retrieveSourceCode(@NotNull BuildLogger buildLogger,
                                     String vcsRevisionKey,
                                     File sourceDirectory,
                                     GerritConfig gerritConfig,
                                     VcsRepositoryData repositoryData) throws RepositoryException {
        String originalVcsRevisionKey = vcsRevisionKey;

//        lastGerritChange = null;

        GerritService gerrit = new GerritService(gerritConfig);
        GerritChangeVO change = gerrit.getChangeByRevision(vcsRevisionKey);

        if (change != null) {
            buildLogger.addBuildLogEntry(String.format(
                    "Retrieving Source for Change: %s, %s, %s", change.getBranch(),
                    change.getId(), change.getCurrentPatchSet().getRef()));

//            lastGerritChange = change;

            vcsRevisionKey = change.getCurrentPatchSet().getRef();
        } else {
            throw new RepositoryException(textProvider.getText("repository.gerrit.messages.error.retrieve"), repositoryData.getId());
        }

        log.debug(String.format("getVcsBranch()=%s", gerritConfig.getBranch().getName()));

        JGitRepository jgitRepo = new JGitRepository();
        jgitRepo.setAccessData(gerritConfig);
        jgitRepo.open(sourceDirectory);
        jgitRepo.openSSHTransport();

        if (gerritConfig.isUseShallowClones()) {
            jgitRepo.fetch(vcsRevisionKey, 1);
        } else {
            jgitRepo.fetch(vcsRevisionKey);
        }

        jgitRepo.checkout(vcsRevisionKey);
        jgitRepo.close();

        return originalVcsRevisionKey;
    }

    public String getName() {
        return textProvider.getText("repository.gerrit.name");
    }

    public VcsBranch getVcsBranch() {
        return vcsBranch;
    }

    public String getProject() {
        return this.project;
    }

    private boolean isOnLocalAgent() {
        return AgentTypeHolder.get() == AgentType.LOCAL;
    }

    public void setBranchIntegrationHelper(VcsBranchIntegrationHelper branchIntegrationHelper) {
        this.branchIntegrationHelper = branchIntegrationHelper;
    }


    public void setBuildLoggerManager(final BuildLoggerManager buildLoggerManager) {
        this.buildLoggerManager = buildLoggerManager;
    }

    @NotNull
    private BuildLogger getBuildLogger(@Nullable final CommonContext commonContext) {
        return commonContext != null ? buildLoggerManager.getLogger(commonContext.getResultKey()) : new NullBuildLogger();
    }
}
