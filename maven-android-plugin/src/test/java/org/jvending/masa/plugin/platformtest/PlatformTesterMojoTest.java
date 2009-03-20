package org.jvending.masa.plugin.platformtest;

import org.junit.Test;
import org.junit.Assert;

import java.net.URL;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.io.File;

public class PlatformTesterMojoTest {
    @Test
    public void givenAndroidManifestThenTargetPackageIsFound() throws MalformedURLException, URISyntaxException {
        final URL url = this.getClass().getResource("AndroidManifest.xml");
        final String foundTargetPackage = new PlatformTesterMojo().extractPackage(new File(url.toURI()));
        Assert.assertEquals(foundTargetPackage, "com.example.android.apis.tests");
    }
}
