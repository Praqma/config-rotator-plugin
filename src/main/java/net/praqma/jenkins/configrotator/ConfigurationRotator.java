package net.praqma.jenkins.configrotator;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.praqma.jenkins.configrotator.scm.ConfigRotatorChangeLogEntry;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.scm.ChangeLogParser;
import hudson.scm.PollingResult;
import hudson.scm.SCMDescriptor;
import hudson.scm.SCMRevisionState;
import hudson.scm.SCM;
import jenkins.model.Jenkins;

import static java.lang.System.*;

public class ConfigurationRotator extends SCM {

    private AbstractConfigurationRotatorSCM acrs;
    private static final Logger LOGGER = Logger.getLogger( ConfigurationRotator.class.getName() );

    /**
     * @return the justConfigured
     */
    public boolean isJustConfigured() {
        return justConfigured;
    }

    /**
     * @param justConfigured the justConfigured to set
     */
    public void setJustConfigured(boolean justConfigured) {
        this.justConfigured = justConfigured;
    }

    public enum ResultType {
        COMPATIBLE,
        INCOMPATIBLE,
        FAILED,
        UNDETERMINED
    }

    public static final String URL_NAME = "config-rotator";
    public static final String NAME = "ConfigRotator";
    public static final String LOGGERNAME = "[" + NAME + "] ";
    private boolean justConfigured = false;

    public static final String SEPARATOR = getProperty( "file.separator" );
    public static final String FEED_DIR = "config-rotator-feeds" + SEPARATOR;

    private static File FEED_PATH;
    private static String VERSION = "Unresolved";

    static {
        if( Jenkins.getInstanceOrNull() != null && Jenkins.getInstance().getPlugin( "config-rotator" ) != null) {
            FEED_PATH = new File( Jenkins.getInstance().getRootDir(), FEED_DIR );
            VERSION = Jenkins.getInstance().getPlugin( "config-rotator" ).getWrapper().getVersion();
        }
    }

    public static File getFeedPath() {
        return FEED_PATH;
    }

    /**
     * Determines whether a new configuration has been entered. If true, the
     * input is new.
     */
    public boolean reconfigure;

    @DataBoundConstructor
    public ConfigurationRotator( AbstractConfigurationRotatorSCM acrs ) {
        this.acrs = acrs;
        this.justConfigured = true;
    }

    public AbstractConfigurationRotatorSCM getAcrs() {
        return acrs;
    }

    public boolean doReconfigure() {
        return reconfigure;
    }

    public void setReconfigure( boolean reconfigure ) {
        this.reconfigure = reconfigure;
    }

    @Override
    public SCMRevisionState calcRevisionsFromBuild( AbstractBuild<?, ?> arg0, Launcher arg1, TaskListener arg2 ) throws IOException, InterruptedException {
        if( !doReconfigure() ) {
            return new SCMRevisionState() {
            };
        } else {
            return null;
        }
    }


    @Override
    public boolean checkout( AbstractBuild<?, ?> build, Launcher launcher, FilePath workspace, BuildListener listener, File file ) throws IOException, InterruptedException {
        PrintStream out = listener.getLogger();
        out.println( LOGGERNAME + "Version: " + VERSION );
        LOGGER.fine( "Version: " + VERSION );

        /*
           * Determine if the job was reconfigured
           */
        if( justConfigured ) {
            reconfigure = acrs.wasReconfigured( build.getProject() );
            listener.getLogger().println("Was reconfigured: "+reconfigure);
            LOGGER.fine( "Was reconfigured: " + reconfigure );
        }

        AbstractConfigurationRotatorSCM.Performer<AbstractConfiguration<?>> performer = acrs.getPerform( build, workspace, listener );
        ConfigurationRotatorBuildAction lastAction = acrs.getLastResult( build.getProject(), performer.getSCMClass() );
        AbstractConfiguration<?> configuration = null;

        /* Regarding JENKINS-14746 */
        ensurePublisher( build );

        boolean performResult = false;
        try {

            if( reconfigure || lastAction == null ) {
                out.println( LOGGERNAME + "Configuration from scratch" );
                configuration = performer.getInitialConfiguration();
            } else {
                out.println( LOGGERNAME + "Getting next configuration" );
                configuration = performer.getNextConfiguration( lastAction );
            }

            acrs.printConfiguration( out, configuration );

            if( configuration != null ) {
                out.println( LOGGERNAME + "Checking configuration(" + configuration.getClass() + ") " + configuration );
                performer.checkConfiguration( configuration );
                performer.createWorkspace( configuration );
                performer.save( configuration );
                performResult = true;

            }
        } catch( Exception e ) {
            LOGGER.log( Level.SEVERE, "Unable to create configuration", e );
            DiedBecauseAction da = new DiedBecauseAction( e.getMessage(), DiedBecauseAction.Die.die, acrs.getTargets() );
            build.addAction( da );
            //We need to reset here.
            reconfigure = false;
            justConfigured = false;
            throw new AbortException( e.getMessage() );
        }

        if( !performResult ) {
            // ConfigurationRotator.perform will return false only if no new baselines found
            // We fail build if there is now new baseline.
            // An alternative would be to do like the CCUCM plugin and make the
            // build result "grey" with an comment "nothing to do".
            DiedBecauseAction da = new DiedBecauseAction( "Nothing to rotate", DiedBecauseAction.Die.survive, acrs.getTargets() );
            build.addAction( da );
            throw new AbortException( "Nothing new to rotate" );
        } else {

            /* Do the change log */
            AbstractConfigurationRotatorSCM.ChangeLogWriter clw = acrs.getChangeLogWriter(file, listener, build );

            try {
                List<ConfigRotatorChangeLogEntry> entries = null;
                if( clw != null ) {
                    if( lastAction == null || reconfigure ) {
                        entries = Collections.emptyList();
                    } else {
                        entries = clw.getChangeLogEntries( configuration );
                    }
                    clw.write( entries );
                } else {
                    LOGGER.info( "Change log writer not implemented" );
                }

            } catch( Exception e ) {
                /* The build must not be terminated because of the change log */
                LOGGER.log( Level.WARNING, "Change log not generated", e );
                out.println( LOGGERNAME + "Change log not generated" );
            }

            /*
                * Config is not fresh anymore
                */
            reconfigure = false;
            justConfigured = false;

            build.getProject().save();

            return true;
        }
    }

