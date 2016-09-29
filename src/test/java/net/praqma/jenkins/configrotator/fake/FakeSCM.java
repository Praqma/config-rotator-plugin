package net.praqma.jenkins.configrotator.fake;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import net.praqma.jenkins.configrotator.*;
import net.praqma.jenkins.configrotator.scm.ConfigRotatorChangeLogParser;

import java.io.File;
import java.io.IOException;
import java.util.List;
import net.praqma.jenkins.configrotator.scm.contribute.ConfigRotatorCompatabilityConverter;

public class FakeSCM extends AbstractConfigurationRotatorSCM {
    
    @Override
    public String getName() {
        return "fakeSCM";
    }

    @Override
    public ConfigRotatorCompatabilityConverter getConverter() {
        return null;
    }
    
    @Override
    public <TT extends AbstractTarget> void setTargets( List<TT> targets ) {
    }

    @Override
    public AbstractConfiguration nextConfiguration( TaskListener listener, AbstractConfiguration configuration, FilePath workspace ) throws ConfigurationRotatorException {
        return null;
    }

    @Override
    public Poller getPoller( AbstractProject<?, ?> project, Launcher launcher, FilePath workspace, TaskListener listener ) {
        return new Poller( project, launcher, workspace, listener );
    }

    @Override
    public Performer getPerform( AbstractBuild<?, ?> build, Launcher launcher, FilePath workspace, BuildListener listener ) throws IOException {
        return new FakePerformer(build, launcher, workspace, listener);
    }

    public class FakePerformer extends Performer<FakeConfiguration> {

        public FakePerformer( AbstractBuild<?, ?> build, Launcher launcher, FilePath workspace, BuildListener listener ) {
            super( build, launcher, workspace, listener );
        }

        @Override
        public FakeConfiguration getInitialConfiguration() throws ConfigurationRotatorException, IOException {
            if( initialConfigurationIsValid ) {
                listener.getLogger().println( "VALID" );
                return new FakeConfiguration();
            } else {
                listener.getLogger().println( "INVALID" );
                return null;
            }
        }

        @Override
        public FakeConfiguration getNextConfiguration( ConfigurationRotatorBuildAction action ) throws ConfigurationRotatorException {
            if( nextConfigurationIsValid ) {
                return new FakeConfiguration();
            } else {
                return null;
            }
        }

        @Override
        public void checkConfiguration( FakeConfiguration configuration ) throws ConfigurationRotatorException {
            /* No op */
        }

        @Override
        public void createWorkspace( FakeConfiguration configuration ) throws ConfigurationRotatorException {
            /* No op */
        }

        @Override
        public void print( FakeConfiguration configuration ) {
            //To change body of implemented methods use File | Settings | File Templates.
        }
    }

    /* Initial configuration */
    private boolean initialConfigurationIsValid = true;
    public FakeSCM setInitialConfigurationAsValid( boolean valid ) {
        this.initialConfigurationIsValid = valid;

        return this;
    }

    /* Next configuration */
    private boolean nextConfigurationIsValid = true;
    public FakeSCM setNextConfigurationAsValid( boolean valid ) {
        this.nextConfigurationIsValid = valid;

        return this;
    }

    /* Last action */
    private boolean lastActionIsValid = true;
    public FakeSCM setLastActionAsValid( boolean valid ) {
        this.lastActionIsValid = valid;

        return this;
    }

    @Override
    public ConfigurationRotatorBuildAction getLastResult( AbstractProject<?, ?> project, Class<? extends AbstractConfigurationRotatorSCM> clazz ) {
        if( this.lastActionIsValid ) {
            return new ConfigurationRotatorBuildAction( null, clazz, null );
        } else {
            return null;
        }
    }

    @Override
    public void setConfigurationByAction( AbstractProject<?, ?> project, ConfigurationRotatorBuildAction action ) throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean wasReconfigured( AbstractProject<?, ?> project ) {
        if(project.getLastBuild() == null) {
            return true;
        }
        return project.getLastBuild().getNumber() == 1;
    }

    @Override
    public ConfigRotatorChangeLogParser createChangeLogParser() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public <TT extends AbstractTarget> List<TT> getTargets() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ChangeLogWriter getChangeLogWriter( File changeLogFile, BuildListener listener, AbstractBuild<?, ?> build ) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }



}
