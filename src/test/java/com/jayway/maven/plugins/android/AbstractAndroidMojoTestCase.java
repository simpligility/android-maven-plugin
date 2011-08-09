package com.jayway.maven.plugins.android;

import java.io.File;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.DebugConfigurationListener;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.path.DefaultPathTranslator;
import org.apache.maven.project.path.PathTranslator;
import org.codehaus.plexus.component.MapOrientedComponent;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;
import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.component.repository.ComponentRequirement;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.Assert;

import com.jayway.maven.plugins.android.standalonemojos.MojoProjectStub;
import com.jayway.maven.plugins.android.standalonemojos.VersionUpdateMojo;

import static org.easymock.EasyMock.*;

public abstract class AbstractAndroidMojoTestCase<T extends AbstractAndroidMojo> extends AbstractMojoTestCase {
    /**
     * The Goal Name of the Plugin being tested.
     * <p>
     * Used to look for <code>&lt;configuration&gt;</code> section in the <code>plugin-config.xml</code> 
     * that will be used to configure the mojo from.
     * 
     * @return the string name of the goal. (eg "version-update", "dex", etc...)
     */
    public abstract String getPluginGoalName();

    /**
     * Copy the project specified into a temporary testing directory. Create the {@link MavenProject} and
     * {@link VersionUpdateMojo}, configure it from the <code>plugin-config.xml</code> and return the created Mojo.
     * <p>
     * Note: only configuration entries supplied in the plugin-config.xml are presently configured in the mojo returned.
     * That means and 'default-value' settings are not automatically injected by this testing framework (or plexus
     * underneath that is suppling this functionality)
     * 
     * @param resourceProject
     *            the name of the goal to look for in the <code>plugin-config.xml</code> that the configuration will be
     *            pulled from.
     * @param resourceProject
     *            the resourceProject path (in src/test/resources) to find the example/test project.
     * @return the created mojo (unexecuted)
     * @throws Exception
     *             if there was a problem creating the mojo.
     */
    protected T createMojo(String resourceProject) throws Exception {
        // Establish test details project example
        String testResourcePath = "src/test/resources/" + resourceProject;
        testResourcePath = FilenameUtils.separatorsToSystem(testResourcePath);
        File exampleDir = new File(getBasedir(), testResourcePath);
        Assert.assertTrue("Path should exist: " + exampleDir, exampleDir.exists());

        // Establish the temporary testing directory.
        String testingPath = "target/tests/" + this.getClass().getSimpleName() + "." + getName();
        testingPath = FilenameUtils.separatorsToSystem(testingPath);
        File testingDir = new File(getBasedir(), testingPath);

        if (testingDir.exists()) {
            FileUtils.cleanDirectory(testingDir);
        } else {
            Assert.assertTrue("Could not create directory: " + testingDir, testingDir.mkdirs());
        }

        // Copy project example into temporary testing directory
        // to avoid messing up the good source copy, as mojo can change
        // the AndroidManifest.xml file.
        FileUtils.copyDirectory(exampleDir, testingDir);

        // Prepare MavenProject
        final MavenProject project = new MojoProjectStub(testingDir);

        // Setup Mojo
        PlexusConfiguration config = extractPluginConfiguration("maven-android-plugin", project.getFile());
        @SuppressWarnings("unchecked")
        final T mojo = (T) lookupMojo(getPluginGoalName(), project.getFile());

        // Inject project itself
        setVariableValueToObject(mojo, "project", project);

        // Configure the rest of the pieces via the PluginParameterExpressionEvaluator
        //  - used for ${plugin.*}
        MojoDescriptor mojoDesc = new MojoDescriptor();
        // - used for error messages in PluginParameterExpressionEvaluator
        mojoDesc.setGoal(getPluginGoalName());
        MojoExecution mojoExec = new MojoExecution(mojoDesc);
        // - Only needed if we start to use expressions like ${settings.*}, ${localRepository}, ${reactorProjects}
        // MavenSession context = null; // Messy to declare, would rather avoid using it.
        // - Used for ${basedir} relative paths
        PathTranslator pathTranslator = new DefaultPathTranslator();
        // - Declared to prevent NPE from logging events in maven core
        Logger logger = new ConsoleLogger(Logger.LEVEL_DEBUG, mojo.getClass().getName());

         MavenSession context = createMock(MavenSession.class);

        expect(context.getExecutionProperties()).andReturn(project.getProperties());
        expect(context.getCurrentProject()).andReturn(project);
        replay(context);

        // Declare evalator that maven itself uses.
        ExpressionEvaluator evaluator = new PluginParameterExpressionEvaluator(
                context, mojoExec, pathTranslator, logger, project, project.getProperties());
        // Lookup plexus configuration component
        ComponentConfigurator configurator = (ComponentConfigurator) lookup(ComponentConfigurator.ROLE);
        // Configure mojo using above
        ConfigurationListener listener = new DebugConfigurationListener( logger );
        configurator.configureComponent( new MapOrientedComponent() {
            public void addComponentRequirement(ComponentRequirement componentRequirement, Object o) throws ComponentConfigurationException {
                System.out.println("componentRequirement = " + componentRequirement);
            }

            public void setComponentConfiguration(Map map) throws ComponentConfigurationException {
                for (Object o : map.keySet()) {
                    String key = (String) o;
                    Object value = map.get(o);
                    try {
                        // FIXME: This is a terrible hack in order to initialize the VersioMojo
                        // FIXME: Should really be removed and the proper plexus configurator
                        // FIXME: should be used instead.  But, for now, since the component
                        // FIXME: found does not support our MOJO, this will have to do.
                        if ("androidManifestFile".equals(key))
                        {
                            setVariableValueToObject(mojo, key, new File((String)value));
                        }
                        else
                        {
                            setVariableValueToObject(mojo, key, Boolean.parseBoolean((String) value));
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, config, evaluator, getContainer().getContainerRealm(), listener );

        return mojo;
    }



    /**
     * Get the project directory used for this mojo.
     * 
     * @param mojo the mojo to query.
     * @return the project directory.
     * @throws IllegalAccessException if unable to get the project directory.
     */
    public File getProjectDir(AbstractAndroidMojo mojo) throws IllegalAccessException {
        MavenProject project = (MavenProject) getVariableValueFromObject(mojo, "project");
        return project.getFile().getParentFile();
    }
}
