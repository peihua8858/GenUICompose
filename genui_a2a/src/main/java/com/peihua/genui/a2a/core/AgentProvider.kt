package com.peihua.genui.a2a.core

import kotlinx.serialization.Serializable

@Serializable
data class AgentProvider(private val organization: String, private val url: String)
