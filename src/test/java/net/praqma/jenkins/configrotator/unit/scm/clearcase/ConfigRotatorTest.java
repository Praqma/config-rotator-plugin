package net.praqma.jenkins.configrotator.unit.scm.clearcase;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import net.praqma.jenkins.configrotator.scm.clearcaseucm.ClearCaseUCM;
import net.praqma.jenkins.configrotator.scm.clearcaseucm.ClearCaseUCMConfiguration;
import net.praqma.jenkins.configrotator.scm.clearcaseucm.ClearCaseUCMConfigurationComponent;
import net.praqma.jenkins.configrotator.scm.clearcaseucm.ClearCaseUCMTarget;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import junit.framework.TestCase;
import net.praqma.clearcase.exceptions.UnableToInitializeEntityException;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Component;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.ucm.entities.Project.PromotionLevel;
import net.praqma.jenkins.configrotator.ConfigurationRotatorException;
import net.praqma.jenkins.configrotator.ConfigurationRotator.ResultType;
import net.praqma.jenkins.configrotator.ConfigurationRotatorBuildAction;
import net.praqma.util.debug.Logger;
import net.praqma.util.debug.Logger.LogLevel;
import net.praqma.util.debug.appenders.Appender;
import net.praqma.util.debug.appenders.ConsoleAppender;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith( PowerMockRunner.class )
@PrepareForTest( ClearCaseUCMConfiguration.class )
public class ConfigRotatorTest extends TestCase {
	
	static {
		Appender appender = new ConsoleAppender();
		appender.setMinimumLevel( LogLevel.DEBUG );
		Logger.addAppender( appender );
	}
	
	/* Typical jenkins objects */
	AbstractProject<?, ?> project;
	AbstractBuild<?, ?> build;
	Launcher launcher;
	TaskListener tasklistener;
	BuildListener buildlistener;
	FilePath workspace = new FilePath( new File( "" ) );
	
	@Before
	public void initialize() {
		project = Mockito.mock( FreeStyleProject.class );
		build = PowerMockito.mock( FreeStyleBuild.class );
		launcher = Mockito.mock( Launcher.class );
		tasklistener = Mockito.mock( TaskListener.class );
		buildlistener = Mockito.mock( BuildListener.class );
		
		/* Behaviour */
		Mockito.when( tasklistener.getLogger() ).thenReturn( System.out );
		Mockito.when( buildlistener.getLogger() ).thenReturn( System.out );
	}




	/**
         * We have here a test wherein an action has a null configuration. This should never happen, we should throw an assertionError
         * @throws IOException
         * @throws InterruptedException
         * @throws ConfigurationRotatorException 
         */
	@Test(expected = AssertionError.class)
	public void testWasReconfigured() throws IOException, InterruptedException, ConfigurationRotatorException {
		ClearCaseUCM ccucm = new ClearCaseUCM( "" );
		ClearCaseUCM spy = Mockito.spy( ccucm );
		
		/* ClearCase mock */
		ConfigurationRotatorBuildAction action = new ConfigurationRotatorBuildAction( build, ClearCaseUCM.class, null );
		
		/* Behaviour */
		Mockito.doReturn( action ).when( spy ).getLastResult( project, ClearCaseUCM.class );
		
		boolean b = spy.wasReconfigured( project );
	}
	
	@Test
	public void testWasReconfiguredSize() throws IOException, InterruptedException, ConfigurationRotatorException {
		ClearCaseUCM ccucm = new ClearCaseUCM( "" );
		List<ClearCaseUCMTarget> targets = new ArrayList<ClearCaseUCMTarget>();
		targets.add( new ClearCaseUCMTarget( "bl1", PromotionLevel.INITIAL, false ) );
		ccucm.targets = targets;
		ClearCaseUCM spy = Mockito.spy( ccucm );
		
		/* ClearCase mock */
		ClearCaseUCMConfiguration ccc = Mockito.mock( ClearCaseUCMConfiguration.class );
		ConfigurationRotatorBuildAction action = new ConfigurationRotatorBuildAction( build, ClearCaseUCM.class, ccc );
		
		/* Behaviour */
		Mockito.doReturn( action ).when( spy ).getLastResult( project, ClearCaseUCM.class );
                Mockito.doReturn( new ArrayList<ClearCaseUCMTarget>() ).when( spy ).getCompareTargets(project);
		//Mockito.doReturn( new ArrayList<ClearCaseUCMConfigurationComponent>() ).when( ccc ).getList();
		
		boolean b = spy.wasReconfigured( project );
		
		assertEquals( true, b );
	}
	
