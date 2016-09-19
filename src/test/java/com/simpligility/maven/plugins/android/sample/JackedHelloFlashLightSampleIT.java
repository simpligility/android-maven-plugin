/*
 * Copyright (C) 2016 simpligility technologies inc.
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
 *
 * Contributors : 
 * JBoss, Home of Professional Open Source
 */
package com.simpligility.maven.plugins.android.sample;

import io.takari.maven.testing.TestResources;
import io.takari.maven.testing.executor.MavenExecutionResult;
import io.takari.maven.testing.executor.MavenRuntime;
import io.takari.maven.testing.executor.MavenVersions;
import io.takari.maven.testing.executor.MavenRuntime.MavenRuntimeBuilder;
import io.takari.maven.testing.executor.junit.MavenJUnitTestRunner;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.simpligility.maven.plugins.android.PluginInfo;

/**
 * 
 * This project tests end to end building of the project using the Jack compiler.
 * 
 * @author summers
 */
@RunWith(MavenJUnitTestRunner.class)
@MavenVersions({"3.0.5", "3.3.9"})
public class JackedHelloFlashLightSampleIT {
  
  @Rule
  public final TestResources resources = new TestResources();
  
  public final MavenRuntime mavenRuntime;
  
  public JackedHelloFlashLightSampleIT(MavenRuntimeBuilder builder) throws Exception {
    this.mavenRuntime = builder.build();
  }
  
  @Test
  public void buildDeployAndRun() throws Exception {
    File basedir = resources.getBasedir( "helloflashlight_jack" );
    MavenExecutionResult result = mavenRuntime
          .forProject(basedir)
          .execute( "clean", PluginInfo.getQualifiedGoal( "undeploy" ), 
              "install", PluginInfo.getQualifiedGoal( "deploy" ),
              PluginInfo.getQualifiedGoal( "run" ) );
    
    result.assertErrorFreeLog();
    result.assertLogText( "Successfully installed" );
    result.assertLogText( "Attempting to start com.simpligility.android.helloflashlight/com.simpligility.android.helloflashlight.HelloFlashlight" );
  }

}