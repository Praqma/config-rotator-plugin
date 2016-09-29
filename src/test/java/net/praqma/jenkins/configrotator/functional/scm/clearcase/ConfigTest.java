package net.praqma.jenkins.configrotator.functional.scm.clearcase;

import hudson.Launcher;
import hudson.model.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.praqma.clearcase.exceptions.CleartoolException;
import net.praqma.clearcase.test.annotations.ClearCaseUniqueVobName;
import net.praqma.clearcase.test.junit.ClearCaseRule;
import net.praqma.jenkins.configrotator.scm.clearcaseucm.ClearCaseUCM;
import net.praqma.jenkins.configrotator.scm.clearcaseucm.ClearCaseUCMConfiguration;
import net.praqma.jenkins.configrotator.scm.clearcaseucm.ClearCaseUCMTarget;
import org.junit.ClassRule;
import org.junit.Rule;

import net.praqma.jenkins.configrotator.ConfigRotatorRule;
import net.praqma.jenkins.configrotator.ConfigurationRotator;
import net.praqma.jenkins.configrotator.ConfigurationRotatorBuildAction;
import net.praqma.jenkins.configrotator.ConfigurationRotatorProjectAction;
import net.praqma.jenkins.configrotator.ConfigurationRotatorReport;

import org.jvnet.hudson.test.TestBuilder;

import static org.junit.Assert.*;


public class ConfigTest {
	
	@ClassRule
	public static ConfigRotatorRule jenkins = new ConfigRotatorRule();
	
	@Rule
	public static ClearCaseRule ccenv = new ClearCaseRule( "cr" );

    // Controls how many seconds a test as minimum takes by
    // waiting before asserting on the test.
    Integer watingSeconds = 10;
    // A time stamp added to ClearCase Vob names to make them unique for each
    // test. They also include the test name.
    // Division by 60000, giving milis to minute precission asuming all tests do
    // not complete within a minute!
    String uniqueTimeStamp = "" + System.currentTimeMillis() / 60000;

    //@Test
    @ClearCaseUniqueVobName( name = "config" )
    public void testConfigRotatorObject() throws Exception {
        // Testing getting stuff and info from config rotator
        String testName = "ConfigRotatorObject";
        String debugLine = "**************************************** '" + testName + "': ";
        System.out.println(debugLine + "Starting");

        // Setup ClearCase UCM as SCM and to use with config-rotator
        ClearCaseUCM ccucm = new ClearCaseUCM(ccenv.getPVob().toString());
        List<ClearCaseUCMTarget> targets = new ArrayList<ClearCaseUCMTarget>();
        targets.add(new ClearCaseUCMTarget("model-1@" + ccenv.getPVob() + ", INITIAL, false"));
        ccucm.targets = targets;
        // create config-rotator, and set it as SCM
        System.out.println(debugLine + "Creating configurationRotator.");
        ConfigurationRotator cr = new ConfigurationRotator( ccucm );

        /*
         * *** now getting and testing a lot of stuff
         */
        System.out.println(debugLine + "cr.supportsPolling: " + cr.supportsPolling());
        assertTrue(cr.supportsPolling());

        System.out.println(debugLine + "cr.justConfigured: " + cr.justConfigured);
        assertTrue(cr.justConfigured); //justconfigured should initially be true

        System.out.println(debugLine + "cr.reconfigure: " + cr.reconfigure);
        assertFalse(cr.reconfigure); //should initially be false

        System.out.println(debugLine + "cr.createChangeLogParser(): " + cr.createChangeLogParser());
        assertNotNull(cr.createChangeLogParser());
        // for now okay null, will be implemented in later versions
        // change log parser, shows scm changes for the build, but we have baselines.
        // consider that we can show the builds baselines? or just remove the changes menu?
        // we can not show all changes from all baselines used...

        System.out.println(debugLine + "cr.getAcrs(): " + cr.getAcrs());
        assertNotNull(cr.getAcrs());

        System.out.println(debugLine + "cr.reconfigure: " + cr.reconfigure);
        System.out.println(debugLine + "Doing cr.setReconfigure(true), now testing for it");
        cr.setReconfigure(true);
        System.out.println(debugLine + "cr.reconfigure: " + cr.reconfigure);
        assertTrue(cr.reconfigure); //should initially be false, but now true

        
        System.out.println(debugLine + "Trying teardown with net.praqma.clearcase.util.SetupUtils.tearDown(ccenv.getPVob())");
        // try catch to avoid teardown failing tests - it's okay for teardown to fail
        // as our test environment later can be cleaned manually
        try 
        {
            net.praqma.clearcase.util.SetupUtils.tearDown(ccenv.getPVob());
        }
        catch (CleartoolException clex)
        {
            System.out.println(debugLine + "net.praqma.clearcase.util.SetupUtils.tearDown(ccenv.getPVob() throwed exception - ignoring on purpose");
            System.out.println(debugLine + "Exception was: " + clex.getMessage());
        }
        System.out.println(debugLine + "Done calling teardown....");


        System.out.println(debugLine + "Test done - waiting... trying avoid Jenkins failing due to clean temp dirs error");
        // waiting is important to ensure unique timestamps and let Jenkins clean
        // workspace after each test
        waiting(watingSeconds);
    }

