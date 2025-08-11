package com.prexoft.prexoai

import org.json.JSONObject

interface AiCallback {
    fun onNormalResponse(message: String)
    fun onAgenticResponse(message: String, jsonObject: List<JSONObject>)
    fun onError(error: String)
}