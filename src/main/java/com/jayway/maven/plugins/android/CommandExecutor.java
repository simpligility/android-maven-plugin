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
package com.jayway.maven.plugins.android;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.codehaus.plexus.util.cli.shell.BourneShell;

/**
 *
 */
public interface CommandExecutor
{
    /**
     * Sets the plexus logger.
     * 
     * @param logger
     *            the plexus logger
     */
    void setLogger( Log logger );

    /**
     * Executes the command for the specified executable and list of command options.
     * 
     * @param executable
     *            the name of the executable (csc, xsd, etc).
     * @param commands
     *            the command options for the compiler/executable
     * @throws ExecutionException
     *             if compiler or executable writes anything to the standard error stream or if the process returns a
     *             process result != 0.
     */
    void executeCommand( String executable, List< String > commands ) throws ExecutionException;

    /**
     * Executes the command for the specified executable and list of command options.
     * 
     * @param executable
     *            the name of the executable (csc, xsd, etc).
     * @param commands
     *            the commands options for the compiler/executable
     * @param failsOnErrorOutput
     *            if true, throws an <code>ExecutionException</code> if there the compiler or executable writes anything
     *            to the error output stream. By default, this value is true
     * @throws ExecutionException
     *             if compiler or executable writes anything to the standard error stream (provided the
     *             failsOnErrorOutput is not false) or if the process returns a process result != 0.
     */
    void executeCommand( String executable, List< String > commands, boolean failsOnErrorOutput )
            throws ExecutionException;

    /**
     * Executes the command for the specified executable and list of command options. If the compiler or executable is
     * not within the environmental path, you should use this method to specify the working directory. Always use this
     * method for executables located within the local maven repository.
     * 
     * @param executable
     *            the name of the executable (csc, xsd, etc).
     * @param commands
     *            the command options for the compiler/executable
     * @param workingDirectory
     *            the directory where the command will be executed
     * @throws ExecutionException
     *             if compiler or executable writes anything to the standard error stream (provided the
     *             failsOnErrorOutput is not false) or if the process returns a process result != 0.
     */
    void executeCommand( String executable, List< String > commands, File workingDirectory, boolean failsOnErrorOutput )
            throws ExecutionException;

    /**
     * Returns the process result of executing the command. Typically a value of 0 means that the process executed
     * successfully.
     * 
     * @return the process result of executing the command
     */
    int getResult();

    /**
     * Get the process id for the executed command.
     * 
     * @return
     */
    long getPid();

    /**
     * Returns the standard output from executing the command.
     * 
     * @return the standard output from executing the command
     */
    String getStandardOut();

    /**
     * Returns the standard error from executing the command.
     * 
     * @return the standard error from executing the command
     */
    String getStandardError();

    /**
     * Adds an environment variable with the specified name and value to the executor.
     * 
     * @param name
     * @param value
     */
    void addEnvironment( String name, String value );

    void setErrorListener( ErrorListener errorListener );

    void setRemoveShellArguments( boolean b );

    /**
     *
     */
    public interface ErrorListener
    {
        boolean isError( String error );
    }

    /**
     * Provides factory services for creating a default instance of the command executor.
     */
    public static class Factory
    {

        /**
         * Constructor
         */
        private Factory()
        {
        }

        private static final class DefaultCommandExecutor implements CommandExecutor
        {
            private Map< String, String > environment;
            /**
             * Instance of a plugin logger.
             */
            private Log logger;
            /**
             * Standard Out
             */
            private StreamConsumer stdOut;
            /**
             * Standard Error
             */
            private ErrorStreamConsumer stdErr;
            /**
             * Process result
             */
            private int result;
            /*
             */
            private ErrorListener errorListener;
            long pid;
            private Commandline commandline;
            private boolean isRemoveshellArgs;

            @Override
            public void setLogger( Log logger )
            {
                this.logger = logger;
            }

            @Override
            public void executeCommand( String executable, List< String > commands ) throws ExecutionException
            {
                executeCommand( executable, commands, null, true );
            }

            @Override
            public void executeCommand( String executable, List< String > commands, boolean failsOnErrorOutput )
                    throws ExecutionException
            {
                executeCommand( executable, commands, null, failsOnErrorOutput );
            }

