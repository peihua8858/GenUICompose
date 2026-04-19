package com.peihua.genui.a2a.core

class PushNotificationConfig(
    /// A unique identifier (e.g. UUID) for the push notification configuration,
    /// set by the client to support multiple notification callbacks.
    val id: String?,

    /// The callback URL where the agent should send push notifications.
    val url: String,

    /// A unique token for this task or session to validate incoming push
    /// notifications.
    val token: String?,

    /// Optional authentication details for the agent to use when calling the
    /// notification URL.
    val authentication: PushNotificationAuthenticationInfo?,
) {

}

class PushNotificationAuthenticationInfo(
    /// A list of supported authentication schemes (e.g., 'Basic', 'Bearer').
    val schemes: List<String>,

    /// Optional credentials required by the push notification endpoint.
    val credentials: String?,
) {

}

class TaskPushNotificationConfig(
    /// The unique identifier (e.g. UUID) of the task.
    val taskId: String,

    /// The push notification configuration for this task.
    val pushNotificationConfig: PushNotificationConfig,
) {
}