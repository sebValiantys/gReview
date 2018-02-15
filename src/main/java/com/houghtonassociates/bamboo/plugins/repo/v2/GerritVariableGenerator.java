package com.houghtonassociates.bamboo.plugins.repo.v2;

import com.atlassian.bamboo.build.fileserver.BuildDirectoryManager;
import com.atlassian.bamboo.credentials.CredentialsAccessor;
import com.atlassian.bamboo.security.TrustedKeyHelper;
import com.atlassian.bamboo.ssh.SshProxyService;
import com.atlassian.bamboo.utils.i18n.I18nBeanFactory;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.bamboo.vcs.configuration.VcsRepositoryData;
import com.atlassian.bamboo.vcs.runtime.VcsVariableGenerator;
import com.google.common.collect.Maps;
import com.houghtonassociates.bamboo.plugins.dao.GerritConfig;
import com.opensymphony.xwork2.TextProvider;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class GerritVariableGenerator extends AbstractGerritExecutor implements VcsVariableGenerator {
    private static final Logger log = Logger.getLogger(GerritVariableGenerator.class);

    private static final String REPOSITORY_URL = "repositoryUrl";
    private static final String REPOSITORY_USERNAME = "username";

    public GerritVariableGenerator(SshProxyService sshProxyService,
                                   CapabilityContext capabilityContext,
                                   CredentialsAccessor credentialsAccessor,
                                   CustomVariableContext customVariableContext,
                                   BuildDirectoryManager buildDirectoryManager,
                                   TextProvider textProvider,
                                   TrustedKeyHelper trustedKeyHelper,
                                   I18nBeanFactory i18nBeanFactory) {
        super(sshProxyService, capabilityContext, credentialsAccessor, customVariableContext, buildDirectoryManager, trustedKeyHelper, i18nBeanFactory);
    }


    @Nullable
    @Override
    public String getLegacyPrefix() {
        // FIXME : check if value is correct (if it is even necessary ...)
        return "repository.gerrit";
    }

    @NotNull
    @Override
    public Map<String, String> getPlanRepositoryVariables(@NotNull final VcsRepositoryData vcsRepositoryData) {
        final GerritConfig substitutedAccessData = getSubstitutedAccessData(vcsRepositoryData);
        Map<String, String> variables = Maps.newHashMap();
        // Returns same values as GerritRepositoryAdapter.getPlanRepositoryVariables()
        variables.put(REPOSITORY_URL, substitutedAccessData.getRepositoryUrl());
        variables.put(REPOSITORY_USERNAME, substitutedAccessData.getUsername());
        return variables;
    }
}
