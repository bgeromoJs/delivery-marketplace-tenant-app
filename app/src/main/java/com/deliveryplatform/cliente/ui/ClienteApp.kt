package com.deliveryplatform.cliente.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.deliveryplatform.cliente.ClienteApplication
import com.deliveryplatform.cliente.ui.components.PrimaryButton
import com.deliveryplatform.cliente.ui.theme.Canvas
import com.deliveryplatform.cliente.ui.theme.ClienteTheme
import com.deliveryplatform.cliente.ui.theme.Muted

private enum class Tab(val label: String, val icon: ImageVector) {
    MENU("Cardápio", Icons.Filled.RestaurantMenu),
    CART("Carrinho", Icons.Filled.ShoppingCart),
    ORDERS("Pedidos", Icons.AutoMirrored.Filled.ReceiptLong),
    PROFILE("Conta", Icons.Filled.Person),
}

/**
 * Raiz do app do cliente. A loja e resolvida por slug (APK white-label ou deep link),
 * o tema vem do tenant-service e a sessao fica no app.
 */
@Composable
fun ClienteApp(slug: String) {
    val context = LocalContext.current
    val container = remember { (context.applicationContext as ClienteApplication).container }
    val viewModel: ClienteViewModel = viewModel(factory = ClienteViewModelFactory(container))
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(slug) { viewModel.bootstrap(slug) }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbar.showSnackbar(it)
            viewModel.clearError()
        }
    }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    ClienteTheme(
        primaryHex = state.theme?.primaryColor ?: "#D9480F",
        secondaryHex = state.theme?.secondaryColor ?: "#B83A0A",
    ) {
        val theme = state.theme

        if (state.booting || theme == null) {
            Box(Modifier.fillMaxSize().background(Canvas), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp),
                ) {
                    Text(
                        theme?.name ?: "Delivery",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    if (state.booting) {
                        CircularProgressIndicator()
                        Text("Abrindo a loja...", color = Muted)
                    } else {
                        Text(
                            "Não conseguimos falar com a loja agora.",
                            color = Muted,
                            textAlign = TextAlign.Center,
                        )
                        PrimaryButton("Tentar novamente", onClick = { viewModel.bootstrap(slug) })
                    }
                }
            }
            return@ClienteTheme
        }

        var tab by rememberSaveable { mutableStateOf(Tab.MENU) }
        val accent = androidx.compose.material3.MaterialTheme.colorScheme.primary

        Scaffold(
            containerColor = Canvas,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                NavigationBar(containerColor = androidx.compose.ui.graphics.Color.White) {
                    Tab.entries.forEach { item ->
                        NavigationBarItem(
                            selected = tab == item,
                            onClick = { tab = item },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = {
                                Text(
                                    if (item == Tab.CART && state.cartCount > 0) {
                                        "${item.label} (${state.cartCount})"
                                    } else {
                                        item.label
                                    },
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = accent,
                                selectedTextColor = accent,
                                indicatorColor = accent.copy(alpha = 0.12f),
                                unselectedIconColor = Muted,
                                unselectedTextColor = Muted,
                            ),
                        )
                    }
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (tab) {
                    Tab.MENU -> MenuScreen(
                        state = state,
                        onTogglePreferences = viewModel::togglePreferenceFilter,
                        onAddToCart = viewModel::addToCart,
                        onOpenCart = { tab = Tab.CART },
                    )

                    Tab.CART -> CartScreen(
                        state = state,
                        loadAvailability = { viewModel.availability() },
                        onChangeQuantity = viewModel::changeQuantity,
                        onRemove = viewModel::removeLine,
                        onCheckout = { address, payment, notes, date, window ->
                            viewModel.placeOrder(address, payment, notes, date, window) {
                                tab = Tab.ORDERS
                            }
                        },
                        onLogin = { tab = Tab.PROFILE },
                        onOpenProfile = { tab = Tab.PROFILE },
                    )

                    Tab.ORDERS -> if (state.loggedIn) {
                        OrdersScreen(state = state, onRefresh = viewModel::loadOrders)
                    } else {
                        AuthScreen(
                            storeName = theme.name,
                            saving = state.saving,
                            onLogin = viewModel::login,
                            onRegister = viewModel::register,
                            onGoogle = viewModel::loginWithGoogle,
                        )
                    }

                    Tab.PROFILE -> if (state.loggedIn) {
                        ProfileScreen(
                            state = state,
                            onSaveProfile = viewModel::saveProfile,
                            onSaveAddress = viewModel::saveAddress,
                            onDeleteAddress = viewModel::deleteAddress,
                            onDefaultAddress = viewModel::setDefaultAddress,
                            onSaveCard = viewModel::saveCard,
                            onDeleteCard = viewModel::deleteCard,
                            onDefaultCard = viewModel::setDefaultCard,
                            onLogout = viewModel::logout,
                        )
                    } else {
                        AuthScreen(
                            storeName = theme.name,
                            saving = state.saving,
                            onLogin = viewModel::login,
                            onRegister = viewModel::register,
                            onGoogle = viewModel::loginWithGoogle,
                        )
                    }
                }
            }
        }
    }
}
