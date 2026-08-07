package io.github.vadimtoptunov.kassitestdata.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import io.github.vadimtoptunov.kassitestdata.generators.CatalogItem
import io.github.vadimtoptunov.kassitestdata.generators.TestDataCatalog
import io.github.vadimtoptunov.kassitestdata.settings.KassiSettings

/**
 * "Insert Test Data". Opens a two-level popup — first the category (Persona, Bank account, Card, National
 * ID, Passport, Phone…), then the items in it (with speed search) — so the now-large catalog isn't one
 * flat wall. Inserts the chosen value at the caret. Available from the editor menu, Tools menu, and a shortcut.
 */
class InsertTestDataAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.getData(CommonDataKeys.EDITOR) != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project

        val insert: (CatalogItem) -> Unit = { item ->
            val text = item.produce(KassiSettings.getInstance().seedOrNull())
            WriteCommandAction.runWriteCommandAction(project) {
                EditorModificationUtil.insertStringAtCaret(editor, text)
            }
        }

        val byGroup = TestDataCatalog.allItems().groupBy { it.group }
        val groups = GROUP_ORDER.filter(byGroup::containsKey) + byGroup.keys.filterNot { it in GROUP_ORDER }.sorted()

        JBPopupFactory.getInstance()
            .createListPopup(GroupStep(groups, byGroup, insert))
            .showInBestPositionFor(editor)
    }

    private companion object {
        // Most-used first; anything not listed is appended alphabetically.
        val GROUP_ORDER = listOf(
            "Persona", "Bank account", "Card", "BIC", "National ID", "Tax ID", "Identifier", "Crypto", "Business", "Phone", "Passport",
        )
    }
}

/** Top level: pick a category. */
private class GroupStep(
    groups: List<String>,
    private val byGroup: Map<String, List<CatalogItem>>,
    private val insert: (CatalogItem) -> Unit,
) : BaseListPopupStep<String>("Insert Test Data", groups) {

    override fun getTextFor(value: String): String = "$value  (${byGroup[value]?.size ?: 0})"

    override fun hasSubstep(selectedValue: String?): Boolean = true

    override fun onChosen(selectedValue: String, finalChoice: Boolean): PopupStep<*> =
        ItemStep(selectedValue, byGroup[selectedValue].orEmpty(), insert)
}

/** Second level: pick an item within the category (speed search enabled). */
private class ItemStep(
    title: String,
    items: List<CatalogItem>,
    private val insert: (CatalogItem) -> Unit,
) : BaseListPopupStep<CatalogItem>(title, items) {

    override fun getTextFor(value: CatalogItem): String = value.label

    override fun isSpeedSearchEnabled(): Boolean = true

    override fun onChosen(selectedValue: CatalogItem, finalChoice: Boolean): PopupStep<*>? =
        doFinalStep { insert(selectedValue) }
}
