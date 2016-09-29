package net.praqma.jenkins.configrotator.scm.clearcaseucm;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.TaskListener;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.praqma.clearcase.PVob;
import net.praqma.clearcase.exceptions.UCMEntityNotInitializedException;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Component;
import net.praqma.clearcase.ucm.entities.Project;
import net.praqma.clearcase.ucm.view.SnapshotView;
import net.praqma.jenkins.configrotator.*;
import net.praqma.jenkins.configrotator.scm.ConfigRotatorChangeLogEntry;
import net.praqma.jenkins.configrotator.scm.ConfigRotatorChangeLogParser;
import net.praqma.jenkins.configrotator.scm.contribute.ConfigRotatorClearCaseConverterImpl;
import net.praqma.jenkins.configrotator.scm.contribute.ConfigRotatorCompatabilityConverter;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

public class ClearCaseUCM extends AbstractConfigurationRotatorSCM implements Serializable {

    private static final Logger logger = Logger.getLogger( ClearCaseUCM.class.getName() );

    public List<ClearCaseUCMTarget> targets;

    private PVob pvob;
    private boolean contribute = false;

    public ClearCaseUCM( PVob pvob) {
        this.pvob = pvob;
    }

    public ClearCaseUCM( String pvobName ) {
        pvob = new PVob( pvobName );
    }

    @DataBoundConstructor
    public ClearCaseUCM( String pvobName, boolean contribute, List<ClearCaseUCMTarget> targets) {
        pvob = new PVob( pvobName );
        this.contribute = contribute;
        this.targets = targets;
    }

    public String getPvobName() {
        return pvob.toString();
    }

    @Override
    public String getName() {
        return "ClearCase UCM";
    }

    @Override
    public ConfigRotatorChangeLogParser createChangeLogParser() {
        return new ConfigRotatorChangeLogParser();
    }

    public List<ClearCaseUCMTarget> getCompareTargets( AbstractProject<?,?> project ) {
        ConfigurationRotatorBuildAction action = getLastResult( project, ClearCaseUCM.class );
        DiedBecauseAction diedAction = getLastDieAction(project);
        if(action != null && action.getConfiguration() == null) {
            throw new AssertionError("The configuration is NOT allowed to be null at this point.");
        }

        List<ClearCaseUCMTarget> list = (action != null) ? getConfigurationAsTargets( (ClearCaseUCMConfiguration)action.getConfiguration() ) : (List<ClearCaseUCMTarget>)diedAction.getTargets();
        return list;
    }

    @Override
    public boolean wasReconfigured( AbstractProject<?, ?> project ) {
        logger.finest( "Checking reconfiguration" );

        ConfigurationRotatorBuildAction action = getLastResult( project, ClearCaseUCM.class );
        DiedBecauseAction diedAction = getLastDieAction(project);

        //If there was NO previous error and action is null that means a new build
        if(diedAction == null && action == null ) {
            return true;
        }

        List<ClearCaseUCMTarget> list = getCompareTargets(project);
        List<ClearCaseUCMTarget> configTargets = getTargets();

        /* Check if the sizes are equal */
        if( configTargets.size() != list.size() ) {
            logger.fine( "Size was not equal" );
            return true;
        }

        for( int i = 0; i < configTargets.size(); ++i ) {
            logger.fine(String.format( "Comparing (SCM Configuration Target) %s to (Previously Completed Build Target) %s", configTargets.get(i), list.get(i)) );
            if( !configTargets.get( i ).equals( list.get( i ) ) ) {
                return true;
            }
        }

        logger.finest( "UCM was not reconfigured" );
        return false;
    }

    @Override
    public Poller getPoller( AbstractProject<?, ?> project, Launcher launcher, FilePath workspace, TaskListener listener ) {
        return new Poller(project, launcher, workspace, listener, false );
    }


    @Override
    public Performer<ClearCaseUCMConfiguration> getPerform( AbstractBuild<?, ?> build, Launcher launcher, FilePath workspace, BuildListener listener ) throws IOException {
        return new UCMPerformer(build, launcher, workspace, listener);
    }

    @Override
    public ConfigRotatorCompatabilityConverter getConverter() {
        return new ConfigRotatorClearCaseConverterImpl();
    }

    @Override
    public boolean isContribute() {
        return contribute;
    }

    public void setContribute(boolean contribute) {
        this.contribute = contribute;
    }

