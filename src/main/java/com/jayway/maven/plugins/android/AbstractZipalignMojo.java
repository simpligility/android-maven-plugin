package com.jayway.maven.plugins.android;

import org.apache.maven.plugin.MojoExecutionException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: manfred
 * Date: 26-Feb-2010
 * Time: 1:44:56 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractZipalignMojo extends AbstractAndroidMojo {

    /**
     * @parameter
     */
    private Zipalign zipalign;

    /**
     * @see Zipalign#enabled
     * @parameter expression="${android.zipalign.enabled}"
     * @readonly
     */
    private Boolean zipalignEnabled;

    /**
     * @see Zipalign#verbose
     * @parameter expression="${android.zipalign.verbose}"
     * @readonly
     */
    private Boolean zipalignVerbose;

    /**
     * @see Zipalign#inputApk
     * @parameter expression="${android.zipalign.inputapk}"
     * @readonly
     */
    private String zipalignInputApk;

    /**
     * @see Zipalign#outputApk
     * @parameter expression="${android.zipalign.outputApk}"
     * @readonly
     */
    private String zipalignOutputApk;

    private Boolean parsedEnabled;
    private Boolean parsedVerbose;
    private String parsedInputApk;
    private String parsedOutputApk;    

        
    protected void zipalign() throws MojoExecutionException {
        parseParameters();
        if (parsedEnabled)
        {
            CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
            executor.setLogger(this.getLog());

            String command = getAndroidSdk().getZipalignPath();

            List<String> parameters = new ArrayList<String>();
            if (parsedVerbose)
            {
                parameters.add("-v");
            }
            parameters.add("-f"); // force overwriting existing output file
            parameters.add("4"); // byte alignment has to be 4!
            parameters.add(parsedInputApk);
            parameters.add(parsedOutputApk);

            try {
                executor.executeCommand(command, parameters);
            } catch (ExecutionException e) {
                throw new MojoExecutionException("", e);
            }
        }
    }
    
    private void parseParameters() {
        // <zipalign> exist in pom file
        if (zipalign != null)
        {
            // <zipalign><enabled> exists in pom file
            if (zipalign.isEnabled() != null)
            {
                parsedEnabled= zipalign.isEnabled();
            }
            else
            {
                parsedEnabled = determineEnabled();
            }
            // <zipalign><options> exists in pom file
            if (zipalign.isVerbose() != null)
            {
                parsedVerbose = zipalign.isVerbose();
            }
            else
            {
                parsedVerbose = determineVerbose();
            }
            // <zipalign><inputApk> exists in pom file
            if (zipalign.getInputApk() != null)
            {
                parsedInputApk = zipalign.getInputApk();
            }
            else
            {
                parsedInputApk = determineInputApk();
            }
            // <zipalign><outputApk> exists in pom file
            if (zipalign.getOutputApk() != null)
            {
                parsedOutputApk = zipalign.getOutputApk();
            }
            else
            {
                parsedOutputApk = determineOutputApk();
            }
        }
        // commandline options
        else
        {
            parsedEnabled = determineEnabled();
            parsedVerbose = determineVerbose();
            parsedInputApk = determineInputApk();
            parsedOutputApk = determineOutputApk();
        }
    }

    /**
     * Get wait value for zipalign from command line option.
     * @return if available return command line value otherwise return default value (5000).
     */
    private Boolean determineEnabled() {
        Boolean enabled;
        if (zipalignEnabled != null)
        {
            enabled = zipalignEnabled;
        }
        else
        {
            enabled = Boolean.FALSE;
        }
        return enabled;
    }

    private Boolean determineVerbose() {
        Boolean enabled;
        if (zipalignEnabled != null)
        {
            enabled = zipalignEnabled;
        }
        else
        {
            enabled = Boolean.FALSE;
        }
        return enabled;
    }

    private String determineInputApk() {
        String inputApk;
        if (zipalignInputApk != null)
        {
            inputApk= zipalignInputApk;
        }
        else
        {
            inputApk = ""; // what should we default to? pom.build.finalname or something?
        }
        return inputApk;
    }

    private String determineOutputApk() {
        String outputApk;
        if (zipalignOutputApk != null)
        {
            outputApk= zipalignOutputApk;
        }
        else
        {
            outputApk = ""; // what should we default to? pom.build.finalname or something? or some as input if possible
        }
        return outputApk;
    }

}
