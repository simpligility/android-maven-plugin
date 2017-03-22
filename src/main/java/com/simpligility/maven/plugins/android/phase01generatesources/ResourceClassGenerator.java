package com.simpligility.maven.plugins.android.phase01generatesources;

import com.android.builder.core.DefaultManifestParser;
import com.android.builder.symbols.RGeneration;
import com.android.builder.symbols.SymbolIo;
import com.android.builder.symbols.SymbolTable;
import com.android.utils.ILogger;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Generates R classes containing appropriate resource values for dependent libraries.
 *
 * @author William Ferguson <william.ferguson@xandar.com.au>
 */
final class ResourceClassGenerator
{
    private final GenerateSourcesMojo mojo;
    private final File targetDirectory;
    private final File genDirectory;
    private final Log log;
    private final ILogger androidUtilsLog;

    ResourceClassGenerator( final GenerateSourcesMojo mojo, final File targetDirectory,
                            final File genDirectory )
    {
        this.mojo = mojo;
        this.targetDirectory = targetDirectory;
        this.genDirectory = genDirectory;
        this.log = mojo.getLog();
        this.androidUtilsLog = new MavenILogger( log );
    }

    /**
     * see {@link com.android.builder.core.AndroidBuilder#processResources(com.android.builder.internal.aapt.Aapt,
     * com.android.builder.internal.aapt.AaptPackageConfig.Builder, boolean)}
     *
     * @param libraries
     * @throws MojoExecutionException
     */
    public void generateLibraryRs( final Set<Artifact> libraries ) throws MojoExecutionException
    {
        // list of all the symbol tables
        final List<SymbolTable> symbolTables = new ArrayList<>( libraries.size() );

        // For each dependency, load its symbol file.
        for ( final Artifact lib : libraries )
        {
            final File unpackedLibDirectory = mojo.getUnpackedLibFolder( lib );
            final File rFile = new File( unpackedLibDirectory, "R.txt" );

            if ( rFile.isFile() )
            {
                final File libManifestFile = new File( unpackedLibDirectory, "AndroidManifest.xml" );
                final String packageName = new DefaultManifestParser( libManifestFile ).getPackage();
                log.info( "Reading R for " + packageName + " at " + rFile );

                SymbolTable libSymbols = SymbolIo.read( rFile );
                libSymbols = libSymbols.rename( packageName, libSymbols.getTableName() );
                symbolTables.add( libSymbols );
            }
        }

        if ( symbolTables.isEmpty() )
        {
            return;
        }

        // load the full resources values from the R.txt calculated for the project.
        final File projectR = new File( targetDirectory, "R.txt" );
        final SymbolTable mainSymbols = SymbolIo.read( projectR );

        // now loop on all the package name, merge all the symbols to write, and write them
        RGeneration.generateRForLibraries( mainSymbols, symbolTables, genDirectory.getAbsoluteFile(), false );
    }

}
