package org.n52.ses.api.ws.impl;

import java.net.URI;

import javax.xml.transform.Result;
import javax.xml.ws.EndpointReference;

/**
 * An implementation of the W3C WS-A endpoint reference.
 * TODO: implement
 */
public class EndpointReferenceImpl extends EndpointReference {

	private URI uri;

	public EndpointReferenceImpl(URI uri) {
		this.uri = uri;
	}

	@Override
	public void writeTo(Result result) {
		
	}

}
