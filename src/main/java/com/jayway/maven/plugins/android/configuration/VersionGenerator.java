
package com.jayway.maven.plugins.android.configuration;

import org.apache.maven.plugin.MojoExecutionException;

import static java.lang.String.format;

/**
 * @author Pappy STĂNESCU <a href="mailto:pappy.stanescu&#64;gmail.com">&lt;pappy.stanescu&#64;gmail.com&gt;</a>
 * @author Wang Xuerui <idontknw.wang@gmail.com>
 */
public class VersionGenerator
{

    private final int elementsCount;

    private final int[] multipliers;

    private final VersionElementParser elementParser;

    public VersionGenerator()
    {
        this( "4,3,3" );
    }

    public VersionGenerator( String versionDigits )
    {
        final String[] digits = versionDigits.split( "[,;]" );

        this.elementsCount = digits.length;
        this.multipliers = new int[this.elementsCount];
        this.elementParser = new SimpleVersionElementParser();

        int total = 0;

        for ( int k = 0; k < this.elementsCount; k++ )
        {
            int value = Integer.valueOf( digits[k].trim() );

            total += value;

            this.multipliers[k] = (int) Math.pow( 10, value );
        }

        if ( total < 1 || total > 10 )
        {
            throw new IllegalArgumentException( format( "Invalid number of digits, got %d", total ) );
        }
    }

    public int generate( String versionName ) throws MojoExecutionException
    {
        final int[] versionElements = elementParser.parseVersionElements( versionName );

        long versionCode = 0;

        for ( int k = 0; k < this.elementsCount; k++ )
        {
            versionCode *= this.multipliers[k];

            if ( k < versionElements.length )
            {
                final int elementValue = versionElements[k];

                if ( elementValue >= this.multipliers[k] )
                {
                    throw new MojoExecutionException( format( "The version element is too large: %d, max %d",
                        elementValue, this.multipliers[k] - 1 ) );
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
