package com.fountofhopedotorg.fohbible.models

sealed class AnimatorDialogType {
    data class Edit(val elementId: String, val initialContent: String) : AnimatorDialogType()
    object AddText : AnimatorDialogType()
    object FetchVerse : AnimatorDialogType()
}