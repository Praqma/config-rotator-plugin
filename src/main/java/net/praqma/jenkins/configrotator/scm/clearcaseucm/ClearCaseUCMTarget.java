package net.praqma.jenkins.configrotator.scm.clearcaseucm;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.ListBoxModel;
import net.praqma.clearcase.ucm.entities.Project;
import net.praqma.jenkins.configrotator.AbstractTarget;
import org.kohsuke.stapler.DataBoundConstructor;

public class ClearCaseUCMTarget extends AbstractTarget<ClearCaseUCMTarget> {

    private String component;
    private String baselineName;
    private Project.PromotionLevel level;
    private boolean fixed;

    public ClearCaseUCMTarget() {
    }

    /**
     * Warning: Only one databound constructor per component. Figured this out
     * the hard way.
     *
     * @param component the Component
     */
    public ClearCaseUCMTarget(String component) {
        this.component = component;
    }

    /**
     * New constructor. Builds a correct component string for backwards
     * compatability.
     *
     * @param baselineName the name of the baseline
     * @param level the baseline promotion level
     * @param fixed fixed or not
     */
    @DataBoundConstructor
    public ClearCaseUCMTarget(String baselineName, Project.PromotionLevel level, boolean fixed) {
        this.component = baselineName + ", " + level + ", " + fixed;
        this.baselineName = baselineName;
        this.level = level;
        this.fixed = fixed;
    }

    public String getComponent() {
        return component;
    }

    public String getBaselineName() {
        return baselineName;
    }

    public void setBaselineName(String baselineName) {
        this.baselineName = baselineName;
    }

    public Project.PromotionLevel getLevel() {
        return level;
    }

    public void setLevel(Project.PromotionLevel level) {
        this.level = level;
    }

    public void setFixed(boolean fixed) {
        this.fixed = fixed;
    }

    public boolean getFixed() {
        return fixed;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    @Override
    public String toString() {
        return String.format("%s", component);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (other instanceof ClearCaseUCMTarget) {
            ClearCaseUCMTarget o = (ClearCaseUCMTarget) other;

            return component.equals(o.getComponent());
        } else {
            return false;
        }
    }

    @Override
    public Descriptor<ClearCaseUCMTarget> getDescriptor() {
        return new DescriptorImpl();
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ClearCaseUCMTarget> {
        public DescriptorImpl() { }

        @Override
        public String getDisplayName() {
            return "ClearCase UCM Configuration";
        }

        public ListBoxModel doFillLevelItems() {
            ListBoxModel lbm = new ListBoxModel();
            lbm.add("INITIAL", "INITIAL");
            lbm.add("BUILT","BUILT");
            lbm.add("TESTED","TESTED");
            lbm.add("RELEASED","RELEASED");
            lbm.add("REJECTED","REJECTED");
            return lbm;
        }

    }
}
