/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.jenkins.configrotator;

import hudson.model.Action;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.praqma.clearcase.PVob;
import net.praqma.jenkins.configrotator.scm.clearcaseucm.ClearCaseUCM;
import net.praqma.jenkins.configrotator.scm.contribute.ClearcaseUCMCompatability;
import net.praqma.jenkins.configrotator.scm.contribute.ConfigRotatorCompatabilityConverter;
import net.praqma.jenkins.configrotator.scm.git.Git;
import net.praqma.jenkins.configrotator.scm.git.GitTarget;
import org.junit.Assert;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Test;
import org.mockito.Mockito;

/**
 *
 * @author Mads
 */
public class ConfigurationRotatorContributeTest {

    @Test
    public void assertConverters() {
        Git g = new Git(new ArrayList<GitTarget>());
        assertNull(g.getConverter());
        ClearCaseUCM ucm = new ClearCaseUCM(new PVob("Whoop@\\DeDoop"));
        assertNotNull(ucm.getConverter());
    }


    @Test
    public void assertConversionsConverts() {
        ClearCaseUCM ccucm = Mockito.mock(ClearCaseUCM.class);


        ClearcaseUCMCompatability compatability = new ClearcaseUCMCompatability();
        compatability.setCompatible(true);


        ConfigurationRotatorBuildAction com = Mockito.mock(ConfigurationRotatorBuildAction.class);

        ConfigRotatorCompatabilityConverter converter = Mockito.mock(ConfigRotatorCompatabilityConverter.class);
        Mockito.when(ccucm.getConverter()).thenReturn(converter);
        Mockito.when(converter.convert((Action) Mockito.any())).thenReturn(compatability);


        ClearcaseUCMCompatability compatability2 = (ClearcaseUCMCompatability)converter.convert(com);

        Assert.assertSame(compatability, compatability2);

    }
}
