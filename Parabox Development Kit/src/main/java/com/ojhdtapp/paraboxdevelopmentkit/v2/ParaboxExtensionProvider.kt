package com.ojhdtapp.paraboxdevelopmentkit.v2

import com.ojhdtapp.parabox.protocol.v2.ExtensionCapability
import com.ojhdtapp.paraboxdevelopmentkit.model.config_item.ParaboxConfigItem
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitAction
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitActionResult
import org.json.JSONObject

data class ParaboxExtensionDescriptor(
    val key: String,
    val name: String,
    val description: String = "",
    val iconPng: ByteArray? = null,
    val capabilities: Set<ExtensionCapability> = emptySet(),
)

data class ParaboxInitStepResult(
    val steps: List<ParaboxInitAction>,
    val nextStepIndex: Int,
    val validationError: String = "",
    val completed: Boolean = false,
    val updatedExtra: JSONObject = JSONObject(),
)

interface ParaboxExtensionProvider {
    val descriptor: ParaboxExtensionDescriptor

    /**
     * Handle initial fetch (currentStepIndex < 0) or user submission at [currentStepIndex].
     * Implementations should validate [submittedValue] when currentStepIndex >= 0,
     * mutate/return [accumulatedExtra], and return the next step index or validation error.
     */
    suspend fun onInitStep(
        wizardSessionId: String,
        currentStepIndex: Int,
        submittedKey: String?,
        submittedValue: Any?,
        accumulatedExtra: JSONObject,
    ): ParaboxInitStepResult

    /**
     * Return declarative config items shown when editing an existing connection.
     */
    suspend fun getConfigItems(currentExtra: JSONObject): List<ParaboxConfigItem> = emptyList()

    /**
     * Validate a single config item edit before applying it.
     */
    suspend fun validateConfig(
        configKey: String,
        value: Any?,
        currentExtra: JSONObject,
    ): ParaboxInitActionResult = ParaboxInitActionResult.Done

    /**
     * Create an isolated session instance for one ConnectionInfo.
     */
    fun createSession(sessionId: String, extra: JSONObject, hostPackageName: String): ParaboxSession
}
