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
import com.jayway.maven.plugins.android.common.AndroidExtension;
import com.jayway.maven.plugins.android.config.ConfigHandler;
import com.jayway.maven.plugins.android.config.ConfigPojo;
import com.jayway.maven.plugins.android.config.PullParameter;
import com.jayway.maven.plugins.android.configuration.DeployApk;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;

/**
 * Deploys a specified Android application apk to attached devices and emulators. 
 * By default it will deploy to all, but subset or single one can be configured 
 * with the device and devices parameters.<br/>
 *
 * @author Manfred Moser <manfred@simpligility.com>
 * 
 * @goal deploy-apk
 * @requiresProject false
 */
public class DeployApkMojo extends AbstractAndroidMojo
{
    @ConfigPojo
    protected DeployApk deployApk;

    /**
     * @parameter expression="${android.deployApk.file}"
     * @optional
    */
    private File deployApkFile;

    @PullParameter
    private File parsedFile;

    /**
     * @parameter expression="${android.deployApk.package}"
     * @optional
    */
    private String deployApkPackage;

    @PullParameter
    private String parsedPackage;

    /**
     * Deploy the app to the attached devices and emulators.
     *
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        ConfigHandler configHandler = new ConfigHandler( this, this.session, this.execution );
        configHandler.parseConfiguration();
        
        if ( parsedFile == null )
        {
            throw new MojoFailureException( "\n\n The parameter android.deployApk.file is missing. \n"
                    + "Try mvn android:deploy-apk -Dandroid.deployApk.file=yourapk.apk\n" );
        }
        else if ( !parsedFile.isFile() )
        {
            throw new MojoFailureException( "\n\n The file parameter does not point to a file: " 
                    + parsedFile.getAbsolutePath() + "\n" );
        }
        else if ( !parsedFile.getAbsolutePath().toLowerCase().endsWith( AndroidExtension.APK ) )
        {
            throw new MojoFailureException( "\n\n The file parameter does not point to an APK: " 
                    + parsedFile.getAbsolutePath() + "\n" );
        }
        else 
        {
            getLog().info( "Deploying apk file at " + parsedFile );
            deployApk( parsedFile );
        } 
    }
}
