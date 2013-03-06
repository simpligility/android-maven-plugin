/*
 * Copyright (C) 2009 Jayway AB
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
package com.jayway.maven.plugins.android.standalonemojos;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;
import com.android.ddmlib.testrunner.UIAutomatorRemoteAndroidTestRunner;
import com.jayway.maven.plugins.android.AbstractInstrumentationMojo;
import com.jayway.maven.plugins.android.DeviceCallback;
import com.jayway.maven.plugins.android.ScreenshotServiceWrapper;
import com.jayway.maven.plugins.android.common.DeviceHelper;
import com.jayway.maven.plugins.android.config.ConfigPojo;
import com.jayway.maven.plugins.android.config.PullParameter;
import com.jayway.maven.plugins.android.configuration.UIAutomatorTest;

/**
 * Can execute tests for ui automator.<br/>
 * Called automatically when the lifecycle reaches phase <code>install</code>.
 * 
 * @author Stéphane Nicolas <snicolas@octo.com>
 * @goal ui-automator-test
 * @phase install
 * @requiresProject false
 */
@SuppressWarnings( "unused" )
public class UIAutomatorTestMojo extends AbstractInstrumentationMojo
{

    @ConfigPojo
    private UIAutomatorTest uiAutomatorTest;

    /**
     * Enables or disables ui automator test goal. If <code>true</code> it will be skipped; if <code>false</code>, it
     * will be run.
     * 
     * @parameter expression="${android.uiautomatortest.skip}" default-value="true"
     */
    private Boolean testSkip;

    @PullParameter( required = false )
    private boolean parsedSkip;

    /**
     * Jar file that will be run during ui automator tests.
     * 
     * @parameter expression="${android.uiautomatortest.jarFile}"
     */
    private String jarFile;

    @PullParameter( defaultValueGetterMethod = "getJarFile" )
    private String parsedJarFile;

    /**
     * Test class or methods to execute during ui automator tests. Each class or method must be fully qualified with the
     * package name, in one of these formats:
     * <ul>
     * <li>package_name.class_name
     * <li>package_name.class_name#method_name
     * </ul>
     * 
     * @parameter expression="${android.uiautomatortest.testClassOrMethod}"
     * 
     */
    private String testClassOrMethod;

    @PullParameter( required = true )
    private String parsedTestClassOrMethod;

    /**
     * Decides whether to run the test to completion on the device even if its parent process is terminated (for
     * example, if the device is disconnected).
     * 
     * @parameter expression="${android.uiautomatortest.noHup}" default-value="false"
     * 
     */
    private Boolean noHup = false;

    @PullParameter( required = false )
    private boolean parsedNoHup;

    /**
     * Decides whether to wait for debugger to connect before starting.
     * 
     * @parameter expression="${android.uiautomatortest.debug}" default-value="false"
     * 
     */
    private Boolean debug = false;

    @PullParameter( required = false )
    private boolean parsedDebug;

    /**
     * Decides whether to use a dump file or not.
     * 
     * @parameter expression="${android.uiautomatortest.useDump}" default-value="false"
     * 
     */
    private Boolean useDump = false;

    @PullParameter( required = false )
    private boolean parsedUseDump;

    /**
     * Generate an XML file with a dump of the current UI hierarchy. If a filepath is not specified, by default, the
     * generated dump file is stored on the device in this location /storage/sdcard0/window_dump.xml.
     * 
     * @parameter expression="${android.uiautomatortest.dumpFilePath}" default-value="/storage/sdcard0/window_dump.xml"
     * 
     */
    private String dumpFilePath;

