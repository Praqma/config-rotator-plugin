package net.praqma.jenkins.configrotator.scm.git;

import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;

import java.io.File;
import java.io.IOException;
import org.jenkinsci.plugins.gitclient.GitClient;
import org.jenkinsci.remoting.RoleChecker;

/**
 * @author cwolfgang
 */
public class Checkout implements FilePath.FileCallable<Boolean> {

    private String commitId;
    private String name;
    private String branch;

    public Checkout( String name, String branch, String commitId ) {
        this.commitId = commitId;
        this.name = name;
        this.branch = branch;
    }

    @Override
    public Boolean invoke( File workspace, VirtualChannel channel ) throws IOException, InterruptedException {
        File local = new File( workspace, name );
        GitClient gc = org.jenkinsci.plugins.gitclient.Git.with(TaskListener.NULL, null).using("git").in(local).getClient();
        gc.checkoutBranch(branch, commitId);
        return true;
    }

    @Override
    public void checkRoles(RoleChecker rc) throws SecurityException {
        //NO-OP
    }
}
