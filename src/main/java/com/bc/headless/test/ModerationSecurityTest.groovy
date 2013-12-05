package com.bc.headless.test;

import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*
import static org.junit.Assert.*

import org.junit.Ignore
import org.junit.Test

import com.bc.headless.AuthResult
import com.bc.headless.HeadlessTestBase

class ModerationSecurityTest extends HeadlessTestBase
{
	// TODO: this test should be in the headless login service test
	@Test void testLogin()
	{
		AuthResult authResult = login( 'intl2member@babycenter.com', '432wer')
			
		assertEquals 200, authResult.code
		assertEquals 'text/xml;charset=UTF-8', authResult.contentType
		assertNotNull authResult.cookie
		assertNotNull authResult.authToken
		
		AuthResult badAuthResult = login('intl2member@babycenter.com', '432werbad')
		
		assertEquals 450, badAuthResult.code
		assertEquals 'text/xml;charset=UTF-8', badAuthResult.contentType
		assertNull badAuthResult.cookie
		assertNull badAuthResult.authToken
	}
	
	// TODO: this test should be in headless baby service test
	@Ignore
	@Test void testAddBaby()
	{
		AuthResult authResult = login('intl2member@babycenter.com', '432wer')
		
		assertEquals 200, authResult.code

		def inputBody = ['name': 'Justin', 'year': '2013', 'month': '2', 'day': '20', 'gender': 'M']
		def result = securePost('/ws/external/mobileApp/baby/v1', inputBody, authResult)
		assertEquals 200, result['code']
	}
	
	@Test void testDeniedModerationOnGuestAccess()
	{
		def result = post('/moderation/user/edit/2000', null, null)
		assertEquals 302, result['code']
	}

	@Test void testDeniedModerationOnNormalUser()
	{
		AuthResult authResult = login('intl2member@babycenter.com', '432wer')
		
		assertEquals 200, authResult.code

		def result = post('/moderation/user/edit/2000', null, authResult)
		assertEquals 403, result['code']
	}
	
	@Test void testLoadMember()
	{
		AuthResult authResult = login('intl2member@babycenter.com', '432wer')
		
		assertEquals 200, authResult.code
		
		def result = secureGet('/ws/external/member/v1', null, authResult) 
		
		assertEquals 200, result['code']
		assertEquals 'text/xml;charset=UTF-8', result['contentType']
		assertNotNull result['body']
		def xml = new XmlSlurper().parseText(result['body'])
		assertEquals 'intl2member@babycenter.com', xml.payload.member.emailAddress as String
	}

}
