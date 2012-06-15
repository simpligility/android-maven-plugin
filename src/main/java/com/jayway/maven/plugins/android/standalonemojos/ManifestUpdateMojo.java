package com.jayway.maven.plugins.android.standalonemojos;

import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import com.jayway.maven.plugins.android.common.AndroidExtension;
import com.jayway.maven.plugins.android.common.XmlHelper;
import com.jayway.maven.plugins.android.configuration.Manifest;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Updates various version attributes present in the <code>AndroidManifest.xml</code> file.
 *
 * @author joakim@erdfelt.com
 * @author nic.strong@gmail.com
 * @author Manfred Moser <manfred@simpligility.com>
 * @goal manifest-update
 * @requiresProject true
 * @phase process-resources
 *
 */
public class ManifestUpdateMojo extends AbstractAndroidMojo {
    // basic attributes
    private static final String ATTR_VERSION_NAME        = "android:versionName";
	private static final String ATTR_VERSION_CODE        = "android:versionCode";
	private static final String ATTR_SHARED_USER_ID      = "android:sharedUserId";
	private static final String ATTR_DEBUGGABLE          = "android:debuggable";

    // supports-screens attributes
	private static final String ATTR_SCREEN_DENSITY      = "android:screenDensity";
    private static final String ATTR_SCREEN_SIZE         = "android:screenSize";

    // compatible-screens attributes
    private static final String ATTR_ANY_DENSITY         = "android:anyDensity";
    private static final String ATTR_SMALL_SCREENS       = "android:smallScreens";
    private static final String ATTR_NORMAL_SCREENS      = "android:normalScreens";
    private static final String ATTR_LARGE_SCREENS       = "android:largeScreens";
    private static final String ATTR_XLARGE_SCREENS      = "android:xlargeScreens";
    private static final String ATTR_RESIZEABLE          = "android:resizeable";
    private static final String ATTR_REQUIRES_SMALLEST_WIDTH_DP = "android:requiresSmallestWidthDp";
    private static final String ATTR_LARGEST_WIDTH_LIMIT_DP     = "android:largestWidthLimitDp";
    private static final String ATTR_COMPATIBLE_WIDTH_LIMIT_DP  = "android:compatibleWidthLimitDp";

	private static final String ELEM_APPLICATION         = "application";
    private static final String ELEM_SUPPORTS_SCREENS    = "supports-screens";
    private static final String ELEM_COMPATIBLE_SCREENS  = "compatible-screens";
    private static final String ELEM_SCREEN              = "screen";

    /**
     * Configuration for the manifest-update goal.
     * <p>
     * You can configure this mojo to update the following basic manifest attributes:
     * </p>
     * <p>
     * <code>android:versionName</code> on the <code>manifest</code> element.
     * <code>android:versionCode</code> on the <code>manifest</code> element.
     * <code>android:sharedUserId</code> on the <code>manifest</code> element.
     * <code>android:debuggable</code> on the <code>application</code> element.
     * </p>
     * <p>
     * Moreover, you may specify custom values for the <code>supports-screens</code> and
     * <code>compatible-screens</code> elements. This is useful if you're using custom build
     * profiles to build APKs tailored to specific screen configurations. Values passed via POM
     * configuration for these elements will be merged with whatever is found in the Manifest file.
     * Values defined in the POM will take precedence.
     * </p>
     * <p>
     * Note: This process will reformat the <code>AndroidManifest.xml</code> per JAXP
     * {@link Transformer} defaults if updates are made to the manifest.
     * <p>
     * You can configure attributes in the plugin configuration like so
     * 
     * <pre>
     *   &lt;plugin&gt;
     *     &lt;groupId&gt;com.jayway.maven.plugins.android.generation2&lt;/groupId&gt;
     *     &lt;artifactId&gt;android-maven-plugin&lt;/artifactId&gt;
     *     &lt;executions&gt;
     *       &lt;execution&gt;
     *         &lt;id&gt;update-manifest&lt;/id&gt;
     *         &lt;goals&gt;
     *           &lt;goal&gt;manifest-update&lt;/goal&gt;
     *         &lt;/goals&gt;
     *         &lt;configuration&gt;
     *           &lt;manifest&gt;
     *             &lt;versionName&gt;&lt;/versionName&gt;
     *             &lt;versionCode&gt;123&lt;/versionCode&gt;
     *             &lt;versionCodeAutoIncrement&gt;true|false&lt;/versionCodeAutoIncrement&gt;
     *             &lt;versionCodeUpdateFromVersion&gt;true|false&lt;/versionCodeUpdateFromVersion&gt;
     *             &lt;sharedUserId&gt;anId&lt;/sharedUserId&gt;
     *             &lt;debuggable&gt;true|false&lt;/debuggable&gt;
     *             
     *             &lt;supports-screens&gt;
     *               &lt;anyDensity&gt;true&lt;/anyDensity&gt;
     *               &lt;xlargeScreens&gt;false&lt;/xlargeScreens&gt;
     *             &lt;/supports-screens&gt;
     *             
     *             &lt;compatible-screens&gt;
     *               &lt;compatible-screen&gt;
     *                 &lt;screenSize&gt;small&lt;/screenSize&gt;
     *                 &lt;screenDensity&gt;ldpi&lt;/screenDensity&gt;
     *               &lt;/compatible-screen&gt;
     *             &lt;/compatible-screens&gt;
     *           &lt;/manifest&gt;
     *         &lt;/configuration&gt;
     *       &lt;/execution&gt;
     *     &lt;/executions&gt;
     *   &lt;/plugin&gt;
     * </pre>
     * 
     * or use properties set in the pom or settings file or supplied as command line parameter. Add
     * "android." in front of the property name for command line usage. All parameters follow a
     * manifest.* naming convention.
     * <p>
     * 
     * @parameter
     */
    private Manifest manifest;

