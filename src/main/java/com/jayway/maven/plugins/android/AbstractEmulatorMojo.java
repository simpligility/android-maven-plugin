/*
 * Copyright (C) 2009, 2010 Jayway AB
 * Copyright (C) 2007-2008 JVending Masa
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jayway.maven.plugins.android;

import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.JXPathNotFoundException;
import org.apache.commons.jxpath.xml.DocumentContainer;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.dependency.utils.resolvers.ArtifactsResolver;
import org.apache.maven.plugin.dependency.utils.resolvers.DefaultArtifactsResolver;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.util.DirectoryScanner;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * Contains common fields and methods for android mojos.
 *
 * @author hugo.josefson@jayway.com
 */
public abstract class AbstractEmulatorMojo extends AbstractAndroidMojo {

    /**
     * The Android emulator configuration to use. All values are optional.
     * &lt;emulator&gt;
     *     &lt;avd&gt;Default&lt;/avd&gt;
     *     &lt;wait&gt;20000&lt;/wait&gt;
     *     &lt;options&gt;-no-skin&lt;/options&gt;
     * &lt;/emulator&gt;
     * </pre>
     * @parameter
     */
    private Emulator emulator;

    /**
     * Name of the Android Virtual Device (emulatorAvd) that will be started by the emulator. Default value is "Default"
     * @see Emulator#avd
     * @parameter expression="${android.emulator.avd}"
     * @readonly
     */
    private String emulatorAvd;


    /**
     * Wait time for the emulator start up.
     * @see Emulator#wait
     * @parameter expression="${android.emulator.wait}"
     * @readonly
     */
    private String emulatorWait;

    /**
     * Additional command line options for the emulator start up. This option can be used to pass any additional
     * options desired to the invocation of the emulator. Use emulator -help for more details. An example would be
     * "-no-skin".
     * @see Emulator#options
     * @parameter expression="${android.emulator.options}"
     * @readonly
     */
    private String emulatorOptions;

    /**
     * Start the Android Emulator with the specified options.
     * @see #emulatorAvd
     * @see #emulatorWait
     * @see #emulatorOptions
     * @throws org.apache.maven.plugin.MojoExecutionException
     */
    protected void startAndroidEmulator() throws MojoExecutionException
    {
        String avd;
        String wait;
        String options;
        // <emulator> exist in pom file
        if (emulator != null)
        {
            // <emulator><avd> exists in pom file
            if (emulator.getAvd() != null)
            {
                avd = emulator.getAvd();
            }
            else
            {
                avd = determineAvd();
            }
            // <emulator><options> exists in pom file
            if (emulator.getOptions() != null)
            {
                options = emulator.getOptions();
            }
            else
            {
                options = determineOptions();
            }
            // <emulator><wait> exists in pom file
            if (emulator.getWait() != null)
            {
                wait = emulator.getWait();
            }
            else
            {
                wait = determineWait();
            }
        }
        // commandline options
        else
        {
            avd = determineAvd();
            options = determineOptions();
            wait = determineWait();
        }

        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger(this.getLog());

        List<String> commands = new ArrayList<String>();
        commands.add("-avd");
        commands.add(avd);
        if (!StringUtils.isEmpty(options))
        {
            commands.add(options);
        }

        getLog().info(getAndroidSdk().getPathForTool("emulator")+" " + commands.toString());

        try {
            executor.executeCommand(getAndroidSdk().getPathForTool("emulator"), commands);
        } catch (ExecutionException e) {
            throw new MojoExecutionException("", e);
        }

        // wait for the emulator to start up
        try {
            getLog().info("Waiting for emulator start:" + wait);
            Thread.sleep(new Long(wait));
        } catch (InterruptedException e) {
             //swallow
        }
    }

    /**
     * Get wait value for emulator from command line option.
     * @return if available return command line value otherwise return default value (5000).
     */
    private String determineWait() {
        String wait;
        if (emulatorWait != null)
        {
            wait = emulatorWait;
        }
        else
        {
            wait = "5000";
        }

        return wait;
    }

    /**
     * Get options value for emulator from command line option.
     * @return if available return command line value otherwise return default value ("").
     */
    private String determineOptions() {
        String options;
        if (emulatorOptions != null)
        {
            options = emulatorOptions;
        }
        else
        {
            options = "";
        }
        return options;
    }

    /**
     * Get avd value for emulator from command line option.
     * @return if available return command line value otherwise return default value ("Default").
     */
    private String determineAvd() {
        String avd;
        if (emulatorAvd != null)
        {
            avd = emulatorAvd;
        }
        else
        {
            avd = "Default";
        }
        return avd;
    }

    /**
     * Stop the running Android Emulator.
     * @throws org.apache.maven.plugin.MojoExecutionException
     * @todo not yet implemented, either we find the process id and use PlexusUtils or whatever to kill the emulator
     * or we find you about the tcp ip connection to the emulator that lets us send a command and then send a shutdown
     * command or something like that
     */
    protected void stopAndroidEmulator() throws MojoExecutionException {


        throw new MojoExecutionException("Not yet implemented..");
    }


}