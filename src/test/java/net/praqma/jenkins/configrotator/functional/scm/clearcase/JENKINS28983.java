/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.jenkins.configrotator.functional.scm.clearcase;

import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.Slave;
import java.util.List;
import net.praqma.clearcase.test.junit.ClearCaseRule;
import net.praqma.jenkins.configrotator.AbstractConfiguration;
import net.praqma.jenkins.configrotator.AbstractConfigurationComponent;
import net.praqma.jenkins.configrotator.ConfigRotatorProject;
import net.praqma.jenkins.configrotator.ConfigRotatorRule2;
import net.praqma.jenkins.configrotator.ConfigurationRotator;
import net.praqma.jenkins.configrotator.ConfigurationRotatorBuildAction;
import net.praqma.jenkins.configrotator.ProjectBuilder;
import net.praqma.jenkins.configrotator.SystemValidator;
import static net.praqma.jenkins.configrotator.functional.scm.clearcase.JENKINS29975.ccenv;
import net.praqma.jenkins.configrotator.scm.clearcaseucm.ClearCaseUCM;
import net.praqma.jenkins.configrotator.scm.clearcaseucm.ClearCaseUCMTarget;
import net.praqma.logging.PraqmaticLogFormatter;
import net.praqma.util.test.junit.LoggingRule;
import static org.junit.Assert.assertNotNull;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

/**
 *
 * @author Mads
 */
public class JENKINS28983 {
    
    private static final String ISSUE_NR = "jenkins_28983";    
    public static final ClearCaseRule ccenv = new ClearCaseRule(ISSUE_NR,"setup_no_label.xml");
    public static final LoggingRule lrule = new LoggingRule( "net.praqma" ).setFormat( PraqmaticLogFormatter.TINY_FORMAT );
    
    @ClassRule
    public static final TestRule chain = RuleChain.outerRule( lrule ).around( ccenv );
    
    @ClassRule
    public static final ConfigRotatorRule2 crrule = new ConfigRotatorRule2(JENKINS29975.class);
    
    @Test
    public void testFastForwardMode() throws Exception {
        ProjectBuilder builder = new ProjectBuilder( new ClearCaseUCM( ccenv.getPVob() ) ).setName( ISSUE_NR ).setUseNewest();
        Slave sl = crrule.createSlave();
        

        ConfigRotatorProject project = builder.getProject();
        project.addTarget( new ClearCaseUCMTarget( "model-1@" + ccenv.getPVob() + ", INITIAL, false" ) ).
                addTarget( new ClearCaseUCMTarget( "client-1@" + ccenv.getPVob() + ", INITIAL, false" ) );
        
        hudson.model.Project<?,?> jProject = project.getJenkinsProject();
        
        //First build (initial configuration) 
        AbstractBuild<?,?> b1 = crrule.buildProject(jProject, false, sl);
        SystemValidator<ClearCaseUCMTarget> targets1 = new SystemValidator<ClearCaseUCMTarget>(b1);
        targets1.checkAction(true)
                .checkTargets( new ClearCaseUCMTarget( "model-1@" + ccenv.getPVob() + ", INITIAL, false" ), new ClearCaseUCMTarget( "client-1@" + ccenv.getPVob() + ", INITIAL, false" ) )
                .checkExpectedResult(Result.SUCCESS).validate();
        
        ConfigurationRotatorBuildAction resetPoint = b1.getAction(ConfigurationRotatorBuildAction.class);
        
        //Assert that there is a new configuration in acrs:
        ConfigurationRotator rotator = (ConfigurationRotator)b1.getProject().getScm();        
        AbstractConfiguration listArg2 = rotator.getAcrs().getConfiguration();
        assertNotNull(listArg2);

        //Next build (Newest on BOTH client and model)
        AbstractBuild<?,?> b2 = crrule.buildProject(jProject, false, sl);
        SystemValidator<ClearCaseUCMTarget> targets2 = new SystemValidator<ClearCaseUCMTarget>(b2);
        targets2.checkAction(true)
                .checkTargets( new ClearCaseUCMTarget( "model-3@" + ccenv.getPVob() + ", INITIAL, false" ), new ClearCaseUCMTarget( "client-3@" + ccenv.getPVob() + ", INITIAL, false" ) )
                .checkExpectedResult(Result.SUCCESS).validate();
        
        //Assert that there is a new configuration in acrs:
        ConfigurationRotator rotator2 = (ConfigurationRotator)b2.getProject().getScm();        
        AbstractConfiguration listArg3 = rotator2.getAcrs().getConfiguration();
        assertNotNull(listArg3);
        
        //Next build (Nothing to do...baselines exhausted)
        AbstractBuild<?,?> b3 = crrule.buildProject(jProject, false, sl);
        SystemValidator<ClearCaseUCMTarget> targets3 = new SystemValidator<ClearCaseUCMTarget>(b3);
        targets3.checkExpectedResult(Result.NOT_BUILT).validate();
        
        //Reset
        project.getConfigurationRotator().setConfigurationByAction(jProject, resetPoint);

        //Expect the same as the first
        AbstractBuild<?,?> b1reset = crrule.buildProject(jProject, false, sl);
        SystemValidator<ClearCaseUCMTarget> targets1reset = new SystemValidator<ClearCaseUCMTarget>(b1reset);
        targets1reset.checkAction(true)
                .checkTargets( new ClearCaseUCMTarget( "model-1@" + ccenv.getPVob() + ", INITIAL, false" ), new ClearCaseUCMTarget( "client-1@" + ccenv.getPVob() + ", INITIAL, false" ) )
                .checkExpectedResult(Result.SUCCESS).validate();
        
    }
}
