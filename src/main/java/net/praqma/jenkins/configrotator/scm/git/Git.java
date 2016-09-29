package net.praqma.jenkins.configrotator.scm.git;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import net.praqma.jenkins.configrotator.*;
import net.praqma.jenkins.configrotator.scm.ConfigRotatorChangeLogEntry;
import net.praqma.jenkins.configrotator.scm.ConfigRotatorChangeLogParser;
import org.eclipse.jgit.revwalk.RevCommit;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.servlet.ServletException;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.praqma.jenkins.configrotator.scm.contribute.ConfigRotatorCompatabilityConverter;

public class Git extends AbstractConfigurationRotatorSCM implements Serializable {

    private static final Logger LOGGER = Logger.getLogger( Git.class.getName() );

    private List<GitTarget> targets = new ArrayList<>();

    @DataBoundConstructor
    public Git(List<GitTarget> targets) {
        this.targets = targets;
    }

    @Override
    public String getName() {
        return "Git repository";
    }

    @Override
    public Poller getPoller( AbstractProject<?, ?> project, Launcher launcher, FilePath workspace, TaskListener listener ) {
        return new Poller(project, launcher, workspace, listener );
    }

    @Override
    public Performer getPerform( AbstractBuild<?, ?> build, Launcher launcher, FilePath workspace, BuildListener listener ) throws IOException {
        return new GitPerformer(build, launcher, workspace, listener);
    }

    @Override
    public ConfigRotatorCompatabilityConverter getConverter() {
        return null;
    }

    public class GitPerformer extends Performer<GitConfiguration> {

        public GitPerformer( AbstractBuild<?, ?> build, Launcher launcher, FilePath workspace, BuildListener listener ) {
            super( build, launcher, workspace, listener );
        }

        @Override
        public GitConfiguration getInitialConfiguration() throws ConfigurationRotatorException {
            return new GitConfiguration( getTargets(), workspace, listener );
        }

        @Override
        public GitConfiguration getNextConfiguration( ConfigurationRotatorBuildAction action ) throws ConfigurationRotatorException {
            GitConfiguration oldconfiguration = action.getConfiguration();
            return (GitConfiguration) nextConfiguration(listener, oldconfiguration, workspace );
        }

        @Override
        public void checkConfiguration( GitConfiguration configuration ) {
            /* TODO: implement */
        }

        @Override
        public void createWorkspace( GitConfiguration configuration ) throws IOException, InterruptedException {
            configuration.checkout( workspace, listener );
        }

        @Override
        public void print( GitConfiguration configuration ) {
            /* TODO: implement */
        }
    }


    @Override
    public void setConfigurationByAction( AbstractProject<?, ?> project, ConfigurationRotatorBuildAction action ) throws IOException {
        GitConfiguration c = action.getConfiguration();
        if( c == null ) {
            throw new AbortException( ConfigurationRotator.LOGGERNAME + "Not a valid configuration" );
        } else {
            this.projectConfiguration = c;
            project.save();
        }
    }

    @Override
    public boolean wasReconfigured( AbstractProject<?, ?> project ) {
        ConfigurationRotatorBuildAction action = getLastResult( project, Git.class );

        if( action == null ) {
            return true;
        }

        GitConfiguration configuration = action.getConfiguration();

        /* Check if the project configuration is even set */
        if( configuration == null ) {
            LOGGER.fine( "Configuration was null" );
            return true;
        }

        /* Check if the sizes are equal */
        if( targets.size() != configuration.getList().size() ) {
            LOGGER.fine( "Size was not equal" );
            return true;
        }

        /**/
        for( int i = 0; i < targets.size(); ++i ) {
            GitTarget t = targets.get( i );
            GitConfigurationComponent c = configuration.getList().get( i );
            if( !t.getBranch().equals( c.getBranch()) ||
                !t.getRepository().equals( c.getRepository() ) ||
                !t.getCommitId().equals( c.getCommitId() )) {
                LOGGER.finer( "Configuration was not equal" );
                return true;
            }
        }

        return false;
    }

    @Override
    public ConfigRotatorChangeLogParser createChangeLogParser() {
        return new ConfigRotatorChangeLogParser();
    }

    @Override
    public ChangeLogWriter getChangeLogWriter( File changeLogFile, BuildListener listener, AbstractBuild<?, ?> build ) {
        return new GitChangeLogWriter(changeLogFile, listener, build);
    }

