package com.peihua.genui.engine

import com.peihua.genui.model.DataModel
import com.peihua.genui.model.InMemoryDataModel
import okhttp3.internal.toImmutableMap


class DataModelStore {
    private val _dataModels: MutableMap<String, DataModel> = mutableMapOf()
    private val _attachedSurfaces: MutableSet<String> = mutableSetOf()

    /**
     * Retrieves the data model for the given [surfaceId], creating it if it
     * does not exist.
     */
    fun getDataModel(surfaceId: String): DataModel {
        return _dataModels.putIfAbsent(surfaceId, InMemoryDataModel())!!
    }

    /** Removes the data model for the given [surfaceId] and detaches the surface.*/
    fun removeDataModel(surfaceId: String) {
        val model = _dataModels.remove(surfaceId);
        model?.dispose();
        _attachedSurfaces.remove(surfaceId);
    }

    /** Marks the surface with the given [surfaceId] as attached.*/
    fun attachSurface(surfaceId: String) {
        _attachedSurfaces.add(surfaceId);
    }

    /** Marks the surface with the given [surfaceId] as detached.*/
    fun detachSurface(surfaceId: String) {
        _attachedSurfaces.remove(surfaceId);
    }

    /** An unmodifiable map of all registered data models.*/
    val dataModels: Map<String, DataModel>
        get() = _dataModels.toImmutableMap()

    /** Disposes of all data models in this store.*/
    fun dispose() {
        for (model in _dataModels.values) {
            model.dispose()
        }
    }
}