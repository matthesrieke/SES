/**
 * Copyright (C) 2012
 * by 52 North Initiative for Geospatial Open Source Software GmbH
 *
 * Contact: Andreas Wytzisk
 * 52 North Initiative for Geospatial Open Source Software GmbH
 * Martin-Luther-King-Weg 24
 * 48155 Muenster, Germany
 * info@52north.org
 *
 * This program is free software; you can redistribute and/or modify it under
 * the terms of the GNU General Public License version 2 as published by the
 * Free Software Foundation.
 *
 * This program is distributed WITHOUT ANY WARRANTY; even without the implied
 * WARRANTY OF MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program (see gnu-gpl v2.txt). If not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA or
 * visit the Free Software Foundation web page, http://www.fsf.org.
 */
// 
// MyCapability.java
// Wed May 21 15:24:25 CEST 2008
// Generated by the Apache Muse Code Generation Tool
// 
package org.n52.ses.wsbr;

import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.namespace.QName;

import org.apache.muse.core.Resource;
import org.apache.muse.core.ResourceManager;
import org.apache.muse.core.ResourceManagerListener;
import org.apache.muse.core.routing.MessageHandler;
import org.apache.muse.util.ReflectUtils;
import org.apache.muse.ws.addressing.EndpointReference;
import org.apache.muse.ws.addressing.soap.SoapFault;
import org.apache.muse.ws.resource.WsResource;
import org.apache.muse.ws.resource.impl.AbstractWsResourceCapability;
import org.apache.muse.ws.resource.lifetime.ScheduledTermination;
import org.n52.oxf.xmlbeans.parser.XMLHandlingException;
import org.n52.ses.api.common.GlobalConstants;
import org.n52.ses.api.common.SensorMLConstants;
import org.n52.ses.api.ws.IPublisherEndpoint;
import org.n52.ses.api.ws.IRegisterPublisher;
import org.n52.ses.common.SensorML;
import org.n52.ses.wsbr.soaphandler.PublisherHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;


/**
 * @author Jan Torben Heuer <jan.heuer@uni-muenster.de>
 * 
 * Handles the creation of new {@link PublisherEndpoint} resources.
 */
