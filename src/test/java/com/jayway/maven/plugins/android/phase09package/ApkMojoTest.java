
package com.jayway.maven.plugins.android.phase09package;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.jayway.maven.plugins.android.AbstractAndroidMojoTestCase;
import com.jayway.maven.plugins.android.config.ConfigHandler;

@RunWith( Parameterized.class )
public class ApkMojoTest
extends AbstractAndroidMojoTestCase<ApkMojo>
{

	@Parameters
	static public List<Object[]> suite()
	{
		final List<Object[]> suite = new ArrayList<Object[]>();

		suite.add( new Object[] { "apk-config-project1", new String[0] } );
		suite.add( new Object[] { "apk-config-project2", new String[] { "persistence.xml" } } );
		suite.add( new Object[] { "apk-config-project2", new String[] { "services/**", "persistence.xml" } } );

		return suite;
	}

	private final String	projectName;

	private final String[]	expected;

	public ApkMojoTest( String projectName, String[] expected )
	{
		this.projectName = projectName;
		this.expected = expected;
	}

	@Override
	public String getPluginGoalName()
	{
		return "apk";
	}

	@Override
	@Before
	public void setUp()
	throws Exception
	{
		super.setUp();
	}

	@Override
	@After
	public void tearDown()
	throws Exception
	{
		super.tearDown();
	}

	@Test
	public void testConfigHelper()
	throws Exception
	{
		final ApkMojo mojo = createMojo( this.projectName );

		final ConfigHandler cfh = new ConfigHandler( mojo );

		cfh.parseConfiguration();

		final String[] includes = getFieldValue( mojo, "apkMetaIncludes" );

		Assert.assertNotNull( includes );
		Assert.assertArrayEquals( this.expected, includes );
	}

	protected <T> T getFieldValue( Object object, String fieldName )
	throws IllegalAccessException
	{
		return (T) super.getVariableValueFromObject( object, fieldName );
	}

}
