package io.github.vadimtoptunov.kassitestdata.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Minimal settings page under Settings ▸ Tools ▸ Kassi Test Data. One field: the optional seed.
 */
class KassiSettingsConfigurable : Configurable {

    private var seedField: JBTextField? = null
    private var panel: JPanel? = null

    override fun getDisplayName(): String = "Kassi Test Data"

    override fun createComponent(): JComponent {
        val field = JBTextField().apply { columns = 20 }
        seedField = field
        val built = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Seed (optional):"), field, true)
            .addComponentFillVertically(JPanel(), 0)
            .panel
        built.toolTipText = "Leave blank for random data. A numeric seed makes every generated value reproducible."
        panel = built
        reset()
        return built
    }

    override fun isModified(): Boolean =
        seedField?.text?.trim() != KassiSettings.getInstance().seedText.trim()

    override fun apply() {
        KassiSettings.getInstance().seedText = seedField?.text?.trim().orEmpty()
    }

    override fun reset() {
        seedField?.text = KassiSettings.getInstance().seedText
    }

    override fun disposeUIResources() {
        seedField = null
        panel = null
    }
}
