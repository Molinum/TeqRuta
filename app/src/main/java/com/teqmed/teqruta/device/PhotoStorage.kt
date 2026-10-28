package com.teqmed.teqruta.device

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/** Archivos de fotos del odómetro en el almacenamiento interno de la app. */
object PhotoStorage {

    fun crearArchivo(context: Context): File {
        val carpeta = File(context.filesDir, "fotos").apply { mkdirs() }
        return File(carpeta, "odometro_${System.currentTimeMillis()}.jpg")
    }

    /** URI que la app de cámara puede escribir, a través del FileProvider. */
    fun uriPara(context: Context, archivo: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
}
