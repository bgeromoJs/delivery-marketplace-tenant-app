package com.deliveryplatform.cliente.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.deliveryplatform.cliente.data.api.OrderDto
import com.deliveryplatform.cliente.ui.components.EmptyState
import com.deliveryplatform.cliente.ui.components.LoadingBox
import com.deliveryplatform.cliente.ui.components.StatusPill
import com.deliveryplatform.cliente.ui.components.brl
import com.deliveryplatform.cliente.ui.theme.Border
import com.deliveryplatform.cliente.ui.theme.Muted
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val orderDateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR"))

/**
 * Aba de pedidos do cliente. Mostra o historico do mais recente para o mais antigo
 * e abre a comanda do pedido escolhido, mantendo a atualizacao automatica a cada 30 s.
 */
@Composable
fun OrdersScreen(
    state: ClienteUiState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = selectedId?.let { id -> state.orders.firstOrNull { it.id == id } }

    LaunchedEffect(state.profile?.id) {
        while (true) {
            onRefresh()
            delay(30_000)
        }
    }

    if (selected != null) {
        OrderDetailScreen(
            order = selected,
            state = state,
            storeName = state.storeName(selected.tenantId),
            onBack = { selectedId = null },
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Seus pedidos", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Do mais recente para o mais antigo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                    )
                }
                TextButton(onClick = onRefresh) { Text("Atualizar") }
            }
        }

        if (state.loadingOrders && state.orders.isEmpty()) {
            item { LoadingBox() }
        } else if (state.orders.isEmpty()) {
            item {
                EmptyState(
                    title = "Nenhum pedido ainda",
                    description = "Escolha os pratos no cardápio e finalize no carrinho. O pedido aparece aqui com o status da cozinha e da entrega.",
                )
            }
        } else {
            items(state.orders, key = { it.id }) { order ->
                OrderRow(
                    order = order,
                    storeName = state.storeName(order.tenantId),
                    onClick = { selectedId = order.id },
                )
            }
        }
    }
}

@Composable
private fun OrderRow(order: OrderDto, storeName: String?, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val inProgress = order.status.uppercase() !in setOf("DELIVERED", "CANCELLED")
    val itemCount = order.items.sumOf { it.quantity }

    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (inProgress) primary.copy(alpha = 0.35f) else Border),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    storeName?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = primary, fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        "Pedido ${order.orderNumber ?: order.id.take(8)}",
                        fontWeight = FontWeight.SemiBold,
                    )
                    order.createdAt?.let {
                        Text(formatOrderDate(it), style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                }
                StatusPill(order.status)
            }

            Text(
                if (itemCount == 1) "1 item" else "$itemCount itens",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Total ${brl(order.totalAmount)}",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "Ver comanda →",
                    style = MaterialTheme.typography.bodySmall,
                    color = primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

internal fun formatOrderDate(value: String): String =
    runCatching { LocalDateTime.parse(value).format(orderDateFormat) }.getOrDefault(value)

internal fun paymentLabel(method: String): String = when (method.uppercase()) {
    "PIX" -> "Pix"
    "CREDIT_CARD" -> "Cartão de crédito"
    "CASH" -> "Dinheiro"
    else -> method
}

/** Remove o marcador interno de pagamento das observacoes do pedido. */
internal fun customerNotes(notes: String?): List<String> =
    notes.orEmpty()
        .split("|")
        .map { it.trim() }
        .filter { it.isNotBlank() && !it.startsWith("Pagamento:", ignoreCase = true) }

@Composable
internal fun DetailHeader(title: String, onBack: () -> Unit, status: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onBack) { Text("← Voltar", color = Muted) }
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        StatusPill(status)
    }
}
