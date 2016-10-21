package net.praqma.jenkins.configrotator.scm.git;

import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import net.praqma.jenkins.configrotator.ConfigurationRotator;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.jenkinsci.remoting.RoleChecker;

/**
 * This involves cloning the repository
 */
public class ResolveConfigurationComponent implements FilePath.FileCallable<GitConfigurationComponent> {

    private String name;
    private String repository;
    private String branch;
    private String commitId;
    private boolean fixed;
    private static final Logger LOGGER = Logger.getLogger( ResolveConfigurationComponent.class.getName() );

    private TaskListener listener;

    public ResolveConfigurationComponent( TaskListener listener, String name, String repository, String branch, String commitId, boolean fixed ) {
        this.name = name;
        this.repository = repository;
        this.branch = branch;
        this.commitId = commitId;
        this.fixed = fixed;

        this.listener = listener;
    }

    private void fixName() {
        /* fixing name */
        if( StringUtils.isBlank(name) ) {
            name = repository.substring( repository.lastIndexOf( "/" ) );

            if( name.matches( ".*?\\.git$" ) ) {
                name = name.substring( 0, name.length() - 4 );
            }

            if( name.startsWith( "/" ) ) {
                name = name.substring( 1 );
            }
        }
    }

    private File safeClone(File workspace) throws IOException {
        File local = new File( workspace, name );
        LOGGER.fine( String.format("Cloning repo from %s", repository) );
        org.eclipse.jgit.api.Git g = null;
        try {
            g = org.eclipse.jgit.api.Git.cloneRepository().setURI( repository ).setDirectory( local ).setCloneAllBranches( true ).call();
        } catch( JGitInternalException e ) {
            if(e.getMessage().contains("already exists and is not an empty directory")) {
                LOGGER.info("Ignoring this error...repo already exists");
            } else {
                LOGGER.log(Level.SEVERE, "The error message is not the one we expected. This one should be thrown", e);
                throw new IOException(e);
            }

        } catch (GitAPIException ex) {
            LOGGER.log(Level.SEVERE, String.format("Failed to clone repo %s", local) , ex);
            throw new IOException(String.format("Failed to clone repo %s", local), ex);
        } finally {
            if (g != null) {
                if(g.getRepository() != null) {
                    g.getRepository().close();
                }
                g.close();
            }
        }
        return local;
    }

    private RevCommit createBranchAndPull(File localClone) throws IOException {
        //Init repository
        RevCommit commit;
        Repository repo = null;
        org.eclipse.jgit.api.Git git = null;
        RevWalk w = null;
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        try {
            repo = builder.setGitDir( new File( localClone, ".git" ) ).readEnvironment().findGitDir().build();
            git = new org.eclipse.jgit.api.Git( repo );
            try {
                LOGGER.fine( String.format( "Creating branch %s",  branch ) );
                git.branchCreate().setUpstreamMode( CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM ).setName( branch ).setStartPoint( "origin/" + branch ).call();
            } catch (RefAlreadyExistsException ignore) {
                //This needs to be ignored.
            }

            git.fetch().call();

            w = new RevWalk( repo );

            LOGGER.fine( String.format( "The commit id: %s", commitId ) );

            if( commitId == null || commitId.matches( "^\\s*$" ) ) {
                LOGGER.fine( "Initial commit not defined, using HEAD" );
                listener.getLogger().println( ConfigurationRotator.LOGGERNAME + "Initial commit not defined, using HEAD" );
                commitId = "HEAD";
            }

            LOGGER.fine( String.format("Getting commit '%s'", commitId ) );
            ObjectId o = repo.resolve( commitId );
            commit = w.parseCommit( o );
            LOGGER.fine( String.format( "RevCommit: %s", commit ) );
        } catch (IOException io) {
            throw io;
        } catch (GitAPIException ex) {
            throw new IOException(ex);
        } finally {

            if(repo != null) {
                repo.close();
            }
            if(w != null) {
                w.close();
                w.dispose();
            }
            if(git != null) {
                git.close();
            }
        }
        return commit;
    }

    @Override
    public GitConfigurationComponent invoke( File workspace, VirtualChannel channel ) throws IOException, InterruptedException {
        fixName();
        LOGGER.fine(String.format("Name: %s", name));

        /* Fixing branch */
        if( StringUtils.isBlank(branch) ) {
            branch = "master";
        }

        File local = safeClone(workspace);
        RevCommit commit = createBranchAndPull(local);


        return new GitConfigurationComponent( name, repository, branch, commit, fixed );

    }

    @Override
    public void checkRoles(RoleChecker rc) throws SecurityException {
        //NO-OP
    }
}
