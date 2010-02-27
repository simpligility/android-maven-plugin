package com.jayway.maven.plugins.android;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for the jarsigner command. This class is only the definition of the parameters that are shadowed in
 * {@link com.jayway.maven.plugins.android.AbstractJarsignerMojo} and used there.
 *
 * @author Manfred Moser
 */
public abstract class AbstractJarsignerMojo extends AbstractMojo {
    
    private Jarsigner jarsigner;

   /**
     * @see Jarsigner#enabled
     * @parameter expression="${android.jarsigner.enabled}"
     * @readonly
     */
    private Boolean jarsignerEnabled;

    /**
     * @see Jarsigner#verbose
     * @parameter expression="${android.jarsigner.verbose}"
     * @readonly
     */
    private Boolean jarsignerVerbose;

    /**
     * @see Jarsigner#inputApk
     * @parameter expression="${android.jarsigner.inputapk}"
     * @readonly
     */
    private String jarsignerInputApk;

    /**
     * @see Jarsigner#keystore
     * @parameter expression="${android.jarsigner.keystore}"
     * @readonly
     */
    private String jarsignerKeystore;

    /**
     * @see Jarsigner#storepass
     * @parameter expression="${android.jarsigner.storepass}"
     * @readonly
     */
    private String jarsignerStorepass;

    /**
     * @see Jarsigner#keypass
     * @parameter expression="${android.jarsigner.keypass}"
     * @readonly
     */
    private String jarsignerKeypass;

    private Boolean parsedEnabled;
    private Boolean parsedVerbose;
    private String parsedInputApk;
    private String parsedKeystore;
    private String parsedStorepass;
    private String parsedKeypass;

    
    protected void signJar() throws MojoExecutionException {
        parseParameters();
        if (parsedEnabled)
        {
            CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
            executor.setLogger(this.getLog());

            String command = "jarsigner";

            List<String> parameters = new ArrayList<String>();
            if (parsedVerbose)
            {
                parameters.add("-verbose");
            };
            if (!StringUtils.isBlank(parsedKeystore))
            {
                parameters.add("-keystore");
                // is this right or should the parameter have .keystore as part of it
                parameters.add(parsedKeystore+".keystore");
            }
            if (!StringUtils.isBlank(parsedStorepass))
            {
                parameters.add("-storepass");
                parameters.add(parsedStorepass);
            }
            if (!StringUtils.isBlank(parsedKeypass))
            {
                parameters.add("-keypass");
                parameters.add(parsedKeypass);
            }
            parameters.add(parsedInputApk);

            try {
                executor.executeCommand(command, parameters);
            } catch (ExecutionException e) {
                throw new MojoExecutionException("", e);
            }
        }
        
    }
    
    private void parseParameters() {
        // <jarsigner> exist in pom file
        if (jarsigner != null)
        {
            // <jarsigner><enabled> exists in pom file
            if (jarsigner.isEnabled() != null)
            {
                parsedEnabled= jarsigner.isEnabled();
            }
            else
            {
                parsedEnabled = determineEnabled();
            }
            // <jarsigner><options> exists in pom file
            if (jarsigner.isVerbose() != null)
            {
                parsedVerbose = jarsigner.isVerbose();
            }
            else
            {
                parsedVerbose = determineVerbose();
            }
            // <jarsigner><inputApk> exists in pom file
            if (jarsigner.getInputApk() != null)
            {
                parsedInputApk = jarsigner.getInputApk();
            }
            else
            {
                parsedInputApk = determineInputApk();
            }
            // <jarsigner><keystore> exists in pom file
            if (jarsigner.getKeystore() != null)
            {
                parsedKeystore = jarsigner.getKeystore();
            }
            else
            {
                parsedKeystore = determineKeystore();
            }
            // <jarsigner><storepass> exists in pom file
            if (jarsigner.getStorepass() != null)
            {
                parsedStorepass = jarsigner.getStorepass();
            }
            else
            {
                parsedStorepass = determineStorepass();
            }
            // <jarsigner><keystore> exists in pom file
            if (jarsigner.getKeypass() != null)
            {
                parsedKeypass = jarsigner.getKeypass();
            }
            else
            {
                parsedKeypass = determineKeypass();
            }

        }
        // commandline options
        else
        {
            parsedEnabled = determineEnabled();
            parsedVerbose = determineVerbose();
            parsedInputApk = determineInputApk();
        }
    }
    
    /**
     * Get wait value for jarsigner from command line option.
     * @return if available return command line value otherwise return default value (5000).
     */
    private Boolean determineEnabled() {
        Boolean enabled;
        if (jarsignerEnabled != null)
        {
            enabled = jarsignerEnabled;
        }
        else
        {
            enabled = Boolean.FALSE;
        }
        return enabled;
    }

    private Boolean determineVerbose() {
        Boolean enabled;
        if (jarsignerEnabled != null)
        {
            enabled = jarsignerEnabled;
        }
        else
        {
            enabled = Boolean.FALSE;
        }
        return enabled;
    }

    private String determineInputApk() {
        String inputApk;
        if (jarsignerInputApk != null)
        {
            inputApk= jarsignerInputApk;
        }
        else
        {
            inputApk = ""; // what should we default to? pom.build.finalname or something?
        }
        return inputApk;
    }

    private String determineKeystore() {
        String keystore;
        if (jarsignerKeystore!= null)
        {
            keystore= jarsignerKeystore;
        }
        else
        {
            keystore = ""; // is that a fine default?
        }
        return keystore;
    }

    private String determineStorepass() {
        String storepass;
        if (jarsignerStorepass != null)
        {
            storepass= jarsignerStorepass;
        }
        else
        {
            storepass = ""; // is that a fine default?
        }
        return storepass;
    }

    private String determineKeypass() {
        String keypass;
        if (jarsignerKeypass!= null)
        {
            keypass= jarsignerKeypass;
        }
        else
        {
            keypass = ""; // is that a fine default?
        }
        return keypass;
    }

}
