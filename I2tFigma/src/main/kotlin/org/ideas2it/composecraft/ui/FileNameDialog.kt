package org.ideas2it.composecraft.ui

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.GridBagLayout
import java.awt.GridBagConstraints
import javax.swing.JPanel
import javax.swing.JComponent

class FileNameDialog(
    private val suggestedName: String,
    private val fileType: String,
    private val description: String
) : DialogWrapper(true) {
    private val nameField = JBTextField(suggestedName.substringBeforeLast('.'))
    private val extension = suggestedName.substringAfterLast('.', "kt")

    init {
        title = "Confirm File Name"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()
        
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.anchor = GridBagConstraints.WEST
        gbc.insets = JBUI.insets(4)
        panel.add(JBLabel("File Type: $fileType"), gbc)

        if (description.isNotEmpty()) {
            gbc.gridy++
            panel.add(JBLabel("Description: $description"), gbc)
        }

        gbc.gridy++
        panel.add(JBLabel("File Name:"), gbc)

        gbc.gridx = 1
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        panel.add(nameField, gbc)

        // Add extension label
        gbc.gridx = 2
        gbc.fill = GridBagConstraints.NONE
        gbc.weightx = 0.0
        panel.add(JBLabel(".$extension"), gbc)

        return panel
    }

    fun getFileName(): String {
        val name = nameField.text.trim()
        return if (name.endsWith(".$extension")) {
            name
        } else {
            "$name.$extension"
        }
    }
} 