package com.peihua.genui.a2a.core

import com.peihua.json.AnyValueSerializer
import kotlinx.serialization.Serializable

@Serializable
data class ListTasksParams(
    // Optional. Filter tasks to only include those belonging to this specific
    // context ID (e.g., a conversation or session).
    val contextId: String?,
    // Optional. Filter tasks by their current [TaskState].
    val status: TaskState?,
    // The maximum number of tasks to return in a single response.
    // Must be between 1 and 100, inclusive. Defaults to 50.
    val pageSize: Int = 50,
    // An opaque token used to retrieve the next page of results.
    // This should be the value of `nextPageToken` from a previous
    // [ListTasksResult]. If omitted, the first page is returned.
    val pageToken: String?,

    // The number of recent messages to include in each task's history.
    // Must be non-negative. Defaults to 0 (no history included).
    val historyLength: Int = 0,

    // Optional. Filter tasks to include only those updated at or after this
    // timestamp (in milliseconds since the Unix epoch).
    val lastUpdatedAfter: Int?,

    // Whether to include associated artifacts in the returned tasks.
    // Defaults to `false` to minimize payload size. Set to `true` to retrieve
    // artifacts.
    val includeArtifacts: Boolean = false,

    // Optional. Request-specific metadata for extensions or custom use cases.
    val metadata: Map<String, @Serializable(AnyValueSerializer::class) Any>?,
) {


}