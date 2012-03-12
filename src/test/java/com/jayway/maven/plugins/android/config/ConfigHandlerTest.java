package com.jayway.maven.plugins.android.config;

import static org.junit.Assert.*;

import org.junit.Test;

public class ConfigHandlerTest {

	private DummyMojo mojo = new DummyMojo();

	@Test
	public void testParseConfigurationDefault() throws Exception {
		ConfigHandler configHandler = new ConfigHandler(mojo);
		configHandler.parseConfiguration();
		assertTrue(mojo.getParsedBooleanValue());
	}

	@Test
	public void testParseConfigurationFromConfigPojo() throws Exception {
		mojo.setConfigPojo(new DummyConfigPojo("from config pojo", null));
		ConfigHandler configHandler = new ConfigHandler(mojo);
		configHandler.parseConfiguration();
		assertEquals("from config pojo",mojo.getParsedStringValue());
	}

	@Test
	public void testParseConfigurationFromMaven() throws Exception {
		mojo.setConfigPojoStringValue("maven value");
		ConfigHandler configHandler = new ConfigHandler(mojo);
		configHandler.parseConfiguration();
		assertEquals("maven value",mojo.getParsedStringValue());
	}

	@Test
	public void testParseConfigurationDefaultMethodValue() throws Exception {
		ConfigHandler configHandler = new ConfigHandler(mojo);
		configHandler.parseConfiguration();
		assertArrayEquals(new String[] {"a","b"},mojo.getParsedMethodValue());
	}
}
