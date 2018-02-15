package com.houghtonassociates.bamboo.plugins.repo.v2.configurator;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.bamboo.vcs.configuration.VcsChangeDetectionOptions;
import com.atlassian.bamboo.vcs.configurator.VcsChangeDetectionOptionsConfigurator;
import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class DefaultChangeDetectionOptionsConfigurator implements VcsChangeDetectionOptionsConfigurator {
    private final boolean quietPeriodSupported;
    private final boolean commitIsolationSupported;

    public DefaultChangeDetectionOptionsConfigurator(final boolean quietPeriodSupported,
                                                     final boolean commitIsolationSupported) {
        this.quietPeriodSupported = quietPeriodSupported;
        this.commitIsolationSupported = commitIsolationSupported;
    }

    public DefaultChangeDetectionOptionsConfigurator() {
        this(true, false);
    }

    @Override
    public void populateContextForCreate(@NotNull final Map<String, Object> context) {

    }

    @Override
    public void populateContextForEdit(@NotNull final Map<String, Object> context, @NotNull final VcsChangeDetectionOptions triggerDefinition) {

    }

    @Override
    public void populateContextForView(@NotNull final Map<String, Object> context, @NotNull final VcsChangeDetectionOptions triggerDefinition) {

    }

    @Override
    public void validate(@NotNull final ActionParametersMap actionParametersMap, @NotNull final ErrorCollection errorCollection) {

    }

    @NotNull
    @Override
    public Map<String, String> generateConfigMap(@NotNull final ActionParametersMap actionParametersMap,
                                                 @Nullable final VcsChangeDetectionOptions previousTriggerDefinition) {
        return Maps.newHashMap();
    }

    @Override
    public boolean isQuietPeriodSupported() {
        return quietPeriodSupported;
    }

    @Override
    public boolean isCommitIsolationSupported() {
        return commitIsolationSupported;
    }
}
