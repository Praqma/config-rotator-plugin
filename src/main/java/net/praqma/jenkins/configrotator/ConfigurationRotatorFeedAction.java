package net.praqma.jenkins.configrotator;

import hudson.model.Action;
import jenkins.model.Jenkins;
import net.praqma.util.xml.feed.AtomPublisher;
import net.praqma.util.xml.feed.Feed;
import org.apache.commons.io.FileUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public abstract class ConfigurationRotatorFeedAction implements Action {
    
    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    public abstract String getComponentName();

    public String getFeedUrl( String component ) {
        return Jenkins.getInstance().getRootUrl() + ConfigurationRotator.URL_NAME + "/" + getComponentName() + "/feed?component=" + component;
    }

    public String getComponentName( String fileName ) {
        return fileName.substring( 0, fileName.lastIndexOf( "." ) );
    }

    public ArrayList<File> getComponents() {
        FileFilter xmlFilter = new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isFile() && file.getName().endsWith( ".xml" );
            }
        };
        return getComponents( xmlFilter );
    }

    public String getFeedTitle( File feed ) {
        String title = "Unknown";
        try {
            title = Feed.getFeed( new AtomPublisher(), feed ).title;
        } catch( Exception e ) {
            /* No op */
        }

        return title;
    }

    public ArrayList<File> getComponents( FileFilter filter ) {

        ArrayList<File> list = new ArrayList<File>();

        File path = new File( ConfigurationRotator.getFeedPath(), getComponentName() );

        for( File f : path.listFiles( filter ) ) {
            list.add( f );
        }

        return list;
    }

    public void doFeed( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        String component = req.getParameter( "component" );
        File file = new File( new File( ConfigurationRotator.getFeedPath(), getComponentName() ), component + ".xml" );

        if( file.exists() ) {
            rsp.serveFile( req, FileUtils.openInputStream( file ), file.lastModified(), file.getTotalSpace(), file.getName() );
        } else {
            rsp.sendError( HttpServletResponse.SC_NOT_FOUND );
        }
    }
}
