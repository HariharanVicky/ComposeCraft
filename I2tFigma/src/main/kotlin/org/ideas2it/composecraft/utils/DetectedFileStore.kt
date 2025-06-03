package org.ideas2it.composecraft.utils

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile

/**
 * Storage and management system for detected file operations in the ComposeCraft plugin.
 * 
 * This singleton object handles the complete lifecycle of file operations within the IntelliJ IDE:
 * - File content analysis and categorization
 * - Resource type detection for Android projects
 * - Safe file system operations
 * - Directory structure management
 * - User notifications
 *
 * Key features:
 * - Intelligent file type detection
 * - Android resource directory structure support
 * - Safe file write operations with conflict detection
 * - Proper handling of different Android resource types
 * - IDE integration with virtual file system
 * - User feedback through notifications
 *
 * Example usage:
 * ```kotlin
 * val metadata = FileMetadata(
 *     relativePath = "app/src/main",
 *     fileName = "activity_main.xml",
 *     content = "<LinearLayout>...</LinearLayout>"
 * )
 * DetectedFileStore.save(project, metadata)
 * ```
 */
object DetectedFileStore {

    /**
     * Enumeration of Android resource types for proper file organization.
     * Each type corresponds to a specific directory under the 'res' folder.
     */
    private enum class AndroidResourceType {
        LAYOUT,      // UI layout files
        DRAWABLE,    // Images and drawable XML files
        VALUES,      // String, dimension, and style resources
        MENU,        // Menu resource files
        NAVIGATION,  // Navigation graph files
        ANIM,        // Animation files
        COLOR,       // Color resource files
        MIPMAP,      // Launcher icons
        RAW,         // Raw resource files
        XML,         // General XML resources
        FONT,        // Font files
        UNKNOWN      // Non-resource files
    }

    /**
     * Saves a file to the project's file system with proper type detection and path resolution.
     *
     * Process flow:
     * 1. Analyzes file content to determine type
     * 2. Resolves appropriate directory path
     * 3. Creates necessary directories
     * 4. Handles potential file conflicts
     * 5. Writes file content
     * 6. Refreshes IDE view
     * 7. Notifies user of operation result
     *
     * @param project The current IntelliJ project
     * @param fileMetadata Metadata containing file information
     */
    fun save(project: Project, fileMetadata: FileMetadata) {
        WriteCommandAction.runWriteCommandAction(project) {
            try {
                val baseDir = project.baseDir
                
                // Analyze content and determine proper file location
                val (finalFileName, fileType, resourceType) = determineFileTypeAndName(fileMetadata)
                
                // Adjust path based on Android resource type
                val adjustedPath = adjustPathForResourceType(fileMetadata.relativePath, resourceType)
                val targetDir = createOrFindDirectory(baseDir, adjustedPath)

                // Handle directory creation failure
                if (targetDir == null) {
                    notifyError("Failed to resolve or create directory: $adjustedPath", project)
                    return@runWriteCommandAction
                }

                // Check for existing file to prevent overwrites
                val existingFile = targetDir.findChild(finalFileName)
                if (existingFile != null) {
                    notifyWarning("$finalFileName already exists. Skipping.", project)
                    return@runWriteCommandAction
                }

                // Clean and write file content
                val cleanedCodeText = cleanCodeContent(fileMetadata.content)
                val newFile = targetDir.createChildData(this, finalFileName)
                
                VfsUtil.saveText(newFile, cleanedCodeText)
                newFile.refresh(false, false)

                // Refresh IDE view
                FileDocumentManager.getInstance().reloadFiles(newFile)
                VfsUtil.markDirtyAndRefresh(true, false, true, newFile)

                notifySuccess("$finalFileName created at $adjustedPath", project)
            } catch (e: Exception) {
                notifyError("Failed to generate file: ${e.message}", project)
                e.printStackTrace()
            }
        }
    }

