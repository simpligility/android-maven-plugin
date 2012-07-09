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

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.jayway.maven.plugins.android.common.DeviceHelper;
import com.jayway.maven.plugins.android.configuration.Emulator;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


/**
 * AbstractEmulatorMojo contains all code related to the interaction with the Android emulator. At this stage that is
 * starting and stopping the emulator.
 *
 * @author Manfred Moser <manfred@simplgility.com>
 * @author Bryan O'Neil <bryan.oneil@hotmail.com>
 * @see com.jayway.maven.plugins.android.configuration.Emulator
 * @see com.jayway.maven.plugins.android.standalonemojos.EmulatorStartMojo
 * @see com.jayway.maven.plugins.android.standalonemojos.EmulatorStopMojo
 * @see com.jayway.maven.plugins.android.standalonemojos.EmulatorStopAllMojo
 */
public abstract class AbstractEmulatorMojo extends AbstractAndroidMojo
{

    /**
     * operating system name.
     */
    public static final String OS_NAME = System.getProperty("os.name").toLowerCase(Locale.US);

    /**
     * Configuration for the emulator goals. Either use the plugin configuration like this
     * <pre>
     * &lt;emulator&gt;
     *   &lt;avd&gt;Default&lt;/avd&gt;
     *   &lt;wait&gt;20000&lt;/wait&gt;
     *   &lt;options&gt;-no-skin&lt;/options&gt;
     * &lt;/emulator&gt;
     * </pre>
     * or configure as properties  on the command line as android.emulator.avd, android.emulator.wait and
     * android.emulator.options or in pom or settings file as emulator.avd, emulator.wait and emulator.options.
     *
     * @parameter
     */
    private Emulator emulator;

    /**
     * Name of the Android Virtual Device (emulatorAvd) that will be started by the emulator. Default value is "Default"
     *
     * @parameter expression="${android.emulator.avd}"
     * @see com.jayway.maven.plugins.android.configuration.Emulator#avd
     */
    private String emulatorAvd;


    /**
     * Wait time for the emulator start up.
     *
     * @parameter expression="${android.emulator.wait}"
     * @see com.jayway.maven.plugins.android.configuration.Emulator#wait
     */
    private String emulatorWait;

    /**
     * Additional command line options for the emulator start up. This option can be used to pass any additional
     * options desired to the invocation of the emulator. Use emulator -help for more details. An example would be
     * "-no-skin".
     *
     * @parameter expression="${android.emulator.options}"
     * @see com.jayway.maven.plugins.android.configuration.Emulator#options
     */
    private String emulatorOptions;

    /**
     * parsed value for avd that will be used for the invocation.
     */
    private String parsedAvd;
    /**
     * parsed value for options that will be used for the invocation.
     */
    private String parsedOptions;
    /**
     * parsed value for wait that will be used for the invocation.
     */
    private String parsedWait;

    private static final String START_EMULATOR_MSG = "Starting android emulator with script: ";
    private static final String START_EMULATOR_WAIT_MSG = "Waiting for emulator start:";

    /**
     * Folder that contains the startup script and the pid file.
     */
    private static final String scriptFolder = System.getProperty("java.io.tmpdir");

    /**
     * Are we running on a flavour of Windows.
     *
     * @return
     */
    private boolean isWindows()
    {
        boolean result;
        if ( OS_NAME.toLowerCase().contains("windows") )
        {
            result = true;
        } else
        {
            result = false;
        }
        getLog().debug("isWindows: " + result);
        return result;
    }

    /**
     * Start the Android Emulator with the specified options.
     *
     * @throws org.apache.maven.plugin.MojoExecutionException
     *
     * @see #emulatorAvd
     * @see #emulatorWait
     * @see #emulatorOptions
     */
    protected void startAndroidEmulator() throws MojoExecutionException
    {
        parseParameters();

        CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
        executor.setLogger(this.getLog());

        try
        {
            String filename;
            if ( isWindows() )
            {
                filename = writeEmulatorStartScriptWindows();
            } else
            {
                filename = writeEmulatorStartScriptUnix();
            }

            final AndroidDebugBridge androidDebugBridge = initAndroidDebugBridge();
            if ( androidDebugBridge.isConnected() )
            {
                waitForInitialDeviceList(androidDebugBridge);
                List<IDevice> devices = Arrays.asList(androidDebugBridge.getDevices());
                int numberOfDevices = devices.size();
                getLog().info("Found " + numberOfDevices + " devices connected with the Android Debug Bridge");

                IDevice existingEmulator = null;

                for ( IDevice device : devices )
                {
                    if ( device.isEmulator() )
                    {
                        if ( isExistingEmulator(device) )
                        {
                            existingEmulator = device;
                            break;
                        }
                    }
                }

                if ( existingEmulator == null )
                {
                    getLog().info(START_EMULATOR_MSG + filename);
                    executor.executeCommand(filename, null);

                    getLog().info(START_EMULATOR_WAIT_MSG + parsedWait);
                    // wait for the emulator to start up
                    Thread.sleep(new Long(parsedWait));
                } else
                {
                    getLog().info(String.format(
                            "Emulator already running [Serial No: '%s', AVD Name '%s']. " + "Skipping start and wait.",
                            existingEmulator.getSerialNumber(), existingEmulator.getAvdName()));
                }
            }
        } catch ( Exception e )
        {
            throw new MojoExecutionException("", e);
        }
    }