	@Test
	public void testWasReconfiguredNotSame() throws IOException, InterruptedException, ConfigurationRotatorException, UnableToInitializeEntityException {
		ClearCaseUCM ccucm = new ClearCaseUCM( "" );
		List<ClearCaseUCMTarget> targets = new ArrayList<ClearCaseUCMTarget>();
		targets.add( new ClearCaseUCMTarget( "bl1", PromotionLevel.INITIAL, false ) );
		ccucm.targets = targets;
		ClearCaseUCM spy = Mockito.spy( ccucm );
		
		/* ClearCase mock */
		List<ClearCaseUCMConfigurationComponent> comps = new ArrayList<ClearCaseUCMConfigurationComponent>();
		comps.add( new ClearCaseUCMConfigurationComponent( Baseline.get( "bl2@\\pvob" ), PromotionLevel.INITIAL, false ) );
		ClearCaseUCMConfiguration ccc = new ClearCaseUCMConfiguration( comps );
		ConfigurationRotatorBuildAction action = new ConfigurationRotatorBuildAction( build, ClearCaseUCM.class, ccc );
		
		/* Behaviour */
		Mockito.doReturn( action ).when( spy ).getLastResult( project, ClearCaseUCM.class );
		
		boolean b = spy.wasReconfigured( project );
		
		assertEquals( true, b );
	}
	
	@Test
	public void testWasReconfiguredSame() throws IOException, InterruptedException, ConfigurationRotatorException, UnableToInitializeEntityException {
		ClearCaseUCM ccucm = new ClearCaseUCM( "" );
		List<ClearCaseUCMTarget> targets = new ArrayList<ClearCaseUCMTarget>();
		targets.add( new ClearCaseUCMTarget( "bl1@\\pvob", PromotionLevel.INITIAL, false ) );
		ccucm.targets = targets;
		ClearCaseUCM spy = Mockito.spy( ccucm );
		
		/* ClearCase mock */
		List<ClearCaseUCMConfigurationComponent> comps = new ArrayList<ClearCaseUCMConfigurationComponent>();
		comps.add( new ClearCaseUCMConfigurationComponent( Baseline.get( "bl1@\\pvob" ), PromotionLevel.INITIAL, false ) );
		ClearCaseUCMConfiguration ccc = new ClearCaseUCMConfiguration( comps );
		ConfigurationRotatorBuildAction action = new ConfigurationRotatorBuildAction( build, ClearCaseUCM.class, ccc );
		
		/* Behaviour */
		Mockito.doReturn( action ).when( spy ).getLastResult( project, ClearCaseUCM.class );
		
		boolean b = spy.wasReconfigured( project );
		
		assertEquals( false, b );
	}

	


	@Test( expected=AbortException.class )
	public void testReconfigureFails() throws IOException, InterruptedException, ConfigurationRotatorException, UnableToInitializeEntityException {
		ClearCaseUCM ccucm = new ClearCaseUCM( "" );
		ClearCaseUCM spy = Mockito.spy( ccucm );

        PowerMockito.mockStatic( ClearCaseUCMConfiguration.class );
        PowerMockito.when( ClearCaseUCMConfiguration.getConfigurationFromTargets( Mockito.anyListOf( ClearCaseUCMTarget.class ), Mockito.any( FilePath.class ), Mockito.any( TaskListener.class) ) ).thenThrow( new ConfigurationRotatorException( "Failing reconfigure" ) );

		spy.reconfigure( workspace, tasklistener );
		
		fail();
	}
	
