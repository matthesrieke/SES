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
package org.n52.ses.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.n52.ses.common.LandingPageServlet;
import org.n52.ses.common.ServiceConfigurationServlet;
import org.n52.ses.request.ExceptionEncoder;
import org.n52.ses.request.Request;
import org.n52.ses.request.RequestHandler;
import org.n52.ses.request.Response;
import org.n52.ses.request.ResponseEncoder;
import org.n52.ses.startupinit.StartupInitServlet;
import org.n52.ses.util.common.ConfigurationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;

@Singleton
public class BaseServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static int minimumContentLengthForGzip = 500000;
	private static ConfigurationRegistry conf;
	private static final Logger logger = LoggerFactory
			.getLogger(BaseServlet.class);

	@Inject
	private Set<RequestHandler> requestHandlers;

	@Inject
	private ExceptionEncoder<RuntimeException> runtimeExceptionEncoder;

	@Inject
	private Set<ResponseEncoder> responseEncoders;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		Map<String, List<String>> params = resolveParameters(req.getParameterMap());
		String contentType = req.getContentType();
		String contextPath = req.getContextPath();
		
		Map<String, String> headers = resolveHeaders(req.getHeaderNames(), req);

		Request wrappedRequest = createRequest(contextPath, params,
				contentType, req.getContentLength(),
				req.getCharacterEncoding(), headers, req.getInputStream(),
				req.getMethod());

		try {
			RequestHandler responseHandler = resolveHandler(wrappedRequest);
			ResponseEncoder responseEncoder = resolveEncoder(wrappedRequest);

			Response response = responseHandler.process(wrappedRequest);

			byte[] bytes = responseEncoder.encode(response, wrappedRequest);

			printResponse(req, resp, bytes,
					responseEncoder.getTargetCharset(wrappedRequest),
					responseHandler.getTargetContentType(wrappedRequest));
		} catch (RuntimeException e) {
			logger.warn(e.getMessage());
			byte[] bytes = runtimeExceptionEncoder.writeException(e);
			resp.setStatus(runtimeExceptionEncoder.resolveHttpStatus(e));
			resp.setContentType(runtimeExceptionEncoder.resolveContentType(e));
			resp.getOutputStream().write(bytes);
			resp.setCharacterEncoding(runtimeExceptionEncoder.getCharset());
			resp.getOutputStream().close();
		}
	}

	private Map<String, List<String>> resolveParameters(Map<?, ?> parameterMap) {
		if (parameterMap.size() == 0) {
			return Collections.emptyMap();
		}
		
		Map<String,List<String>> result = new HashMap<String, List<String>>(parameterMap.size());
		
		for (Object o : parameterMap.keySet()) {
			Object values = parameterMap.get(o);
			
			if (values instanceof String[]) {
				result.put(o.toString(), Arrays.asList((String[]) values));
			}
			else if (values instanceof String) {
				result.put(o.toString(), Collections.singletonList((String) values));
			}
		}
		
		return result;
	}

	private void printResponse(HttpServletRequest request,
			HttpServletResponse response, byte[] bytes, String charset, String contentType)
			throws IOException {
		int contentLength = bytes.length;

		synchronized (BaseServlet.class) {

			if (conf == null) {
				conf = ConfigurationRegistry.getInstance();

				if (conf != null) {
					minimumContentLengthForGzip = Integer
							.parseInt(conf
									.getPropertyForKey(ConfigurationRegistry.MINIMUM_GZIP_SIZE));
				}
			}
		}
		
		response.setContentType(contentType);
		response.setStatus(HttpStatus.SC_OK);

		/*
		 * compressed response
		 */
		if (contentLength > minimumContentLengthForGzip
				&& clientSupportsGzip(request)) {
			response.addHeader("Content-Encoding", "gzip");
			GZIPOutputStream gzip = new GZIPOutputStream(
					response.getOutputStream(), contentLength);
			String type = response.getContentType();
			if (!type.contains("charset")) {
				response.setContentType(type + "; charset=" + charset);
			}
			gzip.write(bytes);
			gzip.flush();
			gzip.finish();
		}
		/*
		 * uncompressed response
		 */
		else {
			response.setContentLength(contentLength);
			response.setCharacterEncoding(charset);
			response.getOutputStream().write(bytes);
			response.getOutputStream().flush();
		}
		
	}

	private boolean clientSupportsGzip(HttpServletRequest request) {
		String header = request.getHeader("Accept-Encoding");
		if (header != null && !header.isEmpty()) {
			String[] split = header.split(",");
			for (String string : split) {
				if (string.equalsIgnoreCase("gzip")) {
					return true;
				}
			}
		}
		return false;
	}

	private ResponseEncoder resolveEncoder(Request request) {

		for (ResponseEncoder rh : responseEncoders) {
			if (rh.supportsEncodingOf(request)) {
				return rh;
			}
		}

		throw new UnsupportedOperationException(String.format(
				"No encoder implementation for requested format '%s' found.",
				request.getAcceptHeaders().toString()));
	}

	private RequestHandler resolveHandler(Request request) {

		for (RequestHandler rh : requestHandlers) {
			if (rh.supportsHandlingOf(request)) {
				return rh;
			}
		}

		throw new UnsupportedOperationException(String.format(
				"No handler implementation for request '%s' found.",
				request.toString()));
	}

	private Request createRequest(String contextPath, Map<String, List<String>> params,
			String contentType, int contentLength, String characterEncoding,
			Map<String, String> headers, ServletInputStream inputStream,
			String method) {
		return new Request(contextPath, params, contentType, contentLength,
				characterEncoding, headers, inputStream, method);
	}

	private Map<String, String> resolveHeaders(Enumeration<?> headerNames,
			HttpServletRequest req) {
		Map<String, String> result = new HashMap<String, String>();

		while (headerNames.hasMoreElements()) {
			String h = headerNames.nextElement().toString();
			result.put(h, req.getHeader(h));
		}

		return result;
	}

	public static class GuiceServletConfig extends
			GuiceServletContextListener {

		@Override
		protected Injector getInjector() {
			ServiceLoader<Module> loader = ServiceLoader.load(Module.class);

			List<Module> modules = new ArrayList<Module>();
			
			for (Module module : loader) {
				modules.add(module);
			}

			modules.add(new ServletModule() {

				@Override
				protected void configureServlets() {
					serve("/config/*").with(ServiceConfigurationServlet.class);
					serve("/startup/*").with(StartupInitServlet.class);
					serve("/services/*").with(BaseServlet.class);
					serve("/").with(LandingPageServlet.class);
				}

			});

			return Guice.createInjector(modules);
		}
	}

}
