package com.simpligility.maven.plugins.android;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>Parses a list of key/value pairs separated by a space in to a map.</p>
 *
 * <p>Example input:</p>
 * <pre>
 *     list[0] = "firstKey firstValue"
 *     list[1] = "secondKey 'second value with space and single quote escape'
 * </pre>
 *
 * <p>Example output:</p>
 * <pre>
 *     map["firstKey"] = "firstValue"
 *     map["secondKey"] = "'second value with space and single quote escape'"
 * </pre>
 */
public class InstrumentationArgumentParser
{
    private static final String SEPARATOR = " ";

    /**
     * Parses the given {@code flatArgs} into a map of key/value pairs.
     *
     * @param flatArgs the flat representation of arguments, might be null
     * @return a map representation of the given key/value pair list, might be empty
     * @throws IllegalArgumentException when the given list contains unparseable entries
     */
    public static Map<String, String> parse( final List<String> flatArgs )
    {
        if ( flatArgs == null )
        {
            return Collections.EMPTY_MAP;
        }

        final Map<String, String> mappedArgs = new HashMap<String, String>();

        for ( final String flatArg : flatArgs )
        {
            final AbstractMap.SimpleEntry<String, String> keyValuePair = parseKeyValuePair( flatArg );
            mappedArgs.put( keyValuePair.getKey(), keyValuePair.getValue() );
        }

        return mappedArgs;
    }

    private static AbstractMap.SimpleEntry<String, String> parseKeyValuePair( final String arg )
    {
        final List<String> keyValueSplit = Lists.newArrayList( Splitter.on( SEPARATOR ).limit( 2 ).split( arg ) );

        if ( keyValueSplit.size() == 1 )
        {
            throw new IllegalArgumentException( "Could not separate \"" + arg + "\" by a whitespace into two parts" );
        }

        return new AbstractMap.SimpleEntry<String, String>( keyValueSplit.get( 0 ), keyValueSplit.get( 1 ) );
    }
}
