package com.jayway.maven.plugins.android.common;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.apache.maven.project.MavenProject;
import org.apache.maven.model.Plugin;

/**
 * Resolves the aar and apklib dependencies for an Artifact.
 *
 * @author Benoit Billington
 */
public final class PomConfigurationHelper
{
    private static final String INCLUDE_LIBS_JARS_AAR_CONFIG_ELEMENT = "includeLibsJarsForAarlib";
    private static final String INCLUDE_LIBS_JARS_APKLIB_CONFIG_ELEMENT = "includeLibsJarsForApklib";
    private static final String AMP = "android-maven-plugin";

    private boolean includeLibsJarsForApklib = false;

    private boolean includeLibsJarsForAar = false;
    
    public PomConfigurationHelper()
    {
    }

    public void retrievePluginConfiguration( MavenProject project )
    {
        for ( Plugin plugin : project.getBuild().getPlugins() )
        {
            if ( plugin.getArtifactId().equals( AMP ) )
            {
                Xpp3Dom configuration = getMojoConfiguration( plugin );
                if ( configuration != null && configuration.getChild( INCLUDE_LIBS_JARS_AAR_CONFIG_ELEMENT ) != null )
                {
                    includeLibsJarsForAar = Boolean.valueOf(
                        configuration.getChild( INCLUDE_LIBS_JARS_AAR_CONFIG_ELEMENT ).getValue() );
                }
                else
                {
                    includeLibsJarsForAar = false;
                }
                if ( configuration != null 
                    && configuration.getChild( INCLUDE_LIBS_JARS_APKLIB_CONFIG_ELEMENT ) != null )
                {
                    includeLibsJarsForApklib = Boolean.valueOf( 
                        configuration.getChild( INCLUDE_LIBS_JARS_APKLIB_CONFIG_ELEMENT ).getValue() );
                }
                else
                {
                    includeLibsJarsForApklib = false;
                }
            }
        }
    }
    
    private Xpp3Dom getMojoConfiguration( Plugin plugin )
    {
        //
        // We need to look in the configuration element, and then look for configuration elements
        // within the executions.
        //
        Xpp3Dom configuration = ( Xpp3Dom ) plugin.getConfiguration();
        if ( configuration == null )
        {
            if ( !plugin.getExecutions().isEmpty() )
            {
                configuration = ( Xpp3Dom ) plugin.getExecutions().get( 0 ).getConfiguration();
            }
        }
        return configuration;
    }

    public boolean includeLibsJarsForApklib()
    {
        return includeLibsJarsForApklib;
    }

    public boolean includeLibsJarsForAar()
    {
        return includeLibsJarsForAar;
    }
}
