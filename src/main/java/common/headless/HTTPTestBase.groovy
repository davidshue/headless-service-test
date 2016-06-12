package common.headless

import groovyx.net.http.HTTPBuilder

import javax.annotation.PostConstruct
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.SecureRandom
import java.security.cert.X509Certificate

import org.apache.http.HttpRequest
import org.apache.http.HttpResponse
import org.apache.http.conn.scheme.Scheme
import org.apache.http.conn.ssl.SSLSocketFactory
import org.apache.http.impl.client.DefaultRedirectStrategy
import org.apache.http.protocol.HttpContext
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Value
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

import static groovyx.net.http.ContentType.TEXT
import static groovyx.net.http.ContentType.URLENC
import static groovyx.net.http.Method.*

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes=HeadlessConfig.class)
@ActiveProfiles('headless')
abstract class HTTPTestBase {
	private Scheme secureScheme
	
	@Value('${headless.url.nonsecure}')
	protected String baseUrl

	@Value('${headless.url.secure}')
	protected String secureBaseUrl
	
	@PostConstruct
	void afterInit() {
		//=== SSL UNSECURE CERTIFICATE ===
		def sslContext = SSLContext.getInstance("SSL")
		sslContext.init(null, [
			new X509TrustManager() {
				public X509Certificate[] getAcceptedIssuers() {null }
				public void checkClientTrusted(X509Certificate[] certs, String authType) { }
				public void checkServerTrusted(X509Certificate[] certs, String authType) { }
			} ] as TrustManager[], new SecureRandom())
		def sf = new SSLSocketFactory(sslContext, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
		secureScheme = new Scheme("https", sf, 443)
	}
	
	protected HTTPBuilder builder() {
		HTTPBuilder http = new HTTPBuilder(baseUrl)
		disableRedirect(http)
		return http
	}
	
	protected HTTPBuilder secureBuilder()
	{
		HTTPBuilder http = new HTTPBuilder(secureBaseUrl)
		// secure HTTPBuilder must register with secureScheme, or it will give secure warnings thus the test will not proceed.
		http.client.connectionManager.schemeRegistry.register(secureScheme)
		disableRedirect(http)
		return http
	}
	
	private void disableRedirect(HTTPBuilder http)
	{
		// We need to disable the redirect for get here. For some reasons, get method is automatically redirected if instructed.
		http.client.redirectStrategy = new DefaultRedirectStrategy() {
			@Override
			boolean isRedirected(HttpRequest request, HttpResponse response, HttpContext context) throws ProtocolException
			{
				return false
			}
		}
		
	}
	
	
	/**
	 * This is used for calling internal web service to seed test data, and it is not meant to be used for testing external services
	 * @param path
	 * @param body
	 * @return Map that contains key/value data, which are code, contentType, data, and headers
	 */
	Map internalPost(String path, String body)
	{
		def result = [:]

		builder().request(POST, TEXT) {
			uri.path = path
			headers['content-type'] = 'text/plain'
			send URLENC, body
			
			response.success = httpClosure(result)
			
			response.failure = httpClosure(result)
		}
		
		return result
	}
	
	/**
    * @param path
    * @param inputBody
    * @return Map that contains key/value data, which are code, contentType, data
    */
	/*
	Map internalGet(String path, Map<String, String> inputBody = [:])
	{
		internalGet(path, inputBody, [Accpet: 'application/json'])
	}*/

	/**
	 * @param path
	 * @param inputBody
	 * @param header
	 * @return Map that contains key/value data, which are code, contentType, data, and headers
	 */
	Map internalGet(String path, Map<String, String> inputBody = [:], Map<String, String> header = [Accept:'application/json'])
	{
		def result = [:]

		builder().request(GET, TEXT) {
			uri.path = path
			if (header) {headers << header}
			if (inputBody) {uri.query = inputBody}

			response.success = httpClosure(result)

			response.failure = httpClosure(result)
		}

		return result
	}

	/**
	 * @param path
	 * @param body
	 * @return Map that contains key/value data, which are code, contentType, data, and headers
	 */
	Map internalHead(String path, Map<String, String> inputBody = [:])
	{
		def result = [:]

		builder().request(HEAD, TEXT) {
			uri.path = path
			if (inputBody)
			{
				uri.query = inputBody
			}

			response.success = httpClosure(result)

			response.failure = httpClosure(result)
		}

		return result
	}
	
	
	/**
	 * Closure for dealing with post callbacks
	 * @param result
	 * @return
	 */
	def httpClosure(result) {
		{ res, reader ->
			result['code'] = res.status
			result['headers'] = [:]
			res.getHeaders('Set-Cookie').each {header ->

				header.elements.each {element ->

					element.each {
						if (it.value) {
							result['headers'].get(header.name, [:])[it.name] = it.value
						}
						else {
							if (!result['headers'][header.name])
								result['headers'][header.name] = it.name
						}

					}
				}
				
			}
			res.allHeaders.each {header ->
				if (header.name == 'Set-Cookie') {
					header.elements.each {element ->
						element.each {
							if (it.value) {
								result['headers'].get(header.name, [:])[it.name] = it.value
							}
						}
					}
				}
				else {
					header.elements.each {element ->
						element.each {
							result['headers'][header.name] = it.name
						}
					}
				}
			}
			if (res.status != 302)
				result['contentType'] =  res.contentType
			result['body'] = reader?.text
		}
	}
}
