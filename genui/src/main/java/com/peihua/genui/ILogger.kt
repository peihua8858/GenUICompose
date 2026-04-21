package com.peihua.genui

import android.content.Context
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import androidx.annotation.WorkerThread
import com.peihua.json.utils.toDouble
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringReader
import java.io.StringWriter
import java.util.Calendar
import javax.xml.transform.OutputKeys
import javax.xml.transform.Source
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

interface ILogger {
    fun dLog(tag: String, message: String)
    fun dLog(message: String)
    fun eLog(tag: String, message: String, e: Throwable? = null)
    fun eLog(message: String, e: Throwable? = null)
    fun wLog(tag: String, message: String)
    fun wLog(message: String)
    fun iLog(tag: String, message: String)
    fun iLog(message: String)
    fun vLog(tag: String, message: String)
    fun vLog(message: String)
}

object Logger : ILogger {
    const val V: Int = 0x1
    const val D: Int = 0x2
    const val I: Int = 0x3
    const val W: Int = 0x4
    const val E: Int = 0x5
    const val A: Int = 0x6
    const val JSON: Int = 0x7
    const val XML: Int = 0x8
    const val JSON_INDENT: Int = 4
    private const val MAX_LENGTH = 2000
    private val LINE_SEPARATOR: String? = File.separator
    private const val NULL_TIPS: String = "Log with null object"
    private const val NULL = "null"
    private const val TAG_DEFAULT = "LogCat"
    private const val SUFFIX = ".java"
    private const val STACK_TRACE_INDEX_6 = 6
    private const val STACK_TRACE_INDEX_4 = 4
    var isShowLog: Boolean = true
    var isWriteLogFile: Boolean = true
    override fun dLog(tag: String, message: String) {
        printLog(STACK_TRACE_INDEX_6, D, tag, message)
    }

    override fun dLog(message: String) {
        printLog(STACK_TRACE_INDEX_6, D, "", message)
    }

    override fun eLog(tag: String, message: String, e: Throwable?) {
        printLog(STACK_TRACE_INDEX_6, E, tag, message)
    }

    override fun eLog(message: String, e: Throwable?) {
        printLog(STACK_TRACE_INDEX_6, E, "", message)
    }

    override fun wLog(tag: String, message: String) {
        printLog(STACK_TRACE_INDEX_6, W, tag, message)
    }

    override fun wLog(message: String) {
        printLog(STACK_TRACE_INDEX_6, W, "", message)
    }

    override fun iLog(tag: String, message: String) {
        printLog(STACK_TRACE_INDEX_6, I, tag, message)
    }

    override fun iLog(message: String) {
        printLog(STACK_TRACE_INDEX_6, I, "", message)
    }

    override fun vLog(tag: String, message: String) {
        printLog(STACK_TRACE_INDEX_6, V, tag, message)
    }

    override fun vLog(message: String) {
        printLog(STACK_TRACE_INDEX_6, V, "", message)
    }

    fun printLog(stackTraceIndex: Int, type: Int, tagStr: String?, vararg objects: Any?) {
        if (!isShowLog) {
            return
        }

        val contents = wrapperContent(stackTraceIndex, tagStr, *objects)
        val tag: String = contents[0]
        val msg = contents[1]
        val headString: String = contents[2]

        when (type) {
            V, D, I, W, E, A -> printDefault(
                type,
                tag,
                headString + msg
            )

            JSON -> printJson(tag, msg, headString)
            XML -> printXml(tag, msg, headString)
            else -> {}
        }
    }

    fun printLog(type: Int, tagStr: String?, vararg objects: Any?) {
        printLog(STACK_TRACE_INDEX_6, type, tagStr, *objects)
    }


    private fun printDebug(tagStr: String?, vararg objects: Any?) {
        printDebug(STACK_TRACE_INDEX_6, tagStr, *objects)
    }

    fun printDebug(stackTraceIndex: Int, tagStr: String?, vararg objects: Any?) {
        val contents = wrapperContent(stackTraceIndex, tagStr, *objects)
        val tag: String = contents[0]
        val msg: String = contents[1]
        val headString: String = contents[2]
        printDefault(D, tag, headString + msg)
    }

