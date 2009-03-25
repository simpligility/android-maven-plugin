package org.jvending.masa.plugin;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class AbstractAndroidMojoTest {
    protected MyAbstractAndroidMojo androidMojo;

    @Before
    public void setUp() throws Exception {
        androidMojo = new MyAbstractAndroidMojo();
    }

    @Test
    public void givenAndroidManifestThenTargetPackageIsFound() throws MalformedURLException, URISyntaxException, MojoExecutionException {
        final URL    url                = this.getClass().getResource("AndroidManifest.xml");
        final URI    uri                = url.toURI();
        final File   file               = new File(uri);
        final String foundTargetPackage = androidMojo.extractPackageNameFromAndroidManifest(file);
        Assert.assertEquals("com.example.android.apis.tests", foundTargetPackage);
    }

    @Test
    public void givenValidAndroidManifestXmlTreeThenPackageIsFound() throws IOException {
        final URL         resource               = this.getClass().getResource("AndroidManifestXmlTree.txt");
        final InputStream inputStream            = resource.openStream();
        final String      androidManifestXmlTree = IOUtils.toString(inputStream);
        final String      foundPackage           = androidMojo.extractPackageNameFromAndroidManifestXmlTree(androidManifestXmlTree);
        Assert.assertEquals("com.example.android.apis", foundPackage);
    }

    @Test
    public void givenApidemosApkThenPackageIsFound() throws IOException, MojoExecutionException {
        final URL    resource     = this.getClass().getResource("apidemos-0.1.0-SNAPSHOT.apk");
        final String foundPackage = androidMojo.extractPackageNameFromApk(new File(resource.getFile()));
        Assert.assertEquals("com.example.android.apis", foundPackage);
    }

    @Test
    public void givenApidemosPlatformtestsApkThenPackageIsFound() throws IOException, MojoExecutionException {
        final URL    resource     = this.getClass().getResource("apidemos-platformtests-0.1.0-SNAPSHOT.apk");
        final String foundPackage = androidMojo.extractPackageNameFromApk(new File(resource.getFile()));
        Assert.assertEquals("com.example.android.apis.tests", foundPackage);
    }

    private static class MyAbstractAndroidMojo extends AbstractAndroidMojo {
        public void execute() throws MojoExecutionException, MojoFailureException {

        }
    }
}
