# ComposeCraftToolWindowFactory Documentation

## Overview
`ComposeCraftToolWindowFactory` is a Kotlin class that implements the `ToolWindowFactory` interface to create and manage a custom tool window in the IntelliJ IDEA IDE. This tool window provides a user interface for the ComposeCraft plugin, featuring a chat interface for AI interactions.

## Class Components

### Main Properties
- `toolbarPanel`: Contains the toolbar UI elements including model selector
- `chatPanel`: Handles the chat interface
- `project`: Reference to the current IntelliJ project
- `aiService`: Service for handling AI-related operations

### Key Features
1. **Chat Interface**
   - Main interaction panel for user communication
   - Displays messages and responses
   - Handles image processing and code generation

2. **Toolbar Integration**
   - Custom toolbar with model selection capabilities
   - Positioned at the top of the tool window

3. **Message Bus Integration**
   - Subscribes to `ComposeCraftNotifier.TOPIC`
   - Handles various events:
     - Refresh requests
     - History clearing
     - Settings changes
     - Image upload processing

## Main Functionality

### Tool Window Creation
The `createToolWindowContent` method sets up the UI structure:
1. Initializes the main panel with BorderLayout
2. Creates and positions the toolbar
3. Sets up the chat panel
4. Configures message bus subscription

### Image Processing
Handles image uploads with the following workflow:
1. Displays processing status
2. Shows loading indicator
3. Processes image using AI service
4. Updates UI with results or error messages
5. Supports optional text prompts with images

### Event Handling
Implements several event handlers through `ComposeCraftNotifier`:
- `onRefreshRequested`: Handles refresh operations
- `onHistoryCleared`: Clears chat history
- `onSettingsChanged`: Applies setting updates
- `onImageUploaded`: Processes uploaded images

## Dependencies
- IntelliJ Platform SDK
- Kotlin Coroutines
- Custom services:
  - `AIService`
  - `ComposeCraftNotifier`
- Custom UI components:
  - `ToolbarPanel`
  - `ChatPanel`

## Usage
This class is automatically instantiated by the IntelliJ Platform when the ComposeCraft plugin is loaded. It creates and manages the plugin's main interface within the IDE.

## Error Handling
- Implements try-catch blocks for image processing
- Displays error messages in the chat interface
- Manages loading states appropriately

## Threading
- Uses Kotlin Coroutines (`CoroutineScope(Dispatchers.IO)`) for asynchronous operations
- Ensures UI responsiveness during heavy operations like image processing

## Best Practices
1. Follows IntelliJ Platform guidelines for tool window creation
2. Implements proper resource management
3. Uses dependency injection for services
4. Maintains separation of concerns between UI and business logic 