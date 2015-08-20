package net.praqma.jenkins.configrotator.scm.clearcaseucm;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.TaskListener;
import net.praqma.jenkins.configrotator.AbstractConfigurationRotatorSCM;
import net.praqma.jenkins.configrotator.AbstractPostConfigurationRotator;
import net.praqma.jenkins.configrotator.ConfigurationRotator;
import net.praqma.jenkins.configrotator.ConfigurationRotatorBuildAction;

@Extension
public class ClearCaseUCMPostBuild extends AbstractPostConfigurationRotator {

    @Override
    public boolean perform(FilePath workspace, TaskListener listener, ConfigurationRotatorBuildAction action) {
        ClearCaseUCMConfiguration current = action.getConfiguration();

        try {
            if (current != null) {
                ConfigurationRotator rotator = (ConfigurationRotator) action.getBuild().getProject().getScm();
                if (current.getChangedComponent() == null) {
                    action.setDescription("New Configuration - no changes yet");
                } else {
                    int currentComponentIndex = current.getChangedComponentIndex();
                    String currentBaseline = ((ClearCaseUCMConfigurationComponent) current.getChangedComponent()).getBaseline().getNormalizedName();
                    ConfigurationRotatorBuildAction previous = rotator.getAcrs().getLastResult(action.getBuild().getProject(), ClearCaseUCM.class);
                    String previousBaseline = ((ClearCaseUCMConfiguration) previous.getConfiguration()).getList().get(currentComponentIndex).getBaseline().getNormalizedName();
                    action.setDescription(String.format("Baseline changed from %s to %s", previousBaseline, currentBaseline));
                }
            }
        } catch (Exception ex) {
            listener.getLogger().println("Failed to create description for job: " + ex);
        }
        return true;
    }

    @Override
    public Class<? extends AbstractConfigurationRotatorSCM> tiedTo() {
        return ClearCaseUCM.class;
    }
}
