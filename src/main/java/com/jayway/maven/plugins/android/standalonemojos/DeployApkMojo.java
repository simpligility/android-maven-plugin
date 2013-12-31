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
import com.jayway.maven.plugins.android.config.ConfigHandler;
import com.jayway.maven.plugins.android.config.ConfigPojo;
import com.jayway.maven.plugins.android.config.PullParameter;
import com.jayway.maven.plugins.android.configuration.DeployApk;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;

/**
 * Deploys the built apk file, or another specified apk, to a connected device.<br/>
 * Automatically performed when running <code>mvn integration-test</code> (or <code>mvn install</code>) on a project
 * with instrumentation tests.
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
     * @parameter expression="${android.deployApk.apkFile}"
     * @optional
    */
    private File apkFile;

    @PullParameter
    private File parsedApkFile;

    /**
     * @parameter expression="${android.deployApk.packageName}"
     * @optional
    */
    private String packageName;

    @PullParameter
    private String parsedPackageName;

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
        
        if ( parsedApkFile != null && parsedApkFile.isFile() )
        {
            getLog().debug( "Deploying with apkFile " + parsedApkFile );
            deployApk( parsedApkFile );
        } 
        else 
        {
            throw new MojoFailureException( "Insufficient parameters ... add more " );
        }
    }

}
