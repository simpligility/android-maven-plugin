package com.jayway.maven.plugins.android.standalonemojos;

import com.jayway.maven.plugins.android.AbstractZipalignMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * ZipalignMojo can run the zipalign command against the apk.
 *
 * @author Manfred Moser <manfred@simpligility.com>
 *
 * @goal zipalign
 * @requiresProject false
 */
public class ZipalignMojo extends AbstractZipalignMojo {

    public void execute() throws MojoExecutionException, MojoFailureException {
        zipalign();
    }


}