    public class UCMPerformer extends Performer<ClearCaseUCMConfiguration> {

        public UCMPerformer( AbstractBuild<?, ?> build, Launcher launcher, FilePath workspace, BuildListener listener ) {
            super( build, launcher, workspace, listener );
        }

        @Override
        public ClearCaseUCMConfiguration getInitialConfiguration() throws IOException, ConfigurationRotatorException {
            return ClearCaseUCMConfiguration.getConfigurationFromTargets( getTargets(), workspace, listener );
        }

        @Override
        public ClearCaseUCMConfiguration getNextConfiguration( ConfigurationRotatorBuildAction action ) throws ConfigurationRotatorException {
            ClearCaseUCMConfiguration oldconfiguration = action.getConfiguration();
            return (ClearCaseUCMConfiguration) nextConfiguration( listener, oldconfiguration, workspace );
        }

        @Override
        public void checkConfiguration( ClearCaseUCMConfiguration configuration ) throws ConfigurationRotatorException {
               simpleCheckOfConfiguration( configuration );
        }

        @Override
        public void createWorkspace( ClearCaseUCMConfiguration configuration ) throws ConfigurationRotatorException {
            try {
                out.println( ConfigurationRotator.LOGGERNAME + "Creating view" );
                logger.fine( "Creating view" );
                SnapshotView view = createView( listener, build, configuration, workspace, pvob );
                logger.fine( String.format("Created view %s", view) );
                configuration.setView( view );
            } catch( Exception e ) {
                out.println( ConfigurationRotator.LOGGERNAME + "Unable to create view" );
                ConfigurationRotatorException ex = new ConfigurationRotatorException( "Unable to create view", e );
                logger.log(Level.SEVERE, "Unable to create view in createWorkspace()", ex);
                throw ex;
            }
        }

        @Override
        public void print( ClearCaseUCMConfiguration configuration ) {
            printConfiguration( out, configuration );
        }
    }

    public void reconfigure( FilePath workspace, TaskListener listener ) throws IOException {
        logger.fine( "Getting configuration" );
        PrintStream out = listener.getLogger();

        /* Resolve the configuration */
        ClearCaseUCMConfiguration inputconfiguration = null;
        try {
            inputconfiguration = ClearCaseUCMConfiguration.getConfigurationFromTargets( getTargets(), workspace, listener );
        } catch( ConfigurationRotatorException e ) {
            if(e.getCause() != null && e.getCause() instanceof UCMEntityNotInitializedException) {
                throw new AbortException(String.format("Reconfigure failed. UCM Entity could not be loaded.%n%s", e.getCause().getMessage() ));
            } else {
                throw new AbortException(String.format("Reconfigure failed.%n%s", ConfigurationRotator.LOGGERNAME + "Unable to parse configuration: " + e.getMessage() ));
            }
        }
        projectConfiguration = inputconfiguration;
    }

    @Override
    public void printConfiguration( PrintStream out, AbstractConfiguration cfg ) {
        out.println( ConfigurationRotator.LOGGERNAME + "The configuration is:" );
        logger.fine( ConfigurationRotator.LOGGERNAME + "The configuration is:" );
        if( cfg instanceof ClearCaseUCMConfiguration ) {
            ClearCaseUCMConfiguration config = (ClearCaseUCMConfiguration) cfg;
            for( ClearCaseUCMConfigurationComponent c : config.getList() ) {
                out.println( " * " + c.getBaseline().getComponent() + ", " + c.getBaseline().getStream() + ", " + c.getBaseline().getNormalizedName() );
                logger.fine( " * " + c.getBaseline().getComponent() + ", " + c.getBaseline().getStream() + ", " + c.getBaseline().getNormalizedName() );
            }
            out.println( "" );
            logger.fine( "" );
        }
    }

