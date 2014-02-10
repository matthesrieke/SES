/**
 * Copyright (C) 2008-2014 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of
 * the following licenses, the combination of the program with the linked
 * library is not considered a "derivative work" of the program:
 *
 *     - Apache License, version 2.0
 *     - Apache Software License, version 1.0
 *     - GNU Lesser General Public License, version 3
 *     - Mozilla Public License, versions 1.0, 1.1 and 2.0
 *     - Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed
 * under the aforementioned licenses, is permitted by the copyright holders
 * if the distribution is compliant with both the GNU General Public
 * icense version 2 and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 */
package org.n52.ses.engine.epos;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.n52.epos.engine.filter.XPathConfiguration;
import org.n52.epos.filter.EposFilter;
import org.n52.epos.filter.FilterInstantiationException;
import org.n52.epos.filter.FilterInstantiationRepository;
import org.n52.ses.api.common.SesConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class EposFilterFactory {
	
	public static final String NAMESPACE_URI = "http://docs.oasis-open.org/wsn/b-2";
	public static final String XPATH_NAMESPACE_URI = 
	        "http://www.w3.org/TR/1999/REC-xpath-19991116";
    public static final QName MESSAGE_CONTENT_QNAME = 
            new QName(NAMESPACE_URI, "MessageContent");
    public static final String DIALECT = "Dialect";

	private static final Logger logger = LoggerFactory
			.getLogger(EposFilterFactory.class);

	public boolean accepts(QName filterName, String filterDialect) {
		if (filterDialect.equals(SesConstants.SES_FILTER_LEVEL_2_DIALECT)
				|| filterDialect
						.equals(SesConstants.SES_FILTER_LEVEL_3_DIALECT)) {
			return true;
		}

		if (filterName.equals(MESSAGE_CONTENT_QNAME)
				&& filterDialect.equals(XPATH_NAMESPACE_URI)) {
			return true;
		}
		
		if (filterDialect.equals(SesConstants.EPL_PURE_DIALECT)) {
			return true;
		}

		return false;
	}

	public EposFilter newInstance(Element filterXML) throws FilterInstantiationException {
		Object input = prepareInput(filterXML);

		EposFilter result = FilterInstantiationRepository.Instance
				.instantiate(input);
		return result;
	}

	private Object prepareInput(Element filterXML) throws FilterInstantiationException {
		Object input = null;
		QName qn = new QName(filterXML.getNamespaceURI(),
				filterXML.getLocalName());
		String dialect = filterXML.getAttribute(DIALECT);

		if (qn.equals(MESSAGE_CONTENT_QNAME)
				&& dialect.equals(XPATH_NAMESPACE_URI)) {
		    input = new XPathConfiguration(stripText(filterXML), 
		    		getNamespaceDeclarations(filterXML));
		} else {
			try {
				input = XmlObject.Factory.parse(findContentChild(filterXML));
			} catch (XmlException e) {
				logger.warn(e.getMessage());
				throw new FilterInstantiationException(e);
			}
		}
		
		return input;
	}


	private String stripText(Node filterXML) {
		NodeList list = filterXML.getChildNodes();
		
		Node item;
		for (int i = 0; i < list.getLength(); i++) {
			item = list.item(i);
			
			if (item.getNodeType() == Node.TEXT_NODE) {
				String result = item.getNodeValue();
				if (result != null) {
					result = result.trim();
					if (!result.isEmpty()) {
						return result;
					}
				}
			}
			else {
				String result = stripText(item);
				if (result != null) {
					return result;
				}
			}
		}
		
		return null;
	}

	private Element findContentChild(Element filterXML) {
		NodeList nodeList = filterXML.getChildNodes();
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				return (Element) node;
			}
		}
		return null;
	}

	/**
	 * 
	 * This is an auxiliary method used to recursively search an Element and its
	 * parent nodes for namespace/prefix definitions. It is used to implement
	 * getNamespaceDeclarations(Element).
	 * 
	 * @param xml
	 *            The current Element in the recursive search. All namespace
	 *            prefixes will be searched. , and then its children will be
	 *            searched.
	 * 
	 * @return The same Map as the second parameter (namespacesByPrefix), but
	 *         with more entries.
	 * 
	 */
	private static Map<String, String> getNamespaceDeclarations(Element xml) {
		Map<String,String> namespacesByPrefix = new HashMap<String, String>();
		
		Node parent = xml;

		int type;
		while ((null != parent)
				&& (((type = parent.getNodeType()) == Node.ELEMENT_NODE) ||
						(type == Node.ENTITY_REFERENCE_NODE))) {
			if (type == Node.ELEMENT_NODE) {
				NamedNodeMap nnm = parent.getAttributes();

				for (int i = 0; i < nnm.getLength(); i++) {
					Node attr = nnm.item(i);
					String aname = attr.getNodeName();
					boolean isPrefix = aname.startsWith("xmlns:");

					if (isPrefix || aname.equals("xmlns")) {
						int index = aname.indexOf(':');
						String pre = isPrefix ? aname.substring(index + 1) : "";
						String namespace = attr.getNodeValue();

						namespacesByPrefix.put(pre, namespace);
					}
				}
			}

			parent = parent.getParentNode();
		}

		return namespacesByPrefix;
	}

}
