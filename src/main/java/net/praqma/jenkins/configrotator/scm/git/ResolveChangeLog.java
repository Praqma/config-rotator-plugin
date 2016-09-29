package net.praqma.jenkins.configrotator.scm.git;

import hudson.FilePath;
import hudson.remoting.VirtualChannel;
import net.praqma.jenkins.configrotator.scm.ConfigRotatorChangeLogEntry;
import net.praqma.jenkins.configrotator.scm.ConfigRotatorVersion;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.jenkinsci.remoting.RoleChecker;

public class ResolveChangeLog implements FilePath.FileCallable<ConfigRotatorChangeLogEntry> {

    private static final Logger LOGGER = Logger.getLogger( ResolveChangeLog.class.getName() );
    private String commitId;
    private String name;

    public ResolveChangeLog( String name, String commitId ) {
        this.commitId = commitId;
        this.name = name;
    }

    @Override
    public ConfigRotatorChangeLogEntry invoke( File workspace, VirtualChannel virtualChannel ) throws IOException, InterruptedException {

        File local = new File( workspace, name );
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        List<DiffEntry> diffs = new ArrayList<DiffEntry>();

        //Resources that NEEDS to be closed
        Repository repo = null;
        RevWalk w = null;

        //References
        RevCommit commit = null;
        RevCommit parent = null;

        try {
            repo = builder.setGitDir( new File( local, ".git" ) ).readEnvironment().findGitDir().build();
            w = new RevWalk( repo );
            ObjectId o = repo.resolve( commitId );
            commit = w.parseCommit( o );
            parent = w.parseCommit( commit.getParent( 0 ).getId() );
            LOGGER.fine(String.format("Diffing %s -> %s", commit.getName(), parent.getName() ) );
            DiffFormatter df = new DiffFormatter( DisabledOutputStream.INSTANCE );
            df.setRepository( repo );
            df.setDiffComparator( RawTextComparator.DEFAULT );
            df.setDetectRenames( true );
            diffs = df.scan( parent.getTree(), commit.getTree() );
        } catch (IOException io) {
            throw io;
        } finally {
            if(repo != null) {
                repo.close();
            }
            if(w != null) {
                w.close();
            }
            if(repo != null) {
                repo.close();
            }
        }

        ConfigRotatorChangeLogEntry entry = new ConfigRotatorChangeLogEntry( commit.getFullMessage(), commit.getAuthorIdent().getName(), new ArrayList<ConfigRotatorVersion>());
        for( DiffEntry diff : diffs ) {
            entry.addVersion( new ConfigRotatorVersion( diff.getNewPath(), "", commit.getAuthorIdent().getName() ) );
        }

        return entry;
    }

    @Override
    public void checkRoles(RoleChecker rc) throws SecurityException {
        //NO-OP
    }

}
