package com.peihua.genui.model

import com.peihua.genui.primitives.JsonMap
import com.peihua.json.schema.Schema
import kotlinx.coroutines.flow.Flow


/**
 * An execution context for client functions, providing access to data and
 * other functions.
 */
interface ExecutionContext {
    /**
     * The path associated with this context.
     */
    val path: DataPath;

    /**
     * Retrieves a function by name from this context.
     */
    fun getFunction(name: String): ClientFunction?;

    /**
     * Subscribes to a path, resolving it against the current context.
     */
    fun <T> subscribe(path: DataPath): Flow<T?>;

    /**
     * Subscribes to a path and returns a [Stream].
     */
    fun <T> subscribeStream(path: DataPath): Flow<T?>;

    /**
     * Gets a value, resolving the path against the current context.
     */
    fun <T> getValue(path: DataPath): T?;

    /**
     * Updates the data model, resolving the path against the current context.
     */
    fun update(path: DataPath, contents: Any?);

    /**
     * Creates a new, nested ExecutionContext for a child widget.
     */
    fun nested(relativePath: DataPath): ExecutionContext;

    /**
     * Resolves a path against the current context's path.
     */
    fun resolvePath(pathToResolve: DataPath): DataPath;

    /**
     * Resolves any dynamic values (bindings or function calls) in the given
     *value .
     * */
    fun resolve(value: Any?): Flow<Any?>

    /**
     * Evaluates a dynamic boolean condition and returns a [Stream<bool>].
     */
    fun evaluateConditionStream(condition: Any?): Flow<Boolean>
}

/**
 * A function that can be invoked by the GenUI expression system.
 *
 * Functions are reactive, returning a [Stream] of values.
 * This allows functions to push updates to the UI (e.g. a clock or network
 * status).
 * The type of value a client function returns.
 */
enum class ClientFunctionReturnType(val value: String) {
    STRING("string"),
    NUMBER("number"),
    BOOLEAN("boolean"),
    ARRAY("array"),
    OBJECT("object"),
    ANY("any"),
    EMPTY("void"); // Called empty because void is a keyword.

}

interface ClientFunction {
    /**
     * The name of the function as used in expressions (e.g. 'stringFormat').
     */
    val name: String;

    /**
     * A human-readable description of what the function does and how to use it.
     */
    val description: String;

    /**
     * The schema for the arguments this function accepts.
     * Used for validation and tool definition generation for the LLM.
     */
    val argumentSchema: Schema;

    /**
     * The type of value this function returns.
     * Defaults to [ClientFunctionReturnType.any].
     */
    val returnType: ClientFunctionReturnType
        get() = ClientFunctionReturnType.ANY

    /**
     * Invokes the function with the given [args].
     *
     * Returns a stream of values.
     *
     * **Reactivity:**
     * - **Argument Changes:** If the input [args] change (e.g. because they were
     *   bound to a changing data path), the `ExpressionParser` will
     *   **re-invoke** this function with the new arguments. The previous stream
     *   will be cancelled, and the new stream subscribed to. Therefore, a single
     *   stream instance does *not* need to handle argument changes.
     * - **Internal Changes:** The stream *should* emit new values if the
     *   function's internal sources change (e.g. a clock tick, a network status
     *   change, or a subscription to a data path looked up via [context]).
     *
     * The [context] is provided to allow the function to resolve other paths
     * or interact with the `DataModel` if necessary (e.g. `subscribeToValue`).
     */
    fun execute(args: JsonMap, context: ExecutionContext): Flow<Any?>;
}
