package com.peihua.genui.a2a.client

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.readRawBytes
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

val HttpResponse.statusCode: Int
    get() = this.status.value
val HttpResponse.isSuccess: Boolean
    get() = this.status.value >= 200 && this.status.value < 300
val HttpResponse.message: String
    get() = this.status.description

suspend fun HttpResponse.readText(): String {
    return this.readRawBytes().toString(Charsets.UTF_8)
}

fun HttpResponse.readLine(): Flow<String> = flow {
    val channel = bodyAsChannel()
    // 循环读取数据流
    while (!channel.isClosedForRead) {
        val line = channel.readUTF8Line()
        if (!line.isNullOrEmpty()) {
            emit(line)
            delay(200)
        }
        println(line)
    }
}
