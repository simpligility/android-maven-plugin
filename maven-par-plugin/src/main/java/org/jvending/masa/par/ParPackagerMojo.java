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
package org.jvending.masa.par;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.zip.ZipArchiver;

import java.io.File;
import java.io.IOException;

/**
 * @goal package
 * @requiresProject true
 * @requiresDependencyResolution runtime
 * @description
 */
public class ParPackagerMojo extends AbstractMojo {

    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * Input directory
     *
     * @parameter expression = "${inputDirectory}" default-value="${project.build.directory}/par-archive"
     * @required
     */
    private File inputDir;

    public void execute() throws MojoExecutionException, MojoFailureException {
        ZipArchiver archiver = new ZipArchiver();
        archiver.setForced(true);
        archiver.setDestFile(new File(project.getBuild().getDirectory(), project.getBuild().getFinalName() + ".par"));

        try {
            archiver.addDirectory(inputDir);
            archiver.createArchive();
        } catch (ArchiverException e) {
            throw new MojoExecutionException("", e);
        } catch (IOException e) {
            throw new MojoExecutionException("", e);
        }
    }
}