    /**
     * Checks whether the given device has the same AVD name as the device which the current command
     * is related to. <code>true</code> returned if the device AVD names are identical (independent of case)
     * and <code>false</code> if the device AVD names are different.
     *
     * @param device The device to check
     * @return Boolean results of the check
     */
    private boolean isExistingEmulator(IDevice device)
    {
        return (device.getAvdName().equalsIgnoreCase(parsedAvd));
    }

    /**
     * Writes the script to start the emulator in the background for windows based environments.
     *
     * @return absolute path name of start script
     * @throws IOException
     * @throws MojoExecutionException
     */
    private String writeEmulatorStartScriptWindows() throws MojoExecutionException
    {

        String filename = scriptFolder + "\\android-maven-plugin-emulator-start.vbs";

        File file = new File(filename);
        PrintWriter writer = null;
        try
        {
            writer = new PrintWriter(new FileWriter(file));


            // command needs to be assembled before unique window title since it parses settings and sets up parsedAvd
            // and others.
            String command = assembleStartCommandLine();
            String uniqueWindowTitle = "AndroidMavenPlugin-AVD" + parsedAvd;
            writer.println("Dim oShell");
            writer.println("Set oShell = WScript.CreateObject(\"WScript.shell\")");
            String cmdPath = System.getenv("COMSPEC");
            if ( cmdPath == null )
            {
                cmdPath = "cmd.exe";
            }
            String cmd = cmdPath + " /X /C START /SEPARATE \"\"" + uniqueWindowTitle + "\"\"  " + command.trim();
            writer.println("oShell.run \"" + cmd + "\"");
        } catch ( IOException e )
        {
            getLog().error("Failure writing file " + filename);
        } finally
        {
            if ( writer != null )
            {
                writer.flush();
                writer.close();
            }
        }
        file.setExecutable(true);
        return filename;
    }

    /**
     * Writes the script to start the emulator in the background for unix based environments.
     *
     * @return absolute path name of start script
     * @throws IOException
     * @throws MojoExecutionException
     */
    private String writeEmulatorStartScriptUnix() throws MojoExecutionException
    {
        String filename = scriptFolder + "/android-maven-plugin-emulator-start.sh";

        File sh;
        sh = new File("/bin/bash");
        if ( !sh.exists() )
        {
            sh = new File("/usr/bin/bash");
        }
        if ( !sh.exists() )
        {
            sh = new File("/bin/sh");
        }

        File file = new File(filename);
        PrintWriter writer = null;
        try
        {
            writer = new PrintWriter(new FileWriter(file));
            writer.println("#!" + sh.getAbsolutePath());
            writer.print(assembleStartCommandLine());
            writer.print(" 1>/dev/null 2>&1 &"); // redirect outputs and run as background task
        } catch ( IOException e )
        {
            getLog().error("Failure writing file " + filename);
        } finally
        {
            if ( writer != null )
            {
                writer.flush();
                writer.close();
            }
        }
        file.setExecutable(true);
        return filename;
    }

    /**
     * Stop the running Android Emulator.
     *
     * @throws org.apache.maven.plugin.MojoExecutionException
     *
     */
    protected void stopAndroidEmulator() throws MojoExecutionException
    {
        parseParameters();

        final AndroidDebugBridge androidDebugBridge = initAndroidDebugBridge();
        if ( androidDebugBridge.isConnected() )
        {
            List<IDevice> devices = Arrays.asList(androidDebugBridge.getDevices());
            int numberOfDevices = devices.size();
            getLog().info("Found " + numberOfDevices + " devices connected with the Android Debug Bridge");

            for ( IDevice device : devices )
            {
                if ( device.isEmulator() )
                {
                    if ( isExistingEmulator(device) )
                    {
                        stopEmulator(device);
                    }
                } else
                {
                    getLog().info("Skipping stop. Not an emulator. " + DeviceHelper.getDescriptiveName(device));
                }
            }
        }
    }

    /**
     * Stop the running Android Emulators.
     *
     * @throws org.apache.maven.plugin.MojoExecutionException
     *
     */
    protected void stopAndroidEmulators() throws MojoExecutionException
    {
        final AndroidDebugBridge androidDebugBridge = initAndroidDebugBridge();
        if ( androidDebugBridge.isConnected() )
        {
            List<IDevice> devices = Arrays.asList(androidDebugBridge.getDevices());
            int numberOfDevices = devices.size();
            getLog().info("Found " + numberOfDevices + " devices connected with the Android Debug Bridge");

            for ( IDevice device : devices )
            {
                if ( device.isEmulator() )
                {
                    stopEmulator(device);
                } else
                {
                    getLog().info("Skipping stop. Not an emulator. " + DeviceHelper.getDescriptiveName(device));
                }
            }
        }
    }

