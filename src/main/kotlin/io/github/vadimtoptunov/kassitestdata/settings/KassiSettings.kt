package io.github.vadimtoptunov.kassitestdata.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

/**
 * Application-level settings. Currently just the optional seed: when set, every generated
 * value/persona is reproducible (determinism is part of the product DNA).
 */
@Service(Service.Level.APP)
@State(name = "KassiTestDataSettings", storages = [Storage("kassiTestData.xml")])
class KassiSettings : PersistentStateComponent<KassiSettings.State> {

    data class State(var seedText: String = "")

    private var myState = State()

    override fun getState(): State = myState
    override fun loadState(state: State) { myState = state }

    var seedText: String
        get() = myState.seedText
        set(value) { myState.seedText = value }

    /** The configured seed, or null when the field is blank / not a valid Long (→ non-deterministic). */
    fun seedOrNull(): Long? = myState.seedText.trim().toLongOrNull()

    companion object {
        fun getInstance(): KassiSettings =
            ApplicationManager.getApplication().getService(KassiSettings::class.java)
    }
}
