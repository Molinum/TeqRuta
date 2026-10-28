package com.teqmed.teqruta.ui.components

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.teqmed.teqruta.device.PhotoStorage

/**
 * Toma la foto del odómetro con la app de cámara del dispositivo y muestra una vista previa.
 * [fotoPath] es la ruta del archivo ya tomado, o null.
 */
@Composable
fun FotoOdometroField(
    fotoPath: String?,
    onFotoTomada: (String) -> Unit,
    obligatoria: Boolean,
    modifier: Modifier = Modifier,
    error: String? = null
) {
    val context = LocalContext.current
    // Ruta del archivo que se está capturando; sobrevive si la actividad se recrea
    var pendiente by rememberSaveable { mutableStateOf<String?>(null) }

    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { exito ->
        if (exito) pendiente?.let(onFotoTomada)
    }

    val vistaPrevia = remember(fotoPath) {
        fotoPath?.let {
            BitmapFactory.decodeFile(it, BitmapFactory.Options().apply { inSampleSize = 4 })?.asImageBitmap()
        }
    }

    Column(modifier) {
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (vistaPrevia != null) {
                    Image(
                        bitmap = vistaPrevia,
                        contentDescription = "Foto del odómetro",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Icon(
                        Icons.Outlined.PhotoCamera,
                        contentDescription = null,
                        tint = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        if (obligatoria) "Foto del odómetro (obligatoria)" else "Foto del odómetro (opcional)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                OutlinedButton(onClick = {
                    val archivo = PhotoStorage.crearArchivo(context)
                    pendiente = archivo.absolutePath
                    camara.launch(PhotoStorage.uriPara(context, archivo))
                }) {
                    Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                    Text(
                        if (fotoPath == null) "Tomar foto" else "Repetir foto",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
        MensajeError(error)
    }
}
