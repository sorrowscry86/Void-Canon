package org.voidcat.voidcanon.ai

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test

class DeepSeekClientTest {
    @Test fun modelDiscoveryUsesDirectAuthenticationWithoutSendingStoryText() = runBlocking {
        MockWebServer().use { server ->
            server.start(); server.enqueue(MockResponse().setBody("""{"data":[{"id":"fixture/current-model"},{"id":"fixture/current-model"},{"id":"bad id"}]}"""))
            val client = OpenRouterClient(baseUrl = server.url("/v1/").toString(), providerName = "DeepSeek", routerRouting = false)
            assertEquals(listOf("fixture/current-model"), client.directModelIds("fixture-key"))
            val request = server.takeRequest()
            assertEquals("GET", request.method); assertEquals("/v1/models", request.path)
            assertEquals("Bearer fixture-key", request.getHeader("Authorization"))
            assertEquals(0L, request.bodySize)
        }
    }
    @Test fun directRequestHasOnlyItsOwnCredentialAndNoRouterOrTemperatureOptions() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"model":"fixture/text","choices":[{"finish_reason":"stop","message":{"content":"A complete response"}}],"usage":{"total_tokens":5}}"""))
            server.start()
            val client = OpenRouterClient(baseUrl = server.url("/v1/").toString(), providerName = "DeepSeek", routerRouting = false)
            val result = client.complete("fixture-deepseek-key", "fixture/text", listOf(PromptMessage("user", "Hello")), false)
            val request = server.takeRequest()
            assertEquals("/v1/chat/completions", request.path)
            assertEquals("Bearer fixture-deepseek-key", request.getHeader("Authorization"))
            val body = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
            assertFalse(body.containsKey("provider")); assertFalse(body.containsKey("temperature"))
            assertFalse(body.containsKey("response_format"))
            assertEquals("A complete response", result.text)
            assertEquals(1, server.requestCount)
        }
    }

    @Test fun structuredStoryExplicitlyRequestsJsonWithoutAddingASecondCall() = runBlocking {
        MockWebServer().use { server ->
            val reply = """{"protocol":1,"baseRevision":7,"narrative":"Mira waits."}"""
            server.enqueue(MockResponse().setBody(buildJsonObject {
                putJsonArray("choices") { addJsonObject {
                    put("finish_reason", "stop"); putJsonObject("message") { put("content", reply) }
                } }
            }.toString()))
            server.start()
            val client = OpenRouterClient(baseUrl = server.url("/v1/").toString(), providerName = "DeepSeek", routerRouting = false)
            val result = client.complete("fixture-key", "fixture/text", listOf(PromptMessage("system", "Reply with JSON.")),
                false, jsonObjectResponse = true)
            val body = Json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
            assertEquals("json_object", body.getValue("response_format").jsonObject.getValue("type").jsonPrimitive.content)
            assertFalse(body.containsKey("provider")); assertFalse(body.containsKey("temperature"))
            assertEquals(reply, result.text)
            assertEquals(1, server.requestCount)
        }
    }

    @Test fun freeOnlyAndImageRequestsAreRejectedBeforeDispatch() = runBlocking {
        MockWebServer().use { server ->
            server.start()
            val client = OpenRouterClient(baseUrl = server.url("/v1/").toString(), providerName = "DeepSeek", routerRouting = false)
            assertTrue(runCatching { client.complete("fixture", "fixture/text", emptyList(), true) }.isFailure)
            assertTrue(runCatching { client.complete("fixture", "fixture/text", emptyList(), false, "data:image/png;base64,AA==") }.isFailure)
            assertEquals(0, server.requestCount)
        }
    }

    @Test fun authenticationFailureIsSanitizedAndNotRetried() = runBlocking {
        MockWebServer().use { server ->
            server.start(); server.enqueue(MockResponse().setResponseCode(401).setBody("private provider diagnostic"))
            val client = OpenRouterClient(baseUrl = server.url("/v1/").toString(), providerName = "DeepSeek", routerRouting = false)
            val error = runCatching { client.complete("fixture-key", "fixture/text", emptyList(), false) }.exceptionOrNull()
            assertNotNull(error); assertTrue(error!!.message.orEmpty().contains("DeepSeek"))
            assertFalse(error.message.orEmpty().contains("private provider diagnostic"))
            assertFalse(error.message.orEmpty().contains("fixture-key"))
            assertEquals(1, server.requestCount)
        }
    }
}
