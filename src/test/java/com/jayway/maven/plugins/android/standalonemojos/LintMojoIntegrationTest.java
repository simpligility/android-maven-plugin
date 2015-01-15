package com.jayway.maven.plugins.android.standalonemojos;

import com.jayway.maven.plugins.android.*;
import io.takari.maven.testing.TestResources;
import io.takari.maven.testing.executor.MavenExecutionResult;
import io.takari.maven.testing.executor.MavenRuntime;
import io.takari.maven.testing.executor.MavenVersions;
import io.takari.maven.testing.executor.junit.MavenJUnitTestRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

/**
 * Test the lint mojo. Tests options' default values and parsing. Tests the parameters passed to lint.
 * 
 * @author Stéphane Nicolas <snicolas@octo.com>
 * @author Benoit Billington
 * 
 */
@RunWith( MavenJUnitTestRunner.class )
@MavenVersions( {"3.0.5", "3.2.5" } )
public class LintMojoIntegrationTest
{
    @Rule
    public final TestResources resources = new TestResources();

    public final MavenRuntime verifier;

    public LintMojoIntegrationTest(MavenRuntime.MavenRuntimeBuilder builder)
    throws Exception
    {
        this.verifier = builder.withCliOptions( "-X" ).build();
    }

    /**
     * Test project with skip lint flag to true
     * 
     * @throws Exception
     */
    @Test
    public void testSkipConfig() throws Exception
    {
        File basedir = resources.getBasedir( "lint-config-project0" );
        MavenExecutionResult result = verifier
                .forProject(basedir)
                .execute( PluginInfo.getQualifiedGoal( "lint" ) );

        result.assertErrorFreeLog();

        result.assertLogText( "Skipping lint analysis" );
    }

    /**
     * Tests execution of Lint
     * 
     * @throws Exception
     */
    @Test
    public void testDefaultUnskippedLintConfig() throws Exception
    {
        File basedir = resources.getBasedir( "lint-config-project1" );
        MavenExecutionResult result = verifier
                .forProject(basedir)
                .execute( PluginInfo.getQualifiedGoal( "lint" ) );

        result.assertErrorFreeLog();

        result.assertLogText( "Lint analysis completed successfully" );
    }

}
