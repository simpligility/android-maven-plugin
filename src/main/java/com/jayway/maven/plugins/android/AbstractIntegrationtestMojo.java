/*
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
package com.jayway.maven.plugins.android;

import java.io.File;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.jayway.maven.plugins.android.asm.AndroidTestFinder;

/**
 * For integrationtest related Mojos.
 *
 * @author hugo.josefson@jayway.com
 */
public abstract class AbstractIntegrationtestMojo extends AbstractAndroidMojo {
    /**
     * -Dmaven.test.skip is commonly used with Maven to skip tests. We honor it too.
     *
     * @parameter expression="${maven.test.skip}" default-value=false
     * @readonly
     */
    private boolean mavenTestSkip;

    /**
     * Enables or disables integration test related goals. If <code>true</code> they will be run; if <code>false</code>,
     * they will be skipped. If <code>auto</code>, they will run if any of the classes inherit from any class in
     * <code>junit.framework.**</code> or <code>android.test.**</code>.
     *
     * @parameter expression="${android.enableIntegrationTest}" default-value="auto"
     */
    private String enableIntegrationTest;

    /**
     * Whether or not to execute integration test related goals. Reads from configuration parameter
     * <code>enableIntegrationTest</code>, but can be overridden with <code>-Dmaven.test.skip</code>.
     *
     * @return <code>true</code> if integration test goals should be executed, <code>false</code> otherwise.
     */
    protected boolean isEnableIntegrationTest() throws MojoFailureException, MojoExecutionException {
        if (mavenTestSkip) {
            return false;
        }

        if ("false".equalsIgnoreCase(enableIntegrationTest)) {
            return false;
        }

        if ("true".equalsIgnoreCase(enableIntegrationTest)) {
            return true;
        }

        if ("auto".equalsIgnoreCase(enableIntegrationTest)) {
            if (extractInstrumentationRunnerFromAndroidManifest(androidManifestFile) == null) {
                return false;
            }
            return AndroidTestFinder.containsAndroidTests(new File(project.getBuild().getDirectory(), "android-classes"));
        }

        throw new MojoFailureException("enableIntegrationTest must be configured as 'true', 'false' or 'auto'.");

    }

    protected void deployDependencies() throws MojoExecutionException {
        Set<Artifact> directDependentArtifacts = project.getDependencyArtifacts();
        if (directDependentArtifacts != null) {
            for (Artifact artifact : directDependentArtifacts) {
                String type = artifact.getType();
                if (type.equals("apk")) {
                    getLog().debug("Detected apk dependency " + artifact + ". Will resolve and deploy to device...");
                    final File targetApkFile = resolveArtifactToFile(artifact);
                    if (undeployBeforeDeploy) {
                        getLog().debug("Attempting undeploy of " + targetApkFile + " from device...");
                        undeployApk(targetApkFile);
                    }
                    getLog().debug("Deploying " + targetApkFile + " to device...");
                    deployApk(targetApkFile);
                }
            }
        }
    }

    protected void deployBuiltApk() throws MojoExecutionException {
        // If we're not on a supported packaging with just skip (Issue 112)
        // http://code.google.com/p/maven-android-plugin/issues/detail?id=112
        if (! SUPPORTED_PACKAGING_TYPES.contains(project.getPackaging())) {
            getLog().info("Skipping deployment on " + project.getPackaging());
            return;
        }
        File apkFile = new File(project.getBuild().getDirectory(), project.getBuild().getFinalName()
                + ANDROID_PACKAGE_EXTENSTION);
        deployApk(apkFile);
    }

    /**
     * Deploy an apk file, but undeploy first if {@link #undeployBeforeDeploy}{@code == true}.
     * @param apkFile the apk file to deploy
     * @throws MojoExecutionException
     */
    protected void deployFile(File apkFile) throws MojoExecutionException {
        if (undeployBeforeDeploy) {
            undeployApk(apkFile);
        }
        deployApk(apkFile);
    }


}
