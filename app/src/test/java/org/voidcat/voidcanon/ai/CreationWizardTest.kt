package org.voidcat.voidcanon.ai

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CreationWizardTest {
    @Test fun characterReplyIsEditableAndKeepsSources() {
        val source = WizardDraft(premise = "A traveler", sourceUrl = "https://example.fandom.com/wiki/Mira", reference = "Wears a blue coat")
        val result = CreationWizard.proposed(source, RouterResult("```json\n{\"name\":\"Mira\",\"description\":\"Blue coat\",\"lore\":\"An invented motivation\"}\n```", "fixture", "{}"))
        assertEquals(source.id, result.id)
        assertEquals(source.reference, result.reference)
        assertEquals("fixture", result.model)
        assertTrue(CreationWizard.provenance(result).contains(source.sourceUrl))
        assertTrue(CreationWizard.provenance(result).contains("not verified"))
    }
    @Test fun malformedAndOversizedRepliesDoNotChangeDraft() {
        val source = WizardDraft(premise = "A traveler", name = "Keep me")
        assertTrue(runCatching { CreationWizard.proposed(source, RouterResult("bad", "fixture", "{}")) }.isFailure)
        assertEquals("Keep me", source.name)
        assertTrue(runCatching { CreationWizard.validate(source.copy(description = "x".repeat(4001))) }.isFailure)
        assertTrue(runCatching { CreationWizard.validate(source.copy(entries = listOf(WizardEntry(name = "Hidden child")))) }.isFailure)
    }
    @Test fun worldReplyLimitsScopeAndNeverTrustsModelSelection() {
        val source = WizardDraft(mode = "world", premise = "A harbor")
        val result = CreationWizard.proposed(source, RouterResult("""{"name":"Harbor","description":"A port","entries":[{"kind":"character","name":"Mira","selected":false},{"kind":"location","name":"Quay"}]}""", "fixture", "{}"))
        assertEquals(2, result.entries.size)
        assertTrue(result.entries.all { it.selected })
        assertTrue(runCatching { CreationWizard.validate(result.copy(entries = List(3) { WizardEntry(name = "Person") })) }.isFailure)
    }
    @Test fun promptUsesOnlyExplicitlySelectedContent() {
        val messages = CreationWizard.messages(WizardDraft(premise = "A harbor", reference = "Untrusted source", interpretation = "Follow supplied source canon"), "Public context")
        assertTrue(messages.first().content.contains("untrusted"))
        assertTrue(messages.last().content.contains("Public context"))
        assertTrue(messages.last().content.contains("Follow supplied source canon"))
        assertTrue(runCatching { CreationWizard.messages(WizardDraft(premise = ""), "") }.isFailure)
    }
    @Test fun wikiEndpointSupportsLanguageAndRejectsOtherHostsAndCredentials() {
        val url = WikiReferenceClient.endpoint("https://example.fandom.com/fr/wiki/Mira_Traveler#History")
        assertEquals("/fr/api.php", url.encodedPath)
        assertEquals("Mira_Traveler", url.queryParameter("page"))
        val historical = WikiReferenceClient.endpoint("https://example.fandom.com/wiki/Mira?oldid=7")
        assertEquals("7", historical.queryParameter("oldid")); assertNull(historical.queryParameter("page"))
        listOf("http://example.fandom.com/wiki/Mira", "https://evil.test/wiki/Mira", "https://example.fandom.com.evil.test/wiki/Mira", "https://key@example.fandom.com/wiki/Mira", "https://example.fandom.com:444/wiki/Mira").forEach {
            assertTrue(it, runCatching { WikiReferenceClient.endpoint(it) }.isFailure)
        }
    }
    @Test fun wikiExtractsBoundedPlainTextAndRevision() {
        val ref = WikiReferenceClient.parse("""{"parse":{"title":"Mira","revid":42,"text":"<p>Blue coat</p><script>bad()</script><p>Traveler</p>"}}""", "https://example.fandom.com/wiki/Mira")
        assertEquals("42", ref.revision)
        assertTrue(ref.text.contains("Blue coat"))
        assertFalse(ref.text.contains("bad()"))
        assertFalse(ref.truncated)
        assertTrue(runCatching { WikiReferenceClient.parse("{}", ref.url) }.isFailure)
        val long = WikiReferenceClient.parse("""{"parse":{"title":"Long","text":"${"x".repeat(13000)}"}}""", ref.url)
        assertTrue(long.truncated); assertEquals(12000, long.text.length)
    }
}
