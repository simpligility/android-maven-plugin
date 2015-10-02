/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.simpligility.maven.plugins.android.phase01generatesources;

import java.io.File;

import io.takari.maven.testing.TestResources;
import io.takari.maven.testing.executor.MavenExecutionResult;
import io.takari.maven.testing.executor.MavenRuntime;
import io.takari.maven.testing.executor.MavenVersions;
import io.takari.maven.testing.executor.junit.MavenJUnitTestRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test Data Binding project
 * @author kedzie
 */
@RunWith( MavenJUnitTestRunner.class )
@MavenVersions( { "3.2.5"} )
public class DataBindingIT {

   @Rule
   public final TestResources resources = new TestResources();

   public final MavenRuntime mavenRuntime;

   public DataBindingIT( MavenRuntime.MavenRuntimeBuilder builder ) throws Exception
   {
      this.mavenRuntime = builder.build();
   }

   @Test
   public void buildDeployAndRun() throws Exception
   {
      File basedir = resources.getBasedir( "databinding" );
      MavenExecutionResult result = mavenRuntime
            .forProject(basedir)
            .execute( "clean", "install" );

      result.assertErrorFreeLog();
   }
}
