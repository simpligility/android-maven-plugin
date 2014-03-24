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


import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import com.jayway.maven.plugins.android.common.AndroidExtension;

/**
 * Undeploys the apk(s) of the current project(s) to all attached devices and emulators.
 * Automatically skips other projects in a multi-module build that do not use packaging
 * apk without terminating.<br/>
 * Deploymnet is automatically performed when running <code>mvn integration-test</code>
 * (or <code>mvn install</code>) on a project with instrumentation tests.
 *
 * @author hugo.josefson@jayway.com
 * @author Manfred Moser <manfred@simpligility.com>
 *
 * @goal undeploy
 * @requiresProject true
 */
public class UndeployMojo extends AbstractAndroidMojo
{
    /**
     *
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    public void doExecute() throws MojoExecutionException, MojoFailureException
    {
        String packageToUndeploy;
        if ( project.getPackaging().equals( AndroidExtension.APK ) ) 
        {
            packageToUndeploy = renameManifestPackage != null
                ? renameManifestPackage
                : extractPackageNameFromAndroidManifest( androidManifestFile );
            if ( StringUtils.isNotBlank( packageToUndeploy ) ) 
            {
                undeployApk( packageToUndeploy );
            }
        }
        else 
        {
            getLog().info( "Project packaging is not apk, skipping undeployment." );
        }
    }
}
