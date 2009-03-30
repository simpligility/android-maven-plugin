/*
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
package org.jvending.masa;

/**
 *
 */
public class ExecutionException
    extends Exception
{

    static final long serialVersionUID = -7843278034782074384L;

    /**
     * Constructs an <code>ExecutionException</code>  with no exception message.
     */
    public ExecutionException()
    {
        super();
    }

    /**
     * Constructs an <code>ExecutionException</code> with the specified exception message.
     *
     * @param message the exception message
     */
    public ExecutionException( String message )
    {
        super( message );
    }

    /**
     * Constructs an <code>ExecutionException</code> with the specified exception message and cause of the exception.
     *
     * @param message the exception message
     * @param cause   the cause of the exception
     */
    public ExecutionException( String message, Throwable cause )
    {
        super( message, cause );
    }

    /**
     * Constructs an <code>ExecutionException</code> with the cause of the exception.
     *
     * @param cause the cause of the exception
     */
    public ExecutionException( Throwable cause )
    {
        super( cause );
    }
}

