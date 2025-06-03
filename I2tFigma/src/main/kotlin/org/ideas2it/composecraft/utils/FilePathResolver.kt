package org.ideas2it.composecraft.utils

import java.io.File

/**
 * Utility object for resolving appropriate file paths for generated code in an Android project.
 * This resolver follows Android project structure conventions and determines the most suitable
 * location for new files based on their content and type.
 *
 * Key features:
 * - Analyzes code content to determine appropriate file locations
 * - Supports both Kotlin and Java source files
 * - Handles Android resource files (layouts, drawables, etc.)
 * - Infers package structure from existing project setup
 * - Follows Android clean architecture conventions for package organization
 */
object FilePathResolver {

    /**
     * Main entry point for resolving file paths based on code content.
     * 
     * @param code The source code content to analyze
     * @param language The programming language (kotlin, java, xml, etc.)
     * @param suggestedFileName Optional suggested name for the file
     * @param project The IntelliJ project instance
     * @return Pair of (fileName, directoryPath)
     */
    fun resolveFilePath(code: String, language: String, suggestedFileName: String?, project: com.intellij.openapi.project.Project): Pair<String, String> {
        // First check for Android-specific Java/Kotlin code patterns
        val isAndroidCode = isAndroidCode(code)
        val isSource = language.equals("kotlin", true) || language.equals("java", true)
        
        // Determine file name and path based on code content and type
        return when {
            isSource -> resolveSourceFilePath(code, language, suggestedFileName, project)
            language.equals("xml", true) -> resolveXmlFilePath(code, isAndroidCode, suggestedFileName)
            else -> "GeneratedFile.txt" to "app/src/main"
        }
    }

    /**
     * Checks for the existence of Kotlin and Java source directories in the project.
     * 
     * @param projectBasePath The root path of the project
     * @return Pair of (kotlinExists, javaExists) booleans
     */
    private fun checkSourceDirectories(projectBasePath: String): Pair<Boolean, Boolean> {
        val kotlinSrcPath = File(projectBasePath, "app/src/main/kotlin")
        val javaSrcPath = File(projectBasePath, "app/src/main/java")
        
        return Pair(
            kotlinSrcPath.exists(),
            javaSrcPath.exists()
        )
    }

    /**
     * Determines if the code contains Android-specific patterns.
     * Checks for Android components, annotations, imports, and lifecycle methods.
     * 
     * @param code The source code to analyze
     * @return true if the code appears to be Android-related
     */
    private fun isAndroidCode(code: String): Boolean {
        return code.contains("class") && (
            // Android component inheritance patterns
            code.contains("extends Activity") ||
            code.contains("extends AppCompatActivity") ||
            code.contains("extends Fragment") ||
            code.contains("extends DialogFragment") ||
            code.contains("extends Service") ||
            code.contains("extends BroadcastReceiver") ||
            code.contains("extends ContentProvider") ||
            code.contains("extends Application") ||
            // Kotlin inheritance patterns
            code.contains(": Activity") ||
            code.contains(": AppCompatActivity") ||
            code.contains(": Fragment") ||
            code.contains(": DialogFragment") ||
            code.contains(": Service") ||
            code.contains(": BroadcastReceiver") ||
            code.contains(": Application") ||
            // Android Jetpack components
            code.contains("extends ViewModel") ||
            code.contains(": ViewModel") ||
            code.contains("extends AndroidViewModel") ||
            code.contains(": AndroidViewModel") ||
            code.contains("extends RecyclerView.Adapter") ||
            code.contains(": RecyclerView.Adapter") ||
            code.contains("extends RecyclerView.ViewHolder") ||
            code.contains(": RecyclerView.ViewHolder") ||
            // Android annotations
            code.contains("@AndroidEntryPoint") ||
            code.contains("@HiltAndroidApp") ||
            code.contains("@Composable") ||
            code.contains("@Layout") ||
            code.contains("@WorkerThread") ||
            code.contains("@UiThread") ||
            // Android imports
            code.contains("import android.") ||
            code.contains("import androidx.") ||
            // Android lifecycle methods
            code.contains("onCreate") ||
            code.contains("onStart") ||
            code.contains("onResume") ||
            code.contains("onPause") ||
            code.contains("onStop") ||
            code.contains("onDestroy") ||
            code.contains("onCreateView") ||
            code.contains("onViewCreated")
        )
    }

