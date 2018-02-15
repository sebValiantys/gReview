package com.houghtonassociates.bamboo.plugins;

import com.atlassian.bamboo.author.Author;
import com.atlassian.bamboo.author.AuthorCachingFacade;
import com.atlassian.bamboo.build.fileserver.BuildDirectoryManager;
import com.atlassian.bamboo.commit.*;
import com.atlassian.bamboo.plan.PlanKey;
import com.atlassian.bamboo.repository.RepositoryException;
import com.atlassian.bamboo.utils.i18n.I18nBean;
import com.atlassian.bamboo.v2.build.BuildRepositoryChanges;
import com.atlassian.bamboo.v2.build.BuildRepositoryChangesImpl;
import com.atlassian.bamboo.vcs.configuration.VcsRepositoryData;
import com.houghtonassociates.bamboo.plugins.dao.GerritChangeVO;
import com.houghtonassociates.bamboo.plugins.dao.GerritConfig;
import com.houghtonassociates.bamboo.plugins.dao.jgit.JGitRepository;
import com.opensymphony.xwork2.TextProvider;
import org.apache.log4j.Logger;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author www.valiantys.com
 *         Date : 26/02/2018
 */
public class GerritHelper {

    public static Logger LOG = Logger.getLogger(GerritHelper.class);

    private static final String GIT_COMMIT_ACTION = "/COMMIT_MSG";

    private final BuildDirectoryManager buildDirectoryManager;
    private final VcsRepositoryData vcsRepositoryData;
    private final GerritConfig gerritConfig;
    private final I18nBean i18n;

    public GerritHelper(BuildDirectoryManager buildDirectoryManager,
                        VcsRepositoryData vcsRepositoryData,
                        GerritConfig gerritConfig,
                        I18nBean i18n) throws IOException {
        this.buildDirectoryManager = buildDirectoryManager;
        this.vcsRepositoryData = vcsRepositoryData;
        this.gerritConfig = gerritConfig;
        this.i18n = i18n;
    }

    public BuildRepositoryChanges getBuildChangesFromJGit(PlanKey actualKey, String lastVcsRevisionKey) throws RepositoryException {
        String currentRevision = "";
        PersonIdent authorIdent = null;
        BuildRepositoryChanges buildChanges = null;

        JGitRepository jgitRepo = new JGitRepository();
        jgitRepo.setAccessData(gerritConfig);
        jgitRepo.open(this.getSourceCodeDirectory(actualKey));
        jgitRepo.openSSHTransport();

        currentRevision = jgitRepo.getLatestRevisionForBranch(gerritConfig.getBranch().getName());

        if (!currentRevision.equals(lastVcsRevisionKey)) {
            String author = Author.UNKNOWN_AUTHOR;

            try {
                RevCommit rev = jgitRepo.resolveRev(currentRevision);

                if (rev != null) {
                    authorIdent = rev.getAuthorIdent();
                }
            } catch (RepositoryException e) {
                LOG.debug(String.format("Failed to retrieve author for %s.", currentRevision));
                lastVcsRevisionKey=currentRevision;
            }

            jgitRepo.close();

            if (author.isEmpty() && (authorIdent != null)) {
                author = authorIdent.getName();
            }

            CommitContextImpl.Builder cc = CommitContextImpl.builder();

            cc.author(author);
            cc.comment(i18n.getText("processor.gerrit.messages.build.error.nochanges"));

            if (authorIdent != null) {
                cc.date(authorIdent.getWhen());
            }

            cc.branch(gerritConfig.getBranch().getName());
            cc.changesetId(lastVcsRevisionKey);

            List<CommitContext> commitlist = Collections.singletonList((CommitContext) cc.build());

            buildChanges = new BuildRepositoryChangesImpl(vcsRepositoryData.getId(), lastVcsRevisionKey,
                    commitlist, vcsRepositoryData.getBranch().getVcsBranch());
        } else {
            buildChanges = new BuildRepositoryChangesImpl(vcsRepositoryData.getId(), lastVcsRevisionKey,
                    null, vcsRepositoryData.getBranch().getVcsBranch());
        }

        return buildChanges;
    }

    public CommitImpl convertChangeToCommit(GerritChangeVO change, boolean useLast) {
        CommitImpl commit = new CommitImpl();

        GerritChangeVO.PatchSet patch;

        if (useLast) {
            patch = change.getCurrentPatchSet();
        } else {
            patch = change.getPatchSets().iterator().next();
        }

        commit.setComment(change.getSubject());

        String author = patch.getAuthorName();

        if (author == null || author.isEmpty()) {
            author = change.getOwnerName();
        }

        commit.setAuthor(new AuthorCachingFacade(author));
        commit.setDate(change.getCreatedOn());
        commit.setChangeSetId(patch.getRevision());
        commit.setCreationDate(change.getCreatedOn());
        commit.setLastModificationDate(change.getLastUpdate());

        Set<GerritChangeVO.FileSet> fileSets = patch.getFileSets();

        for (GerritChangeVO.FileSet fileSet : fileSets) {
            if (!fileSet.getFile().equals(GIT_COMMIT_ACTION)) {
                CommitFile file =
                        new CommitFileImpl(patch.getRevision(), fileSet.getFile());
                commit.addFile(file);
            }
        }

        return commit;
    }

    @NotNull
    public File getSourceCodeDirectory(@NotNull PlanKey planKey) throws RepositoryException {
        return new File(this.getWorkingDirectory(), planKey.getKey());
    }

    public File getWorkingDirectory() {
        return this.buildDirectoryManager.getWorkingDirectoryOfCurrentAgent();
    }

}
