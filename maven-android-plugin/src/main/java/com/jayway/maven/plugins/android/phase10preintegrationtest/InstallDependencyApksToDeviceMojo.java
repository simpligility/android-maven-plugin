/*
 * Copyright (C) 2007-2008 JVending Masa
 * Copyright (C) 2009 Jayway AB
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
package com.jayway.maven.plugins.android.phase10preintegrationtest;

import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.util.Set;

/**
 * Installs the apk we are about to test on the connected device. All directly declared dependencies of
 * <code>&lt;type&gt;android:apk&lt;/type&gt;</code> in this project's pom are presumed to be the apk's to install.<br/>
 * Automatically performed when running <code>mvn integration-test</code> (or <code>mvn install</code>) on a project
 * with <code>&lt;packaging&gt;android:apk:platformTest&lt;/packaging&gt;</code>.
 * @goal installDependencyApksToDevice
 * @phase pre-integration-test
 * @requiresDependencyResolution runtime
 * @author hugo.josefson@jayway.com
 */
public class InstallDependencyApksToDeviceMojo extends AbstractAndroidMojo {

    public void execute() throws MojoExecutionException, MojoFailureException {
        Set<Artifact> directDependentArtifacts = project.getDependencyArtifacts();
        if (directDependentArtifacts != null) {
            for (Artifact artifact : directDependentArtifacts) {
                String type = artifact.getType();
                if (type.equals("android:apk")) {
                    getLog().debug("Detected android:apk dependency " + artifact + ". Will resolve and install to device...");
                    final File targetApkFile = resolveArtifactToFile(artifact);
                    if (uninstallApkBeforeInstallingToDevice){
                        getLog().debug("Attempting uninstall of " + targetApkFile + " from device...");
                        uninstallApkFromDevice(targetApkFile);
                    }
                    getLog().debug("Installing " + targetApkFile + " to device...");
                    installApkToDevice(targetApkFile);
                }
            }
        }
    }

}