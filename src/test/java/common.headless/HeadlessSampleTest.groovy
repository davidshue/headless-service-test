package common.headless

import org.junit.Test

import static org.junit.Assert.*
/**
 * Created by dshue1 on 6/11/16.
 */
class HeadlessSampleTest extends HTTPTestBase {
	private static final URI = '/ip/6.131.144.10/json'

	@Test
	void testIP() {
		def result = internalGet(URI)

		assertEquals 200, result['code']
		assertEquals 'application/json', result['contentType']
	}

}
