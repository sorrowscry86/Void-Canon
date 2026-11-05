package org.voidcat.voidcanon.ai

import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Base64
import java.util.concurrent.TimeUnit

class CloudflareImageClientTest {
    private lateinit var server: MockWebServer
    private lateinit var client: CloudflareImageClient
    private lateinit var connection: ImageConnection
    private val id = "00000000-0000-4000-8000-000000000001"
    @Before fun setup() {
        val certificate = HeldCertificate.Builder().addSubjectAlternativeName("localhost").build()
        val serverTls = HandshakeCertificates.Builder().heldCertificate(certificate).build()
        val clientTls = HandshakeCertificates.Builder().addTrustedCertificate(certificate.certificate).build()
        server = MockWebServer().apply { useHttps(serverTls.sslSocketFactory(), false); start() }
        client = CloudflareImageClient(OkHttpClient.Builder().sslSocketFactory(clientTls.sslSocketFactory(), clientTls.trustManager)
            .retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false).build())
        connection = ImageConnection(server.url("/").toString(), "test-scoped-image-device-token")
    }
    @After fun cleanup() { server.shutdown() }
    private fun response(requestId: String = id, model: String = CloudflareImageClient.MODEL) = buildJsonObject {
        put("requestId", requestId); put("model", model); put("image", Base64.getEncoder().encodeToString(byteArrayOf(1, 2, 3)))
        put("reservedNeurons", 32)
    }.toString()

    @Test fun onlyPromptSizeAndChosenReferenceLeaveDevice() = runBlocking {
        server.enqueue(MockResponse().setBody(response()))
        val result = client.generate(connection, id, "A harbor", ImageSize.Background, byteArrayOf(9, 8))
        assertArrayEquals(byteArrayOf(1, 2, 3), result.bytes)
        val sent = server.takeRequest(5, TimeUnit.SECONDS)!!
        assertEquals("/v1/images", sent.path)
        assertEquals("Bearer test-scoped-image-device-token", sent.getHeader("Authorization"))
        val body = Json.parseToJsonElement(sent.body.readUtf8()).jsonObject
        assertEquals(setOf("requestId", "prompt", "size", "reference"), body.keys)
        assertEquals("background", body["size"]!!.jsonPrimitive.content)
        assertArrayEquals(byteArrayOf(9, 8), Base64.getDecoder().decode(body["reference"]!!.jsonPrimitive.content))
        assertFalse(connection.toString().contains(connection.accessToken))
    }
    @Test fun quotaFailureAndRedirectNeverRetry() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(429))
        val quota = runCatching { client.generate(connection, id, "Portrait", ImageSize.Portrait, null) }.exceptionOrNull()
        assertTrue(quota!!.message!!.contains("budget"))
        assertEquals(1, server.requestCount)
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", server.url("/other")))
        assertTrue(runCatching { client.generate(connection, id, "Portrait", ImageSize.Portrait, null) }.isFailure)
        assertEquals(2, server.requestCount)
    }
    @Test fun mismatchedResponseAndOversizedBodyAreRejected() = runBlocking {
        server.enqueue(MockResponse().setBody(response(requestId = "other")))
        assertTrue(runCatching { client.generate(connection, id, "Portrait", ImageSize.Portrait, null) }.isFailure)
        server.enqueue(MockResponse().setBody(response(model = "paid-alternate")))
        assertTrue(runCatching { client.generate(connection, id, "Portrait", ImageSize.Portrait, null) }.isFailure)
        server.enqueue(MockResponse().setBody("{}").setHeader("Content-Length", 13000000))
        assertTrue(runCatching { client.generate(connection, id, "Portrait", ImageSize.Portrait, null) }.isFailure)
    }
    @Test fun cancellationStopsTheCallWithoutAnotherRequest() = runBlocking {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val task = launch(Dispatchers.IO) { client.generate(connection, id, "Portrait", ImageSize.Portrait, null) }
        assertNotNull(withContext(Dispatchers.IO) { server.takeRequest(5, TimeUnit.SECONDS) })
        task.cancelAndJoin()
        assertEquals(1, server.requestCount)
    }
    @Test fun insecureConnectionsAreRejectedBeforeSending() {
        for (endpoint in listOf("http://example.com", "https://user:password@example.com", "https://example.com?secret=x")) {
            assertTrue(runCatching { ImageConnection(endpoint, connection.accessToken).validate() }.isFailure)
        }
    }
}
