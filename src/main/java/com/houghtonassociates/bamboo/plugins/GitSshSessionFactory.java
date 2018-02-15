package com.houghtonassociates.bamboo.plugins;

import com.atlassian.bamboo.security.TrustedKeyHelper;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.util.FS;
import org.jetbrains.annotations.Nullable;

public class GitSshSessionFactory extends JschConfigSessionFactory {
    @Nullable
    private final String key;
    @Nullable
    private final String passphrase;
    private final TrustedKeyHelper trustedKeysHelper;

    GitSshSessionFactory(GitSshSessionFactory factory) {
        this(factory.key, factory.passphrase, factory.trustedKeysHelper);
    }

    GitSshSessionFactory(@Nullable final String key, @Nullable final String passphrase, TrustedKeyHelper trustedKeysHelper) {
        this.key = key;
        this.passphrase = passphrase;
        this.trustedKeysHelper = trustedKeysHelper;
    }

    @Override
    protected void configure(final OpenSshConfig.Host hc, final Session session) {
        final String enableStrictHostKeyCheck = trustedKeysHelper.isCustomAcceptedSshHostKeysEnabled() ? "yes" : "no";
        session.setConfig("StrictHostKeyChecking", enableStrictHostKeyCheck);
    }

    @Override
    protected JSch getJSch(final OpenSshConfig.Host hc, final FS fs) throws JSchException {
        final JSch jsch = super.getJSch(hc, fs);
        jsch.removeAllIdentity();
        if (StringUtils.isNotEmpty(key)) {
            jsch.addIdentity("identityName", key.getBytes(), null, StringUtils.defaultString(passphrase).getBytes());
        }
        if (trustedKeysHelper.isCustomAcceptedSshHostKeysEnabled()) {
            updateHostKeyRepository(jsch);
        }
        return jsch;
    }

    private void updateHostKeyRepository(JSch jsch) throws JSchException {
        //KnownHosts has package-protected access, so we use this trick to avoid parsing of known_hosts file by Bamboo
        //JSch stores keys in format which is different from format Bamboo user provides
        jsch.setKnownHosts(trustedKeysHelper.getCustomAcceptedSshHostKeysFile().getAbsolutePath());
        jsch.setHostKeyRepository(new HostNameNonSensitiveHostKeyRepository(jsch.getHostKeyRepository()));
    }
}
