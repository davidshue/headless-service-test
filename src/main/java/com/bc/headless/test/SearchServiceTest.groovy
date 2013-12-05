package com.bc.headless.test;

import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*
import static org.junit.Assert.*

import org.junit.Test

import com.bc.headless.HeadlessTestBase

class SearchServiceTest extends HeadlessTestBase
{
	private static final SERVICE_URI = "/ws/public/search/v1"
	
	@Test
	public void test()
	{
		def result = get(SERVICE_URI, [q:'baby'], null)
		assertEquals 'return code must be 200', 200, result['code']
		assertEquals 'text/xml;charset=UTF-8', result['contentType']
		
		def html = new XmlSlurper().parseText(result.body)
		assertEquals 'status must be Success', 'Success', html.status.toString()
		assertEquals 'status code must be 200 here', 200, html.statusCode.toString() as int
		int numResults = html.payload.numResults.toString() as int
		assertTrue 'at some results should be returned', numResults > 0
		if (numResults >= 20)
		{
			assertEquals 20, html.payload.docs.doc.size()
		}
		else
		{
			assertEquals numResults, html.payload.docs.doc.size()
		}
	}

}
