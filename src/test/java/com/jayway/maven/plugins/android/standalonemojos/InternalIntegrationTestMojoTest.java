/*
 * Copyright (C) 2012 Jayway AB
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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.Client;
import com.android.ddmlib.FileListingService;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.IShellOutputReceiver;
import com.android.ddmlib.InstallException;
import com.android.ddmlib.RawImage;
import com.android.ddmlib.ScreenRecorderOptions;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.SyncException;
import com.android.ddmlib.SyncService;
import com.android.ddmlib.TimeoutException;
import com.android.ddmlib.log.LogReceiver;
import com.android.ddmlib.testrunner.RemoteAndroidTestRunner;
import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import com.jayway.maven.plugins.android.AbstractAndroidMojoTestCase;
import com.jayway.maven.plugins.android.DeviceCallback;
import com.jayway.maven.plugins.android.phase12integrationtest.InternalIntegrationTestMojo;

/**
 * Tests the {@link InternalIntegrationTestMojo} mojo, as far as possible without actually
 * connecting and communicating with a device.
 * 
 * @author Erik Ogenvik
 * 
 */
@Ignore("This test has to be migrated to be an IntegrationTest using AbstractAndroidMojoIntegrationTest") 
@RunWith(PowerMockRunner.class)
@PrepareForTest({ RemoteAndroidTestRunner.class, AbstractAndroidMojo.class })
public class InternalIntegrationTestMojoTest extends AbstractAndroidMojoTestCase<InternalIntegrationTestMojo> {

    @Override
    public String getPluginGoalName() {
        return "internal-integration-test";
    }

