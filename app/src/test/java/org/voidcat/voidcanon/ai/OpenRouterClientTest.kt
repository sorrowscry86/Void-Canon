package org.voidcat.voidcanon.ai

import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

class OpenRouterClientTest {
    @Test fun catalogUsesCurrentPricesAndVisionCapabilitiesNotNames() {
        val catalog = OpenRouterClient.parseModels("""{"data":[
          {"id":"real/free","name":"Free vision","context_length":8192,"top_provider":{"max_completion_tokens":2048},"architecture":{"input_modalities":["text","image"],"output_modalities":["text"]},"pricing":{"prompt":"0","completion":"0.0","request":"0","image":"0"}},
          {"id":"name:free","architecture":{"input_modalities":["text"],"output_modalities":["text"]},"pricing":{"prompt":"0.00001","completion":"0"}},
          {"id":"unknown","architecture":{"input_modalities":["text"],"output_modalities":["text"]},"pricing":{}},
          {"id":"flatfee","architecture":{"input_modalities":["text"],"output_modalities":["text"]},"pricing":{"prompt":"0","completion":"0","request":"0.1"}},
          {"id":"image-generator","architecture":{"input_modalities":["text"],"output_modalities":["image"]},"pricing":{"prompt":"0","completion":"0"}}
        ]}""")
        assertEquals(listOf("real/free"), catalog.filter { it.free }.map { it.id })
        assertTrue(catalog.first().vision)
        assertEquals(2048, catalog.first().maxCompletionTokens)
        assertEquals(4, catalog.size)
    }

    @Test fun freeRequestsCapAllPricesDisableFallbackAndSendOnlyChosenImage() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"model":"actual/free","choices":[{"message":{"content":"A green coat."},"finish_reason":"stop"}],"usage":{"total_tokens":20,"cost":0}}"""))
            server.start()
            val client = OpenRouterClient(server.url("/").toString())
            val result = client.complete("fixture-test-key", "chosen/free", listOf(PromptMessage("system", "Observe only"), PromptMessage("user", "One image")), true, "data:image/png;base64,fixture")
            val request = server.takeRequest(5, TimeUnit.SECONDS)!!
            val body = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
            assertEquals("chosen/free", body["model"]!!.jsonPrimitive.content)
            assertFalse(body.containsKey("models"))
            val provider = body["provider"]!!.jsonObject
            assertFalse(provider["allow_fallbacks"]!!.jsonPrimitive.boolean)
            listOf("prompt", "completion", "request", "image").forEach { assertEquals(0, provider["max_price"]!!.jsonObject[it]!!.jsonPrimitive.int) }
            val messages = body["messages"]!!.jsonArray
            assertEquals(2, messages.size)
            assertEquals("data:image/png;base64,fixture", messages.last().jsonObject["content"]!!.jsonArray.last().jsonObject["image_url"]!!.jsonObject["url"]!!.jsonPrimitive.content)
            assertEquals("actual/free", result.model)
            assertTrue(result.usageJson.contains("total_tokens"))
            assertEquals(1, server.requestCount)
        }
    }

    @Test fun rateLimitIsExplicitAndNeverRetried() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(429).setBody("sensitive provider payload must not be shown"))
            server.start()
            var error = ""
            try { OpenRouterClient(server.url("/").toString()).complete("fixture-key", "chosen/free", listOf(PromptMessage("user", "hello")), true) }
            catch (e: Exception) { error = e.message.orEmpty() }
            assertTrue(error.contains("rate limit"))
            assertFalse(error.contains("sensitive"))
            assertEquals(1, server.requestCount)
        }
    }

    @Test fun summaryBudgetIsSentAndCompleteSummaryIsReturned() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"choices":[{"message":{"content":"The harbor investigation continues."},"finish_reason":"stop"}]}"""))
            server.start()
            val messages = listOf(PromptMessage("user", "Summarize the harbor scene."))
            val model = RouterModel("chosen/free", "Free", 32768, false, true, "0", "0")
            val result = OpenRouterClient(server.url("/").toString()).complete("fixture-key", model.id, messages, true,
                maxTokens = summaryOutputTokens(model, messages))
            val body = Json.parseToJsonElement(server.takeRequest()!!.body.readUtf8()).jsonObject
            assertEquals(4096, body["max_tokens"]!!.jsonPrimitive.int)
            assertEquals("The harbor investigation continues.", result.text)
            assertEquals(1, server.requestCount)
        }
    }

    @Test fun exhaustedReasoningAndPartialTextReportOutputBudgetWithoutPublishing() = runBlocking {
        MockWebServer().use { server ->
            server.start()
            for (content in listOf("null", "\"Incomplete summary\"")) {
                server.enqueue(MockResponse().setBody("""{"choices":[{"message":{"content":$content},"finish_reason":"length"}]}"""))
                val error = runCatching {
                    OpenRouterClient(server.url("/").toString()).complete("fixture-key", "chosen/free",
                        listOf(PromptMessage("user", "Summarize")), true, maxTokens = 4096)
                }.exceptionOrNull()
                assertNotNull(error)
                assertTrue(error!!.message.orEmpty().contains("4096-token output budget"))
                assertTrue(error.message.orEmpty().contains("Reasoning"))
            }
            assertEquals(2, server.requestCount)
        }
    }

    @Test fun providerErrorInsideSuccessfulHttpResponseKeepsQuotaGuidance() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"error":{"code":429,"message":"private upstream detail"}}"""))
            server.start()
            val error = runCatching {
                OpenRouterClient(server.url("/").toString()).complete("fixture-key", "chosen/free",
                    listOf(PromptMessage("user", "Summarize")), true)
            }.exceptionOrNull()
            assertTrue(error!!.message.orEmpty().contains("rate limit or quota"))
            assertFalse(error.message.orEmpty().contains("private upstream"))
            assertEquals(1, server.requestCount)
        }
    }

    @Test fun cancelAbortsWaitingRequestWithoutAutomaticRetry() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBodyDelay(2, TimeUnit.SECONDS).setBody("{}"))
            server.start()
            val job = launch(Dispatchers.IO) { OpenRouterClient(server.url("/").toString()).complete("fixture-key", "chosen/free", listOf(PromptMessage("user", "hello")), true) }
            assertNotNull(server.takeRequest(5, TimeUnit.SECONDS))
            job.cancelAndJoin()
            assertTrue(job.isCancelled)
            assertEquals(1, server.requestCount)
        }
    }
}