    /**
     * This method contains the code required to stop an emulator
     *
     * @param device The device to stop
     */
    private void stopEmulator(IDevice device)
    {
        int devicePort = extractPortFromDevice(device);
        if ( devicePort == -1 )
        {
            getLog().info("Unable to retrieve port to stop emulator " + DeviceHelper.getDescriptiveName(device));
        } else
        {
            getLog().info("Stopping emulator " + DeviceHelper.getDescriptiveName(device));

            sendEmulatorCommand(devicePort, "avd stop");
            boolean killed = sendEmulatorCommand(devicePort, "kill");
            if ( !killed )
            {
                getLog().info("Emulator failed to stop " + DeviceHelper.getDescriptiveName(device));
            } else
            {
                getLog().info("Emulator stopped successfully " + DeviceHelper.getDescriptiveName(device));
            }
        }
    }

    /**
     * This method extracts a port number from the serial number of a device.
     * It assumes that the device name is of format [xxxx-nnnn] where nnnn is the
     * port number.
     *
     * @param device The device to extract the port number from.
     * @return Returns the port number of the device
     */
    private int extractPortFromDevice(IDevice device)
    {
        String portStr = StringUtils.substringAfterLast(device.getSerialNumber(), "-");
        if ( StringUtils.isNotBlank(portStr) && StringUtils.isNumeric(portStr) )
        {
            return Integer.parseInt(portStr);
        }

        //If the port is not available then return -1
        return -1;
    }

    /**
     * Sends a user command to the running emulator via its telnet interface.
     *
     * @param port    The emulator's telnet port.
     * @param command The command to execute on the emulator's telnet interface.
     * @return Whether sending the command succeeded.
     */
    private boolean sendEmulatorCommand(
            //final Launcher launcher,
            //final PrintStream logger,
            final int port, final String command)
    {
        Callable<Boolean> task = new Callable<Boolean>()
        {
            public Boolean call() throws IOException
            {
                Socket socket = null;
                BufferedReader in = null;
                PrintWriter out = null;
                try
                {
                    socket = new Socket("127.0.0.1", port);
                    out = new PrintWriter(socket.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    if ( in.readLine() == null )
                    {
                        return false;
                    }

                    out.write(command);
                    out.write("\r\n");
                } finally
                {
                    try
                    {
                        out.close();
                        in.close();
                        socket.close();
                    } catch ( Exception e )
                    {
                        // Do nothing
                    }
                }

                return true;
            }

            private static final long serialVersionUID = 1L;
        };

        boolean result = false;
        try
        {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<Boolean> future = executor.submit(task);
            result = future.get();
        } catch ( Exception e )
        {
            getLog().error(String.format("Failed to execute emulator command '%s': %s", command, e));
        }

        return result;
    }

    /**
     * Assemble the command line for starting the emulator based on the parameters supplied in the pom file and on the
     * command line. It should not be that painful to do work with command line and pom supplied values but evidently
     * it is.
     *
     * @return
     * @throws MojoExecutionException
     * @see com.jayway.maven.plugins.android.configuration.Emulator
     */
    private String assembleStartCommandLine() throws MojoExecutionException
    {
        StringBuilder startCommandline =
                new StringBuilder().append(getAndroidSdk().getEmulatorPath()).append(" -avd ").append(parsedAvd)
                        .append(" ");
        if ( !StringUtils.isEmpty(parsedOptions) )
        {
            startCommandline.append(parsedOptions);
        }
        getLog().info("Android emulator command: " + startCommandline);
        return startCommandline.toString();
    }

    private void parseParameters()
    {
        // <emulator> exist in pom file
        if ( emulator != null )
        {
            // <emulator><avd> exists in pom file
            if ( emulator.getAvd() != null )
            {
                parsedAvd = emulator.getAvd();
            } else
            {
                parsedAvd = determineAvd();
            }
            // <emulator><options> exists in pom file
            if ( emulator.getOptions() != null )
            {
                parsedOptions = emulator.getOptions();
            } else
            {
                parsedOptions = determineOptions();
            }
            // <emulator><wait> exists in pom file
            if ( emulator.getWait() != null )
            {
                parsedWait = emulator.getWait();
            } else
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
     *
     * @return if available return command line value otherwise return default value (5000).
     */
    private String determineWait()
    {
        String wait;
        if ( emulatorWait != null )
        {
            wait = emulatorWait;
        } else
        {
            wait = "5000";
        }

        return wait;
    }

    /**
     * Get options value for emulator from command line option.
     *
     * @return if available return command line value otherwise return default value ("").
     */
    private String determineOptions()
    {
        String options;
        if ( emulatorOptions != null )
        {
            options = emulatorOptions;
        } else
        {
            options = "";
        }
        return options;
    }

    /**
     * Get avd value for emulator from command line option.
     *
     * @return if available return command line value otherwise return default value ("Default").
     */
    private String determineAvd()
    {
        String avd;
        if ( emulatorAvd != null )
        {
            avd = emulatorAvd;
        } else
        {
            avd = "Default";
        }
        return avd;
    }
}