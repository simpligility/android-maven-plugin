/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.simpligility.maven.plugins.android;

import com.android.annotations.NonNull;
import com.android.ide.common.process.JavaProcessExecutor;
import com.android.ide.common.process.JavaProcessInfo;
import com.android.ide.common.process.ProcessException;
import com.android.ide.common.process.ProcessOutput;
import com.android.ide.common.process.ProcessOutputHandler;
import com.android.ide.common.process.ProcessResult;
import com.android.utils.ILogger;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
/**
 * Simple implementation of ProcessExecutor, using the standard Java Process(Builder) API.
 */
public class DefaultJavaProcessExecutor implements JavaProcessExecutor
{
    private final ILogger mLogger;
    public DefaultJavaProcessExecutor( ILogger logger )
    {
        mLogger = logger;
    }
    @NonNull
    @Override
    public ProcessResult execute(
            @NonNull JavaProcessInfo processInfo,
            @NonNull ProcessOutputHandler processOutputHandler )
    {
        List<String> command = Lists.newArrayList();
        command.add( processInfo.getExecutable() );
        command.addAll( processInfo.getArgs() );
        String commandString = Joiner.on( ' ' ).join( command );
        mLogger.info( "command: " + commandString );
        try
        {
            // launch the command line process
            ProcessBuilder processBuilder = new ProcessBuilder( command );
            Map<String, Object> envVariableMap = processInfo.getEnvironment();
            if ( !envVariableMap.isEmpty() )
            {
                Map<String, String> env = processBuilder.environment();
                for ( Map.Entry<String, Object> entry : envVariableMap.entrySet() )
                {
                    env.put( entry.getKey(), entry.getValue().toString() );
                }
            }
            // start the process
            Process process = processBuilder.start();
            // and grab the output, and the exit code
            ProcessOutput output = processOutputHandler.createOutput();
            int exitCode = grabProcessOutput( process, output );
            processOutputHandler.handleOutput( output );
            return new ProcessResultImplCopy( commandString, exitCode );
        }
        catch ( IOException e )
        {
            return new ProcessResultImplCopy( commandString, e );
        }
        catch ( InterruptedException e )
        {
            // Restore the interrupted status
            Thread.currentThread().interrupt();
            return new ProcessResultImplCopy( commandString, e );
        }
        catch ( ProcessException e )
        {
            return new ProcessResultImplCopy( commandString, e );
        }
    }
    /**
     * Get the stderr/stdout outputs of a process and return when the process is done.
     * Both <b>must</b> be read or the process will block on windows.
     *
     * @param process The process to get the output from.
     * @param output The processOutput containing where to send the output.
     *      Note that on Windows capturing the output is not optional. If output is null
     *      the stdout/stderr will be captured and discarded.
     * @return the process return code.
     * @throws InterruptedException if {@link Process#waitFor()} was interrupted.
     */
    private static int grabProcessOutput(
            @NonNull final Process process,
            @NonNull final ProcessOutput output ) throws InterruptedException
    {
        Thread threadErr = new Thread( "stderr" )
        {
            @Override
            public void run()
            {
                InputStream stderr = process.getErrorStream();
                OutputStream stream = output.getErrorOutput();
                try
                {
                    ByteStreams.copy( stderr, stream );
                    stream.flush();
                }
                catch ( IOException e )
                {
                    // ignore?
                }
                finally
                {
                    try
                    {
                        Closeables.close( stderr, true /* swallowIOException */ );
                    }
                    catch ( IOException e )
                    {
                        // cannot happen
                    }
                    try
                    {
                        Closeables.close( stream, true /* swallowIOException */ );
                    }
                    catch ( IOException e )
                    {
                        // cannot happen
                    }
                }
            }
        };
        Thread threadOut = new Thread( "stdout" )
        {
            @Override
            public void run()
            {
                InputStream stdout = process.getInputStream();
                OutputStream stream = output.getStandardOutput();
                try
                {
                    ByteStreams.copy( stdout, stream );
                    stream.flush();
                }
                catch ( IOException e )
                {
                    // ignore?
                }
                finally
                {
                    try
                    {
                        Closeables.close( stdout, true /* swallowIOException */ );
                    }
                    catch ( IOException e )
                    {
                        // cannot happen
                    }
                    try
                    {
                        Closeables.close( stream, true /* swallowIOException */ );
                    }
                    catch ( IOException e )
                    {
                        // cannot happen
                    }
                }
            }
        };
        threadErr.start();
        threadOut.start();
        // it looks like on windows process#waitFor() can return
        // before the thread have filled the arrays, so we wait for both threads and the
        // process itself.
        threadErr.join();
        threadOut.join();
        // get the return code from the process
        return process.waitFor();
    }
}