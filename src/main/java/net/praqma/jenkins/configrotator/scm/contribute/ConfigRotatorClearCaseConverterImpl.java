/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.praqma.jenkins.configrotator.scm.contribute;

import hudson.model.Result;
import java.util.Calendar;
import java.util.List;
import net.praqma.jenkins.configrotator.AbstractConfigurationComponent;
import net.praqma.jenkins.configrotator.ConfigurationRotatorBuildAction;
import net.praqma.jenkins.configrotator.scm.clearcaseucm.ClearCaseUCMConfiguration;
import net.praqma.jenkins.configrotator.scm.clearcaseucm.ClearCaseUCMConfigurationComponent;
import net.praqma.jenkins.configrotator.scm.contribute.dto.ClearCaseUCMConfigurationComponentDTO;
import net.praqma.jenkins.configrotator.scm.contribute.dto.ClearCaseUCMConfigurationDTO;

/**
 *
 * @author Mads
 */
public class ConfigRotatorClearCaseConverterImpl implements ConfigRotatorCompatabilityConverter<ConfigurationRotatorBuildAction> {
    
    public ConfigRotatorClearCaseConverterImpl() { } 
    
    @Override
    public CompatabilityCompatible convert(ConfigurationRotatorBuildAction t) {
        ClearCaseUCMConfiguration ccucmcomp = t.getConfiguration();
        ClearCaseUCMConfigurationDTO config = ClearCaseUCMConfigurationDTO.fromConfiguration(ccucmcomp);         
        boolean success = t.getBuild().getResult().isBetterOrEqualTo(Result.SUCCESS);
        
        ClearCaseUCMConfigurationDTO configComponent = new ClearCaseUCMConfigurationDTO();
        
        //This is a new configuration. We need to add all components as changed
        if(ccucmcomp.getChangedComponents().isEmpty()) {
            configComponent.addAll(config);
        } else {
            //Changed components
            List<AbstractConfigurationComponent> compp = ccucmcomp.getChangedComponents();
            for(AbstractConfigurationComponent abcomp : compp ) {
                configComponent.add(ClearCaseUCMConfigurationComponentDTO.fromComponent((ClearCaseUCMConfigurationComponent)abcomp));
            }
        }
        
        ClearcaseUCMCompatability comp = new ClearcaseUCMCompatability(configComponent, Calendar.getInstance().getTime(), t.getBuild().getProject().getName(), success, config);         
        return comp;
    }     
}
