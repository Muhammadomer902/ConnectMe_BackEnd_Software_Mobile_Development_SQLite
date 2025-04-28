package com.muhammadomer.i220921

data class Message(
    val messageId: String = "",
    val chatId: String = "",
    val text: String = "",
    val imageUrl: String? = null,
    val senderId: String = "",
    val timestamp: Long = 0L,
    val isSeen: Boolean = false,
    val vanish: Boolean = false
)