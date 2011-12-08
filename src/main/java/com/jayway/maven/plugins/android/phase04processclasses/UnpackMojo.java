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
package com.jayway.maven.plugins.android.phase04processclasses;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.jayway.maven.plugins.android.common.JarHelper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.IOUtil;

import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import com.jayway.maven.plugins.android.CommandExecutor;

/**
 * Deprecated, no need to unpack, dex will properly read a list of inputs
 *
 * unpack library.
 * 
 * @author hugo.josefson@jayway.com
 * @goal unpack
 * @phase process-classes
 * @requiresDependencyResolution compile
 */
public class UnpackMojo extends AbstractAndroidMojo {
	/**
	 * If true, the library will be unpacked only when outputDirectory doesn't
	 * exist, i.e, a clean build for most cases.
	 * 
	 * @parameter expression="${android.lazyLibraryUnpack}"
	 *            default-value="false"
	 */
	private boolean lazyLibraryUnpack;
	
	public void execute() throws MojoExecutionException, MojoFailureException {

		CommandExecutor executor = CommandExecutor.Factory
				.createDefaultCommmandExecutor();
		executor.setLogger(this.getLog());

		if (generateApk) {
			// Unpack all dependent and main classes
			unpackClasses();
		}
	}

	private File unpackClasses() throws MojoExecutionException {
		File outputDirectory = new File(project.getBuild().getDirectory(),
				"android-classes");
		if (lazyLibraryUnpack && outputDirectory.exists())
			getLog().info("skip library unpacking due to lazyLibraryUnpack policy");
		else {
			for (Artifact artifact : getRelevantCompileArtifacts()) {
	
				if (artifact.getFile().isDirectory()) {
					try {
						FileUtils
								.copyDirectory(artifact.getFile(), outputDirectory);
					} catch (IOException e) {
						throw new MojoExecutionException(
								"IOException while copying "
										+ artifact.getFile().getAbsolutePath()
										+ " into "
										+ outputDirectory.getAbsolutePath(), e);
					}
				} else {
					try {
                        JarHelper.unjar(new JarFile(artifact.getFile()), outputDirectory, new JarHelper.UnjarListener() {
                            @Override
                            public boolean include(JarEntry jarEntry) {
                                return !jarEntry.getName().startsWith("META-INF") && jarEntry.getName().endsWith(".class");
                            }
                        });
					} catch (IOException e) {
						throw new MojoExecutionException(
								"IOException while unjarring "
										+ artifact.getFile().getAbsolutePath()
										+ " into "
										+ outputDirectory.getAbsolutePath(), e);
					}
				}
	
			}
		}
		
		try {
			File sourceDirectory = new File(project.getBuild().getOutputDirectory());
			FileUtils.copyDirectory(sourceDirectory, outputDirectory);
		} catch (IOException e) {
			throw new MojoExecutionException("IOException while copying "
					+ sourceDirectory.getAbsolutePath() + " into "
					+ outputDirectory.getAbsolutePath(), e);
		}
		return outputDirectory;
	}

}
