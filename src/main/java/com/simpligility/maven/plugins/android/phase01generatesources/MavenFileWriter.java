package com.simpligility.maven.plugins.android.phase01generatesources;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.databinding.tool.writer.JavaFileWriter;
import com.google.repacked.apache.commons.io.IOUtils;
import org.apache.maven.plugin.logging.Log;

/**
 * @author kedzie
 */
public class MavenFileWriter extends JavaFileWriter
{

   private final String outputBase;
   private final Log log;

   /**
    * @param log  maven logger
    * @param outputBase path prepended to output file path
    */
   public MavenFileWriter( Log log, String outputBase )
   {
      this.log = log;
      this.outputBase = outputBase;
   }

   /**
    * write java class
    * @param clazz   class name
    * @param src     java source
    */
   public void writeToFile( String clazz, String src )
   {
      File f = new File( this.outputBase + "/" + clazz.replace( '.', '/' ) + ".java" );
      log.debug( "Asked to write to " + clazz + ". outputting to:" + f.getAbsolutePath() );
      f.getParentFile().mkdirs();
      FileOutputStream fos = null;
      try
      {
         fos = new FileOutputStream( f );
         IOUtils.write( src, fos );
      }
      catch ( IOException ex )
      {
         log.error( "cannot write file " + f.getAbsolutePath(), ex );
      }
      finally
      {
         IOUtils.closeQuietly( fos );
      }
   }
}