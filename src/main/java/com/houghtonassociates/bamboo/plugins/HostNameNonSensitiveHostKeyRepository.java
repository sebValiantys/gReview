package com.houghtonassociates.bamboo.plugins;

import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.UserInfo;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of {@link HostKeyRepository} which uses JSch {@link com.jcraft.jsch.KnownHosts} as delegate, but
 * ignores host name to be consistent with command line clients behaviour.
 */
class HostNameNonSensitiveHostKeyRepository implements HostKeyRepository {
    private static final Logger log = Logger.getLogger(HostNameNonSensitiveHostKeyRepository.class);

    private final Set<String> knownKeys = new HashSet<>();
    private final HostKeyRepository delegate;

    HostNameNonSensitiveHostKeyRepository(@NotNull final HostKeyRepository delegate) {
        this.delegate = delegate;
        for (HostKey hostKey : delegate.getHostKey()) {
            knownKeys.add(hostKey.getKey());
        }
    }

    @Override
    public int check(String host, byte[] key) {
        try {
            HostKey hostKey = new HostKey(host, key);
            if (knownKeys.contains(hostKey.getKey())) {
                return OK;
            }
        } catch (JSchException e) {
            log.error(e.getMessage(), e);
        }
        return NOT_INCLUDED;
    }

    @Override
    public void add(HostKey hostkey, UserInfo ui) {
        delegate.add(hostkey, ui);
    }

    @Override
    public void remove(String host, String type) {
        delegate.remove(host, type);
    }

    @Override
    public void remove(String host, String type, byte[] key) {
        delegate.remove(host, type, key);
    }

    @Override
    public String getKnownHostsRepositoryID() {
        return "known_hosts";
    }

    @Override
    public HostKey[] getHostKey() {
        return delegate.getHostKey();
    }

    @Override
    public HostKey[] getHostKey(String host, String type) {
        return delegate.getHostKey(host, type);
    }
}
