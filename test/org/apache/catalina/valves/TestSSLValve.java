/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.catalina.valves;

import static org.hamcrest.core.StringContains.containsString;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.URLClassLoader;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;

import org.apache.catalina.Globals;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.connector.Request;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestSSLValve {

	public static class MockRequest extends Request {

		private Connector mockConnector = EasyMock.createMock(Connector.class);

		public MockRequest() {
			super();
			setCoyoteRequest(new org.apache.coyote.Request());
		}

		@Override
		public void setAttribute(String name, Object value) {
			getCoyoteRequest().getAttributes().put(name, value);
		}

		@Override
		public Object getAttribute(String name) {
			return getCoyoteRequest().getAttribute(name);
		}

		public void setHeader(String header, String value) {
			getCoyoteRequest().getMimeHeaders().setValue(header).setString(value);
		}

		public void addHeader(String header, String value) {
			getCoyoteRequest().getMimeHeaders().addValue(header).setString(value);
		}

		@Override
		public Connector getConnector() {
			return mockConnector;
		}

	}

	private static final String[] CERTIFICATE_LINES = new String[] { "-----BEGIN CERTIFICATE-----",
			"MIIFXTCCA0WgAwIBAgIJANFf3YTJgYifMA0GCSqGSIb3DQEBCwUAMEUxCzAJBgNV",
			"BAYTAkFVMRMwEQYDVQQIDApTb21lLVN0YXRlMSEwHwYDVQQKDBhJbnRlcm5ldCBX",
			"aWRnaXRzIFB0eSBMdGQwHhcNMTcwNTI2MjEzNjM3WhcNMTgwNTI2MjEzNjM3WjBF",
			"MQswCQYDVQQGEwJBVTETMBEGA1UECAwKU29tZS1TdGF0ZTEhMB8GA1UECgwYSW50",
			"ZXJuZXQgV2lkZ2l0cyBQdHkgTHRkMIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIIC",
			"CgKCAgEA2ykNBanZz4cVITNpZcWNVmErUzqgSNrK361mj9vEdB1UkHatwal9jVrR",
			"QvFgfiZ8Gl+/85t0ebJhJ+rIr1ww6JE7v2s2MThENj95K5EwZOmgvw+CBlBYsFIz",
			"8BtjlVYy+v7RaGPXfjrFkexQP9UIaiIIog2ClDZirRvb+QxS930/YW5Qo+X6EX6W",
			"/m/HvlorD25U4ni2FQ0y+EMO2e1jD88cAAMoP5f+Mf6NBK8I6yUeaSuMq7WqtHGV",
			"e4F1WOg5z9J5c/M69rB0iQr5NUQwZ1mPYf5Kr0P6+TLh8DJphbVvmHJyT3bgofeV",
			"JYl/kdjiXS5P/jwY9tfmhu04tsyzopWRUFCcj5zCiqZYaMn0wtDn08KaAh9oOlg8",
			"Z6mJ9i5EybkLm63W7z7LxuM+qnYzq4wKkKdx8hbpASwPqzJkJeXFL/LzhKdZuHiR",
			"clgPVYnm98URwhObh073dKguG/gkhcnpXcVBBVdVTJZYGBvTpQh0afXd9bcBwOzY",
			"t4MDpGiQB2fLzBOEZhQ37kUcWPmZw5bNPxhx4yE96Md0rx/Gu4ipAHuqLemb1SL5",
			"uWNesVmgY3OXaIamQIm9BCwkf8mMvoYdAT+lukTUZLtJ6s2w+Oxnl10tmb+6sTXy",
			"UB3WcBTp/o3YjAyJPnM1Wq6nVNQ4W2+NbV5purGAP09sumxeJj8CAwEAAaNQME4w",
			"HQYDVR0OBBYEFCGOYMvymUG2ZZT+lK4LvwEvx731MB8GA1UdIwQYMBaAFCGOYMvy",
			"mUG2ZZT+lK4LvwEvx731MAwGA1UdEwQFMAMBAf8wDQYJKoZIhvcNAQELBQADggIB",
			"AG6m4nDYCompUtRVude1qulwwAaYEMHyIIsfymI8uAE7d2o4bGjVpAUOdH/VWSOp",
			"Rzx0oK6K9cHyiBlKHw5zSZdqcRi++tDX3P9Iy5tXO//zkhMEnSpk6RF2+9JXtyhx",
			"Gma4yAET1yES+ybiFT21uZrGCC9r69rWG8JRZshc4RVWGwZsd0zrATVqfY0mZurm",
			"xLgU4UOvkTczjlrLiklwwU68M1DLcILJ5FZGTWeTJ/q1wpIn9isK2siAW/VOcbuG",
			"xdbGladnIFv+iQfuZG0yjcuMsBFsQiXi6ONM8GM+dr+61V63/1s73jYcOToEsTMM",
			"3bHeVffoSkhZvOGTRCI6QhK9wqnIKhAYqu+NbV4OphfE3gOaK+T1cASXUtSQPXoa",
			"sEoIVmbQsWRBhWvYShVqvINsH/hAT3Cf/+SslprtQUqiyt2ljdgrRFZdoyB3S7ky",
			"KWoZRvHRj2cKU65LVYwx6U1A8SGmViz4aHMSai0wwKzOVv9MGHeRaVhlMmMsbdfu",
			"wKoKJv0xYoVwEh1rB8TH8PjbL+6eFLeZXYVZnH71d5JHCghZ8W0a11ZuYkscSQWk",
			"yoTBqEpJloWksrypqp3iL4PAL5+KkB2zp66+MVAg8LcEDFJggBBJCtv4SCWV7ZOB",
			"WLu8gep+XCwSn0Wb6D3eFs4DoIiMvQ6g2rS/pk7o5eWj", "-----END CERTIFICATE-----" };

	private static final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
	private static MutableClassLoader mutableClassLoader;
	private static URLClassLoader previousClassLoader;
	private static Locale previousLocale;

	@ClassRule
	public static TemporaryFolder folder = new TemporaryFolder();

	@SuppressWarnings("deprecation")
	@BeforeClass
	public static void setUpClass() throws Exception {
		previousClassLoader = (URLClassLoader) Thread.currentThread().getContextClassLoader();
		mutableClassLoader = new MutableClassLoader(previousClassLoader);
		Thread.currentThread().setContextClassLoader(mutableClassLoader);

		mutableClassLoader.add(folder.getRoot().toURL());

		File loggingPropertiesFile = new File(folder.getRoot(), "logging.properties");
		loggingPropertiesFile.createNewFile();
		Properties loggingProperties = new Properties();
		loggingProperties.put("handlers", "java.util.logging.ConsoleHandler");
		loggingProperties.put("java.util.logging.ConsoleHandler.level", "FINE");
		loggingProperties.store(new FileOutputStream(loggingPropertiesFile), null);

		System.setErr(new PrintStream(errContent));

		previousLocale = Locale.getDefault();
		Locale.setDefault(Locale.ENGLISH);
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
		Locale.setDefault(previousLocale);

		System.setErr(null);

		Thread.currentThread().setContextClassLoader(previousClassLoader);
	}

	private SSLValve valve = new SSLValve();

	private MockRequest mockRequest = new MockRequest();
	private Valve mockNext = EasyMock.createMock(Valve.class);

	@Before
	public void setUp() throws Exception {
		valve.setNext(mockNext);
		mockNext.invoke(mockRequest, null);
		EasyMock.replay(mockNext);
	}

	@After
	public void tearDown() throws Exception {
		errContent.reset();
	}

	@Test
	public void testSslHeader() {
		final String headerName = "myheader";
		final String headerValue = "BASE64_HEADER_VALUE";
		mockRequest.setHeader(headerName, headerValue);

		Assert.assertEquals(headerValue, valve.mygetHeader(mockRequest, headerName));
	}

	@Test
	public void testSslHeaderNull() {
		final String headerName = "myheader";
		mockRequest.setHeader(headerName, null);

		Assert.assertNull(valve.mygetHeader(mockRequest, headerName));
	}

	@Test
	public void testSslHeaderNullModHeader() {
		final String headerName = "myheader";
		final String nullModHeaderValue = "(null)";
		mockRequest.setHeader(headerName, nullModHeaderValue);

		Assert.assertNull(valve.mygetHeader(mockRequest, nullModHeaderValue));
	}

	@Test
	public void testSslHeaderNullName() throws Exception {
		Assert.assertNull(valve.mygetHeader(mockRequest, null));
	}

	@Test
	public void testSslHeaderMultiples() throws Exception {
		final String headerName = "myheader";
		final String headerValue = "BASE64_HEADER_VALUE";
		mockRequest.addHeader(headerName, headerValue);
		mockRequest.addHeader(headerName, "anyway won't be found");

		Assert.assertEquals(headerValue, valve.mygetHeader(mockRequest, headerName));
	}

	@Test
	public void testSslClientCertHeaderSingleSpace() throws Exception {
		String singleSpaced = certificateSingleLine(" ");
		mockRequest.setHeader(valve.getSslClientCertHeader(), singleSpaced);

		valve.invoke(mockRequest, null);

		assertCertificateParsed();
	}

	@Test
	public void testSslClientCertHeaderMultiSpace() throws Exception {
		String singleSpaced = certificateSingleLine("    ");
		mockRequest.setHeader(valve.getSslClientCertHeader(), singleSpaced);

		valve.invoke(mockRequest, null);

		assertCertificateParsed();
	}

	@Test
	public void testSslClientCertHeaderTab() throws Exception {
		String singleSpaced = certificateSingleLine("\t");
		mockRequest.setHeader(valve.getSslClientCertHeader(), singleSpaced);

		valve.invoke(mockRequest, null);

		assertCertificateParsed();
	}

	@Test
	public void testSslClientCertNull() throws Exception {
		valve.invoke(mockRequest, null);

		EasyMock.verify(mockNext);
		Assert.assertNull((X509Certificate[]) mockRequest.getAttribute(Globals.CERTIFICATES_ATTR));
		Assert.assertEquals(0, errContent.toString().length());
	}

	@Test
	public void testSslClientCertShorter() throws Exception {
		mockRequest.setHeader(valve.getSslClientCertHeader(), "shorter than hell");

		valve.invoke(mockRequest, null);

		EasyMock.verify(mockNext);
		Assert.assertNull((X509Certificate[]) mockRequest.getAttribute(Globals.CERTIFICATES_ATTR));
		Assert.assertEquals(0, errContent.toString().length());
	}

	@Test
	public void testSslClientCertIgnoredBeginEnd() throws Exception {
		String[] linesBeginEnd = Arrays.copyOf(CERTIFICATE_LINES, CERTIFICATE_LINES.length);
		linesBeginEnd[0] = "3fisjcme3kdsakasdfsadkafsd3";
		linesBeginEnd[linesBeginEnd.length - 1] = "erkcnzl3i3nxl3uasdnxñv3if";
		String beginEnd = certificateSingleLine(linesBeginEnd, " ");
		mockRequest.setHeader(valve.getSslClientCertHeader(), beginEnd);

		valve.invoke(mockRequest, null);

		assertCertificateParsed();
	}

	@Test
	public void testSslClientCertBadFormat() throws Exception {
		String[] linesDeleted = Arrays.copyOf(CERTIFICATE_LINES, CERTIFICATE_LINES.length / 2);
		String deleted = certificateSingleLine(linesDeleted, " ");
		mockRequest.setHeader(valve.getSslClientCertHeader(), deleted);

		valve.invoke(mockRequest, null);

		EasyMock.verify(mockNext);
		Assert.assertNull((X509Certificate[]) mockRequest.getAttribute(Globals.CERTIFICATES_ATTR));
		Assert.assertThat(errContent.toString(), containsString("WARN"));
		Assert.assertThat(errContent.toString(), containsString("java.security.cert.CertificateException"));
	}

	@Test
	public void testClientCertProviderNotFound() throws Exception {
		EasyMock.expect(mockRequest.getConnector().getProperty("clientCertProvider")).andStubReturn("wontBeFound");
		EasyMock.replay(mockRequest.getConnector());
		mockRequest.setHeader(valve.getSslClientCertHeader(), certificateSingleLine(" "));

		valve.invoke(mockRequest, null);

		Assert.assertNull((X509Certificate[]) mockRequest.getAttribute(Globals.CERTIFICATES_ATTR));
		Assert.assertThat(errContent.toString(), containsString("SEVERE"));
		Assert.assertThat(errContent.toString(), containsString("java.security.NoSuchProviderException"));
	}

	@Test
	public void testSslCipherHeaderPresent() throws Exception {
		String cipher = "ciphered-with";
		mockRequest.setHeader(valve.getSslCipherHeader(), cipher);

		valve.invoke(mockRequest, null);

		Assert.assertEquals(cipher, (String) mockRequest.getAttribute(Globals.CIPHER_SUITE_ATTR));
	}

	@Test
	public void testSslSessionIdHeaderPresent() throws Exception {
		String session = "ssl-session";
		mockRequest.setHeader(valve.getSslSessionIdHeader(), session);

		valve.invoke(mockRequest, null);

		Assert.assertEquals(session, (String) mockRequest.getAttribute(Globals.SSL_SESSION_ID_ATTR));
	}

	@Test
	public void testSslCipherUserKeySizeHeaderPresent() throws Exception {
		Integer keySize = 452;
		mockRequest.setHeader(valve.getSslCipherUserKeySizeHeader(), String.valueOf(keySize));

		valve.invoke(mockRequest, null);

		Assert.assertEquals(keySize, (Integer) mockRequest.getAttribute(Globals.KEY_SIZE_ATTR));
	}

	@Test(expected = NumberFormatException.class)
	public void testSslCipherUserKeySizeHeaderBadFormat() throws Exception {
		mockRequest.setHeader(valve.getSslCipherUserKeySizeHeader(), "not-an-integer");

		try {
			valve.invoke(mockRequest, null);
		} catch (NumberFormatException e) {
			Assert.assertNull(mockRequest.getAttribute(Globals.KEY_SIZE_ATTR));
			throw e;
		}
	}

	private static String certificateSingleLine(String[] lines, String separator) {
		StringBuilder singleSpaced = new StringBuilder();
		for (String current : lines) {
			singleSpaced.append(current).append(separator);
		}
		singleSpaced.deleteCharAt(singleSpaced.length() - 1);
		return singleSpaced.toString();
	}

	private static String certificateSingleLine(String separator) {
		return certificateSingleLine(CERTIFICATE_LINES, separator);
	}

	private void assertCertificateParsed() throws Exception {
		EasyMock.verify(mockNext);

		X509Certificate[] certificates = (X509Certificate[]) mockRequest.getAttribute(Globals.CERTIFICATES_ATTR);
		Assert.assertNotNull(certificates);
		Assert.assertEquals(1, certificates.length);
		Assert.assertNotNull(certificates[0]);
		Assert.assertEquals(0, errContent.toString().length());
	}

}
