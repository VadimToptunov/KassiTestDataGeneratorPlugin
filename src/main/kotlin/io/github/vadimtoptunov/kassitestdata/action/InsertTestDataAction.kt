package io.github.vadimtoptunov.kassitestdata.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.SimpleListCellRenderer
import io.github.vadimtoptunov.kassitestdata.generators.CatalogItem
import io.github.vadimtoptunov.kassitestdata.generators.TestDataCatalog
import io.github.vadimtoptunov.kassitestdata.settings.KassiSettings
import javax.swing.JList

/**
 * The single entry point: "Insert Test Data". Opens a searchable popup of every generator ×
 * country × variant and inserts the chosen value at the caret. Available from the editor
 * context menu, the Tools menu, and (once bound) a keyboard shortcut.
 */
class InsertTestDataAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.getData(CommonDataKeys.EDITOR) != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project
        val items = TestDataCatalog.allItems()

        JBPopupFactory.getInstance()
            .createPopupChooserBuilder(items)
            .setTitle("Insert Test Data")
            .setNamerForFiltering { it.searchText }
            .setRenderer(object : SimpleListCellRenderer<CatalogItem>() {
                override fun customize(list: JList<out CatalogItem>, value: CatalogItem?, index: Int, selected: Boolean, hasFocus: Boolean) {
                    text = value?.label.orEmpty()
                }
            })
            .setItemChosenCallback { item ->
                val seed = KassiSettings.getInstance().seedOrNull()
                val text = item.produce(seed)
                WriteCommandAction.runWriteCommandAction(project) {
                    insertAtCaret(editor, text)
                }
            }
            .createPopup()
            .showInBestPositionFor(editor)
    }

    private fun insertAtCaret(editor: Editor, text: String) {
        EditorModificationUtil.insertStringAtCaret(editor, text)
    }
}
