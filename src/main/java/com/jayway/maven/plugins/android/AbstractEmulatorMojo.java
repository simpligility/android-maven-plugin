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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * AbstractEmulatorMojo contains all code related to the interaction with the Android emulator. At this stage that is
 * starting and stopping the emulator.
 *
 * @author Manfred Moser <manfred@simplgility.com>
 * @see com.jayway.maven.plugins.android.Emulator
 * @see com.jayway.maven.plugins.android.standalonemojos.EmulatorStartMojo
 * @see com.jayway.maven.plugins.android.standalonemojos.EmulatorStopMojo
 */
public abstract class AbstractEmulatorMojo extends AbstractAndroidMojo {

    /** operating system name. */
    public static final String OS_NAME = System.getProperty("os.name").toLowerCase(Locale.US);

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

    /** parsed value for avd that will be used for the invocation. */
    private String parsedAvd;
    /** parsed value for options that will be used for the invocation. */
    private String parsedOptions;
    /** parsed value for wait that will be used for the invocation. */
    private String parsedWait;

    private static final String STOP_EMULATOR_MSG = "Stopping android emulator with pid: ";
    private static final String START_EMULATOR_MSG = "Starting android emulator with script: ";
    private static final String START_EMULATOR_WAIT_MSG = "Waiting for emulator start:";
    private static final String NO_EMULATOR_RUNNING = "unknown";
    private static final String NO_DEMON_RUNNING_MACOSX = "* daemon not running";

    /** Folder that contains the startup script and the pid file. */
    private static final String scriptFolder = System.getProperty("java.io.tmpdir");
    /** file name for the pid file. */
    private static final String pidFileName = scriptFolder + "/maven-android-plugin-emulator.pid";

    /**
     * Are we running on a flavour of Windows.
     * @return
     */
    private boolean isWindows() {
        boolean result;
        if (OS_NAME.toLowerCase().contains("windows")) {
            result =  true;
        }
        else
        {
            result = false;
        }
        getLog().debug("isWindows: " + result);
        return result;
    }

    /**
     * Start the Android Emulator with the specified options.
     * @see #emulatorAvd
     * @see #emulatorWait
     * @see #emulatorOptions
     * @throws org.apache.maven.plugin.MojoExecutionException
     */
    protected void startAndroidEmulator() throws MojoExecutionException
    {
        parseParameters();

        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger(this.getLog());

        try {
            String filename;
            if (isWindows())
            {
               filename = writeEmulatorStartScriptWindows();
            }
            else
            {
                filename = writeEmulatorStartScriptUnix();
            }

            String emulatorName = getRunningEmulatorName();
            // normally only #NO_EMULATOR_RUNNING  is returned
            // however when starting emulator within intellij on macosx with launchd configuration the first time
            // #NO_DEMON_RUNNING_MACOSX is the start of the first line but needs to be treated the same
            if (emulatorName.equals(NO_EMULATOR_RUNNING) || emulatorName.startsWith(NO_DEMON_RUNNING_MACOSX))
            {
                getLog().info(START_EMULATOR_MSG + filename);
                executor.executeCommand(filename, null);

                getLog().info(START_EMULATOR_WAIT_MSG + parsedWait);
                // wait for the emulator to start up
                Thread.sleep(new Long(parsedWait));
            }
            else
            {
                getLog().info("Emulator " + emulatorName + " already running. Skipping start and wait.");
            }

        } catch (Exception e) {
            throw new MojoExecutionException("", e);
        }
    }

    /**
     * Get the name of the running emulator.
     * @return emulator name or "unknown" if none found running with adb tool.
     * @throws MojoExecutionException
     * @throws ExecutionException
     * @see #NO_EMULATOR_RUNNING
     */
    private String getRunningEmulatorName() throws MojoExecutionException, ExecutionException {
        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger(this.getLog());
        List<String> commands = new ArrayList<String>();
        commands.add("get-serialno");
        executor.executeCommand(getAndroidSdk().getAdbPath(), commands);
        return executor.getStandardOut();  
    }

    /**
     * Writes the script to start the emulator in the background for windows based environments. This is not fully
     * operational. Need to implement pid file write.
     * @return absolute path name of start script
     * @throws IOException
     * @throws MojoExecutionException
     * @see "http://stackoverflow.com/questions/2328776/how-do-i-write-a-pidfile-in-a-windows-batch-file"
     */
    private String writeEmulatorStartScriptWindows() throws IOException, MojoExecutionException {

        String filename = scriptFolder + "\\maven-android-plugin-emulator-start.bat";

        File file = new File(filename);
        PrintWriter writer = new PrintWriter(new FileWriter(file));
        // command needs to be assembled before unique window title since it parses settings and sets up parsedAvd
        // and others.
        String command = assembleStartCommandLine();
        String uniqueWindowTitle = "MavenAndroidPlugin-AVD" + parsedAvd;
        writer.print("START " + uniqueWindowTitle + " " + command);
        writer.println();
        writer.println("FOR /F \"tokens=2\" %%I in ('TASKLIST /NH /FI \"WINDOWTITLE eq " + uniqueWindowTitle + "\"' ) DO SET PID=%%I");
        writer.println("ECHO %PID% > " + pidFileName);
        writer.flush();
        writer.close();
        file.setExecutable(true);
        return filename;
    }

