package com.simpligility.maven.plugins.android;

import java.util.Collection;
import java.util.LinkedHashSet;

import org.apache.maven.artifact.Artifact;
import org.junit.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

import static com.simpligility.maven.plugins.android.InclusionExclusionResolver.filterArtifacts;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

public class InclusionExclusionResolverTest
{

    private static final Artifact artifact1Jar = artifact( "jar", "G1", "A1", "1.0" );
    private static final Artifact artifact2Jar = artifact( "jar", "G2", "A1", "1.0" );
    private static final Artifact artifact3Aar = artifact( "aar", "G1", "A2", "1.0" );
    private static final Artifact artifact4Jar = artifact( "jar", "G1", "A3", "2.0-rc" );
    private static final Artifact artifact5Aar = artifact( "aar", "G2", "A2", "2.0-rc" );

    private static final Collection< Artifact > allArtifacts = collect(
            artifact1Jar, artifact2Jar, artifact3Aar, artifact4Jar, artifact5Aar
    );

    private static final Collection< Artifact > noArtifacts = emptySet();

    @Test
    public void testSkipDependenciesFalse()
    {
        assertEquals(
                "No artifacts must be skiped",
                allArtifacts,
                filterArtifacts( allArtifacts, false, null, null, null, null )
        );
	}

    @Test
    public void testSkipDependenciesTrue()
    {
        assertEquals(
                "All artifacts must be skipped",
                noArtifacts,
                filterArtifacts( allArtifacts, true, null, null, null, null )
        );
    }

    @Test
    public void testSkipDependenciesIncludeTypes()
    {
        assertEquals(
                "All artifacts must be skipped, but AAR artifacts have higher priority",
                collect( artifact3Aar, artifact5Aar ),
                filterArtifacts( allArtifacts, true, singleton( "aar" ), null, null, null )
        );
        assertEquals(
                "All artifacts must be skipped, but JAR artifacts have higher priority",
                collect( artifact1Jar, artifact2Jar, artifact4Jar ),
                filterArtifacts( allArtifacts, true, singleton( "jar" ), null, null, null )
        );
        assertEquals(
                "No artifacts must be skipped",
                allArtifacts,
                filterArtifacts( allArtifacts, false, singleton( "aar" ), null, null, null )
        );
        assertEquals(
                "No artifacts must be skipped",
                allArtifacts,
                filterArtifacts( allArtifacts, false, singleton( "jar" ), null, null, null )
        );
    }

    @Test
    public void testSkipDependenciesExcludeTypes()
    {
        assertEquals(
                "All artifacts must be skipped, especially AAR artifacts",
                noArtifacts,
                filterArtifacts( allArtifacts, true, null, singleton( "aar" ), null, null )
        );
        assertEquals(
                "All artifacts must be skipped, especially JAR artifacts",
                noArtifacts,
                filterArtifacts( allArtifacts, true, null, singleton( "jar" ), null, null )
        );
        assertEquals(
                "AAR artifacts must be skipped",
                collect( artifact1Jar, artifact2Jar, artifact4Jar ),
                filterArtifacts( allArtifacts, false, null, singleton( "aar" ), null, null )
        );
        assertEquals(
                "JAR artifacts must be skipped",
                collect( artifact3Aar, artifact5Aar ),
                filterArtifacts( allArtifacts, false, null, singleton( "jar" ), null, null )
        );
        assertEquals(
                "All artifacts must be skipped, especially both JAR and AAR artifacts",
                noArtifacts,
                filterArtifacts( allArtifacts, false, null, asList( "aar", "jar" ), null, null )
        );
    }

