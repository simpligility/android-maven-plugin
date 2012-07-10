package com.jayway.maven.plugins.android.standalonemojos;

/**
 *
 */
public class SupportsScreens
{

    private String resizeable;
    private String smallScreens, normalScreens, largeScreens, xlargeScreens;
    private String anyDensity;
    private String requiresSmallestWidthDp;
    private String compatibleWidthLimitDp;
    private String largestWidthLimitDp;

    public String getResizeable()
    {
        return resizeable;
    }

    public void setResizeable( String resizable )
    {
        this.resizeable = resizable;
    }

    public String getSmallScreens()
    {
        return smallScreens;
    }

    public void setSmallScreens( String smallScreens )
    {
        this.smallScreens = smallScreens;
    }

    public String getNormalScreens()
    {
        return normalScreens;
    }

    public void setNormalScreens( String normalScreens )
    {
        this.normalScreens = normalScreens;
    }

    public String getLargeScreens()
    {
        return largeScreens;
    }

    public void setLargeScreens( String largeScreens )
    {
        this.largeScreens = largeScreens;
    }

    public String getXlargeScreens()
    {
        return xlargeScreens;
    }

    public void setXlargeScreens( String xlargeScreens )
    {
        this.xlargeScreens = xlargeScreens;
    }

    public String getAnyDensity()
    {
        return anyDensity;
    }

    public void setAnyDensity( String anyDensity )
    {
        this.anyDensity = anyDensity;
    }

    public String getRequiresSmallestWidthDp()
    {
        return requiresSmallestWidthDp;
    }

    public void setRequiresSmallestWidthDp( String requiresSmallestWidthDp )
    {
        this.requiresSmallestWidthDp = requiresSmallestWidthDp;
    }

    public String getCompatibleWidthLimitDp()
    {
        return compatibleWidthLimitDp;
    }

    public void setCompatibleWidthLimitDp( String compatibleWidthLimitDp )
    {
        this.compatibleWidthLimitDp = compatibleWidthLimitDp;
    }

    public String getLargestWidthLimitDp()
    {
        return largestWidthLimitDp;
    }

    public void setLargestWidthLimitDp( String largestWidthLimitDp )
    {
        this.largestWidthLimitDp = largestWidthLimitDp;
    }

}
