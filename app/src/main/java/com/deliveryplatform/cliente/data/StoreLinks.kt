package com.deliveryplatform.cliente.data

import android.net.Uri

/**
 * Links de estabelecimento aceitos pelo app:
 *
 * - `deliveryhub://loja/vera-marmitex`
 * - `https://deliveryhub.com.br/loja/vera-marmitex`
 * - o slug puro `vera-marmitex`, colado manualmente
 *
 * O slug identifica a loja no tenant-service (`GET /api/tenants/{slug}/theme`).
 */
private val linkPattern = Regex("""/loja/([A-Za-z0-9][A-Za-z0-9_-]*)""")
private val slugPattern = Regex("""[a-z0-9][a-z0-9_-]{0,59}""")

/**
 * Link recebido pela Activity. O `nonce` distingue duas aberturas do mesmo link, para
 * que uma rotação de tela não reabra a loja e um novo toque no link sempre funcione.
 */
data class StoreLinkEvent(val slug: String, val nonce: Long = System.nanoTime())

fun storeSlugFromLink(raw: String?): String? {
    val text = raw?.trim().orEmpty()
    if (text.isEmpty()) return null
    val candidate = linkPattern.find(text)?.groupValues?.get(1)
        ?: text.substringAfterLast('/').substringBefore('?').substringBefore('#').trim()
    return candidate.lowercase().takeIf { slugPattern.matches(it) }
}

fun storeSlugFromUri(uri: Uri?): String? {
    if (uri == null) return null
    val segments = uri.pathSegments
    val candidate = when {
        uri.scheme == "deliveryhub" && uri.host == "loja" -> segments.firstOrNull()
        segments.size >= 2 && segments.first() == "loja" -> segments[1]
        else -> null
    }
    return candidate?.lowercase()?.takeIf { slugPattern.matches(it) }
}
