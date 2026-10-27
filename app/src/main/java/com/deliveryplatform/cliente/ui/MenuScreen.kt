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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.deliveryplatform.cliente.data.api.DishDto
import com.deliveryplatform.cliente.data.api.DishOption
import com.deliveryplatform.cliente.data.api.TenantTheme
import com.deliveryplatform.cliente.ui.components.AppTextField
import com.deliveryplatform.cliente.ui.components.CardBox
import com.deliveryplatform.cliente.ui.components.ChoiceChip
import com.deliveryplatform.cliente.ui.components.EmptyState
import com.deliveryplatform.cliente.ui.components.LoadingBox
import com.deliveryplatform.cliente.ui.components.PrimaryButton
import com.deliveryplatform.cliente.ui.components.brl
import com.deliveryplatform.cliente.ui.theme.Border
import com.deliveryplatform.cliente.ui.theme.Ink
import com.deliveryplatform.cliente.ui.theme.Muted
import com.deliveryplatform.cliente.ui.theme.Soft
import com.deliveryplatform.cliente.ui.theme.brandColor

@Composable
fun MenuScreen(
    state: ClienteUiState,
    onTogglePreferences: () -> Unit,
    onAddToCart: (DishDto, Int, String?, List<DishOption>) -> Unit,
    onOpenCart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = state.theme ?: return
    var selectedDish by remember { mutableStateOf<DishDto?>(null) }
    val categories = remember(state.dishes) { state.dishes.map { it.category }.distinct() }
    var category by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(categories) {
        if (category != null && category !in categories) category = null
    }
    val visible = if (category == null) state.dishes else state.dishes.filter { it.category == category }

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { StoreHeader(theme, cartCount = state.cartCount, onOpenCart = onOpenCart) }

            if (state.profile?.foodPreferences?.isNotEmpty() == true) {
                item {
                    PreferenceBanner(
                        preferences = state.profile.foodPreferences,
                        enabled = state.preferenceFilter,
                        onToggle = onTogglePreferences,
                    )
                }
            }

            if (categories.isNotEmpty()) {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item {
                            ChoiceChip(
                                label = "Todos",
                                selected = category == null,
                                onClick = { category = null },
                            )
                        }
                        items(categories) { name ->
                            ChoiceChip(
                                label = name,
                                selected = category == name,
                                onClick = { category = name },
                            )
                        }
                    }
                }
            }

            if (state.loadingMenu) {
                item { LoadingBox() }
            } else if (visible.isEmpty()) {
                item {
                    EmptyState(
                        title = "Nada por aqui ainda",
                        description = if (state.profile?.foodPreferences?.isNotEmpty() == true && state.preferenceFilter) {
                            "Nenhum prato combina com o filtro do seu perfil. Desligue o filtro para ver o cardápio completo."
                        } else {
                            "A loja ainda não publicou pratos neste cardápio."
                        },
                    )
                }
            } else {
                items(visible, key = { it.id }) { dish ->
                    DishCard(dish = dish, onClick = { selectedDish = dish })
                }
            }
        }

        if (state.cartCount > 0) {
            CartBar(
                count = state.cartCount,
                total = state.cartTotal,
                onClick = onOpenCart,
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
            )
        }
    }

    selectedDish?.let { dish ->
        DishDialog(
            dish = dish,
            onDismiss = { selectedDish = null },
            onAdd = { quantity, notes, options ->
                onAddToCart(dish, quantity, notes, options)
                selectedDish = null
            },
        )
    }
}

@Composable
private fun StoreHeader(theme: TenantTheme, cartCount: Int, onOpenCart: () -> Unit) {
    val primary = brandColor(theme.primaryColor)
    val secondary = brandColor(theme.secondaryColor, primary)
    Column {
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
                .height(168.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(primary, secondary))),
            contentAlignment = Alignment.BottomStart,
        ) {
            theme.bannerUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f)),
                            ),
                        ),
                )
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        theme.name,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    if (cartCount > 0) {
                        Surface(
                            color = Color.White.copy(alpha = 0.22f),
                            shape = CircleShape,
                            modifier = Modifier.clickable(onClick = onOpenCart),
                        ) {
                            Text(
                                "$cartCount no carrinho",
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            )
                        }
                    }
                }
                Text(
                    theme.description ?: "Peça agora e receba em casa.",
                    color = Color.White.copy(alpha = 0.92f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFE8A317), modifier = Modifier.size(16.dp))
                Text(
                    " ${theme.rating?.let { String.format("%.1f", it) } ?: "4.8"}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                "Entrega ${brl(theme.deliveryFee ?: 0.0)}",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
            Text(
                "${theme.deliveryTimeMin ?: 40} min",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
        }
    }
}

