package com.houghtonassociates.bamboo.plugins.repo.v2;

import com.atlassian.bamboo.specs.api.codegen.annotations.CodeGenerator;
import com.atlassian.bamboo.specs.api.codegen.annotations.Setter;
import com.atlassian.bamboo.specs.api.exceptions.PropertiesValidationException;
import com.atlassian.bamboo.specs.api.model.AtlassianModuleProperties;
import com.atlassian.bamboo.specs.api.model.BambooOidProperties;
import com.atlassian.bamboo.specs.api.model.repository.VcsChangeDetectionProperties;
import com.atlassian.bamboo.specs.api.model.repository.VcsRepositoryProperties;
import com.atlassian.bamboo.specs.api.model.repository.viewer.VcsRepositoryViewerProperties;
import com.atlassian.bamboo.specs.api.validators.common.ImporterUtils;
import com.atlassian.bamboo.specs.api.validators.common.ValidationContext;
import com.atlassian.bamboo.specs.api.validators.common.ValidationProblem;
import com.atlassian.bamboo.specs.api.validators.common.ValidationUtils;
import com.atlassian.bamboo.specs.codegen.emitters.repository.GitAuthenticationEmitter;
import com.atlassian.bamboo.specs.model.repository.git.AuthenticationProperties;
import com.atlassian.bamboo.specs.model.repository.git.SshPrivateKeyAuthenticationProperties;
import com.atlassian.bamboo.specs.model.repository.git.UserPasswordAuthenticationProperties;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.concurrent.Immutable;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author www.valiantys.com
 * Date : 16/02/2018
 */
@Immutable
public class GerritRepositoryProperties extends VcsRepositoryProperties {
    private static final Set<String> SUPPORTED_SCHEMES = (Set) Stream.of(new String[]{"http", "https", "ssh", null}).collect(Collectors.toSet());
    private static final AtlassianModuleProperties ATLASSIAN_PLUGIN = new AtlassianModuleProperties(GerritConstants.VCS_MODULE_KEY);
    private final String hostname;
    private int port = GerritConstants.REPOSITORY_GERRIT_REPOSITORY_DEFAULT_PORT;
    private final String project;
    private String branch;
    private String defaultBranch;
    @CodeGenerator(GitAuthenticationEmitter.class)
    private final AuthenticationProperties authenticationProperties;
    @Setter("shallowClonesEnabled")
    private final boolean useShallowClones;
    @Setter("submodulesEnabled")
    private final boolean useSubmodules;
    private final Duration commandTimeout;
    private final boolean verboseLogs;
//    @Setter("lfsEnabled")
//    private final boolean useLfs;
    private VcsChangeDetectionProperties vcsChangeDetection;

    private GerritRepositoryProperties() {
        this.hostname = null;
        this.project = null;
        this.branch = null;
        this.defaultBranch = "master";
        this.authenticationProperties = null;
        this.useShallowClones = false;
        this.useSubmodules = false;
        this.commandTimeout = Duration.ofMinutes(180L);
        this.verboseLogs = false;
//        this.useLfs = false;
    }

    public GerritRepositoryProperties(@Nullable String name, @Nullable BambooOidProperties oid,
                                      @Nullable String description,
                                      @Nullable String parent,
                                      @Nullable VcsRepositoryViewerProperties repositoryViewerProperties,
                                      @Nullable String hostname,
                                      @Nullable int port,
                                      @Nullable String project,
                                      @Nullable String branch,
                                      @Nullable String defaultBranch,
                                      @Nullable AuthenticationProperties authenticationProperties,
                                      @Nullable VcsChangeDetectionProperties vcsChangeDetection,
                                      boolean useShallowClones,
                                      boolean useSubmodules,
                                      @NotNull Duration commandTimeout,
                                      boolean verboseLogs /*,boolean useLfs*/)
            throws PropertiesValidationException {
        super(name, oid, description, parent, repositoryViewerProperties);
        this.hostname = hostname;
        this.port = port;
        this.project = project;
        this.branch = branch;
        this.defaultBranch = defaultBranch;
        this.authenticationProperties = authenticationProperties;
        this.vcsChangeDetection = vcsChangeDetection;
        this.useShallowClones = useShallowClones;
        this.useSubmodules = useSubmodules;
        this.commandTimeout = commandTimeout;
        this.verboseLogs = verboseLogs;
//        this.useLfs = useLfs;
        if(!this.hasParent() && StringUtils.isBlank(this.branch)) {
            this.branch = this.defaultBranch;
        }

        this.validate();
    }

    @Nullable
    public AtlassianModuleProperties getAtlassianPlugin() {
        return ATLASSIAN_PLUGIN;
    }

    @Nullable
    public String getHostname() {
        return this.hostname;
    }

    @Nullable
    public String getProject() {
        return this.project;
    }

    @Nullable
    public int getPort() {
        return this.port;
    }

    @Nullable
    public String getBranch() {
        return this.branch;
    }

    @Nullable
    public String getDefaultBranch() {
        return this.defaultBranch;
    }

    @Nullable
    public AuthenticationProperties getAuthenticationProperties() {
        return this.authenticationProperties;
    }

    public boolean isUseShallowClones() {
        return this.useShallowClones;
    }

//    public boolean isUseRemoteAgentCache() {
//        return this.useRemoteAgentCache;
//    }

    public boolean isUseSubmodules() {
        return this.useSubmodules;
    }

    public Duration getCommandTimeout() {
        return this.commandTimeout;
    }

