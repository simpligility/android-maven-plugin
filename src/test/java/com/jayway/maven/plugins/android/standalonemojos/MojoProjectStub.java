package com.jayway.maven.plugins.android.standalonemojos;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Resource;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.junit.Assert;

/**
 * Basic MavenProject implementation that can be used for testing.
 */
public class MojoProjectStub extends MavenProjectStub {
    private File basedir;
    private Build build;

    public MojoProjectStub(File projectDir) {
        this.basedir = projectDir;

        File pom = new File(getBasedir(), "plugin-config.xml");
        MavenXpp3Reader pomReader = new MavenXpp3Reader();
        Model model = null;
        FileReader fileReader = null;

        try {
            fileReader = new FileReader(pom);
            model = pomReader.read(fileReader);
            setModel(model);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(fileReader);
        }

        setGroupId(model.getGroupId());
        setArtifactId(model.getArtifactId());
        setVersion(model.getVersion());
        setName(model.getName());
        setUrl(model.getUrl());
        setPackaging(model.getPackaging());
        setFile(pom);

        build = model.getBuild();
        if(build == null) {
            build = new Build();
        }

        File srcDir = getStandardDir(getBuild().getSourceDirectory(), "src/main/java");
        getBuild().setSourceDirectory(srcDir.getAbsolutePath());

        File targetDir = getStandardDir(getBuild().getDirectory(), "target");
        getBuild().setDirectory(targetDir.getAbsolutePath());

        File outputDir = getStandardDir(getBuild().getOutputDirectory(), "target/classes");
        getBuild().setOutputDirectory(outputDir.getAbsolutePath());

        List<Resource> resources = new ArrayList<Resource>();
        resources.addAll(getBuild().getResources());

        if (resources.isEmpty()) {
            resources = new ArrayList<Resource>();
            Resource resource = new Resource();
            File resourceDir = normalize("src/main/resources");
            resource.setDirectory(resourceDir.getAbsolutePath());
            makeDirs(resourceDir);
            resources.add(resource);
        } else {
            // Make this project work on windows ;-)
            for (Resource resource : resources) {
                File dir = normalize(resource.getDirectory());
                resource.setDirectory(dir.getAbsolutePath());
            }
        }

        getBuild().setResources(resources);
    }
    
    @Override
    public Build getBuild() {
        return this.build;
    }

    private File getStandardDir(String dirPath, String defaultPath) {
        File dir;

        if (StringUtils.isBlank(dirPath)) {
            dir = normalize(defaultPath);
        } else {
            dir = normalize(dirPath);
        }

        makeDirs(dir);
        return dir;
    }
    
    /**
     * Normalize a path.
     * <p>
     * Ensure path is absolute, and has proper system file separators.
     * 
     * @param path the raw path.
     * @return
     */
    private File normalize(final String path) {
        String ospath = FilenameUtils.separatorsToSystem(path);
        File file = new File(ospath);
        if(file.isAbsolute()) {
           return file; 
        } else {
            return new File(getBasedir(), ospath);
        }
    }

    private void makeDirs(File dir) {
        if (dir.exists()) {
            return;
        }

        Assert.assertTrue("Unable to make directories: " + dir, dir.mkdirs());
    }

    @Override
    public File getBasedir() {
        return this.basedir;
    }
}