    /**
     * Attempts to determine the root package name for the project.
     * Uses multiple strategies in order of preference:
     * 1. Gradle build files (namespace or applicationId)
     * 2. Android Manifest
     * 3. Existing source files
     * 4. Fallback to generated name based on project directory
     * 
     * @param projectBasePath The root path of the project
     * @param isKotlin Whether the code is Kotlin (affects source directory searching)
     * @return The inferred root package name
     */
    private fun inferRootPackage(projectBasePath: String, isKotlin: Boolean): String {
        // Strategy 1: Check build.gradle files
        val buildGradleKts = File(projectBasePath, "app/build.gradle.kts")
        val buildGradle = File(projectBasePath, "app/build.gradle")
        
        when {
            buildGradleKts.exists() -> {
                try {
                    val content = buildGradleKts.readText()
                    val namespaceRegex = Regex("""namespace\s*=\s*["']([^"']+)["']""")
                    val applicationIdRegex = Regex("""applicationId\s*=\s*["']([^"']+)["']""")
                    
                    // First try namespace as it's the newer approach
                    namespaceRegex.find(content)?.groupValues?.get(1)?.let { return it }
                    // Fallback to applicationId
                    applicationIdRegex.find(content)?.groupValues?.get(1)?.let { return it }
                } catch (e: Exception) {
                    // Ignore and try next method
                }
            }
            buildGradle.exists() -> {
                try {
                    val content = buildGradle.readText()
                    val namespaceRegex = Regex("""namespace\s+['"]([^'"]+)['"]""")
                    val applicationIdRegex = Regex("""applicationId\s+['"]([^'"]+)['"]""")
                    
                    // First try namespace
                    namespaceRegex.find(content)?.groupValues?.get(1)?.let { return it }
                    // Fallback to applicationId
                    applicationIdRegex.find(content)?.groupValues?.get(1)?.let { return it }
                } catch (e: Exception) {
                    // Ignore and try next method
                }
            }
        }

        // Strategy 2: Check Android Manifest
        val manifestFile = File(projectBasePath, "app/src/main/AndroidManifest.xml")
        if (manifestFile.exists()) {
            try {
                val content = manifestFile.readText()
                val packageRegex = Regex("""package\s*=\s*["']([^"']+)["']""")
                packageRegex.find(content)?.groupValues?.get(1)?.let { return it }
            } catch (e: Exception) {
                // Ignore and try next method
            }
        }

        // Strategy 3: Search source files
        val srcPath = if (isKotlin) "app/src/main/kotlin" else "app/src/main/java"
        val srcDir = File(projectBasePath, srcPath)
        
        if (srcDir.exists()) {
            // Look for main entry point files first
            val mainFiles = srcDir.walk()
                .filter { it.isFile && 
                         (it.name.contains("MainActivity") || 
                          it.name.contains("Application") ||
                          it.name.contains("App.kt") ||
                          it.name.contains("App.java")) }
                .toList()

            for (file in mainFiles) {
                try {
                    val content = file.readText()
                    val packageRegex = Regex("""package\s+([a-zA-Z0-9_.]+)""")
                    packageRegex.find(content)?.groupValues?.get(1)?.let { return it }
                } catch (e: Exception) {
                    continue
                }
            }

            // Fallback to checking first few source files
            val sourceFiles = srcDir.walk()
                .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
                .take(5)
                .toList()

            for (file in sourceFiles) {
                try {
                    val content = file.readText()
                    val packageRegex = Regex("""package\s+([a-zA-Z0-9_.]+)""")
                    packageRegex.find(content)?.groupValues?.get(1)?.let { return it }
                } catch (e: Exception) {
                    continue
                }
            }
        }

        // Strategy 4: Generate from project name
        val projectName = projectBasePath.split(File.separator).last()
            .replace(Regex("[^a-zA-Z0-9]"), "")
            .lowercase()
        
        return "com.example.$projectName"
    }