	/**
	 * Update the <code>android:versionName</code> with the specified parameter. If left empty it
	 * will use the version number of the project. Exposed via the project property
	 * <code>android.manifest.versionName</code>.
	 *
	 * @parameter expression="${android.manifest.versionName}" default-value="${project.version}"
	 */
	protected String manifestVersionName;

	/**
	 * Update the <code>android:versionCode</code> attribute with the specified parameter. Exposed via
	 * the project property <code>android.manifest.versionCode</code>.
	 *
	 * @parameter expression="${android.manifest.versionCode}"
	 */
	protected Integer manifestVersionCode;

	/**
	  * Auto increment the <code>android:versionCode</code> attribute with each build. The value is
	  * exposed via the project property <code>android.manifest.versionCodeAutoIncrement</code> and
	  * the resulting value as <code>android.manifest.versionCode</code>.
	  *
	  * @parameter expression="${android.manifest.versionCodeAutoIncrement}" default-value="false"
	  */
	 private Boolean manifestVersionCodeAutoIncrement = false;

	/**
	 * Update the <code>android:versionCode</code> attribute automatically from the project version
	 * e.g 3.0.1 will become version code 301. As described in this blog post
	 * http://www.simpligility.com/2010/11/release-version-management-for-your-android-application/
	 * but done without using resource filtering. The value is exposed via the project property
	 * property <code>android.manifest.versionCodeUpdateFromVersion</code> and the resulting value
	 * as <code>android.manifest.versionCode</code>.
	 *
	 * @parameter expression="${android.manifest.versionCodeUpdateFromVersion}" default-value="false"
	 */
	protected Boolean manifestVersionCodeUpdateFromVersion = false;

	/**
	 * Update the <code>android:sharedUserId</code> attribute with the specified parameter. If
	 * specified, exposes the project property <code>android.manifest.sharedUserId</code>.
	 *
	 * @parameter expression="${android.manifest.sharedUserId}"
	 */
	protected String manifestSharedUserId;

	/**
	 * Update the <code>android:debuggable</code> attribute with the specified parameter. Exposed via
	 * the project property <code>android.manifest.debuggable</code>.
	 *
	 * @parameter expression="${android.manifest.debuggable}"
	 */
	protected Boolean manifestDebuggable;

    protected SupportsScreens manifestSupportsScreens;

    protected List<CompatibleScreen> manifestCompatibleScreens;

    private String parsedVersionName;
    private Integer parsedVersionCode;
    private boolean parsedVersionCodeAutoIncrement;
    private Boolean parsedVersionCodeUpdateFromVersion;
    private String parsedSharedUserId;
    private Boolean parsedDebuggable;
    private SupportsScreens parsedSupportsScreens;
    private List<CompatibleScreen> parsedCompatibleScreens;

