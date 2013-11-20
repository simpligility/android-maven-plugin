
package com.jayway.maven.plugins.android.standalonemojos;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.jayway.maven.plugins.android.AbstractAndroidMojoTestCase;
import com.jayway.maven.plugins.android.config.ConfigHandler;
import com.jayway.maven.plugins.android.configuration.MetaInf;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Pappy STĂNESCU <a href="mailto:pappy.stanescu&#64;gmail.com">&lt;pappy.stanescu&#64;gmail.com&gt;</a>
 */
@RunWith( Parameterized.class )
@FixMethodOrder( MethodSorters.NAME_ASCENDING )
public class UnpackMojoTest
extends AbstractAndroidMojoTestCase<UnpackMojo>
{

	@Parameters
	static public List<Object[]> suite()
	{
		final List<Object[]> suite = new ArrayList<Object[]>();

		suite.add( new Object[] { "unpack-config-project1", null } );
		suite.add( new Object[] { "unpack-config-project2", new MetaInf().include( "persistence.xml" ) } );
		suite.add( new Object[] { "unpack-config-project3", new MetaInf().include( "services/**", "persistence.xml" ) } );
		suite.add( new Object[] { "unpack-config-project4", new MetaInf() } );

		return suite;
	}

	private final String	projectName;

	private final MetaInf	expected;

	public UnpackMojoTest( String projectName, MetaInf expected )
	{
		this.projectName = projectName;
		this.expected = expected;
	}

	@Override
	public String getPluginGoalName()
	{
		return "unpack";
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
		final UnpackMojo mojo = createMojo( this.projectName );
		final ConfigHandler cfh = new ConfigHandler( mojo );

		cfh.parseConfiguration();

		MetaInf result = getFieldValue( mojo, "unpackMetaInf" );

		Assert.assertEquals( this.expected, result );
		
		Assert.assertEquals( result == null, getFieldValue( mojo, "unpack" ) == null );
	}

	protected <T> T getFieldValue( Object object, String fieldName )
	throws IllegalAccessException
	{
		return (T) super.getVariableValueFromObject( object, fieldName );
	}

}
