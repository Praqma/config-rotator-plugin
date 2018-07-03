package net.praqma.jenkins.configrotator;

import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.Publisher;
import hudson.util.DescribableList;
import net.praqma.jenkins.configrotator.fake.FakeSCM;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ConfigurationRotatorTest {

    /* Typical jenkins objects */
    AbstractProject<?, ?> project;
    AbstractBuild<?, ?> build;
    Launcher launcher;
    TaskListener tasklistener;
    BuildListener buildlistener;
    FilePath workspace = new FilePath( new File( "" ) );

    FakeSCM scm = new FakeSCM();
    ConfigurationRotator cr = new ConfigurationRotator( scm );
    ConfigurationRotator spy;

    List<Publisher> publishers = new ArrayList<Publisher>();


    @Before
    public void initialize() throws IOException {
        project = Mockito.mock( FreeStyleProject.class );
        build = PowerMockito.mock( FreeStyleBuild.class );
        launcher = Mockito.mock( Launcher.class );
        tasklistener = Mockito.mock( TaskListener.class );
        buildlistener = Mockito.mock( BuildListener.class );

        /* Behaviour */
        Mockito.when( tasklistener.getLogger() ).thenReturn( System.out );
        Mockito.when( buildlistener.getLogger() ).thenReturn( System.out );

        /* Saving behaviour */
        Mockito.doReturn( project ).when( build ).getProject();
        Mockito.doNothing().when( project ).save();

        /* Adding publisher behaviour */
        Mockito.doReturn( project ).when( build ).getParent();
        List<Publisher> t = new ArrayList<Publisher>();
        Mockito.doReturn( new DescribableList<Publisher,Descriptor<Publisher>>( null, t ) ).when( project ).getPublishersList();
        spy = Mockito.spy( cr );
        Mockito.doNothing().when( spy ).ensurePublisher( build );
    }

    @Test
    public void checkoutTestInitial() throws IOException, InterruptedException {
        scm.setLastActionAsValid( false );
        boolean result = spy.checkout( build, launcher, workspace, buildlistener, null );
        assertThat( result, is( true ) );
    }

    @Test( expected = AbortException.class )
    public void checkoutTestInitialNull() throws IOException, InterruptedException {
        scm.setInitialConfigurationAsValid( false ).setLastActionAsValid( false );
        boolean result = spy.checkout( build, launcher, workspace, buildlistener, null );
        assertThat( result, is( false ) );
    }

    @Test
    public void checkoutTestNext() throws IOException, InterruptedException {
        boolean result = spy.checkout( build, launcher, workspace, buildlistener, null );

        assertThat( result, is( true ) );
    }
}
