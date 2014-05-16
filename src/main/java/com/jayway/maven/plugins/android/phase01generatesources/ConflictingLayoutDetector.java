package com.jayway.maven.plugins.android.phase01generatesources;

import org.apache.maven.plugin.logging.Log;

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
    private final Log log;
    private Map<String, Collection<String>> map = new HashMap<String, Collection<String>>();

    ConflictingLayoutDetector( Log log )
    {
        this.log = log;
    }

    public void addLayoutFiles( String packageName, String[] layoutFiles )
    {
        map.put( packageName, Arrays.asList( layoutFiles ) );
        log.debug( "addLayoutFiles pkgName=" + packageName + " layoutFiles=" + Arrays.asList( layoutFiles ) );
    }

    public Collection<ConflictingLayout> getConflictingLayouts()
    {
        final Map<String, ConflictingLayout> result = new TreeMap<String, ConflictingLayout>();
        log.debug( "getConflictingLayouts : " + map );
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
                log.debug( "" );
                log.debug( "entryA=" + entryA + " entryA#values=" + map.get( entryA ) );
                log.debug( "entryB=" + entryB + " entryB#values=" + map.get( entryB ) );
                log.debug( "Retained layout files : " + tmp );

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
