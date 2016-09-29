package net.praqma.jenkins.configrotator.scm.clearcaseucm;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import java.io.File;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.praqma.clearcase.exceptions.*;
import net.praqma.clearcase.ucm.entities.Activity;
import net.praqma.clearcase.ucm.entities.Version;
import net.praqma.clearcase.ucm.view.SnapshotView;
import net.praqma.jenkins.configrotator.*;
import net.praqma.jenkins.configrotator.scm.ConfigRotatorChangeLogEntry;
import net.praqma.jenkins.configrotator.scm.ConfigRotatorVersion;
import org.eclipse.jgit.util.StringUtils;

public class ClearCaseUCMConfiguration extends AbstractConfiguration<ClearCaseUCMConfigurationComponent> {

    private static final Logger logger = Logger.getLogger( ClearCaseUCMConfiguration.class.getName() );
    private SnapshotView view;

    public ClearCaseUCMConfiguration() { }



    @Override
    public ClearCaseUCMConfiguration clone() {
        ClearCaseUCMConfiguration n = new ClearCaseUCMConfiguration();

        for( ClearCaseUCMConfigurationComponent cc : this.list ) {
            n.list.add( cc.clone() );
        }

        return n;
    }

    public ClearCaseUCMConfiguration( List<ClearCaseUCMConfigurationComponent> list ) {
        this.list = list;
    }


    public void setView( SnapshotView view ) {
        this.view = view;
    }

    public SnapshotView getView() {
        return view;
    }

    public static ClearCaseUCMConfiguration getConfigurationFromTargets( List<ClearCaseUCMTarget> targets, FilePath workspace, TaskListener listener ) throws ConfigurationRotatorException {
        PrintStream out = listener.getLogger();

        /**/
        ClearCaseUCMConfiguration configuration = new ClearCaseUCMConfiguration();

        /* Each line is component, stream, baseline, plevel, type */
        for( ClearCaseUCMTarget target : targets ) {
            final String[] units = target.getComponent().split( "," );

            if( units.length == 3 ) {
                try {
                    ClearCaseUCMConfigurationComponent config = workspace.act( new GetConfiguration( units, listener ) );
                    configuration.list.add( config );
                    out.println( ConfigurationRotator.LOGGERNAME + "Parsed configuration: " + config );
                } catch( InterruptedException e ) {
                    out.println( ConfigurationRotator.LOGGERNAME + "Error parsing configuration - interrupted: " + e.getMessage() );
                    throw new ConfigurationRotatorException( "Unable parse configuration - interrupted", e );
                } catch( IOException ioe ) {
                    ConfigurationRotatorException configurationRotatorException = new ConfigurationRotatorException( String.format( "Failed to retrieve configuration%n%s", ioe.getCause().getMessage()) );
                    throw configurationRotatorException;
                }
            } else {
                out.println( ConfigurationRotator.LOGGERNAME + "\"" + target.getComponent() + "\" was not correct" );
                throw new ConfigurationRotatorException( "Wrong input, length is " + units.length );
            }
        }

        return configuration;
    }

    @Override
    public String toString() {
        return list.toString();
    }

    @Override
    public boolean equals( Object other ) {
        if( other == this ) {
            return true;
        }

        if( other instanceof ClearCaseUCMConfiguration ) {
            ClearCaseUCMConfiguration o = (ClearCaseUCMConfiguration) other;

            /* Check size */
            if( o.getList().size() != list.size() ) {
                return false;
            }

            /* Check elements, the size is identical */
            for( int i = 0; i < list.size(); ++i ) {
                if( !o.list.get( i ).equals( list.get( i ) ) ) {
                    return false;
                }
            }

            /* Everything is ok */
            return true;
        } else {
            /* Not same type */
            return false;
        }


    }

