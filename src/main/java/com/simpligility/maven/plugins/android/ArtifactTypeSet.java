package com.simpligility.maven.plugins.android;

import java.util.Set;

public class ArtifactTypeSet
{

    private Set< String > includes;
    private Set< String > excludes;

    public Set< String > getIncludes() 
    {
        return includes;
    }
    
    public Set< String > getExcludes() 
    {
        return excludes;
    }

}
