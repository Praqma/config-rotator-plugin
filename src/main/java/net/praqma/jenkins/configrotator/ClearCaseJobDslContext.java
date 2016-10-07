package net.praqma.jenkins.configrotator;

import java.util.ArrayList;
import java.util.List;
import javaposse.jobdsl.dsl.Context;
import static javaposse.jobdsl.plugin.ContextExtensionPoint.executeInContext;
import net.praqma.clearcase.PVob;
import net.praqma.jenkins.configrotator.scm.clearcaseucm.ClearCaseUCMTarget;

class ClearCaseJobDslContext implements Context {

    PVob projectVob;

    public void projectVob(String name) {
        projectVob = new PVob(name);
    }

    boolean globalData;

    public void globalData() {
        globalData = true;
    }

    public void globalData(boolean value) {
        globalData = value;
    }

    boolean useNewest;

    public void useNewest() {
        useNewest = true;
    }

    public void useNewest(boolean value) {
        useNewest = value;
    }

    List<ClearCaseUCMTarget> targets = new ArrayList<ClearCaseUCMTarget>();

    public void target(Runnable closure) {
        ClearCaseTargetJobDslContext context = new ClearCaseTargetJobDslContext();
        executeInContext(closure, context);

        targets.add(new ClearCaseUCMTarget(context.baseline, context.promotionLevel, context.fixed));
    }
}
