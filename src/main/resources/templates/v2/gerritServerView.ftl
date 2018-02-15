[#--
 ~ Copyright 2012 Houghton Associates
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~    http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
--]

[#-- @ftlvariable name="buildConfiguration" type="com.atlassian.bamboo.ww2.actions.build.admin.create.BuildConfiguration" --]
[#-- @ftlvariable name="plan" type="com.atlassian.bamboo.plan.Plan" --]
[#-- @ftlvariable name="repository" type="com.houghtonassociates.bamboo.plugins.repo.v2.GerritRepositoryManager" --]

[@ui.bambooSection]
    [@ww.textfield labelKey='repository.gerrit.hostname' name='repository.gerrit.hostname' disabled='true' /]
    [@ww.textfield labelKey='repository.gerrit.port' name='repository.gerrit.port' disabled='true' /]
    [@ww.textfield labelKey='repository.gerrit.project' name='repository.gerrit.project' disabled='true' /]
    [@ww.textfield labelKey='repository.gerrit.username' name='repository.gerrit.username' disabled='true' /]
    [#--[@ww.checkbox labelKey='repository.passphrase.change' toggle='true' name='temporary.gerrit.ssh.passphrase.change' disabled = true/]--]
[/@ui.bambooSection]