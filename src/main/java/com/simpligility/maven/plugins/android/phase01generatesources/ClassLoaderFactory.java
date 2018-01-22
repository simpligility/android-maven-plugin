/*
 * Copyright (C) 2009 Jayway AB
 * Copyright (C) 2007-2008 JVending Masa
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
package com.simpligility.maven.plugins.android.phase01generatesources;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates a ClassLoader from a Collection of classpath elements.
 *
 * @author William Ferguson - william.ferguson@xandar.com.au
 */
final class ClassLoaderFactory
{
    private final List<String> classpathElements;

    ClassLoaderFactory( List<String> classpathElements )
    {
        this.classpathElements = classpathElements;
    }

    /**
     * @return ClassLoader containing the classpaths.
     */
    ClassLoader create()
    {
        final List<URL> urls = new ArrayList<>();
        for ( final String element : classpathElements )
        {
            try
            {
                urls.add( new File( element ).toURI().toURL() );
            }
            catch ( MalformedURLException e )
            {
                throw new IllegalArgumentException( "Could not resolve dependency : " + element, e );
            }
        }
        return new URLClassLoader(
                urls.toArray( new URL[urls.size()] ),
                Thread.currentThread().getContextClassLoader()
        );
    }
}
