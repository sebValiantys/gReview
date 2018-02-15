package com.houghtonassociates.bamboo.plugins.repo.v2.configurator;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.credentials.CredentialsAccessor;
import com.atlassian.bamboo.crypto.instance.SecretEncryptionService;
import com.atlassian.bamboo.fileserver.SystemDirectory;
import com.atlassian.bamboo.utils.BambooFieldValidate;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.bamboo.vcs.configuration.VcsLocationDefinition;
import com.atlassian.bamboo.vcs.configurator.VcsLocationConfigurator;
import com.atlassian.bamboo.vcs.configurator.VcsType;
import com.atlassian.sal.api.message.I18nResolver;
import com.google.common.collect.Maps;
import com.houghtonassociates.bamboo.plugins.dao.GerritService;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.jgit.transport.URIish;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;

import static com.houghtonassociates.bamboo.plugins.repo.v2.GerritConstants.*;

public class GerritServerConfigurator implements VcsLocationConfigurator {
    private static final Logger log = Logger.getLogger(GerritServerConfigurator.class);

    private I18nResolver i18nResolver;
    private CustomVariableContext customVariableContext;
    private CredentialsAccessor credentialsAccessor;
    private final SecretEncryptionService secretEncryptionService;

//    @Autowired
    public GerritServerConfigurator(CustomVariableContext customVariableContext,
                                    I18nResolver i18nResolver,
                                    CredentialsAccessor credentialsAccessor,
                                    SecretEncryptionService secretEncryptionService) {
        this.i18nResolver = i18nResolver;
        this.customVariableContext = customVariableContext;
        this.credentialsAccessor = credentialsAccessor;
        this.secretEncryptionService = secretEncryptionService;
    }


    @Override
    public void populateContextForCreate(@NotNull final Map<String, Object> context) {
        context.put(REPOSITORY_GERRIT_COMMAND_TIMEOUT, DEFAULT_COMMAND_TIMEOUT_IN_MINUTES);
        // TODO : add default branch ? and other things here ?
    }

    private void populateContextCommon(@NotNull final Map<String, Object> context, @NotNull final VcsLocationDefinition vcsLocationDefinition) {
        final Map<String, String> cfg = vcsLocationDefinition.getConfiguration();

        context.put(REPOSITORY_GERRIT_REPOSITORY_HOSTNAME, cfg.get(REPOSITORY_GERRIT_REPOSITORY_HOSTNAME));
        context.put(REPOSITORY_GERRIT_REPOSITORY_PORT, cfg.get(REPOSITORY_GERRIT_REPOSITORY_PORT));
        context.put(REPOSITORY_GERRIT_PROJECT, cfg.get(REPOSITORY_GERRIT_PROJECT));
        context.put(REPOSITORY_GERRIT_USERNAME, cfg.get(REPOSITORY_GERRIT_USERNAME));
        context.put(REPOSITORY_GERRIT_USE_SHALLOW_CLONES, Boolean.parseBoolean(cfg.get(REPOSITORY_GERRIT_USE_SHALLOW_CLONES)));
//        context.put(REPOSITORY_GIT_USE_REMOTE_AGENT_CACHE, cfg.get(REPOSITORY_GIT_USE_REMOTE_AGENT_CACHE));
        context.put(REPOSITORY_GERRIT_USE_SUBMODULES, cfg.get(REPOSITORY_GERRIT_USE_SUBMODULES));
        context.put(REPOSITORY_GERRIT_COMMAND_TIMEOUT, cfg.get(REPOSITORY_GERRIT_COMMAND_TIMEOUT));
        context.put(REPOSITORY_GERRIT_VERBOSE_LOGS, cfg.get(REPOSITORY_GERRIT_VERBOSE_LOGS));
        context.put(REPOSITORY_GERRIT_DEFAULT_BRANCH, cfg.get(REPOSITORY_GERRIT_DEFAULT_BRANCH));
        context.put(REPOSITORY_GERRIT_CUSTOM_BRANCH, cfg.get(REPOSITORY_GERRIT_CUSTOM_BRANCH));

        //FIXME : add branch data too ?
//        context.put(REPOSITORY_GIT_FETCH_WHOLE_REPOSITORY, Boolean.parseBoolean(cfg.get(REPOSITORY_GIT_FETCH_WHOLE_REPOSITORY)));
//        context.put(REPOSITORY_GIT_LFS_REPOSITORY, Boolean.parseBoolean(cfg.get(REPOSITORY_GIT_LFS_REPOSITORY)));

        //don't put private key nor passphrase into view
        putDummyIfValueIsNotBlank(context, cfg, REPOSITORY_GERRIT_SSH_KEY);
        putDummyIfValueIsNotBlank(context, cfg, REPOSITORY_GERRIT_SSH_PASSPHRASE);
        // context.get("vcsDefinition").getId()
    }

