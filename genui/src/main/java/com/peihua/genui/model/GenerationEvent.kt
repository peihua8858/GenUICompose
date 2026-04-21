package com.peihua.genui.model

sealed class GenerationEvent {
}

/// An event containing a text chunk from the LLM.
class TextEvent(val text: String) : GenerationEvent() {
}

/// An event containing a parsed [A2uiMessage].
class A2uiMessageEvent(val message: A2uiMessage) : GenerationEvent() {
}
