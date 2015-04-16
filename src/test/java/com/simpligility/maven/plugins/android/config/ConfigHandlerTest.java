package com.simpligility.maven.plugins.android.config;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;

import com.simpligility.maven.plugins.android.config.ConfigHandler;

public class ConfigHandlerTest {

	private DummyMojo mojo = new DummyMojo();

    private MavenSession session;
    private MojoExecution execution;

    @Before
    public void setUp()
    {
        session = createNiceMock( MavenSession.class );

        final MavenProject project = new MavenProjectStub();
        MojoDescriptor mojoDesc = new MojoDescriptor();

        this.execution = new MojoExecution( mojoDesc );

        expect( session.getExecutionProperties() ).andReturn( project.getProperties() );
        expect( session.getCurrentProject() ).andReturn( project );
        replay( session );
    }

	@Test
	public void testParseConfigurationDefault() throws Exception {
        ConfigHandler configHandler = new ConfigHandler( mojo, this.session, this.execution );
		configHandler.parseConfiguration();
		assertTrue(mojo.getParsedBooleanValue());
	}

	@Test
	public void testParseConfigurationFromConfigPojo() throws Exception {
		mojo.setConfigPojo(new DummyConfigPojo("from config pojo", null));
        ConfigHandler configHandler = new ConfigHandler( mojo, this.session, this.execution );
		configHandler.parseConfiguration();
		assertEquals("from config pojo",mojo.getParsedStringValue());
	}

	@Test
	public void testParseConfigurationFromMaven() throws Exception {
		mojo.setConfigPojoStringValue("maven value");
        ConfigHandler configHandler = new ConfigHandler( mojo, this.session, this.execution );
		configHandler.parseConfiguration();
		assertEquals("maven value",mojo.getParsedStringValue());
	}

	@Test
	public void testParseConfigurationDefaultMethodValue() throws Exception {
        ConfigHandler configHandler = new ConfigHandler( mojo, this.session, this.execution );
		configHandler.parseConfiguration();
		assertArrayEquals(new String[] {"a","b"},mojo.getParsedMethodValue());
	}
}