    /**
     * Analyzes file content to determine its type and appropriate name.
     * 
     * @param fileMetadata The metadata of the file to analyze
     * @return Triple of (finalFileName, fileType, resourceType)
     */
    private fun determineFileTypeAndName(fileMetadata: FileMetadata): Triple<String, String, AndroidResourceType> {
        val content = fileMetadata.content.trim()
        val suggestedName = fileMetadata.fileName.substringBeforeLast(".", "")

        // Check for Android Manifest
        if (content.contains("<manifest") || content.contains("package=") || 
            content.contains("<application") || content.contains("<activity")) {
            return Triple("AndroidManifest.xml", "XML_MANIFEST", AndroidResourceType.XML)
        }

        // Process XML content
        if (isXmlContent(content)) {
            // Layout files take priority in XML detection
            if (isLayoutFile(content)) {
                return Triple("${suggestedName}.xml", "XML", AndroidResourceType.LAYOUT)
            }

            val resourceType = determineResourceType(content)
            
            // Determine appropriate file name based on resource type
            val fileName = when (resourceType) {
                AndroidResourceType.VALUES -> {
                    when {
                        content.contains("<string") && !content.contains("<style") && !hasLayoutTags(content) -> "strings.xml"
                        content.contains("<dimen") && !hasLayoutTags(content) -> "dimens.xml"
                        content.contains("<style") && !hasLayoutTags(content) -> "styles.xml"
                        content.contains("<color") && !hasLayoutTags(content) -> "colors.xml"
                        else -> "${suggestedName}.xml"
                    }
                }
                else -> "${suggestedName}.xml"
            }

            return Triple(fileName, "XML", resourceType)
        }

        // Check for Kotlin content
        if (isKotlinContent(content)) {
            return Triple("${suggestedName}.kt", "KOTLIN", AndroidResourceType.UNKNOWN)
        }

        // Check for Java content
        if (isJavaContent(content)) {
            return Triple("${suggestedName}.java", "JAVA", AndroidResourceType.UNKNOWN)
        }

        // Default to Kotlin
        return Triple("${suggestedName}.kt", "KOTLIN", AndroidResourceType.UNKNOWN)
    }

    /**
     * Checks if content appears to be XML based on structure and Android-specific attributes.
     */
    private fun isXmlContent(content: String): Boolean {
        return content.startsWith("<?xml") || 
               content.contains("<") && content.contains(">") &&
               (content.contains("android:") || content.contains("app:") ||
                content.contains("tools:") || content.contains("xmlns:"))
    }

    /**
     * Detects Kotlin code by checking for Kotlin-specific syntax and Android imports.
     */
    private fun isKotlinContent(content: String): Boolean {
        return content.contains("@Composable") || 
               content.contains("import androidx.compose") ||
               content.contains("import android.") ||
               content.contains("import androidx.") ||
               content.contains("package ") || 
               content.contains("class ") || 
               content.contains("fun ") || 
               content.contains("val ") || 
               content.contains("var ") || 
               content.contains("object ") ||
               content.contains("@AndroidEntryPoint") ||
               content.contains("@HiltAndroidApp") ||
               content.contains("Activity") ||
               content.contains("Fragment") ||
               content.contains("ViewModel") ||
               content.contains("Service") ||
               content.contains("Receiver")
    }

    /**
     * Detects Java code by checking for Java-specific syntax and Android imports.
     */
    private fun isJavaContent(content: String): Boolean {
        return content.contains("public class") || 
               content.contains("private class") ||
               content.contains("protected class") || 
               content.contains("class ") ||
               content.contains("interface ") || 
               content.contains("enum ") ||
               content.contains("extends Activity") ||
               content.contains("extends Fragment") ||
               content.contains("extends Service") ||
               content.contains("extends Application") ||
               content.contains("implements OnClickListener") ||
               content.contains("@Override") ||
               content.contains("import android.") ||
               content.contains("import androidx.")
    }

    /**
     * Determines if the XML content represents an Android layout file.
     */
    private fun isLayoutFile(content: String): Boolean {
        // Check for common layout root elements
        return content.contains("<androidx.constraintlayout.widget.ConstraintLayout") ||
               content.contains("<LinearLayout") ||
               content.contains("<RelativeLayout") ||
               content.contains("<FrameLayout") ||
               content.contains("<androidx.coordinatorlayout.widget.CoordinatorLayout") ||
               content.contains("<androidx.drawerlayout.widget.DrawerLayout") ||
               content.contains("<androidx.swiperefreshlayout.widget.SwipeRefreshLayout") ||
               // Check for layout attributes
               (content.contains("android:layout_width") && content.contains("android:layout_height")) ||
               content.contains("app:layout_constraint") ||
               content.contains("android:orientation") ||
               // Check for common view elements
               (content.contains("<Button") || content.contains("<TextView") || 
                content.contains("<ImageView") || content.contains("<EditText") ||
                content.contains("<RecyclerView") || content.contains("<ScrollView"))
    }

    /**
     * Checks if XML content contains any layout-related tags.
     */
    private fun hasLayoutTags(content: String): Boolean {
        return content.contains("android:layout_") ||
               content.contains("app:layout_") ||
               content.contains("<Button") ||
               content.contains("<TextView") ||
               content.contains("<ImageView") ||
               content.contains("<EditText") ||
               content.contains("<RecyclerView") ||
               content.contains("<ScrollView") ||
               content.contains("<LinearLayout") ||
               content.contains("<RelativeLayout") ||
               content.contains("<ConstraintLayout") ||
               content.contains("<FrameLayout")
    }

