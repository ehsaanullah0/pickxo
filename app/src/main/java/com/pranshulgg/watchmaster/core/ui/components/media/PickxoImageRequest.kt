package com.pranshulgg.watchmaster.core.ui.components.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.pranshulgg.watchmaster.core.prefs.LocalAppPrefs

/**
 * Builds artwork requests according to Pickxo's Image Storage setting.
 * Online mode keeps artwork out of Coil's disk cache.
 * Offline mode uses Coil's normal disk cache so artwork can be reused locally.
 */
@Composable
fun pickxoImageRequest(url: String?): ImageRequest {
    val context = LocalContext.current
    val prefs = LocalAppPrefs.current

    return ImageRequest.Builder(context)
        .data(url)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(
            if (prefs.onlineImageMode) CachePolicy.DISABLED else CachePolicy.ENABLED
        )
        .networkCachePolicy(CachePolicy.ENABLED)
        .build()
}
