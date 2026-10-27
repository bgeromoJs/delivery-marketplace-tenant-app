package com.deliveryplatform.cliente.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.deliveryplatform.cliente.data.api.AddressRequest
import com.deliveryplatform.cliente.data.api.CardDto
import com.deliveryplatform.cliente.data.api.CardRequest
import com.deliveryplatform.cliente.ui.components.AppTextField
import com.deliveryplatform.cliente.ui.components.CardBox
import com.deliveryplatform.cliente.ui.components.ChoiceChip
import com.deliveryplatform.cliente.ui.components.EmptyState
import com.deliveryplatform.cliente.ui.components.PrimaryButton
import com.deliveryplatform.cliente.ui.components.SectionTitle
import com.deliveryplatform.cliente.ui.components.SecondaryButton
import com.deliveryplatform.cliente.ui.theme.Border
import com.deliveryplatform.cliente.ui.theme.Muted
import com.deliveryplatform.cliente.ui.theme.Soft

private val foodPreferenceOptions = listOf(
    "VEGETARIAN",
    "VEGAN",
    "GLUTEN_FREE",
    "LACTOSE_FREE",
)

@Composable
fun ProfileScreen(
    state: ClienteUiState,
    onSaveProfile: (String, String, List<String>) -> Unit,
    onSaveAddress: (String?, AddressRequest) -> Unit,
    onDeleteAddress: (String) -> Unit,
    onDefaultAddress: (String) -> Unit,
    onSaveCard: (CardRequest) -> Unit,
    onDeleteCard: (String) -> Unit,
    onDefaultCard: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val profile = state.profile ?: return

    var name by remember(profile.name) { mutableStateOf(profile.name) }
    var phone by remember(profile.phone) { mutableStateOf(profile.phone.orEmpty()) }
    val selectedPreferences = remember(profile.foodPreferences) {
        mutableStateListOf<String>().apply { addAll(profile.foodPreferences) }
    }

    var addressDialogOpen by remember { mutableStateOf(false) }
    var editingAddress by remember { mutableStateOf<AddressDto?>(null) }
    var cardDialogOpen by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        profile.name.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(profile.name, style = MaterialTheme.typography.titleLarge)
                    Text(profile.email, style = MaterialTheme.typography.bodySmall, color = Muted)
                }
                TextButton(onClick = onLogout) { Text("Sair") }
            }
        }

        item {
            CardBox {
                Text("Seus dados", style = MaterialTheme.typography.titleMedium)
                AppTextField(value = name, onValueChange = { name = it }, label = "Nome")
                AppTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = "Telefone",
                    keyboardType = KeyboardType.Phone,
                )
                Text("Preferências alimentares", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    foodPreferenceOptions.take(2).forEach { option ->
                        ChoiceChip(
                            label = preferenceLabel(option),
                            selected = selectedPreferences.contains(option),
                            onClick = {
                                if (selectedPreferences.contains(option)) selectedPreferences.remove(option)
                                else selectedPreferences.add(option)
                            },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    foodPreferenceOptions.drop(2).forEach { option ->
                        ChoiceChip(
                            label = preferenceLabel(option),
                            selected = selectedPreferences.contains(option),
                            onClick = {
                                if (selectedPreferences.contains(option)) selectedPreferences.remove(option)
                                else selectedPreferences.add(option)
                            },
                        )
                    }
                }
                Text(
                    "Usamos as preferências para sugerir e filtrar pratos do cardápio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
                PrimaryButton(
                    text = "Salvar dados",
                    onClick = { onSaveProfile(name, phone, selectedPreferences.toList()) },
                    modifier = Modifier.fillMaxWidth(),
                    loading = state.saving,
                )
            }
        }

        item {
            SectionTitle("Endereços") {
                TextButton(onClick = {
                    editingAddress = null
                    addressDialogOpen = true
                }) { Text("Adicionar") }
            }
        }

        if (state.addresses.isEmpty()) {
            item {
                EmptyState(
                    title = "Nenhum endereço",
                    description = "Cadastre Casa ou Trabalho para agilizar o checkout.",
                )
            }
        } else {
            items(state.addresses, key = { it.id }) { address ->
                AddressCard(
                    address = address,
                    onDefault = { onDefaultAddress(address.id) },
                    onEdit = {
                        editingAddress = address
                        addressDialogOpen = true
                    },
                    onDelete = { onDeleteAddress(address.id) },
                )
            }
        }

        item {
            SectionTitle("Cartões") {
                TextButton(onClick = { cardDialogOpen = true }) { Text("Adicionar") }
            }
        }

        if (state.cards.isEmpty()) {
            item {
                EmptyState(
                    title = "Nenhum cartão salvo",
                    description = "Guardamos apenas bandeira, últimos 4 dígitos, titular e validade.",
                )
            }
        } else {
            items(state.cards, key = { it.id }) { card ->
                CardItem(
                    card = card,
                    onDefault = { onDefaultCard(card.id) },
                    onDelete = { onDeleteCard(card.id) },
                )
            }
        }

        item {
            Surface(
                color = Soft,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "Segurança: o número completo do cartão e o CVV nunca são enviados nem armazenados.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                    modifier = Modifier.padding(14.dp),
                )
            }
        }
    }

    if (addressDialogOpen) {
        AddressDialog(
            initial = editingAddress,
            onDismiss = { addressDialogOpen = false },
            onSave = { request ->
                onSaveAddress(editingAddress?.id, request)
                addressDialogOpen = false
            },
        )
    }

    if (cardDialogOpen) {
        CardDialog(
            onDismiss = { cardDialogOpen = false },
            onSave = { request ->
                onSaveCard(request)
                cardDialogOpen = false
            },
        )
    }
}

