
package com.jayway.maven.plugins.android.phase09package;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.jayway.maven.plugins.android.AbstractAndroidMojoTestCase;
import com.jayway.maven.plugins.android.configuration.ConfigHelper;

@RunWith( Parameterized.class )
public class ApkMojoTest
extends AbstractAndroidMojoTestCase<ApkMojo>
{

	@Parameters
	static public List<Object[]> suite()
	{
		final List<Object[]> suite = new ArrayList<Object[]>();

		suite.add( new Object[] { "apk-config-project1", false } );
		suite.add( new Object[] { "apk-config-project2", true } );

		return suite;
	}

	private final String	projectName;

	private final boolean	expected;

	public ApkMojoTest( String projectName, boolean expected )
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
	public void setUp() throws Exception
	{
		super.setUp();
	}

	@Override
	@After
	public void tearDown() throws Exception
	{
		super.tearDown();
	}

	@Test
	public void testConfigHelper() throws Exception
	{
		final ApkMojo mojo = createMojo( this.projectName );

		ConfigHelper.copyValues( mojo, "apk" );

		final Boolean extractDuplicates1 = getFieldValue( mojo, "apkExtractDuplicates" );

		Assert.assertNotNull( extractDuplicates1 );
		Assert.assertEquals( this.expected, extractDuplicates1.booleanValue() );

		final Boolean extractDuplicates2 = getFieldValue( mojo, "extractDuplicates" );

		Assert.assertNotNull( extractDuplicates2 );
		Assert.assertEquals( this.expected, extractDuplicates2.booleanValue() );
	}

	protected <T> T getFieldValue( Object object, String fieldName ) throws IllegalAccessException
	{
		return (T) super.getVariableValueFromObject( object, fieldName );
	}

}
