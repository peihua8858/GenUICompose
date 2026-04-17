package com.peihua.genui.model

import androidx.compose.runtime.State

typealias Function<T> = (path: DataPath, source: State<T>, twoWay: Boolean) -> Unit

interface DataModel {
    /**
     * Updates the data model at a specific absolute path and notifies all
     * relevant subscribers.
     *
     * If [absolutePath] is root, the entire data model is replaced
     * (if contents is a Map).
     */
    fun update(absolutePath: DataPath, contents: Any);

    /** Subscribes to a specific absolute path in the data model.*/
    fun <T> subscribe(absolutePath: DataPath): State<T?>

    /**
     * Binds an external state [source] to a [path] in the DataModel.
     *
     * **Side Effect:** Calling this method immediately performs a synchronous
     * `update()` on the DataModel at the specified [path] using the current
     * value of the [source].
     *
     * If [twoWay] is true, changes in the DataModel at [path] will also
     * update the [source] (assuming [source] is a [ValueNotifier]).
     *
     * Returns a function that disposes the binding.
     */
    fun <T> bindExternalState(): Function<T>

    /** Disposes resources and bindings.*/

    fun dispose();

    /**
     * Retrieves a static, one-time value from the data model at the
     * specified absolute path without creating a subscription.
     */
    fun <T> getValue(absolutePath: DataPath): T?;
}

/** Standard in-memory implementation of [DataModel].*/
class InMemoryDataModel : DataModel {
    override fun update(absolutePath: DataPath, contents: Any) {
        TODO("Not yet implemented")
    }

    override fun <T> subscribe(absolutePath: DataPath): State<T?> {
        TODO("Not yet implemented")
    }

    override fun <T> bindExternalState(): Function<T> {
        TODO("Not yet implemented")
    }

    override fun dispose() {
        TODO("Not yet implemented")
    }

    override fun <T> getValue(absolutePath: DataPath): T? {
        TODO("Not yet implemented")
    }

}