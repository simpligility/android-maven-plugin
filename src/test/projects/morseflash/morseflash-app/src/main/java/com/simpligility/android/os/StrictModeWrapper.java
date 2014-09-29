package com.simpligility.android.os;

import android.os.StrictMode;

/**
 * StrictModeWrapper is a wrapper class for the android class android.os.StrictMode provided with Android 2.3 onwards.
 * It allows usage of StrictMode class on devices/emulators with Android 2.3 or higher, while providing an availability
 * check so that the code can stay in situ for lower platform versions. See the application class for usage.
 *
 * @author Manfred Moser <manfred@simpligility.com>
 *
 * @see "http://android-developers.blogspot.com/2009/04/backward-compatibility-for-android.html"
 * @see "http://android-developers.blogspot.com/2010/12/new-gingerbread-api-strictmode.html"
 */
public class StrictModeWrapper {

    /* class initialization fails when this throws an exception */
   static {
       try {
           Class.forName("android.os.StrictMode", true, Thread.currentThread().getContextClassLoader());
       } catch (Exception ex) {
           throw new RuntimeException(ex);
       }
   }

    /**
     * Check if the class android.os.StrictMode is available at runtime.
     */
   public static void checkAvailable() {}


    /**
     * Call StrictMode.enableDefaults().
     */
    public static void enableDefaults() {
       StrictMode.enableDefaults();
    }

    // all the implementation below is not tested but it should work ... feel free to check and send me fixes..
    public static  void setThreadPolicy(android.os.StrictMode.ThreadPolicy policy) {
        StrictMode.setThreadPolicy(policy);
    }

    public static  android.os.StrictMode.ThreadPolicy getThreadPolicy() {
        return StrictMode.getThreadPolicy();
    }

    public static  android.os.StrictMode.ThreadPolicy allowThreadDiskWrites() {
        return StrictMode.allowThreadDiskWrites();
    }

    public static  android.os.StrictMode.ThreadPolicy allowThreadDiskReads() {
        return StrictMode.allowThreadDiskReads();
    }

    public static  void setVmPolicy(android.os.StrictMode.VmPolicy policy) {
        StrictMode.setVmPolicy(policy);
    }

    public static android.os.StrictMode.VmPolicy getVmPolicy() {
        return StrictMode.getVmPolicy();
    }

    public static final class ThreadPolicyWrapper
    {
        public static final class BuilderWrapper
        {
            StrictMode.ThreadPolicy.Builder builderInstance;

            public  BuilderWrapper() {
                    builderInstance = new StrictMode.ThreadPolicy.Builder();
            }

            public  BuilderWrapper(android.os.StrictMode.ThreadPolicy policy) {
                    builderInstance = new StrictMode.ThreadPolicy.Builder(policy);
            }

            public android.os.StrictMode.ThreadPolicy.Builder detectAll() {
                return builderInstance.detectAll();
            }

            public android.os.StrictMode.ThreadPolicy.Builder permitAll() {
                return builderInstance.permitAll();
            }

            public android.os.StrictMode.ThreadPolicy.Builder detectNetwork() {
                return builderInstance.detectNetwork();
            }

            public android.os.StrictMode.ThreadPolicy.Builder permitNetwork() {
                return builderInstance.permitNetwork();
            }

            public android.os.StrictMode.ThreadPolicy.Builder detectDiskReads() {
                return builderInstance.detectDiskReads();
            }

            public android.os.StrictMode.ThreadPolicy.Builder permitDiskReads() {
                return builderInstance.permitDiskReads();
            }

            public android.os.StrictMode.ThreadPolicy.Builder detectDiskWrites() {
                return builderInstance.detectDiskWrites();
            }

            public android.os.StrictMode.ThreadPolicy.Builder permitDiskWrites() {
                return builderInstance.permitDiskWrites();
            }

            public android.os.StrictMode.ThreadPolicy.Builder penaltyDialog() {
                return builderInstance.penaltyDialog();
            }

            public android.os.StrictMode.ThreadPolicy.Builder penaltyDeath() {
                return builderInstance.penaltyDeath();
            }

            public android.os.StrictMode.ThreadPolicy.Builder penaltyLog() {
                return builderInstance.penaltyLog();
            }

            public android.os.StrictMode.ThreadPolicy.Builder penaltyDropBox() {
                return builderInstance.penaltyDropBox();
            }

            public android.os.StrictMode.ThreadPolicy build() {
                return builderInstance.build();
            }
        }
    }

    public static final class VmPolicyWrapper {
        public static final class BuilderWrapper {
            private StrictMode.VmPolicy.Builder builderInstance;

            public BuilderWrapper() {
                builderInstance = new StrictMode.VmPolicy.Builder();
            }

            public android.os.StrictMode.VmPolicy.Builder detectAll() {
                return builderInstance.detectAll();
            }

            public android.os.StrictMode.VmPolicy.Builder detectLeakedSqlLiteObjects() {
                return builderInstance.detectLeakedSqlLiteObjects();
            }

            public android.os.StrictMode.VmPolicy.Builder penaltyDeath() {
                return builderInstance.penaltyDeath();
            }

            public android.os.StrictMode.VmPolicy.Builder penaltyLog() {
                return builderInstance.penaltyLog();
            }

            public android.os.StrictMode.VmPolicy.Builder penaltyDropBox() {
                return builderInstance.penaltyDropBox();
            }

            public android.os.StrictMode.VmPolicy build() {
                return builderInstance.build();
            }
        }
    }
    // add more wrapping as desired..
}
