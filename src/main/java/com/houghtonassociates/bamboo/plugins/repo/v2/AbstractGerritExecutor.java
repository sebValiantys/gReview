package com.houghtonassociates.bamboo.plugins.repo.v2;

import com.atlassian.bamboo.build.fileserver.BuildDirectoryManager;
import com.atlassian.bamboo.credentials.CredentialsAccessor;
import com.atlassian.bamboo.plugins.git.GitCapabilityTypeModule;
import com.atlassian.bamboo.repository.RepositoryException;
import com.atlassian.bamboo.security.TrustedKeyHelper;
import com.atlassian.bamboo.ssh.SshProxyService;
import com.atlassian.bamboo.utils.i18n.I18nBean;
import com.atlassian.bamboo.utils.i18n.I18nBeanFactory;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.bamboo.vcs.configuration.VcsRepositoryData;
import com.houghtonassociates.bamboo.plugins.dao.GerritConfig;
import com.opensymphony.xwork2.TextProvider;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static com.houghtonassociates.bamboo.plugins.repo.v2.GerritConstants.*;

public abstract class AbstractGerritExecutor {
    private static final Logger log = Logger.getLogger(AbstractGerritExecutor.class);

    public static final String DEFAULT_BRANCH = "master";
    
    protected SshProxyService sshProxyService;
    private CapabilityContext capabilityContext;
    private CredentialsAccessor credentialsAccessor;
    protected CustomVariableContext customVariableContext;
    protected BuildDirectoryManager buildDirectoryManager;
    //protected I18nResolver i18nResolver;
    protected I18nBean textProvider;
    protected TrustedKeyHelper trustedKeyHelper;
    private File sshKeyFile;

    public AbstractGerritExecutor(SshProxyService sshProxyService,
                                  CapabilityContext capabilityContext,
                                  CredentialsAccessor credentialsAccessor,
                                  CustomVariableContext customVariableContext,
                                  BuildDirectoryManager buildDirectoryManager,
                                  TrustedKeyHelper trustedKeyHelper,
                                  I18nBeanFactory i18nBeanFactory) {
        this.sshProxyService = sshProxyService;
        this.capabilityContext = capabilityContext;
        this.credentialsAccessor = credentialsAccessor;
        this.customVariableContext = customVariableContext;
        this.buildDirectoryManager = buildDirectoryManager;
        this.textProvider = i18nBeanFactory.getI18nBean();
        this.trustedKeyHelper = trustedKeyHelper;
    }

    @Nullable
    public File getWorkingDirectory() {
        return buildDirectoryManager.getWorkingDirectoryOfCurrentAgent();
    }

    @Nullable
    public String getGitCapability() {
        return capabilityContext.getCapabilityValue(GitCapabilityTypeModule.GIT_CAPABILITY);
    }

    @Nullable
    public String getSshCapability() {
        return capabilityContext.getCapabilityValue(GitCapabilityTypeModule.SSH_CAPABILITY);
    }

    protected String substituteString(@Nullable final String stringWithValuesToSubstitute) {
        return customVariableContext.substituteString(stringWithValuesToSubstitute);
    }

//    public static boolean isUsingSharedCredentials(@NotNull Map<String, String> serverCfg) {
//        final String chosenAuthentication = serverCfg.getOrDefault(GitRepository.REPOSITORY_GIT_AUTHENTICATION_TYPE, GitAuthenticationType.NONE.name());
//        final String sshCredentialsSource = serverCfg.getOrDefault(GitRepository.REPOSITORY_GIT_SSH_CREDENTIALS_SOURCE, GitSshCredentialsSource.CUSTOM.name());
//        final String passwordCredentialsSource = serverCfg.getOrDefault(GitRepository.REPOSITORY_GIT_PASSWORD_CREDENTIALS_SOURCE, GitPasswordCredentialsSource.CUSTOM.name());
//        return GitAuthenticationType.SSH_KEYPAIR.name().equals(chosenAuthentication) && GitSshCredentialsSource.SHARED_CREDENTIALS.name().equals(sshCredentialsSource) ||
//                GitAuthenticationType.PASSWORD.name().equals(chosenAuthentication) && GitPasswordCredentialsSource.SHARED_CREDENTIALS.name().equals(passwordCredentialsSource);
//    }

//    @NotNull
//    public static String getSharedCredentialsIdField(@NotNull Map<String, String> config) {
//        return GitAuthenticationType.SSH_KEYPAIR.name().equals(config.getOrDefault(GitRepository.REPOSITORY_GIT_AUTHENTICATION_TYPE, GitAuthenticationType.NONE.name()))
//                ? GitRepository.REPOSITORY_GIT_SSH_SHAREDCREDENTIALS_ID
//                : GitRepository.REPOSITORY_GIT_PASSWORD_SHAREDCREDENTIALS_ID;
//    }

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
            if (log.isDebugEnabled()) {
                log.debug("Could not parse port value, using default");
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

    /*
        Code copied from AbstractGitExecutor.
        TODO : check if needed. Handles Repository cache path
     */
//    protected Path getCachePath(@NotNull final GitRepositoryAccessData substitutedAccessData) {
//        return GitCacheDirectory.getCachePath(buildDirectoryManager.getBaseBuildWorkingDirectory(), substitutedAccessData);
//    }


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

    public void setCapabilityContext(final CapabilityContext capabilityContext) {
        this.capabilityContext = capabilityContext;
    }

    public void setCustomVariableContext(final CustomVariableContext customVariableContext) {
        this.customVariableContext = customVariableContext;
    }

    public void setCredentialsAccessor(final CredentialsAccessor credentialsAccessor) {
        this.credentialsAccessor = credentialsAccessor;
    }

    public void setBuildDirectoryManager(final BuildDirectoryManager buildDirectoryManager) {
        this.buildDirectoryManager = buildDirectoryManager;
    }

    public void setSshProxyService(final SshProxyService sshProxyService) {
        this.sshProxyService = sshProxyService;
    }

    public void setTrustedKeyHelper(final TrustedKeyHelper trustedKeyHelper) {
        this.trustedKeyHelper = trustedKeyHelper;
    }
}
