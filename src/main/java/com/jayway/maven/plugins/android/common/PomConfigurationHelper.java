package com.jayway.maven.plugins.android.common;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.project.MavenProject;
import org.apache.maven.model.Plugin;

import com.jayway.maven.plugins.android.PluginInfo;

/**
 * A helper class to access plugin configuration from the pom.
 *
 * @author Benoit Billington
 * @author Manfred Moser
 */
public final class PomConfigurationHelper
{
    public static String getPluginConfigParameter ( MavenProject project, String parameter, String defaultValue )
    {
        String value = null;
        for ( Plugin plugin : project.getBuild().getPlugins() )
        {
            if ( plugin.getArtifactId().equals( PluginInfo.getArtifactId() ) )
            {
                Xpp3Dom configuration = getMojoConfiguration( plugin );
                if ( configuration != null && configuration.getChild( parameter ) != null )
                {
                  value = configuration.getChild( parameter ).getValue() ;
                }
            }
        }
        // if we got nothing, fall back to the default value
        return ( StringUtils.isEmpty( value ) ) ? defaultValue : value;
    }

    public static boolean getPluginConfigParameter ( MavenProject project, String parameter, boolean defaultValue )
    {
        String value = getPluginConfigParameter( project, parameter, Boolean.toString( defaultValue ) );
        return Boolean.valueOf( value );
    }
    
    private static Xpp3Dom getMojoConfiguration( Plugin plugin )
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
}
