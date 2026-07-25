package com.fountofhopedotorg.fohbible.gfx_animator

import android.content.Context
import java.io.File

fun getTemplatesFolder(context: Context): File {
    val dir = File(context.getExternalFilesDir(null), "templates")
    if (!dir.exists()) dir.mkdirs()
    return dir
}

fun getTemplateFiles(context: Context): List<File> {
    val dir = getTemplatesFolder(context)
    return dir.listFiles()?.filter { it.extension.equals("foh", true) }
        ?.sortedByDescending { it.lastModified() } ?: emptyList()
}