    /**
     * Determines the Android resource type based on XML content analysis.
     */
    private fun determineResourceType(content: String): AndroidResourceType {
        return when {
            // Layout files - check first
            isLayoutFile(content) -> AndroidResourceType.LAYOUT

            // Navigation files
            content.contains("<navigation") || 
            content.contains("android:navigation") ||
            content.contains("app:startDestination") -> AndroidResourceType.NAVIGATION

            // Menu files
            content.contains("<menu") || 
            (content.contains("<item") && content.contains("android:menuCategory")) -> AndroidResourceType.MENU

            // Animation files
            content.contains("<animator") || 
            content.contains("<objectAnimator") ||
            content.contains("<set") && content.contains("android:interpolator") -> AndroidResourceType.ANIM

            // Drawable files
            content.contains("<vector") || 
            content.contains("<shape") ||
            content.contains("<selector") || 
            content.contains("<ripple") ||
            content.contains("<inset") || 
            content.contains("<bitmap") ||
            content.contains("<nine-patch") || 
            content.contains("<layer-list") ||
            content.contains("android:drawable") -> AndroidResourceType.DRAWABLE

            // Color files - check before values
            (content.contains("<color") || content.contains("android:color")) &&
            !hasLayoutTags(content) -> AndroidResourceType.COLOR

            // Values files - check last
            (content.contains("<resources") || 
            content.contains("<string") ||
            content.contains("<dimen") ||
            content.contains("<style") ||
            content.contains("<declare-styleable") ||
            content.contains("<integer") ||
            content.contains("<bool") ||
            content.contains("<array") ||
            content.contains("<plurals") ||
            content.contains("<attr")) &&
            !hasLayoutTags(content) -> AndroidResourceType.VALUES

            else -> AndroidResourceType.XML
        }
    }

    /**
     * Adjusts the file path based on Android resource type.
     * Ensures files are placed in the correct resource directory.
     */
    private fun adjustPathForResourceType(basePath: String, resourceType: AndroidResourceType): String {
        // Respect existing resource paths
        if (basePath.contains("/res/")) return basePath

        return when (resourceType) {
            AndroidResourceType.LAYOUT -> "$basePath/res/layout"
            AndroidResourceType.DRAWABLE -> "$basePath/res/drawable"
            AndroidResourceType.VALUES -> "$basePath/res/values"
            AndroidResourceType.MENU -> "$basePath/res/menu"
            AndroidResourceType.NAVIGATION -> "$basePath/res/navigation"
            AndroidResourceType.ANIM -> "$basePath/res/anim"
            AndroidResourceType.COLOR -> "$basePath/res/color"
            AndroidResourceType.MIPMAP -> "$basePath/res/mipmap"
            AndroidResourceType.RAW -> "$basePath/res/raw"
            AndroidResourceType.FONT -> "$basePath/res/font"
            AndroidResourceType.XML -> "$basePath/res/xml"
            AndroidResourceType.UNKNOWN -> basePath
        }
    }

    /**
     * Cleans the code content by removing unnecessary file type indicators.
     */
    private fun cleanCodeContent(content: String): String {
        val lines = content.lines()
        val firstLine = lines.firstOrNull()?.trim() ?: return content
        
        // Remove file type indicator if present
        return if (firstLine in listOf("kotlin", "xml", "java", "groovy", "gradle", "json")) {
            lines.drop(1).joinToString("\n").trimStart()
        } else {
            content.trimStart()
        }
    }

    /**
     * Creates or finds a directory path in the virtual file system.
     * Creates any missing directories in the path.
     */
    private fun createOrFindDirectory(baseDir: VirtualFile, relativePath: String): VirtualFile? {
        var current = baseDir
        for (segment in relativePath.split("/")) {
            current = current.findChild(segment) ?: current.createChildDirectory(this, segment)
        }
        return current
    }

    /**
     * Notification helper methods for user feedback
     */
    private fun notifySuccess(message: String, project: Project) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("ComposeCraft Notifications")
            .createNotification(message, NotificationType.INFORMATION)
            .notify(project)
    }

    private fun notifyError(message: String, project: Project) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("ComposeCraft Notifications")
            .createNotification(message, NotificationType.ERROR)
            .notify(project)
    }

    private fun notifyWarning(message: String, project: Project) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("ComposeCraft Notifications")
            .createNotification(message, NotificationType.WARNING)
            .notify(project)
    }
}