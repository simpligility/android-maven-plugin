package com.jayway.maven.plugins.android.configuration;

import static java.lang.String.format;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Regex-based VersionElementParser implementation.
 *
 * @author Wang Xuerui <idontknw.wang@gmail.com>
 *
 */
public class RegexVersionElementParser implements VersionElementParser
{

    private Pattern namingPattern;

    public RegexVersionElementParser( String pattern )
    {
        namingPattern = Pattern.compile( pattern );
    }

    @Override
    public int[] parseVersionElements( final String versionName ) throws MojoExecutionException
    {
        final Matcher matcher = namingPattern.matcher( versionName );
        if ( ! matcher.find() )
        {
            throw new MojoExecutionException( format(
                    "The version naming pattern failed to match version name: %s against %s",
                    namingPattern, versionName ) );
        }

        int elementCount = matcher.groupCount();
        int[] result = new int[elementCount];

        for ( int i = 0; i < elementCount; i++ )
        {
            // Capturing groups start at index 1
            try
            {
                result[i] = Integer.valueOf( matcher.group( i + 1 ) );
            }
            catch ( NumberFormatException ignored )
            {
                // Either the group is not present, or cannot be cast to integer.
                result[i] = 0;
            }
        }

        return result;
    }
}
