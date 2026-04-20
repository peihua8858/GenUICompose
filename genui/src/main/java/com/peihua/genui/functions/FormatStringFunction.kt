package com.peihua.genui.functions

import com.peihua.genui.Logger
import com.peihua.genui.model.ClientFunction
import com.peihua.genui.model.ClientFunctionReturnType
import com.peihua.genui.model.DataPath
import com.peihua.genui.model.ExecutionContext
import com.peihua.genui.primitives.JsonMap
import com.peihua.json.schema.S
import com.peihua.json.schema.Schema
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.stream.Stream

/// Formats a value as a string.
class FormatStringFunction : ClientFunction {
    override val name: String
        get() = "formatString"
    override val description: String
        get() = " Performs string interpolation of data model values and other functions in the catalog functions list and returns the resulting string. " +
                "The value string can contain interpolated expressions in the \\$\\{expression} format. " +
                " Supported expression types include: JSON Pointer paths to the data model (e.g., \\$\\{/absolute/path} or \\$\\{relative/path}), " +
                "and client-side function calls (e.g., \\$\\{now()}). Function arguments must be named (e.g., \\$\\{formatDate(value:\\$\\{/currentDate}, format:'MM-dd')})." +
                "  To include a literal \\$\\{sequence, escape it as \${.";
    override val returnType: ClientFunctionReturnType
        get() = ClientFunctionReturnType.STRING
    override val argumentSchema: Schema
        get() = S.obj(properties = mapOf("value" to S.any()));

    override fun execute(args: JsonMap, context: ExecutionContext): Flow<Any?> = flow {
        if (!args.containsKey("value")) emit("");
        val value = args["value"];

        ExpressionParser(context).parse(value?.toString() ?: "");
    }
}

/// Exception thrown when the maximum recursion depth is exceeded.
class RecursionExpectedException(override val message: String?) : Exception(message) {
    override fun toString(): String = "RecursionExpectedException: $message"
}

/// Parses and evaluates expressions in the A2UI `${expression}` format.
class ExpressionParser(private val context: ExecutionContext) {

    val _maxRecursionDepth = 100;

    /// Parses the input string and resolves any embedded expressions.
    ///
    /// The return value will always be a [Stream<String>].
    ///
    /// This method is the entry point for expression resolution. It handles
    /// escaping of the `${` sequence using a backslash (e.g. `\${`).
    fun parse(input: String, depth: Int = 0): Flow<String> = flow {
        if (depth > _maxRecursionDepth) {
            throw RecursionExpectedException("Max recursion depth reached in parse");
        }
        if (!input.contains("\\$\\{")) {
            emit(input);
        } else {
            _parseStringWithInterpolations(input = input, depth = depth + 1).collect { event ->
                emit(event.toString())
            }
        }
    }

