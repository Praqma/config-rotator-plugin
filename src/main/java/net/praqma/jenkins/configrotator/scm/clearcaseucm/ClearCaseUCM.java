package net.praqma.jenkins.configrotator.scm.clearcaseucm;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.scm.PollingResult;
import hudson.util.FormValidation;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import net.praqma.clearcase.PVob;
import net.praqma.clearcase.ucm.entities.Activity;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Project;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.ucm.view.SnapshotView;
import net.praqma.clearcase.util.ExceptionUtils;
import net.praqma.jenkins.configrotator.*;
import net.praqma.jenkins.configrotator.scm.ConfigRotatorChangeLogParser;
import net.praqma.jenkins.utils.remoting.DetermineProject;
import net.praqma.jenkins.utils.remoting.GetBaselines;
import net.praqma.util.debug.Logger;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

public class ClearCaseUCM extends AbstractConfigurationRotatorSCM implements Serializable {
    
    private static Logger logger = Logger.getLogger();
    
    public ClearCaseUCMConfiguration projectConfiguration;
    
    public List<ClearCaseUCMTarget> targets;
    
    private PVob pvob;

    /**
     * Version 0.1.0 constructor
     * 
     * Parse config
     * Each line represents a {@link Component}, {@link Stream}, {@link Baseline} and a {@Plevel plevel}
     * @param config
     */
    @DataBoundConstructor
    public ClearCaseUCM( String pvobName ) {
        pvob = new PVob( pvobName );
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
        return new ClearCaseUCMConfigRotatorChangeLogParser();
    }
    
    @Override
    public boolean wasReconfigured( AbstractProject<?, ?> project ) {
        ConfigurationRotatorBuildAction action = getLastResult( project, ClearCaseUCM.class );
        
        if( action == null ) {
            return true;
        }
        
        ClearCaseUCMConfiguration configuration = action.getConfiguration(ClearCaseUCMConfiguration.class);
        
        /* Check if the project configuration is even set */
        if( configuration == null ) {
            logger.debug( "Configuration was null" );
            return true;
        }
        
        /* Check if the sizes are equal */
        if( targets.size() != configuration.getList().size() ) {
            logger.debug( "Size was not equal" );
            return true;
        }
        
        /**/
        List<ClearCaseUCMTarget> list = getConfigurationAsTargets( configuration );
        for( int i = 0 ; i < targets.size() ; ++i ) {
            if( !targets.get( i ).equals( list.get( i ) ) ) {
                logger.debug( "Configuration was not equal" );
                return true;
            }
        }
        
        return false;
    }
    

    @Override
    public boolean perform( AbstractBuild<?, ?> build, Launcher launcher, FilePath workspace, BuildListener listener, boolean reconfigure ) throws IOException {
        PrintStream out = listener.getLogger();
        
        ConfigurationRotatorBuildAction action = getLastResult( build.getProject(), ClearCaseUCM.class );
        
        /* If there's no action, this is the first run */
        if( action == null || reconfigure ) {
            logger.debug( "Getting configuration" );
            
            /* Resolve the configuration */
            ClearCaseUCMConfiguration inputconfiguration = null;
            try {
                inputconfiguration = ClearCaseUCMConfiguration.getConfigurationFromTargets( getTargets(), workspace, listener );
            } catch( ConfigurationRotatorException e ) {
                out.println( "Unable to parse configuration: " + e.getMessage() );
                ExceptionUtils.print( e, out, true );
                throw new AbortException();
            }
            
            projectConfiguration = inputconfiguration;
        } else {
            logger.debug( "Action was NOT null" );
            /* Get the configuration from the action */
            ClearCaseUCMConfiguration oldconfiguration = action.getConfiguration(ClearCaseUCMConfiguration.class);
            /* Get next configuration */
            try {
                logger.debug( "Obtaining new configuration based on old" );
                /* No new baselines */
                if( ( projectConfiguration = nextConfiguration( listener, oldconfiguration, workspace ) ) == null ) {
                    return false;
                }
            } catch( Exception e ) {
                out.println( "Unable to get next configuration: " + e.getMessage() );
                ExceptionUtils.print( e, out, true );
                throw new AbortException();
            }
        }
        
        printConfiguration( out, projectConfiguration );
        
        /* Create the view */
        try {
            out.println( ConfigurationRotator.LOGGERNAME + "Creating view" );
            SnapshotView view = createView( listener, build, projectConfiguration, workspace, pvob );
            projectConfiguration.setView( view );
        } catch( Exception e ) {
            out.println( ConfigurationRotator.LOGGERNAME + "Unable to create view, exception message is: " + e.getMessage() );
            ExceptionUtils.print( e, out, true );
            throw new AbortException();
        }
                
        /* Just try to save */
        logger.debug( "Adding action" );
        final ConfigurationRotatorBuildAction action1 = new ConfigurationRotatorBuildAction( build, ClearCaseUCM.class, projectConfiguration );
        build.addAction( action1 );
        
        return true;
    }
    