    public static void simpleCheckOfConfiguration( AbstractConfiguration cfg ) throws ConfigurationRotatorException {
        if( cfg instanceof ClearCaseUCMConfiguration ) {
            ClearCaseUCMConfiguration config = (ClearCaseUCMConfiguration) cfg;
            Set<Component> ccucmcfgset = new HashSet<Component>();

            // loops iterates over clear case component which must have unique
            // hash representation
            // Notice: we should throw abort exception that is catched by jenkins
            // and message printed to the console by Jenkins.
            // Therefore we like it to be descriptive.
            for( ClearCaseUCMConfigurationComponent c : config.getList() ) {
                // check 1) is a component more than once in the configuration?
                // as baselines are part of component, this also ensure no two baseline
                // for the same component are used.
                Component currentClearCaseComponent = c.getBaseline().getComponent();
                if( !ccucmcfgset.contains( currentClearCaseComponent ) ) {
                    ccucmcfgset.add( currentClearCaseComponent );
                } else {
                    String errorMessage = ConfigurationRotator.LOGGERNAME + "Simple check of configuration failed because component used more than once in configuration. Component is: \n";
                    errorMessage += " * " + c.getBaseline().getComponent() + ", " + c.getBaseline().getStream() + ", " + c.getBaseline().getNormalizedName();
                    throw new ConfigurationRotatorException( errorMessage );
                }
            }
        } else {
            throw new ConfigurationRotatorException( "simpleCheckOfconfiguration failed " + cfg );
        }
    }

    @Override
    public AbstractConfiguration nextConfiguration( TaskListener listener, AbstractConfiguration configuration, FilePath workspace ) throws ConfigurationRotatorException {

        Baseline oldest = null, current;
        ClearCaseUCMConfigurationComponent chosen = null;

        listener.getLogger().printf("%sUsing newsest baseline:%s%n", ConfigurationRotator.LOGGERNAME, isUseNewest());

        ClearCaseUCMConfiguration nconfig = ((ClearCaseUCMConfiguration) configuration).clone();

        List<Baseline> changes = new ArrayList<Baseline>();

        for( ClearCaseUCMConfigurationComponent config : nconfig.getList() ) {
            /* This configuration is not fixed */
            if( !config.isFixed() ) {
                try {
                    Baseline previous = config.getBaseline();
                    current = workspace.act( new NextBaseline(
                            config.getBaseline().getStream(),
                            config.getBaseline().getComponent(),
                            config.getPlevel(), config.getBaseline(), isUseNewest() ) );

                    if(isUseNewest()) {
                        config.setChangedLast(true);
                        config.setBaseline(current);
                        changes.add(current);
                        listener.getLogger().printf("%sBaseline switched from %s to %s%n", ConfigurationRotator.LOGGERNAME, previous, current);
                    } else {
                        if(oldest == null || current.getDate().before(oldest.getDate())) {
                            oldest = current;
                            chosen = config;
                            config.setChangedLast(false);
                        }
                    }
                } catch( Exception e ) {
                    /* No baselines found .get(0) above throws exception if no new baselines*/
                    logger.log(Level.FINE, ConfigurationRotator.LOGGERNAME + "No baselines found. Exception message follows", e );
                }
            }
        }

        //No changes for 'newest'.
        if( isUseNewest() && changes.isEmpty() ) {
            listener.getLogger().println( ConfigurationRotator.LOGGERNAME + "No new baselines" );
            return null;
        } else if( isUseNewest() && !changes.isEmpty() ) {
            return nconfig;
        } else if( !isUseNewest() && chosen != null && oldest != null ) {
            listener.getLogger().printf("%sSwitched a single baseline %s%n", ConfigurationRotator.LOGGERNAME, oldest);
            chosen.setBaseline(oldest);
            chosen.setChangedLast(true);
            return nconfig;
        } else {
            return null;
        }
    }

    public SnapshotView createView( TaskListener listener, AbstractBuild<?, ?> build, ClearCaseUCMConfiguration configuration, FilePath workspace, PVob pvob ) throws IOException, InterruptedException {
        logger.fine( "Getting project" );
        Project project = workspace.act( new DetermineProject( Arrays.asList( new String[]{ "jenkins", "Jenkins", "hudson", "Hudson" } ), pvob ) );

        /* Create baselines list */
        List<Baseline> selectedBaselines = new ArrayList<Baseline>();
        logger.fine( "Selected baselines:" );
        for( ClearCaseUCMConfigurationComponent config : configuration.getList() ) {
            logger.fine( String.format( "Component: %s", config ) );
            selectedBaselines.add( config.getBaseline() );
        }

        /* Create a config rotator project name. Later machine name is appended in the resulting viewtag*/
        String crProjectName = "cr-" + build.getProject().getDisplayName().replaceAll( "\\s", "_" );
        return workspace.act( new PrepareWorkspace( project, selectedBaselines, crProjectName, listener ) );

    }

    @Override
    public <TT extends AbstractTarget> void setTargets( List<TT> targets ) {
        this.targets = (List<ClearCaseUCMTarget>) targets;
    }

