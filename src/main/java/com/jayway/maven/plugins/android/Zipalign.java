package com.jayway.maven.plugins.android;

/**
 * Configuration for the zipalign command. This class is only the definition of the parameters that are shadowed in
 * {@link com.jayway.maven.plugins.android.AbstractZipalignMojo} and used there.
 *
 * @author Manfred Moser <manfred@simnpligility.com> 
 */
public class Zipalign {

    /**
     * Skip the zipalign command if desired. Similar to test.skip for surefire plugin.
     * @parameter
     * @default=false
     */
    private Boolean skip;

    /**
     * Activate verbose output of the zipalign command.
     * @parameter
     * @default=true
     */
    private Boolean verbose;


    /**
     * The apk file to be zipaligned. Per default the file is taken from build directory (target normally) using the
     * build final name as file name and apk as extension.
     * @parameter
     */
    private String inputApk;

    /**
     * The apk file produced by the zipalign process. Per default the file is placed into the build directory (target
     * normally) using the build final name appended with "-aligned" as file name and apk as extension. 
     * @parameter
     */
    private String outputApk;


    public Boolean isSkip() {
        return skip;
    }

    public Boolean isVerbose() {
        return verbose;
    }

    public String getInputApk() {
        return inputApk;
    }

    public String getOutputApk() {
        return outputApk;
    }
}
