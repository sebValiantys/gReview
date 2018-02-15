package com.houghtonassociates.bamboo.plugins.repo.v2.converter;

import com.atlassian.bamboo.utils.ConfigUtils;
import com.atlassian.bamboo.vcs.configuration.VcsRepositoryData;
import com.atlassian.bamboo.vcs.configurator.VcsChangeDetectionOptionsConfigurator;
import com.atlassian.bamboo.vcs.converter.Repository2VcsTypeConverter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.houghtonassociates.bamboo.plugins.repo.v2.GerritConstants.*;

public class GerritConfigurationConverter implements Repository2VcsTypeConverter {
    private static final Logger log = Logger.getLogger(GerritConfigurationConverter.class);

    private static final List<String> KEYS_TO_SKIP = ImmutableList.of(ConfigUtils.BAMBOO_DELIMITER_PARSING_DISABLED,
            "[@xml:space]",
            REPOSITORY_GERRIT_BRANCH,
            VcsChangeDetectionOptionsConfigurator.QUIET_PERIOD_ENABLED_CFG_KEY,
            VcsChangeDetectionOptionsConfigurator.QUIET_PERIOD_CFG_KEY,
            VcsChangeDetectionOptionsConfigurator.QUIET_PERIOD_MAX_RETRIES_CFG_KEY,
            VcsChangeDetectionOptionsConfigurator.CHANGESET_FILTER_REGEX_CFG_KEY,
            VcsChangeDetectionOptionsConfigurator.COMMIT_ISOLATION_OPTION_CFG_KEY,
            VcsChangeDetectionOptionsConfigurator.FILTER_PATTERN_OPTION_CFG_KEY,
            VcsChangeDetectionOptionsConfigurator.FILTER_PATTERN_REGEX_CFG_KEY
    );

    @Override
    public String acceptedRepositoryPluginKey() {
        return GERRIT_MODULE_KEY;
    }

    @Override
    public String producedVcsTypePluginKey() {
        return VCS_MODULE_KEY;
    }

    @NotNull
    @Override
    public Map<String, String> extractServerConfiguration(@NotNull final HierarchicalConfiguration repositoryConfiguration) {
        final Map<String, String> result = new HashMap<>();
        ConfigUtils.asMap(repositoryConfiguration, null)
                .entrySet()
                .stream()
                .filter(e -> !KEYS_TO_SKIP.contains(e.getKey()))
                .forEach(e -> result.put(e.getKey(), e.getValue()));
        return result;
    }

    @Nullable
    @Override
    public Map<String, String> extractBranchConfiguration(@NotNull final HierarchicalConfiguration repositoryConfiguration) {
        return ImmutableMap.of(REPOSITORY_GERRIT_BRANCH, repositoryConfiguration.getString(REPOSITORY_GERRIT_BRANCH));
    }

    @Override
    public HierarchicalConfiguration asLegacyData(@NotNull final VcsRepositoryData vcsRepositoryData) {
        final HierarchicalConfiguration configuration = ConfigUtils.newConfiguration();

        Stream.concat(vcsRepositoryData.getVcsLocation().getConfiguration().entrySet().stream(),
                vcsRepositoryData.getBranch().getConfiguration().entrySet().stream())
                .forEach(e -> configuration.setProperty(e.getKey(), e.getValue()));

        return configuration;
    }
}