    public void execute() throws MojoExecutionException, MojoFailureException {
		if (!AndroidExtension.isAndroidPackaging(project.getPackaging())) {
			return; // skip, not an android project.
		}

		if (androidManifestFile == null) {
			return; // skip, no androidmanifest.xml defined (rare case)
		}

        parseConfiguration();

		getLog().info("Attempting to update manifest " + androidManifestFile);
		getLog().debug("    versionName=" + parsedVersionName);
		getLog().debug("    versionCode=" + parsedVersionCode);
		getLog().debug("    versionCodeAutoIncrement=" + parsedVersionCodeAutoIncrement);
		getLog().debug("    versionCodeUpdateFromVersion=" + parsedVersionCodeUpdateFromVersion);
		getLog().debug("    sharedUserId=" + parsedSharedUserId);
		getLog().debug("    debuggable=" + parsedDebuggable);
        getLog().debug(
                "    supports-screens: " + (parsedSupportsScreens == null ? "not set" : "set"));
        getLog().debug(
                "    compatible-screens: " + (parsedCompatibleScreens == null ? "not set" : "set"));

		if (!androidManifestFile.exists()) {
			return; // skip, no AndroidManifest.xml file found.
		}

		try {
			updateManifest(androidManifestFile);
		} catch (IOException e) {
			throw new MojoFailureException("XML I/O error: " + androidManifestFile, e);
		} catch (ParserConfigurationException e) {
			throw new MojoFailureException("Unable to prepare XML parser", e);
		} catch (SAXException e) {
			throw new MojoFailureException("Unable to parse XML: " + androidManifestFile, e);
		} catch (TransformerException e) {
			throw new MojoFailureException("Unable write XML: " + androidManifestFile, e);
		}
	}

    private void parseConfiguration() {
        // manifest element found in plugin config in pom
        if (manifest != null) {
        	if (StringUtils.isNotEmpty(manifest.getVersionName())) {
                parsedVersionName = manifest.getVersionName();
            } else {
                parsedVersionName = manifestVersionName;
            }
            if (manifest.getVersionCode() != null) {
                parsedVersionCode = manifest.getVersionCode();
            } else {
                parsedVersionCode = manifestVersionCode;
            }
            if (manifest.getVersionCodeAutoIncrement() != null) {
                parsedVersionCodeAutoIncrement = manifest.getVersionCodeAutoIncrement();
            } else {
                parsedVersionCodeAutoIncrement = manifestVersionCodeAutoIncrement;
            }
            if (manifest.getVersionCodeUpdateFromVersion() != null) {
                parsedVersionCodeUpdateFromVersion = manifest.getVersionCodeUpdateFromVersion();
            } else {
                parsedVersionCodeUpdateFromVersion = manifestVersionCodeUpdateFromVersion;
            }
            if (StringUtils.isNotEmpty(manifest.getSharedUserId())) {
                parsedSharedUserId = manifest.getSharedUserId();
            } else {
                parsedSharedUserId = manifestSharedUserId;
            }
            if (manifest.getDebuggable() != null) {
                parsedDebuggable = manifest.getDebuggable();
            } else {
                parsedDebuggable = manifestDebuggable;
            }
            if (manifest.getSupportsScreens() != null) {
                parsedSupportsScreens = manifest.getSupportsScreens();
            } else {
                parsedSupportsScreens = manifestSupportsScreens;
            }
            if (manifest.getCompatibleScreens() != null) {
                parsedCompatibleScreens = manifest.getCompatibleScreens();
            } else {
                parsedCompatibleScreens = manifestCompatibleScreens;
            }
        } else {
            parsedVersionName = manifestVersionName;
            parsedVersionCode = manifestVersionCode;
            parsedVersionCodeAutoIncrement = manifestVersionCodeAutoIncrement;
            parsedVersionCodeUpdateFromVersion = manifestVersionCodeUpdateFromVersion;
            parsedSharedUserId = manifestSharedUserId;
            parsedDebuggable = manifestDebuggable;
            parsedSupportsScreens = manifestSupportsScreens;
            parsedCompatibleScreens = manifestCompatibleScreens;
        }
    }

