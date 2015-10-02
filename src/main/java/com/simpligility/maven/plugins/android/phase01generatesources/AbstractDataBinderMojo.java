package com.simpligility.maven.plugins.android.phase01generatesources;

import javax.tools.Diagnostic;
import java.io.File;
import java.util.Arrays;

import android.databinding.tool.LayoutXmlProcessor;
import android.databinding.tool.util.L;
import com.simpligility.maven.plugins.android.AbstractAndroidMojo;
import com.simpligility.maven.plugins.android.common.AndroidExtension;
import com.simpligility.maven.plugins.android.config.ConfigHandler;
import com.simpligility.maven.plugins.android.config.ConfigPojo;
import com.simpligility.maven.plugins.android.config.PullParameter;
import com.simpligility.maven.plugins.android.configuration.DataBinding;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * Base class for Data Binding generation mojos
 * @author kedzie
 */
public abstract class AbstractDataBinderMojo extends AbstractAndroidMojo
{
   /**
    * Embedded configuration of this mojo.
    */
   @Parameter
   @ConfigPojo( prefix = "dataBinding" )
   protected DataBinding dataBinding;

   @Parameter( property = "android.dataBinding.skip" )
   @PullParameter( defaultValue = "true" )
   protected Boolean dataBindingSkip;

   @Parameter( property = "android.dataBinding.printEncodedErrors" )
   @PullParameter( defaultValue = "false" )
   protected Boolean dataBindingPrintEncodedErrors;

   @Parameter( property = "android.dataBinding.verbose" )
   @PullParameter( defaultValue = "false" )
   protected Boolean dataBindingVerbose;

   @Parameter( property = "android.dataBinding.bindingInfoDirectory" )
   @PullParameter( defaultValue = "${project.build.directory}/data-binding-info" )
   protected File dataBindingBindingInfoDirectory;

   @Parameter( property = "android.dataBinding.layoutInfoDirectory" )
   @PullParameter( defaultValue = "${project.build.directory}/layout-info" )
   protected File dataBindingLayoutInfoDirectory;

   @Parameter( property = "android.dataBinding.resourceDirectory" )
   @PullParameter( defaultValue = "${project.build.directory}/databinding-res" )
   protected File dataBindingResourceDirectory;

   protected File dataBindingGeneratedClassListFile;

   @Component
   protected BuildContext buildContext;

   protected LayoutXmlProcessor processor;

   protected LayoutXmlProcessor getProcessor() throws MojoExecutionException
   {
      if ( processor == null )
      {
         processor = new LayoutXmlProcessor(
               extractPackageNameFromAndroidManifest( androidManifestFile ),   //applicationPackage
               Arrays.asList( new File[] { dataBindingResourceDirectory } ), //resources
               new MavenFileWriter( getLog(), dataBindingBindingInfoDirectory.getAbsolutePath() ), //fileWriter
               getAndroidSdk().getSdkMajorVersion(),      //minSdk
               project.getPackaging().equals( AndroidExtension.AAR ) ); //isLibrary
      }
      return processor;
   }

   protected boolean isLibrary()
   {
      return project.getPackaging().equals( AndroidExtension.AAR );
   }

   @Override
   public void execute() throws MojoExecutionException, MojoFailureException
   {
      L.setDebugLog( true );
      L.setClient( new MavenLoggerClient( getLog() ) );
      new ConfigHandler( this, this.session, this.execution ).parseConfiguration();
      if ( isLibrary() )
      {
         dataBindingGeneratedClassListFile = new File( dataBindingLayoutInfoDirectory, "_generated.txt" );
      }
   }
}

class MavenLoggerClient implements L.Client
{
   private Log logger;

   MavenLoggerClient( Log logger )
   {
      this.logger = logger;
   }

   @Override
   public void printMessage( Diagnostic.Kind kind, String s )
   {
      switch ( kind )
      {
      case ERROR:
         logger.error( s );
         break;
      case WARNING:
         logger.warn( s );
         break;
      case OTHER:
         logger.debug( s );
         break;
      case NOTE:
      default:
         logger.info ( s );
      }
   }
}
