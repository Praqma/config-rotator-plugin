/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.jenkins.configrotator.scm.contribute;

import hudson.model.TaskListener;
import hudson.remoting.Callable;
import java.util.logging.Logger;
import jenkins.model.GlobalConfiguration;
import net.praqma.jenkins.configrotator.ConfigurationRotatorBuildAction;
import org.jenkinsci.plugins.compatibilityaction.CompatibilityDataException;
import org.jenkinsci.plugins.compatibilityaction.CompatibilityDataPlugin;
import org.jenkinsci.plugins.compatibilityaction.MongoProviderImpl;
import org.jenkinsci.remoting.RoleChecker;

/**
 *
 * @author Mads
 */
public class CompatabilityContributor implements Callable<Boolean, CompatibilityDataException> {

    private static final Logger LOG = Logger.getLogger(CompatabilityContributor.class.getName());
    private TaskListener listener;
    private ConfigurationRotatorBuildAction action;
    private ConfigRotatorCompatabilityConverter converter;

    public CompatabilityContributor() { }

    public CompatabilityContributor(TaskListener listener, ConfigurationRotatorBuildAction action, ConfigRotatorCompatabilityConverter converter) {
        this.listener = listener;
        this.action = action;
        this.converter = converter;
    }

    @Override
    public Boolean call() throws CompatibilityDataException {
        if(action != null) {
            //Only add if the action is not 'Nothing to do' (grey)
            if(action.isCompatible() && converter != null) {
                CompatabilityCompatible compatabilityCompatible = converter.convert(action);

                 if(GlobalConfiguration.all().get(CompatibilityDataPlugin.class).getProvider() instanceof MongoProviderImpl) {
                    LOG.finest("Global configuration present, adding data to MongoDB");
                    MongoProviderImpl impl =  ((MongoProviderImpl)GlobalConfiguration.all().get(CompatibilityDataPlugin.class).getProvider());
                    impl.create(compatabilityCompatible);
                    LOG.finest(String.format("Done adding compatability information. Added compatability %s", compatabilityCompatible) );
                 }
            }

        }

        listener.getLogger().println("Added comptability in Remote!");
        return true;
    }

    /**
     * @return the listener
     */
    public TaskListener getListener() {
        return listener;
    }

    /**
     * @param listener the listener to set
     */
    public void setListener(TaskListener listener) {
        this.listener = listener;
    }

    @Override
    public void checkRoles(RoleChecker rc) throws SecurityException {
        //NO-OP
    }
}
