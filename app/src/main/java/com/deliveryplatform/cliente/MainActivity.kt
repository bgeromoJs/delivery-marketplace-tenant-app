package com.deliveryplatform.cliente

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.deliveryplatform.cliente.ui.ClienteApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val slug = storeSlugFrom(intent?.data) ?: BuildConfig.STORE_SLUG
        setContent { ClienteApp(slug) }
    }

    /**
     * Cada loja publica seu proprio APK, mas o mesmo app aceita o convite por link:
     * deliveryhub://loja/{slug} ou https://deliveryhub.com.br/loja/{slug}.
     */
    private fun storeSlugFrom(uri: Uri?): String? {
        if (uri == null) return null
        val segments = uri.pathSegments
        return when {
            uri.scheme == "deliveryhub" && uri.host == "loja" -> segments.firstOrNull()
            segments.size >= 2 && segments.first() == "loja" -> segments[1]
            else -> null
        }?.takeIf { it.isNotBlank() }
    }
}
