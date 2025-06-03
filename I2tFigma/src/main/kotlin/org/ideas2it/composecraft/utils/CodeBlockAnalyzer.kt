package org.ideas2it.composecraft.utils

import java.io.File
import java.util.regex.Pattern

/**
 * Utility class for analyzing code blocks in the ComposeCraft plugin.
 * This analyzer determines whether code snippets can be generated as valid files
 * based on their content and programming language.
 *
 * Key features:
 * - Language-specific code analysis
 * - Support for Kotlin, Java, and XML files
 * - Android component detection
 * - Resource file validation
 *
 * Supported languages and their validations:
 * 1. Kotlin:
 *    - Classes, interfaces, and objects
 *    - Composable functions
 *    - Android components (Activities, Fragments, ViewModels)
 *
 * 2. XML:
 *    - Android layouts
 *    - Resource files
 *    - Navigation graphs
 *    - Drawable resources
 *
 * 3. Java:
 *    - Classes and interfaces
 *    - Enums and annotations
 *
 * Example usage:
 * ```kotlin
 * val kotlinCode = """
 *     class MainActivity : AppCompatActivity() {
 *         // ...
 *     }
 * """
 * val canGenerate = CodeBlockAnalyzer.canGenerateFile(kotlinCode, "kotlin")
 * // Returns true as it contains a valid class definition
 * ```
 */
object CodeBlockAnalyzer {

    /**
     * Analyzes a code block to determine if it can be generated as a valid file.
     * The analysis is based on the presence of specific language constructs and
     * Android-specific components.
     *
     * Decision criteria:
     * - For Kotlin: Checks for classes, interfaces, objects, composables, and Android components
     * - For XML: Validates resource file structure and Android layout components
     * - For Java: Verifies class definitions and other Java-specific constructs
     *
     * @param content The code content to analyze
     * @param language The programming language of the code ("kotlin", "java", or "xml")
     * @return true if the code can be generated as a file, false otherwise
     */
    fun canGenerateFile(content: String, language: String): Boolean {
        val normalizedLang = language.lowercase()
        return when {
            // Kotlin file validation
            normalizedLang == "kotlin" -> {
                content.contains("class ") ||    // Class definitions
                content.contains("interface ") || // Interface definitions
                content.contains("object ") ||    // Singleton objects
                content.contains("fun ") ||       // Function declarations
                content.contains("@Composable") || // Jetpack Compose functions
                // Android components
                content.contains("Activity") ||   // Activity classes
                content.contains("Fragment") ||   // Fragment classes
                content.contains("ViewModel")     // ViewModel classes
            }
            
            // XML file validation
            normalizedLang == "xml" -> {
                content.contains("<?xml") ||           // XML declaration
                content.contains("<resources") ||      // Resource files
                content.contains("<layout") ||         // Data binding layouts
                // Layout components
                content.contains("<LinearLayout") ||   // Linear layouts
                content.contains("<RelativeLayout") || // Relative layouts
                content.contains("<ConstraintLayout") ||// Constraint layouts
                content.contains("<navigation") ||     // Navigation graphs
                content.contains("<menu") ||           // Menu resources
                // Drawable resources
                content.contains("<vector") ||         // Vector drawables
                content.contains("<shape") ||          // Shape drawables
                content.contains("<selector")          // State list drawables
            }
            
            // Java file validation
            normalizedLang == "java" -> {
                content.contains("class ") ||      // Class definitions
                content.contains("interface ") ||  // Interface definitions
                content.contains("enum ") ||       // Enum declarations
                content.contains("@interface")     // Annotation declarations
            }

            // Unsupported file types
            else -> false
        }
    }
} 