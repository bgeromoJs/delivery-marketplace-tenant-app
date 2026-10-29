package com.deliveryplatform.cliente.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deliveryplatform.cliente.data.api.OrderDto
import com.deliveryplatform.cliente.data.api.OrderItemDto
import com.deliveryplatform.cliente.ui.components.SecondaryButton
import com.deliveryplatform.cliente.ui.components.brl
import com.deliveryplatform.cliente.ui.theme.Border
import com.deliveryplatform.cliente.ui.theme.Danger
import com.deliveryplatform.cliente.ui.theme.Ink
import com.deliveryplatform.cliente.ui.theme.Muted
import com.deliveryplatform.cliente.ui.theme.Soft
import com.deliveryplatform.cliente.ui.theme.Success
import com.deliveryplatform.cliente.ui.theme.Warning

/**
 * Comanda do pedido: status atual, linha do tempo da cozinha e entrega,
 * itens, valores e endereco. A tela e atualizada pelo polling da aba de pedidos.
 */
@Composable
fun OrderDetailScreen(
    order: OrderDto,
    state: ClienteUiState,
    storeName: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val storeTheme = state.storeTheme(order.tenantId)
    val title = storeName ?: storeTheme?.name ?: "Sua loja"
    val storePhone = storeTheme?.phone?.takeIf { it.isNotBlank() }
    val context = LocalContext.current

    Column(modifier.fillMaxSize()) {
        DetailHeader(
            title = "Pedido ${order.orderNumber ?: order.id.take(8)}",
            onBack = onBack,
            status = order.status,
        )
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { TrackingCard(order = order) }
            item { TicketCard(order = order, storeName = title) }
            if (storePhone != null) {
                item {
                    SecondaryButton(
                        text = "Falar com a loja",
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$storePhone"))
                            runCatching { context.startActivity(intent) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackingCard(order: OrderDto) {
    val status = order.status.uppercase()
    val cancelled = status == "CANCELLED"
    val delivered = status == "DELIVERED"
    val accent = when (status) {
        "DELIVERED" -> Success
        "CANCELLED" -> Danger
        "READY" -> Warning
        "PICKED_UP" -> Color(0xFF0B5CAD)
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        color = Color.White,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(statusTitle(status), style = MaterialTheme.typography.titleLarge, color = accent)
            Text(statusDescription(status), style = MaterialTheme.typography.bodySmall, color = Muted)

            if (!cancelled) {
                Timeline(current = statusStep(status))
            }

            if (!cancelled && !delivered) {
                Surface(color = Soft, shape = RoundedCornerShape(10.dp)) {
                    Text(
                        "Atualizamos o status automaticamente a cada 30 segundos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

private data class TimelineStep(val label: String, val hint: String)

private val timelineSteps = listOf(
    TimelineStep("Pedido recebido", "A loja registrou seu pedido."),
    TimelineStep("Em preparo", "A cozinha está preparando."),
    TimelineStep("Pronto", "Aguardando um entregador retirar."),
    TimelineStep("Saiu para entrega", "O entregador está a caminho."),
    TimelineStep("Entregue", "Pedido entregue. Bom apetite!"),
)

@Composable
private fun Timeline(current: Int) {
    val primary = MaterialTheme.colorScheme.primary
    Column {
        timelineSteps.forEachIndexed { index, step ->
            val reached = index <= current
            val active = index == current
            Row {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(if (reached) primary else Soft),
                    )
                    if (index != timelineSteps.lastIndex) {
                        Box(
                            Modifier
                                .width(2.dp)
                                .height(30.dp)
                                .background(if (index < current) primary else Border),
                        )
                    }
                }
                Column(
                    Modifier
                        .padding(start = 12.dp, bottom = if (index == timelineSteps.lastIndex) 0.dp else 16.dp)
                        .fillMaxWidth(),
                ) {
                    Text(
                        step.label,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        color = if (reached) Ink else Muted,
                    )
                    if (active) {
                        Text(step.hint, style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun TicketCard(order: OrderDto, storeName: String) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(storeName, style = MaterialTheme.typography.titleMedium)
                    Text("Comanda do pedido", style = MaterialTheme.typography.bodySmall, color = Muted)
                }
                Text(
                    "#${order.orderNumber ?: order.id.take(8)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
            }
            order.createdAt?.let {
                Text(formatOrderDate(it), style = MaterialTheme.typography.bodySmall, color = Muted)
            }

            DashedDivider()

            order.items.forEach { item -> OrderItemRow(item) }

            DashedDivider()

            SummaryRow("Subtotal", brl(order.subtotal))
            SummaryRow("Entrega", if (order.deliveryFee == 0.0) "Grátis" else brl(order.deliveryFee))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Total", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(brl(order.totalAmount), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            DashedDivider()

            InfoRow("Pagamento", paymentLabel(order.paymentMethod))
            InfoRow("Endereço de entrega", order.deliveryAddress)
            order.scheduledWindow?.takeIf { it.isNotBlank() }?.let { InfoRow("Agendado para", it) }
            val notes = customerNotes(order.notes)
            if (notes.isNotEmpty()) InfoRow("Observações", notes.joinToString(" · "))

            DashedDivider()

            Text(
                "Obrigado pela preferência!",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun OrderItemRow(item: OrderItemDto) {
    val options = parseOptions(item.optionsJson).map { it.name }
    Row(verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "${item.quantity}× ${item.dishName}",
                fontWeight = FontWeight.SemiBold,
            )
            if (options.isNotEmpty()) {
                Text(options.joinToString(", "), style = MaterialTheme.typography.bodySmall, color = Muted)
            }
        }
        Text(brl(item.totalPrice ?: item.unitPrice * item.quantity), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Muted, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall, color = Ink)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Muted)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Linha tracejada que imita o corte da comanda impressa. */
@Composable
private fun DashedDivider() {
    Canvas(Modifier.fillMaxWidth().height(1.dp)) {
        drawLine(
            color = Border,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
        )
    }
}

private fun statusTitle(status: String): String = when (status) {
    "CREATED", "PAID" -> "Pedido recebido"
    "ACCEPTED" -> "Pedido confirmado"
    "PREPARING" -> "Em preparo"
    "READY" -> "Pronto para entrega"
    "PICKED_UP" -> "Saiu para entrega"
    "DELIVERED" -> "Entregue"
    "CANCELLED" -> "Pedido cancelado"
    else -> status
}

private fun statusDescription(status: String): String = when (status) {
    "CREATED", "PAID" -> "A loja recebeu seu pedido e já vai começar o preparo."
    "ACCEPTED" -> "A loja confirmou o pedido e reservou o horário na cozinha."
    "PREPARING" -> "Sua comida está sendo preparada agora."
    "READY" -> "O pedido está pronto. Aguardando um entregador retirar."
    "PICKED_UP" -> "O entregador retirou o pedido e está a caminho."
    "DELIVERED" -> "Pedido entregue. Bom apetite!"
    "CANCELLED" -> "Este pedido foi cancelado pela loja."
    else -> "Acompanhe aqui as próximas atualizações do pedido."
}

private fun statusStep(status: String): Int = when (status) {
    "CREATED", "PAID", "ACCEPTED" -> 0
    "PREPARING" -> 1
    "READY" -> 2
    "PICKED_UP" -> 3
    "DELIVERED" -> 4
    else -> 0
}
