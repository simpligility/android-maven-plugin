package com.simpligility.maven.plugins.android;

import com.google.common.collect.Lists;
import com.simpligility.maven.plugins.android.InstrumentationArgumentParser;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class InstrumentationArgumentParserTest
{
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Test
    public void should_return_an_empty_map_for_null_list()
    {
        assertTrue( InstrumentationArgumentParser.parse( null ).isEmpty() );
    }

    @Test
    public void two_flat_args_should_be_parsed_into_two_key_value_pairs()
    {
        final List<String> flatArgs = Lists.newArrayList( "key1 value1", "key2 value2" );
        final Map<String,String> parsedArgs = InstrumentationArgumentParser.parse( flatArgs );

        assertThat( parsedArgs.get( "key1" ), is( "value1" ) );
        assertThat( parsedArgs.get( "key2" ), is( "value2" ) );
    }

    @Test
    public void should_parse_values_with_space_character()
    {
        final List<String> flatArgs = Lists.newArrayList( "key1 'value with spaces'" );
        final Map<String,String> parsedArgs = InstrumentationArgumentParser.parse( flatArgs );

        assertThat( parsedArgs.get( "key1" ), is( "'value with spaces'" ) );
    }

    @Test
    public void missing_value_should_throw_IllegalArgumentException()
    {
        expectedException.expect( IllegalArgumentException.class );
        expectedException.expectMessage( is( "Could not separate \"key1\" by a whitespace into two parts" ) );

        final List<String> flatArgs = Lists.newArrayList( "key1" );
        InstrumentationArgumentParser.parse( flatArgs );
    }

    @Test
    public void empty_pair_should_throw_IllegalArgumentException()
    {
        expectedException.expect( IllegalArgumentException.class );
        expectedException.expectMessage( is( "Could not separate \"\" by a whitespace into two parts" ) );

        final List<String> flatArgs = Lists.newArrayList( "" );
        InstrumentationArgumentParser.parse( flatArgs );
    }
}
