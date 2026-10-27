package com.deliveryplatform.cliente.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.deliveryplatform.cliente.data.api.AddressDto
import com.deliveryplatform.cliente.data.api.AvailabilityWindow
import com.deliveryplatform.cliente.data.api.OrderAvailability
import com.deliveryplatform.cliente.ui.components.AppTextField
import com.deliveryplatform.cliente.ui.components.CardBox
import com.deliveryplatform.cliente.ui.components.ChoiceChip
import com.deliveryplatform.cliente.ui.components.EmptyState
import com.deliveryplatform.cliente.ui.components.PrimaryButton
import com.deliveryplatform.cliente.ui.components.SecondaryButton
import com.deliveryplatform.cliente.ui.components.brl
import com.deliveryplatform.cliente.ui.theme.Border
import com.deliveryplatform.cliente.ui.theme.Danger
import com.deliveryplatform.cliente.ui.theme.Ink
import com.deliveryplatform.cliente.ui.theme.Muted
import com.deliveryplatform.cliente.ui.theme.Soft
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val windowDateFormat = DateTimeFormatter.ofPattern("EEE, dd/MM", Locale("pt", "BR"))

@Composable
fun CartScreen(
    state: ClienteUiState,
    loadAvailability: suspend () -> OrderAvailability?,
    onChangeQuantity: (CartLine, Int) -> Unit,
    onRemove: (CartLine) -> Unit,
    onCheckout: (AddressDto, String, String?, String?, String?) -> Unit,
    onLogin: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = state.theme ?: return
    var paymentMethod by remember { mutableStateOf("PIX") }
    var notes by remember { mutableStateOf("") }
    var scheduleLater by remember { mutableStateOf(false) }
    var pickedWindow by remember { mutableStateOf<AvailabilityWindow?>(null) }
    var selectedAddressId by remember { mutableStateOf<String?>(null) }
    var availability by remember { mutableStateOf<OrderAvailability?>(null) }
    var loadingWindows by remember { mutableStateOf(false) }
    var validation by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.addresses) {
        if (state.addresses.none { it.id == selectedAddressId }) {
            selectedAddressId = state.defaultAddress?.id
        }
    }

    LaunchedEffect(state.cart.size, state.theme?.tenantId) {
        if (state.cart.isEmpty()) {
            availability = null
            return@LaunchedEffect
        }
        loadingWindows = true
        availability = loadAvailability()
        loadingWindows = false
    }

    val address = state.addresses.firstOrNull { it.id == selectedAddressId } ?: state.defaultAddress
    val deliveryFee = theme.deliveryFee ?: 0.0
    val total = state.cartTotal + deliveryFee

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Seu carrinho", style = MaterialTheme.typography.headlineSmall)
        }

        if (state.cart.isEmpty()) {
            item {
                EmptyState(
                    title = "O carrinho está vazio",
                    description = "Escolha um prato no cardápio para começar.",
                )
            }
            return@LazyColumn
        }

        items(state.cart, key = { it.key }) { line ->
            CartLineCard(
                line = line,
                onPlus = { onChangeQuantity(line, 1) },
                onMinus = { onChangeQuantity(line, -1) },
                onRemove = { onRemove(line) },
            )
        }

        item {
            CardBox {
                SummaryRow("Subtotal", brl(state.cartTotal))
                SummaryRow("Taxa de entrega", brl(deliveryFee))
                HorizontalDivider(color = Border)
                SummaryRow("Total", brl(total), bold = true)
            }
        }

        if (!state.loggedIn) {
            item {
                CardBox {
                    Text("Entre para finalizar", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Seus endereços e cartões salvos agilizam o checkout.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                    )
                    PrimaryButton("Entrar ou criar conta", onClick = onLogin, modifier = Modifier.fillMaxWidth())
                }
            }
            return@LazyColumn
        }

        item {
            CardBox {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Endereço de entrega", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text(
                        "Gerenciar",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(onClick = onOpenProfile),
                    )
                }
                if (state.addresses.isEmpty()) {
                    Text(
                        "Nenhum endereço cadastrado. Cadastre na aba Conta para receber o pedido.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Danger,
                    )
                } else {
                    state.addresses.forEach { item ->
                        AddressOption(
                            address = item,
                            selected = item.id == address?.id,
                            onClick = { selectedAddressId = item.id },
                        )
                    }
                }
            }
        }

        item {
            CardBox {
                Text("Pagamento", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceChip("Pix", selected = paymentMethod == "PIX", onClick = { paymentMethod = "PIX" })
                    ChoiceChip(
                        "Cartão",
                        selected = paymentMethod == "CREDIT_CARD",
                        onClick = { paymentMethod = "CREDIT_CARD" },
                    )
                }
                if (paymentMethod == "PIX") {
                    Text(
                        "O pagamento é simulado: a loja confirma o Pix na entrega do pedido.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                    )
                } else {
                    val card = state.defaultCard
                    if (card == null) {
                        Text(
                            "Cadastre um cartão na aba Conta para escolher esta opção.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Danger,
                        )
                        SecondaryButton("Cadastrar cartão", onClick = onOpenProfile, modifier = Modifier.fillMaxWidth())
                    } else {
                        Text(
                            "Cartão ${card.brand} •••• ${card.last4} — ${card.holderName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Muted,
                        )
                    }
                }
            }
        }

        item {
            CardBox {
                Text("Quando você quer receber?", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceChip("O quanto antes", selected = !scheduleLater, onClick = { scheduleLater = false })
                    ChoiceChip("Agendar", selected = scheduleLater, onClick = { scheduleLater = true })
                }

                if (scheduleLater) {
                    val windows = availability?.windows.orEmpty()
                    when {
                        loadingWindows -> Text("Consultando a agenda da cozinha...", style = MaterialTheme.typography.bodySmall, color = Muted)
                        windows.isEmpty() -> Text(
                            "Não há janelas disponíveis para estes itens. Tente outra combinação ou peça para agora.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Danger,
                        )
                        else -> {
                            Text(
                                "A cozinha reserva ${availability?.effortMinutes ?: 0} min de preparo para este pedido.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Muted,
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(windows) { window ->
                                    ChoiceChip(
                                        label = "${windowDateLabel(window.date)} · ${window.window}",
                                        selected = pickedWindow == window,
                                        onClick = { pickedWindow = window },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            CardBox {
                AppTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = "Observação do pedido (opcional)",
                    singleLine = false,
                    keyboardType = KeyboardType.Text,
                )
            }
        }

        validation?.let { message ->
            item { Text(message, color = Danger, style = MaterialTheme.typography.bodySmall) }
        }

        item {
            PrimaryButton(
                text = "Confirmar pedido · ${brl(total)}",
                loading = state.saving,
                onClick = {
                    validation = null
                    if (address == null) {
                        validation = "Cadastre um endereço de entrega na aba Conta."
                        return@PrimaryButton
                    }
                    if (paymentMethod == "CREDIT_CARD" && state.defaultCard == null) {
                        validation = "Cadastre um cartão ou escolha Pix."
                        return@PrimaryButton
                    }
                    if (scheduleLater && pickedWindow == null) {
                        validation = "Escolha a data e a janela de entrega."
                        return@PrimaryButton
                    }
                    onCheckout(
                        address,
                        paymentMethod,
                        notes,
                        if (scheduleLater) pickedWindow?.date else null,
                        if (scheduleLater) pickedWindow?.window else null,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CartLineCard(
    line: CartLine,
    onPlus: () -> Unit,
    onMinus: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    line.dish.name,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    shape = CircleShape,
                    color = Soft,
                    modifier = Modifier.size(30.dp).clickable(onClick = onRemove),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remover", tint = Muted, modifier = Modifier.size(16.dp))
                    }
                }
            }
            line.options.takeIf { it.isNotEmpty() }?.let { options ->
                Text(
                    options.joinToString(" · ") { it.name },
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
            }
            line.notes?.let {
                Text("“$it”", style = MaterialTheme.typography.bodySmall, color = Muted)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                QuantityButton(Icons.Filled.Remove, onClick = onMinus)
                Text(" ${line.quantity} ", fontWeight = FontWeight.Bold)
                QuantityButton(Icons.Filled.Add, onClick = onPlus)
                Box(Modifier.weight(1f))
                Text(brl(line.total), fontWeight = FontWeight.Bold, color = Ink)
            }
        }
    }
}

@Composable
private fun QuantityButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(shape = CircleShape, color = Soft, modifier = Modifier.size(30.dp).clickable(onClick = onClick)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Ink, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun AddressOption(address: AddressDto, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.White,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else Border),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                address.label + if (address.isDefault) " · padrão" else "",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                address.formatted ?: "${address.street}, ${address.number}",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        )
        Text(value, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium)
    }
}

private fun windowDateLabel(date: String): String =
    runCatching { LocalDate.parse(date).format(windowDateFormat) }.getOrDefault(date)