    @ClearCaseUniqueVobName( name = "reconfigure" )
    //@Test
    public void testTryReconfigure() throws Exception {
        // Testing reconfigure
        String testName = "TryReconfigure";
        String debugLine = "**************************************** '" + testName + "': ";
        System.out.println(debugLine + "Starting");
        // ONLY alphanumeric chars

        // create Jenkins job - also use unique name
        FreeStyleProject project = jenkins.createProject(ccenv.getVobName());
        // Setup ClearCase UCM as SCM and to use with config-rotator
        ClearCaseUCM ccucm = new ClearCaseUCM(ccenv.getPVob().toString());
        List<ClearCaseUCMTarget> targets = new ArrayList<ClearCaseUCMTarget>();
        targets.add(new ClearCaseUCMTarget("model-1@" + ccenv.getPVob() + ", INITIAL, false"));
        ccucm.targets = targets;
        // create config-rotator, and set it as SCM
        System.out.println(debugLine + "Create configurationRotator.");
        ConfigurationRotator cr = new ConfigurationRotator( ccucm );
        System.out.println(debugLine + "cr.supportsPolling: " + cr.supportsPolling());
        System.out.println(debugLine + "cr.justConfigured: " + cr.justConfigured);
        assertTrue(cr.justConfigured);
        System.out.println(debugLine + "Set ConfigurationRotator as SCM");
        project.setScm(cr);

        // Try to build
        System.out.println(debugLine + "Scheduling a build for ONLY model-1");
        FreeStyleBuild b = project.scheduleBuild2(0).get();
        // now investigate result and print debug out
        assertNotNull(b);
        System.out.println(debugLine + "... build is done");
        System.out.println(debugLine + "Printing logfile: " + b.getLogFile());
        BufferedReader br = new BufferedReader(new FileReader(b.getLogFile()));
        String line = "";
        while ((line = br.readLine()) != null) {
            System.out.println("[JENKINS] " + line);
        }
        br.close();
        System.out.println(debugLine + "... done printing logfile");
        // build should be good
        System.out.println(debugLine + "build.getResult():" + b.getResult().toString());
        assertEquals(b.getResult(), Result.SUCCESS);


        ConfigurationRotatorBuildAction action = b.getAction(ConfigurationRotatorBuildAction.class);
        System.out.println(debugLine + "action: " + action);
        // action expected not to be null
        assertNotNull(action);

        // check config rotator result
        System.out.println(debugLine + "action.getResult(): " + action.getResult());
        assertEquals(action.getResult(), net.praqma.jenkins.configrotator.ConfigurationRotator.ResultType.COMPATIBLE);
        System.out.println(debugLine + "action.isCompatible: " + action.isCompatible());
        assertTrue(action.isCompatible());

        ClearCaseUCMConfiguration configuration = action.getConfiguration();
        System.out.println(debugLine + "getShortname(): " + configuration.getList().get(0).getBaseline().getShortname());
        assertEquals("model-1", configuration.getList().get(0).getBaseline().getShortname());


        // now after one successfull build, justconfigured should be false
        System.out.println(debugLine + "cr.justConfigured: " + cr.justConfigured);
        assertFalse(cr.justConfigured);

        // trying to change configuration to what happens....
        ccucm.targets.add(new ClearCaseUCMTarget("client-1@" + ccenv.getPVob() + ", INITIAL, false"));
        System.out.println(debugLine + "Changed targets adding client-1 on ccucm.targets. Target now contains:" + ccucm.targets.size());
        System.out.println(debugLine + "cr.justConfigured: " + cr.justConfigured);
        System.out.println(debugLine + "cr.reconfigure: " + cr.reconfigure);
        // These will be false, though we expect them to be true
        // We can not detect the changes before build time, thus these will on be 
        // correct just after a build.
        // Something about we don't have the context when creating the new instance
        // Conlusion: justConfigured will only be true just after creating a 
        // configrotator, but as soon a build is done the will be set false correctly
        // Thus, changing target will not make them true as expected!
        assertFalse(cr.justConfigured);
        assertFalse(cr.reconfigure);

        // This emulates what is none in GUI
        cr.justConfigured = true;
        ccucm.setConfiguration( null ); // Emulates what GUI saves does, creates all new objects
        // Try to build
        System.out.println(debugLine + "Scheduling a build for new configuration: model-1 and client-1");
        b = project.scheduleBuild2(0).get();
        // now investigate result and print debug out
        assertNotNull(b);
        System.out.println(debugLine + "... build is done");
        System.out.println(debugLine + "Printing logfile: " + b.getLogFile());
        br = new BufferedReader(new FileReader(b.getLogFile()));
        line = "";
        while ((line = br.readLine()) != null) {
            System.out.println("[JENKINS] " + line);
        }
        br.close();
        System.out.println(debugLine + "... done printing logfile");
        // build should be good
        System.out.println(debugLine + "build.getResult():" + b.getResult().toString());
        assertEquals(b.getResult(), Result.SUCCESS);


        action = b.getAction(ConfigurationRotatorBuildAction.class);
        System.out.println(debugLine + "action: " + action);
        // action expected not to be null
        assertNotNull(action);

        // check config rotator result
        System.out.println(debugLine + "action.getResult(): " + action.getResult());
        assertEquals(action.getResult(), net.praqma.jenkins.configrotator.ConfigurationRotator.ResultType.COMPATIBLE);
        System.out.println(debugLine + "action.isCompatible: " + action.isCompatible());
        assertTrue(action.isCompatible());

        configuration = action.getConfiguration();
        System.out.println(debugLine + "getShortname(): " + configuration.getList().get(0).getBaseline().getShortname());
        assertEquals("model-1", configuration.getList().get(0).getBaseline().getShortname());
        System.out.println(debugLine + "configuration.getList().size(): " + configuration.getList().size());
        System.out.println(debugLine + "getShortname(): " + configuration.getList().get(1).getBaseline().getShortname());
        assertEquals("client-1", configuration.getList().get(1).getBaseline().getShortname());

        // now after one successfull build, justconfigured should be false
        System.out.println(debugLine + "cr.justConfigured: " + cr.justConfigured);
        assertFalse(cr.justConfigured);
        
        System.out.println(debugLine + "Trying teardown with net.praqma.clearcase.util.SetupUtils.tearDown(ccenv.getPVob())");
        // try catch to avoid teardown failing tests - it's okay for teardown to fail
        // as our test environment later can be cleaned manually
        try 
        {
            net.praqma.clearcase.util.SetupUtils.tearDown(ccenv.getPVob());
        }
        catch (CleartoolException clex)
        {
            System.out.println(debugLine + "net.praqma.clearcase.util.SetupUtils.tearDown(ccenv.getPVob() throwed exception - ignoring on purpose");
            System.out.println(debugLine + "Exception was: " + clex.getMessage());
        }
        System.out.println(debugLine + "Done calling teardown....");
        
        System.out.println(debugLine + "Test done - waiting... trying avoid Jenkins failing due to clean temp dirs error");
        // waiting is important to ensure unique timestamps and let Jenkins clean
        // workspace after each test
        waiting(watingSeconds);

    }

    @ClearCaseUniqueVobName( name = "buildaction" )
    //@Test
    public void testConfigurationRotatorBuildAction() throws Exception {
        // Just boosting coverage getting stuff on action object
        String testName = "ConfigurationRotatorBuildAction";
        String debugLine = "**************************************** '" + testName + "': ";
        System.out.println(debugLine + "Starting");

        // create Jenkins job - also use unique name
        FreeStyleProject project = jenkins.createProject(ccenv.getVobName());
        // Setup ClearCase UCM as SCM and to use with config-rotator
        ClearCaseUCM ccucm = new ClearCaseUCM(ccenv.getPVob().toString());
        List<ClearCaseUCMTarget> targets = new ArrayList<ClearCaseUCMTarget>();
        // A first configuration added as targets: model-1 and client-1 that we 
        // would know to be compatible.
        targets.add(new ClearCaseUCMTarget("model-1@" + ccenv.getPVob() + ", INITIAL, false"));
        targets.add(new ClearCaseUCMTarget("client-1@" + ccenv.getPVob() + ", INITIAL, false"));
        ccucm.targets = targets;
        // create config-rotator, and set it as SCM
        System.out.println(debugLine + "Creating configurationRotator.");
        ConfigurationRotator cr = new ConfigurationRotator( ccucm );
        System.out.println(debugLine + "Set ConfigurationRotator as SCM");
        project.setScm(cr);

        // Try to build model-1 and client-1 to se if they are compatible
        System.out.println(debugLine + "Scheduling a build for model-1 and client-1...");
        FreeStyleBuild b = project.scheduleBuild2(0).get();
        // now investigate result and print debug out
        assertNotNull(b);
        System.out.println(debugLine + "... build is done");
        System.out.println(debugLine + "Printing logfile: " + b.getLogFile());
        BufferedReader br = new BufferedReader(new FileReader(b.getLogFile()));
        String line = "";
        while ((line = br.readLine()) != null) {
            System.out.println("[JENKINS] " + line);
        }
        br.close();
        System.out.println(debugLine + "... done printing logfile");
        // build should be good
        System.out.println(debugLine + "build.getResult():" + b.getResult().toString());
        assertEquals(b.getResult(), Result.SUCCESS);


        // Now trying to test methods on action
        ConfigurationRotatorBuildAction action = b.getAction(ConfigurationRotatorBuildAction.class);
        System.out.println(debugLine + "action: " + action);
        // action expected not to be null
        assertNotNull(action);

        System.out.println(debugLine + "action.getBuild(): " + action.getBuild());
        assertEquals(b, action.getBuild());

        System.out.println(debugLine + "action.getResult(): " + action.getResult());
        assertEquals(action.getResult(), net.praqma.jenkins.configrotator.ConfigurationRotator.ResultType.COMPATIBLE);
        System.out.println(debugLine + "action.isCompatible: " + action.isCompatible());
        assertTrue(action.isCompatible());

        ClearCaseUCMConfiguration configuration = action.getConfiguration();
        System.out.println(debugLine + "getShortname(): " + configuration.getList().get(0).getBaseline().getShortname());
        System.out.println(debugLine + "getShortname(): " + configuration.getList().get(1).getBaseline().getShortname());
        assertEquals("model-1", configuration.getList().get(0).getBaseline().getShortname());
        assertEquals("client-1", configuration.getList().get(1).getBaseline().getShortname());

        System.out.println(debugLine + "action.isDetermined(): " + action.isDetermined());
        assertTrue(action.isDetermined());
        System.out.println(debugLine + "action.isCompatible(): " + action.isCompatible());
        assertTrue(action.isCompatible());

        System.out.println(debugLine + "action.getIconFileName(): " + action.getIconFileName());
        assertEquals(null, action.getIconFileName());

        System.out.println(debugLine + "action.getDisplayName(): " + action.getDisplayName());
        assertEquals(null, action.getDisplayName());

        System.out.println(debugLine + "action.getUrlName(): " + action.getUrlName());
        assertEquals("config-rotator", action.getUrlName());

        System.out.println(debugLine + "Setting result failed... action.setResult(ConfigurationRotator.ResultType.FAILED)");
        action.setResult(ConfigurationRotator.ResultType.FAILED);
        System.out.println(debugLine + "action.getResult(): " + action.getResult());
        assertEquals(action.getResult(), ConfigurationRotator.ResultType.FAILED);

        
        System.out.println(debugLine + "Trying teardown with net.praqma.clearcase.util.SetupUtils.tearDown(ccenv.getPVob())");
        // try catch to avoid teardown failing tests - it's okay for teardown to fail
        // as our test environment later can be cleaned manually
        try 
        {
            net.praqma.clearcase.util.SetupUtils.tearDown(ccenv.getPVob());
        }
        catch (CleartoolException clex)
        {
            System.out.println(debugLine + "net.praqma.clearcase.util.SetupUtils.tearDown(ccenv.getPVob() throwed exception - ignoring on purpose");
            System.out.println(debugLine + "Exception was: " + clex.getMessage());
        }
        System.out.println(debugLine + "Done calling teardown....");
        
        
        System.out.println(debugLine + "Test done - waiting... trying avoid Jenkins failing due to clean temp dirs error");
        // waiting is important to ensure unique timestamps and let Jenkins clean
        // workspace after each test
        waiting(watingSeconds);
    }

