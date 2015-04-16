
package com.simpligility.maven.plugins.android.standalonemojos;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.simpligility.maven.plugins.android.AbstractAndroidMojoTestCase;
import com.simpligility.maven.plugins.android.config.ConfigHandler;
import com.simpligility.maven.plugins.android.configuration.MetaInf;
import com.simpligility.maven.plugins.android.standalonemojos.UnpackMojo;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Pappy STĂNESCU <a href="mailto:pappy.stanescu&#64;gmail.com">&lt;pappy.stanescu&#64;gmail.com&gt;</a>
 */
@Ignore("This test has to be migrated to be an IntegrationTest using AbstractAndroidMojoIntegrationTest") 
@RunWith( Parameterized.class )
public class UnpackMojoLazyTest
extends AbstractAndroidMojoTestCase<UnpackMojo>
{

	@Parameters
	static public List<Object[]> suite()
	{
		final List<Object[]> suite = new ArrayList<Object[]>();

		suite.add( new Object[] { "unpack-config-lazy" } );
		suite.add( new Object[] { "unpack-config-lazy-deprecated" } );

		return suite;
	}

	private final String	projectName;

	public UnpackMojoLazyTest( String projectName )
	{
		this.projectName = projectName;
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

	@Override
	public String getName()
	{
		return projectName;
	}
	
	@Test
	public void testConfigHelper()
	throws Exception
	{
		final UnpackMojo mojo = createMojo( this.projectName );
        final ConfigHandler cfh = new ConfigHandler( mojo, this.session, this.execution );

		cfh.parseConfiguration();

		Boolean result = getFieldValue( mojo, "unpackLazy" );
		
		Assert.assertNotNull(result);
		Assert.assertTrue( result );
	}

	protected <T> T getFieldValue( Object object, String fieldName )
	throws IllegalAccessException
	{
		return (T) super.getVariableValueFromObject( object, fieldName );
	}

}
