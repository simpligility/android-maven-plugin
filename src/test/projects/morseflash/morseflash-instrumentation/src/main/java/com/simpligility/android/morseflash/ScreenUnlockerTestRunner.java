package com.simpligility.android.morseflash;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.PowerManager;
import android.support.test.runner.AndroidJUnitRunner;

import static android.content.Context.KEYGUARD_SERVICE;
import static android.content.Context.POWER_SERVICE;
import static android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP;
import static android.os.PowerManager.FULL_WAKE_LOCK;
import static android.os.PowerManager.ON_AFTER_RELEASE;

/**
 * Instrumentation which unlocks the screen before running any test.
 * This needs the following permissions in the <b>application</b> manifest:
 *
 * <pre>
 *     <uses-permission android:name="android.permission.DISABLE_KEYGUARD"/>
 *     <uses-permission android:name="android.permission.WAKE_LOCK"/>
 * </pre>
 *
 * Adapted from https://github.com/JakeWharton/u2020/blob/master/src/androidTestInternal/java/com/jakewharton/u2020/U2020TestRunner.java.
 */
public final class ScreenUnlockerTestRunner extends AndroidJUnitRunner {

    @Override
    public void onStart() {
        runOnMainSync(new Runnable() {
            @SuppressWarnings("deprecation")
            // We don't care about deprecation here.
            public void run() {
                Context app = getTargetContext().getApplicationContext();

                String name = ScreenUnlockerTestRunner.class.getSimpleName();
                // Unlock the device so that the tests can input keystrokes.
                KeyguardManager keyguard = (KeyguardManager) app.getSystemService(KEYGUARD_SERVICE);
                keyguard.newKeyguardLock(name).disableKeyguard();
                // Wake up the screen.
                PowerManager power = (PowerManager) app.getSystemService(POWER_SERVICE);
                power.newWakeLock(FULL_WAKE_LOCK | ACQUIRE_CAUSES_WAKEUP | ON_AFTER_RELEASE, name).acquire();
            }
        });

        super.onStart();
    }
}