    /**
     * Resolves the appropriate path for source files (Kotlin/Java).
     * Determines package structure and file location based on code content.
     * 
     * @param code The source code content
     * @param language The programming language (kotlin/java)
     * @param suggestedFileName Optional suggested file name
     * @param project The IntelliJ project instance
     * @return Pair of (fileName, directoryPath)
     */
    private fun resolveSourceFilePath(code: String, language: String, suggestedFileName: String?, project: com.intellij.openapi.project.Project): Pair<String, String> {
        val isKotlin = language.equals("kotlin", true)
        val projectBasePath = project.basePath ?: ""
        
        // Check existing source directories
        val (kotlinExists, javaExists) = checkSourceDirectories(projectBasePath)
        
        // Determine appropriate source directory
        val srcPath = when {
            isKotlin && kotlinExists -> "app/src/main/kotlin"
            isKotlin && !kotlinExists && !javaExists -> "app/src/main/kotlin"
            !isKotlin && javaExists -> "app/src/main/java"
            !isKotlin && !javaExists -> "app/src/main/java"
            kotlinExists -> "app/src/main/kotlin"
            else -> "app/src/main/java"
        }

        // Get package structure
        val basePackage = inferRootPackage(projectBasePath, isKotlin)
        val (subPackage, className) = determineSourceFileDetails(code)
        
        // Build complete file path
        val targetPath = when {
            basePackage.isNotEmpty() && subPackage.isNotEmpty() -> {
                val basePackagePath = basePackage.replace('.', '/')
                "$srcPath/$basePackagePath/$subPackage"
            }
            basePackage.isNotEmpty() -> {
                val basePackagePath = basePackage.replace('.', '/')
                "$srcPath/$basePackagePath"
            }
            subPackage.isNotEmpty() -> {
                "$srcPath/$subPackage"
            }
            else -> srcPath
        }
        
        val fileName = suggestedFileName ?: "${className}.${if (isKotlin) "kt" else "java"}"
        return fileName to targetPath
    }

    /**
     * Resolves the appropriate path for XML resource files.
     * Determines the correct resource directory based on XML content.
     * 
     * @param code The XML content
     * @param isAndroidCode Whether the content appears to be Android-related
     * @param suggestedFileName Optional suggested file name
     * @return Pair of (fileName, directoryPath)
     */
    private fun resolveXmlFilePath(code: String, isAndroidCode: Boolean, suggestedFileName: String?): Pair<String, String> {
        // Handle non-XML content that was misidentified
        if (!code.trim().startsWith("<?xml") && !code.trim().startsWith("<") && isAndroidCode) {
            return (suggestedFileName ?: "GeneratedFile.java") to "app/src/main/java"
        }

        // Determine resource type and location
        val (targetPath, defaultFileName) = when {
            // Layout files
            code.contains("<LinearLayout") ||
            code.contains("<RelativeLayout") ||
            code.contains("<FrameLayout") ||
            code.contains("<androidx.constraintlayout.widget.ConstraintLayout") ||
            code.contains("<androidx.coordinatorlayout.widget.CoordinatorLayout") ||
            code.contains("<androidx.drawerlayout.widget.DrawerLayout") ||
            code.contains("<androidx.swiperefreshlayout.widget.SwipeRefreshLayout") ||
            (code.contains("android:layout_width") && code.contains("<")) -> 
                "app/src/main/res/layout" to "layout_generated.xml"
            
            // Navigation files
            code.contains("<navigation") ||
            code.contains("<fragment") && code.contains("android:name=") ->
                "app/src/main/res/navigation" to "nav_graph.xml"
            
            // Menu files
            code.contains("<menu") ||
            code.contains("<item") && code.contains("android:title=") ->
                "app/src/main/res/menu" to "menu_generated.xml"
            
            // Drawable files
            code.contains("<vector") ||
            code.contains("<shape") ||
            code.contains("<selector") ||
            code.contains("<ripple") ||
            code.contains("<animated-vector") ->
                "app/src/main/res/drawable" to "drawable_generated.xml"
            
            // Values files
            code.contains("<resources") && (
                code.contains("<string") ||
                code.contains("<dimen") ||
                code.contains("<color") ||
                code.contains("<style") ||
                code.contains("<theme") ||
                code.contains("<array") ||
                code.contains("<integer")
            ) -> "app/src/main/res/values" to "values.xml"
            
            // Manifest file
            code.contains("<manifest") ||
            code.contains("<application") ||
            code.contains("<activity") ||
            code.contains("<service") ||
            code.contains("<receiver") ||
            code.contains("<provider") ->
                "app/src/main" to "AndroidManifest.xml"
            
            else -> "app/src/main/res/layout" to "layout_generated.xml"
        }

        return (suggestedFileName ?: defaultFileName) to targetPath
    }