@Composable
private fun AddressCard(
    address: AddressDto,
    onDefault: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(address.label, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (address.isDefault) {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(50)) {
                        Text(
                            "Padrão",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Text(
                address.formatted ?: "${address.street}, ${address.number}",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!address.isDefault) {
                    TextButton(onClick = onDefault) { Text("Tornar padrão") }
                }
                TextButton(onClick = onEdit) { Text("Editar") }
                TextButton(onClick = onDelete) { Text("Excluir") }
            }
        }
    }
}

@Composable
private fun CardItem(card: CardDto, onDefault: () -> Unit, onDelete: () -> Unit) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${card.brand} •••• ${card.last4}",
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (card.isDefault) {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(50)) {
                        Text(
                            "Padrão",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Text(
                "${card.holderName} · validade ${String.format("%02d", card.expiryMonth)}/${card.expiryYear}",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!card.isDefault) {
                    TextButton(onClick = onDefault) { Text("Tornar padrão") }
                }
                TextButton(onClick = onDelete) { Text("Excluir") }
            }
        }
    }
}

@Composable
private fun AddressDialog(
    initial: AddressDto?,
    onDismiss: () -> Unit,
    onSave: (AddressRequest) -> Unit,
) {
    var label by remember { mutableStateOf(initial?.label ?: "Casa") }
    var street by remember { mutableStateOf(initial?.street.orEmpty()) }
    var number by remember { mutableStateOf(initial?.number.orEmpty()) }
    var complement by remember { mutableStateOf(initial?.complement.orEmpty()) }
    var district by remember { mutableStateOf(initial?.district.orEmpty()) }
    var city by remember { mutableStateOf(initial?.city ?: "São Paulo") }
    var state by remember { mutableStateOf(initial?.state ?: "SP") }
    var zip by remember { mutableStateOf(initial?.zipCode.orEmpty()) }
    var reference by remember { mutableStateOf(initial?.reference.orEmpty()) }
    var makeDefault by remember { mutableStateOf(initial?.isDefault ?: false) }
    var error by remember { mutableStateOf<String?>(null) }

    DialogShell(title = if (initial == null) "Novo endereço" else "Editar endereço", onDismiss = onDismiss) {
        Text("Rótulo", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Casa", "Trabalho", "Outro").forEach { option ->
                ChoiceChip(option, selected = label == option, onClick = { label = option })
            }
        }
        AppTextField(value = street, onValueChange = { street = it }, label = "Rua")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppTextField(value = number, onValueChange = { number = it }, label = "Número", modifier = Modifier.weight(1f))
            AppTextField(value = complement, onValueChange = { complement = it }, label = "Complemento", modifier = Modifier.weight(1f))
        }
        AppTextField(value = district, onValueChange = { district = it }, label = "Bairro")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppTextField(value = city, onValueChange = { city = it }, label = "Cidade", modifier = Modifier.weight(1f))
            AppTextField(value = state, onValueChange = { state = it.take(2).uppercase() }, label = "UF", modifier = Modifier.weight(0.4f))
        }
        AppTextField(
            value = zip,
            onValueChange = { zip = it },
            label = "CEP",
            keyboardType = KeyboardType.Number,
        )
        AppTextField(value = reference, onValueChange = { reference = it }, label = "Ponto de referência")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Usar como padrão", modifier = Modifier.weight(1f))
            ChoiceChip(
                label = if (makeDefault) "Sim" else "Não",
                selected = makeDefault,
                onClick = { makeDefault = !makeDefault },
            )
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        PrimaryButton(
            text = "Salvar endereço",
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                error = null
                if (street.isBlank() || number.isBlank() || city.isBlank()) {
                    error = "Informe rua, número e cidade."
                    return@PrimaryButton
                }
                onSave(
                    AddressRequest(
                        label = label,
                        street = street,
                        number = number,
                        complement = complement.ifBlank { null },
                        district = district.ifBlank { null },
                        city = city,
                        state = state,
                        zipCode = zip.ifBlank { null },
                        reference = reference.ifBlank { null },
                        isDefault = makeDefault,
                    ),
                )
            },
        )
    }
}