    /**
     * Writes the script to start the emulator in the background for unix based environments and write process id into
     * pidfile.
     * @return absolute path name of start script
     * @throws IOException
     * @throws MojoExecutionException
     */
    private String writeEmulatorStartScriptUnix() throws IOException, MojoExecutionException {
        String filename = scriptFolder + "/maven-android-plugin-emulator-start.sh";

        File sh;
        sh = new File("/bin/bash");
        if (!sh.exists()) {
            sh = new File("/usr/bin/bash");
        }
        if (!sh.exists()) {
            sh = new File("/bin/sh");
        }

        File file = new File(filename);
        PrintWriter writer = new PrintWriter(new FileWriter(file));
        writer.println("#!" + sh.getAbsolutePath());
        writer.print(assembleStartCommandLine());
        writer.print(" 1>/dev/null 2>&1 &"); // redirect outputs and run as background task
        writer.println();
        writer.println("echo $! > " + pidFileName); // process id from stdout into pid file
        writer.flush();
        writer.close();
        file.setExecutable(true);
        return filename;
    }

    /**
     * Stop the running Android Emulator.
     * @throws org.apache.maven.plugin.MojoExecutionException
     */
    protected void stopAndroidEmulator() throws MojoExecutionException {
        parseParameters();

        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger(this.getLog());

        try {
            FileReader fileReader = new FileReader(pidFileName);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String pid;
            pid = bufferedReader.readLine(); // just read the first line, don't worry about anything else
            bufferedReader.close();

            if (isWindows())
            {
                stopEmulatorWindows(executor, pid);
            }
            else
            {
                stopEmulatorUnix(executor, pid);
            }
        } catch (Exception e) {
            throw new MojoExecutionException("", e);
        }
    }

    /**
     * Stop the emulator by using the taskkill command.
     * @param executor
     * @param pid
     * @throws ExecutionException
     */
    private void stopEmulatorWindows(CommandExecutor executor, String pid) throws ExecutionException {
        String stopCommand = "TASKKILL"; // this assumes that the command is on the path
        List<String> commands = new ArrayList<String>();
        commands.add("/PID");
        commands.add(pid);
        getLog().info(STOP_EMULATOR_MSG + pid);
        executor.executeCommand(stopCommand, commands);
    }

    /**
     * Stop the emulator under Unix by using the kill command.
     * @param executor
     * @param pid
     * @throws ExecutionException
     */
    private void stopEmulatorUnix(CommandExecutor executor, String pid) throws ExecutionException {
        String stopCommand = "kill";
        List<String> commands = new ArrayList<String>();
        commands.add(pid);
        getLog().info(STOP_EMULATOR_MSG + pid);
        executor.executeCommand(stopCommand, commands);
    }

    /**
     * Assemble the command line for starting the emulator based on the parameters supplied in the pom file and on the
     * command line. It should not be that painful to do work with command line and pom supplied values but evidently
     * it is.
     * @return
     * @throws MojoExecutionException
     * @see com.jayway.maven.plugins.android.Emulator
     */
    private String assembleStartCommandLine() throws MojoExecutionException {
        StringBuilder startCommandline = new StringBuilder()
                .append(getAndroidSdk().getEmulatorPath())
                .append(" -avd ")
                .append(parsedAvd)
                .append(" ");
        if (!StringUtils.isEmpty(parsedOptions))
        {
            startCommandline.append(parsedOptions);
        }
        getLog().info("Android emulator command: " + startCommandline);
        return startCommandline.toString();
    }

    private void parseParameters() {
        // <emulator> exist in pom file
        if (emulator != null)
        {
            // <emulator><avd> exists in pom file
            if (emulator.getAvd() != null)
            {
                parsedAvd = emulator.getAvd();
            }
            else
            {
                parsedAvd = determineAvd();
            }
            // <emulator><options> exists in pom file
            if (emulator.getOptions() != null)
            {
                parsedOptions = emulator.getOptions();
            }
            else
            {
                parsedOptions = determineOptions();
            }
            // <emulator><wait> exists in pom file
            if (emulator.getWait() != null)
            {
                parsedWait = emulator.getWait();
            }
            else
            {
                parsedWait = determineWait();
            }
        }
        // commandline options
        else
        {
            parsedAvd = determineAvd();
            parsedOptions = determineOptions();
            parsedWait = determineWait();
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
}