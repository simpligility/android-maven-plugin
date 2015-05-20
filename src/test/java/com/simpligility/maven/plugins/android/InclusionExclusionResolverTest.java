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

    private static final Artifact artifact1Jar = artifact( "jar" );
    private static final Artifact artifact2Jar = artifact( "jar" );
    private static final Artifact artifact3Aar = artifact( "aar" );
    private static final Artifact artifact4Jar = artifact( "jar" );
    private static final Artifact artifact5Aar = artifact( "aar" );

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
                filterArtifacts( allArtifacts, false, null, null )
        );
	}

    @Test
    public void testSkipDependenciesTrue()
    {
        assertEquals(
                "All artifacts must be skipped",
                noArtifacts,
                filterArtifacts( allArtifacts, true, null, null )
        );
    }

    @Test
    public void testSkipDependenciesIncludeTypes()
    {
        assertEquals(
                "All artifacts must be skipped, but AAR artifacts have higher priority",
                collect(artifact3Aar, artifact5Aar),
                filterArtifacts( allArtifacts, true, singleton( "aar" ), null )
        );
        assertEquals(
                "All artifacts must be skipped, but JAR artifacts have higher priority",
                collect(artifact1Jar, artifact2Jar, artifact4Jar),
                filterArtifacts( allArtifacts, true, singleton( "jar" ), null )
        );
        assertEquals(
                "No artifacts must be skipped",
                allArtifacts,
                filterArtifacts( allArtifacts, false, singleton( "aar" ), null )
        );
        assertEquals(
                "No artifacts must be skipped",
                allArtifacts,
                filterArtifacts( allArtifacts, false, singleton( "jar" ), null )
        );
    }

    @Test
    public void testSkipDependenciesExcludeTypes()
    {
        assertEquals(
                "All artifacts must be skipped, especially AAR artifacts",
                noArtifacts,
                filterArtifacts( allArtifacts, true, null, singleton( "aar" ) )
        );
        assertEquals(
                "All artifacts must be skipped, especially JAR artifacts",
                noArtifacts,
                filterArtifacts( allArtifacts, true, null, singleton( "jar" ) )
        );
        assertEquals(
                "AAR artifacts must be skipped",
                collect(artifact1Jar, artifact2Jar, artifact4Jar),
                filterArtifacts( allArtifacts, false, null, singleton( "aar" ) )
        );
        assertEquals(
                "JAR artifacts must be skipped",
                collect(artifact3Aar, artifact5Aar),
                filterArtifacts( allArtifacts, false, null, singleton( "jar" ) )
        );
        assertEquals(
                "All artifacts must be skipped, especially both JAR and AAR artifacts",
                noArtifacts,
                filterArtifacts( allArtifacts, false, null, asList( "aar", "jar" ) )
        );
    }

    @Test
    public void testMatchingArtifactTypesIncludeExcludePriority()
    {
        assertEquals(
                "Include must have higher priority",
                allArtifacts,
                filterArtifacts( allArtifacts, false, singleton( "jar" ), singleton( "jar" ) )
        );
        assertEquals(
                "Include must have higher priority",
                collect(artifact1Jar, artifact2Jar, artifact4Jar),
                filterArtifacts( allArtifacts, false, singleton( "jar" ), asList( "aar", "jar" ) )
        );
    }

    private static Collection<Artifact> collect( Artifact... artifacts )
    {
        return new LinkedHashSet< Artifact >( asList( artifacts ) );
    }

    private static Artifact artifact( String type )
    {
        final Artifact artifact = createMock( Artifact.class );
        expect( artifact.getType() ).andReturn( type ).anyTimes();
        replay(artifact);
        return artifact;
    }

}
