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
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import com.jayway.maven.plugins.android.AbstractAndroidMojo;
import com.jayway.maven.plugins.android.common.AndroidExtension;

/**
 * Updates various version attributes present in the <code>AndroidManifest.xml</code> file.
 * <p>
 * You can configure this mojo to update the following manifest attributes:
 * </p>
 * <p>
 * <code>android:versionName</code> on the <code>manifest</code> element.
 * <code>android:versionCode</code> on the <code>manifest</code> element.
 * <code>android:sharedUserId</code> on the <code>manifest</code> element.
 * <code>android:debuggable</code> on the <code>application</code> element.
 * </p>
 * <p>
 * Note: This process will reformat the <code>AndroidManifest.xml</code> per JAXP {@link Transformer} defaults if updates are made to the manifest.
 * <p>
 * Updating Your <code>android:debuggable</code> attribute
 * <pre>
 *   &lt;plugin&gt;
 *     &lt;groupId&gt;com.jayway.maven.plugins.android.generation2&lt;/groupId&gt;
 *     &lt;artifactId&gt;maven-android-plugin&lt;/artifactId&gt;
 *     &lt;executions&gt;
 *       &lt;execution&gt;
 *         &lt;id&gt;update-manifest&lt;/id&gt;
 *         &lt;goals&gt;
 *           &lt;goal&gt;manifest-update&lt;/goal&gt;
 *         &lt;/goals&gt;
 *         &lt;configuration&gt;
 *           &lt;debuggable&gt;true&lt;/debuggable&gt;
 *         &lt;/configuration&gt;
 *       &lt;/execution&gt;
 *     &lt;/executions&gt;
 *   &lt;/plugin&gt;
 * </pre>
 * <p>
 *
 *
 *
 *
 * @author nic.strong@gmail.com
 * @goal manifest-update
 * @requiresProject true
 * @phase process-resources
 */
public class ManifestUpdateMojo extends AbstractAndroidMojo {
	private static final String ATTR_VERSION_NAME        = "android:versionName";
	private static final String ATTR_VERSION_CODE        = "android:versionCode";
	private static final String ATTR_SHARED_USER_ID      = "android:sharedUserId";
	private static final String ATTR_DEBUGGABLE          = "android:debuggable";

	private static final String ELEM_APPLICATION         = "application";
	/**
	 * Update the <code>android:versionName</code> with the specified parameter.
	 *
	 * @parameter expression="${android.manifest.versionName}"
	 */
	private String          versionName;

	/**
	 * Update the <code>android:versionCode</code> attribute with the specified parameter.
	 *
	 * @parameter expression="${android.manifest.versionCode}"
	 */
	private Integer         versionCode;

	/**
	 * Update the <code>android:sharedUserId</code> attribute with the specified parameter.
	 *
	 * @parameter expression="${android.manifest.sharedUserId}"
	 */
	private String          sharedUserId;

	/**
	 * Update the <code>android:debuggable</code> attribute with the specified parameter.
	 *
	 * @parameter expression="${android.manifest.debuggable}"
	 */
	private Boolean         debuggable;

	public void execute() throws MojoExecutionException, MojoFailureException {
		if (!AndroidExtension.isAndroidPackaging(project.getPackaging())) {
			return; // skip, not an android project.
		}

		if (androidManifestFile == null) {
			return; // skip, no androidmanifest.xml defined (rare case)
		}

		getLog().info("Attempting to update manifest " + androidManifestFile);

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

		Element manifestElement = doc.getDocumentElement();

		boolean dirty = false;

		if (!StringUtils.isEmpty(versionName)) {
			Attr versionNameAttrib = manifestElement.getAttributeNode(ATTR_VERSION_NAME);

			if (versionNameAttrib == null || !StringUtils.equals(versionName, versionNameAttrib.getValue())) {
				getLog().info("Setting " + ATTR_VERSION_NAME +" to " + versionName);
				manifestElement.setAttribute(ATTR_VERSION_NAME, versionName);
				dirty = true;
			}
		}

		if (versionCode != null) {
			Attr versionCodeAttr = manifestElement.getAttributeNode(ATTR_VERSION_CODE);
			int currentVersionCode = 0;
			if (versionCodeAttr != null) {
				currentVersionCode = NumberUtils.toInt(versionCodeAttr.getValue(), 0);
			}
			if (currentVersionCode != versionCode) {
				getLog().info("Setting " + ATTR_VERSION_CODE + " to " + versionCode);
				manifestElement.setAttribute(ATTR_VERSION_CODE, String.valueOf(versionCode));
				dirty = true;
			}
		}

		if (!StringUtils.isEmpty(sharedUserId)) {
			Attr sharedUserIdAttrib = manifestElement.getAttributeNode(ATTR_SHARED_USER_ID);

			if (sharedUserIdAttrib == null || !StringUtils.equals(sharedUserId, sharedUserIdAttrib.getValue())) {
				getLog().info("Setting " + ATTR_SHARED_USER_ID +" to " + sharedUserId);
				manifestElement.setAttribute(ATTR_SHARED_USER_ID, sharedUserId);
				dirty = true;
			}
		}

		if (debuggable != null) {
			NodeList appElems = manifestElement.getElementsByTagName(ELEM_APPLICATION);

			// Update all application nodes. Not sure whether there will ever be more than one.
			for (int i = 0; i < appElems.getLength(); ++i) {
				Node node = appElems.item(i);
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					Element element = (Element)node;
					Attr debuggableAttrib = element.getAttributeNode(ATTR_DEBUGGABLE);
					if (debuggableAttrib == null || debuggable != BooleanUtils.toBoolean(debuggableAttrib.getValue())) {
						getLog().info("Setting " + ATTR_DEBUGGABLE + " to " + debuggable);
						element.setAttribute(ATTR_DEBUGGABLE, String.valueOf(debuggable));
						dirty = true;
					}
				}
			}
		}

		if (dirty) {
			if (!manifestFile.delete()) {
				getLog().warn("Could not remove old " + manifestFile);
			}
			writeManifest(manifestFile, doc);
		}
	}
}
