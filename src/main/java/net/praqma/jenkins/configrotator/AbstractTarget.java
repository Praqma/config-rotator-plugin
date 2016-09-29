package net.praqma.jenkins.configrotator;

import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

public abstract class AbstractTarget<T extends Describable<T>> implements Describable<T> {

}
