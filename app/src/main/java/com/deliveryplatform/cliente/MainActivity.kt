package com.deliveryplatform.cliente

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.deliveryplatform.cliente.data.StoreLinkEvent
import com.deliveryplatform.cliente.data.storeSlugFromUri
import com.deliveryplatform.cliente.ui.ClienteApp
import kotlinx.coroutines.flow.MutableSharedFlow

class MainActivity : ComponentActivity() {

    /**
     * A loja nao vem mais do APK: ela chega pelo link do estabelecimento.
     * O fluxo tem replay para o link que abriu o app nao se perder antes da coleta.
     */
    private val storeLinks = MutableSharedFlow<StoreLinkEvent>(
        replay = 1,
        extraBufferCapacity = 1,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Em rotação a Activity é recriada com o mesmo Intent; reemitir o link
        // reabriria a loja sem o cliente ter tocado em nada.
        if (savedInstanceState == null) {
            handleIntent(intent)
        }
        setContent { ClienteApp(storeLinks) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val slug = storeSlugFromUri(intent?.data) ?: return
        storeLinks.tryEmit(StoreLinkEvent(slug))
    }
}
