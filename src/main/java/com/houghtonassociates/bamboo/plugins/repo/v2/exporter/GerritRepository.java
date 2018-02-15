package com.houghtonassociates.bamboo.plugins.repo.v2.exporter;

import com.atlassian.bamboo.specs.api.builders.repository.VcsChangeDetection;
import com.atlassian.bamboo.specs.api.builders.repository.VcsRepository;
import com.atlassian.bamboo.specs.api.model.repository.VcsChangeDetectionProperties;
import com.atlassian.bamboo.specs.api.util.EntityPropertiesBuilders;
import com.atlassian.bamboo.specs.api.validators.common.ImporterUtils;
import com.atlassian.bamboo.specs.model.repository.git.AuthenticationProperties;
import com.atlassian.bamboo.specs.model.repository.git.SshPrivateKeyAuthenticationProperties;
import com.houghtonassociates.bamboo.plugins.repo.v2.GerritRepositoryProperties;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * @author www.valiantys.com
 * Date : 16/02/2018
 */
public class GerritRepository extends VcsRepository<GerritRepository, GerritRepositoryProperties> {

    private String hostName;
    private int port;
    private String project;
    private String defaultBranch;
    private String branch;
    private AuthenticationProperties authentication;
    private boolean useShallowClones;
    private boolean useSubmodules;
    private Duration commandTimeout = Duration.ofMinutes(180L);
    private boolean verboseLogs;
//    private boolean useLfs;
    private VcsChangeDetectionProperties vcsChangeDetection;

    public GerritRepository() {}

    public GerritRepository hostName(@NotNull String hostName) {
        ImporterUtils.checkNotBlank("hostName", hostName);
        this.hostName = hostName;
        return this;
    }

    public GerritRepository project(@NotNull String project) {
        ImporterUtils.checkNotBlank("project", project);
        this.project = project;
        return this;
    }

    public GerritRepository port(int port) {
        this.port = port;
        return this;
    }

    public GerritRepository defaultBranch(@NotNull String branch) {
        ImporterUtils.checkNotNull("defaultBranch", branch);
        this.defaultBranch = branch;
        return this;
    }

    public GerritRepository branch(@NotNull String branch) {
        ImporterUtils.checkNotNull("branch", branch);
        this.branch = branch;
        return this;
    }

    public GerritRepository authentication(@NotNull String sshPrivateKeyAuthentication, String passphrase) {
        ImporterUtils.checkNotNull("sshPrivateKeyAuthentication", sshPrivateKeyAuthentication);
        this.authentication = new SshPrivateKeyAuthenticationProperties(sshPrivateKeyAuthentication, passphrase);
        return this;
    }

    public GerritRepository shallowClonesEnabled(boolean useShallowClones) {
        this.useShallowClones = useShallowClones;
        return this;
    }

    public GerritRepository submodulesEnabled(boolean useSubmodules) {
        this.useSubmodules = useSubmodules;
        return this;
    }

    public GerritRepository commandTimeout(Duration commandTimeout) {
        this.commandTimeout = commandTimeout;
        return this;
    }

    public GerritRepository verboseLogs(boolean verboseLogs) {
        this.verboseLogs = verboseLogs;
        return this;
    }

//    public GerritRepository lfsEnabled(boolean useLfs) {
//        this.useLfs = useLfs;
//        return this;
//    }

    public GerritRepository defaultChangeDetection() {
        this.vcsChangeDetection = null;
        return this;
    }

    public GerritRepository changeDetection(@NotNull VcsChangeDetection vcsChangeDetection) {
        ImporterUtils.checkNotNull("vcsChangeDetection", vcsChangeDetection);
        this.vcsChangeDetection = (VcsChangeDetectionProperties)EntityPropertiesBuilders.build(vcsChangeDetection);
        return this;
    }


    @Override
    protected GerritRepositoryProperties build() {
        return new GerritRepositoryProperties(this.name, this.oid, this.description, this.parent, this.repositoryViewer,
                this.hostName, this.port, this.project, this.branch, this.defaultBranch, this.authentication,
                this.vcsChangeDetection, this.useShallowClones, this.useSubmodules, this.commandTimeout, this.verboseLogs
                /*, this.useLfs*/);
    }
}
