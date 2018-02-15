package com.houghtonassociates.bamboo.plugins.repo.v2.configurator;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.plan.branch.VcsBranch;
import com.atlassian.bamboo.plan.branch.VcsBranchImpl;
import com.atlassian.bamboo.utils.BambooFieldValidate;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.bamboo.vcs.configuration.VcsBranchDefinition;
import com.atlassian.bamboo.vcs.configurator.VcsBranchConfigurator;
import com.atlassian.sal.api.message.I18nResolver;
import com.google.common.collect.Maps;
import com.houghtonassociates.bamboo.plugins.repo.v2.GerritConstants;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class GerritBranchConfigurator implements VcsBranchConfigurator {
    private I18nResolver i18nResolver;
    private CustomVariableContext customVariableContext;

    public GerritBranchConfigurator(I18nResolver i18nResolver, CustomVariableContext customVariableContext) {
        this.i18nResolver = i18nResolver;
        this.customVariableContext = customVariableContext;
    }

    @Override
    public void populateContextForCreate(@NotNull final Map<String, Object> context) {
    }

    @Override
    public void populateContextForEdit(@NotNull final Map<String, Object> context, @NotNull final VcsBranchDefinition branchDefinition) {
        populateContextCommon(context, branchDefinition);
    }

    @Override
    public void populateContextForView(@NotNull final Map<String, Object> context, @NotNull final VcsBranchDefinition branchDefinition) {
        populateContextCommon(context, branchDefinition);
    }

    private void populateContextCommon(@NotNull final Map<String, Object> context, @NotNull final VcsBranchDefinition vcsLocationDefinition) {
        final Map<String, String> cfg = vcsLocationDefinition.getConfiguration();
        context.put(GerritConstants.REPOSITORY_GERRIT_BRANCH, cfg.get(GerritConstants.REPOSITORY_GERRIT_BRANCH));
    }

    private String substituteString(@Nullable final String stringWithValuesToSubstitute) {
        return customVariableContext.substituteString(stringWithValuesToSubstitute);
    }

    public void validate(@NotNull final ActionParametersMap actionParametersMap, @NotNull final ErrorCollection errorCollection) {
        BambooFieldValidate.findFieldShellInjectionViolation(errorCollection, i18nResolver, GerritConstants.REPOSITORY_GERRIT_BRANCH,
                substituteString(actionParametersMap.getString(GerritConstants.REPOSITORY_GERRIT_BRANCH)));
    }

    @NotNull
    @Override
    public Map<String, String> generateConfigMap(@NotNull final ActionParametersMap actionParametersMap,
                                                 @Nullable final VcsBranchDefinition previousBranchDefinition,
                                                 @NotNull Map<String, String> locationConfiguration) {
        final Map<String, String> cfgMap = Maps.newHashMap();
        String branchType = actionParametersMap.getString(GerritConstants.REPOSITORY_GERRIT_DEFAULT_BRANCH);
        if (branchType.equals(GerritConstants.CUSTOM)) {
            cfgMap.put(GerritConstants.REPOSITORY_GERRIT_BRANCH, actionParametersMap.getString(GerritConstants.REPOSITORY_GERRIT_CUSTOM_BRANCH));
        } else {
            cfgMap.put(GerritConstants.REPOSITORY_GERRIT_BRANCH, branchType);
        }

        return cfgMap;
    }

    @NotNull
    @Override
    public VcsBranch getVcsBranchFromConfig(@NotNull final Map<String, String> configMap) {
        return new VcsBranchImpl(StringUtils.defaultIfBlank(configMap.get(GerritConstants.REPOSITORY_GERRIT_BRANCH), GerritConstants.DEFAULT_BRANCH));
    }

    @NotNull
    @Override
    public Map<String, String> setVcsBranchInConfig(@NotNull final Map<String, String> configMap, @NotNull final VcsBranch vcsBranch) {
        configMap.put(GerritConstants.REPOSITORY_GERRIT_BRANCH, vcsBranch.getName());
        return configMap;
    }

    @NotNull
    @Override
    public VcsBranch createVcsBranchFromName(@NotNull String name) {
        return new VcsBranchImpl(name);
    }

    public void setI18nResolver(final I18nResolver i18nResolver) {
        this.i18nResolver = i18nResolver;
    }

    public void setCustomVariableContext(final CustomVariableContext customVariableContext) {
        this.customVariableContext = customVariableContext;
    }
}
