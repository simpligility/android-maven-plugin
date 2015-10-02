package com.simpligility.maven.plugins.android.phase01generatesources;

import android.databinding.tool.LayoutXmlProcessor;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Processes layouts to remove data binding
 *
 * @author kedzie
 */
@Mojo(
      name = "databinding-process-layouts",
      defaultPhase = LifecyclePhase.GENERATE_SOURCES,
      requiresDependencyResolution = ResolutionScope.COMPILE
)
public class DataBindingProcessLayoutsMojo extends AbstractDataBinderMojo
{

   @Override
   public void execute() throws MojoExecutionException, MojoFailureException
   {
      super.execute();
      try
      {
         if ( resourceDirectory.exists() )
         {
            getLog().debug( "Copying resources" );
            org.apache.commons.io.FileUtils.copyDirectory( resourceDirectory, dataBindingResourceDirectory );
         }
         if ( dataBindingSkip )
         {
            return;
         }
         //TODO add annotation processor automagically
         //      Dependency compilerDep = new Dependency();
         //      compilerDep.setGroupId( "com.android.databinding" );
         //      compilerDep.setArtifactId( "compiler" );
         //      compilerDep.setVersion( "1.0-rc2" );
         //      compilerDep.setType( "jar" );
         //      compilerDep.setScope( "provided" );
         //      getLog().info( "Adding Data Binding compiler dependency: " + compilerDep );
         //      project.getDependencies().add( compilerDep );

         for ( File layout : LayoutXmlProcessor.getLayoutFiles( Arrays.asList( dataBindingResourceDirectory ) ) )
         {
            getLog().debug( "Added from file comment to layout: " + layout.getName() );
            String r = resourceDirectory.getAbsolutePath();
            String dr = dataBindingResourceDirectory.getAbsolutePath();
            String postFix = layout.getAbsolutePath().substring( dr.length() );
            String original = r + postFix;
            String comment = String.format( "<!-- From: file:%s -->", original );
            com.google.common.io.Files.append( comment, layout, Charset.defaultCharset() );
         }
         getLog().info( "Data Binder Processing Layouts" );
         getProcessor().processResources( getAndroidSdk().getSdkMajorVersion() );
         buildContext.refresh( resourceDirectory );
         buildContext.refresh( dataBindingResourceDirectory );
      }
      catch ( ParserConfigurationException e )
      {
         throw new MojoExecutionException( "failed", e );
      }
      catch ( SAXException e )
      {
         throw new MojoExecutionException( "failed", e );
      }
      catch ( XPathExpressionException e )
      {
         throw new MojoExecutionException( "failed", e );
      }
      catch ( IOException e )
      {
         throw new MojoExecutionException( "failed", e );
      }

      try
      {
         getLog().info( "Writing Layout Info to: " + dataBindingLayoutInfoDirectory );
         getProcessor().writeLayoutInfoFiles( dataBindingLayoutInfoDirectory );
      }
      catch ( JAXBException e )
      {
         throw new MojoExecutionException( "failed", e );
      }
   }
}