@Composable
private fun CardDialog(onDismiss: () -> Unit, onSave: (CardRequest) -> Unit) {
    var brand by remember { mutableStateOf("Visa") }
    var last4 by remember { mutableStateOf("") }
    var holder by remember { mutableStateOf("") }
    var month by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var makeDefault by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    DialogShell(title = "Novo cartão", onDismiss = onDismiss) {
        Text("Bandeira", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Visa", "Master", "Elo", "Amex").forEach { option ->
                ChoiceChip(option, selected = brand == option, onClick = { brand = option })
            }
        }
        AppTextField(
            value = last4,
            onValueChange = { value -> last4 = value.filter { it.isDigit() }.take(4) },
            label = "Últimos 4 dígitos",
            keyboardType = KeyboardType.Number,
        )
        AppTextField(value = holder, onValueChange = { holder = it.uppercase() }, label = "Nome impresso no cartão")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppTextField(
                value = month,
                onValueChange = { value -> month = value.filter { it.isDigit() }.take(2) },
                label = "Mês (MM)",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
            AppTextField(
                value = year,
                onValueChange = { value -> year = value.filter { it.isDigit() }.take(4) },
                label = "Ano (AAAA)",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Usar como padrão", modifier = Modifier.weight(1f))
            ChoiceChip(
                label = if (makeDefault) "Sim" else "Não",
                selected = makeDefault,
                onClick = { makeDefault = !makeDefault },
            )
        }
        Text(
            "Digite apenas os 4 últimos dígitos. Não pedimos o número completo nem o CVV.",
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        PrimaryButton(
            text = "Salvar cartão",
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                error = null
                val monthValue = month.toIntOrNull()
                val yearValue = year.toIntOrNull()
                if (last4.length != 4) {
                    error = "Informe os 4 últimos dígitos."
                    return@PrimaryButton
                }
                if (holder.isBlank()) {
                    error = "Informe o nome impresso no cartão."
                    return@PrimaryButton
                }
                if (monthValue == null || monthValue !in 1..12) {
                    error = "Mês inválido."
                    return@PrimaryButton
                }
                if (yearValue == null || yearValue < 2024) {
                    error = "Ano inválido."
                    return@PrimaryButton
                }
                onSave(
                    CardRequest(
                        brand = brand,
                        last4 = last4,
                        holderName = holder,
                        expiryMonth = monthValue,
                        expiryYear = yearValue,
                        isDefault = makeDefault,
                    ),
                )
            },
        )
    }
}

@Composable
private fun DialogShell(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(22.dp), color = Color.White) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 620.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                content()
                SecondaryButton("Cancelar", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
