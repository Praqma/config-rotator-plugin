package net.praqma.jenkins.configrotator;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

import net.praqma.jenkins.configrotator.ConfigurationRotator.ResultType;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import java.util.logging.Level;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.praqma.jenkins.configrotator.scm.contribute.CompatabilityCompatible;
import net.praqma.jenkins.configrotator.scm.contribute.RemoteCompatabilityContributor;
import org.jenkinsci.plugins.compatibilityaction.CompatibilityDataException;
import org.jenkinsci.plugins.compatibilityaction.CompatibilityDataPlugin;

public class ConfigurationRotatorPublisher extends Notifier {

    private static final Logger logger = Logger.getLogger(ConfigurationRotatorPublisher.class.getName());

    public ConfigurationRotatorPublisher() {
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        PrintStream out = listener.getLogger();

        /* This must be ConfigRotator job */
        if (build.getProject().getScm() instanceof ConfigurationRotator) {
            ConfigurationRotator scmConverted = (ConfigurationRotator)build.getProject().getScm();
            ConfigurationRotatorBuildAction action = build.getAction(ConfigurationRotatorBuildAction.class);
            if (action != null) {

                Result r = build.getResult();

                if (r != null && r.isBetterOrEqualTo(Result.SUCCESS)) {
                    action.setResult(ResultType.COMPATIBLE);
                } else {
                    action.setResult(ResultType.INCOMPATIBLE);
                }

                /**
                 * If the database is installed try to store information
                 */
                if(Jenkins.getInstance().getPlugin("compatibility-action-storage") != null) {
                    if(scmConverted.getAcrs().isContribute()) {
                        try {
                            FilePath p = build.getWorkspace();
                            if(p != null) {
                                CompatabilityCompatible compatible = scmConverted.getAcrs().getConverter().convert(build.getAction(ConfigurationRotatorBuildAction.class));
                                listener.getLogger().println(ConfigurationRotator.LOGGERNAME + "Preparing to contribute data about compatability");
                                CompatibilityDataPlugin cp = GlobalConfiguration.all().get(CompatibilityDataPlugin.class);
                                if(cp != null) {
                                    p.act(new RemoteCompatabilityContributor(compatible, cp.getProvider(), listener));
                                }
                            }
                        } catch (CompatibilityDataException dataex) {
                            listener.getLogger().println(dataex.getMessage());
                        } catch (Exception ex) {
                            listener.getLogger().println("Unknown error. See logs for more detail");
                            logger.log(Level.WARNING, "Unknown error encountered while trying to add comptability data. Trace follows", ex);
                        }
                    }
                }

                out.println(ConfigurationRotator.LOGGERNAME + "Configuration is " + action.getResult());

                return AbstractPostConfigurationRotator.doit(build.getWorkspace(), listener, action);


            } else {
                DiedBecauseAction da = build.getAction(DiedBecauseAction.class);
                out.println(ConfigurationRotator.LOGGERNAME + "Action was null, unable to set compatibility of configuration");

                if (da != null) {
                    out.println(da.toString());
                    logger.fine(da.toString());
                    if (!da.died()) {
                        hadNothingToDo(build);
                    }
                }
            }
        }

        return true;
    }

    public void hadNothingToDo(AbstractBuild build) throws IOException {
        String d = build.getDescription();
        if (d != null) {
            build.setDescription((d.length() > 0 ? d + "<br/>" : "") + "Nothing to do");
        } else {
            build.setDescription("Nothing to do");
        }

        build.setResult(Result.NOT_BUILT);
    }

    @Override
    public boolean needsToRunAfterFinalized() {
        return false;
    }

    @Override
    public Collection<Action> getProjectActions(AbstractProject<?, ?> project) {
        return Collections.singleton((Action) new ConfigurationRotatorProjectAction(project));
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Override
        public Notifier newInstance(StaplerRequest req, JSONObject formData) throws Descriptor.FormException {
            return new ConfigurationRotatorPublisher();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Configuration Rotator Publisher";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws Descriptor.FormException {
            save();
            return super.configure(req, json);
        }

        public DescriptorImpl() {
            super(ConfigurationRotatorPublisher.class);
            load();
        }
    }
}