    @Test
    public void testTestProject() throws Exception {

        // We need to do some fiddling to make sure we run as far into the Mojo as possible without
        // actually sending stuff to a device.
        PowerMock.suppress(MemberMatcher.methodsDeclaredIn(RemoteAndroidTestRunner.class));
        PowerMock.replace(AbstractAndroidMojo.class.getDeclaredMethod("doWithDevices", DeviceCallback.class)).with(new InvocationHandler() {

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                // Just fake that we've found a device.
                DeviceCallback callback = (DeviceCallback) args[0];
                callback.doWithDevice(new IDevice() {

                    @Override
                    public String getSerialNumber() {

                        return null;
                    }

                    @Override
                    public String getAvdName() {

                        return null;
                    }

                    @Override
                    public DeviceState getState() {

                        return null;
                    }

                    @Override
                    public Map<String, String> getProperties() {

                        return null;
                    }

                    @Override
                    public int getPropertyCount() {

                        return 0;
                    }

                    @Override
                    public String getProperty(String name) {

                        return null;
                    }

                    @Override
                    public String getMountPoint(String name) {

                        return null;
                    }

                    @Override
                    public boolean isOnline() {

                        return false;
                    }

                    @Override
                    public boolean isEmulator() {

                        return false;
                    }

                    @Override
                    public boolean isOffline() {

                        return false;
                    }

                    @Override
                    public boolean isBootLoader() {

                        return false;
                    }

                    @Override
                    public boolean hasClients() {

                        return false;
                    }

                    @Override
                    public Client[] getClients() {

                        return null;
                    }

                    @Override
                    public Client getClient(String applicationName) {

                        return null;
                    }

                    @Override
                    public SyncService getSyncService() throws TimeoutException, AdbCommandRejectedException, IOException {

                        return null;
                    }

                    @Override
                    public FileListingService getFileListingService() {

                        return null;
                    }

                    @Override
                    public RawImage getScreenshot() throws TimeoutException, AdbCommandRejectedException, IOException {

                        return null;
                    }

                    @Override
                    public void executeShellCommand(String command, IShellOutputReceiver receiver) throws
                            TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException,
                            IOException {
                    }

                    @Override
                    public void executeShellCommand(String command, IShellOutputReceiver receiver,
                                                    int maxTimeToOutputResponse) throws TimeoutException,
                            AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException {

                    }

                    @Override
                    public void runEventLogService(LogReceiver receiver) throws TimeoutException,
                            AdbCommandRejectedException, IOException {

                    }

                    @Override
                    public void runLogService(String logname, LogReceiver receiver) throws TimeoutException,
                            AdbCommandRejectedException, IOException {

                    }

                    @Override
                    public void createForward(int localPort, int remotePort) throws TimeoutException,
                            AdbCommandRejectedException, IOException {

                    }

                    @Override
                    public void removeForward(int localPort, int remotePort) throws TimeoutException,
                            AdbCommandRejectedException, IOException {

                    }

                    @Override
                    public String getClientName(int pid) {

                        return null;
                    }

                    @Override
                    public String syncPackageToDevice(String localFilePath) throws TimeoutException, AdbCommandRejectedException, IOException, SyncException {

                        return null;
                    }


                    @Override
                    public void removeRemotePackage(String remoteFilePath) throws InstallException {

                    }

                    @Override
                    public String uninstallPackage(String packageName) throws InstallException {
                        return null;
                    }

                    @Override
                    public void reboot(String into) throws TimeoutException, AdbCommandRejectedException, IOException {

                    }

                    @Override
                    public boolean arePropertiesSet() {
                        return false;
                    }

                    @Override
                    public String getPropertySync(String s) throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException {
                        return null;
                    }

                    @Override
                    public String getPropertyCacheOrSync(String s) throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException {
                        return null;
                    }

                    @Override
                    public void pushFile(String s, String s1) throws IOException, AdbCommandRejectedException, TimeoutException, SyncException {

                    }

                    @Override
                    public void pullFile(String s, String s1) throws IOException, AdbCommandRejectedException, TimeoutException, SyncException {

                    }

                    @Override
                    public String installPackage(String s, boolean b, String... strings) throws InstallException {
                        return null;
                    }

                    @Override
                    public void installPackages(List<String> list, int i, boolean b, String... strings) throws InstallException {

                    }

                    @Override
                    public String installRemotePackage(String s, boolean b, String... strings) throws InstallException {
                        return null;
                    }

                    @Override
                    public Integer getBatteryLevel() throws TimeoutException, AdbCommandRejectedException, IOException, ShellCommandUnresponsiveException {
                        return null;
                    }

                    @Override
                    public Integer getBatteryLevel(long l) throws TimeoutException, AdbCommandRejectedException, IOException, ShellCommandUnresponsiveException {
                        return null;
                    }

                    @Override
                    public Future<Integer> getBattery() {
                        return null;
                    }

                    @Override
                    public Future<Integer> getBattery(long l, TimeUnit timeUnit) {
                        return null;
                    }

                    @Override
                    public void createForward(int arg0, String arg1, DeviceUnixSocketNamespace arg2)
                            throws TimeoutException, AdbCommandRejectedException, IOException
                    {
                    }

                    @Override
                    public String getName()
                    {
                        return null;
                    }

                    @Override
                    public void executeShellCommand(String s, IShellOutputReceiver iShellOutputReceiver, long l, TimeUnit timeUnit)
                        throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException
                    {
                    }

                    @Override
                    public Future<String> getSystemProperty(String s) {
                        return null;
                    }

                    @Override
                    public void removeForward(int arg0, String arg1, DeviceUnixSocketNamespace arg2)
                            throws TimeoutException, AdbCommandRejectedException, IOException
                    {
                    }

                    @Override
                    public boolean supportsFeature(Feature feature) {
                        return false;
                    }

                    @Override
                    public void startScreenRecorder(String remoteFilePath,
                            ScreenRecorderOptions options,
                            IShellOutputReceiver receiver)
                            throws TimeoutException,
                            AdbCommandRejectedException, IOException,
                            ShellCommandUnresponsiveException {
                    }

                    @Override
                    public boolean supportsFeature(HardwareFeature arg0) {
                      // TODO Auto-generated method stub
                      return false;
                    }

                    @Override
                    public List<String> getAbis() {
                      // TODO Auto-generated method stub
                      return null;
                    }

                    @Override
                    public int getDensity() {
                      // TODO Auto-generated method stub
                      return 0;
                    }
                    
                    
                });
                return null;
            }
        });

        InternalIntegrationTestMojo mojo = createMojo("manifest-tests/test-project");

        mojo.execute();
        List<String> classes = Whitebox.getInternalState(mojo, "parsedClasses");
        assertNotNull(classes);
        assertEquals(1, classes.size());
    }
}
