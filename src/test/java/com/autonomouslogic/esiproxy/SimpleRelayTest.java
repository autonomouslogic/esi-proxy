package com.autonomouslogic.esiproxy;

import static com.autonomouslogic.esiproxy.test.TestConstants.MOCK_ESI_PORT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

/**
 * Tests the basic ESI relay functionality.
 */
@MicronautTest
@SetEnvironmentVariable(key = "ESI_BASE_URL", value = "http://localhost:" + MOCK_ESI_PORT)
public class SimpleRelayTest {
	@Inject
	EmbeddedServer server;

	@Inject
	OkHttpClient client;

	MockWebServer mockEsi;

	@BeforeEach
	@SneakyThrows
	void setup() {
		mockEsi = new MockWebServer();
		mockEsi.start(MOCK_ESI_PORT);
	}

	@AfterEach
	@SneakyThrows
	void stop() {
		mockEsi.shutdown();
	}

	@ParameterizedTest
	@ValueSource(strings = {"/", "/path", "/path/with/multiple/segments"})
	@SneakyThrows
	void shouldRelaySuccessfulGetRequests(String path) {
		mockEsi.enqueue(new MockResponse()
				.setResponseCode(200)
				.setBody("Test body")
				.addHeader("X-Server-Header", "Test server header"));

		var proxyResponse = client.newCall(new Request.Builder()
						.get()
						.url("http://localhost:" + server.getPort() + path)
						.header("X-Client-Header", "Test client header")
						.build())
				.execute();
		assertEquals(200, proxyResponse.code());
		assertEquals("Test body", proxyResponse.body().string());
		assertEquals("Test server header", proxyResponse.header("X-Server-Header"));

		var esiRequest = mockEsi.takeRequest(0, TimeUnit.SECONDS);

		assertEquals("localhost:" + MOCK_ESI_PORT, esiRequest.getHeader("Host"));
		assertEquals("Host", esiRequest.getHeaders().name(0));
		assertEquals("GET", esiRequest.getMethod());
		assertEquals(path, esiRequest.getPath());
		assertEquals("Test client header", esiRequest.getHeader("X-Client-Header"));

		assertNull(mockEsi.takeRequest(0, TimeUnit.SECONDS));
	}

	@Test
	@SneakyThrows
	void shouldNotFollowRedirects() {
		mockEsi.enqueue(new MockResponse()
				.setResponseCode(302)
				.addHeader("Location", "http://localhost:" + MOCK_ESI_PORT + "/redirected"));

		var proxyResponse = client.newCall(new Request.Builder()
						.get()
						.url("http://localhost:" + server.getPort())
						.build())
				.execute();
		assertEquals(302, proxyResponse.code());
		assertEquals("http://localhost:" + MOCK_ESI_PORT + "/redirected", proxyResponse.header("Location"));

		assertNotNull(mockEsi.takeRequest(0, TimeUnit.SECONDS));
		assertNull(mockEsi.takeRequest(0, TimeUnit.SECONDS));
	}
}
