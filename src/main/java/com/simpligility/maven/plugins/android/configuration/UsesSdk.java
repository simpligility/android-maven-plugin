package com.simpligility.maven.plugins.android.configuration;

/**
 * Abstraction for uses-sdk tag in the Android manifest.
 *
 * @author Francisco Javier Fernandez <fjfernandez@tuenti.com>
 */

public class UsesSdk
{
    private String minSdkVersion;
    private String maxSdkVersion;
    private String targetSdkVersion;

    private static final int PRIME_NUMBER = 31;

    public String getMinSdkVersion()
    {
        return minSdkVersion;
    }

    public String getMaxSdkVersion()
    {
        return maxSdkVersion;
    }

    public String getTargetSdkVersion()
    {
        return targetSdkVersion;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        UsesSdk usesSdk = ( UsesSdk ) o;

        if ( maxSdkVersion != null
                ? !maxSdkVersion.equals( usesSdk.maxSdkVersion )
                : usesSdk.maxSdkVersion != null )
        {
            return false;
        }
        if ( minSdkVersion != null
                ? !minSdkVersion.equals( usesSdk.minSdkVersion )
                : usesSdk.minSdkVersion != null )
        {
            return false;
        }
        if ( targetSdkVersion != null
                ? !targetSdkVersion.equals( usesSdk.targetSdkVersion )
                : usesSdk.targetSdkVersion != null )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = minSdkVersion != null ? minSdkVersion.hashCode() : 0;
        result = PRIME_NUMBER * result + ( maxSdkVersion != null ? maxSdkVersion.hashCode() : 0 );
        result = PRIME_NUMBER * result + ( targetSdkVersion != null ? targetSdkVersion.hashCode() : 0 );
        return result;
    }

    @Override
    public String toString()
    {
        return minSdkVersion + " : " + maxSdkVersion + " : " + targetSdkVersion;
    }
}
