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
import com.jayway.maven.plugins.android.configuration.ValidationResponse;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

/**
 * Deploys a specified Android application apk to attached devices and emulators. 
 * By default it will deploy to all, but a subset or single one can be configured 
 * with the device and devices parameters.This goal can be used in non-android 
 * projects and as standalone execution on the command line. <br/>
 *
 * @author Manfred Moser <manfred@simpligility.com>
 */
@Mojo( name = "deploy-apk", requiresProject = false )
public class DeployApkMojo extends AbstractAndroidMojo
{
    /**
     * Configuration for apk file deployment within a pom file. See {@link #deployapkFilename}. 
     * <p/>
     * <pre>
     * &lt;deployapk&gt;
     *    &lt;filename&gt;yourapk.apke&lt;/filename&gt;
     * &lt;/deployapk&gt;
     * </pre>
     */
    @Parameter
    @ConfigPojo
    protected DeployApk deployapk;

    @Parameter( property = "android.deployapk.filename" )
    private File deployapkFilename;

    @PullParameter
    private File parsedFilename;

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
        
        ValidationResponse response = DeployApk.validFileParameter( parsedFilename );
        if ( response.isValid() ) 
        {   
            getLog().info( "Deploying apk file at " + parsedFilename );
            deployApk( parsedFilename );
        } 
        else 
        {
            throw new MojoFailureException( response.getMessage() );
        }
    }
}