    /**
     * Analyzes source code to determine appropriate package and class name.
     * Uses naming conventions and code patterns to organize files according to clean architecture.
     * 
     * @param code The source code to analyze
     * @return Pair of (subPackagePath, className)
     */
    private fun determineSourceFileDetails(code: String): Pair<String, String> {
        // Check for Compose UI components
        val composableRegex = Regex("""@Composable\s+fun\s+([A-Za-z0-9_]+)""")
        val composableMatch = composableRegex.find(code)
        
        // Check for class inheritance patterns
        val classWithExtendsRegex = Regex("""class\s+([A-Za-z0-9_]+)(?:\s*:\s*([A-Za-z0-9_.<>]+))""")
        val classWithExtendsMatch = classWithExtendsRegex.find(code)
        
        // Check for regular class definitions
        val classRegex = Regex("""(class|interface|object)\s+([A-Za-z0-9_]+)""")
        val classMatch = classRegex.find(code)
        
        return when {
            // Composable functions go to UI/Compose package
            composableMatch != null -> {
                val name = composableMatch.groupValues[1]
                val screenName = if (name.endsWith("Screen")) name else "${name}Screen"
                "ui/compose/screens" to screenName
            }
            
            // Classes with inheritance
            classWithExtendsMatch != null -> {
                val name = classWithExtendsMatch.groupValues[1]
                val parentClass = classWithExtendsMatch.groupValues[2].trim()
                
                when {
                    parentClass.contains("Activity") -> 
                        "ui/activities" to if (name.endsWith("Activity")) name else "${name}Activity"
                    parentClass.contains("Fragment") -> 
                        "ui/fragments" to if (name.endsWith("Fragment")) name else "${name}Fragment"
                    parentClass.contains("ViewModel") -> 
                        "ui/viewmodels" to if (name.endsWith("ViewModel")) name else "${name}ViewModel"
                    parentClass.contains("Adapter") -> 
                        "ui/adapters" to if (name.endsWith("Adapter")) name else "${name}Adapter"
                    parentClass.contains("Repository") -> 
                        "data/repositories" to if (name.endsWith("Repository")) name else "${name}Repository"
                    parentClass.contains("DataSource") -> 
                        "data/sources" to if (name.endsWith("DataSource")) name else "${name}DataSource"
                    else -> "" to name
                }
            }
            
            // Regular classes - organized by naming convention
            classMatch != null -> {
                val name = classMatch.groupValues[2]
                when {
                    // UI Layer
                    name.endsWith("Activity") -> "ui/activities" to name
                    name.endsWith("Fragment") -> "ui/fragments" to name
                    name.endsWith("Dialog") -> "ui/dialogs" to name
                    name.endsWith("BottomSheet") -> "ui/bottomsheets" to name
                    name.endsWith("ViewModel") -> "ui/viewmodels" to name
                    name.endsWith("Adapter") -> "ui/adapters" to name
                    name.endsWith("ViewHolder") -> "ui/viewholders" to name
                    
                    // Data Layer
                    name.endsWith("Repository") -> "data/repositories" to name
                    name.endsWith("DataSource") -> "data/sources" to name
                    name.endsWith("Database") -> "data/database" to name
                    name.endsWith("Dao") -> "data/database/dao" to name
                    name.endsWith("Entity") -> "data/database/entities" to name
                    name.endsWith("Model") || name.endsWith("Dto") -> "data/models" to name
                    
                    // Domain Layer
                    name.endsWith("UseCase") -> "domain/usecases" to name
                    name.endsWith("Interactor") -> "domain/interactors" to name
                    
                    // Network Layer
                    name.endsWith("Api") || name.endsWith("Service") -> "data/network" to name
                    name.endsWith("Client") -> "data/network/clients" to name
                    name.endsWith("Interceptor") -> "data/network/interceptors" to name
                    
                    // Dependency Injection
                    name.endsWith("Module") -> "di/modules" to name
                    name.endsWith("Component") && code.contains("@Component") -> "di/components" to name
                    name.endsWith("Qualifier") -> "di/qualifiers" to name
                    
                    // Utilities
                    name.endsWith("Util") || name.endsWith("Utils") -> "utils" to name
                    name.endsWith("Helper") -> "utils/helpers" to name
                    name.endsWith("Manager") -> "utils/managers" to name
                    name.endsWith("Provider") -> "utils/providers" to name
                    name.endsWith("Factory") -> "utils/factories" to name
                    name.endsWith("Constants") -> "utils/constants" to name
                    
                    else -> "" to name
                }
            }
            
            else -> "" to "GeneratedFile"
        }
    }
} 