package net.praqma.jenkins.configrotator;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.praqma.util.xml.feed.*;

/**
 * <p>
 * RunLister implements onCompleted method that runs for every job on Jenkins
 * when completed, thus we check if it is a CR job and harvest the result of
 * the configuration.
 * </p>
 * <p>
 * Design notes - using a runListener vs. notifier for writing feed
 * -----------------------------------------------------------------
 * </p>
 * <p>
 * http://jenkinshost/config-rotator shows a summary with atom-feed and links
 * for the feed for every configuration of component tried with the config-rotator
 * plug-in (from now denoted CR) on the Jenkins host.
 * </p>
 * <p>
 * That is every job using the plug-in could have a result that should be shown
 * in one or more feeds, thus we must "subscribe" or ensure these data are
 * collected.
 * </p>
 * <p>
 * We already decided for an easy format for storing the feed - one file per
 * component in the feed XML format.
 * </p>
 * <p>
 * The feed can be written easily from two places: a runlistener or from the
 * notifier part of CR.
 * </p>
 * <p>
 * We chose to implement this as a runlistener we find the runlistener way of
 * doing it more clear instead of using the notifier (the post build step).
 * We know there might be some concurrency problems writing the feed files
 * if a lots of build finish at the same time.
 * TODO: This will be handled later and implemented.
 * </p>
 * <p>
 * Writing feeds: we would have liked to use a fully tested and mature open
 * source library for writing feeds, and did a short searching and found to
 * large one: ROME and Apache Abdera
 * ROME though, seemed not to be maintained or changed for some time,
 * but seemed the easiest to use.
 * NONE of them did we succeed in use: both had weird classloader problems under
 * Jenkins (but not if using them alone). The problem seems to comes from Jenkins
 * making the hpi file, from splits of jar-files. Others have this problem,
 * and we do to with some internal libraries (cool test case not being able to
 * use setup.xml across projects).
 * Thus we ended up creating our own simple framework in praqmajutils
 * </p>
 * @author bue
 */
@Extension
public class ConfigurationRotatorRunListener extends RunListener<Run> {

    private static Logger logger = Logger.getLogger( ConfigurationRotatorReport.class.getName() );

    private TaskListener localListener;

    public ConfigurationRotatorRunListener() {
        super( Run.class );

    }

    /**
     * Run listener for ConfigurationRotator jobs that does atom feed writing
     * for the results.
     * Writes entries to an existing feed, which is read from the component-
     * feedfile. If this file does not exist, an initial feed is created incl.
     * the file.
     * <p>
     * Upon adding the new entry, the feed is written to the file
     * (overwriting the existing feed).
     * Feed clients uses the entries id and timestamps, so they only see
     * the new entries added.
     * </p>
     * <p>
     * A component, as we for now only speak ClearCase, is only unique combined
     * with the PVob-name, so component feed-files are stored in folders under
     * there PVob in the feed-directory.
     * </p>
     * When a configuration is know to be compatible or incompatible, several
     * feeds must be updated. Say CR1-1 and CR2-1 and CR3-2 all in PVob 'myPVob'
     * was found to be compatible, then three feed are updated.
     * Eg. CR1-1 feed becomes an entry about it found compatible or incompatible
     * with components CR2-1 and CR3-2.
     * CR2-1 will have a feed entry about compatibility with CR1-1 and CR3-2.
     * They will be stored in:
     * JENKINNS-ROOTFOLDER/config-rotator/feed/myPVob/CR1-1.xml etc.
     *
     * @param run current running build
     * @param listener Jenkins listener
     */
    @Override
    public void onCompleted( Run run, TaskListener listener ) {
        localListener = listener;

        AbstractBuild<?, ?> build = (AbstractBuild<?, ?>) run;
        if( build.getProject().getScm() instanceof ConfigurationRotator ) {

            AbstractConfigurationRotatorSCM acscm = ((ConfigurationRotator)build.getProject().getScm()).getAcrs();
            ConfigurationRotatorBuildAction action = build.getAction( ConfigurationRotatorBuildAction.class );
            // if no action, build failed someway to set ConfigurationRotatorBuildAction, thus we can not
            // say anything about configuration.
            if( action != null ) {
                AbstractConfiguration configuration = action.getConfigurationWithOutCast();
                List<AbstractConfigurationComponent> components = configuration.getList();

                try {

                    for( AbstractConfigurationComponent component : components ) {
                        File feedFile = component.getFeedFile( acscm.getFeedPath() );
                        Date updated = new Date();

                        Feed feed = component.getFeed( feedFile, acscm.getFeedURL(), updated );
                        Entry e = component.getFeedEntry( build, updated );

                        feed.addEntry( e );

                        feed.updated = updated;

                        writeFeedToFile( feed, feedFile );
                    }
                } catch( Exception fe ) {
                    logger.log( Level.SEVERE, "Feed error", fe );
                    localListener.getLogger().println( "ConfigRotator RunListener caught excetption. Trace written to log.");
                }
            }
        }
    }


    /**
     * Write the feed to the file componentFileName overwriting existing xml
     * file with atoms feed. If entries have unique id that are not changes,
     * subscribers will see only new ones.
     *
     * @param feed feed
     * @throws IOException when an error occurs (General)
     * @throws FeedException when error in feeed
     */
    private void writeFeedToFile( Feed feed, File feedFile ) throws FeedException {
        Writer writer = null;
        try {
            if( !feedFile.exists() ) {
                if( !feedFile.getParentFile().exists() ) {
                    // create file including dirs
                    if( !feedFile.getParentFile().mkdirs() ) {
                        throw new IOException( "ConfigRotator RunListener - writeFeedToFile: failed to make dirs" );
                    }
                }
            }

            writer = new FileWriter( feedFile );
            writer.write( feed.getXML( new AtomPublisher() ) );
            writer.close();
        } catch( IOException ex ) {
            if( writer != null ) {
                try {
                    localListener.getLogger().println( "ConfigRotator RunListener - writeFeedToFile: write failed caught IOException meaning feed may not have been written " + " Exception was: " + ex.getMessage() );
                    writer.close();
                } catch( IOException ex1 ) {
                    localListener.getLogger().println( "ConfigRotator RunListener - writeFeedToFile: write.close failed too caught IOException meaning feed may not have been written " + " Exception was: " + ex1.getMessage() );
                }
            } else {
                localListener.getLogger().println( "ConfigRotator RunListener - writeFeedToFile: writer  WAS NULL, caught IOException meaning feed may not have been written " + " Exception was: " + ex.getMessage() );
            }
        }
    }
}