    fun wrapperContent(stackTraceIndex: Int, tagStr: String?, vararg objects: Any?): Array<String> {
        val stackTrace = Thread.currentThread().getStackTrace()
        val targetElement = stackTrace[stackTraceIndex]
        val fileName = targetElement.fileName
        var className = fileName
        if (TextUtils.isEmpty(fileName)) {
            className = targetElement.className
            val classNameInfo: Array<String?> =
                className.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (classNameInfo.isNotEmpty()) {
                className = classNameInfo[classNameInfo.size - 1] + SUFFIX
            }
            if (className.contains("$")) {
                className = className.split("\\$".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()[0] + SUFFIX
            }
        }

        val methodName = targetElement.methodName
        var lineNumber = targetElement.lineNumber

        if (lineNumber < 0) {
            lineNumber = 0
        }

        var tag: String? = (tagStr ?: className)

        if (TextUtils.isEmpty(tag)) {
            tag = TAG_DEFAULT
        }

        val msg =
            if (objects.isEmpty()) NULL_TIPS else getObjectsString(*objects)
        val headString = "[ ($className:$lineNumber)#$methodName ] "

        return arrayOf<String>(tag!!, msg, headString)
    }

    private fun getObjectsString(vararg objects: Any?): String {
        if (objects.size > 1) {
            val stringBuilder = StringBuilder()
            stringBuilder.append("\n")
            for (i in objects.indices) {
                val `object`: Any? = objects[i]
                if (`object` == null) {
                    stringBuilder.append("[").append(i).append("]").append(" = ")
                        .append(
                            NULL
                        ).append("\n")
                } else {
                    stringBuilder.append("[").append(i).append("]").append(" = ")
                        .append(`object`.toString()).append("\n")
                }
            }
            return stringBuilder.toString()
        } else {
            val `object`: Any? = objects[0]
            return if (`object` == null) NULL else `object`.toString()
        }
    }

    fun printDefault(type: Int, tag: String?, msg: String) {
        var index = 0
        val length = msg.length
        val countOfSub = length / MAX_LENGTH

        if (countOfSub > 0) {
            for (i in 0..<countOfSub) {
                val sub = msg.substring(index, index + MAX_LENGTH)
                printSub(type, tag, sub)
                index += MAX_LENGTH
            }
            printSub(type, tag, msg.substring(index, length))
        } else {
            printSub(type, tag, msg)
        }
    }

    private fun printSub(type: Int, tag: String?, sub: String) {
        when (type) {
            V -> Log.v(tag, sub)
            D -> Log.d(tag, sub)
            I -> Log.i(tag, sub)
            W -> Log.w(tag, sub)
            E -> Log.e(tag, sub)
            A -> Log.wtf(tag, sub)
            else -> {}
        }
    }

    fun printXml(tag: String?, xml: String?, headString: String?) {
        var xml = xml
        if (xml != null) {
            xml = formatXML(xml)
            xml = headString + "\n" + xml
        } else {
            xml = headString + NULL_TIPS
        }

        printLine(tag, true)
        val lines: Array<String?> =
            xml.split(LINE_SEPARATOR!!.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (line in lines) {
            if (!isEmpty(line)) {
                Log.d(tag, "║ $line")
            }
        }
        printLine(tag, false)
    }

    private fun formatXML(inputXML: String?): String? {
        try {
            val xmlInput: Source = StreamSource(StringReader(inputXML))
            val xmlOutput = StreamResult(StringWriter())
            val transformer = TransformerFactory.newInstance().newTransformer()
            transformer.setOutputProperty(OutputKeys.INDENT, "yes")
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
            transformer.transform(xmlInput, xmlOutput)
            return xmlOutput.writer.toString().replaceFirst(">".toRegex(), ">\n")
        } catch (e: Exception) {
            e.printStackTrace()
            return inputXML
        }
    }

    fun printJson(tag: String?, msg: String, headString: String?) {
        var message: String?

        try {
            if (msg.startsWith("{")) {
                val jsonObject = JSONObject(msg)
                message = jsonObject.toString(JSON_INDENT)
            } else if (msg.startsWith("[")) {
                val jsonArray = JSONArray(msg)
                message = jsonArray.toString(JSON_INDENT)
            } else {
                message = msg
            }
        } catch (e: JSONException) {
            message = msg
        }

        printLine(tag, true)
        message = headString + LINE_SEPARATOR + message
        val lines: Array<String?> =
            message.split(LINE_SEPARATOR!!.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (line in lines) {
            Log.d(tag, "║$line")
        }
        printLine(tag, false)
    }

    fun isEmpty(line: String?): Boolean {
        return TextUtils.isEmpty(line) || line == "\n" || line == "\t" || TextUtils.isEmpty(line!!.trim { it <= ' ' })
    }

    fun printLine(tag: String?, isTop: Boolean) {
        if (isTop) {
            Log.d(
                tag,
                "╔═══════════════════════════════════════════════════════════════════════════════════════"
            )
        } else {
            Log.d(
                tag,
                "╚═══════════════════════════════════════════════════════════════════════════════════════"
            )
        }
    }

    /**
     * log 写入文件
     *
     * @param tagStr
     * @param log
     */
    fun writeLog(context: Context, tagStr: String?, log: String?) {
        writeLog(context, STACK_TRACE_INDEX_4, tagStr, log)
    }

    /**
     * log 写入文件
     *
     * @param tagStr
     * @param log
     */
    @WorkerThread
    fun writeLog(context: Context, stackTraceIndex: Int, tagStr: String?, log: String?) {
        val contents = wrapperContent(stackTraceIndex, tagStr, log)
        val tag: String = contents[0]
        val msg = contents[1]
        val headString: String = contents[2]
        val logMsg = headString + msg
        printLog(D, tag, logMsg)
        if (!isWriteLogFile) return
        FileLog.writeLog(context, "$tag $logMsg")
    }

    /**
     * 日志写入文件
     */
    internal object FileLog {
        private const val DIR = "Logcat"

        /**
         * 一天
         */
        val ONE_DAY: Int = 60 * 1000 * 60 * 24

        /**
         * 5天
         */
        val FRI_DAY: Int = ONE_DAY * 5

        fun writeLog(context: Context, logString: String?) {
            try {
                val rootFile = Environment.getExternalStorageDirectory()
                val parentFile = context.externalCacheDir
                //                File parentFile = new File(rootFile, "/Android/data/" + BuildConfig.APPLICATION_ID + "/cache");
                val fileParentDir = File(parentFile, DIR) //判断log目录是否存在
                if (!fileParentDir.exists()) {
                    fileParentDir.mkdirs()
                }

                try {
                    val files = fileParentDir.listFiles()
                    if (files != null && files.size >= 5) {
                        for (file in files) {
                            if (isRemoveFile(file)) {
                                file.delete()
                                Log.d("FileLog", "delete file =" + file.getName())
                            }
                        }
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                }

                val logFile = File(fileParentDir, "Logcat-$curData.log") //日志文件名
                val printWriter = PrintWriter(FileOutputStream(logFile, true)) //紧接文件尾写入日志字符串
                val time = "[$curTime] "
                printWriter.println(time + logString)
                printWriter.flush()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }

        val curData: String
            get() {
                val cd = Calendar.getInstance() //日志文件时间
                val year = cd.get(Calendar.YEAR)
                val month =
                    addZero(cd.get(Calendar.MONTH) + 1)
                val day =
                    addZero(cd.get(Calendar.DAY_OF_MONTH))
                return "$year-$month-$day"
            }

        val curTime: String
            get() {
                val cd = Calendar.getInstance() //日志文件时间
                val year = cd.get(Calendar.YEAR)
                val month =
                    addZero(cd.get(Calendar.MONTH) + 1)
                val day =
                    addZero(cd.get(Calendar.DAY_OF_MONTH))
                val hour =
                    addZero(cd.get(Calendar.HOUR_OF_DAY))
                val min =
                    addZero(cd.get(Calendar.MINUTE))
                val sec =
                    addZero(cd.get(Calendar.SECOND))
                return "$year-$month-$day $hour:$min:$sec"
            }

        private fun addZero(i: Int): String {
            if (i < 10) {
                val tmpString = "0" + i
                return tmpString
            } else {
                return i.toString()
            }
        }

        private fun isRemoveFile(file: File): Boolean {
            //log-2024-04-29.log
            val fileName = file.getName()
            val dateStr = fileName.substring(4, fileName.indexOf("."))
            val dates: Array<String?> =
                dateStr.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val cd = Calendar.getInstance() //日志文件时间
            val curTime = cd.getTimeInMillis()
            cd.add(Calendar.YEAR, toInt(dates[0]))
            cd.add(Calendar.MONTH, toInt(dates[1]) - 1)
            cd.add(Calendar.DAY_OF_MONTH, toInt(dates[2]))
            val fileTime = cd.getTimeInMillis()
            Log.d(
                "FileLog",
                "delete file =" + file.getName() + ",dateStr:" + dateStr + ",dates:" + dates.contentToString()
            )
            return (curTime - FRI_DAY) > fileTime
        }
    }

    /**
     * 将Object对象转成Integer类型
     *
     * @param value
     * @return 如果value不能转成Integer，则默认0
     */
    @JvmOverloads
    internal fun toInt(value: Any?, defaultValue: Int = 0): Int {
        if (value == null) {
            return defaultValue
        }
        if (value is Int) {
            return value
        }
        if (value is Number) {
            return value.toInt()
        }
        if (value is String) {
            return try {
                value.toDouble().toInt()
            } catch (e: Exception) {
                defaultValue
            }
        }
        return defaultValue
    }

}