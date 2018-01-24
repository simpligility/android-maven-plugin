package com.simpligility.maven.plugins.android.phase01generatesources;

import com.android.builder.core.DefaultManifestParser;
import com.android.builder.symbols.RGeneration;
import com.android.builder.symbols.SymbolIo;
import com.android.builder.symbols.SymbolTable;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Generates R classes containing appropriate resource values for dependent libraries.
 *
 * @author William Ferguson - william.ferguson@xandar.com.au
 */
final class ResourceClassGenerator
{
    private final GenerateSourcesMojo mojo;
    private final File targetDirectory;
    private final File genDirectory;
    private final Log log;
    private final ClassLoader compileClassLoader;

    ResourceClassGenerator( final GenerateSourcesMojo mojo,
                            final File targetDirectory,
                            final File genDirectory,
                            final ClassLoader compileClassLoader
    )
    {
        this.mojo = mojo;
        this.targetDirectory = targetDirectory;
        this.genDirectory = genDirectory;
        this.log = mojo.getLog();
        this.compileClassLoader = compileClassLoader;
    }

    /**
     * Generates R java files those libraries that do not already have their R java in the compile classpath.
     *
     * If we are generating an integration test APK and the APK under test has a reference to a library for which
     * it generated an R java, then we don't want to generate that R java again for the test APK because otherwise
     * Proguard will fail when building the test APK because of duplication of the R java in the compile classpath.
     *
     * See {@link com.android.builder.core.AndroidBuilder#processResources(com.android.builder.internal.aapt.Aapt,
     * com.android.builder.internal.aapt.AaptPackageConfig.Builder, boolean)}
     *
     * @param libraries AAR libraries for which to generate R java files.
     */
    public void generateLibraryRs( final Set<Artifact> libraries )
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
                if ( rJavaAlreadyExists( packageName ) )
                {
                    log.info( "Not creating R for " + packageName + " as it already exists" );
                    continue;
                }
                log.info( "Generating R for " + packageName + " at " + rFile );

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

    private boolean rJavaAlreadyExists( String packageName )
    {
        final String rJavaClass = packageName + ".R";
        try
        {
            compileClassLoader.loadClass( rJavaClass );
            return true;
        }
        catch ( ClassNotFoundException e )
        {
            log.debug( "Could not resolve R java : " + rJavaClass );
            return false;
        }
    }
}
