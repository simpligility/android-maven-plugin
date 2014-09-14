package com.jayway.maven.plugins.android;

import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;

/**
 * PluginInfo reads plugin.properties which contains filtered 
 * values from the build like the GAV coordinates of the plugin itself
 * and provides convenience methods for accessing these properties 
 * and related things about the plugin.
 * 
 * @author Manfred Moser
 */
public class PluginInfo 
{

  static 
  {
    loadProperties();
  }

  private static final String COLON = ":";
  private static Properties prop;
  private static String groupId;
  private static String artifactId;
  private static String version;
  
  private static void loadProperties() 
  {
    prop = new Properties();
    InputStream in = PluginInfo.class.getResourceAsStream( "plugin.properties" );
    try 
    {
      prop.load( in );
      groupId = prop.getProperty( "groupId" );
      artifactId = prop.getProperty( "artifactId" );
      version = prop.getProperty( "version" );
      in.close();
    } 
    catch ( IOException e ) 
    {
      e.printStackTrace();
    }
  }
  
  public static String getGAV()
  {
    StringBuilder builder = new StringBuilder()
      .append( groupId )
      .append( COLON )
      .append( artifactId )
      .append( COLON )
      .append( version );
      return builder.toString();
  }
  
  public static Properties getProperties()
  {
    return prop;
  }
  
  public static String getGroupId() 
  {
    return groupId;
  }
  
  public static String getArtifactId()
  {
    return artifactId;
  }
  
  public static String getVersion()
  {
    return version;
  }
}