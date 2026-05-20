package com.codegrogu.guava.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.codegrogu.guava.ui.theme.SafetyOrange

@Composable
fun GarageImagePreview(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(4.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .border(1.dp, SafetyOrange.copy(alpha = 0.5f), shape)
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        when {
            imageUrl.startsWith("data:image") -> {
                val imageBitmap = remember(imageUrl) {
                    val encoded = imageUrl.substringAfter("base64,", "")
                    runCatching {
                        val bytes = Base64.decode(encoded, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                    }.getOrNull()
                }

                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = contentDescription,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    ImageFallbackText("IMAGE ERROR")
                }
            }

            imageUrl.startsWith("http://") ||
                imageUrl.startsWith("https://") ||
                imageUrl.startsWith("file://") ||
                imageUrl.startsWith("content://") -> {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            imageUrl.startsWith("gs://") -> {
                ImageFallbackText("PHOTO SAVED")
            }

            else -> {
                ImageFallbackText("NO PHOTO")
            }
        }
    }
}

@Composable
private fun ImageFallbackText(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(8.dp),
        fontFamily = FontFamily.Monospace,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
    )
}
