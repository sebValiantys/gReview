package com.houghtonassociates.bamboo.plugins.repo.v2.exporter;

import com.atlassian.bamboo.core.BambooEntityOid;
import com.atlassian.bamboo.credentials.CredentialsAccessor;
import com.atlassian.bamboo.credentials.CredentialsData;
import com.atlassian.bamboo.plugins.git.GitAuthenticationType;
import com.atlassian.bamboo.specs.api.exceptions.PropertiesValidationException;
import com.atlassian.bamboo.specs.api.model.BambooOidProperties;
import com.atlassian.bamboo.specs.api.model.credentials.SharedCredentialsIdentifierProperties;
import com.atlassian.bamboo.specs.api.validators.common.ValidationContext;
import com.atlassian.bamboo.specs.api.validators.common.ValidationProblem;
import com.atlassian.bamboo.specs.model.repository.git.AuthenticationProperties;
import com.atlassian.bamboo.specs.model.repository.git.SshPrivateKeyAuthenticationProperties;
import com.atlassian.bamboo.util.Narrow;
import com.atlassian.bamboo.vcs.configuration.*;
import com.atlassian.bamboo.vcs.export.DefaultVcsRepositoryDataExporter;
import com.atlassian.bamboo.vcs.export.VcsRepositoryDataExporter;
import com.atlassian.sal.api.message.I18nResolver;
import com.houghtonassociates.bamboo.plugins.repo.v2.GerritRepositoryProperties;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.*;

import static com.atlassian.bamboo.specs.api.validators.common.ValidationUtils.containsBambooVariable;
import static com.houghtonassociates.bamboo.plugins.repo.v2.GerritConstants.*;

public class GerritConfigurationExporter implements VcsRepositoryDataExporter<GerritRepository, GerritRepositoryProperties> {
    @Autowired
    private CredentialsAccessor credentialsAccessor;

    @Autowired
    private I18nResolver i18nResolver;

    private final ValidationContext validationContext = ValidationContext.of("Gerrit repository");

    @NotNull
    @Override
    public GerritRepository getEntityPropertiesBuilder(@NotNull final VcsRepositoryData vcsRepositoryData) {
        return new GerritRepository();
    }

    @NotNull
    @Override
    public GerritRepository appendLocationData(@NotNull final GerritRepository builder, @NotNull final VcsLocationDefinition vcsLocationDefinition) {
        Map<String, String> configuration = vcsLocationDefinition.getConfiguration();

        final int commandTimeout = configuration.containsKey(REPOSITORY_GERRIT_COMMAND_TIMEOUT)
                ? Integer.valueOf(configuration.get(REPOSITORY_GERRIT_COMMAND_TIMEOUT))
                : DEFAULT_COMMAND_TIMEOUT_IN_MINUTES;

        final GerritRepository gerritRepository = builder.hostName(configuration.get(REPOSITORY_GERRIT_REPOSITORY_HOSTNAME))
                .port(Integer.valueOf(configuration.get(REPOSITORY_GERRIT_REPOSITORY_PORT)))
                .project(configuration.get(REPOSITORY_GERRIT_PROJECT))
                .branch(configuration.get(REPOSITORY_GERRIT_BRANCH))
                .defaultBranch(configuration.get(REPOSITORY_GERRIT_DEFAULT_BRANCH))
                .shallowClonesEnabled(Boolean.valueOf(configuration.get(REPOSITORY_GERRIT_USE_SHALLOW_CLONES)))
                .submodulesEnabled(Boolean.valueOf(configuration.get(REPOSITORY_GERRIT_USE_SUBMODULES)))
//                .remoteAgentCacheEnabled(Boolean.valueOf(configuration.get(REPOSITORY_GERRIT_USE_REMOTE_AGENT_CACHE)))
                .commandTimeout(Duration.ofMinutes(commandTimeout))
                .verboseLogs(Boolean.valueOf(configuration.get(REPOSITORY_GERRIT_VERBOSE_LOGS)));
//                .lfsEnabled(Boolean.valueOf(configuration.get(REPOSITORY_GERRIT_LFS_REPOSITORY)));

        gerritRepository.authentication(configuration.get(REPOSITORY_GERRIT_SSH_KEY), configuration.get(REPOSITORY_GERRIT_SSH_PASSPHRASE));

        return gerritRepository;
    }

    @NotNull
    @Override
    public GerritRepository appendBranchData(@NotNull final GerritRepository builder, @NotNull final VcsBranchDefinition vcsBranchDefinition) {
        return builder.branch(StringUtils.defaultIfBlank(vcsBranchDefinition.getConfiguration().get(REPOSITORY_GERRIT_BRANCH), "master"));
    }

    @NotNull
    @Override
    public GerritRepository appendChangeDetectionOptions(@NotNull final GerritRepository builder, @NotNull final VcsChangeDetectionOptions changeDetectionOptions) {
        builder.changeDetection(DefaultVcsRepositoryDataExporter.exportStandardChangeDetectionOptions(changeDetectionOptions));
        return builder;
    }

    @NotNull
    @Override
    public GerritRepository appendBranchDetectionOptions(@NotNull final GerritRepository builder, @NotNull final VcsBranchDetectionOptions branchDetectionOptions) {
        return builder;
    }

