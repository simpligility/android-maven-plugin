package com.simpligility.maven.plugins.android.phase04processclasses;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import com.google.repacked.apache.commons.io.IOUtils;
import com.simpligility.maven.plugins.android.phase01generatesources.AbstractDataBinderMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Exludes generated data binding classes in library projects.
 * They get regenerated in the apk.  We must avoid duplicate dex errors.
 * @author kedzie
 */
@Mojo(
      name = "databinding-exclude-dependencies",
      defaultPhase = LifecyclePhase.PROCESS_CLASSES
)
public class DataBindingExcludeGeneratedMojo extends AbstractDataBinderMojo
{
   private static final String EXCLUDE_PATTERN = "android/databinding/layouts/*.*";

   @Override
   public void execute() throws MojoExecutionException, MojoFailureException
   {
      super.execute();
      if ( dataBindingSkip )
      {
         return;
      }
      String appPackage = extractPackageNameFromAndroidManifest( androidManifestFile ).replace( '.', '/' );
      String infoClass = getProcessor().getInfoClassFullName().replace( '.', '/' ) + ".class";
      exclude( infoClass );
      if ( isLibrary() )
      {
         getLog().info( "Excluding generated data binding files in library" );
         exclude( appPackage + "/BR.class" );
         for ( String clazz : readGeneratedClasses() )
         {
            exclude( clazz.replace( '.', '/' ) + ".class" );
         }
      }
   }

   private void exclude( String filename )
   {
      File classFile = new File( projectOutputDirectory, filename );
      getLog().info( "Excluding class " + classFile.getAbsolutePath() );
      classFile.delete();
   }

   private List<String> readGeneratedClasses() throws MojoExecutionException
   {
      FileInputStream fis = null;
      try
      {
         fis = new FileInputStream( dataBindingGeneratedClassListFile );
         return IOUtils.readLines( fis );
      }
      catch ( FileNotFoundException e )
      {
         throw new MojoExecutionException(  "Unable to read generated class list from "
               + dataBindingGeneratedClassListFile.getAbsolutePath(), e );
      }
      catch ( IOException e )
      {
         throw new MojoExecutionException( "Unexpected exception while reading "
               + dataBindingGeneratedClassListFile.getAbsolutePath(), e );
      }
      finally
      {
         IOUtils.closeQuietly( fis );
      }
   }
}
