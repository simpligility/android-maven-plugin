package com.simpligility.maven.plugins.android.common.aapt;

import com.simpligility.maven.plugins.android.AndroidSdk;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Collates commands used to invoke Aapt.
 *
 * @author Oleg Green
 * @author William Ferguson
 * @author Manfred Moser
 */
public class AaptCommandBuilder
{
    protected final AndroidSdk androidSdk;
    protected final List<String> commands;
    protected final Log log;

    protected AaptCommandBuilder( AndroidSdk androidSdk, Log log )
    {
        this.androidSdk = androidSdk;
        this.log = log;
        this.commands = new ArrayList<>();
    }

    /**
     * @return Path to the version of AAPT to use. This will be AAPT2 if it is available otherwise AAPT.
     */
    public String getApplicationPath()
    {
        final String aaptPath = androidSdk.getAaptPath();
        final String aapt2Path = androidSdk.getAapt2Path();
        return aapt2Exists( androidSdk ) ? aapt2Path : aaptPath;
    }

    /**
     * @return true if the aapt2 executable exists.
     */
    private static boolean aapt2Exists( AndroidSdk androidSdk )
    {
        final String aapt2Path = androidSdk.getAapt2Path();
        return new File( aapt2Path ).exists();
    }

    /**
     * Compile the android resources.
     *
     * @return instance of {@link AaptPackageCommandBuilder}
     */
    public static AaptCompileCommandBuilder compileResources( AndroidSdk androidSdk, Log log )
    {
        return aapt2Exists( androidSdk )
                ? new Aapt2CompileCommandBuilder( androidSdk, log )
                : new AaptPackageCommandBuilder( androidSdk, log );
    }

    /**
     * Linig (and package) the android resources.
     *
     * @return instance of {@link AaptPackageCommandBuilder}
     */
    public static AaptLinkCommandBuilder linkResources( AndroidSdk androidSdk, Log log )
    {
        return aapt2Exists( androidSdk )
                ? new Aapt2LinkCommandBuilder( androidSdk, log )
                : new AaptPackageCommandBuilder( androidSdk, log );
    }

    /**
     * Dump label, icon, permissions, compiled xmls etc.
     *
     * @return instance of {@link AaptDumpCommandBuilder}
     */
    public static AaptDumpCommandBuilder dump( AndroidSdk androidSdk, Log log )
    {
        return new AaptDumpCommandBuilder( androidSdk, log );
    }

    @Override
    public String toString()
    {
        return commands.toString();
    }

    /**
     * Provides unmodifiable list of a aapt commands
     *
     * @return unmodifiable {@link List} of {@link String} commands
     */
    public List<String> build()
    {
        return Collections.unmodifiableList( commands );
    }
}
