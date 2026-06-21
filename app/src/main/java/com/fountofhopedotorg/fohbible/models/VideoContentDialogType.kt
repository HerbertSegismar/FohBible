package com.fountofhopedotorg.fohbible.models

sealed class VideoContentDialogType {
    data class Edit(val noteId: String, val initialContent: String) : VideoContentDialogType()
    object AddText : VideoContentDialogType()
    object FetchVerse : VideoContentDialogType()
}