package com.jayway.maven.plugins.android.standalonemojos;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.jayway.maven.plugins.android.AbstractAndroidMojo;

/**
 * Updates various version attributes present in the <code>AndroidManifest.xml</code> file.
 * <p>
 * You can configure this mojo to update the <code>android:versionName</code> attribute 
 * automatically to the value present in your project's version.
 * <p>
 * You can also configure this mojo to auto-increment the <code>android:versionCode</code>
 * attribute on each build as well.
 * <p>
 * Note: This process might reformat the <code>AndroidManifest.xml</code> per JAXP {@link Transformer} defaults.
 * <p>
 * Updating Your <code>android:versionName</code> attribute
 * <pre>
 *   &lt;plugin&gt;
 *     &lt;groupId&gt;com.jayway.maven.plugins.android.generation2&lt;/groupId&gt;
 *     &lt;artifactId&gt;maven-android-plugin&lt;/artifactId&gt;
 *     &lt;executions&gt;
 *       &lt;execution&gt;
 *         &lt;id&gt;update-version&lt;/id&gt;
 *         &lt;goals&gt;
 *           &lt;goal&gt;version-update&lt;/goal&gt;
 *         &lt;/goals&gt;
 *         &lt;configuration&gt;
 *           &lt;versionNameUpdate&gt;true&lt;/versionNameUpdate&gt;
 *         &lt;/configuration&gt;
 *       &lt;/execution&gt;
 *     &lt;/executions&gt;
 *   &lt;/plugin&gt;
 * </pre>
 * <p>
 * Auto-Incrementing your <code>android:versionCode</code> attribute
 * <pre>
 *   &lt;plugin&gt;
 *     &lt;groupId&gt;com.jayway.maven.plugins.android.generation2&lt;/groupId&gt;
 *     &lt;artifactId&gt;maven-android-plugin&lt;/artifactId&gt;
 *     &lt;executions&gt;
 *       &lt;execution&gt;
 *         &lt;id&gt;update-version&lt;/id&gt;
 *         &lt;goals&gt;
 *           &lt;goal&gt;version-update&lt;/goal&gt;
 *         &lt;/goals&gt;
 *         &lt;configuration&gt;
 *           &lt;versionCodeAutoIncrement&gt;true&lt;/versionCodeAutoIncrement&gt;
 *         &lt;/configuration&gt;
 *       &lt;/execution&gt;
 *     &lt;/executions&gt;
 *   &lt;/plugin&gt;
 * </pre>
 *
 * 
 * 
 * @author joakim@erdfelt.com
 * @goal version-update
 * @requiresProject true
 * @phase prepare-resources
 */
public class VersionUpdateMojo extends AbstractAndroidMojo {
    private static final String ATTR_VERSION_NAME        = "android:versionName";
    private static final String ATTR_VERSION_CODE        = "android:versionCode";

    /**
     * Update the <code>android:versionName</code> with the <code>&lt;version&gt;</code> value from the maven project.
     * 
     * @parameter expression="${android.versionname.update}" default-value="true"
     */
    private boolean             versionNameUpdate        = true;

    /**
     * Auto increment the <code>android:versionCode</code> attribute with each build.
     * 
     * @parameter expression="${android.versioncode.autoincrement}" default-value="false"
     */
    private boolean             versionCodeAutoIncrement = false;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!"apk".equals(project.getPackaging())) {
            return; // skip, not an android project.
        }

        if (androidManifestFile == null) {
            return; // skip, no androidmanifest.xml defined (rare case)
        }

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
            TransformerException {
        Document doc = readManifest(manifestFile);

        Element root = doc.getDocumentElement();

        boolean dirty = false;

        if (versionNameUpdate) {
            String projectVersion = project.getVersion();
            Attr versionName = root.getAttributeNode(ATTR_VERSION_NAME);

            if ((versionName == null) || (StringUtils.equals(projectVersion, versionName.getValue()) == false)) {
                root.setAttribute(ATTR_VERSION_NAME, projectVersion);
                dirty = true;
            }
        }

        if (versionCodeAutoIncrement) {
            Attr versionCode = root.getAttributeNode(ATTR_VERSION_CODE);
            int currentVersionCode = 0;
            if (versionCode != null) {
                currentVersionCode = NumberUtils.toInt(versionCode.getValue(), 0);
            }
            currentVersionCode++;
            root.setAttribute(ATTR_VERSION_CODE, String.valueOf(currentVersionCode));
            dirty = true;
        }

        if (dirty) {
            writeManifest(manifestFile, doc);
        }
    }
}
