package com.jayway.maven.plugins.android.configuration;

import static org.junit.Assert.assertArrayEquals;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;

/**
 * @author Wang Xuerui <idontknw.wang@gmail.com>
 */
public class SimpleVersionElementParserTest
{

    @Test
    public void simple() throws MojoExecutionException
    {
        assertArrayEquals( new int[] { 2, 14, 748, 3647 }, new SimpleVersionElementParser().parseVersionElements( "2.14.748.3647" ) );
        assertArrayEquals( new int[] { 2147, 483, 64, 7 }, new SimpleVersionElementParser().parseVersionElements( "2147.483.64.7" ) );
        assertArrayEquals( new int[] { 2, 1, 4, 7, 4, 8, 3, 6, 4, 7 }, new SimpleVersionElementParser().parseVersionElements( "2.1.4.7.4.8.3.6.4.7" ) );
    }

    @Test
    public void mixed() throws MojoExecutionException
    {
        assertArrayEquals( new int[] { 4, 1, 16, 8, 1946 }, new SimpleVersionElementParser().parseVersionElements( "4.1.16.8-SNAPSHOT.1946" ) );
        assertArrayEquals( new int[] { 1, 2, 15, 8, 1946 }, new SimpleVersionElementParser().parseVersionElements( "1.2.15.8-SNAPSHOT.1946" ) );
        assertArrayEquals( new int[] { 2, 1, 6, 1246 }, new SimpleVersionElementParser().parseVersionElements( "2.1.6-SNAPSHOT.1246" ) );
    }
}
