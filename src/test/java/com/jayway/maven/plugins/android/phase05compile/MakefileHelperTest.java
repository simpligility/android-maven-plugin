package com.jayway.maven.plugins.android.phase05compile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

/**
 * @author Johan Lindquist
 * @author Andi Everitt
 */
public class MakefileHelperTest 
{
    
    private static final String FOLDER_FOO = "foo";
    private static final String FOLDER_FOOBAR = FOLDER_FOO + File.separator + "bar";
    private static final String FOLDER_FOOBARBAZ = FOLDER_FOOBAR + File.separator + "baz";
    private static final String QUXFILE = "qux.file";
    
    private static final String FOOBARBAZ_QUXFILE = FOLDER_FOOBARBAZ + File.separator + QUXFILE;
    private static final String FOOBAR_QUXFILE = FOLDER_FOOBAR + File.separator + QUXFILE;

    private static final String FOLDER_FRED = "fred";
    private static final String FOLDER_FREDBARNY = FOLDER_FRED + File.separator + "barny";

    @Test
    public void testSplitSimpleFolder() 
    {
        // Split '/foo/bar/baz'
        String[] expected = {"foo", "bar", "baz"};
        assertEquals( Arrays.asList( expected ), 
                      MakefileHelper.splitPath( new File( File.separator, FOLDER_FOOBARBAZ ) ) );
    }
    
    @Test
    public void testSplitAbsoluteFolder() throws IOException
    {
        // Split '/foo/bar/baz'
        List<String> expected = getSplit( new File ( File.separator ).getCanonicalFile() );
        expected.add( "foo" );
        expected.add( "bar" );
        expected.add( "baz" );
        assertEquals( expected, 
                      MakefileHelper.splitPath( new File( File.separator, FOLDER_FOOBARBAZ ).getAbsoluteFile() ) );
    }
    
    @Test
    public void testSplitSimpleFile() 
    {
        String[] expected = {"qux.file"};
        assertEquals( Arrays.asList( expected ), 
                      MakefileHelper.splitPath( new File( File.separator, QUXFILE ) ) );
    }
    
    @Test
    public void testSplitSimpleFolderAndFile() 
    {
        String[] expected = {"foo", "bar", "baz", "qux.file"};
        assertEquals( Arrays.asList( expected ), 
                      MakefileHelper.splitPath( new File( File.separator, FOOBARBAZ_QUXFILE ) ) );
    }
    
    @Test
    public void testSplitRelativeFolder() 
    {
        // Split 'foo/bar/baz' (note no leading slash)
        String[] expected = {"foo", "bar", "baz"};
        assertEquals( Arrays.asList( expected ), 
                      MakefileHelper.splitPath( new File( FOLDER_FOOBARBAZ ) ) );
    }
    
    @Test
    public void testSplitAbsoluteFolderFromCWD() throws IOException
    {
        // Split absolute path of 'foo/bar/baz' (note no leading slash)
        List<String> expected = getCWDSplit();
        expected.add( "foo" );
        expected.add( "bar" );
        assertEquals( expected, 
                      MakefileHelper.splitPath( new File( FOLDER_FOOBAR ).getAbsoluteFile() ) );
    }
    
    @Test
    public void testSplitRelativeFile() 
    {
        String[] expected = {"qux.file"};
        assertEquals( Arrays.asList( expected ), 
                      MakefileHelper.splitPath( new File( QUXFILE ) ) );
    }
    
    @Test
    public void testSplitRoot()
    {
        assertEquals( Collections.EMPTY_LIST, MakefileHelper.splitPath( new File( File.separator ) ) );
    }
    
    @Test
    public void testResolveRelativePathSimpleRelative() throws IOException
    {
        File directory = new File( System.getProperty( "user.dir" ) );
        File file = new File( System.getProperty( "user.dir" ), QUXFILE );
        
        assertEquals( "qux.file", MakefileHelper.resolveRelativePath( directory, file ) );
    }

    @Test
    public void testResolveRelativePathSimpleRelative2() throws IOException
    {
        File directory = new File( System.getProperty( "user.dir" ) );
        File file = new File( System.getProperty( "user.dir" ), FOOBARBAZ_QUXFILE );
        
        assertEquals( FOOBARBAZ_QUXFILE, MakefileHelper.resolveRelativePath( directory, file ) );
    }

    @Test
    public void testResolveRelativePathParent() throws IOException
    {
        File directory = new File( File.separator + FOLDER_FOOBARBAZ );
        File file = new File( File.separator + FOOBAR_QUXFILE );
        
        assertEquals( ".." + File.separator + "qux.file", 
                MakefileHelper.resolveRelativePath( directory, file ) );
    }

    @Test
    public void testResolveRelativePathNointersect() throws IOException
    {
        File directory = new File( File.separator + FOLDER_FREDBARNY );
        File file = new File( File.separator + FOOBARBAZ_QUXFILE );
        assertEquals( ".." + File.separator + ".." + File.separator + FOOBARBAZ_QUXFILE, 
                MakefileHelper.resolveRelativePath( directory, file ) );

        // This would work except Windows has drive letters
        // we therefore need to use paths with different drive letters
        // to fully test this
        // NOTE: THIS TEST WILL ONLY PASS IF DRIVES C AND D CAN BE READ
        if ( MakefileHelper.IS_WINDOWS )
        {
            directory = new File( "C:\\" + FOLDER_FREDBARNY );
            file = new File( "D:\\" + FOOBARBAZ_QUXFILE );

            try
            {
                MakefileHelper.resolveRelativePath( directory, file );
                fail( "Expected exception not thrown (relative paths cannot cross windows drives)" );
            }
            catch ( IOException ioex )
            {
                assertEquals( "Unable to resolve relative path across windows drives", ioex.getMessage() );
            }
        }
        
    }
    
    @Test
    public void testResolveRelativePathNearRootParent() throws IOException
    {
        File directory = new File( File.separator + FOLDER_FOO );
        File file = new File( File.separator + QUXFILE );
        
        assertEquals( ".." + File.separator + "qux.file", 
                MakefileHelper.resolveRelativePath( directory, file ) );
    }
    

    /**
     * Crude method to split paths into components for constructing test data.
     * @param toSplit the path to split
     * @return a List of Strings each of which is a component of the path represented by toSplit
     */
    private List<String> getSplit( File toSplit )
    {
        List<String> result = new ArrayList<String>();
        if ( File.separator.equals( "\\" ) )
        {
            result.addAll( Arrays.asList( toSplit.toString().split( "\\\\" ) ) );
        }
        else
        {
            result.addAll( Arrays.asList( toSplit.toString().split( File.separator ) ) );
        }
        return result;
    }

    /**
     * Gets the current working directory (CWD) and splits it into path components
     * @return a List of Strings each of which is a component of the path of the CWD
     * @throws IOException if there is a problem getting the CWD from Java
     */
    private List<String> getCWDSplit() throws IOException
    {
        return getSplit( new File( "." ).getCanonicalFile() );
    }
}