    @ClearCaseUniqueVobName( name = "revert" )
    //@Test
    public void testRevertToConfiguration() throws Exception {
        // Tests if we can revert to another configuration from an old build
        String testName = "RevertToConfiguration";
        String debugLine = "**************************************** '" + testName + "': ";
        System.out.println(debugLine + "Starting");

        // create Jenkins job - also use unique name
        FreeStyleProject project = jenkins.createProject( ccenv.getVobName());
        // Setup ClearCase UCM as SCM and to use with config-rotator
        ClearCaseUCM ccucm = new ClearCaseUCM(ccenv.getPVob().toString());
        List<ClearCaseUCMTarget> targets = new ArrayList<ClearCaseUCMTarget>();
        // A first configuration added as targets: model-1 and client-1 that we 
        // would know to be compatible.
        targets.add(new ClearCaseUCMTarget("model-1@" + ccenv.getPVob() + ", INITIAL, false"));
        targets.add(new ClearCaseUCMTarget("client-1@" + ccenv.getPVob() + ", INITIAL, false"));
        ccucm.targets = targets;
        // create config-rotator, and set it as SCM
        System.out.println(debugLine + "Creating configurationRotator.");
        ConfigurationRotator cr = new ConfigurationRotator( ccucm );
        System.out.println(debugLine + "Set ConfigurationRotator as SCM");
        project.setScm(cr);

        // Try to build model-1 and client-1 to se if they are compatible
        System.out.println(debugLine + "Scheduling a build for model-1 and client-1...");
        FreeStyleBuild b = project.scheduleBuild2(0).get();
        // now investigate result and print debug out
        assertNotNull(b);
        System.out.println(debugLine + "... build is done");
        System.out.println(debugLine + "Printing logfile: " + b.getLogFile());
        BufferedReader br = new BufferedReader(new FileReader(b.getLogFile()));
        String line = "";
        while ((line = br.readLine()) != null) {
            System.out.println("[JENKINS] " + line);
        }
        br.close();
        System.out.println(debugLine + "... done printing logfile");
        // build should be good
        System.out.println(debugLine + "build.getResult():" + b.getResult().toString());
        assertEquals(b.getResult(), Result.SUCCESS);


        ConfigurationRotatorBuildAction action = b.getAction(ConfigurationRotatorBuildAction.class);
        System.out.println(debugLine + "action: " + action);
        // action expected not to be null
        assertNotNull(action);

        // check config rotator result
        System.out.println(debugLine + "action.getResult(): " + action.getResult());
        assertEquals(action.getResult(), net.praqma.jenkins.configrotator.ConfigurationRotator.ResultType.COMPATIBLE);
        System.out.println(debugLine + "action.isCompatible: " + action.isCompatible());
        assertTrue(action.isCompatible());

        ClearCaseUCMConfiguration configuration = action.getConfiguration();
        System.out.println(debugLine + "getShortname(): " + configuration.getList().get(0).getBaseline().getShortname());
        System.out.println(debugLine + "getShortname(): " + configuration.getList().get(1).getBaseline().getShortname());
        assertEquals("model-1", configuration.getList().get(0).getBaseline().getShortname());
        assertEquals("client-1", configuration.getList().get(1).getBaseline().getShortname());




        // Building again, to make it possible to reset later to model-1 and client-1
        System.out.println(debugLine + "Scheduling a build 2, for model-2 and client-1...");
        FreeStyleBuild b2 = project.scheduleBuild2(0).get();
        // now investigate result and print debug out
        assertNotNull(b2);
        System.out.println(debugLine + "... build is done");
        System.out.println(debugLine + "Printing logfile: " + b2.getLogFile());
        br = new BufferedReader(new FileReader(b2.getLogFile()));
        line = "";
        while ((line = br.readLine()) != null) {
            System.out.println("[JENKINS] " + line);
        }
        br.close();
        System.out.println(debugLine + "... done printing logfile");
        // build should be good
        System.out.println(debugLine + "build.getResult():" + b2.getResult().toString());
        assertEquals(b2.getResult(), Result.SUCCESS);


        ConfigurationRotatorBuildAction action2 = b2.getAction(ConfigurationRotatorBuildAction.class);
        System.out.println(debugLine + "action: " + action2);
        // action expected not to be null
        assertNotNull(action2);

        System.out.println(debugLine + "Setting configuration from last action.");
        cr.setConfigurationByAction(project, action);
        System.out.println(debugLine + "cr.justConfigured: " + cr.justConfigured);
        assertFalse(cr.justConfigured); // dit not just configure...
        System.out.println(debugLine + "cr.reconfigure: " + cr.reconfigure);
        assertTrue(cr.reconfigure); //but RECONFIGURED ....

        // seems configuration worked, not trying to build to see if it is the old one
        System.out.println(debugLine + "Scheduling a build 3, that should be for old config model-1 and client-1...");
        FreeStyleBuild b3 = project.scheduleBuild2(0).get();
        // now investigate result and print debug out
        assertNotNull(b3);
        System.out.println(debugLine + "... build is done");
        System.out.println(debugLine + "Printing logfile: " + b3.getLogFile());
        br = new BufferedReader(new FileReader(b3.getLogFile()));
        line = "";
        while ((line = br.readLine()) != null) {
            System.out.println("[JENKINS] " + line);
        }
        br.close();
        System.out.println(debugLine + "... done printing logfile");
        // build should be good
        System.out.println(debugLine + "build.getResult():" + b3.getResult().toString());
        assertEquals(b3.getResult(), Result.SUCCESS);


        ConfigurationRotatorBuildAction action3 = b3.getAction(ConfigurationRotatorBuildAction.class);
        System.out.println(debugLine + "action: " + action3);
        // action expected not to be null
        assertNotNull(action3);

        // check config rotator result
        System.out.println(debugLine + "action.getResult(): " + action3.getResult());
        assertEquals(action3.getResult(), net.praqma.jenkins.configrotator.ConfigurationRotator.ResultType.COMPATIBLE);
        System.out.println(debugLine + "action.isCompatible: " + action3.isCompatible());
        assertTrue(action3.isCompatible());

        ClearCaseUCMConfiguration configuration3 = action.getConfiguration();
        System.out.println(debugLine + "getShortname(): " + configuration3.getList().get(0).getBaseline().getShortname());
        System.out.println(debugLine + "getShortname(): " + configuration3.getList().get(1).getBaseline().getShortname());
        assertEquals("model-1", configuration3.getList().get(0).getBaseline().getShortname());
        assertEquals("client-1", configuration3.getList().get(1).getBaseline().getShortname());

        System.out.println(debugLine + "Trying teardown with net.praqma.clearcase.util.SetupUtils.tearDown(ccenv.getPVob())");
        // try catch to avoid teardown failing tests - it's okay for teardown to fail
        // as our test environment later can be cleaned manually
        try 
        {
            net.praqma.clearcase.util.SetupUtils.tearDown(ccenv.getPVob());
        }
        catch (CleartoolException clex)
        {
            System.out.println(debugLine + "net.praqma.clearcase.util.SetupUtils.tearDown(ccenv.getPVob() throwed exception - ignoring on purpose");
            System.out.println(debugLine + "Exception was: " + clex.getMessage());
        }
        System.out.println(debugLine + "Done calling teardown....");        
        
        System.out.println(debugLine + "Test done - waiting... trying avoid Jenkins failing due to clean temp dirs error");
        // waiting is important to ensure unique timestamps and let Jenkins clean
        // workspace after each test
        waiting(watingSeconds);
    }

