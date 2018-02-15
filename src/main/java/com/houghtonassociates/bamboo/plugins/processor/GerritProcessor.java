/**
 * Copyright 2012 Houghton Associates
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.houghtonassociates.bamboo.plugins.processor;

import com.atlassian.bamboo.build.CustomBuildProcessor;
import com.atlassian.bamboo.build.CustomBuildProcessorServer;
import com.atlassian.bamboo.build.fileserver.BuildDirectoryManager;
import com.atlassian.bamboo.builder.BuildState;
import com.atlassian.bamboo.configuration.AdministrationConfiguration;
import com.atlassian.bamboo.configuration.AdministrationConfigurationAccessor;
import com.atlassian.bamboo.plan.Plan;
import com.atlassian.bamboo.repository.RepositoryException;
import com.atlassian.bamboo.template.TemplateRenderer;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.bamboo.utils.i18n.I18nBeanFactory;
import com.atlassian.bamboo.utils.i18n.TextProviderAdapter;
import com.atlassian.bamboo.v2.build.BaseConfigurableBuildPlugin;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.v2.build.CurrentBuildResult;
import com.atlassian.bamboo.v2.build.task.BuildTask;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.bamboo.vcs.configuration.PlanRepositoryDefinition;
import com.atlassian.bamboo.vcs.configuration.VcsRepositoryData;
import com.atlassian.bamboo.ww2.actions.build.admin.create.BuildConfiguration;
import com.atlassian.spring.container.LazyComponentReference;
import com.houghtonassociates.bamboo.plugins.dao.GerritChangeVO;
import com.houghtonassociates.bamboo.plugins.dao.GerritConfig;
import com.houghtonassociates.bamboo.plugins.dao.GerritService;
import com.houghtonassociates.bamboo.plugins.repo.v2.GerritConstants;
import com.opensymphony.xwork2.TextProvider;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.houghtonassociates.bamboo.plugins.repo.v2.GerritConstants.*;

/**
 * Post processor which updates Gerrit after build completes
 */
