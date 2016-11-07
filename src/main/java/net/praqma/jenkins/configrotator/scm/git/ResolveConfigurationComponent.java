package net.praqma.jenkins.configrotator.scm.git;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.plugins.git.GitException;
import hudson.remoting.VirtualChannel;
import net.praqma.jenkins.configrotator.ConfigurationRotator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.URIish;
import org.jenkinsci.plugins.gitclient.GitClient;
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

    private RevCommit createBranchAndPull(File localClone, GitClient client) throws IOException, URISyntaxException {
        //Init repository
        RevCommit commit = null;
        Repository repo = null;
        org.eclipse.jgit.api.Git git = null;
        RevWalk w = null;

        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        try {
            client.checkout().deleteBranchIfExist(true).branch("origin/"+branch).execute();
            repo = builder.setGitDir( new File( localClone, ".git" ) ).readEnvironment().findGitDir().build();
            git = new org.eclipse.jgit.api.Git( repo );

            client.fetch_().from(new URIish(repository), Collections.singletonList(new RefSpec(branch))).execute();
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
        } catch (GitException ex) {
            Logger.getLogger(ResolveConfigurationComponent.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(ResolveConfigurationComponent.class.getName()).log(Level.SEVERE, null, ex);
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

        File local = new File(workspace, name);

        if(local.mkdirs()) {
            LOGGER.fine("Created repo for component "+name);
        }

        GitClient c = org.jenkinsci.plugins.gitclient.Git.with(listener, null).using("git").in(local).getClient();

        if(!c.hasGitRepo()) {
            LOGGER.fine("Cloning "+repository);
            c.clone_().url(repository).execute();
            LOGGER.fine(repository + " cloned sucessfully");
        }

        RevCommit commit;
        try {
            commit = createBranchAndPull(local, c);
        } catch (URISyntaxException ex) {
            throw new IOException("URI syntax exception in invoke", ex);
        }

        return new GitConfigurationComponent( name, repository, branch, commit.getName(), fixed );

    }

    @Override
    public void checkRoles(RoleChecker rc) throws SecurityException {
        //NO-OP
    }
}
