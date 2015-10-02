package com.simpligility.maven.plugins.android.phase01generatesources;


import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Generates DataBindingInfo class
 *
 * @author kedzie
 */
@Mojo(
      name = "databinding-export-info",
      defaultPhase = LifecyclePhase.GENERATE_SOURCES,
      requiresDependencyResolution = ResolutionScope.COMPILE
)
public class DataBindingExportInfoMojo extends AbstractDataBinderMojo
{
   @Override
   public void execute() throws MojoExecutionException, MojoFailureException
   {
      super.execute();
      if ( dataBindingSkip )
      {
         return;
      }
      getProcessor().writeInfoClass(
            getAndroidSdk().getSdkPath(), //classString
            dataBindingLayoutInfoDirectory,       //layoutInfoPath
            dataBindingGeneratedClassListFile, //exportClassListTOPath
            getLog().isDebugEnabled() || dataBindingVerbose,
            dataBindingPrintEncodedErrors );
      buildContext.refresh( dataBindingBindingInfoDirectory );
      getLog().info( "Adding Data Binding Info gen folder to compile classpath: " + dataBindingBindingInfoDirectory );
      project.addCompileSourceRoot( dataBindingBindingInfoDirectory.getPath() );
   }
}