    @ClearCaseUniqueVobName( name = "wrongtarget" )
    //@Test
    public void testInputWrongTargetName() throws Exception {
        // Do we handle user inputting wrong target names?
        String testName = "InputWrongTargetName";
        String debugLine = "**************************************** '" + testName + "': ";
        System.out.println(debugLine + "Starting");
        
        // create Jenkins job - also use unique name
        FreeStyleProject project = jenkins.createProject(ccenv.getVobName());
        // Setup ClearCase UCM as SCM and to use with config-rotator
        ClearCaseUCM ccucm = new ClearCaseUCM(ccenv.getPVob().toString());
        List<ClearCaseUCMTarget> targets = new ArrayList<ClearCaseUCMTarget>();
        System.out.println(debugLine + "Adding two targets with wrong name...");
        targets.add(new ClearCaseUCMTarget("model-WrongName@" + ccenv.getPVob() + ", INITIAL, false"));
        targets.add(new ClearCaseUCMTarget("client-WrongName@" + ccenv.getPVob() + ", INITIAL, false"));
        ccucm.targets = targets;
        // create config-rotator, and set it as SCM
        System.out.println(debugLine + "Create configurationRotator.");
        ConfigurationRotator cr = new ConfigurationRotator( ccucm );
        System.out.println(debugLine + "cr.supportsPolling: " + cr.supportsPolling());
        System.out.println(debugLine + "Set ConfigurationRotator as SCM");
        project.setScm(cr);

        // Try to build
        System.out.println(debugLine + "Scheduling a build...");
        FreeStyleBuild b = project.scheduleBuild2(0).get();
        System.out.println(debugLine + "After scheduling build IS DONE!");
        // now investigate result and print debug out
        System.out.println(debugLine + "build.getResult():" + b.getResult().toString());
        // build should fail for wrong targets
        assertEquals(b.getResult(), Result.FAILURE);

        ConfigurationRotatorBuildAction action = b.getAction(ConfigurationRotatorBuildAction.class);
        System.out.println(debugLine + "action: " + action);
        // action expected to be null
        assertNull(action);

        System.out.println(debugLine + "Printing logfile: " + b.getLogFile());
        BufferedReader br = new BufferedReader(new FileReader(b.getLogFile()));
        String line = "";
        while ((line = br.readLine()) != null) {
            System.out.println("[JENKINS] " + line);
        }
        br.close();
        System.out.println(debugLine + "... done printing logfile");

        System.out.println(debugLine + "Trying teardown with net.praqma.clearcase.util.SetupUtils.tearDown(ccenv.getPVob())");
        // try catch to avoid teardown failing tests - it's okay for teardown to fail
        // as our test environment later can be cleaned manually
        try 
        {
            net.praqma.clearcase.util.SetupUtils.tearDown(ccenv.getPVob());
        }
        catch (CleartoolException clex)
        {
            System.out.println(debugLine + "net.praqma.clearcase.util.SetupUtils.tearDown(ccenv.getPVob() throwed exception - ignoring on purpose");
            System.out.println(debugLine + "Exception was: " + clex.getMessage());
        }
        System.out.println(debugLine + "Done calling teardown....");

        System.out.println(debugLine + "Test done - waiting... trying avoid Jenkins failing due to clean temp dirs error");
        // waiting is important to ensure unique timestamps and let Jenkins clean
        // workspace after each test
        waiting(watingSeconds);

    }

    @ClearCaseUniqueVobName( name = "ccucmtarget" )
    //@Test
    public void testClearCaseUCMTarget() throws Exception {
        // Test is supposed to test last parts of UCMTarget component
        // not hit in the other tests
        String testName = "ClearCaseUCMTarget";
        String debugLine = "**************************************** '" + testName + "': ";
        System.out.println(debugLine + "Starting");

        // Make targets.... and test on those
        // try constructor 
        System.out.println(debugLine + "Trying CCUCMtarget constructor - plain");
        ClearCaseUCMTarget target1 = new ClearCaseUCMTarget();
        // set component on it
        System.out.println(debugLine + "Setting component");
        target1.setComponent("model-1@" + ccenv.getPVob() + ", INITIAL, false");
        // test getComponent - should be the same
        System.out.println(debugLine + "Getting component and checking equality");
        assertEquals("model-1@" + ccenv.getPVob() + ", INITIAL, false", target1.getComponent());
        // test equals
        System.out.println(debugLine + "Comparing target1 with target1");
        assertTrue(target1.equals(target1));
        System.out.println(debugLine + "Creating a target2");
        ClearCaseUCMTarget target2 = new ClearCaseUCMTarget("client-1@" + ccenv.getPVob() + ", INITIAL, false");
        System.out.println(debugLine + "Comparing target1 with target2");
        assertFalse(target1.equals(target2));

        System.out.println(debugLine + "Trying teardown with net.praqma.clearcase.util.SetupUtils.tearDown(ccenv.getPVob())");
        // try catch to avoid teardown failing tests - it's okay for teardown to fail
        // as our test environment later can be cleaned manually
        try 
        {
            net.praqma.clearcase.util.SetupUtils.tearDown(ccenv.getPVob());
        }
        catch (CleartoolException clex)
        {
            System.out.println(debugLine + "net.praqma.clearcase.util.SetupUtils.tearDown(ccenv.getPVob() throwed exception - ignoring on purpose");
            System.out.println(debugLine + "Exception was: " + clex.getMessage());
        }
        System.out.println(debugLine + "Done calling teardown....");
        
        System.out.println(debugLine + "Test done - waiting... trying avoid Jenkins failing due to clean temp dirs error");
        // waiting is important to ensure unique timestamps and let Jenkins clean
        // workspace after each test
        waiting(watingSeconds);
    }

    @ClearCaseUniqueVobName( name = "ccucmobj" )
    //@Test
    public void testClearCaseUCMObject() throws Exception {
        // Boosting coverage
        String testName = "CClearCaseUCMObject";
        String debugLine = "**************************************** '" + testName + "': ";
        System.out.println(debugLine + "Starting");

        // create Jenkins job - also use unique name
        FreeStyleProject project = jenkins.createProject(ccenv.getVobName());
        // Setup ClearCase UCM as SCM and to use with config-rotator
        ClearCaseUCM ccucm = new ClearCaseUCM(ccenv.getPVob().toString());
        List<ClearCaseUCMTarget> targets = new ArrayList<ClearCaseUCMTarget>();
        // A first configuration added as targets: model-1 and client-1 that we 
        // would know to be compatible.
        targets.add(new ClearCaseUCMTarget("model-1@" + ccenv.getPVob() + ", INITIAL, false"));
        targets.add(new ClearCaseUCMTarget("client-1@" + ccenv.getPVob() + ", INITIAL, false"));
        ccucm.targets = targets;
        System.out.println(debugLine + "Usual ccucm target set on ccucm");

        System.out.println(debugLine + "ccucm.getPvobName():" + ccucm.getPvobName());
        assertEquals("\\" + ccenv.getVobName() + "_PVOB", ccucm.getPvobName());

        System.out.println(debugLine + "ccucm.getName():" + ccucm.getName());
        assertEquals("ClearCase UCM", ccucm.getName());

        System.out.println(debugLine + "ccucm.wasReconfigured(project):" + ccucm.wasReconfigured(project));
        assertTrue(ccucm.wasReconfigured(project)); // because targets just added

        // create config-rotator, and set it as SCM
        System.out.println(debugLine + "Create configurationRotator.");
        ConfigurationRotator cr = new ConfigurationRotator( ccucm );
        System.out.println(debugLine + "cr.supportsPolling: " + cr.supportsPolling());
        System.out.println(debugLine + "Set ConfigurationRotator as SCM");
        project.setScm(cr);

        System.out.println(debugLine + "ccucm.wasReconfigured(project):" + ccucm.wasReconfigured(project));
        assertTrue(ccucm.wasReconfigured(project)); // still not builded yet

        // Try to build
        System.out.println(debugLine + "Scheduling a build ...");
        FreeStyleBuild b = project.scheduleBuild2(0).get();
        // now investigate result and print debug out
        assertNotNull(b);
        System.out.println(debugLine + "... build is done");
        System.out.println(debugLine + "Printing logfile: " + b.getLogFile());
        BufferedReader br = new BufferedReader(new FileReader(b.getLogFile()));
        String line = "";
        while ((line = br.readLine()) != null) {
            System.out.println("[JENKINS] " + line);
        }
        br.close();
        System.out.println(debugLine + "... done printing logfile");
        // build should be good
        System.out.println(debugLine + "build.getResult():" + b.getResult().toString());
        assertEquals(b.getResult(), Result.SUCCESS);

        System.out.println(debugLine + "ccucm.wasReconfigured(project):" + ccucm.wasReconfigured(project));
        assertFalse(ccucm.wasReconfigured(project)); // now it should not just be reconfigured

        // change targets
        System.out.println(debugLine + "clearing targets on ccucm");
        ccucm.targets.clear();
        System.out.println(debugLine + "ccucm.wasReconfigured(project):" + ccucm.wasReconfigured(project));
        assertTrue(ccucm.wasReconfigured(project)); // true, just removed targets

        System.out.println(debugLine + "adding one target to ccucm");
        ccucm.targets.add(new ClearCaseUCMTarget("model-1@" + ccenv.getPVob() + ", INITIAL, false"));
        System.out.println(debugLine + "ccucm.wasReconfigured(project):" + ccucm.wasReconfigured(project));
        assertTrue(ccucm.wasReconfigured(project)); // true, just added a target
        System.out.println(debugLine + "ccucm.wasReconfigured(project):" + ccucm.wasReconfigured(project));
        assertTrue(ccucm.wasReconfigured(project)); // still, should be same result ?

        System.out.println(debugLine + "Trying teardown with net.praqma.clearcase.util.SetupUtils.tearDown(ccenv.getPVob())");
        // try catch to avoid teardown failing tests - it's okay for teardown to fail
        // as our test environment later can be cleaned manually
        try 
        {
            net.praqma.clearcase.util.SetupUtils.tearDown(ccenv.getPVob());
        }
        catch (CleartoolException clex)
        {
            System.out.println(debugLine + "net.praqma.clearcase.util.SetupUtils.tearDown(ccenv.getPVob() throwed exception - ignoring on purpose");
            System.out.println(debugLine + "Exception was: " + clex.getMessage());
        }
        System.out.println(debugLine + "Done calling teardown....");

        System.out.println(debugLine + "Test done - waiting... trying avoid Jenkins failing due to clean temp dirs error");
        // waiting is important to ensure unique timestamps and let Jenkins clean
        // workspace after each test
        waiting(watingSeconds);
    }

