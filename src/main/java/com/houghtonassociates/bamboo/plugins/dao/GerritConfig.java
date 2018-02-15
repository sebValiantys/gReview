package com.houghtonassociates.bamboo.plugins.dao;

import com.atlassian.bamboo.plan.branch.VcsBranch;
import com.houghtonassociates.bamboo.plugins.repo.v2.GerritConstants;
import com.sonymobile.tools.gerrit.gerritevents.ssh.Authentication;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.Serializable;

public class GerritConfig implements Serializable {

    private static final long serialVersionUID = 7887858666763224079L;

    private String repositoryUrl = "";
    private String host = "";
    private int port = GerritConstants.REPOSITORY_GERRIT_REPOSITORY_DEFAULT_PORT;
    private String proxy = "";
    private VcsBranch branch;
    private String username = "";
    private String password = "";
    private String userEmail = "";
    private String project = "";
    private File sshKeyFile = null;
    private String workingDirectoryPath = "";
    private String sshKey = "";
    private String sshPassphrase = "";
    private boolean useShallowClones = false;
    private boolean useSubmodules = false;
    private int commandTimeout = 0;
    private boolean verboseLogs = false;

    public static final class Builder {
        private String repositoryUrl;
        private String host = "";
        private int port = GerritConstants.REPOSITORY_GERRIT_REPOSITORY_DEFAULT_PORT;
        private String proxy = "";
        private String username;
        private VcsBranch branch;
        private String password;
        private String project = "";
        private String userEmail = "";
        private File sshKeyFile = null;
        private String workingDirectoryPath = "";
        private String sshKey;
        private String sshPassphrase;
        private boolean useShallowClones;
        private boolean useSubmodules;
        private int commandTimeout;
        private boolean verboseLogs;

        public Builder clone(final GerritConfig gerritConfig) {
            this.repositoryUrl = gerritConfig.repositoryUrl;
            this.host = gerritConfig.host;
            this.port = gerritConfig.port;
            this.proxy = gerritConfig.proxy;
            this.username = gerritConfig.username;
            this.password = gerritConfig.password;
            this.branch= gerritConfig.branch;
            this.userEmail = gerritConfig.userEmail;
            this.project = gerritConfig.project;
            this.sshKeyFile = gerritConfig.sshKeyFile;
            this.workingDirectoryPath = gerritConfig.workingDirectoryPath;
            this.sshKey = gerritConfig.sshKey;
            this.sshPassphrase = gerritConfig.sshPassphrase;
            this.useShallowClones = gerritConfig.useShallowClones;
            this.useSubmodules = gerritConfig.useSubmodules;
            this.commandTimeout = gerritConfig.commandTimeout;
            this.verboseLogs = gerritConfig.verboseLogs;
            return this;
        }

        public Builder repositoryUrl(final String repositoryUrl) {
            this.repositoryUrl = repositoryUrl;
            return this;
        }

        public Builder host(final String host) {
            this.host = host;
            return this;
        }

        public Builder port(final int port) {
            this.port = port;
            return this;
        }

        public Builder proxy(final String proxy) {
            this.proxy = proxy;
            return this;
        }

        public Builder project(final String project) {
            this.project = project;
            return this;
        }

        public Builder branch(final VcsBranch branch) {
            this.branch = branch;
            return this;
        }

        public Builder username(final String username) {
            this.username = username;
            return this;
        }

        public Builder password(final String password) {
            this.password = password;
            return this;
        }

        public Builder userEmail(final String userEmail) {
            this.userEmail = userEmail;
            return this;
        }

        public Builder sshKeyFile(final File sshKeyFile) {
            this.sshKeyFile = sshKeyFile;
            return this;
        }

        public Builder workingDirectoryPath(final String workingDirectoryPath) {
            this.workingDirectoryPath = workingDirectoryPath;
            return this;
        }

        public Builder sshKey(final String sshKey) {
            this.sshKey = sshKey;
            return this;
        }

        public Builder sshPassphrase(final String sshPassphrase) {
            this.sshPassphrase = sshPassphrase;
            return this;
        }

        public Builder useShallowClones(final boolean useShallowClones) {
            this.useShallowClones = useShallowClones;
            return this;
        }

        public Builder useSubmodules(final boolean useSubmodules) {
            this.useSubmodules = useSubmodules;
            return this;
        }

        public Builder commandTimeout(final int commandTimeout) {
            this.commandTimeout = commandTimeout;
            return this;
        }

        public Builder verboseLogs(final boolean verboseLogs) {
            this.verboseLogs = verboseLogs;
            return this;
        }

        public GerritConfig build() {
            GerritConfig data = new GerritConfig();
            data.repositoryUrl = this.repositoryUrl;
            data.host = this.host;
            data.port = this.port;
            data.proxy = this.proxy;
            data.username = this.username;
            data.password = this.password;
            data.userEmail = this.userEmail;
            data.branch = this.branch;
            data.project = this.project;
            data.sshKeyFile = this.sshKeyFile;
            data.workingDirectoryPath = this.workingDirectoryPath;
            data.sshKey = this.sshKey;
            data.sshPassphrase = this.sshPassphrase;
            data.useShallowClones = this.useShallowClones;
            data.useSubmodules = this.useSubmodules;
            data.commandTimeout = this.commandTimeout;
            data.verboseLogs = this.verboseLogs;
            return data;
        }

    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(@NotNull GerritConfig config) {
        return new Builder().clone(config);
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getProxy() {
        return proxy;
    }

    public void setProxy(String proxy) {
        this.proxy = proxy;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public String getProject() {
        return project;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public File getSshKeyFile() {
        return sshKeyFile;
    }

    public void setSshKeyFile(File sshKeyFile) {
        this.sshKeyFile = sshKeyFile;
    }

    public String getWorkingDirectoryPath() {
        return workingDirectoryPath;
    }

    public void setWorkingDirectory(String workingDirectoryPath) {
        this.workingDirectoryPath = workingDirectoryPath;
    }

    public String getSshKey() {
        return sshKey;
    }

    public void setSshKey(String sshKey) {
        this.sshKey = sshKey;
    }

    public String getSshPassphrase() {
        return sshPassphrase;
    }

    public void setSshPassphrase(String sshPassphrase) {
        this.sshPassphrase = sshPassphrase;
    }

    public boolean isUseShallowClones() {
        return useShallowClones;
    }

    public void setUseShallowClones(boolean useShallowClones) {
        this.useShallowClones = useShallowClones;
    }

    public boolean isUseSubmodules() {
        return useSubmodules;
    }

    public void setUseSubmodules(boolean useSubmodules) {
        this.useSubmodules = useSubmodules;
    }

    public int getCommandTimeout() {
        return commandTimeout;
    }

    public void setCommandTimeout(int commandTimeout) {
        this.commandTimeout = commandTimeout;
    }

    public boolean isVerboseLogs() {
        return verboseLogs;
    }

    public void setVerboseLogs(boolean verboseLogs) {
        this.verboseLogs = verboseLogs;
    }

    public Authentication getAuth() {
        return new Authentication(sshKeyFile, username, sshPassphrase);
    }

    public VcsBranch getBranch() {
        return branch;
    }

    public void setBranch(VcsBranch branch) {
        this.branch = branch;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public void setWorkingDirectoryPath(String workingDirectoryPath) {
        this.workingDirectoryPath = workingDirectoryPath;
    }
}
