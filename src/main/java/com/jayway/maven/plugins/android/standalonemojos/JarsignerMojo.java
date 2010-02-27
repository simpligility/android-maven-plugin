package com.jayway.maven.plugins.android.standalonemojos;

import com.jayway.maven.plugins.android.AbstractJarsignerMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * JarsignerMojo can sign the created apk.
 *
 * @author Manfred Moser <manfred@simpligility.com>
 * @goal jarsigner
 * @requiresProject false
 */
public class JarsignerMojo extends AbstractJarsignerMojo {
    public void execute() throws MojoExecutionException, MojoFailureException {
        signJar();
    }
}
