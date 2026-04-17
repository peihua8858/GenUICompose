package com.peihua.genui.a2a.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

sealed interface Event {
    /**
     * The type of this event, always 'status-update'.
     */
    val kind: String;

    /**
     * The unique ID of the updated task.
     */
    val taskId: String;

    /**
     * The unique context ID for the task.
     */
    val contextId: String;

    companion object {
        fun fromJson(json: JsonObject): Event {
            val kind = json["kind"].toString()
            return when (kind) {
                "status-update" -> StatusUpdate.fromJson(json)
                "task-status-update" -> TaskStatusUpdate.fromJson(json)
                "artifact-update" -> ArtifactUpdate.fromJson(json)
                else -> throw IllegalArgumentException("Invalid union type ${json["kind"]}!")
            }
        }

        fun statusUpdate(
            kind: String = "status_update",
            taskId: String,
            contextId: String,
            status: TaskStatus,
            final: Boolean = false,
        ): StatusUpdate {
            return StatusUpdate(kind, taskId, contextId, status, final)
        }

        fun taskStatusUpdate(
            kind: String = "task-status-update",
            taskId: String,
            contextId: String,
            status: TaskStatus,
            final: Boolean = false,
        ): TaskStatusUpdate {
            return TaskStatusUpdate(kind, taskId, contextId, status, final)
        }

        fun artifactUpdate(
            kind: String = "artifact-update",
            taskId: String,
            contextId: String,
            artifact: Artifact,
            append: Boolean,
            lastChunk: Boolean,
        ): ArtifactUpdate {
            return ArtifactUpdate(kind, taskId, contextId, artifact, append, lastChunk)
        }
    }
}

@Serializable
class StatusUpdate(
    override val kind: String = "status_update",
    //The unique ID of the updated task.
    override val taskId: String,
    //The unique context ID for the task.
    override val contextId: String,
    //The new status of the task.
    val status: TaskStatus,
    //If `true`, this is the final event for this task stream.
    @SerialName("final")
    val final: Boolean = false,
) : Event {
    companion object {
        fun fromJson(json: JsonObject): StatusUpdate {
            return Json.decodeFromJsonElement(json)
        }
    }
}

@Serializable
class TaskStatusUpdate(
    override val kind: String = "task-status-update",
    override val taskId: String,
    override val contextId: String,
    val status: TaskStatus,
    @SerialName("final")
    val final: Boolean = false,
) : Event {
    companion object {
        fun fromJson(json: JsonObject): TaskStatusUpdate {
            return Json.decodeFromJsonElement(json)
        }

    }

}
@Serializable
class ArtifactUpdate(
    override val kind: String = "artifact-update",
    override val taskId: String,
    override val contextId: String,
    val artifact: Artifact,
    val append: Boolean,
    val lastChunk: Boolean,
) : Event {
companion object{
    fun fromJson(json: JsonObject): ArtifactUpdate {
        return Json.decodeFromJsonElement(json)
    }
}
}