    public class GitChangeLogWriter extends ChangeLogWriter<GitConfigurationComponent, GitConfiguration> {

        public GitChangeLogWriter( File changeLogFile, BuildListener listener, AbstractBuild<?, ?> build ) {
            super( changeLogFile, listener, build );
        }

        @Override
        protected List<ConfigRotatorChangeLogEntry> getChangeLogEntries( GitConfiguration configuration, GitConfigurationComponent configurationComponent ) throws ConfigurationRotatorException {
            LOGGER.fine( "Change log entry, " + configurationComponent );
            try {
                ConfigRotatorChangeLogEntry entry = build.getWorkspace().act( new ResolveChangeLog( configurationComponent.getName(), configurationComponent.getCommitId() ) );
                LOGGER.fine("ENTRY: " + entry);
                return Collections.singletonList( entry );
            } catch( Exception e ) {
                throw new ConfigurationRotatorException( "Unable to resolve changelog " + configurationComponent.getCommitId(), e );
            }
        }
    }

    @Override
    public AbstractConfiguration nextConfiguration( TaskListener listener, AbstractConfiguration configuration, FilePath workspace ) throws ConfigurationRotatorException {
        LOGGER.fine( "Getting next Git configuration: " + configuration);

        RevCommit oldest = null;
        GitConfigurationComponent chosen = null;
        GitConfiguration nconfig = ((GitConfiguration) configuration).clone();

        /* Find oldest commit, newer than current */
        for( GitConfigurationComponent config : nconfig.getList() ) {
            if( !config.isFixed() ) {
                try {
                    LOGGER.fine("Config: " + config);
                    RevCommit commit = workspace.act( new ResolveNextCommit( config.getName(), config.getCommitId() ) );
                    if( commit != null ) {
                        LOGGER.fine( "Current commit: " + commit.getName() );
                        LOGGER.fine( "Current commit: " + commit.getCommitTime() );
                        if( oldest != null ) {
                            LOGGER.fine( "Oldest  commit: " + oldest.getName() );
                            LOGGER.fine( "Oldest  commit: " + oldest.getCommitTime() );
                        }
                        if( oldest == null || commit.getCommitTime() < oldest.getCommitTime() ) {
                            oldest = commit;
                            chosen = config;
                        }

                        config.setChangedLast( false );
                    }

                } catch( Exception e ) {
                    LOGGER.log( Level.FINE, "No commit found", e );
                }

            }
        }

        LOGGER.fine( "Configuration component: " + chosen );
        LOGGER.fine( "Oldest valid commit: " + oldest );
        if( chosen != null && oldest != null ) {
            LOGGER.fine( "There was a new commit: " + oldest );
            listener.getLogger().println( ConfigurationRotator.LOGGERNAME + "Next commit: " + chosen );
            chosen.setCommitId( oldest.getName() );
            chosen.setChangedLast( true );
        } else {
            listener.getLogger().println( ConfigurationRotator.LOGGERNAME + "No new commits" );
            LOGGER.fine( "No new commits" );
            return null;
        }

        return nconfig;
    }


    private List<GitTarget> getConfigurationAsTargets( GitConfiguration config ) {
        List<GitTarget> list = new ArrayList<GitTarget>();
        if( config.getList() != null && config.getList().size() > 0 ) {
            for( GitConfigurationComponent c : config.getList() ) {
                if( c != null ) {
                    list.add( new GitTarget( c.getName(), c.getRepository(), c.getBranch(), c.getCommitId(), c.isFixed() ) );
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
    public <TT extends AbstractTarget> void setTargets( List<TT> targets ) {
        this.targets = (List<GitTarget>) targets;
    }

    @Override
    public List<GitTarget> getTargets() {
        if( projectConfiguration != null ) {
            return getConfigurationAsTargets( (GitConfiguration) projectConfiguration );
        } else {
            return targets;
        }
    }


    @Extension
    public static final class DescriptorImpl extends ConfigurationRotatorSCMDescriptor<Git> {

        @Override
        public String getDisplayName() {
            return "Git Repositories";
        }

        @Override
        public String getFeedComponentName() {
            return Git.class.getSimpleName();
        }

        public FormValidation doTest(  ) throws IOException, ServletException {
            return FormValidation.ok();
        }

        public List<GitTarget> getTargets( Git instance ) {
            if( instance == null ) {
                return new ArrayList<GitTarget>();
            } else {
                return instance.getTargets();
            }
        }
    }
}
