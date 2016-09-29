package net.praqma.jenkins.configrotator;

import hudson.Launcher;
import hudson.model.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Logger;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;

/**
 * @author cwolfgang
 */
public class ConfigRotatorRule2 extends JenkinsRule {

    private static final Logger logger = Logger.getLogger( ConfigRotatorRule2.class.getName() );

    private File outputDir;

    public ConfigRotatorRule2( Class<?> clazz ) {

        if( System.getenv().containsKey( "BUILD_NUMBER" ) ) {
            String bname = System.getenv( "JOB_NAME" );
            Integer number = new Integer( System.getenv( "BUILD_NUMBER" ) );

            this.outputDir = new File( new File( new File( new File( System.getProperty( "user.dir" ) ), "test-logs" ), number.toString() ), ConfigRotatorProject.getSafeName( clazz.getName() ) );
        } else {
            this.outputDir = new File( new File( System.getProperty( "user.dir" ) ), "runs" );
        }

        this.outputDir.mkdirs();
    }

    public AbstractBuild<?, ?> buildProject( Project<?, ?> project, boolean fail, Slave slave ) throws IOException {

        if( slave != null ) {
            logger.fine( String.format( "Running on %s", slave.getSelfLabel().getName() ) );
            project.setAssignedLabel(slave.getSelfLabel());
        }

        if( fail ) {
            logger.info( String.format( "Failing %s", project ) );
            project.getBuildersList().add( new Failer() );
        } else {
            /* Should remove fail task */
            project.getBuildersList().remove( Failer.class );
        }

        AbstractBuild<?, ?> build = null;
        try {
            EnableLoggerAction action = new EnableLoggerAction( outputDir );
            build = project.scheduleBuild2( 0, new Cause.UserCause(), action ).get();
        } catch( Exception e ) {
            e.printStackTrace();
        }

        PrintStream out = new PrintStream( new File( outputDir, "jenkins." + ConfigRotatorProject.getSafeName( project.getDisplayName() ) + "." + build.getNumber() + ".log" ) );

        out.println( "Build      : " + build );
        out.println( "Workspace  : " + build.getWorkspace() );
        out.println( "Logfile    : " + build.getLogFile() );
        out.println( "Description: " + build.getDescription() );
        out.println();
        out.println( "-------------------------------------------------" );
        out.println( "                JENKINS LOG: " );
        out.println( "-------------------------------------------------" );
        out.println( getLog( build ) );
        out.println( "-------------------------------------------------" );
        out.println( "-------------------------------------------------" );
        out.println();

        return build;
    }

    public static class Failer extends TestBuilder {

        @Override
        public boolean perform( AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener ) throws InterruptedException, IOException {
            return false;
        }
    }
}
