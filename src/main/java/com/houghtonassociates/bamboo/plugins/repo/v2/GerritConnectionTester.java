package com.houghtonassociates.bamboo.plugins.repo.v2;

import com.atlassian.bamboo.build.fileserver.BuildDirectoryManager;
import com.atlassian.bamboo.credentials.CredentialsAccessor;
import com.atlassian.bamboo.repository.RepositoryException;
import com.atlassian.bamboo.security.TrustedKeyHelper;
import com.atlassian.bamboo.ssh.SshProxyService;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.bamboo.utils.error.SimpleErrorCollection;
import com.atlassian.bamboo.utils.i18n.I18nBeanFactory;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.bamboo.vcs.configuration.VcsRepositoryData;
import com.atlassian.bamboo.vcs.runtime.VcsConnectionTester;
import com.houghtonassociates.bamboo.plugins.GerritHelper;
import com.houghtonassociates.bamboo.plugins.dao.GerritConfig;
import com.houghtonassociates.bamboo.plugins.dao.GerritService;
import com.opensymphony.xwork2.TextProvider;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class GerritConnectionTester extends AbstractGerritExecutor implements VcsConnectionTester {
    private final Logger LOG = Logger.getLogger(this.getClass());

    public GerritConnectionTester(SshProxyService sshProxyService,
                                  CapabilityContext capabilityContext,
                                  CredentialsAccessor credentialsAccessor,
                                  CustomVariableContext customVariableContext,
                                  BuildDirectoryManager buildDirectoryManager,
                                  I18nBeanFactory i18nBeanFactory,
                                  TrustedKeyHelper trustedKeyHelper) {
        super(sshProxyService, capabilityContext, credentialsAccessor, customVariableContext, buildDirectoryManager, trustedKeyHelper, i18nBeanFactory);
    }

    @NotNull
    @Override
    public ErrorCollection testConnection(@NotNull final VcsRepositoryData repositoryData, final long timeout, @NotNull final TimeUnit unit) {
        final GerritConfig substitutedAccessData = getSubstitutedAccessData(repositoryData);

        if (StringUtils.isEmpty(substitutedAccessData.getSshKey())) {
            return new SimpleErrorCollection(textProvider.getText("repository.test.connection.private.key.not.received")); // FIXME : change key
        }

        return testConnectionToRepository(repositoryData, substitutedAccessData);
    }

    @NotNull
    private ErrorCollection testConnectionToRepository(@NotNull final VcsRepositoryData repositoryData, GerritConfig substitutedAccessData) {
        final ErrorCollection errorCollection = new SimpleErrorCollection();
        GerritHelper helper = null;
        try {
            this.prepareSSHKeyFile(substitutedAccessData);
            GerritService gerrit = new GerritService(substitutedAccessData);
            try {
                gerrit.testGerritConnection();
                if (!gerrit.isGerritProject(substitutedAccessData.getProject()))
                    errorCollection.addError(GerritConstants.REPOSITORY_GERRIT_PROJECT,"Project doesn't exist!");
            } catch (Exception e) {
                errorCollection.addError(GerritConstants.REPOSITORY_GERRIT_REPOSITORY_HOSTNAME,e.getMessage());
                LOG.error("Unable to test connection", e);
            }
        } catch (RepositoryException e) {
            errorCollection.addError(GerritConstants.REPOSITORY_GERRIT_REPOSITORY_HOSTNAME,"unable to test connection");
            LOG.error("Unable to test connection", e);
        } finally {
            if (helper != null) {
                this.cleanUpSSHFile();
            }
        }

        return errorCollection;
    }
}