public class RegisterPublisher extends AbstractWsResourceCapability
implements IRegisterPublisher, ResourceManagerListener {

	private static Logger logger = LoggerFactory.getLogger(RegisterPublisher.class);

	private ConcurrentHashMap<EndpointReference, IPublisherEndpoint> map = new ConcurrentHashMap<EndpointReference, IPublisherEndpoint>();

	/**
	 * constant for the resource type
	 */
	public final static String RESOURCE_TYPE = GlobalConstants.PUBLISHER_REGISTRATION_MANAGER_CONTEXT_PATH;

	private String registerPath;


	QName[] PROPERTIES = new QName[] {};

	private String publisherRegistrationManagerEndpoint;

	private ResourceManager manager;


	@Override
	public void initialize() throws SoapFault {
		super.initialize();

		/* get the Manager instance */
		WsResource resource = getWsResource();
		this.manager = resource.getResourceManager();


		/*
		 * select the register method that is called on "register" by the
		 * Publisher message handler
		 */
		setMessageHandler(createrRegisterPublisherHandler());

		//
		// make sure we're exposing the advertise endpoint so that
		// clients can manage subscriptions/consumers
		//
		this.registerPath = this.manager.getResourceContextPath(RegisterPublisher.class);
		if (this.registerPath == null) {
			throw new RuntimeException("NoRegisterPublisher");
		}

		/* retrieve the PublicationEndpoint context path */
		this.publisherRegistrationManagerEndpoint = this.manager.getResourceContextPath(PublisherEndpoint.class);

		if (this.publisherRegistrationManagerEndpoint == null) {
			throw new RuntimeException("No PublicationEndpoint deployed");
		}

		/* add ourselves as listener for shutdown of PRM instances */
		this.manager.addListener(this);

		//		SESNotificationProducer producer = (SESNotificationProducer)getResource().getCapability(WsnConstants.PRODUCER_URI);
	}

	@Override
	public void initializeCompleted() throws SoapFault {
		super.initializeCompleted();
	}

	/**
	 * Sets the Handler for incoming SOAP requests
	 * 
	 * @return a new {@link PublisherHandler} instance
	 */
	private MessageHandler createrRegisterPublisherHandler() {

		MessageHandler handler = new PublisherHandler();
		Method method = ReflectUtils.getFirstMethod(getClass(), "registerPublisher");
		handler.setMethod(method);

		return handler;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.n52.ses.wsbr.IRegisterPublisher#registerPublisher(org.apache.muse.ws.addressing.EndpointReference,
	 *      org.w3c.dom.Element[], boolean, java.util.Date)
	 * 
	 * This method is called for incomming SOAP requests. Messages are extracted
	 * by the PublisherHander. The response is rendered by the PublisherHandler,
	 * too.
	 */
	@Override
	public Resource registerPublisher(EndpointReference publisherReference, QName[] topic,
			boolean demand, Date initialTerminationTime, org.w3c.dom.Element domElem) throws SoapFault {
		Resource publisherResource = null;

		/* create new resource (@see SimpleNotificationProducer) */
		publisherResource = this.manager.createResource(this.publisherRegistrationManagerEndpoint);

		EndpointReference epr = publisherResource.getEndpointReference();
		PublisherEndpoint publisherCapability =
				(PublisherEndpoint) publisherResource.getCapability(IPublisherEndpoint.NAMESPACE_URI);

		/* set topics */
		publisherCapability.setTopic(topic);

		/* check for AdvertiseSOS or SensorML */
		if (domElem == null) {
			throw new NullPointerException("There" +
					" was no SensorML element found in the request.");
		}

		try {
			publisherCapability.registerSensorML(domElem);
			SensorML sml = (SensorML) publisherResource.getCapability(SensorMLConstants.NAMESPACE);
			sml.setSensorML(domElem);
		} catch (XMLHandlingException e) {
			throw new SoapFault("An error occured while processing " +
					"SensorML: "+ e.getMessage());
		}
		
		Collection<IPublisherEndpoint> existingEndpoints = getPublisherEndpoints();
		for (IPublisherEndpoint pe : existingEndpoints) {
			if (pe.getSensorId().equals(publisherCapability.getSensorId())) {
				throw new IllegalStateException("The SensorID '" + pe.getSensorId()
						+"' is already registered within this service instance.");
			}
		}
		
		/*
		 * set termination time
		 * @see SimpleScheduledTermination
		 */
		ScheduledTermination scheduledTermination = (ScheduledTermination) publisherResource
				.getCapability("http://docs.oasis-open.org/wsrf/rlw-2/ScheduledResourceTermination");
		scheduledTermination.setTerminationTime(generateTerminationTime(initialTerminationTime));

		synchronized (this) {
			this.map.put(epr, publisherCapability);
		}

		/* don't change the EPR after the following step */
		publisherResource.initialize();
		this.manager.addResource(epr, publisherResource);


		return publisherResource;
	}

	/**
	 * calculates a valid termination time.
	 * 
	 * @param initialTerminationTime the termination time. can be null
	 * @return the generated termination time.
	 */
	protected Date generateTerminationTime(Date initialTerminationTime) {
		Date now = new Date();
		if (initialTerminationTime != null && initialTerminationTime.after(now)) {
			return initialTerminationTime;
		}
		/* complicated java date handling ... */
		Calendar cal = new GregorianCalendar();
		cal.add(Calendar.DAY_OF_YEAR, 1);
		return cal.getTime();
	}

	/**
	 * 
	 * @see org.apache.muse.core.ResourceManagerListener#resourceAdded(org.apache.muse.ws.addressing.EndpointReference,
	 *      org.apache.muse.core.Resource)
	 *      
	 * @throws SoapFault  if an error occurred on adding
	 */
	@Override
	public void resourceAdded(EndpointReference epr, Resource resource) throws SoapFault {
		/* no action needed, yet */
	}

	/**
	 * 
	 * @see org.apache.muse.core.ResourceManagerListener#resourceRemoved(org.apache.muse.ws.addressing.EndpointReference)
	 * 
	 * @throws SoapFault if an error occurred on removing
	 */
	@Override
	public void resourceRemoved(EndpointReference epr) throws SoapFault {
		synchronized (this) {
			if (epr != null) {
				Object isInMap = this.map.remove(epr);
				if(isInMap instanceof PublisherEndpoint) {
					logger.info("The PublisherEndpoint " + isInMap.toString() + " has been removed.");
				}
			} else {
				logger.debug("Attemp to remove a null value from PublisherEnpoint map");
			}
		}
	}

	/**
	 * @return all {@link PublisherEndpoint} instances that have been created by
	 *         this instance
	 */
	public Collection<IPublisherEndpoint> getPublisherEndpoints() {
		synchronized (this) {
			return this.map.values();	
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.muse.ws.resource.impl.AbstractWsResourceCapability#getPropertyNames()
	 */
	@Override
	public QName[] getPropertyNames() {
		return this.PROPERTIES;
	}

	/**
	 * Used as part of the persistency mechanism. Re-registers a publishers endpoint.
	 * 
	 * TODO: topic, demand and termination time!
	 * 
	 * @param publisherReference the EPF
	 * @param topic topic on which the publisher publishes
	 * @param demand true if the publisher is in "on demand" mode
	 * @param initialTerminationTime termination time for the publisher
	 * @param sensorML sensorML doc
	 * @param publisherCapability the {@link PublisherEndpoint} instance
	 */
	public void reRegisterPublisher(EndpointReference publisherReference,
			QName[] topic, boolean demand, Date initialTerminationTime,
			Element sensorML, IPublisherEndpoint publisherCapability) {

		synchronized (this) {
			this.map.put(publisherCapability.getPublisherReference(), publisherCapability);
		}
	}

}