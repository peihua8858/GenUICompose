package com.peihua.genui.model

import com.peihua.genui.model.DataPath.Companion._separator

fun DataPath(path: String): DataPath {
    if (path == _separator) return DataPath.root;
    val segments = path.split(_separator).filter { it.isNotEmpty() }.toList()
    return DataPath(segments, path.startsWith(_separator));
}

class DataPath internal constructor(
    //The segments of the path.
    val segments: List<String>,
    //Whether the path is absolute (starts with a separator).
    val isAbsolute: Boolean,
) {

    companion object {
        const val _separator = "/";

        /// The root path.
        val root = DataPath(listOf<String>(), true);

    }


    /// The last segment of the path.
    val basename: String = if (segments.isEmpty()) "" else segments.last();

    /// The path without the last segment.
    val dirname: DataPath
        get() {
            if (segments.isEmpty()) return this;
            return DataPath(segments.subList(0, segments.size - 1), isAbsolute)
        }

    /// Joins this path with another path.
    fun join(other: DataPath): DataPath {
        if (isAbsolute && other.isAbsolute) {
            throw IllegalArgumentException("Cannot join two absolute paths: $this and $other");
        }
        if (other.isAbsolute) {
            return other;
        }
        val result = mutableListOf<String>()
        result.addAll(segments)
        result.addAll(other.segments)
        return DataPath(result, isAbsolute);
    }

    /// Returns whether this path starts with the other path.
    fun startsWith(other: DataPath): Boolean {
        if (other.isAbsolute && !isAbsolute) {
            return false;
        }
        if (other.segments.size > segments.size) {
            return false;
        }
        for ((index, item) in segments.withIndex()) {
            if (item != other.segments[index]) {
                return false;
            }
        }
        return true;
    }

    override fun toString(): String {
        val path = segments.joinToString(_separator);
        return if (isAbsolute) "$_separator$path" else path;
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DataPath

        if (isAbsolute != other.isAbsolute) return false
        if (segments != other.segments) return false
        if (basename != other.basename) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isAbsolute.hashCode()
        result = 31 * result + segments.hashCode()
        result = 31 * result + basename.hashCode()
        return result
    }

}
