package com.jayway.maven.plugins.android.configuration;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Interface for parsing version names into version elements.
 *
 * @author Wang Xuerui <idontknw.wang@gmail.com>
 *
 */
public interface VersionElementParser
{
    int[] parseVersionElements( final String versionName ) throws MojoExecutionException;
}
