package com.prexoft.prexoai

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.Content
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

var prexoAi = GenerativeModel(
    modelName = "gemini-2.5-flash-lite",
    apiKey = "",
    safetySettings = listOf(
        SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.NONE),
        SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.NONE),
        SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.NONE),
        SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.NONE)
    )
)

val prexoHis = mutableListOf<Content>()
var isAgentModeEnabled = true

@OptIn(DelicateCoroutinesApi::class)
class Ai() {
    fun sendMessage(message: String, callback: AiCallback) {
        sendMessage(Content(role = "user", parts = listOf(TextPart(message))), callback)
    }

    fun sendMessage(image: Bitmap, callback: AiCallback) {
        sendMessage(Content(role = "user", parts = listOf(ImagePart(image))), callback)
    }

    fun sendMessage(content: Content, callback: AiCallback) {
        GlobalScope.launch(Dispatchers.Main) {
            try {
                val res = prexoAi.startChat(prexoHis).sendMessage(content)
                prexoHis.add(content)
                prexoHis.add(res.candidates[0].content)

                val response = (res.candidates[0].content.parts[0] as TextPart).text
                if (isAgentModeEnabled) {
                    if (response.startsWith("{") && response.endsWith("}")) {
                        try {
                            val jsonObject = JSONObject("""
                                 ${response.removePrefix("```json").removeSuffix("```").trim()}
                                 """.trim()
                            )
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
                            val jsonArray = JSONArray(response.removePrefix("```json").removeSuffix("```").trim())
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

    fun updateModel(apiKey: String, modelName: String = prexoAi.modelName, agentMode: Boolean = true, systemInstruction: String = "You're a GenZ AI assistant powered by Prexoft", safetySetting: List<SafetySetting> = prexoAi.safetySettings!!, customAgenticMessage: String = prompt) {
        isAgentModeEnabled = agentMode
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
