package net.praqma.jenkins.configrotator;

import net.praqma.jenkins.configrotator.scm.ConfigRotatorChangeLogEntry;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractConfiguration<T extends AbstractConfigurationComponent> implements Serializable {
    public abstract List<ConfigRotatorChangeLogEntry> difference( T component, T other ) throws ConfigurationRotatorException;

    protected List<T> list = new ArrayList<T>();

    protected String description = null;


    public String getView( Class<?> clazz ) {
        return clazz.getName().replace( '.', '/' ).replace( '$', '/' ) + "/" + "cr.jelly";
    }

    public List<AbstractConfigurationComponent> getChangedComponents() {
        List<AbstractConfigurationComponent> changedComponents = new ArrayList<AbstractConfigurationComponent>();
        for( AbstractConfigurationComponent configuration : this.getList() ) {
            if( configuration.isChangedLast() ) {
                changedComponents.add(configuration);
            }
        }
        return changedComponents;
    }

    /**
     * Gets the index of the changed component.
     *
     * @return the index of the changed component. If there is no changed component default return value is -1
     */
    public List<Integer> getChangedComponentIndecies() {
        List<Integer> indicies = new ArrayList<Integer>();
        List<? extends AbstractConfigurationComponent> l = this.getList();
        for( AbstractConfigurationComponent configuration : l ) {
            if( configuration.isChangedLast() ) {
                int i = l.indexOf( configuration );
                indicies.add(i);                
            }
        }

        return indicies;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + list + "]";
    }

    public List<T> getList() {
        return list;
    }

    public abstract String toHtml();

    
    public String getDescription( ConfigurationRotatorBuildAction action ) {
        if( description == null ) {
            ConfigurationRotator rotator = (ConfigurationRotator) action.getBuild().getProject().getScm();
            if( getChangedComponents().isEmpty() ) {
                return "New Configuration - no changes yet";
            } else {
                ConfigurationRotatorBuildAction previous = rotator.getAcrs().getPreviousResult( action.getBuild(), null );
                List<Integer> changes = getChangedComponentIndecies();
                List<AbstractConfigurationComponent> changedComps = getChangedComponents();
                
                StringBuilder builder = new StringBuilder();
                for(Integer i : changes) {
                   String c = String.format( "%s<br/>%s%n", ((T)previous.getConfigurationWithOutCast().getList().get( i) ).prettyPrint(), changedComps.get(i).prettyPrint() );
                   builder.append(c);
                }
                
                return builder.toString();
            }
        }

        return description;
    }
    

    public String basicHtml( StringBuilder builder, String ... titles ) {

        builder.append( "<table style=\"text-align:left;border-solid:hidden;border-collapse:collapse;\">" );
        builder.append( "<thead>" );
        for( String title : titles ) {
            builder.append( "<th style=\"padding-right:15px\">" ).append( title ).append( "</th>" );
        }
        builder.append( "</thead>" );

        builder.append( "<tbody>" );
        for( T comp : getList() ) {
            builder.append( comp.toHtml() );
        }
        builder.append( "</tbody>" );

        builder.append( "</table>" );
        return builder.toString();
    }
}