    @Test
    public void testMatchingArtifactTypesIncludeExcludePriority()
    {
        assertEquals(
                "Include must have higher priority",
                allArtifacts,
                filterArtifacts( allArtifacts, false, singleton( "jar" ), singleton( "jar" ), null, null )
        );
        assertEquals(
                "Include must have higher priority",
                collect( artifact1Jar, artifact2Jar, artifact4Jar ),
                filterArtifacts( allArtifacts, false, singleton( "jar" ), asList("aar", "jar"), null, null )
        );
        assertEquals(
                "Include must have higher priority",
                collect( artifact1Jar, artifact2Jar, artifact4Jar ),
                filterArtifacts( allArtifacts, true, singleton( "jar" ), singleton( "jar" ), null, null )
        );
        assertEquals(
                "Include must have higher priority",
                collect( artifact1Jar, artifact2Jar, artifact4Jar ),
                filterArtifacts( allArtifacts, true, singleton( "jar" ), asList( "aar", "jar" ), null, null )
        );
    }

    @Test
    public void testIncludeExcludeByQualifiers()
    {
        assertEquals(
                "Empty exclude must do nothing",
                allArtifacts,
                filterArtifacts( allArtifacts, false, null, null, null, singleton( "" ) )
        );
        assertEquals(
                "Empty include must do nothing",
                noArtifacts,
                filterArtifacts( allArtifacts, true, null, null, singleton( "" ), null )
        );
        assertEquals(
                "Skip all and must include all of group G2",
                collect( artifact2Jar, artifact5Aar ),
                filterArtifacts( allArtifacts, true, null, null, singleton( "G2" ), null )
        );
        assertEquals(
                "Skip all and must include all of group G2 and artifact A2",
                collect( artifact5Aar ),
                filterArtifacts( allArtifacts, true, null, null, singleton( "G2:A2" ), null )
        );
        assertEquals(
                "Do not skip and must exclude group G2",
                collect( artifact1Jar, artifact3Aar, artifact4Jar ),
                filterArtifacts( allArtifacts, false, null, null, null, singleton( "G2" ) )
        );
        assertEquals(
                "Do not skip and must exclude group G2 and artifact A2",
                collect( artifact1Jar, artifact2Jar, artifact3Aar, artifact4Jar ),
                filterArtifacts( allArtifacts, false, null, null, null, singleton( "G2:A2" ) )
        );
        assertEquals(
                "Do not skip and must exclude group G2 and artifact A2 with invalid version",
                allArtifacts,
                filterArtifacts( allArtifacts, false, null, null, null, singleton( "G2:A2:-" ) )
        );
        assertEquals(
                "Do not skip and must exclude group G2 and artifact A2 with valid version",
                collect( artifact1Jar, artifact2Jar, artifact3Aar, artifact4Jar ),
                filterArtifacts( allArtifacts, false, null, null, null, singleton( "G2:A2:2.0-rc" ) )
        );
    }

    @Test
    public void testIncludeExcludeTypeQualifierIntersections()
    {
        assertEquals(
                "Exclude all JARs but include by artifact qualifiers",
                collect( artifact2Jar, artifact3Aar, artifact4Jar, artifact5Aar ),
                filterArtifacts( allArtifacts, false, null, singleton( "jar" ), asList( "G2:A1", "G1:A3" ), null )
        );
        assertEquals(
                "Skip all but must include all AAR files despite the concrete artifact exclusion",
                collect( artifact3Aar, artifact5Aar ),
                filterArtifacts( allArtifacts, true, singleton( "aar" ), null, null, singleton( "G2:A2:2.0-rc" ) )
        );
    }

    @Test( expected = IllegalArgumentException.class )
    public void testIllegalQualifier()
    {
        filterArtifacts( allArtifacts, false, null, null, singleton( "G1:A1:V:X" ), null );
    }

    private static Collection<Artifact> collect( Artifact... artifacts )
    {
        return new LinkedHashSet< Artifact >( asList( artifacts ) );
    }

    private static Artifact artifact( String type, String groupId, String artifactId, String version )
    {
        final Artifact artifact = createMock( Artifact.class );
        expect( artifact.getType() ).andReturn( type ).anyTimes();
        expect( artifact.getGroupId() ).andReturn( groupId ).anyTimes();
        expect( artifact.getArtifactId() ).andReturn( artifactId ).anyTimes();
        expect( artifact.getVersion() ).andReturn(version).anyTimes();
        replay(artifact);
        return artifact;
    }

}
