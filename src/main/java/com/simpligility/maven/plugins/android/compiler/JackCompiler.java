package com.simpligility.maven.plugins.android.compiler;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.plexus.compiler.AbstractCompiler;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerException;
import org.codehaus.plexus.compiler.CompilerMessage;
import org.codehaus.plexus.compiler.CompilerMessage.Kind;
import org.codehaus.plexus.compiler.CompilerOutputStyle;
import org.codehaus.plexus.compiler.CompilerResult;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.codehaus.plexus.util.cli.WriterStreamConsumer;

/**
 * @plexus.component role="org.codehaus.plexus.compiler.Compiler"
 * role-hint="jack"
 */
@Component( role = Compiler.class, hint = "jack" )
public class JackCompiler extends AbstractCompiler
{

    private static final Log LOG = LogFactory.getLog( JackCompiler.class );
    
    public static final String JACK_COMPILER_ID = "jack";
    
    public JackCompiler()
    {
        super( CompilerOutputStyle.ONE_OUTPUT_FILE_FOR_ALL_INPUT_FILES, ".java", ".dex", null );
    }

    @Override
    public String[] createCommandLine( CompilerConfiguration cc ) throws CompilerException
    {
        String androidHome = System.getenv( "ANDROID_HOME" );
        String jackJarPath = androidHome + "/build-tools/24.0.2/jack.jar";
        String androidJarPath = androidHome + "/platforms/android-24/android.jar";
        String command =
        
            ( "java -jar " + jackJarPath + " "
            + " -D jack.java.source.version=" + cc.getSourceVersion()
            + " --classpath " + androidJarPath
            + " --output-dex " + cc.getBuildDirectory()
            + " src/main/java/ target/generated-sources/r/" );

        LOG.debug( String.format( " jack command : %s", command ) );
        
        return trim( command.split( "\\s" ) ) ;
    }

    @Override
    public boolean canUpdateTarget( CompilerConfiguration configuration )
            throws CompilerException
    {
        return false;
    }

    @Override
    public CompilerResult performCompile( CompilerConfiguration configuration ) throws CompilerException
    {
        String[] commandLine = this.createCommandLine( configuration );
        
        List<CompilerMessage> messages = compileOutOfProcess( configuration.getWorkingDirectory(), 
                                              configuration.getBuildDirectory(), 
                                              commandLine[ 0 ], 
                                              Arrays.copyOfRange( commandLine, 1, commandLine.length )
            );


        return new CompilerResult().compilerMessages( messages );

    }

    @Override
    public String getOutputFile( CompilerConfiguration configuration ) throws CompilerException
    {
        return "classes.dex";
    }

    
    @SuppressWarnings( "deprecation" )
    private List<CompilerMessage> compileOutOfProcess( File workingDirectory, File target, String executable,
            String[] args )
            throws CompilerException
    {        // ----------------------------------------------------------------------
        // Build the @arguments file
        // ----------------------------------------------------------------------

        File file;

        PrintWriter output = null;

        try
        {
            file = new File( target, "jack-argmuents" );

            output = new PrintWriter( new FileWriter( file ) );

            for ( String arg : args )
            {
                output.println( arg );
            }
        } 
        catch ( IOException e )
        {
            throw new CompilerException( "Error writing arguments file.", e );
        } 
        finally
        {
            IOUtil.close( output );
        }

        // ----------------------------------------------------------------------
        // Execute!
        // ----------------------------------------------------------------------
        Commandline cli = new Commandline();

        cli.setWorkingDirectory( workingDirectory.getAbsolutePath() );

        cli.setExecutable( executable );

        cli.addArguments( args );
        
        Writer stringWriter = new StringWriter();

        StreamConsumer out = new WriterStreamConsumer( stringWriter );

        StreamConsumer err = new WriterStreamConsumer( stringWriter );

        int returnCode;

        List<CompilerMessage> messages;

        try
        {
            returnCode = CommandLineUtils.executeCommandLine( cli, out, err );

            messages = parseCompilerOutput( new BufferedReader( new StringReader( stringWriter.toString() ) ) );
        } 
        catch ( CommandLineException e )
        {
            throw new CompilerException( "Error while executing the external compiler.", e );
        } 
        catch ( IOException e )
        {
            throw new CompilerException( "Error while executing the external compiler.", e );
        }

        if ( returnCode != 0  )
        {            
            StringBuilder errorBuilder = new StringBuilder();
            for ( CompilerMessage message : messages ) 
            {
                errorBuilder.append( message.getMessage() );
            }
            
            
            throw new CompilerException( errorBuilder.toString() );
        }

        return messages;
    }

    public static List<CompilerMessage> parseCompilerOutput( BufferedReader bufferedReader )
            throws IOException
    {
        List<CompilerMessage> messages = new ArrayList<CompilerMessage>();

        String line = bufferedReader.readLine();

        while ( line != null )
        {
            messages.add( new CompilerMessage( line, Kind.NOTE ) );

            line = bufferedReader.readLine();
        }

        return messages;
    }

    private String[] trim( String[] split ) 
    {
        Iterable<String> filtered = Iterables.filter( 
                Arrays.asList( split ), 
                new Predicate<String>() 
                {
                    @Override
                    public boolean apply( String t ) 
                    {
                        return !Strings.isNullOrEmpty( t );
                    }
                }
        );
        
        return Iterables.toArray( filtered, String.class );
    }

}
