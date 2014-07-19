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
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Deploys all directly declared dependencies of <code>&lt;type&gt;apk&lt;/type&gt;</code> in this project's pom.<br/>
 * Usually used in a project with instrumentation tests, to deploy the apk to test onto the device before running the
 * deploying and running the instrumentation tests apk.<br/>
 * Automatically performed when running <code>mvn integration-test</code> (or <code>mvn install</code>) on a project
 * with instrumentation tests.
 *
 * @author hugo.josefson@jayway.com
 */
@Mojo( name = "deploy-dependencies", requiresDependencyResolution = ResolutionScope.RUNTIME )
public class DeployDependenciesMojo extends AbstractAndroidMojo
{

    /**
     *
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        deployDependencies();
    }


}