    @ClearCaseUniqueVobName( name = "misc" )
    //@Test
    public void testMiscObjects() throws Exception {
        // is this cheating ?
        String testName = "MiscObjects";
        String debugLine = "**************************************** '" + testName + "': ";
        System.out.println(debugLine + "Starting");

        // create Jenkins job - also use unique name
        FreeStyleProject project = jenkins.createProject(ccenv.getVobName());
        // Setup ClearCase UCM as SCM and to use with config-rotator
        ClearCaseUCM ccucm = new ClearCaseUCM(ccenv.getPVob().toString());
        List<ClearCaseUCMTarget> targets = new ArrayList<ClearCaseUCMTarget>();
        // A first configuration added as targets: model-1 and client-1 that we 
        // would know to be compatible.
        targets.add(new ClearCaseUCMTarget("model-1@" + ccenv.getPVob() + ", INITIAL, false"));
        targets.add(new ClearCaseUCMTarget("client-1@" + ccenv.getPVob() + ", INITIAL, false"));
        ccucm.targets = targets;
        // create config-rotator, and set it as SCM
        System.out.println(debugLine + "Create configurationRotator.");
        ConfigurationRotator cr = new ConfigurationRotator( ccucm );
        System.out.println(debugLine + "cr.supportsPolling: " + cr.supportsPolling());
        System.out.println(debugLine + "Set ConfigurationRotator as SCM");
        project.setScm(cr);

        System.out.println(debugLine + "cr.getDescriptor(): " + cr.getDescriptor());
        //assertEqual(cr.getDescriptor();
        System.out.println(debugLine + "cr.getDescriptor(): " + ConfigurationRotator.all());
        System.out.println(debugLine + "cr.getDescriptor(): " + ConfigurationRotator._for(project));


        // Try to build model-1 and client-1 to se if they are compatible
        System.out.println(debugLine + "Scheduling a build for model-1 and client-1...");
        FreeStyleBuild b = project.scheduleBuild2(0).get();
        // now investigate result and print debug out
        assertNotNull(b);
        System.out.println(debugLine + "... build is done");
        System.out.println(debugLine + "Printing logfile: " + b.getLogFile());
        BufferedReader br = new BufferedReader(new FileReader(b.getLogFile()));
        String line = "";
        while ((line = br.readLine()) != null) {
            System.out.println("[JENKINS] " + line);
        }
        br.close();
        System.out.println(debugLine + "... done printing logfile");
        // build should be good
        System.out.println(debugLine + "build.getResult():" + b.getResult().toString());
        assertEquals(b.getResult(), Result.SUCCESS);


        ConfigurationRotatorBuildAction action = b.getAction(ConfigurationRotatorBuildAction.class);
        System.out.println(debugLine + "action: " + action);
        // action expected not to be null
        assertNotNull(action);

        // check config rotator result
        System.out.println(debugLine + "action.getResult(): " + action.getResult());
        assertEquals(action.getResult(), net.praqma.jenkins.configrotator.ConfigurationRotator.ResultType.COMPATIBLE);
        System.out.println(debugLine + "action.isCompatible: " + action.isCompatible());
        assertTrue(action.isCompatible());

        ClearCaseUCMConfiguration configuration = action.getConfiguration();
        System.out.println(debugLine + "getShortname(): " + configuration.getList().get(0).getBaseline().getShortname());
        System.out.println(debugLine + "getShortname(): " + configuration.getList().get(1).getBaseline().getShortname());
        assertEquals("model-1", configuration.getList().get(0).getBaseline().getShortname());
        assertEquals("client-1", configuration.getList().get(1).getBaseline().getShortname());

        ConfigurationRotatorReport crr = new ConfigurationRotatorReport();
        System.out.println(debugLine + "crr.getIconFileName(): " + crr.getIconFileName());
        System.out.println(debugLine + "crr.getDisplayName(): " + crr.getDisplayName());
        System.out.println(debugLine + "crr.getUrlName(): " + crr.getUrlName());
        System.out.println(debugLine + "crr.getSearchUrl(): " + crr.getSearchUrl());
//		System.out.println( debugLine + "crr.getSearchUrl(): " + crr.doFeed("model-1@" + ccenv.getPVob() + ", INITIAL, false", "text/plain") );

        ConfigurationRotatorProjectAction crpa = new ConfigurationRotatorProjectAction(project);
        assertEquals("Config Rotator", crpa.getDisplayName());
        assertEquals("/plugin/config-rotator/images/rotate.png", crpa.getIconFileName());
        //assertEquals("config-rotator", crpa.getSearchUrl());
        assertEquals("config-rotator", crpa.getUrlName());
        
        
        System.out.println(debugLine + "Trying teardown with net.praqma.clearcase.util.SetupUtils.tearDown(ccenv.getPVob())");
        // try catch to avoid teardown failing tests - it's okay for teardown to fail
        // as our test environment later can be cleaned manually
        try 
        {
            net.praqma.clearcase.util.SetupUtils.tearDown(ccenv.getPVob());
        }
        catch (CleartoolException clex)
        {
            System.out.println(debugLine + "net.praqma.clearcase.util.SetupUtils.tearDown(ccenv.getPVob() throwed exception - ignoring on purpose");
            System.out.println(debugLine + "Exception was: " + clex.getMessage());
        }
        System.out.println(debugLine + "Done calling teardown....");

        System.out.println(debugLine + "Test done - waiting... trying avoid Jenkins failing due to clean temp dirs error");
        // waiting is important to ensure unique timestamps and let Jenkins clean
        // workspace after each test
        waiting(watingSeconds);

    }