    /**
     * Reconfigure the project configuration given the targets from the configuration page
     * @param workspace A FilePath
     * @param listener A TaskListener
     * @throws IOException
     */
    public void reconfigure( FilePath workspace, TaskListener listener ) throws IOException {
        logger.debug( "Getting configuration" );
        PrintStream out = listener.getLogger();
        
        /* Resolve the configuration */
        ClearCaseUCMConfiguration inputconfiguration = null;
        try {
            inputconfiguration = ClearCaseUCMConfiguration.getConfigurationFromTargets( getTargets(), workspace, listener );
        } catch( ConfigurationRotatorException e ) {
            out.println( "Unable to parse configuration: " + e.getMessage() );
            ExceptionUtils.print( e, out, true );
            throw new AbortException();
        }
        
        projectConfiguration = inputconfiguration;
    }
    
    public void printConfiguration( PrintStream out, AbstractConfiguration cfg ) {
        out.println( "The configuration is:" );
        if( cfg instanceof ClearCaseUCMConfiguration ) {
            ClearCaseUCMConfiguration config = (ClearCaseUCMConfiguration)cfg;
            for( ClearCaseUCMConfigurationComponent c : config.getList() ) {
                out.println( " * " + c.getBaseline().getComponent() + ", " + c.getBaseline().getStream() + ", " + c.getBaseline().getNormalizedName() );
            }
            out.println( "" );
        }
    }
    
    public ClearCaseUCMConfiguration nextConfiguration( TaskListener listener, ClearCaseUCMConfiguration configuration, FilePath workspace ) throws IOException, InterruptedException, ConfigurationRotatorException {
        
        Baseline oldest = null, current;
        ClearCaseUCMConfigurationComponent chosen = null;
        
        ClearCaseUCMConfiguration nconfig = configuration.clone();
        
        logger.debug( "Foreach configuration component" );
        listener.getLogger().println( ConfigurationRotator.LOGGERNAME + "Foreach configuration component" );
        for( ClearCaseUCMConfigurationComponent config : nconfig.getList() ) {
            logger.debug( "CONFIG: " + config );
            listener.getLogger().println( ConfigurationRotator.LOGGERNAME + "CONFIG: " + config );
            /* This configuration is not fixed */
            if( !config.isFixed() ) {
                logger.debug( "Wasn't fixed: " + config.getBaseline().getNormalizedName() );
                listener.getLogger().println( ConfigurationRotator.LOGGERNAME + "Wasn't fixed: " + config.getBaseline().getNormalizedName() );
                
                try {
                    current = workspace.act( new GetBaselines( listener, config.getBaseline().getComponent(), config.getBaseline().getStream(), config.getPlevel(), 1, config.getBaseline() ) ).get( 0 ); //.get(0) newest baseline, they are sorted!
                    listener.getLogger().println( ConfigurationRotator.LOGGERNAME + "Current configuration found on slave is: " + current );
                    if( oldest == null || current.getDate().before( oldest.getDate() ) )  {
                        logger.debug( "Was older: " + current );
                        listener.getLogger().println( ConfigurationRotator.LOGGERNAME + "Was older: " + current );
                        oldest = current;
                        chosen = config;
                    }
                    
                    /* Reset */
                    config.setChangedLast( false );

                } catch( Exception e ) {
                    /* No baselines found .get(0) above throws exception if no new baselines*/
                    listener.getLogger().println( ConfigurationRotator.LOGGERNAME + "No baselines found (exception caught) for component : " 
                            + config + ". Exception was: \n" + e.getMessage() );
                    logger.debug( "No baselines found: " + e.getMessage() );
                    ExceptionUtils.print( e, listener.getLogger(), true );
                    listener.getLogger().println( ConfigurationRotator.LOGGERNAME + "After Exceptionutils print");
                    // return null;
                    // DO not return here, then we will skip components in the foreach
                }
                
            }
        }
        
        /**/
        logger.debug( "chosen: " + chosen );
        logger.debug( "oldest: " + oldest );
        if( chosen != null && oldest != null ) {
            logger.debug( "There was a baseline: " + oldest );
            listener.getLogger().println( ConfigurationRotator.LOGGERNAME + "There was a baseline: " + oldest );
            chosen.setBaseline( oldest );
            chosen.setChangedLast( true );
        } else {
            listener.getLogger().println( ConfigurationRotator.LOGGERNAME + "No new baselines" );
            return null;
        }
        
        return nconfig;
    }
    
