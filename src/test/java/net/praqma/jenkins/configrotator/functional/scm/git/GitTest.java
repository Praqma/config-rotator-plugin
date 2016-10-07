package net.praqma.jenkins.configrotator.functional.scm.git;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.triggers.SCMTrigger;
import net.praqma.jenkins.configrotator.*;
import net.praqma.jenkins.configrotator.scm.clearcaseucm.ClearCaseUCMTarget;
import net.praqma.jenkins.configrotator.scm.git.Git;
import net.praqma.jenkins.configrotator.scm.git.GitTarget;
import net.praqma.logging.PraqmaticLogFormatter;
import net.praqma.util.test.junit.LoggingRule;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class GitTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Rule
    public GitRule git = new GitRule();

    @ClassRule
    public static LoggingRule lrule = new LoggingRule( Level.ALL, "net.praqma" ).setFormat( PraqmaticLogFormatter.NORMAL_FORMAT );

    @ClassRule
    public static ConfigRotatorRule2 crRule = new ConfigRotatorRule2( GitTest.class );

    @Test
    public void basic() throws IOException, GitAPIException, InterruptedException {
        git.initialize( folder.newFolder() );
        RevCommit commit1 = git.createCommit( "text.txt", "1" );

        ProjectBuilder builder = new ProjectBuilder( new Git(new ArrayList<GitTarget>()) ).setName( "git-test-01" );
        ConfigRotatorProject project = builder.getProject();
        project.addTarget( new GitTarget( "test", git.getRepo(), "master", commit1.getName(), false ) );

        AbstractBuild<?, ?> build = crRule.buildProject( project.getJenkinsProject(), false, null );

        FilePath path = new FilePath( project.getJenkinsProject().getLastBuiltOn().getWorkspaceFor( (FreeStyleProject)project.getJenkinsProject() ), "test" );
        File filePath = new File( path.toURI() );

        SystemValidator<ClearCaseUCMTarget> val = new SystemValidator<ClearCaseUCMTarget>( build );
        val.checkExpectedResult( Result.SUCCESS ).
                checkAction( true ).
                checkCompatability( true ).
                checkTargets( new GitTarget( "test", git.getRepo(), "master", commit1.getName(), false ) ).
                checkContent( new File( filePath, "text.txt" ), "1" ).
                validate();
    }

    @Test
    public void basicNothingToDo() throws IOException, GitAPIException, InterruptedException {
        git.initialize( folder.newFolder() );
        RevCommit commit1 = git.createCommit( "text.txt", "1" );

        ProjectBuilder builder = new ProjectBuilder( new Git(new ArrayList<GitTarget>()) ).setName( "git-test-02" );
        ConfigRotatorProject project = builder.getProject();
        project.addTarget( new GitTarget( "test", git.getRepo(), "master", commit1.getName(), false ) );

        AbstractBuild<?, ?> build = crRule.buildProject( project.getJenkinsProject(), false, null );

        FilePath path = new FilePath( project.getJenkinsProject().getLastBuiltOn().getWorkspaceFor( (FreeStyleProject)project.getJenkinsProject() ), "test" );
        File filePath = new File( path.toURI() );

        SystemValidator<ClearCaseUCMTarget> val = new SystemValidator<ClearCaseUCMTarget>( build );
        val.checkExpectedResult( Result.SUCCESS ).
                checkAction( true ).
                checkCompatability( true ).
                checkTargets( new GitTarget( "test", git.getRepo(), "master", commit1.getName(), false ) ).
                checkContent( new File( filePath, "text.txt" ), "1" ).
                validate();

        AbstractBuild<?, ?> build2 = crRule.buildProject( project.getJenkinsProject(), false, null );

        SystemValidator<ClearCaseUCMTarget> val2 = new SystemValidator<ClearCaseUCMTarget>( build2 );
        val2.checkExpectedResult( Result.NOT_BUILT ).checkAction( false ).validate();
    }


    @Test
    public void basicMultiple() throws IOException, GitAPIException, InterruptedException {
        git.initialize( folder.newFolder() );
        RevCommit commit1 = git.createCommit( "text.txt", "1" );
        RevCommit commit2 = git.createCommit( "text.txt", "2" );

        ProjectBuilder builder = new ProjectBuilder( new Git(new ArrayList<GitTarget>()) ).setName( "git-test-03" );
        ConfigRotatorProject project = builder.getProject();
        project.addTarget( new GitTarget( "test", git.getRepo(), "master", commit1.getName(), false ) );

        AbstractBuild<?, ?> build1 = crRule.buildProject( project.getJenkinsProject(), false, null );
        AbstractBuild<?, ?> build2 = crRule.buildProject( project.getJenkinsProject(), false, null );

        FilePath path = new FilePath( project.getJenkinsProject().getLastBuiltOn().getWorkspaceFor( (FreeStyleProject)project.getJenkinsProject() ), "test" );
        File filePath = new File( path.toURI() );

        SystemValidator<ClearCaseUCMTarget> val2 = new SystemValidator<ClearCaseUCMTarget>( build2 );
        val2.checkExpectedResult( Result.SUCCESS ).
                checkAction( true ).
                checkCompatability( true ).
                checkTargets( new GitTarget( "test", git.getRepo(), "master", commit2.getName() , false ) ).
                checkContent( new File( filePath, "text.txt" ), "2" ).
                validate();

        AbstractBuild<?, ?> build3 = crRule.buildProject( project.getJenkinsProject(), false, null );

        SystemValidator<ClearCaseUCMTarget> val3 = new SystemValidator<ClearCaseUCMTarget>( build3 );
        val3.checkExpectedResult( Result.NOT_BUILT ).validate();
    }

    @Test
    public void basicTag() throws IOException, GitAPIException, InterruptedException {
        git.initialize(folder.newFolder());
        RevCommit commit1 = git.createCommit("text.txt", "1");
        String tagName = "configRotator";
        git.createTag(tagName);

        ProjectBuilder builder = new ProjectBuilder(new Git(new ArrayList<GitTarget>())).setName("git-test-04");
        ConfigRotatorProject project = builder.getProject();
        project.addTarget(new GitTarget("test", git.getRepo(), "master", tagName, false));

        AbstractBuild<?, ?> build = crRule.buildProject(project.getJenkinsProject(), false, null);

        FilePath path = new FilePath(project.getJenkinsProject().getLastBuiltOn().getWorkspaceFor((FreeStyleProject) project.getJenkinsProject()), "test");
        File filePath = new File(path.toURI());

        SystemValidator<ClearCaseUCMTarget> val = new SystemValidator<ClearCaseUCMTarget>(build);
        val.checkExpectedResult(Result.SUCCESS).
                checkAction(true).
                checkCompatability(true).
                checkTargets(new GitTarget("test", git.getRepo(), "master", commit1.getName(), false)).
                checkContent(new File(filePath, "text.txt"), "1").
                validate();
    }

    @Test
    public void tagDeleted_Regression_JENKINS22533_GITHUB11() throws Exception {
        File f = folder.newFolder();
        git.initialize(f);
        RevCommit commit1 = git.createCommit("text.txt", "1");
        RevCommit commit2 = git.createCommit("text.txt", "2");
        String tagName = "configRotator";
        git.createTag(tagName);
        RevCommit commit3 = git.createCommit("text.txt", "3");

        ProjectBuilder builder = new ProjectBuilder(new Git(new ArrayList<GitTarget>())).setName("git-test-JENKINS22533");
        ConfigRotatorProject project = builder.getProject();

        GitTarget t = new GitTarget("test", git.getRepo(), "master", tagName, false);
        project.addTarget(t);

        AbstractBuild<?, ?> build = crRule.buildProject(project.getJenkinsProject(), false, null);
        crRule.waitUntilNoActivityUpTo(60000);

        //We've just reconfigured...use a non-existing illegal ref.
        project.reconfigure().addTarget(new GitTarget("test", git.getRepo(), "master", "NOT_THERE", false));
        //Update config
        project.getJenkinsProject().save();

        triggerProject(project.getJenkinsProject());
        crRule.waitUntilNoActivityUpTo(60000);
        //Start the build...with an invalid configuration (we were just reconfigured)
        AbstractBuild<?,?> secondBuild = project.getJenkinsProject().getBuildByNumber(2);

        triggerProject(project.getJenkinsProject());
        crRule.waitUntilNoActivityUpTo(60000);
        AbstractBuild<?,?> thirdBuild = project.getJenkinsProject().getBuildByNumber(3);

        assertNull(String.format("Number of builds should be two, was not two. This is the console log for the extra build %s",
                thirdBuild != null ? FileUtils.readFileToString(thirdBuild.getLogFile()) : "")
                , thirdBuild);

        project.reconfigure().addTarget(new GitTarget("test", git.getRepo(), "master", tagName, false));

        triggerProject(project.getJenkinsProject());
        crRule.waitUntilNoActivityUpTo(60000);

        thirdBuild = project.getJenkinsProject().getBuildByNumber(3);
        assertNotNull(thirdBuild);
    }

    public static void triggerProject(AbstractProject<?,?> project) throws Exception {
        project.getTriggers().clear();
        SCMTrigger scmTrigger = new SCMTrigger("@daily", true);
        project.addTrigger(scmTrigger);
        scmTrigger.start(project, true);
        scmTrigger.new Runner().run();
    }

    @After
    public void cleanUpGit() {
        git.cleanup();
    }

}
