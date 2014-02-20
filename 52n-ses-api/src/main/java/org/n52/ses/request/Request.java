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
package org.n52.ses.request;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Request {

	private Content content;
	private String contextPath;
	private Map<String, String> headers;
	private Map<String, List<String>> parameters;
	private String method;

	public Request(String contextPath, Map<String, List<String>> params, String contentType,
			int contentLength, String characterEncoding,
			Map<String, String> headers, InputStream inputStream, String method) {
		this.contextPath = contextPath;
		this.headers = headers;
		this.parameters = params;
		this.content = new Content(contentType, contentLength, characterEncoding,
				inputStream);
		this.method = method;
	}
	
	
	public String getMethod() {
		return method;
	}

	public Map<String, List<String>> getParameters() {
		return parameters;
	}

	public Content getContent() {
		return content;
	}

	public String getContextPath() {
		return contextPath;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public Map<String, String> getAcceptHeaders() {
		Map<String,String> result = new HashMap<String, String>();
		
		for (String s : headers.keySet()) {
			if (s.startsWith("Accept")) {
				result.put(s, headers.get(s));
			}
		}
		
		return result;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Request: content=");
		sb.append(content.toString());
		sb.append(", Header=");
		sb.append(headers.toString());
		sb.append(", Parameter=");
		sb.append(parameters);
		return sb.toString();
	}

	public class Content {

		private Charset encoding;
		private String contentType;
		private int contentLength;
		private InputStream inputStream;

		public Content(String contentType, int contentLength,
				String characterEncoding, InputStream inputStream) {
			this.contentType = contentType;
			this.contentLength = contentLength;
			this.encoding = characterEncoding == null ? null : Charset.forName(characterEncoding);
			this.inputStream = inputStream;
		}

		public Charset getEncoding() {
			return encoding;
		}

		public String getContentType() {
			return contentType;
		}

		public int getContentLength() {
			return contentLength;
		}

		public InputStream getInputStream() {
			return inputStream;
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("Content: Content-Type=");
			sb.append(contentType);
			sb.append(", Content-Length=");
			sb.append(contentLength);
			sb.append(", Encoding=");
			sb.append(encoding);
			return sb.toString();
		}
		
	}


}
