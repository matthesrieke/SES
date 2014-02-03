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
// 
// IMyCapability.java
// Wed May 21 15:24:25 CEST 2008
// Generated by the Apache Muse Code Generation Tool
// 
package org.n52.ses.common;

import org.w3c.dom.Element;

/**
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public interface ICapabilites {

	/**
	 * Method header of the DescriceSensor method.
	 * 
	 * @param SensorID ID of the sensor to describe
	 * @return DescribeSensor response
	 * @throws Exception if Sensor was not found inside the service.
	 */
	public Element describeSensor(Element SensorID) throws Exception;
	
	/**
	 * Method header of the DescribeStoredFilter method.
	 * 
	 * @param storedFilterID the unique ID
	 * @return DescribeStoredFilterResponse
	 * @throws Exception if stored filter could not be found
	 */
	public Element describeStoredFilter(Element storedFilterID) throws Exception;

	/**
	 * Method header of the DescriceSensor method.
	 * 
	 * @param xml the request
	 * @return GetCapabilites response
	 * @throws Exception if an error occurred during the capabilities creation
	 */
	public Element getCapabilities(Element xml) throws Exception;

}