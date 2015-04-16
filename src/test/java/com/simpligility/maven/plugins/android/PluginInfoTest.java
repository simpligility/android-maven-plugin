package com.simpligility.maven.plugins.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.simpligility.maven.plugins.android.PluginInfo;

public class PluginInfoTest {
  
  @Test
  public void confirmGroupId()
  {
    assertEquals( "com.simpligility.maven.plugins", PluginInfo.getGroupId() );
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
    assertTrue( PluginInfo.getGAV()
        .startsWith( "com.simpligility.maven.plugins:android-maven-plugin:" ) );
  }
}
