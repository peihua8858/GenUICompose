package com.peihua.genui.interfaces

import com.peihua.genai.primitives.ChatMessage
import com.peihua.genui.model.A2uiMessage
import kotlinx.coroutines.flow.Flow

/// An interface for transporting messages between GenUI and an AI service.
///
/// This unifies the concept of incoming streams (text chunks and A2UI messages)
/// and outgoing requests.
interface Transport {
    /// A stream of raw text chunks received from the AI service.
    ///
    /// This is typically used for "streaming" responses where the text is built
    /// up over time.
    val incomingText: Flow<String>;

    /// A stream of parsed [A2uiMessage]s received from the AI service.
    val incomingMessages: Flow<A2uiMessage>;

    /// Sends a request to the AI service.
   suspend fun sendRequest(message: ChatMessage)

    /// Disposes of any resources used by this transport.
    fun dispose();
    fun addChunk(text: String);
    fun addMessage(message: A2uiMessage);
}
