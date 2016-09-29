package net.praqma.jenkins.configrotator;

import hudson.model.AbstractBuild;
import net.praqma.html.Html;
import net.praqma.util.xml.feed.Entry;
import net.praqma.util.xml.feed.Feed;
import net.praqma.util.xml.feed.FeedException;
import net.praqma.util.xml.feed.Person;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Abstract class defining one component of a configuration
 *
 * @author wolfgang
 */
public abstract class AbstractConfigurationComponent implements Serializable, Feedable {
    protected boolean changedLast = false;
    protected boolean fixed = false;

    public AbstractConfigurationComponent( boolean fixed ) {
        this.fixed = fixed;
    }

    public boolean isChangedLast() {
        return changedLast;
    }

    public void setChangedLast( boolean b ) {
        this.changedLast = b;
    }

    public boolean isFixed() {
        return fixed;
    }

    public abstract String getComponentName();

    public abstract String prettyPrint();

    @Override
    public File getFeedFile( File path ) {
        return new File( path, ConfigurationRotatorReport.urlTtransform( getComponentName() ) + ".xml" );
    }

    public Feed getFeed( File feedFile, String url, Date updated ) throws FeedException, IOException {
        String feedId = url + "feed/?component=" + ConfigurationRotatorReport.urlTtransform( getComponentName() );
        String feedTitle = getComponentName();

        Feed feed = ConfigurationRotatorReport.getFeedFromFile( feedFile, feedTitle, feedId, updated );

        return feed;
    }
    
    @Override
    public Entry getFeedEntry( AbstractBuild<?, ?> build, Date updated ) {
        ConfigurationRotatorBuildAction action = build.getAction( ConfigurationRotatorBuildAction.class );
        AbstractConfiguration configuration = action.getConfigurationWithOutCast();
        List<AbstractConfigurationComponent> components = configuration.getList();


        String id = build.getParent().getDisplayName() + "#" + build.getNumber() + ", " + getFeedId();

        Entry entry = new Entry( getFeedName() + " in new " + action.getResult().toString() + " configuration", id, updated );
        int l = components.size() - 1;
        entry.summary = getFeedName() + " is " + action.getResult().toString() + " with "
                + l + " other component" + ( l == 1 ? "" : "s" );

        entry.author = new Person( "Jenkins config-rotator job: "
                + build.getParent().getDisplayName() + ", build: #" + build.getNumber() );

        entry.content = configuration.getDescription( action );
        Html.Break br1 = new Html.Break();
        Html.Anchor linkFeeds = new Html.Anchor( ConfigurationRotatorReport.FeedFrontpageUrl(), "Click here for a list of available feeds" );
        Html.Break br2 = new Html.Break();
        Html.Anchor joblink = new Html.Anchor( ConfigurationRotatorReport.GenerateJobUrl( build ), "Click here to go to the build that created this feed" );

        entry.content += configuration.toHtml() + br1 + linkFeeds + br2 + joblink;

        return entry;
    }

    public static class Element {
        public String element;
        public boolean highlight;

        public Element( String element, boolean highlight ) {
            this.element = element;
            this.highlight = highlight;
        }
    }

    protected String getBasicHtml( StringBuilder builder, Element... elements ) {
        builder.append( "<tr>" );
        for( Element e : elements ) {
            if( e.highlight ) {
                builder.append( "<td style=\"font-weight:bold;color:#FF6633;padding-right:15px\">" ).append( e.element ).append( "</td>" );
            } else {
                builder.append( "<td style=\"padding-right:15px\">" ).append( e.element ).append( "</td>" );
            }
        }
        builder.append( "</tr>" );
        return builder.toString();
    }
}
