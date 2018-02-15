[#-- @ftlvariable name="repository" type="com.atlassian.bamboo.plugins.git.GitRepository" --]
<table class="aui">
  <tbody>
    <tr>
      <td>[@ww.select name='repository.gerrit.default.branch' id='repositoryGerritDefaultBranch' label='Default Branch' maxlength='50' size='1' list="{'master','All branches','Custom'}" disabled=true/]</td>
      <td>[@ww.textfield name='repository.gerrit.custom.branch' id='repositoryGerritCustomBranch' labelKey='Custom Branch' disabled='true'/]</td>
    </tr>
  </tbody>
</table>