package com.jayway.maven.plugins.android.phase04processclasses;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.IOUtil;

import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import com.jayway.maven.plugins.android.CommandExecutor;

/**
 * unpack library.
 * 
 * @author hugo.josefson@jayway.com
 * @goal unpack
 * @phase process-classes
 * @requiresDependencyResolution compile
 */
public class UnpackMojo extends AbstractAndroidMojo {
	public void execute() throws MojoExecutionException, MojoFailureException {

		CommandExecutor executor = CommandExecutor.Factory
				.createDefaultCommmandExecutor();
		executor.setLogger(this.getLog());

		File inputFile = new File(project.getBuild().getDirectory()
				+ File.separator + project.getBuild().getFinalName() + ".jar");

		if (generateApk) {
			// Unpack all dependent and main classes
			unpackClasses(inputFile);
		}
	}

	private File unpackClasses(File inputFile) throws MojoExecutionException {
		File outputDirectory = new File(project.getBuild().getDirectory(),
				"android-classes");
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
					unjar(new JarFile(artifact.getFile()), outputDirectory);
				} catch (IOException e) {
					throw new MojoExecutionException(
							"IOException while unjarring "
									+ artifact.getFile().getAbsolutePath()
									+ " into "
									+ outputDirectory.getAbsolutePath(), e);
				}
			}

		}

		try {
			unjar(new JarFile(inputFile), outputDirectory);
		} catch (IOException e) {
			throw new MojoExecutionException("IOException while unjarring "
					+ inputFile.getAbsolutePath() + " into "
					+ outputDirectory.getAbsolutePath(), e);
		}
		return outputDirectory;
	}

	private void unjar(JarFile jarFile, File outputDirectory)
			throws IOException {
		for (Enumeration en = jarFile.entries(); en.hasMoreElements();) {
			JarEntry entry = (JarEntry) en.nextElement();
			File entryFile = new File(outputDirectory, entry.getName());
			if (!entryFile.getParentFile().exists()
					&& !entry.getName().startsWith("META-INF")) {
				entryFile.getParentFile().mkdirs();
			}
			if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
				final InputStream in = jarFile.getInputStream(entry);
				try {
					final OutputStream out = new FileOutputStream(entryFile);
					try {
						IOUtil.copy(in, out);
					} finally {
						IOUtils.closeQuietly(out);
					}
				} finally {
					IOUtils.closeQuietly(in);
				}
			}
		}
	}
}
