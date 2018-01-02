/**
 * Copyright (C) Original Authors 2017
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jenkinsci.plugins.jx.pipelines.helpers;

import io.fabric8.utils.XmlUtils;
import org.jenkinsci.plugins.jx.pipelines.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.net.URL;


/**
 */
public class DomUtils {

    /**
     * Returns the first element in the document with the given name
     */
    public static Element firstElement(Document doc, String elementName) {
        NodeList nodeList = doc.getElementsByTagName(elementName);
        for (int i = 0, size = nodeList.getLength(); i < size; i++) {
            Node item = nodeList.item(i);
            if (item instanceof Element) {
                return (Element) item;
            }
        }
        return null;
    }

    public static String firstElementText(Logger logger, Document doc, String elementName, String message) {
        Element e = firstElement(doc, elementName);
        if (e != null) {
            return e.getTextContent();
        } else {
            logger.error(message + " does not contain a <" + elementName + "> element!");
            return null;
        }
    }

    public static String parseXmlForURLAndReturnFirstElementText(Logger logger, String url, String elementName) {
        Document doc;
        try {
            doc = XmlUtils.parseDoc(new URL(url).openStream());
        } catch (Exception e) {
            logger.error("Failed to parse pom.xml", e);
            return null;
        }
        return firstElementText(logger, doc, elementName, url);
    }
}
