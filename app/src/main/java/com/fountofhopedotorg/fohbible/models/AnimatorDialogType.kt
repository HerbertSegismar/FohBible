package com.fountofhopedotorg.fohbible.models

sealed class AnimatorDialogType {
    data class Edit(val noteId: String, val initialContent: String) : AnimatorDialogType()
    object AddText : AnimatorDialogType()
    object FetchVerse : AnimatorDialogType()
}