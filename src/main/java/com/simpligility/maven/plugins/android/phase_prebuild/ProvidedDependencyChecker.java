package com.simpligility.maven.plugins.android.phase_prebuild;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.logging.Logger;

class ProvidedDependencyChecker
{

    void checkProvidedDependencies( Set<Artifact> artifacts, Logger log )
    {
        List<Artifact> conflictingArtifacts = new ArrayList<Artifact>();

        for ( Artifact artifact : artifacts )
        {
            if ( artifact.getScope().equals( Artifact.SCOPE_TEST ) )
            {
                continue;
            }
            
            String group = artifact.getGroupId();
            String name = artifact.getArtifactId();
            
            if ( ( "org.apache.httpcomponents".equals( group ) && "httpclient".equals( name ) )
                    || ( "xpp3".equals( group ) && name.equals( "xpp3" ) )
                    || ( "commons-logging".equals( group ) && "commons-logging".equals( name ) )
                    || ( "xerces".equals( group ) && "xmlParserAPIs".equals( name ) ) )
            {
                conflictingArtifacts.add( artifact );
            }
            if ( "org.json".equals( group ) && "json".equals( name ) )
            {
                conflictingArtifacts.add( artifact );
            }
            if ( "org.khronos".equals( group ) && "opengl-api".equals( name ) )
            {
                conflictingArtifacts.add( artifact );
            }
        }

        if ( !conflictingArtifacts.isEmpty() )
        {
            log.warn( "The following dependencies may conflict with the "
                    + "internal versions provided by the Android platform:\n" + conflictingArtifacts
                    + "\nIt is recommended to shade these artifacts. " + "You can read more about this here: "
                    + "http://simpligility.github.io/android-maven-plugin/shaded-commons-codec.html.\n"
                    + "Alternatively, you can disable this warning with the"
                    + "'disableConflictingDependenciesWarning' parameter." );
        }
    }
}
