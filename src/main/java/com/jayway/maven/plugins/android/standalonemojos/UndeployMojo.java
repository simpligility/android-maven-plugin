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

import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;

/**
 * Undeploys the built apk file, or another specified apk, from a connected device.<br/>
 *
 * @author hugo.josefson@jayway.com
 * @goal undeploy
 * @requiresProject false
 */
public class UndeployMojo extends AbstractAndroidMojo
{

    /**
     * Optionally used to specify a different apk package to undeploy from a connected emulator or usb device, instead
     * of the built apk from this project.
     *
     * @parameter property="package" expression="${android.package}" default-value="null"
     */
    private String packageName;

    public void setPackage( String packageName )
    {
        this.packageName = packageName;
    }

    /**
     * Optionally used to specify a different apk file to undeploy from a connected emulator or usb device, instead of
     * the built apk from this project.
     *
     * @parameter expression="${android.file}"
     */
    private File file;

    /**
     *
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        String packageToUndeploy = packageName;
        if ( packageToUndeploy != null && ! "".equals( packageToUndeploy ) && ! "null".equals( packageToUndeploy ) )
        {
            undeployApk( packageToUndeploy );
        }
        else
        {
            if ( file != null )
            {
                undeployApk( file );
            }
            else
            {
                if ( ! SUPPORTED_PACKAGING_TYPES.contains( project.getPackaging() ) )
                {
                    getLog().info( "Skipping undeploy on " + project.getPackaging() );
                    getLog().info( "Execute undeploy within an Maven Android project or specify package with e.g. "
                            + "-Dandroid.package=com.simpligility.android.helloflashlight" );
                    return;
                }

                packageToUndeploy = renameManifestPackage != null
                    ? renameManifestPackage
                    : extractPackageNameFromAndroidManifest( androidManifestFile );

                undeployApk( packageToUndeploy );
            }
        }

    }
}
