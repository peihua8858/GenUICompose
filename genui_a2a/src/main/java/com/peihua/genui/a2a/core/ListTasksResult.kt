package com.peihua.genui.a2a.core

import kotlinx.serialization.Serializable

@Serializable
data class ListTasksResult(
    // The list of [Task] objects matching the specified filters and
    // pagination.
    val tasks: List<Task>,

    // The total number of tasks available on the server that match the filter
    // criteria (ignoring pagination).
    val totalSize: Int,

    // The maximum number of tasks requested per page.
    val pageSize: Int,

    // An opaque token for retrieving the next page of results.
    // If this string is empty, there are no more pages.
    val nextPageToken: String,
) {
}