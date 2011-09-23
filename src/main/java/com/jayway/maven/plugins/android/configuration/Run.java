package com.jayway.maven.plugins.android.configuration;

/**
 * Configuration for the Run goal.
 *
 * @author Manfred Moser <manfred@simpligility.com>
 * @see com.jayway.maven.plugins.android.standalonemojos.RunMojo
 */
public class Run {

    /**
      * If true, the device or emulator will pause execution of the process at
      * startup to wait for a debugger to connect.
      *
      * @parameter expression="${android.run.debug}" default-value="false"
      */
    protected boolean debug;

    public boolean isDebug() {
        return debug;
    }
}