	/**
	 * Tests fb case 6168, no new baselines starts an execution.<br>
	 * Originally, the getLastResult method would return build #01, when it should return build #02.<br>
	 * This is due to the fact, that we fail a build not having a build action(no bew baselines).
	 */
	@Test
	public void testGetLastResult() {
		ClearCaseUCM ccucm = new ClearCaseUCM( "" );
		
		/* Initialize builds */
		/* 01: compatible */
		AbstractBuild<?, ?> build01 = Mockito.mock( AbstractBuild.class );
		ConfigurationRotatorBuildAction action01 = new ConfigurationRotatorBuildAction( build01, ClearCaseUCM.class, new ClearCaseUCMConfiguration() );
		action01.setResult( ResultType.COMPATIBLE );
		Mockito.doReturn( action01 ).when( build01 ).getAction( ConfigurationRotatorBuildAction.class );
		Mockito.doReturn( "01" ).when( build01 ).toString();
		Mockito.doReturn( null ).when( build01 ).getPreviousNotFailedBuild();
		
		/* 02: incompatible */
		AbstractBuild<?, ?> build02 = Mockito.mock( AbstractBuild.class );
		Mockito.doReturn( build01 ).when( build02 ).getPreviousBuild();
		ConfigurationRotatorBuildAction action02 = new ConfigurationRotatorBuildAction( build02, ClearCaseUCM.class, new ClearCaseUCMConfiguration() );
		action02.setResult( ResultType.INCOMPATIBLE );
		Mockito.doReturn( action02 ).when( build02 ).getAction( ConfigurationRotatorBuildAction.class );
		Mockito.doReturn( "02" ).when( build02 ).toString();
		Mockito.doReturn( build01 ).when( build02 ).getPreviousNotFailedBuild();
		
		/* 02: failed, no action(no baselines) */
		AbstractBuild<?, ?> build03 = Mockito.mock( AbstractBuild.class );
		Mockito.doReturn( build02 ).when( build03 ).getPreviousBuild();
		Mockito.doReturn( "03" ).when( build03 ).toString();
		Mockito.doReturn( build01 ).when( build03 ).getPreviousNotFailedBuild();
		
		/* Initialize project */
		AbstractProject<?, ?> project = Mockito.mock( AbstractProject.class );
		Mockito.doReturn( build03 ).when( project ).getLastCompletedBuild();
		
		ConfigurationRotatorBuildAction action = ccucm.getLastResult( project, ClearCaseUCM.class );
		System.out.println( "-----> " + action.getBuild() );
		
		assertEquals( action02, action );
	}
	
	@Test
	public void testPrint() throws UnableToInitializeEntityException, UnsupportedEncodingException {
		ClearCaseUCM ccucm = new ClearCaseUCM( "" );
		
		/* Configuration component 1*/
		Stream stream1 = Stream.get( "stream1@\\pvob" );
		Component comp1 = Component.get( "comp1@\\pvob" );
		Baseline bl1 = Mockito.mock( Baseline.class );
		Mockito.doReturn( stream1 ).when( bl1 ).getStream();
		Mockito.doReturn( comp1 ).when( bl1 ).getComponent();
		Mockito.doReturn( "bl1@\\pvob" ).when( bl1 ).getNormalizedName();
		
		/* Configuration component 2*/
		Stream stream2 = Stream.get( "stream2@\\pvob" );
		Component comp2 = Component.get( "comp2@\\pvob" );
		Baseline bl2 = Mockito.mock( Baseline.class );
		Mockito.doReturn( stream2 ).when( bl2 ).getStream();
		Mockito.doReturn( comp2 ).when( bl2 ).getComponent();
		Mockito.doReturn( "bl2@\\pvob" ).when( bl2 ).getNormalizedName();
		
		ClearCaseUCMConfigurationComponent cccc1 = new ClearCaseUCMConfigurationComponent( bl1, PromotionLevel.INITIAL, false );
		ClearCaseUCMConfigurationComponent cccc2 = new ClearCaseUCMConfigurationComponent( bl2, PromotionLevel.INITIAL, false );
		
		List<ClearCaseUCMConfigurationComponent> list = new ArrayList<ClearCaseUCMConfigurationComponent>();
		list.add( cccc1 );
		list.add( cccc2 );
		
		ClearCaseUCMConfiguration ccc = new ClearCaseUCMConfiguration( list );
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream( baos );
		
		ccucm.printConfiguration( ps, ccc );
		
		String nl = System.getProperty("line.separator");
		String content = baos.toString("latin1");
		String expected = "[ConfigRotator] The configuration is:" + nl + " * component:comp1@\\pvob, stream:stream1@\\pvob, bl1@\\pvob" + nl + " * component:comp2@\\pvob, stream:stream2@\\pvob, bl2@\\pvob" + nl + nl;
		
		System.out.println( "CONTENT : " + content );
		System.out.println( "EXPECTED: " + expected );
		
		assertEquals( expected, content );
		//assertSame( expected, content );
	}
}
