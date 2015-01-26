
package com.jayway.maven.plugins.android.configuration;

import static java.lang.String.format;

import org.apache.maven.plugin.MojoExecutionException;

public class VersionGenerator
{

    private int elementsCount;

    private int multiplier;

    public VersionGenerator( int elementsCount, int elementDigits )
    {
        this.elementsCount = elementsCount;
        this.multiplier = (int) Math.pow( 10, elementDigits );
    }

    public int generate( String versionName ) throws MojoExecutionException
    {
        String[] versionNameElements = versionName.replaceAll( "[^0-9.]", "" ).split( "\\." );

        long versionCode = 0;

        for ( int i = 0; i < elementsCount; i++ )
        {
            versionCode *= multiplier;

            if ( i < versionNameElements.length )
            {
                String versionElement = versionNameElements[i];
                int elementValue = Integer.valueOf( versionElement );

                if ( i > 0 && elementValue >= multiplier )
                {
                    throw new MojoExecutionException( format( "The version element is too large: %d", elementValue ) );
                }

                versionCode += elementValue;
            }
        }

        if ( versionCode > Integer.MAX_VALUE )
        {
            throw new MojoExecutionException( format( "The version code is too large: %d", versionCode ) );
        }

        return (int) versionCode;
    }
}
