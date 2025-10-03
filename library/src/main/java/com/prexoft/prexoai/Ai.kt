package com.prexoft.prexoai

import android.annotation.SuppressLint
import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.ImagePart
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.TextPart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private var prexoAi = GenerativeModel(
    modelName = "gemini-2.5-flash-lite",
    apiKey = "",
    safetySettings = listOf(
        SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.NONE),
        SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.NONE),
        SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.NONE),
        SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.NONE)
    )
)

private val prexoHis = mutableListOf<Content>()
private var isAgentModeEnabled = true
private var maxHistoryLength = 30
private var contextualMode = true

@OptIn(DelicateCoroutinesApi::class)
class Ai() {
    fun sendMessage(message: String, callback: AiCallback) {
        sendMessage(Content(role = "user", parts = listOf(TextPart(message))), callback)
    }

    fun sendMessage(image: Bitmap, callback: AiCallback) {
        sendMessage(Content(role = "user", parts = listOf(ImagePart(image))), callback)
    }

    fun sendMessage(content: Content, callback: AiCallback) {
        if (prexoAi.apiKey.isBlank()) {
            callback.onError("Please provide an API key using 'ai.updateModel(apiKey = \"...\")' method.")
            return
        }
        GlobalScope.launch(Dispatchers.Main) {
            try {
                val res: GenerateContentResponse
                if (contextualMode && maxHistoryLength > 0) {
                    res = prexoAi.startChat(prexoHis).sendMessage(content)
                    prexoHis.add(content)
                    prexoHis.add(res.candidates[0].content)
                    if (prexoHis.size -2 > maxHistoryLength) prexoHis.subList(0, if (maxHistoryLength > 10) maxHistoryLength/3 else 2).clear()
                }
                else res = prexoAi.generateContent(content)

                val response = (res.candidates[0].content.parts[0] as TextPart).text.removePrefix("```json").removeSuffix("```").trim()
                if (isAgentModeEnabled) {
                    if (response.startsWith("{") && response.endsWith("}")) {
                        try {
                            val jsonObject = JSONObject(response)
                            var message = ""
                            if (jsonObject.has("message")) {
                                message = jsonObject.getString("message")
                                jsonObject.remove("message")
                            }
                            callback.onAgenticResponse(message, listOf(jsonObject))
                        }
                        catch (e: Exception) {
                            callback.onError(e.message?:"Invalid response format.")
                        }
                        return@launch
                    }
                    if (response.startsWith("[") && response.endsWith("]")) {
                        try {
                            val jsonArray = JSONArray(response)
                            val list = mutableListOf<JSONObject>()
                            var messages = ""

                            for (i in 0 until jsonArray.length()) {
                                val jsonObject = jsonArray.getJSONObject(i)
                                if (jsonObject.has("message")) {
                                    messages = messages + "\n" + jsonObject.getString("message")
                                    jsonObject.remove("message")
                                }
                                list.add(jsonArray.getJSONObject(i))
                            }
                            callback.onAgenticResponse(messages.removePrefix("\n"), list)
                        }
                        catch (e: Exception) {
                            callback.onError(e.message?:"Invalid response format.")
                        }
                        return@launch
                    }
                }
                callback.onNormalResponse(response)
            }
            catch (e: Exception) {
                callback.onError(e.message?:"Something went wrong.")
            }
        }
    }

    fun sendMessage(images: List<Bitmap>, callback: AiCallback) {
        val imageParts = mutableListOf<ImagePart>()
        images.forEach { imageParts.add(ImagePart(it)) }
        sendMessage(Content(role = "user", parts = imageParts), callback)
    }

    fun clearMemory() {
        prexoHis.clear()
    }

    fun updateModel(apiKey: String, modelName: String = prexoAi.modelName, agentMode: Boolean = true,
                    contextual: Boolean = true,
                    systemInstruction: String = "You're a GenZ AI assistant powered by Prexoft",
                    safetySetting: List<SafetySetting> = prexoAi.safetySettings!!,
                    customAgenticMessage: String = prompt, maxHistoryCount: Int = maxHistoryLength) {
        isAgentModeEnabled = agentMode
        maxHistoryLength = maxHistoryCount
        contextualMode = contextual

        prexoAi = GenerativeModel(
            modelName = modelName,
            apiKey = apiKey,
            safetySettings = safetySetting,
            systemInstruction = Content(role = "system", parts = listOf(TextPart(
                if (agentMode) systemInstruction + "\n" + customAgenticMessage
                else systemInstruction
            )))
        )
    }
}

@SuppressLint("ConstantLocale")
private val prompt = """
RESPONSE MODE RULES

If the user request is an agentic task, respond only in valid JSON format.
If the request is not an agentic task, respond normally in plain text.
Never mix JSON and plain text in the same reply.

JSON RULES

Always include "id" and "message" in every JSON object.
Add task-specific fields as required.
Multiple JSON objects allowed in an array.
No extra text outside JSON.
If there’s a conflict between ride, food, tweet → process only the first in user intent order.

SMART DATA FETCHING RULES

If a request needs device data and it can be fetched automatically, first return:
{"id": "data", "type": "<type>", "message": "📂 Fetching required data..."}
Files (delete, rename, copy, move) → "files"
Contacts (calls, texts, emails) → "contacts"
Photos/media → "photos"
Calendar/reminders → "calendar"
SMS reading → "sms"
Call logs → "callHistory"
If info cannot be fetched automatically (e.g., missing pickup location), ask the user in plain text.

AGENTIC TASK CATEGORIES

Communication
Call → "id": "call" → contact, platform
Text → "id": "text" → contact, text, platform
Email → "id": "email" → to, subject, body
Post Social Media → "id": "tweet" → content

Search & Information
Search Web → "id": "search" → query, source
Weather → "id": "weather" → location, date
Translate → "id": "translate" → text, targetLang
Currency Conversion → "id": "currency" → amount, from, to
Battery Status → "id": "battery"
Read Notifications → "id": "notifications"

Device & App Control
Open App → "id": "app" → platform
Open Website → "id": "web" → platform
Quick Action → "id": "action" → name, todo (flashlight, vibrate, brightness, volume)
Toggle Wi-Fi → "id": "wifi" → state: "on" | "off"
Toggle Bluetooth → "id": "bluetooth" → state: "on" | "off"
Toggle Airplane Mode → "id": "airplane" → state
Lock Phone → "id": "lock"
Do Not Disturb → "id": "dnd" → state

Media & Capture
Capture Photo/Video/Screenshot → "id": "capture" → mode: "front" | "back" | "screen"
Play Media → "id": "media" → mediaType, title, platform

Smart Home
"id": "smarthome" → device, action

Scheduling & Notes
Alarm/Reminder → "id": "reminder" → time, priority, note
Create Note → "id": "note" → content, optional title

Orders & Bookings
Food Order → "id": "food" → item, priceRange, preference
Ride Booking → "id": "ride" → pickup, drop, vehicle

File Management
Delete File/Folder → "id": "delete" → list
Rename File/Folder → "id": "rename" → list
Copy File/Folder → "id": "copy" → list
Move File/Folder → "id": "move" → list

Device Data Access
Data Fetch → "id": "data" → type: "photos" | "calendar" | "contacts" | "sms" | "files" | "callHistory"

FORMATTING RULES

Always use emojis in "message" for a friendly futuristic vibe.
Always return valid JSON for agentic tasks.
Never mix JSON with extra explanation.
""".trimIndent()