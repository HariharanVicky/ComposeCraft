package org.ideas2it.composecraft.utils

/**
 * Data class representing metadata for a detected file in the ComposeCraft plugin.
 * This class encapsulates all necessary information about a file that needs to be
 * processed or generated within the plugin's ecosystem.
 * 
 * Example usage:
 * ```kotlin
 * val metadata = FileMetadata(
 *     relativePath = "app/src/main/kotlin/com/example/ui",
 *     fileName = "MainScreen.kt",
 *     content = "@Composable fun MainScreen() { ... }",
 *     suggestedName = "MainScreen.kt",
 *     fileType = "kt",
 *     description = "Main composable screen of the application"
 * )
 * ```
 * 
 * @property relativePath The relative path of the file within the project structure
 *                       Example: "app/src/main/kotlin/com/example/ui"
 * 
 * @property fileName The name of the file including extension
 *                   Example: "MainScreen.kt", "activity_main.xml"
 * 
 * @property content The actual content/code of the file
 *                  This can be source code, XML, or any other file content
 * 
 * @property suggestedName The suggested name for the file, defaults to fileName
 *                        Useful when the file needs to be renamed or generated with a different name
 *                        Example: "CustomMainScreen.kt"
 * 
 * @property fileType The file extension, automatically extracted from fileName
 *                   Common values: "kt", "java", "xml", "gradle"
 *                   Used for determining file type and appropriate handling
 * 
 * @property description An optional description for the file
 *                      Useful for documentation and code generation purposes
 *                      Example: "Main screen containing the app's primary UI"
 */
data class FileMetadata(
    // The relative path where the file is or should be located
    val relativePath: String,
    
    // The actual file name with extension
    val fileName: String,
    
    // The file's content (source code, XML, etc.)
    val content: String,
    
    // Suggested alternative name, defaults to the actual file name
    val suggestedName: String = fileName,
    
    // File extension, automatically extracted from the file name
    val fileType: String = fileName.substringAfterLast('.'),
    
    // Optional description of the file's purpose
    val description: String = ""
)
