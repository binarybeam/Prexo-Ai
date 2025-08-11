# Prexo - AI

**Integrate Google Gemini AI into your Android apps — now with Agentic Task Detection**

PrexoAI is a lightweight Kotlin library that makes it effortless to bring Gemini AI’s capabilities to your Android projects. With support for **text, image, and multi-modal inputs**, persistent conversation history, and **smart agentic task detection**, you can go beyond chat — automate actions, parse structured JSON commands, and build futuristic AI-powered experiences.

---

## Features

* Simple Gemini integration with minimal setup.
* Multi-modal support for text, images, and combined inputs.
* Agentic task detection returning structured JSON.
* Persistent memory for contextual conversations.
* Configurable model, safety settings, and system prompts.
* Quick and lightweight.

---

## Installation

Add JitPack to your `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

And include the dependency in your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.binarybeam:Prexo-Ai:1.0.0")
}
```

---

## Quick Start

```kotlin
val ai = Ai()
ai.updateModel(apiKey = "YOUR_API_KEY", agentMode = true)

ai.sendMessage("Turn on the flashlight", object : AiCallback {
    override fun onNormalResponse(message: String) { println("AI: $message") }
    override fun onAgenticResponse(message: String, jsonObject: List<JSONObject>) {
        println("Agentic Task: $message")
        println("Actions: $jsonObject")
    }
    override fun onError(error: String) { println("Error: $error") }
})
```

---

## Agentic Mode

When enabled, responses for actionable tasks are returned in JSON format only.

```json
{
  "id": "wifi",
  "state": "on",
}
```

---

## Configuration

```kotlin
ai.updateModel(
    apiKey = "YOUR_API_KEY",
    modelName = "gemini-2.5-flash",
    agentMode = false,
    systemInstruction = "You're a helpful assistant"
)
```

---

## Memory Management

```kotlin
ai.clearMemory()
```

---

## Advanced Examples

**1. Sending Multiple Messages**

```kotlin
ai.sendMessage(listOf("What's the weather in New York?", "Translate it to French"), callback)
```

**2. Sending an Image for Analysis**

```kotlin
val bitmap: Bitmap = ...
ai.sendMessage(bitmap, callback)
```

**3. Multi-modal Input**

```kotlin
val bitmap: Bitmap = ...
val content = Content(role = "user", parts = listOf(TextPart("Describe this"), ImagePart(bitmap)))
ai.sendMessage(content, callback)
```

**4. Handling Multiple Actions**

```kotlin
override fun onAgenticResponse(message: String, jsonObject: List<JSONObject>) {
    jsonObject.forEach { task ->
        when (task.getString("id")) {
            "wifi" -> toggleWifi(task.getString("state"))
            "bluetooth" -> toggleBluetooth(task.getString("state"))
        }
    }
}
```

**6. Dynamic Model Switching**

```kotlin
fun switchToProModel() {
    ai.updateModel(apiKey = "YOUR_API_KEY", modelName = "gemini-2.5-pro", agentMode = true)
}
```

---

## Use Cases

* AI chatbots with contextual memory.
* Smart assistants for device control.
* Automated file/contact/media management.
* In-app support powered by Gemini.
* Creative tools for AI-generated content.

---

## License

Apache 2.0 License © 2025 [Prexoft](https://github.com/prexoft)
