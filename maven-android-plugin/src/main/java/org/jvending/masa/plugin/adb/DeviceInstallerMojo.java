/*
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
package org.jvending.masa.plugin.adb;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.jvending.masa.plugin.AbstractAndroidMojo;

import java.io.File;

/**
 * Installs the apk file to a connected device. Automatically performed when running <code>mvn integration-test</code>
 * (or <code>mvn install</code>) on a project with
 * <code>&lt;packaging&gt;android:apk:platformTest&lt;/packaging&gt;</code>.
 * @goal adbInstall
 */
public class DeviceInstallerMojo extends AbstractAndroidMojo {

    public void execute() throws MojoExecutionException, MojoFailureException {
        File inputFile = new File(project.getBuild().getDirectory(), project.getBuild().getFinalName() + ".apk");
        if (uninstallApkBeforeInstallingToDevice){
            uninstallApkFromDevice(inputFile);
        }
        installApkToDevice(inputFile);
    }

}