    @PullParameter( required = false, defaultValue = "/storage/sdcard0/window_dump.xml" )
    private String parsedDumpFilePath;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        instrument();
    }

    @Override
    protected void instrument() throws MojoExecutionException, MojoFailureException
    {
        // parseConfiguration();

        DeviceCallback instrumentationTestExecutor = new DeviceCallback()
        {
            @Override
            public void doWithDevice( final IDevice device ) throws MojoExecutionException, MojoFailureException
            {
                String deviceLogLinePrefix = DeviceHelper.getDeviceLogLinePrefix( device );

                UIAutomatorRemoteAndroidTestRunner uIAutomatorRemoteAndroidTestRunner //
                = new UIAutomatorRemoteAndroidTestRunner( jarFile, testClassOrMethod, device );

                uIAutomatorRemoteAndroidTestRunner.setDebug( debug );

                getLog().info( deviceLogLinePrefix + "Running instrumentation tests in <package>" );
                try
                {
                    AndroidTestRunListener testRunListener = new AndroidTestRunListener( project, device );
                    uIAutomatorRemoteAndroidTestRunner.run( testRunListener );
                    if ( testRunListener.hasFailuresOrErrors() )
                    {
                        throw new MojoFailureException( deviceLogLinePrefix + "Tests failed on device." );
                    }
                    if ( testRunListener.testRunFailed() )
                    {
                        throw new MojoFailureException( deviceLogLinePrefix + "Test run failed to complete: "
                                + testRunListener.getTestRunFailureCause() );
                    }
                    if ( testRunListener.threwException() )
                    {
                        throw new MojoFailureException( deviceLogLinePrefix + testRunListener.getExceptionMessages() );
                    }
                }
                catch ( TimeoutException e )
                {
                    throw new MojoExecutionException( deviceLogLinePrefix + "timeout", e );
                }
                catch ( AdbCommandRejectedException e )
                {
                    throw new MojoExecutionException( deviceLogLinePrefix + "adb command rejected", e );
                }
                catch ( ShellCommandUnresponsiveException e )
                {
                    throw new MojoExecutionException( deviceLogLinePrefix + "shell command " + "unresponsive", e );
                }
                catch ( IOException e )
                {
                    throw new MojoExecutionException( deviceLogLinePrefix + "IO problem", e );
                }
            }
        };

        instrumentationTestExecutor = new ScreenshotServiceWrapper( instrumentationTestExecutor, project, getLog() );

        doWithDevices( instrumentationTestExecutor );
    }

    private void parseConfiguration()
    {
        // we got config in pom ... lets use it,
        if ( uiAutomatorTest != null )
        {
            if ( uiAutomatorTest.getSkip() != null )
            {
                parsedSkip = uiAutomatorTest.getSkip();
            }
            else
            {
                parsedSkip = testSkip;
            }
            if ( StringUtils.isNotEmpty( uiAutomatorTest.getJarFile() ) )
            {
                parsedJarFile = uiAutomatorTest.getJarFile();
            }
            else
            {
                parsedJarFile = jarFile;
            }
            if ( uiAutomatorTest.getTestClassOrMethod() != null && !uiAutomatorTest.getTestClassOrMethod().isEmpty() )
            {
                parsedTestClassOrMethod = buildSpaceSeparatedString( uiAutomatorTest.getTestClassOrMethod() );
            }
            else
            {
                parsedTestClassOrMethod = testClassOrMethod;
            }

        }
        // no pom, we take properties
        else
        {
            parsedSkip = testSkip;
            parsedJarFile = jarFile;
            parsedTestClassOrMethod = testClassOrMethod;
            parsedNoHup = noHup;
            parsedDebug = debug;
            parsedUseDump = useDump;
            parsedDumpFilePath = dumpFilePath;
        }
    }

    private String getJarFile()
    {
        getLog().debug( "get parsed jar file:" + parsedJarFile );

        if ( parsedJarFile == null )
        {
            File jarFilePath = new File( project.getBuild().getDirectory() + File.separator
                    + project.getBuild().getFinalName() + ".jar" );
            return jarFilePath.getAbsolutePath();
        }
        return parsedJarFile;
    }

    /**
     * Helper method to build a comma separated string from a list. Blank strings are filtered out
     * 
     * @param lines
     *            A list of strings
     * @return Comma separated String from given list
     */
    protected static String buildSpaceSeparatedString( List< String > lines )
    {
        if ( lines == null || lines.size() == 0 )
        {
            return null;
        }

        List< String > strings = new ArrayList< String >( lines.size() );
        for ( String str : lines )
        { // filter out blank strings
            if ( StringUtils.isNotBlank( str ) )
            {
                strings.add( StringUtils.trimToEmpty( str ) );
            }
        }

        return StringUtils.join( strings, " " );
    }

}
