package com.jayway.maven.plugins.android.phase00clean;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * @author Johan Lindquist <johanlindquist@gmail.com>
 * @goal clean
 * @requiresProject true
 * @requiresOnline false
 * @phase clean
 */
public class NdkCleanMojo extends AbstractMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Cleaning out native code");
    }

}
