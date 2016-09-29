package net.praqma.jenkins.configrotator;

import java.io.IOException;

import hudson.DescriptorExtensionList;
import hudson.model.*;
import net.praqma.jenkins.configrotator.scm.clearcaseucm.ClearCaseUCMFeedAction;
import net.praqma.jenkins.configrotator.scm.git.GitFeedAction;
import net.praqma.util.xml.feed.AtomPublisher;
import net.praqma.util.xml.feed.Feed;
import net.praqma.util.xml.feed.FeedException;

import hudson.Extension;

import java.io.*;
import java.util.*;

import jenkins.model.Jenkins;
import static net.praqma.jenkins.configrotator.ConfigurationRotatorReport.getRootUrl;

@Extension
public class ConfigurationRotatorReport extends Actionable implements UnprotectedRootAction {

    @Override
    public String getIconFileName() {
        return "/plugin/config-rotator/images/rotate.png";
    }

    @Override
    public String getDisplayName() {
        return "Config Rotator";
    }

    @Override
    public String getUrlName() {
        return "config-rotator";
    }

    @Override
    public String getSearchUrl() {
        return getUrlName();
    }

    public DescriptorExtensionList<AbstractConfigurationRotatorSCM, ConfigurationRotatorSCMDescriptor<AbstractConfigurationRotatorSCM>> getSCMs() {
        return AbstractConfigurationRotatorSCM.all();
    }

    @Override
    public synchronized List<Action> getActions() {
        List<Action> actions = new ArrayList<Action>();
        actions.add(new GitFeedAction());
        actions.add(new ClearCaseUCMFeedAction());
        return actions;
    }

    public String getUrl(ConfigurationRotatorSCMDescriptor<AbstractConfigurationRotatorSCM> scm) {
        return getRootUrl() + getUrlName() + "/" + scm.getFeedComponentName();
    }

    public static Feed getFeedFromFile(File feedFile, String name, String feedId, Date feedUpdated) throws FeedException, IOException {
        if (feedFile.exists()) {
            return Feed.getFeed(new AtomPublisher(), feedFile);
        } else {
            return new Feed(name, feedId, feedUpdated);
        }
    }

    public static String urlTtransform(String url) {
        return url.replaceAll("[^a-zA-Z0-9]", "_");
    }

    public static String FeedFrontpageUrl() {
        return getRootUrl() + ConfigurationRotator.URL_NAME + "/";
    }

    public static String GenerateJobUrl(AbstractBuild<?, ?> build) {
        return getRootUrl() + build.getUrl();
    }

    public static String getRootUrl() {
        if (Jenkins.getInstance() == null || Jenkins.getInstance().getRootUrl() == null) {
            return "http://localhost:8080/";
        } else {
            return Jenkins.getInstance().getRootUrl();
        }
    }
}
