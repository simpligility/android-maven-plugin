package com.jayway.maven.plugins.android.phase01generatesources;

import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Covers method {@link GenerateSourcesMojo#getPackageCompareMap(Set)} with tests
 *
 * @author Oleg Green <olegalex.green@gmail.com>
 */

@RunWith( PowerMockRunner.class )
@PrepareForTest( GenerateSourcesMojo.class )
public class GetPackageCompareMapTest
{
    public static final String PROJECT_ARTIFACT_ID = "main_application";
    public static final String PROJECT_PACKAGE_NAME = "com.jayway.maven.application";
    public static final String COM_JAYWAY_MAVEN_LIBRARY_PACKAGE = "com.jayway.maven.library";
    public static final String COM_JAYWAY_MAVEN_LIBRARY2_PACKAGE = "com.jayway.maven.library2";
    public static final String COM_JAYWAY_MAVEN_LIBRARY3_PACKAGE = "com.jayway.maven.library3";
    public static final Artifact LIBRARY1_ARTIFACT = createArtifact("library1");
    public static final Artifact LIBRARY2_ARTIFACT = createArtifact("library2");
    public static final Artifact LIBRARY3_ARTIFACT = createArtifact("library3");
    public static final Map<Artifact, String > TEST_DATA_1 = new HashMap<Artifact, String>()
    {
        {
            put( LIBRARY1_ARTIFACT, COM_JAYWAY_MAVEN_LIBRARY_PACKAGE );
            put( LIBRARY2_ARTIFACT, PROJECT_PACKAGE_NAME );
            put( LIBRARY3_ARTIFACT, COM_JAYWAY_MAVEN_LIBRARY_PACKAGE );
        }
    };
    public static final Map<Artifact, String > TEST_DATA_2 = new HashMap<Artifact, String>()
    {
        {
            put( LIBRARY1_ARTIFACT, COM_JAYWAY_MAVEN_LIBRARY_PACKAGE );
            put( LIBRARY2_ARTIFACT, COM_JAYWAY_MAVEN_LIBRARY2_PACKAGE );
            put( LIBRARY3_ARTIFACT, COM_JAYWAY_MAVEN_LIBRARY3_PACKAGE );
        }
    };

    private MavenProject project;
    private Artifact projectArtifact;
    private GenerateSourcesMojo mojo;

    @Before
    public void setUp() throws Exception {

        mojo = PowerMock.createPartialMock( GenerateSourcesMojo.class,
                "extractPackageNameFromAndroidManifest",
                "extractPackageNameFromAndroidArtifact" );

        setUpMainProject();
        Whitebox.setInternalState(mojo, "project", project);

        Method extractPackageNameFromAndroidManifestMethod = Whitebox.getMethod(
                AbstractAndroidMojo.class,
                "extractPackageNameFromAndroidManifest",
                File.class
        );
        PowerMock.expectPrivate(
                mojo,
                extractPackageNameFromAndroidManifestMethod,
                EasyMock.anyObject( File.class )
        ).andReturn(PROJECT_PACKAGE_NAME).once();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoDependencies() throws MojoExecutionException
    {
        PowerMock.replay( mojo );

        mojo.getPackageCompareMap(null);
    }

    @Test
    public void testEmptyDependencies() throws MojoExecutionException
    {
        PowerMock.replay( mojo );

        Map<String, Set<Artifact>> map = mojo.getPackageCompareMap( new HashSet<Artifact>() );
        assertNotNull( map );
        assertEquals( 1, map.size() );
        assertTrue( map.containsKey( PROJECT_PACKAGE_NAME ) );

        Set<Artifact> artifactSet = map.get( PROJECT_PACKAGE_NAME );
        assertEquals( 1, artifactSet.size() );
        assertTrue( artifactSet.contains( projectArtifact ) );
    }

    @Test
    public void testData1() throws Exception
    {
        mockExtractPackageNameFromArtifactMethod( TEST_DATA_1 );
        PowerMock.replay( mojo );

        Map<String, Set<Artifact>> map = mojo.getPackageCompareMap( TEST_DATA_1.keySet() );
        assertNotNull( map );
        assertEquals( 2, map.size() );
        assertTrue( map.containsKey( PROJECT_PACKAGE_NAME ) );
        assertTrue( map.containsKey( COM_JAYWAY_MAVEN_LIBRARY_PACKAGE ) );

        Set<Artifact> artifactSet1 = map.get( PROJECT_PACKAGE_NAME );
        assertEquals( 2, artifactSet1.size() );
        assertTrue( artifactSet1.contains( LIBRARY2_ARTIFACT ) );
        assertTrue( artifactSet1.contains( projectArtifact ) );

        Set<Artifact> artifactSet2 = map.get( COM_JAYWAY_MAVEN_LIBRARY_PACKAGE );
        assertEquals( 2, artifactSet2.size() );
        assertTrue( artifactSet2.contains( LIBRARY1_ARTIFACT ) );
        assertTrue( artifactSet2.contains( LIBRARY3_ARTIFACT ) );

        PowerMock.verify( mojo );
        EasyMock.verify( project, projectArtifact );
    }

    @Test
    public void testData2() throws Exception
    {
        mockExtractPackageNameFromArtifactMethod( TEST_DATA_2 );
        PowerMock.replay( mojo );

        Map<String, Set<Artifact>> map = mojo.getPackageCompareMap( TEST_DATA_2.keySet() );
        assertNotNull( map );
        assertEquals( 4, map.size() );
        assertTrue( map.containsKey( PROJECT_PACKAGE_NAME ) );

        Set<Artifact> artifactSet1 = map.get( PROJECT_PACKAGE_NAME );
        assertEquals( 1, artifactSet1.size() );
        assertTrue( artifactSet1.contains( projectArtifact ) );

        Set<Artifact> artifactSet2 = map.get( COM_JAYWAY_MAVEN_LIBRARY_PACKAGE );
        assertEquals( 1, artifactSet2.size() );
        assertTrue( artifactSet2.contains( LIBRARY1_ARTIFACT ) );

        Set<Artifact> artifactSet3 = map.get( COM_JAYWAY_MAVEN_LIBRARY2_PACKAGE );
        assertEquals( 1, artifactSet3.size() );
        assertTrue( artifactSet3.contains( LIBRARY2_ARTIFACT ) );

        Set<Artifact> artifactSet4 = map.get( COM_JAYWAY_MAVEN_LIBRARY3_PACKAGE );
        assertEquals( 1, artifactSet4.size() );
        assertTrue( artifactSet4.contains( LIBRARY3_ARTIFACT ) );

        PowerMock.verify( mojo );
        EasyMock.verify( project, projectArtifact );

    }

    private void setUpMainProject()
    {
        projectArtifact = EasyMock.createMock( Artifact.class );
        EasyMock.expect( projectArtifact.getArtifactId() ).andReturn( PROJECT_ARTIFACT_ID ).anyTimes();
        EasyMock.replay( projectArtifact );

        project = EasyMock.createNiceMock( MavenProject.class );
        EasyMock.expect( project.getArtifact() ).andReturn( projectArtifact );
        EasyMock.replay( project );
    }

    private void mockExtractPackageNameFromArtifactMethod( final Map<Artifact, String> testData  ) throws Exception
    {
        Method extractPackageNameFromAndroidArtifact = Whitebox.getMethod(
                AbstractAndroidMojo.class,
                "extractPackageNameFromAndroidArtifact",
                Artifact.class
        );
        PowerMock.expectPrivate(
                mojo,
                extractPackageNameFromAndroidArtifact,
                EasyMock.anyObject( Artifact.class )
        ).andAnswer(new IAnswer<String>() {
            @Override
            public String answer() throws Throwable {
                final Object[] args = EasyMock.getCurrentArguments();
                final Artifact inputArtifact = (Artifact)args[0];
                return testData.get(inputArtifact);
            }
        }).anyTimes();
    }

    private static Artifact createArtifact( String artifactId )
    {
        Artifact artifactMock = EasyMock.createMock( Artifact.class );
        EasyMock.expect( artifactMock.getArtifactId() ).andReturn( artifactId ).anyTimes();
        EasyMock.replay( artifactMock );
        return artifactMock;
    }
}
