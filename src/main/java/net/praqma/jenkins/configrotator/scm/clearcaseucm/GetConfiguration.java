package net.praqma.jenkins.configrotator.scm.clearcaseucm;

import java.io.File;
import java.io.IOException;

import net.praqma.clearcase.exceptions.ClearCaseException;
import hudson.FilePath.FileCallable;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import net.praqma.clearcase.exceptions.UnableToInitializeEntityException;
import org.jenkinsci.remoting.RoleChecker;

public class GetConfiguration implements FileCallable<ClearCaseUCMConfigurationComponent> {

    private String[] units;
    private TaskListener listener;

    public GetConfiguration( String[] units, TaskListener listener ) {
        this.units = units;
        this.listener = listener;
    }

    @Override
    public ClearCaseUCMConfigurationComponent invoke( File f, VirtualChannel channel ) throws IOException, InterruptedException {
        try {
            boolean fixed = false;
            if( units[2].trim().equalsIgnoreCase( "manual" ) || units[2].trim().matches( "^\\s*$" ) || units[2].trim().matches( "^(?i)fixed*$" ) || units[2].trim().matches( "^(?i)true*$" ) ) {
                fixed = true;
            } else {
                fixed = false;
            }
            return new ClearCaseUCMConfigurationComponent( units[0].trim(), units[1].trim(), fixed );
        } catch (UnableToInitializeEntityException ninitex) {
            //this happens when the baseline has an incorrect pattern.
            IOException ioe2 = new IOException(String.format("Failed to load baseline from baseline configuration string (%s)%nCheck your component configuration syntax.", units[0].trim()), ninitex.getCause());
            throw ioe2;

        } catch( ClearCaseException e ) {
            // ClearCaseException can not be passed through from slave to master
            // but IOException can, so using that one, and packing out later
            IOException ioe = new IOException( e );
            throw ioe;
        }
    }

    @Override
    public void checkRoles(RoleChecker rc) throws SecurityException {
        //NO-op
    }
}