    private void putDummyIfValueIsNotBlank(@NotNull final Map<String, Object> context, @NotNull final Map<String, String> cfg, final String key) {
        if (StringUtils.isNotBlank(cfg.get(key))) {
            context.put(key, "dummy");
        }
    }

    @Override
    public void populateContextForEdit(@NotNull final Map<String, Object> context, @NotNull final VcsLocationDefinition vcsLocationDefinition) {
        populateContextCommon(context, vcsLocationDefinition);
    }

    @Override
    public void populateContextForView(@NotNull final Map<String, Object> context, @NotNull final VcsLocationDefinition vcsLocationDefinition) {
        populateContextCommon(context, vcsLocationDefinition);
    }

    private String substituteString(@Nullable final String stringWithValuesToSubstitute) {
        return customVariableContext.substituteString(stringWithValuesToSubstitute);
    }

    @Override
    public void validate(@NotNull final ActionParametersMap actionParametersMap,
                         @Nullable final VcsLocationDefinition previousDefinition,
                         @NotNull final ErrorCollection errorCollection) {
        Map<String, String> cfgMap = generateConfigMap(actionParametersMap, previousDefinition);

        final String hostname = StringUtils.trim(cfgMap.get(REPOSITORY_GERRIT_REPOSITORY_HOSTNAME));
        final String username = StringUtils.trim(cfgMap.get(REPOSITORY_GERRIT_USERNAME));
        final String portAsString = StringUtils.trim(cfgMap.get(REPOSITORY_GERRIT_REPOSITORY_PORT));
        final String project = StringUtils.trim(cfgMap.get(REPOSITORY_GERRIT_PROJECT));

        if (BambooFieldValidate.findFieldShellInjectionViolation(errorCollection,
                i18nResolver,
                REPOSITORY_GERRIT_REPOSITORY_HOSTNAME,
                substituteString(cfgMap.get(REPOSITORY_GERRIT_REPOSITORY_HOSTNAME)))) {
            return;
        }

        if (BambooFieldValidate.findFieldShellInjectionViolation(errorCollection,
                i18nResolver,
                REPOSITORY_GERRIT_USERNAME,
                substituteString(cfgMap.get(REPOSITORY_GERRIT_USERNAME)))) {
            return;
        }

        if (BambooFieldValidate.findFieldShellInjectionViolation(errorCollection,
                i18nResolver,
                REPOSITORY_GERRIT_REPOSITORY_PORT,
                substituteString(cfgMap.get(REPOSITORY_GERRIT_REPOSITORY_PORT)))) {
            return;
        }

        if (BambooFieldValidate.findFieldShellInjectionViolation(errorCollection,
                i18nResolver,
                REPOSITORY_GERRIT_PROJECT,
                substituteString(cfgMap.get(REPOSITORY_GERRIT_PROJECT)))) {
            return;
        }

        boolean areUrlParamsCorrect = true;

        if (StringUtils.isBlank(hostname)) {
            errorCollection.addError(REPOSITORY_GERRIT_REPOSITORY_HOSTNAME, i18nResolver.getText("repository.gerrit.messages.empty.hostname"));
            areUrlParamsCorrect = false;
        }

        if (StringUtils.isBlank(username)) {
            errorCollection.addError(REPOSITORY_GERRIT_USERNAME, i18nResolver.getText("repository.gerrit.messages.empty.username"));
            areUrlParamsCorrect = false;
        }

        if (StringUtils.isBlank(portAsString)) {
            errorCollection.addError(REPOSITORY_GERRIT_REPOSITORY_PORT, i18nResolver.getText("repository.gerrit.messages.empty.port"));
            areUrlParamsCorrect = false;
        } else {
            try {
                Integer.valueOf(portAsString);
            } catch (NumberFormatException e) {
                errorCollection.addError(REPOSITORY_GERRIT_REPOSITORY_PORT, i18nResolver.getText("repository.gerrit.messages.wrong.port"));
                areUrlParamsCorrect = false;
            }
        }

        if (StringUtils.isBlank(project)) {
            errorCollection.addError(REPOSITORY_GERRIT_PROJECT, i18nResolver.getText("repository.gerrit.messages.empty.project"));
            areUrlParamsCorrect = false;
        }

        if (areUrlParamsCorrect) {
            String gitRepoUrl = "ssh://" + username + "@" + hostname + ":" + portAsString + "/" + project;
            try {
                final URIish uri = new URIish(gitRepoUrl);
            } catch (URISyntaxException e) {
                errorCollection.addErrorMessage(i18nResolver.getText("repository.gerrit.messages.error.connection"));
            }
        }

        String key = cfgMap.get(REPOSITORY_GERRIT_SSH_KEY);
        if (!StringUtils.isBlank(key) && !key.contains("-----BEGIN")) {
            errorCollection.addError(TEMPORARY_GERRIT_SSH_KEY_CHANGE, i18nResolver.getText("repository.gerrit.messages.error.ssh.key.invalid"));
        }
    }