    /// Evaluates a function call defined by [callDefinition].
    ///
    /// If [dependencies] is provided, the function will not be executed, but
    /// any data path dependencies within the arguments will be added to the set.
    fun evaluateFunctionCall(callDefinition: Map<String, Any?>, dependencies: MutableSet<DataPath>? = null, depth: Int = 0): Flow<Any?> {
        if (depth > _maxRecursionDepth) {
            throw RecursionExpectedException("Max recursion depth reached in evaluateFunctionCall");
        }

        val name = callDefinition["call"] as? String;
        if (name == null) {
            return flowOf(null)
        }

        // 1. Resolve arguments
        val args = mutableMapOf<String, Any?>()
        val argsJson = callDefinition["args"];
        var hasStreams = false;

        if (argsJson is Map<*, *>) {
            for (key in argsJson.keys) {
                val argName = key.toString();
                val value = argsJson[key];
                var resolvedValue: Any? = null

                if (value is String) {
                    resolvedValue = _parseStringWithInterpolations(
                        value,
                        dependencies,
                        depth = depth + 1,
                    );
                } else if (value is Map<*, *> && value.containsKey("path")) {
                    resolvedValue = _resolvePath(value["path"] as String, dependencies);
                } else if (value is Map<*, *> && value.containsKey("call")) {
                    resolvedValue = evaluateFunctionCall(value as Map<String, Any?>, dependencies = dependencies, depth = depth + 1);
                } else {
                    resolvedValue = value;
                }

                if (resolvedValue is Flow<*>) {
                    hasStreams = true;
                }
                args[argName] = resolvedValue;
            }
        } else if (argsJson != null) {
            Logger.wLog(
                "Function $name called with invalid args type: " +
                        "${argsJson.javaClass}. Expected Map. Arguments dropped.",
            );
        }

        if (dependencies != null) {
            return flowOf(null); // Dependency collection only
        }

        val func: ClientFunction? = context.getFunction(name);
        if (func == null) {
            Logger.wLog("Function not found: $name");
            return flowOf(null);
        }

        // 2. Execute function
        if (!hasStreams) {
            // Synchronous execution (returns Stream, but args are static)
            return flowOf(func.execute(args, context));
        }

        // 3. Handle Stream arguments
        // Create a stream that combines all argument streams, then switches to the
        // result of execution.
        val keys = args.keys.toList();
        val flows: List<Flow<Any?>> = keys.map { key ->
            val arg = args[key];
            if (arg is Flow<*>) {
                return@map arg
            }
            return@map flow { emit(arg) }
        }.toList()
        return combine(flows) { values: Array<Any?> ->
            val combinedArgs = mutableMapOf<String, Any?>()
            for (i in keys.indices) {
                combinedArgs[keys[i]] = values[i]
            }
            combinedArgs
        }.flatMapLatest { combinedArgs ->
            func.execute(combinedArgs, context).map { it }
        }
    }

    fun _parseStringWithInterpolations(input: String, dependencies: MutableSet<DataPath>? = null, depth: Int = 0): Flow<Any?> {
        if (depth > _maxRecursionDepth) {
            throw RecursionExpectedException("Max recursion depth reached in _parseStringWithInterpolations");
        }

        var i = 0;
        val parts = mutableListOf<Any?>()

        while (i < input.length) {
            val startIndex = input.indexOf("\\$\\{", i);
            if (startIndex == -1) {
                parts.add(input.substring(i));
                break;
            }

            if (startIndex > 0 && input[startIndex - 1] == '\\') {
                parts.add(input.substring(i, startIndex - 1));
                parts.add("\\$\\{");
                i = startIndex + 2;
                continue;
            }

            if (startIndex > i) {
                parts.add(input.substring(i, startIndex));
            }
            val (content: String, endIndex: Int) = _extractExpressionContent(input, startIndex + 2);
            if (endIndex == -1) {
                parts.add(input.substring(startIndex));
                break;
            }

            val value = _evaluateExpression(content, depth + 1, dependencies);
            parts.add(value);

            i = endIndex + 1; // Skip closing '}'
        }

        if (parts.isEmpty()) return flowOf("");

        if (parts.size == 1 && parts[0] !is String) {
            val part = parts[0];
            if (part is Flow<*>) {
                return part
            }
            return flowOf(part)
        }

        if (dependencies != null) {
            return flowOf(null)
        }

        // Combine streams for string interpolation
        val flows = parts.map { part ->
            if (part is Flow<*>) return@map part
            return@map flowOf(part);
        }.toList();
        return combine(flows) { values: Array<Any?> ->
            values.joinToString(separator = "") { it?.toString() ?: "" }
        }
    }

    fun _extractExpressionContent(input: String, start: Int): Pair<String, Int> {
        var balance = 1;
        var i = start;
        while (i < input.length) {
            if (input[i] == '{') {
                balance++;
            } else if (input[i] == '}') {
                balance--;
                if (balance == 0) {
                    return input.substring(start, i) to i;
                }
            }
            if (input[i] == '\'' || input[i] == '"') {
                val quote = input[i];
                i++;
                while (i < input.length) {
                    if (input[i] == quote && input[i - 1] != '\\') {
                        break;
                    }
                    i++;
                }
            }
            i++;
        }
        return ("" to -1);
    }

