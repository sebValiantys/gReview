package com.houghtonassociates.bamboo.plugins.repo.v2;

import com.atlassian.bamboo.plan.branch.VcsBranch;
import com.atlassian.bamboo.plan.branch.VcsBranchImpl;
import com.atlassian.bamboo.utils.SystemProperty;

/**
 * @author www.valiantys.com
 * Date : 16/02/2018
 */
public class GerritConstants {
    public static final String REPOSITORY_URL = "repositoryUrl";
    public static final String REPOSITORY_USERNAME = "username";

    public static final String PLUGIN_KEY = "com.houghtonassociates.bamboo.plugins.gReview";
    public static final String GERRIT_MODULE_KEY = "com.houghtonassociates.bamboo.plugins.gReview:gerrit";
    public static final String VCS_MODULE_KEY = "com.houghtonassociates.bamboo.plugins.gReview:gerritv2";

    @Deprecated
    public static final String REPOSITORY_GERRIT_PLAN_KEY = "planKey";

    public static final String REPOSITORY_GERRIT_PROJECT_KEY = "projectKey";
    public static final String REPOSITORY_GERRIT_PROJECT_NAME = "projectName";
    public static final String REPOSITORY_GERRIT_CHAIN_KEY = "chainKey";
    public static final String REPOSITORY_GERRIT_CHAIN_NAME = "chainName";
    public static final String REPOSITORY_GERRIT_CHAIN_DESC =
            "chainDescription";

    public static final String REPOSITORY_GERRIT_REPOSITORY_HOSTNAME =
            "repository.gerrit.hostname";
    public static final String REPOSITORY_GERRIT_REPOSITORY_PORT =
            "repository.gerrit.port";
    public static final int REPOSITORY_GERRIT_REPOSITORY_DEFAULT_PORT = 29418;
    public static final String REPOSITORY_GERRIT_PROJECT =
            "repository.gerrit.project";

    public static final String REPOSITORY_GERRIT_BRANCH =
            "repository.gerrit.branch";
    public static String DEFAULT_BRANCH = "master";
    public static String ALL_BRANCHES = "All branches";
    public static String CUSTOM = "Custom";
    public static final String REPOSITORY_GERRIT_DEFAULT_BRANCH =
            "repository.gerrit.default.branch";
    public static final String REPOSITORY_GERRIT_CUSTOM_BRANCH =
            "repository.gerrit.custom.branch";
    public static final VcsBranch ALL_BRANCH = new VcsBranchImpl("All branches");
    public static final VcsBranch MASTER_BRANCH = new VcsBranchImpl("master");

    public static final String REPOSITORY_GERRIT_USERNAME =
            "repository.gerrit.username";
    public static final String REPOSITORY_GERRIT_EMAIL =
            "repository.gerrit.email";
    public static final String SHARED_CREDENTIALS = "SHARED_CREDENTIALS";
    public static final String REPOSITORY_GIT_SHAREDCREDENTIALS_ID =
            "repository.gerrit.sharedCrendentials";
    public static final String REPOSITORY_GIT_AUTHENTICATION_TYPE =
            "repository.gerrit.authenticationType";
    public static final String REPOSITORY_GERRIT_SSH_KEY =
            "repository.gerrit.ssh.key";
    public static final String REPOSITORY_GERRIT_SSH_KEY_FILE =
            "repository.gerrit.ssh.keyfile";
    public static final String REPOSITORY_GERRIT_CONFIG_DIR =
            "repository.gerrit.config.dir";
    public static final String REPOSITORY_GERRIT_SSH_PASSPHRASE =
            "repository.gerrit.ssh.passphrase";
    public static final String TEMPORARY_GERRIT_SSH_PASSPHRASE =
            "temporary.gerrit.ssh.passphrase";
    public static final String TEMPORARY_GERRIT_SSH_PASSPHRASE_CHANGE =
            "temporary.gerrit.ssh.passphrase.change";
    public static final String TEMPORARY_GERRIT_SSH_KEY_FROM_FILE =
            "temporary.gerrit.ssh.keyfile";
    public static final String TEMPORARY_GERRIT_SSH_KEY_CHANGE =
            "temporary.gerrit.ssh.key.change";

    public static final String REPOSITORY_GERRIT_USE_SHALLOW_CLONES =
            "repository.gerrit.useShallowClones";
    public static final String REPOSITORY_GERRIT_USE_SUBMODULES =
            "repository.gerrit.useSubmodules";
    public static final String REPOSITORY_GERRIT_COMMAND_TIMEOUT =
            "repository.gerrit.commandTimeout";
    public static final String REPOSITORY_GERRIT_VERBOSE_LOGS =
            "repository.gerrit.verbose.logs";
    public static final int DEFAULT_COMMAND_TIMEOUT_IN_MINUTES = 180;

    public static final String GIT_COMMIT_ACTION = "/COMMIT_MSG";

    protected static boolean USE_SHALLOW_CLONES = new SystemProperty(false,
            "atlassian.bamboo.git.useShallowClones",
            "ATLASSIAN_BAMBOO_GIT_USE_SHALLOW_CLONES").getValue(true);

    public static final String REPOSITORY_GERRIT_CHANGE_ID =
            "repository.gerrit.change.id";
    public static final String REPOSITORY_GERRIT_CHANGE_NUMBER =
            "repository.gerrit.change.number";
    public static final String REPOSITORY_GERRIT_REVISION_NUMBER =
            "repository.gerrit.revision.number";
}
