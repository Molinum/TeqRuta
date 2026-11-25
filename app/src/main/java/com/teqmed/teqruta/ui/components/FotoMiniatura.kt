package com.teqmed.teqruta.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Foto del odómetro con su etiqueta; se decodifica fuera del hilo principal. */
@Composable
fun FotoMiniatura(etiqueta: String, ruta: String?, modifier: Modifier = Modifier) {
    val imagen by produceState<ImageBitmap?>(initialValue = null, ruta) {
        value = ruta?.let {
            withContext(Dispatchers.IO) {
                BitmapFactory.decodeFile(it, BitmapFactory.Options().apply { inSampleSize = 4 })?.asImageBitmap()
            }
        }
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.labelMedium)
        val foto = imagen
        if (foto != null) {
            Image(
                bitmap = foto,
                contentDescription = etiqueta,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(8.dp))
            )
        } else {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(110.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        if (ruta == null) "Sin foto" else "Foto no disponible",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
