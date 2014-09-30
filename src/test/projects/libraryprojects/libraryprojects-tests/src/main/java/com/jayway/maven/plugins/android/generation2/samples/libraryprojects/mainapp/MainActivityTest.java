package com.jayway.maven.plugins.android.generation2.samples.libraryprojects.mainapp;

import android.test.ActivityInstrumentationTestCase2;
import com.robotium.solo.Solo;

/**
 * Tests that {@link MainActivity} displays correct data from its libraries.
 *
 * @author hugo.josefson@jayway.com
 */
public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private Solo robotium;

    public MainActivityTest() {
        super("com.jayway.maven.plugins.android.generation2.samples.libraryprojects.mainapp", MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        robotium = new Solo(getInstrumentation(), getActivity());
    }

    public void testFrameworkLaunchesAtAll() {
    }

    public void testMainActivityAppears() {
        assertTrue(robotium.waitForActivity("MainActivity", 20000));
    }


    public void testAndroidResourcesAppear() {
        assertTrue(robotium.searchText("libraryprojects-apk-with-deps Android resource"));
        assertTrue(robotium.searchText("libraryprojects-aar1 Android resource"));
        assertTrue(robotium.searchText("libraryprojects-apklib1 Android resource"));
        assertTrue(robotium.searchText("libraryprojects-aar-from-aar Android resource"));
        assertTrue(robotium.searchText("libraryprojects-apklib-from-apklib Android resource"));
    }

    public void testAndroidAssetsAppear() {
        assertTrue(robotium.searchText("libraryprojects-apk-with-deps Android asset"));
        assertTrue(robotium.searchText("libraryprojects-aar1 Android asset"));
        assertTrue(robotium.searchText("libraryprojects-apklib1 Android asset"));
        assertTrue(robotium.searchText("libraryprojects-aar-from-aar Android asset"));
        assertTrue(robotium.searchText("libraryprojects-apklib-from-apklib Android asset"));
    }

    public void testJavaClassesAppear() {
        assertTrue(robotium.searchText("libraryprojects-apk-with-deps Java class"));
        assertTrue(robotium.searchText("libraryprojects-aar1 Java class"));
        assertTrue(robotium.searchText("libraryprojects-apklib1 Java class"));
        assertTrue(robotium.searchText("libraryprojects-aar-from-aar Java class"));
        assertTrue(robotium.searchText("libraryprojects-apklib-from-apklib Java class"));
    }
    
    public void testJavaResourcesAppear() {
        assertTrue(robotium.searchText("libraryprojects-apk-with-deps Java resource"));
        assertTrue(robotium.searchText("libraryprojects-aar1 Java resource"));
        assertTrue(robotium.searchText("libraryprojects-apklib1 Java resource"));
        assertTrue(robotium.searchText("libraryprojects-aar-from-aar Java resource"));
        assertTrue(robotium.searchText("libraryprojects-apklib-from-apklib Java resource"));
    }

    /**
     * Finalizes the Robotium Solo instance, as recommended by Robotium's Getting Started guide.
     *
     * @throws Exception if {@code super.tearDown()} does.
     */
    @Override
    public void tearDown() throws Exception {
        try {
            robotium.finalize();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        getActivity().finish();
        super.tearDown();
    }

}
