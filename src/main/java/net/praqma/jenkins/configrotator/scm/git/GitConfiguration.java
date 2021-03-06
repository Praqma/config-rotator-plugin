package net.praqma.jenkins.configrotator.scm.git;

import hudson.FilePath;
import hudson.model.TaskListener;
import net.praqma.jenkins.configrotator.*;
import net.praqma.jenkins.configrotator.scm.ConfigRotatorChangeLogEntry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GitConfiguration extends AbstractConfiguration<GitConfigurationComponent> implements Cloneable {

    private static final Logger LOGGER = Logger.getLogger( GitConfiguration.class.getName() );

    private GitConfiguration() { super(); }

    public GitConfiguration( List<GitTarget> targets, FilePath workspace, TaskListener listener ) throws ConfigurationRotatorException {
        for( AbstractTarget t : targets ) {
            GitTarget target = (GitTarget)t;

            LOGGER.fine( String.format( "Getting component for %s", target ) );
            GitConfigurationComponent c = null;
            try {
                c = workspace.act( new ResolveConfigurationComponent( listener, target.getName(), target.getRepository(), target.getBranch(), target.getCommitId(), target.getFixed() ) );
                target.setCommitId(c.getCommitId());
            } catch( Exception e ) {
                LOGGER.log( Level.WARNING, "Whoops", e );
                throw new ConfigurationRotatorException( "Unable to get component for " + target, e );
            }

            LOGGER.fine( String.format( "Adding %s", c ) );
            list.add( c );
        }
    }

    public void checkout( FilePath workspace, TaskListener listener ) throws IOException, InterruptedException {
        for( GitConfigurationComponent c : getList() ) {
            c.checkout( workspace, listener );
        }
    }

    @Override
    public int hashCode() {
        final int prime = 5;
        int result = 1;
        result = result * prime + list.hashCode();
        return result;
    }

    @Override
    public boolean equals( Object other ) {
        if( other == this ) {
            return true;
        }

        if( other instanceof GitConfiguration ) {
            GitConfiguration o = (GitConfiguration) other;
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
            return true;
        }
    }

    @Override
    public List<ConfigRotatorChangeLogEntry> difference( GitConfigurationComponent component, GitConfigurationComponent other ) throws ConfigurationRotatorException {
        return null;
    }

    @Override
    public GitConfiguration clone() throws CloneNotSupportedException {
        GitConfiguration n = (GitConfiguration)super.clone();
        n.list = new ArrayList<>(this.list);
        return n;
    }

    @Override
    public String toHtml() {
        StringBuilder builder = new StringBuilder();
        return basicHtml( builder, "Repository", "Branch", "Commit", "Fixed" );
    }
}