    @ClearCaseUniqueVobName( name = "fail" )
    //@Test
    public void testTryFailingBuild() throws Exception {
        // This test is supposed to "manually" iterate over baselines by scheduling
        // a build. Each build scheduled will poll scm and should find a new baseline
        // to test, until there is no more.
        // For each build, we check a lot of output, results etc.
        String testName = "TryFailingBuild";
        String debugLine = "**************************************** '" + testName + "': ";
        System.out.println(debugLine + "Starting");

        // create Jenkins job - also use unique name
        FreeStyleProject project = jenkins.createProject(ccenv.getVobName());
        // Setup ClearCase UCM as SCM and to use with config-rotator
        ClearCaseUCM ccucm = new ClearCaseUCM(ccenv.getPVob().toString());
        List<ClearCaseUCMTarget> targets = new ArrayList<ClearCaseUCMTarget>();
        // A first configuration added as targets: model-1 and client-1 that we 
        // would know to be compatible.
        targets.add(new ClearCaseUCMTarget("model-1@" + ccenv.getPVob() + ", INITIAL, false"));
        targets.add(new ClearCaseUCMTarget("client-1@" + ccenv.getPVob() + ", INITIAL, false"));
        ccucm.targets = targets;
        // create config-rotator, and set it as SCM
        System.out.println(debugLine + "Create configurationRotator.");
        ConfigurationRotator cr = new ConfigurationRotator( ccucm );
        System.out.println(debugLine + "cr.supportsPolling: " + cr.supportsPolling());
        System.out.println(debugLine + "Set ConfigurationRotator as SCM");
        project.setScm(cr);

        project.getBuildersList().add(new TestBuilder() {

            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
                    BuildListener listener) throws InterruptedException, IOException {
                return false;
            }
        });


        // Try to build model-1 and client-1 to se if they are compatible
        System.out.println(debugLine + "Scheduling a build for model-1 and client-1...");
        FreeStyleBuild b = project.scheduleBuild2(0).get();
        // now investigate result and print debug out
        assertNotNull(b);
        System.out.println(debugLine + "... build is done");
        System.out.println(debugLine + "Printing logfile: " + b.getLogFile());
        BufferedReader br = new BufferedReader(new FileReader(b.getLogFile()));
        String line = "";
        while ((line = br.readLine()) != null) {
            System.out.println("[JENKINS] " + line);
        }
        br.close();
        System.out.println(debugLine + "... done printing logfile");
        // build should be good
        System.out.println(debugLine + "build.getResult():" + b.getResult().toString());
        assertEquals(b.getResult(), Result.FAILURE);


        ConfigurationRotatorBuildAction action = b.getAction(ConfigurationRotatorBuildAction.class);
        System.out.println(debugLine + "action: " + action);
        // action expected not to be null
        assertNotNull(action);

        // check config rotator result
        System.out.println(debugLine + "action.getResult(): " + action.getResult());
		assertEquals(action.getResult(), net.praqma.jenkins.configrotator.ConfigurationRotator.ResultType.INCOMPATIBLE);
        System.out.println(debugLine + "action.isCompatible: " + action.isCompatible());
		//The assertion above assumes that the result is 'INCOMPATIBLE'...
        assertFalse(action.isCompatible());

        ClearCaseUCMConfiguration configuration = action.getConfiguration();
        System.out.println(debugLine + "getShortname(): " + configuration.getList().get(0).getBaseline().getShortname());
        System.out.println(debugLine + "getShortname(): " + configuration.getList().get(1).getBaseline().getShortname());
		assertEquals("model-1", configuration.getList().get(0).getBaseline().getShortname());
		assertEquals("client-1", configuration.getList().get(1).getBaseline().getShortname());

        System.out.println(debugLine + "Trying teardown with net.praqma.clearcase.util.SetupUtils.tearDown(ccenv.getPVob())");
        // try catch to avoid teardown failing tests - it's okay for teardown to fail
        // as our test environment later can be cleaned manually
        try 
        {
            net.praqma.clearcase.util.SetupUtils.tearDown(ccenv.getPVob());
        }
        catch (CleartoolException clex)
        {
            System.out.println(debugLine + "net.praqma.clearcase.util.SetupUtils.tearDown(ccenv.getPVob() throwed exception - ignoring on purpose");
            System.out.println(debugLine + "Exception was: " + clex.getMessage());
        }
        System.out.println(debugLine + "Done calling teardown....");


