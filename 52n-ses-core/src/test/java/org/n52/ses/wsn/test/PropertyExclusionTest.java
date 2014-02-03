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
package org.n52.ses.wsn.test;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.muse.util.xml.XmlUtils;
import org.apache.muse.ws.notification.NotificationMessage;
import org.apache.muse.ws.notification.impl.SimpleNotificationMessage;
import org.junit.Assert;
import org.junit.Test;
import org.n52.ses.filter.dialects.SelectiveMetadataFilter;
import org.n52.ses.wsn.contentfilter.PropertyExclusionContentFilter;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class PropertyExclusionTest {

	@Test
	public void testPropertyExclusionContentFilter() throws IOException, SAXException {
		NotificationMessage message = createMessage();
		
		SelectiveMetadataFilter smf = new SelectiveMetadataFilter();
		smf.initialize(readFilter());
		
		PropertyExclusionContentFilter filter = new PropertyExclusionContentFilter(smf.getExcludedQNames());
		filter.filterMessage(message);
		checkForQNames(smf.getExcludedQNames(), message);
	}
	
	private Element readFilter() throws IOException, SAXException {
		return XmlUtils.createDocument(getClass().getResourceAsStream("propertyExclusionFilter.xml")).getDocumentElement();
	}

	private void checkForQNames(List<QName> qnames, NotificationMessage message) {
		Collection<?> contents = message.getMessageContentNames();
		for (Object object : contents) {
			QName qn = (QName) object;
			Element content = message.getMessageContent(qn);
			for (QName excludedQN : qnames) {
				NodeList list = content.getElementsByTagNameNS(excludedQN.getNamespaceURI(), excludedQN.getLocalPart());
				Assert.assertTrue("Content not removed!", list.getLength() == 0);
			}
		}
	}

	private NotificationMessage createMessage() throws IOException, SAXException {
		SimpleNotificationMessage result = new SimpleNotificationMessage();
		result.addMessageContent(createAIXMBasicMessage());
		return result;
	}

	private Element createAIXMBasicMessage() throws IOException, SAXException {
		return XmlUtils.createDocument(getClass().getResourceAsStream("aixmBasicMessage_propertyExclusion.xml")).getDocumentElement();
	}
	
}