    @Nullable
    @Override
    public Map<String, String> importLocationData(@NotNull final GerritRepositoryProperties repositoryProperties, @Nullable VcsLocationDefinition existingData) {
        if (repositoryProperties.getHostname() != null) {
            final Map<String, String> configuration = new HashMap<>();
            configuration.put(REPOSITORY_GERRIT_REPOSITORY_HOSTNAME, repositoryProperties.getHostname());
            configuration.put(REPOSITORY_GERRIT_REPOSITORY_PORT, Integer.toString(repositoryProperties.getPort()));
            configuration.put(REPOSITORY_GERRIT_PROJECT, repositoryProperties.getProject());
            configuration.put(REPOSITORY_GERRIT_BRANCH, repositoryProperties.getBranch());
            configuration.put(REPOSITORY_GERRIT_DEFAULT_BRANCH, repositoryProperties.getDefaultBranch());
            configuration.put(REPOSITORY_GERRIT_USE_SHALLOW_CLONES, Boolean.toString(repositoryProperties.isUseShallowClones()));
            configuration.put(REPOSITORY_GERRIT_USE_SUBMODULES, Boolean.toString(repositoryProperties.isUseSubmodules()));
            configuration.put(REPOSITORY_GERRIT_COMMAND_TIMEOUT, Long.toString(repositoryProperties.getCommandTimeout().toMinutes()));
            configuration.put(REPOSITORY_GERRIT_VERBOSE_LOGS, Boolean.toString(repositoryProperties.isVerboseLogs()));
//            configuration.put(REPOSITORY_GERRIT_LFS_REPOSITORY, Boolean.toString(repositoryProperties.isUseLfs()));

            final AuthenticationProperties authenticationProperties = repositoryProperties.getAuthenticationProperties();
            if (authenticationProperties != null) {

                    final SshPrivateKeyAuthenticationProperties sshPrivateKeyAuthenticationProperties =
                            Narrow.to(authenticationProperties, SshPrivateKeyAuthenticationProperties.class);

                    configuration.put(REPOSITORY_GIT_AUTHENTICATION_TYPE, GitAuthenticationType.SSH_KEYPAIR.name());
//                    configuration.put(REPOSITORY_GIT_SSH_CREDENTIALS_SOURCE, GitSshCredentialsSource.CUSTOM.name());
                    configuration.put(REPOSITORY_GERRIT_SSH_KEY, sshPrivateKeyAuthenticationProperties.getSshPrivateKey());
                    configuration.put(REPOSITORY_GERRIT_SSH_PASSPHRASE, StringUtils.defaultString(sshPrivateKeyAuthenticationProperties.getPassphrase()));

            }

            List<ValidationProblem> problems = validateLocationData(configuration);
            if (!problems.isEmpty()) {
                throw new PropertiesValidationException(problems);
            }
            return configuration;
        } else if (!repositoryProperties.hasParent()) {
            throw new PropertiesValidationException(Collections.singletonList(new ValidationProblem("Gerrit server data not defined or incomplete")));
        }
        return null;
    }

    @Nullable
    private CredentialsData findCredentialByOidOrName(final SharedCredentialsIdentifierProperties sharedCredentials) {
        final BambooOidProperties sharedCredentialsOid = sharedCredentials.getOid();
        if (sharedCredentials.getOid() != null) {
            return credentialsAccessor.getCredentialsByOid(BambooEntityOid.createFromExternalValue(sharedCredentialsOid.getOid()));
        } else {
            return credentialsAccessor.getCredentialsByName(sharedCredentials.getName());
        }
    }

    @Nullable
    @Override
    public Map<String, String> importBranchData(@NotNull final GerritRepositoryProperties repositoryProperties, @Nullable VcsBranchDefinition existingData) {
        if (repositoryProperties.getBranch() != null) {
            Map<String, String> configuration = new HashMap<>();
            configuration.put(REPOSITORY_GERRIT_BRANCH, repositoryProperties.getBranch());

            return configuration;
        } else if (!repositoryProperties.hasParent()) {
            throw new PropertiesValidationException(Collections.singletonList(new ValidationProblem("Git branch not defined")));
        }
        return null;
    }

    @Nullable
    @Override
    public Map<String, String> importChangeDetectionOptions(@NotNull final GerritRepositoryProperties repositoryProperties, @Nullable VcsChangeDetectionOptions existingData) {
        return DefaultVcsRepositoryDataExporter.importStandardChangeDetectionOptions(repositoryProperties, repositoryProperties.getVcsChangeDetection());
    }

    @Nullable
    @Override
    public Map<String, String> importBranchDetectionOptions(@NotNull final GerritRepositoryProperties repositoryProperties, @Nullable VcsBranchDetectionOptions existingData) {
        return null;
    }

    @NotNull
    private List<ValidationProblem> validateLocationData(@NotNull Map<String, String> configuration) {
        List<ValidationProblem> errors = new ArrayList<>();

        final String hostName = configuration.get(REPOSITORY_GERRIT_REPOSITORY_HOSTNAME);
        if (hostName != null && !containsBambooVariable(hostName)) {
            ; //TODO : other checks ?
        } else {
            errors.add(new ValidationProblem(i18nResolver.getText("repository.gerrit.messages.empty.hostname")));
        }

        return errors;
    }

    public void setCredentialsAccessor(final CredentialsAccessor credentialsAccessor) {
        this.credentialsAccessor = credentialsAccessor;
    }

    public void setI18nResolver(final I18nResolver i18nResolver) {
        this.i18nResolver = i18nResolver;
    }

}
