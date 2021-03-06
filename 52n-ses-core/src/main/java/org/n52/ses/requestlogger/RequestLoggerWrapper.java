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
package org.n52.ses.requestlogger;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import org.apache.muse.util.xml.XmlUtils;
import org.n52.ses.util.common.ConfigurationRegistry;
import org.n52.ses.util.concurrent.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 * This class is for unloading the file logging of tomcat. Instead logging
 * every request in the logs, they are sent to an additional logging servlet
 * component.
 * 
 * A simple webapp also deployed to the servlet container is used to retreive
 * and store the requests. This way the SES is not inflated with database and stuff...
 * See project RequestLogger.
 * 
 * If this wrapper failes 3 times connecting to the servlet, default logging is
 * activated (if this wrapper is set to activation in the config file -->
 * USE_REQUEST_LOGGER = true
 * 
 * The following indicates if the webapp should be used for logging. If not set
 * in the config, default logging is used instead:
 * REQUEST_LOGGER_URL = <a_valid_URL>
 * 
 * @author Matthes Rieke
 *
 */
public class RequestLoggerWrapper {
	
	private static RequestLoggerWrapper _instance;
	
	private static final Logger requestLogger = LoggerFactory.getLogger("sesRequestLogger");
	
	private ExecutorService sendExecutor;
	URL url;
	private int failureCount;
	private boolean useWebapp;
	private boolean useLogger;
	private static boolean _active;
	
	private RequestLoggerWrapper(boolean active, String urlstring) {
		_active = active;
		if (!_active) return;
		
		/*
		 * if a URL string is set, use the webapp code, otherwise
		 * use default logger
		 */
		if (urlstring.equals("")) {
			this.useWebapp = false;
			this.useLogger = true;
		}
		else {
			this.useWebapp = true;
			this.useLogger = false;
			
			try {
				this.url = new URL(urlstring);
			} catch (MalformedURLException e) {
				this.useWebapp = false;
				this.useLogger = true;
				requestLogger.warn(e.getMessage(), e);
			}
			
			this.sendExecutor = Executors.newFixedThreadPool(3,
					new NamedThreadFactory("RequestLoggerPool"));
		}
	}

	
	/**
	 * @return the singleton instance
	 */
	public static synchronized RequestLoggerWrapper getInstance() {
		return _instance;
	}
	
	/**
	 * @return if activated
	 */
	public static boolean isActive() {
		return _active;
	}
	
	

	private void logRequest(final long timestamp, final String xml) {
		
		if (this.useLogger) {
			if (requestLogger.isDebugEnabled())
				requestLogger.debug("Incoming request: " + System.getProperty("line.separator") + xml);
		}
		else if (this.useWebapp) {
			this.sendExecutor.submit(new Runnable() {
				@Override
				public void run() {
					try {
						HttpURLConnection conn = (HttpURLConnection) RequestLoggerWrapper.this.url.openConnection();
						conn.setDoOutput(true);
						conn.setDoInput(true);

						conn.setRequestProperty("requesttime", Long.toString(timestamp));
						
						conn.connect();
						
						OutputStream os = conn.getOutputStream();

						os.write(xml.getBytes());
						
						os.flush();
						os.close();
						
						int resCode = conn.getResponseCode();
						conn.disconnect();
						
						if (resCode > 299) {
							RequestLoggerWrapper.this.incrementFailureCount();
						}
					} catch (IOException e) {
						RequestLoggerWrapper.requestLogger.warn(e.getMessage());
						RequestLoggerWrapper.this.incrementFailureCount();
					}
				}
			});
		}		
	}
	
	/**
	 * Forwards a request with the given timestamp to
	 * the logging webapp.
	 * 
	 * @param timestamp the timetsamp of request
	 * @param soapRequest the xml request
	 */
	public void logRequest(final long timestamp, final Document soapRequest) {
		String xml = null;
		if (this.useLogger || this.useWebapp) {
			xml  = XmlUtils.toString(soapRequest);
			logRequest(timestamp, xml);
		}
	}

	/**
	 * method is used to switch back to default logging if no
	 * request logging webapp could be connected to.
	 */
	protected synchronized void incrementFailureCount() {
		this.failureCount++;
		
		/*
		 * switch to default logging after too many fails
		 */
		if (this.failureCount > 3) {
			requestLogger.warn("Could not connect to logging WebApp. Switching to default logging.");
			this.useLogger = true;
			this.useWebapp = false;
		}
	}

	/**
	 * Called to init the logging wrapper
	 * 
	 * @param configurationRegistry the config instance
	 */
	public static void init(ConfigurationRegistry conf) {
		String active = conf.getPropertyForKey(ConfigurationRegistry.USE_REQUEST_LOGGER);
		String urlstring = conf.getPropertyForKey(ConfigurationRegistry.REQUEST_LOGGER_URL);
		
		_instance = new RequestLoggerWrapper(Boolean.parseBoolean(active), urlstring);
	}

	/**
	 * method for shutting down the executor threads
	 */
	public void shutdown() {
		if (this.sendExecutor != null)
			this.sendExecutor.shutdown();
	}
	
	public static void main(String[] args) {
		RequestLoggerWrapper rw = new RequestLoggerWrapper(true, "");
		rw.logRequest(System.currentTimeMillis(), "test");
	}



}