    /**
     * Get the configuration as targets. If the project configuration is null, the last targets defined by the configuration page is returned otherwise the current project configuration is returned as targets
     *
     * @return A list of targets
     */
    @Override
    public final List<ClearCaseUCMTarget> getTargets() {
        if( projectConfiguration != null ) {
            return getConfigurationAsTargets( (ClearCaseUCMConfiguration) projectConfiguration );
        } else {
            return targets;
        }
    }

    private List<ClearCaseUCMTarget> getConfigurationAsTargets( ClearCaseUCMConfiguration config ) {
        List<ClearCaseUCMTarget> list = new ArrayList<ClearCaseUCMTarget>();
        if( config.getList() != null && config.getList().size() > 0 ) {
            for( ClearCaseUCMConfigurationComponent c : config.getList() ) {
                if( c != null ) {
                    list.add( new ClearCaseUCMTarget( c.getBaseline().getNormalizedName(), c.getPlevel(), c.isFixed() ) );
                } else {
                    /* A null!? The list is corrupted, return targets */
                    return targets;
                }
            }

            return list;
        } else {
            return targets;
        }
    }

    @Override
    public void setConfigurationByAction( AbstractProject<?, ?> project, ConfigurationRotatorBuildAction action ) throws IOException {
        ClearCaseUCMConfiguration c = action.getConfiguration();
        if( c == null ) {
            throw new AbortException( ConfigurationRotator.LOGGERNAME + "Not a valid configuration" );
        } else {
            this.projectConfiguration = c;
            project.save();
        }
    }

    @Override
    public ChangeLogWriter getChangeLogWriter( File changeLogFile, BuildListener listener, AbstractBuild<?, ?> build ) {
        return new UCMChangeLogWriter( changeLogFile, listener, build );
    }

    public class UCMChangeLogWriter extends ChangeLogWriter<ClearCaseUCMConfigurationComponent, ClearCaseUCMConfiguration> {

        public UCMChangeLogWriter( File changeLogFile, BuildListener listener, AbstractBuild<?, ?> build ) {
            super( changeLogFile, listener, build );
        }

        @Override
        protected List<ConfigRotatorChangeLogEntry> getChangeLogEntries( ClearCaseUCMConfiguration configuration, ClearCaseUCMConfigurationComponent component ) throws ConfigurationRotatorException {
            try {
                return build.getWorkspace().act( new ClearCaseGetBaseLineCompare(listener, configuration, component ) );
            } catch( Exception e ) {
                throw new ConfigurationRotatorException( e );
            }
        }
    }

    @Extension
    public static final class DescriptorImpl extends ConfigurationRotatorSCMDescriptor<ClearCaseUCM> {

        @Override
        public String getDisplayName() {
            return "ClearCase UCM Components";
        }

        @Override
        public String getFeedComponentName() {
            return ClearCaseUCM.class.getSimpleName();
        }

        @Override
        public AbstractConfigurationRotatorSCM newInstance( StaplerRequest req, JSONObject formData, AbstractConfigurationRotatorSCM i ) throws FormException {
            ClearCaseUCM instance = (ClearCaseUCM) i;

            List<ClearCaseUCMTarget> targets = new ArrayList<ClearCaseUCMTarget>();
            try {
                JSONArray obj = formData.getJSONObject( "acrs" ).getJSONArray( "targets" );
                targets = req.bindJSONToList( ClearCaseUCMTarget.class, obj );
            } catch( net.sf.json.JSONException jasonEx ) {
                //This happens if the targets is not an array!
                JSONObject obj = formData.getJSONObject( "acrs" ).getJSONObject( "targets" );
                if( obj != null ) {
                    ClearCaseUCMTarget target = req.bindJSON( ClearCaseUCMTarget.class, obj );
                    if( target != null && target.getBaselineName() != null && !target.getBaselineName().equals( "" ) ) {
                        targets.add( target );
                    }
                }
            }
            instance.targets = targets;
            save();
            return instance;
        }

        public List<ClearCaseUCMTarget> getTargets( ClearCaseUCM instance ) {
            if( instance == null ) {
                return new ArrayList<ClearCaseUCMTarget>();
            } else {
                return instance.getTargets();
            }
        }

        public Project.PromotionLevel[] getPromotionLevels() {
            return Project.PromotionLevel.values();
        }


    }
}