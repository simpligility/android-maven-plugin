package com.jayway.maven.plugins.android.standalonemojos;

public class CompatibleScreen
{

    private String screenSize, screenDensity;

    public String getScreenSize()
    {
        return screenSize;
    }

    public void setScreenSize( String screenSize )
    {
        this.screenSize = screenSize;
    }

    public String getScreenDensity()
    {
        return screenDensity;
    }

    public void setScreenDensity( String screenDensity )
    {
        this.screenDensity = screenDensity;
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( obj instanceof CompatibleScreen )
        {
            CompatibleScreen that = ( CompatibleScreen ) obj;
            return this.screenDensity.equals( that.screenDensity ) && this.screenSize.equals( that.screenSize );
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return ( screenDensity + screenSize ).hashCode();
    }

    @Override
    public String toString()
    {
        return screenSize + ":" + screenDensity;
    }
}
