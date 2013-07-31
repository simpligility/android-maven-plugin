package com.jayway.maven.plugins.android.common;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.DefaultArtifactFactory;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.SilentLog;
import org.apache.maven.plugin.testing.stubs.ArtifactStub;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonatype.aether.impl.internal.DefaultRepositorySystem;
import org.sonatype.aether.repository.RemoteRepository;

import java.io.File;
import java.util.Collections;
import java.util.Set;

/**
 * @author Johan Lindquist
 */
public class NativeHelperTest {

    @Rule
    public TemporaryFolder apklibDir = new TemporaryFolder();

    private NativeHelper nativeHelper;

    @Before
    public void setupNativeHelper() {
        MavenProject project = new MavenProject();
        project.setDependencyArtifacts(Collections.<Artifact>emptySet());

        ArtifactStub apklib = new ArtifactStub() {
            @Override
            public String getId() {
                return getArtifactId();
            }
        };
        apklib.setArtifactId("some-apklib");
        apklib.setGroupId("group");
        apklib.setType(AndroidExtension.APKLIB);
        project.addAttachedArtifact(apklib);

        nativeHelper = new NativeHelper(project, Collections.<RemoteRepository>emptyList(),
                new MavenRepositorySystemSession(), new DefaultRepositorySystem(),
                new DefaultArtifactFactory(), new SilentLog());
    }

    @Test
    public void invalidVersions()
    {
        String[] versions = {"r4", "r5", "r5b", "r5c", "r6", "r6b"};

        for (int i = 0; i < versions.length; i++) {
            String version = versions[i];
            try {
                NativeHelper.validateNDKVersion(7,version);
                Assert.fail("Version should fail: " + version);
            } catch (MojoExecutionException e) {
            }
        }
    }

    @Test
    public void validVersions()
    {
        String[] versions = {"r7", "r8a", "r8z", "r10", "r19b", "r25", "r100", "r100b"};

        for (int i = 0; i < versions.length; i++) {
            String version = versions[i];
            try {
                NativeHelper.validateNDKVersion(7, version);
            } catch (MojoExecutionException e) {
                Assert.fail("Version should not fail: " + version);
            }
        }
    }

    @Test
    public void shouldNotIncludeLibsFolderAsNativeDependenciesSourceWhenNoNativeLibsInside() throws Exception {
        new File(apklibDir.getRoot(), "some-apklib/libs").mkdirs();
        new File(apklibDir.getRoot(), "some-apklib/libs/some.jar").createNewFile();

        Set<Artifact> nativeDependencies = nativeHelper.getNativeDependenciesArtifacts(apklibDir.getRoot(), true);

        assertTrue("Included JARs as native dependencies, but shouldn't", nativeDependencies.isEmpty());
    }

    @Test
    public void shouldIncludeLibsFolderAsNativeDependenciesSourceWhenNativeLibsInside() throws Exception {
        new File(apklibDir.getRoot(), "some-apklib/libs").mkdirs();
        new File(apklibDir.getRoot(), "some-apklib/libs/some.jar").createNewFile();
        new File(apklibDir.getRoot(), "some-apklib/libs/some.so").createNewFile();

        Set<Artifact> nativeDependencies = nativeHelper.getNativeDependenciesArtifacts(apklibDir.getRoot(), true);

        assertFalse("Excluded native dependencies, but shouldn't", nativeDependencies.isEmpty());
    }
}
