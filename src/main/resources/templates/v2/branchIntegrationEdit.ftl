[#assign useShallowClones = stack.findValue('repository.gerrit.useShallowClones')!false/]

[#if useShallowClones]
    [@ui.messageBox type='info']
        [@s.text name='repository.gerrit.messages.branchIntegration.shallowClonesWillBeDisabled'/]
    [/@ui.messageBox]
[/#if]
