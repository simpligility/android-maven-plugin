/**
 * Copyright 2012 SonyEricsson
 */

package com.jayway.maven.plugins.android.standalonemojos;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

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
                    public void executeShellCommand(String command, IShellOutputReceiver receiver) throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException {

                    }

                    @Override
                    public void executeShellCommand(String command, IShellOutputReceiver receiver, int maxTimeToOutputResponse) throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException {

                    }

                    @Override
                    public void runEventLogService(LogReceiver receiver) throws TimeoutException, AdbCommandRejectedException, IOException {

                    }

                    @Override
                    public void runLogService(String logname, LogReceiver receiver) throws TimeoutException, AdbCommandRejectedException, IOException {

                    }

                    @Override
                    public void createForward(int localPort, int remotePort) throws TimeoutException, AdbCommandRejectedException, IOException {

                    }

                    @Override
                    public void removeForward(int localPort, int remotePort) throws TimeoutException, AdbCommandRejectedException, IOException {

                    }

                    @Override
                    public String getClientName(int pid) {

                        return null;
                    }

                    @Override
                    public String installPackage(String packageFilePath, boolean reinstall) throws InstallException {

                        return null;
                    }

                    @Override
                    public String syncPackageToDevice(String localFilePath) throws TimeoutException, AdbCommandRejectedException, IOException, SyncException {

                        return null;
                    }

                    @Override
                    public String installRemotePackage(String remoteFilePath, boolean reinstall) throws InstallException {

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
