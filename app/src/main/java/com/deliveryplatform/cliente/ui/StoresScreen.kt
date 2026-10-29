package com.deliveryplatform.cliente.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.deliveryplatform.cliente.BuildConfig
import com.deliveryplatform.cliente.data.api.CustomerStoreDto
import com.deliveryplatform.cliente.data.api.TenantTheme
import com.deliveryplatform.cliente.data.storeSlugFromLink
import com.deliveryplatform.cliente.ui.components.AppTextField
import com.deliveryplatform.cliente.ui.components.EmptyState
import com.deliveryplatform.cliente.ui.components.LoadingBox
import com.deliveryplatform.cliente.ui.components.PrimaryButton
import com.deliveryplatform.cliente.ui.components.SecondaryButton
import com.deliveryplatform.cliente.ui.components.brl
import com.deliveryplatform.cliente.ui.theme.Border
import com.deliveryplatform.cliente.ui.theme.Danger
import com.deliveryplatform.cliente.ui.theme.Ink
import com.deliveryplatform.cliente.ui.theme.Muted
import com.deliveryplatform.cliente.ui.theme.Soft
import com.deliveryplatform.cliente.ui.theme.brandColor

/**
 * Tela inicial depois do login: os estabelecimentos que o cliente abriu pelo link.
 *
 * Com uma única loja o app pula esta tela e abre o cardápio direto; com duas ou mais,
 * esta é a lista de escolha. O vínculo é criado pelo link do estabelecimento, não pelo
 * APK, então sempre existe a opção de colar o link aqui.
 */
@Composable
fun StoresScreen(
    state: ClienteUiState,
    onOpenStore: (String) -> Unit,
    onLinkStore: (String) -> Unit,
    onDismissLinkError: () -> Unit,
    onLoadAvailable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var linkDialogOpen by rememberSaveable { mutableStateOf(false) }
    // Slug enviado pelo diálogo: quando ele aparece na lista, o vínculo terminou.
    var submittedSlug by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(state.stores, state.linking) {
        val pending = submittedSlug ?: return@LaunchedEffect
        if (!state.linking && state.stores.any { it.slug == pending }) {
            submittedSlug = null
            linkDialogOpen = false
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Seus estabelecimentos", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "As lojas que você abriu pelo link ficam guardadas aqui, na sua conta.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
            }
        }

        if (state.loadingStores && state.stores.isEmpty()) {
            item { LoadingBox() }
        } else if (state.stores.isEmpty()) {
            item {
                EmptyState(
                    title = "Nenhum estabelecimento ainda",
                    description = "Abra o app pelo link que a loja enviou (deliveryhub://loja/nome-da-loja) " +
                        "ou cole o link abaixo. Com uma única loja, o cardápio abre direto.",
                )
            }
        } else {
            items(state.stores, key = { it.id }) { store ->
                StoreRow(
                    store = store,
                    theme = state.storeThemes[store.slug],
                    onClick = { onOpenStore(store.slug) },
                )
            }
        }

        item {
            SecondaryButton(
                text = "Adicionar loja pelo link",
                onClick = { linkDialogOpen = true },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (BuildConfig.DEBUG) {
            item {
                LaunchedEffect(Unit) { onLoadAvailable() }
                DebugStoreShortcuts(
                    state = state,
                    onOpenStore = onOpenStore,
                )
            }
        }
    }

    if (linkDialogOpen) {
        LinkStoreDialog(
            linking = state.linking,
            error = state.linkError,
            onDismiss = {
                linkDialogOpen = false
                submittedSlug = null
                onDismissLinkError()
            },
            onConfirm = { link ->
                // O diálogo continua aberto: mostra o progresso e o erro do vínculo.
                submittedSlug = storeSlugFromLink(link)
                onLinkStore(link)
            },
        )
    }
}

@Composable
private fun StoreRow(store: CustomerStoreDto, theme: TenantTheme?, onClick: () -> Unit) {
    val name = theme?.name ?: store.name ?: store.slug
    val primary = brandColor(theme?.primaryColor)
    val logo = theme?.logoUrl ?: store.logoUrl

    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Border),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                if (!logo.isNullOrBlank()) {
                    AsyncImage(
                        model = logo,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(
                        name.take(1).uppercase(),
                        color = primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                    )
                }
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    name,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                theme?.description?.takeIf { it.isNotBlank() }?.let { description ->
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color(0xFFE8A317),
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        theme?.rating?.let { String.format("%.1f", it) } ?: "Novo",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                    )
                    Text(
                        deliveryLabel(theme?.deliveryFee),
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                    )
                    theme?.deliveryTimeMin?.let { minutes ->
                        Text("$minutes min", style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                }
            }

            Text(
                "Abrir →",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun deliveryLabel(fee: Double?): String = when {
    fee == null -> "Entrega a combinar"
    fee == 0.0 -> "Entrega grátis"
    else -> brl(fee)
}

@Composable
private fun LinkStoreDialog(
    linking: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var link by remember { mutableStateOf("") }
    Dialog(onDismissRequest = { if (!linking) onDismiss() }) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Abrir loja pelo link", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Cole o link que o estabelecimento enviou. Ex.: deliveryhub://loja/vera-marmitex",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
                AppTextField(
                    value = link,
                    onValueChange = { link = it },
                    label = "Link da loja",
                )
                error?.let { message ->
                    Text(
                        message,
                        style = MaterialTheme.typography.bodySmall,
                        color = Danger,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !linking,
                    ) {
                        Text("Cancelar", color = Muted)
                    }
                    PrimaryButton(
                        text = "Abrir",
                        onClick = { onConfirm(link) },
                        modifier = Modifier.weight(1f),
                        loading = linking,
                        enabled = link.isNotBlank() && !linking,
                    )
                }
            }
        }
    }
}

/**
 * Atalho de desenvolvimento: sem um link real em mãos, lista as lojas do ambiente para
 * entrar nelas. Só existe no build debug.
 */
@Composable
private fun DebugStoreShortcuts(state: ClienteUiState, onOpenStore: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "Lojas neste ambiente (debug)",
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
        )
        if (state.loadingAvailable) {
            Text("Procurando lojas...", style = MaterialTheme.typography.bodySmall, color = Muted)
        } else if (state.availableStores.isEmpty()) {
            Text(
                "Nenhuma loja em GET /api/tenants. Confira se o backend está no ar.",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
        } else {
            state.availableStores.forEach { store ->
                Surface(
                    color = Soft,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().clickable { onOpenStore(store.slug) },
                ) {
                    Text(
                        "${store.name} · ${store.slug}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Ink,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}
