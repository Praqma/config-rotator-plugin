package net.praqma.jenkins.configrotator;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.scm.SCM;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.logging.Logger;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class SystemValidator<T extends AbstractTarget> {

    private static Logger logger = Logger.getLogger( SystemValidator.class.getName() );

    private ConfigurationRotator cr;
    private AbstractBuild<?, ?> build;
    private ConfigurationRotatorBuildAction action;
    private PrintStream out;

    /**/
    private Result expectedResult;
    private boolean checkExpectedResult = false;

    /**/
    private boolean compatible;
    private boolean checkCompatible = false;

    /**/
    private boolean wasReconfigured;
    private boolean checkWasReconfigured = false;

    /**/
    private boolean actionIsValid;
    private boolean checkActionIsValid = false;

    /**/
    private List<AbstractTarget> targets = new LinkedList<AbstractTarget>();
    private boolean checkTargets = false;

    /* Validate path elements */
    private boolean checkPathElements = false;
    private Map<FilePath, List<Element>> pathsToCheck = new HashMap<FilePath, List<Element>>();

    public SystemValidator() {

    }

    public SystemValidator( AbstractBuild<?, ?> build ) {
        this( build, System.out );
    }

    public SystemValidator( AbstractBuild<?, ?> build, PrintStream out ) {
        this.build = build;
        this.out = out;

        SCM scm = build.getProject().getScm();
        if( scm instanceof ConfigurationRotator ) {
            this.cr = (ConfigurationRotator) scm;
        } else {
            throw new IllegalStateException( build.getProject().getScm() + " is not ConfigRotator" );
        }

        action = build.getAction( ConfigurationRotatorBuildAction.class );
    }

    public void validate() {
        
        logger.info( "-----= Validating build: " + this.build.getProject().getDisplayName() + " : " + this.build.getDisplayName() + " =-----" );

        try {
            
            if( this.checkExpectedResult ) {
                logger.info( "Expected result must be " + this.expectedResult + " (" + build.getResult() + ")" );
                assertThat( "Validating expected result", build.getResult(), is( this.expectedResult ) );
            }

            if( this.checkCompatible ) {
                logger.info( "Compatibility must be " + ( this.compatible ? "compatible" : "incompatible" ) + " (" + action.isCompatible() + ")" );
                assertThat( "Validating compatibility", action.isCompatible(), is( this.compatible ) );
            }

            if( this.checkWasReconfigured ) {
                logger.info( "Reconfigured must be " + this.wasReconfigured + " (" + cr.getAcrs().wasReconfigured( build.getProject() ) + ")" );
                assertThat( "Validating reconfiguration", cr.getAcrs().wasReconfigured( build.getProject() ), is( this.wasReconfigured ) );
            }

            if( this.checkTargets ) {
                logger.info("Targets must be " + this.targets);
                logger.info("Target size must be " + this.targets.size());
                for (int i = 0; i < this.targets.size(); i++) {
                    AbstractTarget rotatorTarget = cr.getAcrs().getTargets().get(i);
                    AbstractTarget expectedTarget = this.targets.get(i);
                    logger.info(String.format(" * %s == %s", rotatorTarget, equalTo(expectedTarget)));
                    assertThat("Validating target", rotatorTarget, equalTo(expectedTarget));
                }
            }

            if( this.checkActionIsValid ) {
                logger.info( "Action must be " + ( this.actionIsValid ? "valid" : "invalid" ) + " (" + action + ")" );
                if( this.actionIsValid ) {
                    assertNotNull( "Action was not valid", action );
                } else {
                    assertNull( "Action was not null", action );
                }
            }

            if( this.checkPathElements ) {
                logger.info( "Checking path elements" );
                try {
                    doCheckPaths();
                } catch( Exception e ) {
                    fail( e.getMessage() );
                }
            }

            if( this.checkContent ) {
                logger.info( "Checking content of " + contentFile );
                try {
                    doCheckContent();
                } catch ( Exception e ) {
                    fail( e.getMessage() );
                }
            }
            
        } catch(AssertionError assertError) {
            logger.info("-----=Console output for failed system=-----");
            try {
                String console = FileUtils.readFileToString(build.getLogFile());
                logger.info(console);
            } catch (Exception ex) { }
            logger.info("-----=End console output for failed system=-----");
            throw assertError;
        }

        logger.info( "Successfully validated system" );

        logger.info( "-----= Successfully validated system =-----" );
        logger.info( "" );
    }

    public void validatePath() {
        logger.info( "Validating path elements" );
        try {
            doCheckPaths();
        } catch( Exception e ) {
            fail( e.getMessage() );
        }
    }

    public SystemValidator checkExpectedResult( Result expectedResult ) {
        this.expectedResult = expectedResult;
        this.checkExpectedResult = true;

        return this;
    }

    public SystemValidator checkCompatability( boolean compatible ) {
        this.compatible = compatible;
        this.checkCompatible = true;

        return this;
    }

    public SystemValidator checkWasReconfigured( boolean wasReconfigured ) {
        this.wasReconfigured = wasReconfigured;
        this.checkWasReconfigured = true;

        return this;
    }

    public SystemValidator checkAction( boolean valid ) {
        this.checkActionIsValid = true;
        this.actionIsValid = valid;

        return this;
    }

    public SystemValidator checkTargets( T... targets ) {
        for( T t : targets ) {
            this.targets.add( t );
        }
        this.checkTargets = true;

        return this;
    }

    public static class Element {
        private boolean mustExist;
        private String element;

        public Element( String element, boolean mustExist ) {
            this.element = element;
            this.mustExist = mustExist;
        }

        @Override
        public String toString() {
            return element;
        }
    }

    public SystemValidator checkPath( FilePath path, List<Element> elements ) {
        this.checkPathElements = true;

        pathsToCheck.put( path, elements );
        String[] l = new String[] {};

        return this;
    }

    public SystemValidator addElementToPathCheck( FilePath path, Element element ) {
        this.checkPathElements = true;

        if( pathsToCheck.containsKey( path ) ) {
            pathsToCheck.get( path ).add( element );
        } else {
            List<Element> e = new ArrayList<Element>();
            e.add( element );
            pathsToCheck.put( path, e );
        }

        return this;
    }

    private void doCheckPaths() throws IOException, InterruptedException {
        for( FilePath path : pathsToCheck.keySet() ) {
            List<Element> elements = pathsToCheck.get( path );
            logger.severe( "Checking " + path );

            for( Element element : elements ) {
                if( element.mustExist ) {
                    logger.info( "Path must have " + element );
                    assertTrue( "The path " + path + " does not have " + element, new FilePath( path, element.element ).exists() );                    
                } else {
                    logger.info( "Path must NOT have " + element );
                    assertFalse( "The path " + path + " does have " + element, new FilePath( path, element.element ).exists() );                    
                }
            }
        }
    }

    private boolean checkContent = false;
    private File contentFile;
    private String content = "";

    public SystemValidator checkContent( File file, String content ) {
        this.checkContent = true;
        this.contentFile = file;
        this.content = content;

        return this;
    }

    private void doCheckContent() throws IOException {
        String oc = FileUtils.readFileToString( contentFile );

        logger.info( "Content of " + contentFile + " must be " + content + "(" + oc + ")" );
        assertThat( oc, is( content ) );
    }
}
