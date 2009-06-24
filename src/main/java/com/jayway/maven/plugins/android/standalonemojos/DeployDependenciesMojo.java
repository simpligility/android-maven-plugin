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

import com.jayway.maven.plugins.android.AbstractIntegrationtestMojo;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.util.Set;

/**
 * Deploys the apk we are about to test on the connected device. All directly declared dependencies of
 * <code>&lt;type&gt;apk&lt;/type&gt;</code> in this project's pom are presumed to be the apk's to deploy.<br/>
 * Automatically performed when running <code>mvn integration-test</code> (or <code>mvn install</code>) on an Android
 * platformtest project.
 * @goal deployDependencies
 * @phase pre-integration-test
 * @requiresDependencyResolution runtime
 * @author hugo.josefson@jayway.com
 */
public class DeployDependenciesMojo extends AbstractIntegrationtestMojo {

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!isEnableIntegrationTest()){
            return;
        }

        Set<Artifact> directDependentArtifacts = project.getDependencyArtifacts();
        if (directDependentArtifacts != null) {
            for (Artifact artifact : directDependentArtifacts) {
                String type = artifact.getType();
                if (type.equals("apk")) {
                    getLog().debug("Detected apk dependency " + artifact + ". Will resolve and deploy to device...");
                    final File targetApkFile = resolveArtifactToFile(artifact);
                    if (undeployApkBeforeDeploying){
                        getLog().debug("Attempting undeploy of " + targetApkFile + " from device...");
                        undeployApk(targetApkFile);
                    }
                    getLog().debug("Deploying " + targetApkFile + " to device...");
                    deployApk(targetApkFile);
                }
            }
        }
    }

}
