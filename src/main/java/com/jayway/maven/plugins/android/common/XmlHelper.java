package com.jayway.maven.plugins.android.common;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XmlHelper {

    public static void removeDirectChildren(Node parent) {
        NodeList childNodes = parent.getChildNodes();
        while (childNodes.getLength() > 0) {
            parent.removeChild(childNodes.item(0));
        }
    }

}
