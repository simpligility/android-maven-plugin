package com.simpligility.maven.plugins.android.configuration;

import com.simpligility.maven.plugins.android.compiler.JackCompiler;
import org.apache.maven.plugins.annotations.Parameter;

public class Jack
{

    private Boolean enabled;

    /**
     * @parameter expression="${maven.compiler.compilerId}"  default-value=""
     */
    @Parameter ( property = "maven.compiler.compilerId" , defaultValue = "" )
    private String mavenCompilerId;
    
    public Boolean isEnabled()
    {
        return enabled || mavenCompilerId.equals( JackCompiler.JACK_COMPILER_ID );
    }
    
    
}