    public boolean isVerboseLogs() {
        return this.verboseLogs;
    }

//    public boolean isUseLfs() {
//        return this.useLfs;
//    }

    @Nullable
    public VcsChangeDetectionProperties getVcsChangeDetection() {
        return this.vcsChangeDetection;
    }

    public void validate() {
        super.validate();
        ValidationContext context = ValidationContext.of("Gerrit repository");
        ArrayList errors = new ArrayList();
        if(!this.hasParent()) {
            ImporterUtils.checkRequired(context.with("HOSTNAME"), this.hostname);
        }

        if(this.hostname != null && !ValidationUtils.containsBambooVariable(this.hostname)) {
            ValidationUtils.validateNotContainsRelaxedXssRelatedCharacters(context.with("HOSTNAME"), this.hostname).ifPresent(errors::add);
            ValidationUtils.validateNotContainsShellInjectionRelatedCharacters(context.with("HOSTNAME"), this.hostname).ifPresent(errors::add);
            this.checkHostName(context.with("HOSTNAME")).ifPresent(errors::add);
        }

        if(this.branch != null) {
            ValidationUtils.validateNotContainsShellInjectionRelatedCharacters(context.with("Branch name"), this.branch).ifPresent(errors::add);
        }

        if(this.vcsChangeDetection != null && !this.vcsChangeDetection.getConfiguration().isEmpty()) {
            errors.add(new ValidationProblem(context.with("Change detection"), "Git repository cannot have any extra change detection configuration."));
        }

        ImporterUtils.checkNoErrors(errors);
    }

    private Optional<ValidationProblem> checkHostName(@NotNull ValidationContext validationContext) {
        if(this.hostname == null) {
            return Optional.empty();
        } else {
            try {
                URI e = new URI(this.hostname);
                String scheme = e.getScheme();
                if(!SUPPORTED_SCHEMES.contains(e.getScheme())) {
                    return Optional.of(new ValidationProblem(validationContext, "scheme \'%s\' is not supported - supported schemes are: %s", new Object[]{scheme, String.join(", ", SUPPORTED_SCHEMES)}));
                }

                String userInfo = e.getUserInfo();
                if(StringUtils.isNotBlank(userInfo)) {
                    int d = userInfo.indexOf(58);
                    String user = d < 0?userInfo:userInfo.substring(0, d);
                    String pass = d < 0?null:userInfo.substring(d + 1);
                    boolean duplicateUsername = !StringUtils.isBlank(user) && this.authenticationProperties instanceof UserPasswordAuthenticationProperties && StringUtils.isNotBlank(((UserPasswordAuthenticationProperties)this.authenticationProperties).getUsername());
                    boolean duplicatePassword = !StringUtils.isBlank(pass) && this.authenticationProperties instanceof UserPasswordAuthenticationProperties && StringUtils.isNotBlank(((UserPasswordAuthenticationProperties)this.authenticationProperties).getPassword());
                    if(duplicateUsername) {
                        return Optional.of(new ValidationProblem(validationContext, "Duplicate username" + (duplicatePassword?" & password":"")));
                    }

                    if(duplicatePassword) {
                        return Optional.of(new ValidationProblem(validationContext, "Duplicate password"));
                    }
                }

                if(this.authenticationProperties instanceof SshPrivateKeyAuthenticationProperties && ("http".equals(e.getScheme()) || "https".equals(e.getScheme()))) {
                    return Optional.of(new ValidationProblem(validationContext, "Ssh authentication not supported with " + e.getScheme()));
                }
            } catch (URISyntaxException var10) {
                if(!this.hostname.startsWith("\\\\") && this.hostname.contains("://")) {
                    return Optional.of(new ValidationProblem(validationContext, String.format("Malformed URL: %s", new Object[]{this.hostname})));
                }

                return Optional.empty();
            }

            return Optional.empty();
        }
    }

    /*
     * TODO : check if all params are taken into account
     */
    public boolean equals(Object o) {
        if(this == o) {
            return true;
        } else if(o != null && this.getClass() == o.getClass()) {
            if(!super.equals(o)) {
                return false;
            } else {
                GerritRepositoryProperties that = (GerritRepositoryProperties)o;
                return this.isUseShallowClones() == that.isUseShallowClones() && this.isUseSubmodules() == that.isUseSubmodules()
                        && this.isVerboseLogs() == that.isVerboseLogs() /*&& this.isUseLfs() == that.isUseLfs()*/
                        && Objects.equals(this.getHostname(), that.getHostname()) && this.getPort() == that.getPort() && Objects.equals(this.getProject(), that.getProject())
                        && Objects.equals(this.getBranch(), that.getBranch()) && Objects.equals(this.getAuthenticationProperties(), that.getAuthenticationProperties())
                        && Objects.equals(this.getCommandTimeout(), that.getCommandTimeout()) && Objects.equals(this.getVcsChangeDetection(), that.getVcsChangeDetection());
            }
        } else {
            return false;
        }
    }

    /*
     * TODO : check if all params are taken into account
     */
    public int hashCode() {
        return Objects.hash(new Object[]{Integer.valueOf(super.hashCode()), this.getHostname(), Integer.valueOf(this.getPort()),
                this.getProject(), this.getBranch(), this.getAuthenticationProperties(), Boolean.valueOf(this.isUseShallowClones()),
                Boolean.valueOf(this.isUseSubmodules()), this.getCommandTimeout(), Boolean.valueOf(this.isVerboseLogs()), /*Boolean.valueOf(isUseLfs()),*/
                this.getVcsChangeDetection()});
    }
}