        System.out.println(debugLine + "Test done - waiting... trying avoid Jenkins failing due to clean temp dirs error");
        // waiting is important to ensure unique timestamps and let Jenkins clean
        // workspace after each test
        waiting(watingSeconds);

    }

    @ClearCaseUniqueVobName( name = "fixedtargets" )
    //@Test
    public void testManualIterateWithFixedTargets() throws Exception {
        // This test is supposed to "manually" iterate over baselines by scheduling
        // a build. Each build scheduled will poll scm and should find a new baseline
        // to test, until there is no more.
        // NOTE: One of the targets is fixed!
        // For each build, we check a lot of output, results etc.
        String testName = "ManualIterateWithFixedTargets";
        String debugLine = "**************************************** '" + testName + "': ";
        System.out.println(debugLine + "Starting");

        // create Jenkins job - also use unique name
        FreeStyleProject project = jenkins.createProject(ccenv.getVobName());
        // Setup ClearCase UCM as SCM and to use with config-rotator
        ClearCaseUCM ccucm = new ClearCaseUCM(ccenv.getPVob().toString());
        List<ClearCaseUCMTarget> targets = new ArrayList<ClearCaseUCMTarget>();
        // A first configuration added as targets: model-1 and client-1 that we 
        // would know to be compatible.
        targets.add(new ClearCaseUCMTarget("model-1@" + ccenv.getPVob() + ", INITIAL, true"));
        targets.add(new ClearCaseUCMTarget("client-1@" + ccenv.getPVob() + ", INITIAL, false"));
        ccucm.targets = targets;
        // create config-rotator, and set it as SCM
        System.out.println(debugLine + "Create configurationRotator.");
        ConfigurationRotator cr = new ConfigurationRotator( ccucm );
        System.out.println(debugLine + "cr.supportsPolling: " + cr.supportsPolling());
        System.out.println(debugLine + "Set ConfigurationRotator as SCM");
        project.setScm(cr);

        // Try to build model-1 and client-1 to se if they are compatible
        System.out.println(debugLine + "Scheduling a build for model-1 and client-1...");
        FreeStyleBuild b = project.scheduleBuild2(0).get();
        // now investigate result and print debug out
        assertNotNull(b);
        System.out.println(debugLine + "... build is done");
        System.out.println(debugLine + "Printing logfile: " + b.getLogFile());
        BufferedReader br = new BufferedReader(new FileReader(b.getLogFile()));
        String line = "";
        while ((line = br.readLine()) != null) {
            System.out.println("[JENKINS] " + line);
        }
        br.close();
        System.out.println(debugLine + "... done printing logfile");
        // build should be good
        System.out.println(debugLine + "build.getResult():" + b.getResult().toString());
        assertEquals(b.getResult(), Result.SUCCESS);


        ConfigurationRotatorBuildAction action = b.getAction(ConfigurationRotatorBuildAction.class);
        System.out.println(debugLine + "action: " + action);
        // action expected not to be null
        assertNotNull(action);

        // check config rotator result
        System.out.println(debugLine + "action.getResult(): " + action.getResult());
        assertEquals(action.getResult(), net.praqma.jenkins.configrotator.ConfigurationRotator.ResultType.COMPATIBLE);
        System.out.println(debugLine + "action.isCompatible: " + action.isCompatible());
        assertTrue(action.isCompatible());

        ClearCaseUCMConfiguration configuration = action.getConfiguration();
        System.out.println(debugLine + "getShortname(): " + configuration.getList().get(0).getBaseline().getShortname());
        System.out.println(debugLine + "getShortname(): " + configuration.getList().get(1).getBaseline().getShortname());
        assertEquals("model-1", configuration.getList().get(0).getBaseline().getShortname());
        assertEquals("client-1", configuration.getList().get(1).getBaseline().getShortname());





        System.out.println(debugLine + "Scheduling a build for model-1 and client-2 (model-1 target is fixed)...");
        b = project.scheduleBuild2(0).get();
        // now investigate result and print debug out
        assertNotNull(b);
        System.out.println(debugLine + "... build is done");
        System.out.println(debugLine + "Printing logfile: " + b.getLogFile());
        br = new BufferedReader(new FileReader(b.getLogFile()));
        line = "";
        while ((line = br.readLine()) != null) {
            System.out.println("[JENKINS] " + line);
        }
        br.close();
        System.out.println(debugLine + "... done printing logfile");
        // build should be good
        System.out.println(debugLine + "build.getResult():" + b.getResult().toString());
        assertEquals(b.getResult(), Result.SUCCESS);

        action = b.getAction(ConfigurationRotatorBuildAction.class);
        System.out.println(debugLine + "action: " + action);
        // action expected not to be null
        assertNotNull(action);

        // check config rotator result
        System.out.println(debugLine + "action.getResult(): " + action.getResult());
        assertEquals(action.getResult(), net.praqma.jenkins.configrotator.ConfigurationRotator.ResultType.COMPATIBLE);
        System.out.println(debugLine + "action.isCompatible: " + action.isCompatible());
        assertTrue(action.isCompatible());

        configuration = action.getConfiguration();
        System.out.println(debugLine + "getShortname(): " + configuration.getList().get(0).getBaseline().getShortname());
        System.out.println(debugLine + "getShortname(): " + configuration.getList().get(1).getBaseline().getShortname());
        assertEquals("model-1", configuration.getList().get(0).getBaseline().getShortname());
        assertEquals("client-2", configuration.getList().get(1).getBaseline().getShortname());





        System.out.println(debugLine + "Scheduling a build for model-1 and client-3 (model-1 target is fixed)...");
        b = project.scheduleBuild2(0).get();
        // now investigate result and print debug out
        assertNotNull(b);
        System.out.println(debugLine + "... build is done");
        System.out.println(debugLine + "Printing logfile: " + b.getLogFile());
        br = new BufferedReader(new FileReader(b.getLogFile()));
        line = "";
        while ((line = br.readLine()) != null) {
            System.out.println("[JENKINS] " + line);
        }
        br.close();
        System.out.println(debugLine + "... done printing logfile");
        // build should be good
        System.out.println(debugLine + "build.getResult():" + b.getResult().toString());
        assertEquals(b.getResult(), Result.SUCCESS);

        action = b.getAction(ConfigurationRotatorBuildAction.class);
        System.out.println(debugLine + "action: " + action);
        // action expected not to be null
        assertNotNull(action);

        // check config rotator result
        System.out.println(debugLine + "action.getResult(): " + action.getResult());
        assertEquals(action.getResult(), net.praqma.jenkins.configrotator.ConfigurationRotator.ResultType.COMPATIBLE);
        System.out.println(debugLine + "action.isCompatible: " + action.isCompatible());
        assertTrue(action.isCompatible());

        configuration = action.getConfiguration();
        System.out.println(debugLine + "getShortname(): " + configuration.getList().get(0).getBaseline().getShortname());
        System.out.println(debugLine + "getShortname(): " + configuration.getList().get(1).getBaseline().getShortname());
        assertEquals("model-1", configuration.getList().get(0).getBaseline().getShortname());
        assertEquals("client-3", configuration.getList().get(1).getBaseline().getShortname());



        System.out.println(debugLine + "Trying teardown with net.praqma.clearcase.util.SetupUtils.tearDown(ccenv.getPVob())");
        // try catch to avoid teardown failing tests - it's okay for teardown to fail
        // as our test environment later can be cleaned manually
        try 
        {
            net.praqma.clearcase.util.SetupUtils.tearDown(ccenv.getPVob());
        }
        catch (CleartoolException clex)
        {
            System.out.println(debugLine + "net.praqma.clearcase.util.SetupUtils.tearDown(ccenv.getPVob() throwed exception - ignoring on purpose");
            System.out.println(debugLine + "Exception was: " + clex.getMessage());
        }
        System.out.println(debugLine + "Done calling teardown....");


        System.out.println(debugLine + "Test done - waiting... trying avoid Jenkins failing due to clean temp dirs error");
        // waiting is important to ensure unique timestamps and let Jenkins clean
        // workspace after each test
        waiting(watingSeconds);

    }
    
    
    @ClearCaseUniqueVobName( name = "iterateall" )
    //@Test
    public void testManualIterateThroughAllBaselines() throws Exception {
        // This test is supposed to "manually" iterate over baselines by scheduling
        // a build. Each build scheduled will poll scm and should find a new baseline
        // to test, until there is no more.
        // For each build, we check a lot of output, results etc.
        String testName = "ManualIterateThroughAllBaselines";
        String debugLine = "**************************************** '" + testName + "': ";
        System.out.println(debugLine + "Starting");

        // create Jenkins job - also use unique name
        FreeStyleProject project = jenkins.createProject(ccenv.getVobName());
        // Setup ClearCase UCM as SCM and to use with config-rotator
        ClearCaseUCM ccucm = new ClearCaseUCM(ccenv.getPVob().toString());
        List<ClearCaseUCMTarget> targets = new ArrayList<ClearCaseUCMTarget>();
        // A first configuration added as targets: model-1 and client-1 that we 
        // would know to be compatible.
        targets.add(new ClearCaseUCMTarget("model-1@" + ccenv.getPVob() + ", INITIAL, false"));
        targets.add(new ClearCaseUCMTarget("client-1@" + ccenv.getPVob() + ", INITIAL, false"));
        ccucm.targets = targets;
        // create config-rotator, and set it as SCM
        System.out.println(debugLine + "Create configurationRotator.");
        ConfigurationRotator cr = new ConfigurationRotator( ccucm );
        System.out.println(debugLine + "cr.supportsPolling: " + cr.supportsPolling());
        System.out.println(debugLine + "Set ConfigurationRotator as SCM");
        project.setScm(cr);

        // Try to build model-1 and client-1 to se if they are compatible
        System.out.println(debugLine + "Scheduling a build for model-1 and client-1...");
        FreeStyleBuild b = project.scheduleBuild2(0).get();
        // now investigate result and print debug out
        assertNotNull(b);
        System.out.println(debugLine + "... build is done");
        System.out.println(debugLine + "Printing logfile: " + b.getLogFile());
        BufferedReader br = new BufferedReader(new FileReader(b.getLogFile()));
        String line = "";
        while ((line = br.readLine()) != null) {
            System.out.println("[JENKINS] " + line);
        }
        br.close();
        System.out.println(debugLine + "... done printing logfile");
        // build should be good
        System.out.println(debugLine + "build.getResult():" + b.getResult().toString());
        assertEquals(b.getResult(), Result.SUCCESS);


        ConfigurationRotatorBuildAction action = b.getAction(ConfigurationRotatorBuildAction.class);
        System.out.println(debugLine + "action: " + action);
        // action expected not to be null
        assertNotNull(action);

        // check config rotator result
        System.out.println(debugLine + "action.getResult(): " + action.getResult());
        assertEquals(action.getResult(), net.praqma.jenkins.configrotator.ConfigurationRotator.ResultType.COMPATIBLE);
        System.out.println(debugLine + "action.isCompatible: " + action.isCompatible());
        assertTrue(action.isCompatible());

        ClearCaseUCMConfiguration configuration = action.getConfiguration();
        System.out.println(debugLine + "getShortname(): " + configuration.getList().get(0).getBaseline().getShortname());
        System.out.println(debugLine + "getShortname(): " + configuration.getList().get(1).getBaseline().getShortname());
        assertEquals("model-1", configuration.getList().get(0).getBaseline().getShortname());
        assertEquals("client-1", configuration.getList().get(1).getBaseline().getShortname());




        /*
         * ******************************************************
         * Now doing a new build, and expect to find baseline model-2, and that
         * is compatible with client-1
         */
        System.out.println(debugLine + "Scheduling a build for model-2 and client-1...");
        b = project.scheduleBuild2(0).get();
        // now investigate result and print debug out
        assertNotNull(b);
        System.out.println(debugLine + "... build is done");
        System.out.println(debugLine + "Printing logfile: " + b.getLogFile());
        br = new BufferedReader(new FileReader(b.getLogFile()));
        line = "";
        while ((line = br.readLine()) != null) {
            System.out.println("[JENKINS] " + line);
        }
        br.close();
        System.out.println(debugLine + "... done printing logfile");
        // build should be good
        System.out.println(debugLine + "build.getResult():" + b.getResult().toString());
        assertEquals(b.getResult(), Result.SUCCESS);

        action = b.getAction(ConfigurationRotatorBuildAction.class);
        System.out.println(debugLine + "action: " + action);
        // action expected not to be null
        assertNotNull(action);

        // check config rotator result
        System.out.println(debugLine + "action.getResult(): " + action.getResult());
        assertEquals(action.getResult(), net.praqma.jenkins.configrotator.ConfigurationRotator.ResultType.COMPATIBLE);
        System.out.println(debugLine + "action.isCompatible: " + action.isCompatible());
        assertTrue(action.isCompatible());

        configuration = action.getConfiguration();
        System.out.println(debugLine + "getShortname(): " + configuration.getList().get(0).getBaseline().getShortname());
        System.out.println(debugLine + "getShortname(): " + configuration.getList().get(1).getBaseline().getShortname());
        assertEquals("model-2", configuration.getList().get(0).getBaseline().getShortname());
        assertEquals("client-1", configuration.getList().get(1).getBaseline().getShortname());

        /*
         * ******************************************************
         * Now doing a new build, and expect to find baseline model-3, and that
         * is compatible with client-1
         */
        System.out.println(debugLine + "Scheduling a build for model-3 and client-1...");
        b = project.scheduleBuild2(0).get();
        // now investigate result and print debug out
        assertNotNull(b);
        System.out.println(debugLine + "... build is done");
        System.out.println(debugLine + "Printing logfile: " + b.getLogFile());
        br = new BufferedReader(new FileReader(b.getLogFile()));
        line = "";
        while ((line = br.readLine()) != null) {
            System.out.println("[JENKINS] " + line);
        }
        br.close();
        System.out.println(debugLine + "... done printing logfile");
        // build should be good
        System.out.println(debugLine + "build.getResult():" + b.getResult().toString());
        assertEquals(b.getResult(), Result.SUCCESS);

        action = b.getAction(ConfigurationRotatorBuildAction.class);
        System.out.println(debugLine + "action: " + action);
        // action expected not to be null
        assertNotNull(action);

        // check config rotator result
        System.out.println(debugLine + "action.getResult(): " + action.getResult());
        assertEquals(action.getResult(), net.praqma.jenkins.configrotator.ConfigurationRotator.ResultType.COMPATIBLE);
        System.out.println(debugLine + "action.isCompatible: " + action.isCompatible());
        assertTrue(action.isCompatible());

        configuration = action.getConfiguration();
        System.out.println(debugLine + "getShortname(): " + configuration.getList().get(0).getBaseline().getShortname());
        System.out.println(debugLine + "getShortname(): " + configuration.getList().get(1).getBaseline().getShortname());
        assertEquals("model-3", configuration.getList().get(0).getBaseline().getShortname());
        assertEquals("client-1", configuration.getList().get(1).getBaseline().getShortname());




        /*
         * ******************************************************
         * Now doing a new build, and expect to find baseline model-3, and that
         * is compatible with client-2
         */
        System.out.println(debugLine + "Scheduling a build for model-3 and client-2...");
        b = project.scheduleBuild2(0).get();
        // now investigate result and print debug out
        assertNotNull(b);
        System.out.println(debugLine + "... build is done");
        System.out.println(debugLine + "Printing logfile: " + b.getLogFile());
        br = new BufferedReader(new FileReader(b.getLogFile()));
        line = "";
        while ((line = br.readLine()) != null) {
            System.out.println("[JENKINS] " + line);
        }
        br.close();
        System.out.println(debugLine + "... done printing logfile");
        // build should be good
        System.out.println(debugLine + "build.getResult():" + b.getResult().toString());
        assertEquals(b.getResult(), Result.SUCCESS);

        action = b.getAction(ConfigurationRotatorBuildAction.class);
        System.out.println(debugLine + "action: " + action);
        // action expected not to be null
        assertNotNull(action);

        // check config rotator result
        System.out.println(debugLine + "action.getResult(): " + action.getResult());
        assertEquals(action.getResult(), net.praqma.jenkins.configrotator.ConfigurationRotator.ResultType.COMPATIBLE);
        System.out.println(debugLine + "action.isCompatible: " + action.isCompatible());
        assertTrue(action.isCompatible());

        configuration = action.getConfiguration();
        System.out.println(debugLine + "getShortname(): " + configuration.getList().get(0).getBaseline().getShortname());
        System.out.println(debugLine + "getShortname(): " + configuration.getList().get(1).getBaseline().getShortname());
        assertEquals("model-3", configuration.getList().get(0).getBaseline().getShortname());
        assertEquals("client-2", configuration.getList().get(1).getBaseline().getShortname());


        /*
         * ******************************************************
         * Now doing a new build, and expect to find baseline model-3, and that
         * is compatible with client-3
         */
        System.out.println(debugLine + "Scheduling a build for model-3 and client-3...");
        b = project.scheduleBuild2(0).get();
        // now investigate result and print debug out
        assertNotNull(b);
        System.out.println(debugLine + "... build is done");
        System.out.println(debugLine + "Printing logfile: " + b.getLogFile());
        br = new BufferedReader(new FileReader(b.getLogFile()));
        line = "";
        while ((line = br.readLine()) != null) {
            System.out.println("[JENKINS] " + line);
        }
        br.close();
        System.out.println(debugLine + "... done printing logfile");
        // build should be good
        System.out.println(debugLine + "build.getResult():" + b.getResult().toString());
        assertEquals(b.getResult(), Result.SUCCESS);

        action = b.getAction(ConfigurationRotatorBuildAction.class);
        System.out.println(debugLine + "action: " + action);
        // action expected not to be null
        assertNotNull(action);

        // check config rotator result
        System.out.println(debugLine + "action.getResult(): " + action.getResult());
        assertEquals(action.getResult(), net.praqma.jenkins.configrotator.ConfigurationRotator.ResultType.COMPATIBLE);
        System.out.println(debugLine + "action.isCompatible: " + action.isCompatible());
        assertTrue(action.isCompatible());

        configuration = action.getConfiguration();
        System.out.println(debugLine + "getShortname(): " + configuration.getList().get(0).getBaseline().getShortname());
        System.out.println(debugLine + "getShortname(): " + configuration.getList().get(1).getBaseline().getShortname());
        assertEquals("model-3", configuration.getList().get(0).getBaseline().getShortname());
        assertEquals("client-3", configuration.getList().get(1).getBaseline().getShortname());




        /*
         * ******************************************************
         * Now doing to do a new build but there will be NO new baselines
         */
        System.out.println(debugLine + "Scheduling a build but expect no new baselines...");
        b = project.scheduleBuild2(0).get();
        System.out.println(debugLine + "... build is done");
        System.out.println(debugLine + "Checking for build is not null.");
        assertNotNull("Build check for null", b);
        // now investigate result and print debug out

        System.out.println(debugLine + "Printing logfile: " + b.getLogFile());
        br = new BufferedReader(new FileReader(b.getLogFile()));
        line = "";
        while ((line = br.readLine()) != null) {
            System.out.println("[JENKINS] " + line);
        }
        br.close();
        System.out.println(debugLine + "... done printing logfile");
        // build should be good
        System.out.println(debugLine + "build.getResult():" + b.getResult().toString());
        assertEquals(Result.FAILURE, b.getResult());

        action = b.getAction(ConfigurationRotatorBuildAction.class);
        System.out.println(debugLine + "action: " + action);
        // action expected to be null, as build failed because of no new baselines
        assertNull(action);


        System.out.println(debugLine + "Trying teardown with net.praqma.clearcase.util.SetupUtils.tearDown(ccenv.getPVob())");
        // try catch to avoid teardown failing tests - it's okay for teardown to fail
        // as our test environment later can be cleaned manually
        try 
        {
            net.praqma.clearcase.util.SetupUtils.tearDown(ccenv.getPVob());
        }
        catch (CleartoolException clex)
        {
            System.out.println(debugLine + "net.praqma.clearcase.util.SetupUtils.tearDown(ccenv.getPVob() throwed exception - ignoring on purpose");
            System.out.println(debugLine + "Exception was: " + clex.getMessage());
        }
        System.out.println(debugLine + "Done calling teardown....");

        System.out.println(debugLine + "Test done - waiting... trying avoid Jenkins failing due to clean temp dirs error");
        // waiting is important to ensure unique timestamps and let Jenkins clean
        // workspace after each test
        waiting(watingSeconds);

    }

    private static void waiting(int seconds) {

        long t0, t1;

        t0 = System.currentTimeMillis();

        do {
            t1 = System.currentTimeMillis();
        } while ((t1 - t0) < (seconds * 1000));
    }
}