    public SnapshotView createView( TaskListener listener, AbstractBuild<?, ?> build, ClearCaseUCMConfiguration configuration, FilePath workspace, PVob pvob ) throws IOException, InterruptedException {
        Project project = null;

        logger.debug( "Getting project" );
        project = workspace.act( new DetermineProject( Arrays.asList( new String[] { "jenkins", "Jenkins", "hudson", "Hudson" } ), pvob ) );
        
        logger.debug( "Project is " + project );
        
        /* Create baselines list */
        List<Baseline> selectedBaselines = new ArrayList<Baseline>();
        logger.debug( "Selected baselines:" );
        for( ClearCaseUCMConfigurationComponent config : configuration.getList() ) {
            logger.debug( config.getBaseline().getNormalizedName() );
            selectedBaselines.add( config.getBaseline() );
        }
        
        /* Make a view tag*/
        String viewtag = "cr-" + build.getProject().getDisplayName().replaceAll( "\\s", "_" ) + "-" + System.getenv( "COMPUTERNAME" );
        
        return workspace.act( new PrepareWorkspace( project, selectedBaselines, viewtag, listener ) );
        
    }
    
    /**
     * Get the configuration as targets. If the project configuration is null, the last targets defined by the configuration page is returned otherwise the current project configuration is returned as targets
     * @return A list of targets
     */
    public List<ClearCaseUCMTarget> getTargets() {
        if( projectConfiguration != null ) {
            return getConfigurationAsTargets( projectConfiguration );
        } else {
            return targets;
        }
    }
    
