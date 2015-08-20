package net.praqma.jenkins.configrotator.scm.clearcaseucm;

import hudson.FilePath.FileCallable;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.praqma.clearcase.ConfigSpec;
import net.praqma.clearcase.Rebase;
import net.praqma.clearcase.exceptions.ClearCaseException;
import net.praqma.clearcase.exceptions.RebaseException;
import net.praqma.clearcase.exceptions.UnableToInitializeEntityException;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Project;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.ucm.view.GetView;
import net.praqma.clearcase.ucm.view.SnapshotView;
import net.praqma.clearcase.ucm.view.UpdateView;
import net.praqma.jenkins.configrotator.ConfigurationRotator;

public class PrepareWorkspace implements FileCallable<SnapshotView> {

    private final Project project;
    private static final Logger log = Logger.getLogger(PrepareWorkspace.class.getName());
    private final TaskListener listener;
    private final String jenkinsProjectName;
    private final List<Baseline> baselines;

    public PrepareWorkspace(Project project, List<Baseline> baselines, String jenkinsProjectName, TaskListener listener) {
        this.project = project;
        this.jenkinsProjectName = jenkinsProjectName;
        this.listener = listener;
        this.baselines = baselines;
    }

    @Override
    public SnapshotView invoke(File workspace, VirtualChannel channel) throws IOException, InterruptedException {
        //Viewtag now becomes the jenkinsProjectName + remote computer name
        String viewtag = jenkinsProjectName + "-" + System.getenv("COMPUTERNAME");
        PrintStream out = listener.getLogger();

        out.println(String.format("%sResulting viewtag is: %s", ConfigurationRotator.LOGGERNAME, viewtag));

        SnapshotView view = null;
        File viewroot = new File(workspace, "view");

        /* Changle stream, if exists */
        String streamName = viewtag + "@" + project.getPVob();
        Stream devStream;

        try {
            devStream = Stream.get(streamName);
        } catch (UnableToInitializeEntityException e) {
            throw new IOException("No entity", e);
        }

        /* If the stream exists, change it */
        if (devStream.exists()) {
            out.println(ConfigurationRotator.LOGGERNAME + "Stream exists");

            try {
                /**
                 * FogBugz 11220, when we get the view, we must make sure that the view is present. 
                 */
                view = new GetView(viewroot, viewtag).createIfAbsent().setStream(devStream).get();                
            } catch (ClearCaseException e) {
                throw new IOException("Could not get view", e);
            }
            try {
                Rebase rb = new Rebase(devStream);                
                out.println(ConfigurationRotator.LOGGERNAME + "Rebasing stream to " + devStream.getNormalizedName());
                rb.setViewTag(viewtag).addBaselines(baselines).dropFromStream().rebase(true, true);                
            } catch (RebaseException e) {
                throw new IOException(e.getMessage(), e);
            } catch (ClearCaseException reb) {
                throw new IOException("Could not load " + devStream, reb);
            } 

            /* The view */
            try {
                out.println(ConfigurationRotator.LOGGERNAME + "View root: " + new File(workspace, "view"));
                out.println(ConfigurationRotator.LOGGERNAME + "View tag : " + viewtag);
                new ConfigSpec(viewroot).addLoadRule(baselines).generate().appy();
                new UpdateView(view).setLoadRules(new SnapshotView.LoadRules2(SnapshotView.Components.ALL)).swipe().overwrite().update();
            } catch (ClearCaseException e) {
                throw new IOException("Unable to create view", e);
            }

        } else {
            out.println(ConfigurationRotator.LOGGERNAME + "Creating a new environment");
            try {
                out.println(ConfigurationRotator.LOGGERNAME + "Creating new stream");
                devStream = Stream.create(project.getIntegrationStream(), streamName, true, baselines);
            } catch (ClearCaseException e1) {
                throw new IOException("Unable to create stream " + streamName, e1);
            }
            try {
                view = new GetView(viewroot, viewtag).setStream(devStream).createIfAbsent().get();
                new UpdateView(view).setLoadRules(new SnapshotView.LoadRules2(SnapshotView.Components.ALL)).generate().update();
            } catch (ClearCaseException e) {
                log.log(Level.WARNING, "Failed to update view, exception to follow", e);
                throw new IOException("Unable to create view", e);
            }
        }

        return view;
    }
}