    /**
	 * Read manifest using JAXP
	 */
	private Document readManifest(File manifestFile) throws IOException, ParserConfigurationException, SAXException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(manifestFile);
		return doc;
	}

	/**
	 * Write manifest using JAXP transformer
	 */
	private void writeManifest(File manifestFile, Document doc) throws IOException, TransformerException {
		TransformerFactory xfactory = TransformerFactory.newInstance();
		Transformer xformer = xfactory.newTransformer();
		xformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		Source source = new DOMSource(doc);

		FileWriter writer = null;
		try {
			writer = new FileWriter(manifestFile, false);
			String xmldecl = String.format("<?xml version=\"%s\" encoding=\"%s\"?>%n", doc.getXmlVersion(),
					doc.getXmlEncoding());
			writer.write(xmldecl);
			Result result = new StreamResult(writer);

			xformer.transform(source, result);
		} finally {
			IOUtils.closeQuietly(writer);
		}
	}

	public void updateManifest(File manifestFile) throws IOException, ParserConfigurationException, SAXException,
			TransformerException, MojoFailureException {
		Document doc = readManifest(manifestFile);

		Element manifestElement = doc.getDocumentElement();

		boolean dirty = false;

		if (StringUtils.isEmpty(parsedVersionName)) {  // default to ${project.version}
			parsedVersionName =  project.getVersion();
		}

		Attr versionNameAttrib = manifestElement.getAttributeNode(ATTR_VERSION_NAME);

		if (versionNameAttrib == null || !StringUtils.equals(parsedVersionName, versionNameAttrib.getValue())) {
			getLog().info("Setting " + ATTR_VERSION_NAME +" to " + parsedVersionName);
			manifestElement.setAttribute(ATTR_VERSION_NAME, parsedVersionName);
			dirty = true;
		}

		if ((parsedVersionCodeAutoIncrement && parsedVersionCode != null) ||
			(parsedVersionCodeUpdateFromVersion && parsedVersionCode != null) ||
			(parsedVersionCodeAutoIncrement && parsedVersionCodeUpdateFromVersion)) {
			throw new MojoFailureException("versionCodeAutoIncrement, versionCodeUpdateFromVersion and versionCode " +
					"are mutual exclusive. They cannot be specified at the same time. " +
					"Please specify either versionCodeAutoIncrement, versionCodeUpdateFromVersion or versionCode!");
		}

		// Expose the version properties and other simple parsed manifest entries
		project.getProperties().setProperty("android.manifest.versionName", parsedVersionName);
		project.getProperties().setProperty("android.manifest.versionCodeAutoIncrement", String.valueOf(parsedVersionCodeAutoIncrement));
		project.getProperties().setProperty("android.manifest.versionCodeUpdateFromVersion", String.valueOf(parsedVersionCodeUpdateFromVersion));
		project.getProperties().setProperty("android.manifest.debuggable", String.valueOf(parsedDebuggable));
		if (parsedSharedUserId != null) {
			project.getProperties().setProperty("android.manifest.sharedUserId", parsedSharedUserId);
		}

		if (parsedVersionCodeAutoIncrement) {
            Attr versionCode = manifestElement.getAttributeNode(ATTR_VERSION_CODE);
            int currentVersionCode = 0;
            if (versionCode != null) {
                currentVersionCode = NumberUtils.toInt(versionCode.getValue(), 0);
            }
            currentVersionCode++;
            manifestElement.setAttribute(ATTR_VERSION_CODE, String.valueOf(currentVersionCode));
            project.getProperties().setProperty("android.manifest.versionCode", String.valueOf(currentVersionCode));
            dirty = true;
        }

        if (parsedVersionCodeUpdateFromVersion) {
            String verString = project.getVersion();
            getLog().debug("Generating versionCode for " + verString);
            ArtifactVersion artifactVersion = new DefaultArtifactVersion(verString);
            // invalid version, something went wrong in parsing, do the old fall back method
            String verCode;
            if (artifactVersion.getMajorVersion() == 0 & artifactVersion.getMinorVersion() == 0
                    && artifactVersion.getIncrementalVersion() == 0) {
                getLog().warn("Problem parsing version number occurred. Using fall back to determine version code. ");

                verCode = verString.replaceAll("\\D", "");

                Attr versionCodeAttr = manifestElement.getAttributeNode(ATTR_VERSION_CODE);
                int currentVersionCode = 0;
                if (versionCodeAttr != null) {
                    currentVersionCode = NumberUtils.toInt(versionCodeAttr.getValue(), 0);
                }

                if(Integer.parseInt(verCode) < currentVersionCode){
                    getLog().info(verCode + " < " + currentVersionCode + " so padding versionCode");
                    verCode = StringUtils.rightPad(verCode, versionCodeAttr.getValue().length(), "0");
                }
            } else {
                verCode = Integer.toString(artifactVersion.getMajorVersion()) +
                        Integer.toString(artifactVersion.getMinorVersion()) +
                        Integer.toString(artifactVersion.getIncrementalVersion());
            }
            getLog().info("Setting " + ATTR_VERSION_CODE + " to " + verCode);
            manifestElement.setAttribute(ATTR_VERSION_CODE, verCode);
            project.getProperties().setProperty("android.manifest.versionCode", String.valueOf(verCode));
            dirty = true;
        }

		if (parsedVersionCode != null) {
			Attr versionCodeAttr = manifestElement.getAttributeNode(ATTR_VERSION_CODE);
			int currentVersionCode = 0;
			if (versionCodeAttr != null) {
				currentVersionCode = NumberUtils.toInt(versionCodeAttr.getValue(), 0);
			}
			if (currentVersionCode != parsedVersionCode) {
				getLog().info("Setting " + ATTR_VERSION_CODE + " to " + parsedVersionCode);
				manifestElement.setAttribute(ATTR_VERSION_CODE, String.valueOf(parsedVersionCode));
				dirty = true;
			}
			project.getProperties().setProperty("android.manifest.versionCode", String.valueOf(parsedVersionCode));
		}

		if (!StringUtils.isEmpty(parsedSharedUserId)) {
			Attr sharedUserIdAttrib = manifestElement.getAttributeNode(ATTR_SHARED_USER_ID);

			if (sharedUserIdAttrib == null || !StringUtils.equals(parsedSharedUserId, sharedUserIdAttrib.getValue())) {
				getLog().info("Setting " + ATTR_SHARED_USER_ID +" to " + parsedSharedUserId);
				manifestElement.setAttribute(ATTR_SHARED_USER_ID, parsedSharedUserId);
				dirty = true;
			}
		}

		if (parsedDebuggable != null) {
			NodeList appElems = manifestElement.getElementsByTagName(ELEM_APPLICATION);

			// Update all application nodes. Not sure whether there will ever be more than one.
			for (int i = 0; i < appElems.getLength(); ++i) {
				Node node = appElems.item(i);
				getLog().info("Testing if node " + node.getNodeName() + " is application");
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					Element element = (Element)node;
					Attr debuggableAttrib = element.getAttributeNode(ATTR_DEBUGGABLE);
					if (debuggableAttrib == null || parsedDebuggable != BooleanUtils.toBoolean(debuggableAttrib.getValue())) {
						getLog().info("Setting " + ATTR_DEBUGGABLE + " to " + parsedDebuggable);
						element.setAttribute(ATTR_DEBUGGABLE, String.valueOf(parsedDebuggable));
						dirty = true;
					}
				}
			}
		}

		if (parsedSupportsScreens != null) {
            Element supportsScreensElem = XmlHelper.getOrCreateElement(doc, manifestElement,
                    ELEM_SUPPORTS_SCREENS);

            getLog().info("Setting " + ELEM_SUPPORTS_SCREENS);

            if (parsedSupportsScreens.getAnyDensity() != null) {
                supportsScreensElem.setAttribute(ATTR_ANY_DENSITY,
                        parsedSupportsScreens.getAnyDensity());
                dirty = true;
            }
            if (parsedSupportsScreens.getSmallScreens() != null) {
                supportsScreensElem.setAttribute(ATTR_SMALL_SCREENS,
                        parsedSupportsScreens.getSmallScreens());
                dirty = true;
            }
            if (parsedSupportsScreens.getNormalScreens() != null) {
                supportsScreensElem.setAttribute(ATTR_NORMAL_SCREENS,
                        parsedSupportsScreens.getNormalScreens());
                dirty = true;
            }
            if (parsedSupportsScreens.getLargeScreens() != null) {
                supportsScreensElem.setAttribute(ATTR_LARGE_SCREENS,
                        parsedSupportsScreens.getLargeScreens());
                dirty = true;
            }
            if (parsedSupportsScreens.getXlargeScreens() != null) {
                supportsScreensElem.setAttribute(ATTR_XLARGE_SCREENS,
                        parsedSupportsScreens.getXlargeScreens());
                dirty = true;
            }
            if (parsedSupportsScreens.getCompatibleWidthLimitDp() != null) {
                supportsScreensElem.setAttribute(ATTR_COMPATIBLE_WIDTH_LIMIT_DP,
                        parsedSupportsScreens.getCompatibleWidthLimitDp());
                dirty = true;
            }
            if (parsedSupportsScreens.getLargestWidthLimitDp() != null) {
                supportsScreensElem.setAttribute(ATTR_LARGEST_WIDTH_LIMIT_DP,
                        parsedSupportsScreens.getLargestWidthLimitDp());
                dirty = true;
            }
            if (parsedSupportsScreens.getRequiresSmallestWidthDp() != null) {
                supportsScreensElem.setAttribute(ATTR_REQUIRES_SMALLEST_WIDTH_DP,
                        parsedSupportsScreens.getRequiresSmallestWidthDp());
                dirty = true;
            }
            if (parsedSupportsScreens.getResizeable() != null) {
                supportsScreensElem.setAttribute(ATTR_RESIZEABLE,
                        parsedSupportsScreens.getResizeable());
                dirty = true;
            }
        }

        if (parsedCompatibleScreens != null) {
            getLog().info("Setting " + ELEM_COMPATIBLE_SCREENS);
            updateCompatibleScreens(doc, manifestElement);
            dirty = true;
        }

        if (dirty) {
            if (!manifestFile.delete()) {
                getLog().warn("Could not remove old " + manifestFile);
            }
            getLog().info("Made changes to manifest file, updating " + manifestFile);
            writeManifest(manifestFile, doc);
        } else {
            getLog().info("No changes found to write to manifest file");
        }
    }

    private void updateCompatibleScreens(Document doc, Element manifestElement) {
        Element compatibleScreensElem = XmlHelper.getOrCreateElement(doc, manifestElement,
                ELEM_COMPATIBLE_SCREENS);

        // read those screen elements that were already defined in the Manifest
        NodeList manifestScreenElems = compatibleScreensElem.getElementsByTagName(ELEM_SCREEN);
        int numManifestScreens = manifestScreenElems.getLength();
        ArrayList<CompatibleScreen> manifestScreens = new ArrayList<CompatibleScreen>(
                numManifestScreens);
        for (int i = 0; i < numManifestScreens; i++) {
            Element screenElem = (Element) manifestScreenElems.item(i);

            CompatibleScreen screen = new CompatibleScreen();
            screen.setScreenDensity(screenElem.getAttribute(ATTR_SCREEN_DENSITY));
            screen.setScreenSize(screenElem.getAttribute(ATTR_SCREEN_SIZE));

            manifestScreens.add(screen);
            getLog().debug("Found Manifest compatible-screen: " + screen);
        }

        // remove all child nodes, since we'll rebuild the element
        XmlHelper.removeDirectChildren(compatibleScreensElem);

        for (CompatibleScreen screen : parsedCompatibleScreens) {
            getLog().debug("Found POM compatible-screen: " + screen);
        }

        // merge those screens defined in the POM, overriding any matching screens
        // already defined in the Manifest
        HashSet<CompatibleScreen> mergedScreens = new HashSet<CompatibleScreen>();
        mergedScreens.addAll(manifestScreens);
        mergedScreens.addAll(parsedCompatibleScreens);

        for (CompatibleScreen screen : mergedScreens) {
            getLog().debug("Using compatible-screen: " + screen);
            Element screenElem = doc.createElement(ELEM_SCREEN);
            screenElem.setAttribute(ATTR_SCREEN_SIZE, screen.getScreenSize());
            screenElem.setAttribute(ATTR_SCREEN_DENSITY, screen.getScreenDensity());

            compatibleScreensElem.appendChild(screenElem);
        }
    }
}