    @Override
    public String toHtml() {
        StringBuilder builder = new StringBuilder();

        builder.append( "<table border=\"0\" style=\"text-align:left;\">" );
        builder.append( "<thead>" );
        builder.append( "<th>" ).append( "Component" ).append( "</th>" );
        builder.append( "<th>" ).append( "Stream" ).append( "</th>" );
        builder.append( "<th>" ).append( "Baseline" ).append( "</th>" );
        builder.append( "<th>" ).append( "Promotion level" ).append( "</th>" );
        builder.append( "<th>" ).append( "Fixed" ).append( "</th>" );

        for( ClearCaseUCMConfigurationComponent comp : getList() ) {
            builder.append( comp.toHtml() );
        }

        builder.append( "</thead>" );
        builder.append( "</table>" );
        return builder.toString();
    }

    public static String itemizeForHtml(List<? extends AbstractConfigurationComponent> previousComponents, List<? extends AbstractConfigurationComponent> changedComponents, List<Integer> changedIndexes) {
        if(changedIndexes.isEmpty()) {
            return "New Configuration - no changes yet";
        } else {
            List<String> stringChanges = new ArrayList<String>();
            for(Integer i : changedIndexes) {
                String currentBaseline = ((ClearCaseUCMConfigurationComponent)changedComponents.get(i)).getBaseline().getNormalizedName();
                String previousBaseline = ((ClearCaseUCMConfigurationComponent)previousComponents.get(i)).getBaseline().getNormalizedName();
                stringChanges.add(String.format("Baseline changed from %s to %s", previousBaseline, currentBaseline));
            }
            return StringUtils.join(stringChanges, "<br/>");
        }
    }

    public static String itemizeForHtml(ConfigurationRotatorBuildAction action ) {
        ConfigurationRotator rotator = (ConfigurationRotator)action.getBuild().getProject().getScm();

        //Arg1 previous configuration:
        List<? extends AbstractConfigurationComponent> listArg1 = new ArrayList<AbstractConfigurationComponent>();
        ConfigurationRotatorBuildAction crba = rotator.getAcrs().getPreviousResult(action.getBuild(), null);
        if(crba!=null) {
            listArg1 = crba.getConfiguration().getList();
        }
        //Arg2 current components
        AbstractConfiguration ac = action.getConfiguration();
        List<? extends AbstractConfigurationComponent> listArg2 = ac != null ? ac.getList() : new ArrayList<AbstractConfigurationComponent>();

        //Arg3 list of indexed changes
        List<Integer> listArg3 = ac != null ? ac.getChangedComponentIndecies() : new ArrayList<Integer>();
        return ClearCaseUCMConfiguration.itemizeForHtml(listArg1, listArg2, listArg3);
    }

    public String getDescription( ConfigurationRotatorBuildAction action ) {
        /**
         * Ensure backwards compatability
         */
        if(description == null) {
            return ClearCaseUCMConfiguration.itemizeForHtml(action);
        }
        return description;
    }

    @Override
    public List<ConfigRotatorChangeLogEntry> difference( ClearCaseUCMConfigurationComponent component, ClearCaseUCMConfigurationComponent other ) throws ConfigurationRotatorException {
        List<ConfigRotatorChangeLogEntry> entries = new LinkedList<ConfigRotatorChangeLogEntry>();

        try {
            List<Activity> activities = Version.getBaselineDiff( component.getBaseline(), ( other != null ? other.getBaseline() : null ), false, new File( getView().getPath() ) );
            for( Activity a : activities ) {
                ConfigRotatorChangeLogEntry entry = new ConfigRotatorChangeLogEntry( a.getHeadline(), a.getUser(), new ArrayList<ConfigRotatorVersion>() );
                for( Version v : a.changeset.versions ) {
                    entry.addVersion( new ConfigRotatorVersion( v.getSFile(), v.getVersion(), a.getUser() ) );
                }

                entries.add( entry );
            }
        } catch( ClearCaseException e ) {
            logger.log( Level.WARNING, "Unable to generate change log entries", e );
        }
        return entries;
    }
}