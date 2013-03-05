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

import java.io.IOException;

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

/**
 * Internal. Do not use.<br/>
 * Called automatically when the lifecycle reaches phase
 * <code>integration-test</code>. Figures out whether to call goals in this
 * phase; and if so, calls <code>android:instrument</code>.
 * @author hugo.josefson@jayway.com
 * @goal ui-automator-test
 * @phase install
 */
public class UIAutomatorTestMojo extends AbstractInstrumentationMojo {

    private String jarFile = "android-sample-ui-tests-0.0.1-SNAPSHOT.jar";
    // TODO should be a space separated array
    private String testClassOrMethod = "com.octo.android.sample.uitest.LaunchSettings";
    private boolean noHup = false;
    private boolean debug = false;
    private boolean useDump = false;
    private String dumpFile;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        instrument();
    }

    @Override
    protected void instrument() throws MojoExecutionException, MojoFailureException {
        // parseConfiguration();

        DeviceCallback instrumentationTestExecutor = new DeviceCallback() {
            @Override
            public void doWithDevice(final IDevice device) throws MojoExecutionException, MojoFailureException {
                String deviceLogLinePrefix = DeviceHelper.getDeviceLogLinePrefix(device);

                UIAutomatorRemoteAndroidTestRunner uIAutomatorRemoteAndroidTestRunner = new UIAutomatorRemoteAndroidTestRunner(jarFile,
                    testClassOrMethod, device);

                uIAutomatorRemoteAndroidTestRunner.setDebug(debug);

                getLog().info(deviceLogLinePrefix + "Running instrumentation tests in <package>");
                try {
                    AndroidTestRunListener testRunListener = new AndroidTestRunListener(project, device);
                    uIAutomatorRemoteAndroidTestRunner.run(testRunListener);
                    if (testRunListener.hasFailuresOrErrors()) {
                        throw new MojoFailureException(deviceLogLinePrefix + "Tests failed on device.");
                    }
                    if (testRunListener.testRunFailed()) {
                        throw new MojoFailureException(deviceLogLinePrefix + "Test run failed to complete: "
                            + testRunListener.getTestRunFailureCause());
                    }
                    if (testRunListener.threwException()) {
                        throw new MojoFailureException(deviceLogLinePrefix + testRunListener.getExceptionMessages());
                    }
                } catch (TimeoutException e) {
                    throw new MojoExecutionException(deviceLogLinePrefix + "timeout", e);
                } catch (AdbCommandRejectedException e) {
                    throw new MojoExecutionException(deviceLogLinePrefix + "adb command rejected", e);
                } catch (ShellCommandUnresponsiveException e) {
                    throw new MojoExecutionException(deviceLogLinePrefix + "shell command " + "unresponsive", e);
                } catch (IOException e) {
                    throw new MojoExecutionException(deviceLogLinePrefix + "IO problem", e);
                }
            }
        };

        instrumentationTestExecutor = new ScreenshotServiceWrapper(instrumentationTestExecutor, project, getLog());

        doWithDevices(instrumentationTestExecutor);
    }

}
