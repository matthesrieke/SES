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
package org.n52.ses.api;

import org.apache.muse.ws.notification.impl.FilterCollection;
import org.n52.ses.api.ws.INotificationMessage;
import org.n52.ses.api.ws.ISubscriptionManager;

/**
 * 
 * @author Matthes Rieke <m.rieke@uni-muenster.de>
 *
 */
public interface IFilterEngine  {
	
	/**
	 * Filter incoming data. Method parses the data from the
	 * given XML document (O&M).
	 * 
	 * @param message the notification message to filter
	 */
	public abstract void filter(INotificationMessage message);
	
	/**
	 * register a new subscription filter.
	 * @param subMgr the subscription manager
	 * @param engineCoveredFilters 
	 * @throws Exception exception if error occurs during the registration
	 * @return true, if the subscriptionmanager was registered at the engine instance
	 * with a filter
	 */
	public abstract boolean registerFilter(ISubscriptionManager subMgr, FilterCollection engineCoveredFilters) throws Exception;
	
	/**
	 * remove a subscription from the filter engine 
	 * @param subMgr the subscription manager responsible for the subscription
	 * @throws Exception if error occurs during unregistration
	 */
	public abstract void unregisterFilter(ISubscriptionManager subMgr) throws Exception;

	
	/**
	 * shutdown the resource
	 */
	public abstract void shutdown();

}
