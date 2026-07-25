package com.fountofhopedotorg.fohbible.gfx_animator

import android.content.Context
import java.io.File
import android.content.Intent
import androidx.core.content.FileProvider

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

fun shareTemplateFile(context: Context, file: File) {
    val authority = "${context.packageName}.fileprovider"
    val uri = FileProvider.getUriForFile(context, authority, file)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/x-foh"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share Template"))
}