package net.praqma.jenkins.configrotator.scm.clearcaseucm;

import net.praqma.clearcase.exceptions.ClearCaseException;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Project;
import net.praqma.clearcase.ucm.entities.Project.PromotionLevel;
import net.praqma.jenkins.configrotator.*;

public class ClearCaseUCMConfigurationComponent extends AbstractConfigurationComponent {

    private Baseline baseline;
    private PromotionLevel plevel;

    public ClearCaseUCMConfigurationComponent( Baseline baseline, PromotionLevel plevel, boolean fixed ) {
        super( fixed );
        this.baseline = baseline;
        this.plevel = plevel;
    }

    public ClearCaseUCMConfigurationComponent( String baseline, String plevel, boolean fixed ) throws ClearCaseException {
        super( fixed );
        this.baseline = Baseline.get( baseline ).load();
        this.plevel = Project.PromotionLevel.valueOf( plevel );
    }
    
    @Override
    public ClearCaseUCMConfigurationComponent clone() {
        ClearCaseUCMConfigurationComponent cc = new ClearCaseUCMConfigurationComponent( this.baseline, this.plevel, this.fixed );
        return cc;
    }

    public void setBaseline( Baseline baseline ) {
        this.baseline = baseline;
    }

    public Baseline getBaseline() {
        return baseline;
    }

    public PromotionLevel getPlevel() {
        return plevel;
    }

    @Override
    public String toString() {
        return baseline.getNormalizedName() + "@" + plevel + "(" + fixed + "/" + changedLast + ")";
    }

    @Override
    public String prettyPrint() {
        return baseline.getNormalizedName() + "@" + plevel;
    }

    @Override
    public boolean equals( Object other ) {
        if( other == this ) {
            return true;
        }

        if( other instanceof ClearCaseUCMConfigurationComponent ) {
            ClearCaseUCMConfigurationComponent o = (ClearCaseUCMConfigurationComponent) other;

            return ( o.baseline.equals( baseline ) && ( o.plevel.equals( plevel ) ) && ( o.isFixed() == fixed ) );
        } else {
            return false;
        }
    }

    @Override
    public String getComponentName() {
        return baseline.getComponent().getNormalizedName();
    }

    @Override
    public String getFeedName() {
        return baseline.getComponent().getNormalizedName();
    }

    @Override
    public String getFeedId() {
        return baseline.getComponent().getNormalizedName();
    }

    /**
     *
     * @return a html'ified version of this clearcase components. In this case it is a table row.
     */
    @Override
    public String toHtml() {
        StringBuilder builder = new StringBuilder();
        builder.append( "<tr>" );
        builder.append( "<td style=\"padding:5px 10px;\">" ).append( baseline.getComponent().getNormalizedName() ).append( "</td>" );
        builder.append( "<td style=\"padding:5px 10px;\">" ).append( baseline.getStream().getNormalizedName() ).append( "</td>" );
        if( isChangedLast() ) {
            builder.append( "<td style=\"font-weight:bold;color:#FF6633;padding:5px 10px;\">" ).append( baseline.getNormalizedName() ).append( "</td>" );
        } else {
            builder.append( "<td style=\"padding:5px 10px;\">" ).append( baseline.getNormalizedName() ).append( "</td>" );
        }
        builder.append( "<td style=\"padding:5px 10px;\">" ).append( plevel.toString() ).append( "</td>" );
        builder.append( "<td style=\"padding:5px 10px;\">" ).append( fixed ).append( "</td>" ).append( "</tr>" );
        return builder.toString();
    }


}