    public void ensurePublisher( AbstractBuild build ) throws IOException {
        Describable describable = build.getProject().getPublishersList().get( ConfigurationRotatorPublisher.class );
        if( describable == null ) {
            LOGGER.info( "Adding publisher to project" );
            build.getProject().getPublishersList().add( new ConfigurationRotatorPublisher() );
        }
    }

    public void setConfigurationByAction( AbstractProject<?, ?> project, ConfigurationRotatorBuildAction action ) throws IOException {
        acrs.setConfigurationByAction( project, action );
        justConfigured = true;
    }

    @Override
    protected PollingResult compareRemoteRevisionWith( AbstractProject<?, ?> project, Launcher launcher, FilePath workspace, TaskListener listener, SCMRevisionState arg4 ) throws IOException, InterruptedException {
        PrintStream out = listener.getLogger();
        LOGGER.fine( VERSION );
        // This little check ensures changes are not found while building, as
        // this with many concurrent builds and polling leads to issues where
        // we saw a build was long in the queue, and when started, the polling found
        // changed a did schedule the same build with the same changes as last time
        // between the last one started and finished.
        // Basically this disables polling while the job has a build in the queue.
        if( project.isInQueue() ) {
            out.println( "A build already in queue - cancelling poll" );
            LOGGER.fine( "A build already in queue - cancelling poll" );
            return PollingResult.NO_CHANGES;
        }

        /*
           * Determine if the job was reconfigured
           */
        if( justConfigured ) {
            reconfigure = acrs.wasReconfigured( project );
            LOGGER.fine( "Was reconfigured: " + reconfigure );
        }

        AbstractConfigurationRotatorSCM.Poller poller = acrs.getPoller(project, workspace, listener );

        ConfigurationRotatorBuildAction lastAction = acrs.getLastResult( project, null );
        DiedBecauseAction dieaction = acrs.getLastDieAction(project);

        try {
            if( reconfigure ) {
                LOGGER.fine( "Reconfigured, build now!" );

                out.println( LOGGERNAME + "Configuration from scratch, build now!" );
                return PollingResult.BUILD_NOW;
            } else if( lastAction == null)  {
                if(dieaction != null && dieaction.died()) {
                    LOGGER.fine( "Do actual polling" );
                    out.println( LOGGERNAME + "Error in configuration...do not start build" );
                    return PollingResult.NO_CHANGES;
                } else {
                    LOGGER.fine( "Do actual polling" );
                    out.println( LOGGERNAME + "Getting next configuration" );
                    return poller.poll( lastAction );
                }
            } else {
                LOGGER.fine( "Do actual polling" );
                out.println( LOGGERNAME + "Getting next configuration" );
                //Final check. Was the last build a misconfiguration?
                if(dieaction != null && dieaction.died()) {
                    return PollingResult.NO_CHANGES;
                } else {
                    return poller.poll( lastAction );
                }
            }
        } catch( Exception e ) {
            LOGGER.log( Level.SEVERE, "Unable to poll", e );
            throw new AbortException( e.getMessage() );
        }
    }

    /**
     * Delegate the change log parser to abstract subtypes.
     *
     * @return a new change log parser
     */
    @Override
    public ChangeLogParser createChangeLogParser() {
        return acrs.createChangeLogParser();
    }

    @Override
    public boolean requiresWorkspaceForPolling() {
        return acrs.requiresWorkspaceForPolling();
    }

    @Extension
    public static final class RotatorDescriptor extends SCMDescriptor<ConfigurationRotator> {

        public RotatorDescriptor() {
            super( ConfigurationRotator.class, null );
        }

        @Override
        public String getDisplayName() {
            return "Config rotator";
        }

        public List<ConfigurationRotatorSCMDescriptor<?>> getSCMs() {
            return AbstractConfigurationRotatorSCM.getDescriptors();
        }

    }
}
