package com.jayway.maven.plugins.android.phase08preparepackage;

import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import com.jayway.maven.plugins.android.configuration.Emma;
import com.vladium.emma.instr.InstrProcessor;
import com.vladium.emma.instr.InstrProcessor.OutMode;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.ArrayList;

/**
 * After compiled Java classes use emma tool
 *
 * @author mariusz@saramak.eu
 * @goal emma
 * @phase prepare-package
 * @requiresDependencyResolution compile
 */
public class EmmaMojo extends AbstractAndroidMojo
{

    private static final String EMMA_FOLDER_NAME = "emma";
    private static final String CLASSES_FOLDER_NAME = "classes";
    private static final String COVERAGE_METADATA_NAME = "coverage.em";

    /**
     * Configuration for the emma command execution. It can be configured in the plugin configuration like so
     * <p/>
     * <pre>
     * &lt;emma&gt;
     *   &lt;enable&gt;true|false&lt;/enable&gt;
     *   &lt;classFolders&gt;${project}/target/classes&lt;/classFolders&gt;
     *   &lt;outputMetaFile&gt;${project}/target/emma/coverage.em&lt;/outputMetaFile&gt;
     *   &lt;filters&gt;${project}classpaths,comma separatedlt;/filters&gt;
     * &lt;/emma&gt;
     * </pre>
     * <p/>
     * or via properties emma.* or command line parameters android.emma.*
     *
     * @parameter
     */
    private Emma emma;
    /**
     * Decides whether to enable or not enable emma.
     *
     * @parameter expression="${android.emma.enable}" default-value="false"
     */
    private boolean emmaEnable;
    /**
     * Configure directory where compiled classes are.
     *
     * @parameter expression="${android.emma.classFolders}" default-value="${project.build.directory}/classes/"
     */
    private String emmaClassFolders;
    /**
     * Decides whether to enable or not enable emma.
     *
     * @parameter expression="${android.emma.outputMetaFile}"
     * default-value="${project.build.directory}/emma/coverage.es"
     */
    private File emmaOutputMetaFile;

    /**
     * Decides whether to enable or not enable emma.
     *
     * @parameter expression="${android.emma.filters}"
     */
    private String emmaFilters;

    private boolean parsedEnable;
    private String[] parsedEmmaClassFolders;
    private String parsedOutputMetadataFile;
    private String parsedFilters;
    
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        getLog().debug( "Emma start working. Before parse configuration" );
        parseConfiguration();
        if ( parsedEnable )
        {
            getLog().info( "Emma OVERWRITE compiled class is on for this project! "
                    + "Do NOT use this project on production" );
            getLog().debug( "configuration:  Class Folders - this file will be modified by emma "
                    + parsedEmmaClassFolders );
            getLog().debug( "configuration:  parsedOutputMetadataFile " + parsedOutputMetadataFile );
            InstrProcessor processor = InstrProcessor.create();
            if ( StringUtils.isNotEmpty( parsedFilters ) )
            {
                processor.setInclExclFilter( parsedFilters.split( "," ) );
            }
            processor.setInstrPath( parsedEmmaClassFolders, true );
            processor.setInstrOutDir( parsedEmmaClassFolders[ 0 ] ); //always to first define folder
            processor.setMetaOutFile( parsedOutputMetadataFile );
            processor.setOutMode( OutMode.OUT_MODE_OVERWRITE );
            processor.setMetaOutMerge( Boolean.TRUE );
            processor.run();
        }
        getLog().debug( "Emma OVERWRITE is OFF for this project (" + project.getArtifactId()
                + ") target/classes files are safe" );
    }

    private void parseConfiguration() throws MojoExecutionException
    {
        if ( emma != null )
        {
            if ( emma.isEnable() == null )
            {
                parsedEnable = emmaEnable;
            }
            else
            {
                parsedEnable = emma.isEnable();
            }
            if ( emma.getClassFolders() != null )
            {
                parsedEmmaClassFolders = getAllCompiledDirectory();
            }
            else
            {
                parsedEmmaClassFolders = getDefaultCompiledFolders();
            }
            if ( emma.getOutputMetaFile() != null )
            {
                parsedOutputMetadataFile = emma.getOutputMetaFile();
            }
            else
            {
                parsedOutputMetadataFile = getDefaultMetaDataFile();
            }
            if ( StringUtils.isNotEmpty( emma.getFilters() ) ) 
            {    
                parsedFilters = emma.getFilters();
            }
        }
        else
        {
            parsedEnable = emmaEnable;
            parsedEmmaClassFolders = new String[]{ emmaClassFolders };
            parsedOutputMetadataFile = emmaOutputMetaFile.getAbsolutePath();
            parsedFilters = emmaFilters;
        }
    }

    private String getDefaultMetaDataFile()
    {
        File outputFolder = new File( project.getBuild().getDirectory() + File.separator + EMMA_FOLDER_NAME
                + File.separator + COVERAGE_METADATA_NAME );
        return outputFolder.getAbsolutePath();
    }

    private String[] getDefaultCompiledFolders()
    {
        File sourceJavaFolder = new File(
                project.getBuild().getDirectory() + File.separator + CLASSES_FOLDER_NAME + File.separator );
        return new String[]{ sourceJavaFolder.getAbsolutePath() };
    }

    private String[] getAllCompiledDirectory() throws MojoExecutionException
    {
        String classFoldersTemp = emma.getClassFolders();
        String[] classFolders;
        if ( classFoldersTemp == null )
        {
            return new String[]{ emmaClassFolders };
        }
        else
        {
            classFolders = classFoldersTemp.split( "," );
        }
        ArrayList<String> classFoldersArray = new ArrayList<String>();
        for ( String folder : classFolders )
        {
            File directory = new File( folder );
            if ( directory.exists() && directory.isDirectory() )
            {
                classFoldersArray.add( directory.getAbsolutePath() );
            }
        }
        return classFoldersArray.toArray( new String[ 0 ] );
    }
}
