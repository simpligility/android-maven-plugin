/*
 * Copyright (C) 2009-2011 Jayway AB
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
package com.jayway.maven.plugins.android.standalonemojos;

import com.jayway.maven.plugins.android.AbstractZipalignMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * ZipalignMojo can run the zipalign command against the apk.
 *
 * @author Manfred Moser <manfred@simpligility.com>
 * @goal zipalign
 * @requiresProject false
 */
public class ZipalignMojo extends AbstractZipalignMojo
{

    public void execute() throws MojoExecutionException, MojoFailureException
    {
        zipalign();
    }


}
