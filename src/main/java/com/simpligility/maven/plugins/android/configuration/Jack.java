package com.simpligility.maven.plugins.android.configuration;

import com.simpligility.maven.plugins.android.compiler.JackCompiler;
import java.util.Map;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

public class Jack
{

    private Boolean enabled = false;
    
    public Jack() 
    {
    }
    
    public Jack( Map pluginContext ) 
    {
        MavenProject project = ( MavenProject ) pluginContext.get( "project" );
        mavenCompilerId = project.getProperties().getProperty( "maven.compiler.compilerId", "" );
    }
    
    /**
     * @parameter expression="maven.compiler.compilerId"  default-value=""
     */
    @Parameter ( property = "maven.compiler.compilerId" , defaultValue = "" )
    private String mavenCompilerId = "";
    
    public Boolean isEnabled()
    {
        return enabled || mavenCompilerId.equals( JackCompiler.JACK_COMPILER_ID );
    }
    
    
}
