package com.deliveryplatform.cliente.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deliveryplatform.cliente.data.api.OrderDto
import com.deliveryplatform.cliente.ui.components.EmptyState
import com.deliveryplatform.cliente.ui.components.LoadingBox
import com.deliveryplatform.cliente.ui.components.StatusPill
import com.deliveryplatform.cliente.ui.components.brl
import com.deliveryplatform.cliente.ui.theme.Border
import com.deliveryplatform.cliente.ui.theme.Muted
import com.deliveryplatform.cliente.ui.theme.Soft
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val orderDateFormat = DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm", Locale("pt", "BR"))

@Composable
fun OrdersScreen(state: ClienteUiState, onRefresh: () -> Unit, modifier: Modifier = Modifier) {
    LaunchedEffect(state.profile?.id) {
        while (true) {
            onRefresh()
            delay(30_000)
        }
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
                        "Atualizamos automaticamente a cada 30 segundos.",
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
                    description = "Quando você fizer um pedido, ele aparece aqui com o status da cozinha e da entrega.",
                )
            }
        } else {
            items(state.orders, key = { it.id }) { order -> OrderCard(order) }
        }
    }
}

@Composable
private fun OrderCard(order: OrderDto) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Pedido ${order.orderNumber ?: order.id.take(8)}",
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                StatusPill(order.status)
            }
            order.createdAt?.let {
                Text(formatDate(it), style = MaterialTheme.typography.bodySmall, color = Muted)
            }
            order.scheduledWindow?.let {
                Surface(color = Soft, shape = RoundedCornerShape(50)) {
                    Text(
                        "Agendado para $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
            HorizontalDivider(color = Border)
            order.items.forEach { item ->
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        "${item.quantity}× ${item.dishName}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    Text(brl(item.totalPrice ?: item.unitPrice * item.quantity), style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Entrega em ${order.deliveryAddress}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                    modifier = Modifier.weight(1f),
                )
                Text(brl(order.totalAmount), fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun formatDate(value: String): String =
    runCatching { LocalDateTime.parse(value).format(orderDateFormat) }.getOrDefault(value)
