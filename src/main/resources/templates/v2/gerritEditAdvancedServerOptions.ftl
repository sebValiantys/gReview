[@s.checkbox labelKey='repository.git.useShallowClones' toggle=true name='repository.git.useShallowClones' /]
[#if (plan.buildDefinition.branchIntegrationConfiguration.enabled)!false ]
    [@ui.bambooSection dependsOn='repository.gerrit.useShallowClones']
        [@ui.messageBox type='info' titleKey='repository.gerrit.messages.branchIntegration.shallowClonesWillBeDisabled' /]
    [/@ui.bambooSection]
[/#if]

[#--[@s.checkbox labelKey='repository.gerrit.useRemoteAgentCache' toggle=false name='repository.git.useRemoteAgentCache' /]--]

[@s.checkbox labelKey='repository.gerrit.useSubmodules' name='repository.gerrit.useSubmodules' /]
[@s.textfield labelKey='repository.gerrit.commandTimeout' name='repository.gerrit.commandTimeout' /]
[@s.checkbox labelKey='repository.gerrit.verbose.logs' name='repository.gerrit.verbose.logs' /]
[#--[@s.checkbox labelKey='repository.git.fetch.whole.repository' name='repository.git.fetch.whole.repository' /]--]
[#--[@s.checkbox labelKey='repository.git.lfs' name='repository.git.lfs' /]--]