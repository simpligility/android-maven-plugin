package com.jayway.maven.plugins.android;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class AbstractEmulatorMojoTest {

	private AbstractEmulatorMojo mockMojo;

	@Before
	public void setUp() throws Exception {
		mockMojo = new mockAbstractEmulatorMojo();
	}
	
	@Ignore
	@Test
	public final void testStartAndStopAndroidEmulator() throws MojoExecutionException {
		mockMojo.startAndroidEmulator();
		mockMojo.stopAndroidEmulator();
	}

	private class mockAbstractEmulatorMojo extends AbstractEmulatorMojo {

		public void execute() throws MojoExecutionException,
		MojoFailureException {
			// TODO Auto-generated method stub

		}

	}

}
