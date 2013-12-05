package com.mentat.headless

import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*
import groovyx.net.http.HTTPBuilder

import java.nio.charset.Charset
import java.security.SecureRandom
import java.security.cert.X509Certificate

import javax.annotation.PostConstruct
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

import org.apache.commons.io.IOUtils
import org.apache.http.Header
import org.apache.http.conn.scheme.Scheme
import org.apache.http.conn.ssl.SSLSocketFactory
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Value
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(['classpath:com/bc/intl/selenium/headless-test-context.xml'])
class HeadlessTestBase
{
	private static final LOGIN_SERVICE_URI = "/ws/external/login/v1"
	
	@Value('${base.url}')
	protected String baseUrl
	
	@Value('${base.url.secure}')
	protected String secureBaseUrl
	
	protected Scheme secureScheme
	
	@PostConstruct
	void afterInit()
	{
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
	
	private HTTPBuilder builder()
	{
		new HTTPBuilder(baseUrl)
	}
	
	private HTTPBuilder secureBuilder()
	{
		HTTPBuilder http = new HTTPBuilder(secureBaseUrl)
		http.client.connectionManager.schemeRegistry.register(secureScheme)
		return http
	}
	
	AuthResult login(String username, String password)
	{
		int code
		String contentType
		String icbcCookie
		String authToken
		
		def inputBody = [:]
		inputBody['m_email'] = username
		inputBody['m_password'] = password
		inputBody['_rememberMe'] = 'true'

		secureBuilder().request(POST) {
			uri.path = LOGIN_SERVICE_URI
			send URLENC, inputBody
			
			response.success = {res ->
				code = res.statusLine.statusCode as int
				Header[] headers = res.getHeaders('Set-Cookie')
				headers.each { header ->
					if (header.value.startsWith('icbc'))
					{
						icbcCookie = header.value.split(';')[0]
					}
				}
				contentType =  res.entity.contentType?.value
				StringWriter writer = new StringWriter()
				IOUtils.copy(res.entity.content, writer, Charset.forName('UTF-8'))
				String body = writer.toString()
				def xml = new XmlSlurper().parseText(body)
				authToken = xml.payload.member.authToken
			}
			
			response.failure = {res ->
				code = res.statusLine.statusCode as int
				contentType =  res.entity.contentType?.value
			}
		}
					
		return new AuthResult(code: code, contentType: contentType, cookie: icbcCookie, authToken: authToken)
	}
	
	Map post(String path, Map<String, String> inputBody, AuthResult authResult)
	{
		_post(builder(), path, inputBody, authResult)
	}
	
	Map securePost(String path, Map<String, String> inputBody, AuthResult authResult)
	{
		_post(secureBuilder(), path, inputBody, authResult)
	}
	
	private Map _post(HTTPBuilder http, String path, Map<String, String> inputBody, AuthResult authResult)
	{
		def result = [:]
		if (!inputBody)
		{
			inputBody = [:]
		}
		if (authResult)
		{
			inputBody['authToken'] = authResult.authToken
		}

		http.request(POST) {
			uri.path = path
			if (authResult)
			{
				headers['cookie'] = authResult.cookie
			}
			send URLENC, inputBody
			
			response.success = postClosure(result)
			
			response.failure = postClosure(result)
		}
		
		return result
	}
	
	Map get(String path, Map<String, String> inputBody, AuthResult authResult)
	{
		_get(builder(), path, inputBody, authResult)
	}
	
	Map secureGet(String path, Map<String, String> inputBody, AuthResult authResult)
	{
		_get(secureBuilder(), path, inputBody, authResult)
	}
	
	private Map _get(HTTPBuilder http, String path, Map<String, String> inputBody, AuthResult authResult)
	{
		def result = [:]
		if (!inputBody)
		{
			inputBody = [:]
		}
		if (authResult)
		{
			inputBody['authToken'] = authResult.authToken
		}
		
		http.request(GET, TEXT) { req ->
			uri.path = path
			if (inputBody)
			{
				uri.query = inputBody
			}
			if (authResult)
			{
				headers['cookie'] = authResult.cookie
			}
			headers['Accept'] = 'text/xml'
			
			response.success = getClosure(result)
			
			response.failure = getClosure(result)
		}
		
		return result
	}
	
	def postClosure(result) {
		{ res ->
			result['code'] = res.statusLine.statusCode as int
			result['contentType'] =  res.entity.contentType?.value
			StringWriter writer = new StringWriter()
			IOUtils.copy(res.entity.content, writer, Charset.forName('UTF-8'))
			result['body'] = writer.toString()
		}
	}
	
	def getClosure(result) {
		{ res, reader ->
				result['code'] = res.statusLine.statusCode as int
				result['contentType'] =  res.entity.contentType?.value
				result['body'] = reader.text
		}
	}

}
