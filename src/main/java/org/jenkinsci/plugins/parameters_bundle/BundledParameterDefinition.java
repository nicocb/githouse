package org.jenkinsci.plugins.parameters_bundle;

import hudson.Extension;
import hudson.model.*;
import hudson.util.FormValidation;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by nicocb on 12/29/13.
 */
public class BundledParameterDefinition extends ChoiceParameterDefinition {

    private static final Logger LOGGER = Logger.getLogger(BundledParameterDefinition.class.getName());

    private final String bundle;

    @Extension
    public static class DescriptorImpl extends ParameterDescriptor {
        @Override
        public String getDisplayName() {
            return "Bundled Parameter";
        }


        public FormValidation doCheckBundle(@QueryParameter String value, @AncestorInPath AbstractProject project) {
            if(value == null || value.length()==0) {
                return FormValidation.warning("Won't work without a bundle");
            } else {
                ParametersDefinitionProperty params = (ParametersDefinitionProperty) project.getProperty(ParametersDefinitionProperty.class);
                if(!params.getParameterDefinitionNames().contains(value)) {
                    return FormValidation.error("Couldn't find bundle parameter "+value);
                }
                if(params.getParameterDefinition(value) instanceof BundledParameterDefinition) {
                    return FormValidation.error("The bundle parameter can't be a bundled parameter");
                }
            }
            return FormValidation.ok();
        }
    }

    @DataBoundConstructor
    public BundledParameterDefinition(String name, String choices, String description, String bundle) {
        super(name, choices, description);
        this.bundle = bundle;
    }

    @Override
    public ParameterValue createValue(StaplerRequest staplerRequest, JSONObject jsonObject) {
        //Check if the value is overridden
        String value = (String) jsonObject.get("value");
        if(value ==null || value.length()==0) {
            //Else get bundle value
            String bundleValue = null;
            try {
                bundleValue = getBundleValue(staplerRequest.getSubmittedForm().getJSONArray("parameter"), (String) jsonObject.get("bundle"));
            } catch (ServletException e) {
                LOGGER.log(Level.SEVERE, "Error parsing JSON object",e);
            }
            if( bundleValue!= null){
                value = getChoiceValue(getChoices(), bundleValue);
            }
        }

        return new StringParameterValue(this.getName(),value);

    }

    @Override
    public StringParameterValue createValue(String value) {
        return super.createValue(value);
    }



    private String getBundleValue(JSONArray parameters, String bundle) {
        String val = null;
        for (Object parameter : parameters) {
            if(parameter instanceof JSONObject) {
                if(((JSONObject)parameter).containsKey("name") && ((JSONObject)parameter).containsKey("value") && ((JSONObject)parameter).getString("name").equals(bundle)) {
                    val = ((JSONObject)parameter).getString("value");
                    break;
                }
            }
        }
        LOGGER.log(Level.SEVERE, "Bundle parameter " + bundle + " not found");
        return val;
    }

    private String getChoiceValue(List<String> choices, String bundleValue) {
        for (String choice : choices) {
            if(choice.startsWith(bundleValue+":")) {
                return choice.split(":")[1];
            }
        }
        LOGGER.log(Level.SEVERE, "No value found for bundle value " + bundleValue);
        return null;
    }

    public String getBundle() {
        return bundle;
    }

    @Override
    public String getFormattedDescription(){
        return super.getFormattedDescription()+"\nValues : \n"+getChoicesText();
    }

}
