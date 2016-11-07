package net.praqma.jenkins.configrotator.scm.git;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import org.jenkinsci.plugins.gitclient.GitClient;
import org.jenkinsci.remoting.RoleChecker;

public class ResolveNextCommit implements FilePath.FileCallable<RevCommit> {

    private String commitId;
    private String name;
    private String branch = "git";
    private String repoUrl;
    private String credentials;

    private static final Logger LOGGER = Logger.getLogger( ResolveNextCommit.class.getName() );

    public ResolveNextCommit(String name, String commitId, String branch, String repoUrl) {
        this.commitId = commitId;
        this.name = name;
        this.branch = branch;
        this.repoUrl = repoUrl;
    }

    @Deprecated
    public ResolveNextCommit( String name, String commitId ) {
        this.commitId = commitId;
        this.name = name;
    }

    @Override
    public RevCommit invoke( File workspace, VirtualChannel virtualChannel ) throws IOException, InterruptedException {
        //Resources
        Repository repo = null;
        org.eclipse.jgit.api.Git git = null;
        RevWalk w = null;
        RevCommit next = null;

        try {

            File local = new File( workspace, name );

            if(local.mkdirs()) {
                LOGGER.fine("Created workdir for component "+name);
            }

            GitClient gc = org.jenkinsci.plugins.gitclient.Git.with(TaskListener.NULL, EnvVars.getRemote(virtualChannel)).using("git").in(local).getClient();
            if(!gc.hasGitRepo()) {
                LOGGER.fine("Cloning "+repoUrl);
                gc.clone_().url(repoUrl).execute();
                LOGGER.fine(repo + " cloned sucessfully");
            }

            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            LOGGER.fine( "Initializing repo" );
            repo = builder.setGitDir( new File( local, ".git" ) ).readEnvironment().findGitDir().build();
            git = new org.eclipse.jgit.api.Git( repo );

            LOGGER.fine( String.format( "Updating to %s", branch ) );
            git.fetch().call();

            w = new RevWalk( repo );

            ObjectId ohead = repo.resolve( "refs/remotes/origin/"+branch );
            ObjectId ostart = repo.resolve( commitId );
            RevCommit commithead = w.parseCommit( ohead );
            RevCommit commit = w.parseCommit( ostart );

            LOGGER.fine( String.format ("Commit start: %s", commitId ) );

            w.markStart( commithead );


            for( RevCommit c : w ) {
                if( c != null && c.equals(commit) ) {
                    break;
                }

                if( c == null ) {
                    break;
                }

                if( c.getParentCount() > 1 ) {
                    continue;
                }

                next = c;
            }
            LOGGER.fine( "Next is " + ( next == null ? "N/A" : next.getName() ) );
        } catch (GitAPIException ex) {
            throw new IOException(ex);
        } catch (IOException iox) {
            throw iox;
        } finally {
            if(repo != null) {
                repo.close();
            }
            if(w != null) {
                w.release();
                w.dispose();
            }

            if(git != null) {
                git.close();
            }
        }


        return next;
    }

    @Override
    public void checkRoles(RoleChecker rc) throws SecurityException {
        //NO-OP
    }

}
