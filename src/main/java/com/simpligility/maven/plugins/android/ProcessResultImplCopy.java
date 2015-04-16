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

import com.android.ide.common.process.ProcessException;
import com.android.ide.common.process.ProcessResult;

/**
 * Internal implementation of ProcessResult used by DefaultProcessExecutor.
 */
class ProcessResultImplCopy implements ProcessResult
{
    private final String mCommand;
    private final int mExitValue;
    private final Exception mFailure;
    ProcessResultImplCopy( String command, int exitValue )
    {
        this( command, exitValue, null );
    }
    ProcessResultImplCopy( String command, Exception failure )
    {
        this( command, -1, failure );
    }
    ProcessResultImplCopy( String command, int exitValue, Exception failure )
    {
        mCommand = command;
        mExitValue = exitValue;
        mFailure = failure;
    }
    @Override
    public ProcessResult assertNormalExitValue() throws ProcessException
    {
        if ( mExitValue != 0 )
        {
            throw new ProcessException(
                    String.format( "Return code %d for process '%s'", mExitValue, mCommand ) );
        }
        return this;
    }
    @Override
    public int getExitValue()
    {
        return mExitValue;
    }
    @Override
    public ProcessResult rethrowFailure() throws ProcessException
    {
        if ( mFailure != null )
        {
            throw new ProcessException( "", mFailure );
        }
        return this;
    }
}
