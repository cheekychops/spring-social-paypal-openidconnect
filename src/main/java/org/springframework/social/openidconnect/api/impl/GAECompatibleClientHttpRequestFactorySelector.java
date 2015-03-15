/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.social.openidconnect.api.impl;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.util.Properties;

import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.ClassUtils;

/**
 * This is a modified version of
 * org.springframework.social.support.ClientHttpRequestFactorySelector which
 * avoids using the HttpComponentsClientHttpRequestFactory's default
 * constructor, which calls HttpClients.createSystem(), which leads to the use
 * of java.net.ProxySelector, which is a restricted class on Google App Engine.
 *
 * @author Colin Smith
 */
public class GAECompatibleClientHttpRequestFactorySelector {

	public static ClientHttpRequestFactory getRequestFactory() {
		Properties properties = System.getProperties();
		String proxyHost = properties.getProperty("http.proxyHost");
		int proxyPort = properties.containsKey("http.proxyPort") ? Integer.valueOf(properties.getProperty("http.proxyPort")) : 80;
		if (HTTP_COMPONENTS_AVAILABLE) {
			HttpClientBuilder httpClientBuilder = HttpClients.custom();
			if (proxyHost != null) {
				HttpHost proxy = new HttpHost(proxyHost, proxyPort);
				httpClientBuilder.setProxy(proxy);
			}
			return HttpComponentsClientRequestFactoryCreator.createRequestFactory(httpClientBuilder.build(), proxyHost, proxyPort);
		} else {
			SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
			if (proxyHost != null) {
				requestFactory.setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort)));
			}
			return requestFactory;
		}
	}

	/**
	 * Decorates a request factory to buffer responses so that the responses may be repeatedly read.
	 * @param requestFactory the request factory to be decorated for buffering
	 * @return a buffering request factory
	 */
	public static ClientHttpRequestFactory bufferRequests(ClientHttpRequestFactory requestFactory) {
		return new BufferingClientHttpRequestFactory(requestFactory);
	}

	private static final boolean HTTP_COMPONENTS_AVAILABLE = ClassUtils.isPresent("org.apache.http.client.HttpClient", ClientHttpRequestFactory.class.getClassLoader());

	public static class HttpComponentsClientRequestFactoryCreator {

		public static ClientHttpRequestFactory createRequestFactory(HttpClient httpClient, String proxyHost, int proxyPort) {

			HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient) {
				@Override
				protected HttpContext createHttpContext(HttpMethod httpMethod, URI uri) {
					HttpClientContext context = new HttpClientContext();
					context.setAttribute("http.protocol.expect-continue", false);
					return context;
				}
			};

			return requestFactory;

		}
	}

}
