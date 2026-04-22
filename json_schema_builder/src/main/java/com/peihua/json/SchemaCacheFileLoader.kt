package com.peihua.json

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI

/// A schema cache that supports reading files from the local file system.
///
/// This implementation is used on platforms that have access to `dart:io`.
class SchemaCacheFileLoader {
    /// Creates a new file-aware schema cache.
    suspend fun getFile(uri: URI): String {
        return withContext(Dispatchers.IO) {
            require(uri.scheme == "file") { "URI scheme must be file" }
            val file = File(uri)
            file.readText(Charsets.UTF_8)
        }
    }
}