            @Override
            public void executeCommand( String executable, List< String > commands, File workingDirectory,
                    boolean failsOnErrorOutput ) throws ExecutionException
            {
                if ( commands == null )
                {
                    commands = new ArrayList< String >();
                }
                stdOut = new StreamConsumerImpl( logger );
                stdErr = new ErrorStreamConsumer( logger, errorListener );
                commandline = new Commandline();
                if ( isRemoveshellArgs )
                {
                    commandline.setShell( new CustomBourneShell() );
                }
                commandline.setExecutable( executable );

                // Add the environment variables as needed
                if ( environment != null )
                {
                    for ( Map.Entry< String, String > entry : environment.entrySet() )
                    {
                        commandline.addEnvironment( entry.getKey(), entry.getValue() );
                    }
                }

                commandline.addArguments( commands.toArray( new String[ commands.size() ] ) );
                if ( workingDirectory != null && workingDirectory.exists() )
                {
                    commandline.setWorkingDirectory( workingDirectory.getAbsolutePath() );
                }
                try
                {
                    result = CommandLineUtils.executeCommandLine( commandline, stdOut, stdErr );
                    if ( logger != null )
                    {
                        logger.debug( "ANDROID-040-000: Executed command: Commandline = " + commandline + ", Result = "
                                + result );
                    }
                    else
                    {
                        System.out.println( "ANDROID-040-000: Executed command: Commandline = " + commandline
                                + ", Result = " + result );
                    }
                    if ( failsOnErrorOutput && stdErr.hasError() || result != 0 )
                    {
                        throw new ExecutionException( "ANDROID-040-001: Could not execute: Command = "
                                + commandline.toString() + ", Result = " + result );
                    }
                }
                catch ( CommandLineException e )
                {
                    throw new ExecutionException( "ANDROID-040-002: Could not execute: Command = "
                            + commandline.toString() + ", Error message = " + e.getMessage() );
                }
                setPid( commandline.getPid() );
            }

            @Override
            public int getResult()
            {
                return result;
            }

            @Override
            public String getStandardOut()
            {
                return stdOut.toString();
            }

            @Override
            public String getStandardError()
            {
                return stdErr.toString();
            }

            @Override
            public void addEnvironment( String name, String value )
            {
                if ( environment == null )
                {
                    environment = new HashMap< String, String >();
                }
                environment.put( name, value );
            }

            @Override
            public void setErrorListener( ErrorListener errorListener )
            {
                this.errorListener = errorListener;
            }

            public void setPid( long pid )
            {
                this.pid = pid;
            }

            @Override
            public long getPid()
            {
                return pid;
            }

            @Override
            public void setRemoveShellArguments( boolean b )
            {
                this.isRemoveshellArgs = b;
            }
        }

        private static final class CustomBourneShell extends BourneShell
        {
            @Override
            public List< String > getShellArgsList()
            {
                List< String > shellArgs = new ArrayList< String >();
                List< String > existingShellArgs = super.getShellArgsList();

                if ( existingShellArgs != null && !existingShellArgs.isEmpty() )
                {
                    shellArgs.addAll( existingShellArgs );
                }

                return shellArgs;
            }

            @Override
            public String[] getShellArgs()
            {
                String[] shellArgs = super.getShellArgs();
                if ( shellArgs == null )
                {
                    shellArgs = new String[ 0 ];
                }

                return shellArgs;
            }

        }

        /**
         * StreamConsumer instance that buffers the entire output
         */
        static class StreamConsumerImpl implements StreamConsumer
        {
            private StringBuffer sb = new StringBuffer();
            private final Log logger;

            public StreamConsumerImpl( Log logger )
            {
                this.logger = logger;
            }

            @Override
            public void consumeLine( String line )
            {
                sb.append( line );
                if ( logger != null )
                {
                    logger.debug( line );
                }
            }

            /**
             * Returns the stream
             * 
             * @return the stream
             */
            @Override
            public String toString()
            {
                return sb.toString();
            }
        }

        /**
         * Provides behavior for determining whether the command utility wrote anything to the Standard Error Stream.
         * NOTE: I am using this to decide whether to fail the NMaven build. If the compiler implementation chooses to
         * write warnings to the error stream, then the build will fail on warnings!!!
         */
        static class ErrorStreamConsumer implements StreamConsumer
        {
            /** Is true if there was anything consumed from the stream, otherwise false */
            private boolean error;
            /** Buffer to store the stream */
            private StringBuffer sbe = new StringBuffer();
            private final Log logger;
            private final ErrorListener errorListener;

            public ErrorStreamConsumer( Log logger, ErrorListener errorListener )
            {
                this.logger = logger;
                this.errorListener = errorListener;

                if ( logger == null )
                {
                    System.out.println( "ANDROID-040-003: Error Log not set: Will not output error logs" );
                }
                error = false;
            }

            @Override
            public void consumeLine( String line )
            {
                sbe.append( line );
                if ( logger != null )
                {
                    logger.info( line );
                }
                if ( errorListener != null )
                {
                    error = errorListener.isError( line );
                }
                else
                {
                    error = true;
                }
            }

            /**
             * Returns false if the command utility wrote to the Standard Error Stream, otherwise returns true.
             * 
             * @return false if the command utility wrote to the Standard Error Stream, otherwise returns true.
             */
            public boolean hasError()
            {
                return error;
            }

            /**
             * Returns the error stream
             * 
             * @return error stream
             */
            @Override
            public String toString()
            {
                return sbe.toString();
            }
        }

        /**
         * Returns a default instance of the command executor
         * 
         * @return a default instance of the command executor
         */
        public static CommandExecutor createDefaultCommmandExecutor()
        {
            return new DefaultCommandExecutor();

        }
    }
}