    private List<ClearCaseUCMTarget> getConfigurationAsTargets( ClearCaseUCMConfiguration config ) {
        List<ClearCaseUCMTarget> list = new ArrayList<ClearCaseUCMTarget>();
        if( config.getList() != null && config.getList().size() > 0 ) {
            for( ClearCaseUCMConfigurationComponent c : config.getList() ) {
                if( c != null ) {
                    //list.add( new ClearCaseUCMTarget( c.getBaseline().getNormalizedName() + ", " + c.getPlevel().toString() + ", " + c.isFixed() ) );
                    list.add(new ClearCaseUCMTarget(c.getBaseline().getNormalizedName(), c.getPlevel(), c.isFixed()));
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
        ClearCaseUCMConfiguration c = action.getConfiguration(ClearCaseUCMConfiguration.class);
        if(c == null) {
            throw new AbortException( "Not a valid configuration" );
        } else {
            this.projectConfiguration = c;
            project.save();
        }
    }

    @Override
    public PollingResult poll( AbstractProject<?, ?> project, Launcher launcher, FilePath workspace, TaskListener listener, boolean reconfigure ) throws IOException, InterruptedException {
        PrintStream out = listener.getLogger();
        out.println( ConfigurationRotator.LOGGERNAME + "Polling" );

        ClearCaseUCMConfiguration configuration = null;
        if( projectConfiguration == null ) {
            if( reconfigure ) {
                try {
                    out.println( ConfigurationRotator.LOGGERNAME + "Project action was null and we need to reconfigure!" );
                    configuration = ClearCaseUCMConfiguration.getConfigurationFromTargets( getTargets(), workspace, listener );
                } catch( ConfigurationRotatorException e ) {
                    throw new IOException( "Unable to get configurations from targets", e );
                }
            } else {
                out.println( ConfigurationRotator.LOGGERNAME + "Project configuration was null, finding last action" );
                ConfigurationRotatorBuildAction action = getLastResult( project, ClearCaseUCM.class );
                
                if( action == null ) {
                    out.println( ConfigurationRotator.LOGGERNAME + "No previous actions, build now" );
                    return PollingResult.BUILD_NOW;
                }
                
                configuration = action.getConfiguration(ClearCaseUCMConfiguration.class);
            }
        } else {
            out.println( ConfigurationRotator.LOGGERNAME + "Project configuration was not null" );
            configuration = this.projectConfiguration;
        }    

        /* Only look ahead if the build was NOT reconfigured */
        if( configuration != null && !reconfigure ) {
            out.println( ConfigurationRotator.LOGGERNAME + "Configuration is not null and was not reconfigured" );
            try {
                ClearCaseUCMConfiguration other;
                other = nextConfiguration( listener, configuration, workspace );
                if( other != null ) {
                    printConfiguration( out, other );
                    return PollingResult.BUILD_NOW;
                // FIXME ? what if other is an old one ?
                } else {
                    out.println( ConfigurationRotator.LOGGERNAME + "No changes!" );
                    return PollingResult.NO_CHANGES;
                }
            } catch( ConfigurationRotatorException e ) {
                throw new IOException( "Unable to poll: " + e.getMessage(), e );
            } catch (Exception e) {
                out.println(ConfigurationRotator.LOGGERNAME + "Polling caught unhandled exception. Message was: " + e.getMessage());
                throw new IOException( "Polling caught unhandled exception! Message was: " + e.getMessage(), e );
            }
        }
        
        return PollingResult.BUILD_NOW;
    }

    @Override
    public void writeChangeLog(File f, BuildListener listener, AbstractBuild<?, ?> build) throws IOException, ConfigurationRotatorException, InterruptedException {
        PrintWriter writer = null;
        List<ClearCaseActivity> changes = new ArrayList<ClearCaseActivity>();
        //First obtain last succesful result
        ConfigurationRotatorBuildAction crbac = getLastResult(build.getProject(), this.getClass());
        
        //Special case: This is the first build
         if(crbac == null) {
            
        } else {
             List<AbstractConfigurationComponent> currentComponentList = null;
             ConfigurationRotatorBuildAction current = build.getAction(ConfigurationRotatorBuildAction.class);
             if(current != null)
                 currentComponentList = current.getConfiguration().getList();
             
             int compareIndex = -1;
             
             if(currentComponentList != null) {
                for(AbstractConfigurationComponent acc : currentComponentList) {
                    if(acc.isChangedLast()) {
                        compareIndex = currentComponentList.indexOf(acc);
                        break;
                    }
                }
             }
             
             //The compare is totally new. Else compare the previous component
             if(compareIndex == -1) {
                 
             } else {
                 if(currentComponentList.get(compareIndex) instanceof ClearCaseUCMConfigurationComponent) {
                    changes = build.getWorkspace().act(new ClearCaseGetBaseLineCompare(listener, current.getConfiguration(ClearCaseUCMConfiguration.class), crbac.getConfiguration(ClearCaseUCMConfiguration.class)));
                 }
             }
        }
        
        try {
            
            writer = new PrintWriter(new FileWriter(f));
            
            
            writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            writer.println("<changelog>");
            
            for(ClearCaseActivity a : changes) {
                writer.println("<activity>");
                writer.println(String.format("<author>%s</author>", a.getAuthor()));
                writer.println(String.format("<activityName>%s</activityName>", a.getActivityName()));
                writer.println("<versions>");
                for(ClearCaseVersion v : a.getVersions()) {
                    writer.println("<version>");
                    writer.println(String.format("<name>%s</name>", v.getName()));
                    writer.println(String.format("<file>%s</file>", v.getFile()));
                    writer.println(String.format("<user>%s</user>", v.getUser()));
                    writer.println("</version>");
                }
                writer.println("</versions>");
                writer.print("</activity>");
                
                
            }
            
            writer.println("</changelog>");
            
        
        } catch (IOException e) {
            listener.getLogger().println("Unable to create change log!" +e);
        } finally {
            writer.close();
        }
    }
    
    @Extension
    public static final class DescriptorImpl extends ConfigurationRotatorSCMDescriptor<ClearCaseUCM> {

        @Override
        public String getDisplayName() {
            return "ClearCase UCM";
        }
        
        public FormValidation doTest(  ) throws IOException, ServletException {
            return FormValidation.ok();
        }
         
        @Override
        public AbstractConfigurationRotatorSCM newInstance( StaplerRequest req, JSONObject formData, AbstractConfigurationRotatorSCM i ) throws FormException {
            ClearCaseUCM instance = (ClearCaseUCM)i;
            //Default to an empty configuration. When the plugin is first started this should be an empty list
            List<ClearCaseUCMTarget> targets = new ArrayList<ClearCaseUCMTarget>();
            
            
            try {                
                JSONArray obj = formData.getJSONObject( "acrs" ).getJSONArray( "targets" );
                targets = req.bindJSONToList( ClearCaseUCMTarget.class, obj );
            } catch (net.sf.json.JSONException jasonEx) {
                //This happens if the targets is not an array!
                JSONObject obj = formData.getJSONObject( "acrs" ).getJSONObject( "targets" );
                if(obj != null) {
                    ClearCaseUCMTarget target = req.bindJSON(ClearCaseUCMTarget.class, obj);
                    if(target != null && target.getBaselineName() != null && !target.getBaselineName().equals("")) {
                        targets.add(target);
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