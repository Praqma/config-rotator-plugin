package net.praqma.jenkins.configrotator.scm;

import hudson.model.User;
import hudson.scm.ChangeLogSet.Entry;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author Praqma
 */
public class ConfigRotatorChangeLogEntry extends Entry implements Serializable {

    protected ConfigRotatorChangeLogSet parent;
    private String commitMessage;
    private String user;
    private ArrayList<ConfigRotatorVersion> versions;

    /**
     * Default constructor
     */
    public ConfigRotatorChangeLogEntry() {
        versions = new ArrayList<ConfigRotatorVersion>();
    }

    public ConfigRotatorChangeLogEntry(String commitMessage, String user, ArrayList<ConfigRotatorVersion> versions) {
        this.commitMessage = commitMessage;
        this.user = user;
        this.versions = versions;
    }

    @Override
    public String getMsg() {
        return commitMessage;
    }

    @Override
    public Collection<String> getAffectedPaths() {
        ArrayList<String> strings = new ArrayList<String>();
        for (ConfigRotatorVersion ccv : getVersions()) {
            strings.add(ccv.getFile());
        }
        return strings;
    }

    /**
     * @return the activityName
     */
    public String getCommitMessage() {
        return commitMessage;
    }

    public void setCommitMessage(String commitMessage) {
        this.commitMessage = commitMessage;
    }

    public ArrayList<ConfigRotatorVersion> getVersions() {
        return versions;
    }

    public void setVersions(ArrayList<ConfigRotatorVersion> versions) {
        this.versions = versions;
    }

    public void addVersion(ConfigRotatorVersion version) {
        versions.add(version);
    }

    public String getUser() {
        return user;
    }

    @Override
    public User getAuthor() {
        if (user == null) {
            return User.getUnknown();
        }
        User u = User.get(user);
        return u;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public <T extends ConfigRotatorChangeLogSet> void setParent(T t) {
        this.parent = t;
    }

    public <T extends ConfigRotatorChangeLogSet> T getParent(Class<T> type) {
        return (T) parent;
    }

    public String toString() {
        return user + " - " + commitMessage;
    }
}