    @NotNull
    @Override
    public void validateForConnectionTesting(@NotNull final ActionParametersMap actionParametersMap,
                                             @Nullable final VcsLocationDefinition previousDefinition,
                                             @NotNull final ErrorCollection errorCollection) {
        final boolean changingSshKey = actionParametersMap.getBoolean(TEMPORARY_GERRIT_SSH_KEY_CHANGE);
        if (changingSshKey) {
            errorCollection.addError(TEMPORARY_GERRIT_SSH_KEY_FROM_FILE, i18nResolver.getText("repository.test.connection.private.key.not.received"));
        } else {
            validate(actionParametersMap, previousDefinition, errorCollection);
        }
    }

    @NotNull
    @Override
    public Map<String, String> generateConfigMap(@NotNull final ActionParametersMap actionParametersMap,
                                                 @Nullable final VcsLocationDefinition previousVcsLocationDefinition) {
        final Map<String, String> cfgMap = Maps.newHashMap();
        if (previousVcsLocationDefinition != null) {
            cfgMap.putAll(previousVcsLocationDefinition.getConfiguration());
        }

        cfgMap.put(REPOSITORY_GERRIT_REPOSITORY_HOSTNAME, actionParametersMap.getString(REPOSITORY_GERRIT_REPOSITORY_HOSTNAME));
        cfgMap.put(REPOSITORY_GERRIT_REPOSITORY_PORT, actionParametersMap.getString(REPOSITORY_GERRIT_REPOSITORY_PORT));
        cfgMap.put(REPOSITORY_GERRIT_USERNAME, actionParametersMap.getString(REPOSITORY_GERRIT_USERNAME));
        cfgMap.put(REPOSITORY_GERRIT_PROJECT, actionParametersMap.getString(REPOSITORY_GERRIT_PROJECT));
        cfgMap.put(REPOSITORY_GERRIT_USE_SHALLOW_CLONES, Boolean.toString(actionParametersMap.getBoolean(REPOSITORY_GERRIT_USE_SHALLOW_CLONES)));
        cfgMap.put(REPOSITORY_GERRIT_DEFAULT_BRANCH, actionParametersMap.getString(REPOSITORY_GERRIT_DEFAULT_BRANCH));
        cfgMap.put(REPOSITORY_GERRIT_CUSTOM_BRANCH, actionParametersMap.getString(REPOSITORY_GERRIT_CUSTOM_BRANCH));
        cfgMap.put(REPOSITORY_GERRIT_BRANCH, actionParametersMap.getString(REPOSITORY_GERRIT_BRANCH));
        cfgMap.put(REPOSITORY_GERRIT_USE_SUBMODULES, Boolean.toString(actionParametersMap.getBoolean(REPOSITORY_GERRIT_USE_SUBMODULES)));
        cfgMap.put(REPOSITORY_GERRIT_COMMAND_TIMEOUT, actionParametersMap.getString(REPOSITORY_GERRIT_COMMAND_TIMEOUT));
        cfgMap.put(REPOSITORY_GERRIT_VERBOSE_LOGS, Boolean.toString(actionParametersMap.getBoolean(REPOSITORY_GERRIT_VERBOSE_LOGS)));

        if (actionParametersMap.getBoolean(TEMPORARY_GERRIT_SSH_KEY_CHANGE)) {
            final File keyFile = actionParametersMap.getFiles().get(TEMPORARY_GERRIT_SSH_KEY_FROM_FILE);
            if (keyFile != null) {
                try {
                    String key = FileUtils.readFileToString(keyFile);
                    cfgMap.put(REPOSITORY_GERRIT_SSH_KEY, key);
                } catch (IOException e) {
                    log.error("Cannot read uploaded ssh key file", e);
                }
            } else {
                cfgMap.remove(REPOSITORY_GERRIT_SSH_KEY);
            }
        }
        if (actionParametersMap.getBoolean(TEMPORARY_GERRIT_SSH_PASSPHRASE_CHANGE)) {
            cfgMap.put(REPOSITORY_GERRIT_SSH_PASSPHRASE, actionParametersMap.getString(TEMPORARY_GERRIT_SSH_PASSPHRASE));
        }

        return cfgMap;
    }