public class GerritProcessor extends BaseConfigurableBuildPlugin implements
        CustomBuildProcessorServer, BuildTask {

    private final Logger logger = Logger.getLogger(GerritProcessor.class);

    // dependencies
    private TextProvider textProvider = null;
    private BuildDirectoryManager buildDirectoryManager = null;
    private AdministrationConfigurationAccessor administrationConfigurationAccess;
    private TemplateRenderer templateRenderer;
    private CustomVariableContext customVariableContext;
    private File sshKeyFile;

    public GerritProcessor(TextProvider textProvider,
                           BuildDirectoryManager buildDirectoryManager,
                           AdministrationConfigurationAccessor administrationConfigurationAccess,
                           TemplateRenderer templateRenderer,
                           CustomVariableContext customVariableContext)
    {
        // perform a quick test
        // not sure why this is the case, but textprovider fails on remote agent
        // There's some tickets open against it and open end questions:
        // https://answers.atlassian.com/questions/20566/textprovider-in-sdk-bamboo-helloworld-task-example-does-not-work
        String test = textProvider.getText("repository.gerrit.name");
        if (test != null) {
            logger.debug("setTextProvider: On local agent, keeping textProvider..");
            this.textProvider = textProvider;
        } else {
            logger.debug("setTextProvider: On remote agent, switching textProvider..");
            LazyComponentReference<I18nBeanFactory> i18nBeanFactoryReference =
                    new LazyComponentReference<I18nBeanFactory>("i18nBeanFactory");
            I18nBeanFactory i18nBeanFactory = i18nBeanFactoryReference.get();
            this.textProvider =
                    new TextProviderAdapter(i18nBeanFactory.getI18nBean(Locale.getDefault()));
        }
        this.buildDirectoryManager = buildDirectoryManager;
        this.administrationConfigurationAccess = administrationConfigurationAccess;
        this.setTemplateRenderer(templateRenderer);
        this.customVariableContext = customVariableContext;
    }

    private Map<String, String> customConfiguration = null;
    private static final String GERRIT_RUN = "custom.gerrit.run";

    @Override
    public void init(BuildContext buildContext) {
        super.init(buildContext);
        this.customConfiguration = buildContext.getBuildDefinition().getCustomConfiguration();
    }

    public void setBuildDirectoryManager(BuildDirectoryManager buildDirectoryManager) {
        logger.debug(String.format("setBuildDirectoryManager: setting build directory manager, %s..",buildDirectoryManager.toString()));
        this.buildDirectoryManager = buildDirectoryManager;
    }

    public synchronized void setTextProvider(TextProvider textProvider) {
        // perform a quick test
        // not sure why this is the case, but textprovider fails on remote agent
        // There's some tickets open against it and open end questions:
        // https://answers.atlassian.com/questions/20566/textprovider-in-sdk-bamboo-helloworld-task-example-does-not-work
        String test = textProvider.getText("repository.gerrit.name");
        if (test != null) {
            logger.debug("setTextProvider: On local agent, keeping textProvider..");
            this.textProvider = textProvider;
        } else {
            logger.debug("setTextProvider: On remote agent, switching textProvider..");
            LazyComponentReference<I18nBeanFactory> i18nBeanFactoryReference =
                new LazyComponentReference<I18nBeanFactory>("i18nBeanFactory");
            I18nBeanFactory i18nBeanFactory = i18nBeanFactoryReference.get();
            this.textProvider =
                new TextProviderAdapter(i18nBeanFactory.getI18nBean(Locale.getDefault()));
        }

    }

    public void setAdministrationConfigurationAccessor(AdministrationConfigurationAccessor administrationConfigAccessor) {
        this.administrationConfigurationAccess = administrationConfigAccessor;
    }

    @Override
    public void prepareConfigObject(BuildConfiguration buildConfiguration) {
        super.prepareConfigObject(buildConfiguration);
    }

    @Override
    public ErrorCollection validate(BuildConfiguration buildConfiguration) {
        return super.validate(buildConfiguration);
    }

    @Override
    protected void populateContextForView(Map<String, Object> context, Plan plan) {
        super.populateContextForView(context, plan);
    }

    @Override
    protected void populateContextForEdit(Map<String, Object> context, BuildConfiguration buildConfiguration, Plan plan) {
        super.populateContextForEdit(context, buildConfiguration, plan);
    }

    private String buildStatusString(CurrentBuildResult results) {
        AdministrationConfiguration config = administrationConfigurationAccess.getAdministrationConfiguration();

        String resultsUrl = config.getBaseUrl() + "/browse/" + buildContext.getPlanResultKey().toString();

        List<String> errors = results.getBuildErrors();

        if (!results.getBuildState().equals(BuildState.SUCCESS)) {
            return textProvider.getText("processor.gerrit.messages.build.failed", Arrays.asList(resultsUrl));
        }

        String msg =
            textProvider.getText("processor.gerrit.messages.build.sucess",Arrays.asList(resultsUrl));

        logger.debug("buildStatusString: " + msg);

        return msg;
    }

    @Override
    public BuildContext call() throws InterruptedException, Exception {
        final String buildPlanKey = buildContext.getPlanKey();
        final CurrentBuildResult results = buildContext.getBuildResult();
        final Boolean runVerification = Boolean.parseBoolean(customConfiguration.get(GERRIT_RUN));

        logger.info("Run verification: " + runVerification);

        if (runVerification) {
            final List<PlanRepositoryDefinition> vcsRepositories = buildContext.getVcsRepositories();

            for (PlanRepositoryDefinition definition : vcsRepositories) {
                if (definition.getPluginKey().equals(GerritConstants.VCS_MODULE_KEY)) {
                    logger.info("Updating Change Verification...");
                    updateChangeVerification(definition, buildPlanKey, results);
                }
            }
        }

        return buildContext;
    }

    private void updateChangeVerification(PlanRepositoryDefinition definition,
                                             String buildPlanKey,
                                             CurrentBuildResult results) throws RepositoryException {
        GerritConfig gc = this.getAccessData(definition);
        String revNumber = results.getCustomBuildData().get("repository.revision.number");
        final String vcsRevision = buildContext.getBuildChanges().getVcsRevisionKey(definition.getId());
        final String prevVcsRevision = buildContext.getBuildChanges().getPreviousVcsRevisionKey(definition.getId());
        this.prepareSSHKeyFile(gc);
        final GerritService service = new GerritService(gc);

        logger.debug(String.format("revNumber=%s, vcsRevision=%s, prevVcsRevision=%s", revNumber,vcsRevision, prevVcsRevision));

        try {
            final GerritChangeVO change = service.getChangeByRevision(vcsRevision);

            if (change == null) {
                logger.error(textProvider.getText("repository.gerrit.messages.error.retrieve"));
                return;
            } else if (change.isMerged()) {
                logger.info(textProvider.getText("processor.gerrit.messages.build.verified.merged",Arrays.asList(change.getId())));
                return;
            }

            if ((results.getBuildReturnCode() == 0) && results.getBuildState().equals(BuildState.SUCCESS)) {
                if (service.verifyChange(true, change.getNumber(), change.getCurrentPatchSet().getNumber(), buildStatusString(results))) {
                    logger.info(textProvider.getText("processor.gerrit.messages.build.verified.pos"));
                } else {
                    logger.error(textProvider.getText("processor.gerrit.messages.build.verified.failed",Arrays.asList(change.getId())));
                }
            } else if (service.verifyChange(false, change.getNumber(), change.getCurrentPatchSet().getNumber(), buildStatusString(results))) {
                logger.info(textProvider.getText("processor.gerrit.messages.build.verified.neg"));
            } else {
                logger.error(textProvider.getText("processor.gerrit.messages.build.verified.failed",Arrays.asList(change.getId())));
            }
        } finally {
            this.cleanUpSSHFile();
        }
    }

    protected GerritConfig getAccessData(@NotNull VcsRepositoryData vcsRepositoryData) {
        Map<String, String> serverCfg = vcsRepositoryData.getVcsLocation().getConfiguration();

        String commandTimeoutString = serverCfg.get(REPOSITORY_GERRIT_COMMAND_TIMEOUT);
        int commandTimeout = StringUtils.isNotBlank(commandTimeoutString) ? Integer.parseInt(commandTimeoutString) : DEFAULT_COMMAND_TIMEOUT_IN_MINUTES;

        final String sshPassphrase = serverCfg.getOrDefault(REPOSITORY_GERRIT_SSH_PASSPHRASE, "");
        final String sshKey = serverCfg.getOrDefault(REPOSITORY_GERRIT_SSH_KEY, "");
        final String username = serverCfg.getOrDefault(REPOSITORY_GERRIT_USERNAME, "");
        final String email = serverCfg.getOrDefault(REPOSITORY_GERRIT_EMAIL, "");
        final String hostname = StringUtils.trimToEmpty(serverCfg.get(REPOSITORY_GERRIT_REPOSITORY_HOSTNAME));
        final String project = StringUtils.trimToEmpty(serverCfg.get(REPOSITORY_GERRIT_PROJECT));
        int port = GerritConstants.REPOSITORY_GERRIT_REPOSITORY_DEFAULT_PORT;
        try {
            port = Integer.valueOf(serverCfg.get(REPOSITORY_GERRIT_REPOSITORY_PORT));
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Could not parse port value, using default");
            }
        }

        String repositoryUrl = "ssh://" + username + "@" + hostname + ":" + port + "/" + project;

        return GerritConfig.builder()
                .host(hostname)
                .port(port)
                .project(project)
                .username(username)
                .userEmail(email)
                .repositoryUrl(repositoryUrl)
                .branch(vcsRepositoryData.getBranch().getVcsBranch())
                .sshKey(sshKey)
                .sshPassphrase(sshPassphrase)
                .useShallowClones(Boolean.valueOf(serverCfg.get(REPOSITORY_GERRIT_USE_SHALLOW_CLONES)))
//                .useRemoteAgentCache(Boolean.valueOf(serverCfg.get(REPOSITORY_GIT_USE_REMOTE_AGENT_CACHE)))
                .useSubmodules(Boolean.valueOf(serverCfg.get(REPOSITORY_GERRIT_USE_SUBMODULES)))
                .commandTimeout(commandTimeout)
                .verboseLogs(Boolean.valueOf(serverCfg.get(REPOSITORY_GERRIT_VERBOSE_LOGS)))
//                .lfs(Boolean.valueOf(serverCfg.getOrDefault(REPOSITORY_GIT_LFS_REPOSITORY, "false")))
                .build();
    }

    protected GerritConfig.Builder getSubstitutedAccessDataBuilder(@NotNull VcsRepositoryData vcsRepositoryData) {
        GerritConfig accessData = getAccessData(vcsRepositoryData);

        return GerritConfig.builder(accessData)
                .repositoryUrl(substituteString(accessData.getRepositoryUrl()))
                .host(accessData.getHost())
                .port(accessData.getPort())
                .project(accessData.getProject())
                .username(substituteString(accessData.getUsername()))
                .userEmail(substituteString(accessData.getUserEmail()))
                .useShallowClones(accessData.isUseShallowClones())
                .useSubmodules(accessData.isUseSubmodules())
                .sshKey(accessData.getSshKey())
                .commandTimeout(accessData.getCommandTimeout())
                .verboseLogs(accessData.isVerboseLogs())
                .sshPassphrase(accessData.getSshPassphrase());
    }

    protected GerritConfig getSubstitutedAccessData(@NotNull VcsRepositoryData vcsRepositoryData) {
        return getSubstitutedAccessDataBuilder(vcsRepositoryData).build();
    }

    protected String substituteString(@Nullable final String stringWithValuesToSubstitute) {
        return customVariableContext.substituteString(stringWithValuesToSubstitute);
    }

    protected synchronized void prepareSSHKeyFile(GerritConfig gerritConfig) throws RepositoryException {
        try {
            sshKeyFile = File.createTempFile("gerritShhKey", ".txt");
            sshKeyFile.setReadable(true, true);
            sshKeyFile.setWritable(true, true);
            sshKeyFile.setExecutable(false, false);
            FileUtils.writeStringToFile(sshKeyFile, gerritConfig.getSshKey());
            if (SystemUtils.IS_OS_UNIX || SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC_OSX)
                Runtime.getRuntime().exec("chmod 700 " + sshKeyFile.getAbsolutePath());
            gerritConfig.setSshKeyFile(sshKeyFile);
        } catch (IOException e) {
            throw new RepositoryException(this.textProvider.getText("repository.gerrit.messages.ssh.file.error"));
        }
    }

    public void cleanUpSSHFile() {
        if (sshKeyFile != null) {
            sshKeyFile.delete();
        }
    }
}
