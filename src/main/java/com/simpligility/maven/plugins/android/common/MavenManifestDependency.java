package com.simpligility.maven.plugins.android.common;

import com.android.builder.dependency.ManifestDependency;

import java.io.File;
import java.util.List;

public class MavenManifestDependency implements ManifestDependency
{
    private File manifestFile;
    private String name;
    private List<MavenManifestDependency> manifestDependencies;

    public MavenManifestDependency(
            File manifestFile, String name, List<MavenManifestDependency> manifestDependencies )
    {
        this.manifestFile = manifestFile;
        this.name = name;
        this.manifestDependencies = manifestDependencies;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public List<? extends ManifestDependency> getManifestDependencies()
    {
        return manifestDependencies;
    }

    @Override
    public File getManifest()
    {
        return manifestFile;
    }
}