@Composable
private fun PreferenceBanner(preferences: List<String>, enabled: Boolean, onToggle: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Filtrar pelo seu perfil alimentar", fontWeight = FontWeight.SemiBold)
                Text(
                    preferences.joinToString(" · ") { preferenceLabel(it) },
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
            }
            Switch(checked = enabled, onCheckedChange = { onToggle() })
        }
    }
}

@Composable
private fun DishCard(dish: DishDto, onClick: () -> Unit) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Border),
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DishThumbnail(dish)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    dish.name,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                dish.description?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val dishTags = tags(dish)
                if (dishTags.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        dishTags.take(3).forEach { tag ->
                            Surface(color = Soft, shape = RoundedCornerShape(50)) {
                                Text(
                                    preferenceLabel(tag),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Muted,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(brl(dish.price), fontWeight = FontWeight.Bold, color = Ink)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Adicionar", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DishThumbnail(dish: DishDto) {
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .size(88.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(primary.copy(alpha = 0.18f), Soft))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            dish.name.take(1).uppercase(),
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = primary,
        )
        dish.imageUrl?.takeIf { it.isNotBlank() }?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun CartBar(count: Int, total: Double, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (count == 1) "1 item" else "$count itens",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
            )
            Box(Modifier.weight(1f))
            Text(brl(total), color = Color.White, fontWeight = FontWeight.Bold)
            Text("  Ver carrinho", color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DishDialog(
    dish: DishDto,
    onDismiss: () -> Unit,
    onAdd: (Int, String?, List<DishOption>) -> Unit,
) {
    val options = remember(dish.id) { parseOptions(dish.optionsJson) }
    val chosen = remember(dish.id) { mutableStateListOf<String>() }
    var quantity by remember(dish.id) { mutableStateOf(1) }
    var notes by remember(dish.id) { mutableStateOf("") }

    val selectedOptions = options.filter { chosen.contains(optionKey(it)) }
    val missingRequired = options.any { it.required } && selectedOptions.none { it.required }
    val unitPrice = dish.price + selectedOptions.sumOf { it.price }
    val total = unitPrice * quantity

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(22.dp), color = Color.White) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 620.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(dish.name, style = MaterialTheme.typography.titleLarge)
                dish.description?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = Muted)
                }
                Text(brl(dish.price), fontWeight = FontWeight.Bold)
                val dishTags = tags(dish)
                if (dishTags.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        dishTags.forEach { tag ->
                            Surface(color = Soft, shape = RoundedCornerShape(50)) {
                                Text(
                                    preferenceLabel(tag),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Muted,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                }

                if (options.isNotEmpty()) {
                    HorizontalDivider(color = Border)
                    Text("Adicionais", style = MaterialTheme.typography.titleMedium)
                    options.forEach { option ->
                        val key = optionKey(option)
                        val checked = chosen.contains(key)
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                if (checked) chosen.remove(key) else chosen.add(key)
                            },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.5.dp, if (checked) MaterialTheme.colorScheme.primary else Border),
                                color = if (checked) MaterialTheme.colorScheme.primary else Color.White,
                                modifier = Modifier.size(22.dp),
                            ) {
                                if (checked) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("✓", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Text(
                                option.name + if (option.required) " (obrigatório)" else "",
                                modifier = Modifier.weight(1f).padding(start = 10.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (option.required) Ink else MaterialTheme.colorScheme.onSurface,
                            )
                            if (option.price > 0.0) {
                                Text("+ ${brl(option.price)}", style = MaterialTheme.typography.bodySmall, color = Muted)
                            }
                        }
                    }
                }

                HorizontalDivider(color = Border)
                AppTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = "Comentário para a cozinha (opcional)",
                    singleLine = false,
                    keyboardType = KeyboardType.Text,
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Quantidade", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    IconButtonCircle(Icons.Filled.Remove, enabled = quantity > 1) { quantity-- }
                    Text(" $quantity ", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    IconButtonCircle(Icons.Filled.Add) { quantity++ }
                }

                if (missingRequired) {
                    Text(
                        "Escolha pelo menos uma opção obrigatória.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                PrimaryButton(
                    text = "Adicionar · ${brl(total)}",
                    onClick = { onAdd(quantity, notes.takeIf { it.isNotBlank() }, selectedOptions) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !missingRequired,
                )
            }
        }
    }
}

@Composable
private fun IconButtonCircle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        shape = CircleShape,
        color = if (enabled) Soft else Soft.copy(alpha = 0.4f),
        modifier = Modifier.size(32.dp).clickable(enabled = enabled, onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (enabled) Ink else Muted.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private fun optionKey(option: DishOption): String = option.id ?: option.name

internal fun preferenceLabel(value: String): String = when (value.uppercase()) {
    "VEGETARIAN" -> "Vegetariano"
    "VEGAN" -> "Vegano"
    "GLUTEN_FREE" -> "Sem glúten"
    "LACTOSE_FREE" -> "Sem lactose"
    else -> value.lowercase().replaceFirstChar { it.uppercase() }
}
