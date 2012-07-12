/*
 * Copyright (C) 2009 Jayway AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jayway.maven.plugins.android.asm;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.DirectoryWalkListener;
import org.codehaus.plexus.util.DirectoryWalker;
import org.objectweb.asm.ClassReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Finds Android instrumentation test classes in a directory of compiled Java classes.
 *
 * @author hugo.josefson@jayway.com
 */
public class AndroidTestFinder
{

    private static final String[] TEST_PACKAGES = { "junit/framework/", "android/test/" };

    public static boolean containsAndroidTests( File classesBaseDirectory ) throws MojoExecutionException
    {

        if ( classesBaseDirectory == null || ! classesBaseDirectory.isDirectory() )
        {
            throw new IllegalArgumentException( "classesBaseDirectory must be a valid directory!" );
        }

        final List<File> classFiles = findEligebleClassFiles( classesBaseDirectory );
        final DescendantFinder descendantFinder = new DescendantFinder( TEST_PACKAGES );

        for ( File classFile : classFiles )
        {
            ClassReader classReader;
            FileInputStream inputStream = null;
            try
            {
                inputStream = new FileInputStream( classFile );
                classReader = new ClassReader( inputStream );

                classReader.accept( descendantFinder, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES
                        | ClassReader.SKIP_CODE );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Error reading " + classFile + ".\nCould not determine whether it "
                        + "contains tests. Please specify with plugin config parameter "
                        + "<enableIntegrationTest>true|false</enableIntegrationTest>.", e );
            }
            finally
            {
                IOUtils.closeQuietly( inputStream );
            }
        }

        return descendantFinder.isDescendantFound();
    }

    private static List<File> findEligebleClassFiles( File classesBaseDirectory )
    {
        final List<File> classFiles = new LinkedList<File>();
        final DirectoryWalker walker = new DirectoryWalker();
        walker.setBaseDir( classesBaseDirectory );
        walker.addSCMExcludes();
        walker.addInclude( "**/*.class" );
        walker.addDirectoryWalkListener( new DirectoryWalkListener()
        {
            public void directoryWalkStarting( File basedir )
            {
            }

            public void directoryWalkStep( int percentage, File file )
            {
                classFiles.add( file );
            }

            public void directoryWalkFinished()
            {
            }

            public void debug( String message )
            {
            }
        } );
        walker.scan();
        return classFiles;
    }


}
