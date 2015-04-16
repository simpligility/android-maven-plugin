package com.simpligility.maven.plugins.android.phase01generatesources;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * Represents a layout that is duplicating among more than one Android package.
 */
final class ConflictingLayout
{
    private final String layoutFileName;
    private final Set<String> packageNames = new TreeSet<String>();

    ConflictingLayout( String layoutFileName )
    {
        this.layoutFileName = layoutFileName;
    }

    public String getLayoutFileName()
    {
        return layoutFileName;
    }

    public void addPackageName( String packageName )
    {
        packageNames.add( packageName );
    }

    public Set<String> getPackageNames()
    {
        return Collections.unmodifiableSet( packageNames );
    }
}