    fun _evaluateExpression(content: String, depth: Int, dependencies: MutableSet<DataPath>? = null): Any? {
        if (depth > _maxRecursionDepth) {
            throw RecursionExpectedException("Max recursion depth reached in expression: $content");
        }

        val trimmed = content.trim();

        val match = Regex("""^([a-zA-Z0-9_]+)\s*\(""").find(trimmed)
        if (match != null && content.endsWith(')')) {
            val funcName = match.groupValues[1]
            val argsStr = content.substring(match.range.last + 1, content.length - 1);
            val args = _parseNamedArgs(argsStr, depth + 1, dependencies);

            if (dependencies != null) {
                return null;
            }

            // Construct a call definition for evaluateFunctionCall to reuse logic
            return evaluateFunctionCall(mutableMapOf("call" to funcName, "args" to args));
        }

        return _resolvePath(content, dependencies);
    }

    fun _parseNamedArgs(argsStr: String, depth: Int, dependencies: MutableSet<DataPath>? = null): Map<String, Any?> {
        val args = mutableMapOf<String, Any?>();
        var i = 0;

        while (i < argsStr.length) {
            while (i < argsStr.length && argsStr[i].isWhitespace()) {
                i++;
            }
            if (i >= argsStr.length) break;

            val keyStart = i;
            while (i < argsStr.length &&
                argsStr[i] != ':' &&
                argsStr[i] != ' ' &&
                argsStr[i] != ','
            ) {
                i++;
            }
            val key = argsStr.substring(keyStart, i).trim();

            while (i < argsStr.length && argsStr[i].isWhitespace()) {
                i++;
            }

            if (i < argsStr.length && argsStr[i] == ':') {
                i++;
            } else {
                Logger.wLog("Invalid named argument format (missing colon) at index $i: $argsStr");
                return args;
            }

            while (i < argsStr.length && argsStr[i].isWhitespace()) {
                i++;
            }

            val (value, nextIndex) = _parseValue(argsStr, i, depth, dependencies);
            args[key] = value;
            i = nextIndex;

            while (i < argsStr.length && argsStr[i].isWhitespace()) {
                i++;
            }

            if (i < argsStr.length && argsStr[i] == ',') i++;
        }
        return args;
    }

    fun _parseValue(input: String, start: Int, depth: Int, dependencies: MutableSet<DataPath>? = null): Pair<Any?, Int> {
        if (start >= input.length) return (null to start);

        val char = input[start];

        if (char == '\'' || char == '"') {
            val quote = char;
            var i = start + 1;
            while (i < input.length) {
                if (input[i] == quote && input[i - 1] != '\\') {
                    break;
                }
                i++;
            }
            if (i < input.length) {
                var str = input.substring(start + 1, i);
                return (_parseStringWithInterpolations(str, dependencies, depth = depth + 1) to i + 1);
            }
            return (input.substring(start) to input.length);
        }

        if (char == '$' && start + 1 < input.length && input[start + 1] == '{') {
            val (content, end) = _extractExpressionContent(input, start + 2);
            if (end != -1) {
                val obj = _evaluateExpression(content, depth, dependencies);
                return (obj to end + 1);
            }
        }

        var i = start;
        while (i < input.length) {
            val c = input[i];
            if (c == ',' ||
                c == ')' ||
                c == '}' ||
                c == ' ' ||
                c == '\t' ||
                c == '\n'
            ) {
                break;
            }
            i++;
        }

        val token = input.substring(start, i)
        if (token == "true") return (true to i)
        if (token == "false") return (false to i)
        if (token == "null") return (null to i)

        val numVal: Number? = token.toDoubleOrNull()
        if (numVal != null) return (numVal to i);

        return (_resolvePath(token, dependencies) to i);
    }

    fun _resolvePath(pathStr: String, dependencies: MutableSet<DataPath>? = null): Flow<Any?> = flow {
        val pathStr = pathStr.trim();
        if (dependencies != null) {
            dependencies.add(context.resolvePath(DataPath(pathStr)));
            emit(null)
            return@flow
        }
        context.subscribeStream<Any?>(DataPath(pathStr)).collect {
            emit(it)
        }
    }
}
