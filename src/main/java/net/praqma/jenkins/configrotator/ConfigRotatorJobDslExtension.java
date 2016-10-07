package net.praqma.jenkins.configrotator;

import hudson.Extension;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import static javaposse.jobdsl.dsl.Preconditions.checkArgument;
import javaposse.jobdsl.dsl.RequiresPlugin;
import javaposse.jobdsl.dsl.helpers.ScmContext;
import javaposse.jobdsl.dsl.helpers.publisher.PublisherContext;
import javaposse.jobdsl.plugin.ContextExtensionPoint;
import javaposse.jobdsl.plugin.DslExtensionMethod;
import net.praqma.jenkins.configrotator.scm.clearcaseucm.ClearCaseUCM;
import net.praqma.jenkins.configrotator.scm.git.Git;
import net.praqma.jenkins.configrotator.scm.git.GitTarget;

/*

scm can be: 'git' or 'clearCase'.
Configuration differs depending on choice.

GIT
```
job{
    scm{
        configRotator('git'){
            target{
                name (String name)
                repository (String repo)
                branch (String branch)
                commit (String commit)
                fixed (boolean fixed = true)    // Defaults to false
            }
        }
    }

    publishers{
        configRotatorPublisher()
    }
}
```
For example:
```
job('letters_GEN'){
    scm{
        configRotator('git'){
            target{
                name    'capital'
                repository  'https://github.com/praqma-test/capital-letters.git'
                branch  'master'
                commit  '522e47d6bc88948d0182902badde8c9a0eb14a82'
                fixed   false
            }
            target{
                name    'lower'
                repository  'https://github.com/praqma-test/lower-letters.git'
                branch  'master'
                commit  '85c76dd6a4037085a5b0f6c986e392d4386395cd'
                fixed   false
            }
        }
    }

    publishers{
        configRotatorPublisher()
    }
}
```

CLEARCASE
```
job{
    scm{
        configRotator('clearCase'){
            projectVob (String pvob)
            useNewest (boolean useNewest = true)                // Defaults to false
            globalData (boolean contributeDataGlobally = true)  // Defaults to false
            target{
                baseline (String baseline)
                promotionLevel (String promotionLevel)          // Can be: INITIAL, BUILT, TESTED, RELEASED or REJECTED. Defaults to INITIAL.
                fixed (boolean fixed = true)                    // Defaults to false
            }
        }
    }

    publishers{
        configRotatorPublisher()
    }
}
```
For example:
```
job('crot_GEN'){
    scm{
        configRotator('clearCase'){
            projectVob '\\crot_PVOB'
            useNewest()
            globalData()
            target{
                baseline 'client-1@\\crot_PVOB'
                promotionLevel 'INITIAL'
                fixed false
            }
            target{
                baseline 'model-1@\\crot_PVOB'
                promotionLevel 'INITIAL'
                fixed false
            }
        }
    }

    publishers{
        configRotatorPublisher()
    }
}
```
*/

@Extension(optional = true)
public class ConfigRotatorJobDslExtension extends ContextExtensionPoint {

    private final Set<String> SCM = new HashSet<String>() {
        {
            add("git");
            add("clearCase");
        }
    };

    @RequiresPlugin(id = "config-rotator", minimumVersion = "1.2.2")
    @DslExtensionMethod(context = PublisherContext.class)
    public Object configRotatorPublisher() {
        return new ConfigurationRotatorPublisher();
    }

    @RequiresPlugin(id = "config-rotator", minimumVersion = "1.2.2")
    @DslExtensionMethod(context = ScmContext.class)
    public Object configRotator(String scm, Runnable closure) {
        checkArgument(SCM.contains(scm), "scm must be one of: " + SCM.toString());

        AbstractConfigurationRotatorSCM acrs = null;

        if (scm.equals("git")) {
            GitJobDslContext context = new GitJobDslContext();
            executeInContext(closure, context);
            Git git = new Git(new ArrayList<GitTarget>());
            git.setTargets(context.targets);
            git.setUseNewest(false);
            acrs = git;
        } else if (scm.equals("clearCase")) {
            ClearCaseJobDslContext context = new ClearCaseJobDslContext();
            executeInContext(closure, context);
            ClearCaseUCM clearCase = new ClearCaseUCM(context.projectVob);
            clearCase.setTargets(context.targets);
            clearCase.setContribute(context.globalData);
            clearCase.setUseNewest(context.useNewest);
            acrs = clearCase;
        }

        return new ConfigurationRotator(acrs);
    }
}
