package org.voidcat.voidcanon.data

import android.util.AtomicFile
import kotlinx.serialization.encodeToString
import org.voidcat.voidcanon.ai.CreationWizard
import org.voidcat.voidcanon.ai.WizardDraft
import java.io.File

class WizardDraftStore(directory: File) {
    private val file = AtomicFile(File(directory, "creation-wizard.json"))
    fun load(): WizardDraft? = runCatching { file.openRead().use { input ->
        val bytes = input.readBytes(); require(bytes.size <= 300000)
        CreationWizard.json.decodeFromString<WizardDraft>(bytes.toString(Charsets.UTF_8))
    } }.getOrNull()
    fun save(draft: WizardDraft) {
        val output = file.startWrite()
        try { output.write(CreationWizard.json.encodeToString(draft).toByteArray()); file.finishWrite(output) }
        catch (e: Exception) { file.failWrite(output); throw e }
    }
    fun clear() = file.delete()
}
