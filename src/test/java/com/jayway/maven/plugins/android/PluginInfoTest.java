package com.jayway.maven.plugins.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class PluginInfoTest {

  @Test
  public void confirmPropertiesRead() 
  {
    assertNotNull( PluginInfo.getProperties() );
  }
  
  @Test
  public void confirmGroupId()
  {
    assertEquals( "com.jayway.maven.plugins.android.generation2", PluginInfo.getGroupId() );
  }

  @Test
  public void confirmArtifactId()
  {
    assertEquals( "android-maven-plugin", PluginInfo.getArtifactId() );
  }

  @Test
  public void confirmVersion()
  {
    assertNotNull( PluginInfo.getVersion() );
  }

  @Test
  public void confirmGav()
  {
    assertNotNull( PluginInfo.getGAV() );
  }
}