    public void addDefaultsForAdvancedOptions(final Map<String, String> cfgMap) {
        cfgMap.put(REPOSITORY_GERRIT_COMMAND_TIMEOUT, Integer.toString(DEFAULT_COMMAND_TIMEOUT_IN_MINUTES));
        cfgMap.put(REPOSITORY_GERRIT_DEFAULT_BRANCH, DEFAULT_BRANCH);
    }

    @NotNull
    @Override
    public String getLocationIdentifier(@NotNull final VcsLocationDefinition vcsLocationDefinition) {
        Map<String, String> config = vcsLocationDefinition.getConfiguration();
        String hostname = config.get(REPOSITORY_GERRIT_REPOSITORY_HOSTNAME);
        String username = config.get(REPOSITORY_GERRIT_USERNAME);
        String port = config.get(REPOSITORY_GERRIT_REPOSITORY_PORT);
        String project = config.get(REPOSITORY_GERRIT_PROJECT);
        String gitRepoUrl ="ssh://" + username + "@" + hostname + ":" + port + "/" + project;
        return gitRepoUrl;
    }

    @NotNull
    @Override
    public String getServerHost(@NotNull final VcsLocationDefinition vcsLocationDefinition) {
        String hostname = vcsLocationDefinition.getConfiguration().get(REPOSITORY_GERRIT_REPOSITORY_HOSTNAME);
        return hostname;
    }

    @Override
    public Optional<VcsType> getScmType(@NotNull final VcsLocationDefinition vcsLocationDefinition) {
        return Optional.of(VcsType.GIT);
    }

    private String getBaseConfigPath() {
        String bambooConfigPath = SystemDirectory.getConfigDirectory().getAbsolutePath();
        String baseGerritConfigPath = bambooConfigPath + File.separator + GerritService.SYSTEM_DIRECTORY
                + File.separator + GerritService.CONFIG_DIRECTORY;
        return baseGerritConfigPath;
    }

    private String getRelativeRepoConfigPath(VcsLocationDefinition repoDefinition) {
        String baseConfigPath = this.getBaseConfigPath();
        String repoConfigPath = baseConfigPath + File.separator; // FIXME : add specific directory to store data. Preferably based on an id
        return repoConfigPath.replace("\\", "/");
    }

    public synchronized File prepareConfigDir(String strRelativePath) {
        String filePath = getBaseConfigPath() + File.separator + strRelativePath;

        File f = new File(filePath);

        f.setReadable(true, true);
        f.setWritable(true, true);
        f.setExecutable(true, true);

        f.mkdir();

        return f;
    }

    public synchronized File prepareSSHKeyFile(String strRelativePath, String sshKey) {
        String filePath = getBaseConfigPath() + File.separator + strRelativePath;

        File f = new File(filePath);

        f.setReadable(true, true);
        f.setWritable(true, true);
        f.setExecutable(false, false);

        try {
            FileUtils.writeStringToFile(f, sshKey);
        } catch (IOException e) {
            log.error(e.getMessage());
            return null;
        }

        try {
            if (SystemUtils.IS_OS_UNIX || SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC_OSX) {
                Runtime.getRuntime().exec("chmod 700 " + filePath);
            }
        } catch (IOException e) {
            log.warn(e.getMessage());
        }

        return f;
    }

    public void setI18nResolver(final I18nResolver i18nResolver) {
        this.i18nResolver = i18nResolver;
    }

    public void setCustomVariableContext(final CustomVariableContext customVariableContext) {
        this.customVariableContext = customVariableContext;
    }

    public void setCredentialsAccessor(final CredentialsAccessor credentialsAccessor) {
        this.credentialsAccessor = credentialsAccessor;
    }
}
