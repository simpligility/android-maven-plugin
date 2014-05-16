package com.jayway.maven.plugins.android.phase01generatesources;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Looks for duplicate layout files across Android resource packages.
 */
final class ConflictingLayoutDetector
{
    private Map<String, Collection<String>> map = new HashMap<String, Collection<String>>();

    public void addLayoutFiles( String packageName, String[] layoutFiles )
    {
        map.put( packageName, Arrays.asList( layoutFiles ) );
    }

    public Collection<ConflictingLayout> getConflictingLayouts()
    {
        final Map<String, ConflictingLayout> result = new TreeMap<String, ConflictingLayout>();
        for ( final String entryA : map.keySet() )
        {
            for ( final String entryB : map.keySet() )
            {
                if ( entryA.equals( entryB ) )
                {
                    continue;
                }

                // Find any layout files that are in both packages.
                final Set<String> tmp = new HashSet<String>();
                tmp.addAll( map.get( entryA ) );
                tmp.retainAll( map.get( entryB ) );

                for ( final String layoutFile : tmp )
                {
                    if ( !result.containsKey( layoutFile ) )
                    {
                        result.put( layoutFile, new ConflictingLayout( layoutFile ) );
                    }
                    final ConflictingLayout layout = result.get( layoutFile );
                    layout.addPackageName( entryA );
                    layout.addPackageName( entryB );
                }
            }
        }

        return result.values();
    }
}
