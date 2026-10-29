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
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.deliveryplatform.cliente.BuildConfig
import com.deliveryplatform.cliente.ClienteApplication
import com.deliveryplatform.cliente.data.StoreLinkEvent
import com.deliveryplatform.cliente.ui.components.EmptyState
import com.deliveryplatform.cliente.ui.components.PrimaryButton
import com.deliveryplatform.cliente.ui.theme.Canvas
import com.deliveryplatform.cliente.ui.theme.ClienteTheme
import com.deliveryplatform.cliente.ui.theme.Ink
import com.deliveryplatform.cliente.ui.theme.Muted
import kotlinx.coroutines.flow.Flow

private enum class Tab(val label: String, val icon: ImageVector) {
    MENU("Cardápio", Icons.Filled.RestaurantMenu),
    CART("Carrinho", Icons.Filled.ShoppingCart),
    ORDERS("Pedidos", Icons.AutoMirrored.Filled.ReceiptLong),
    PROFILE("Conta", Icons.Filled.Person),
}

/**
 * Raiz do app do cliente.
 *
 * O app não é mais de uma loja só: o cliente entra com a conta `CUSTOMER` e os
 * estabelecimentos chegam pelos links (`deliveryhub://loja/{slug}`). Uma única loja
 * vinculada abre o cardápio direto; duas ou mais mostram a lista inicial.
 */
@Composable
fun ClienteApp(storeLinks: Flow<StoreLinkEvent>) {
    val context = LocalContext.current
    val container = remember { (context.applicationContext as ClienteApplication).container }
    val viewModel: ClienteViewModel = viewModel(factory = ClienteViewModelFactory(container))
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.bootstrap() }

    // O fluxo tem replay: guardar o nonce processado evita reabrir a loja em cada
    // recomposição, mas um novo toque no mesmo link (novo nonce) continua funcionando.
    var handledLinkNonce by rememberSaveable { mutableStateOf<Long?>(null) }
    LaunchedEffect(storeLinks) {
        storeLinks.collect { event ->
            if (event.nonce == handledLinkNonce) return@collect
            handledLinkNonce = event.nonce
            viewModel.openStoreLink(event.slug)
        }
    }

    // Erros de abertura e de login ficam na tela; no app aberto viram snackbar.
    LaunchedEffect(state.error, state.loggedIn) {
        val error = state.error ?: return@LaunchedEffect
        if (state.loggedIn) {
            snackbar.showSnackbar(error)
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
        primaryHex = state.theme?.primaryColor ?: state.pendingStoreTheme?.primaryColor ?: "#D9480F",
        secondaryHex = state.theme?.secondaryColor ?: state.pendingStoreTheme?.secondaryColor ?: "#B83A0A",
    ) {
        when {
            state.booting -> BootScreen()

            !state.loggedIn -> LoginScreen(
                theme = state.pendingStoreTheme,
                saving = state.saving,
                serverError = state.error,
                onClearError = viewModel::clearError,
                onLogin = viewModel::login,
                onRegister = viewModel::register,
                onGoogle = viewModel::loginWithGoogle,
            )

            else -> LoggedInShell(state = state, viewModel = viewModel, snackbar = snackbar)
        }
    }
}

@Composable
private fun BootScreen() {
    Box(Modifier.fillMaxSize().background(Canvas), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Text(
                BuildConfig.APP_NAME,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Ink,
            )
            CircularProgressIndicator()
            Text("Abrindo sua conta...", color = Muted)
        }
    }
}

@Composable
private fun LoggedInShell(
    state: ClienteUiState,
    viewModel: ClienteViewModel,
    snackbar: SnackbarHostState,
) {
    var tab by rememberSaveable { mutableStateOf(Tab.MENU) }

    // Um link aberto com o app em segundo plano leva direto para o cardápio.
    LaunchedEffect(state.activeSlug) {
        if (state.activeSlug != null) tab = Tab.MENU
    }

    val accent = MaterialTheme.colorScheme.primary
    val hasStore = state.theme != null

    Scaffold(
        containerColor = Canvas,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                Tab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = {
                            Icon(
                                imageVector = if (item == Tab.MENU && !hasStore) {
                                    Icons.Filled.Storefront
                                } else {
                                    item.icon
                                },
                                contentDescription = if (item == Tab.MENU && !hasStore) "Lojas" else item.label,
                            )
                        },
                        label = {
                            Text(
                                when {
                                    item == Tab.MENU && !hasStore -> "Lojas"
                                    item == Tab.CART && state.cartCount > 0 -> "${item.label} (${state.cartCount})"
                                    else -> item.label
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
                Tab.MENU -> if (state.theme == null) {
                    StoresScreen(
                        state = state,
                        onOpenStore = viewModel::selectStore,
                        onLinkStore = viewModel::openStoreLink,
                        onDismissLinkError = viewModel::clearLinkError,
                        onLoadAvailable = viewModel::loadAvailableStores,
                    )
                } else {
                    MenuScreen(
                        state = state,
                        onTogglePreferences = viewModel::togglePreferenceFilter,
                        onAddToCart = viewModel::addToCart,
                        onOpenCart = { tab = Tab.CART },
                        onChangeStore = {
                            viewModel.showStoreList()
                            tab = Tab.MENU
                        },
                    )
                }

                Tab.CART -> if (state.theme == null) {
                    PickStoreForCart(onOpenStores = { tab = Tab.MENU })
                } else {
                    CartScreen(
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
                }

                Tab.ORDERS -> OrdersScreen(state = state, onRefresh = viewModel::loadOrders)

                Tab.PROFILE -> ProfileScreen(
                    state = state,
                    onSaveProfile = viewModel::saveProfile,
                    onSaveAddress = viewModel::saveAddress,
                    onDeleteAddress = viewModel::deleteAddress,
                    onDefaultAddress = viewModel::setDefaultAddress,
                    onSaveCard = viewModel::saveCard,
                    onDeleteCard = viewModel::deleteCard,
                    onDefaultCard = viewModel::setDefaultCard,
                    onOpenStores = {
                        viewModel.showStoreList()
                        tab = Tab.MENU
                    },
                    onOpenStore = { slug ->
                        viewModel.selectStore(slug)
                        tab = Tab.MENU
                    },
                    onUnlinkStore = viewModel::unlinkStore,
                    onLogout = viewModel::logout,
                )
            }
        }
    }
}

@Composable
private fun PickStoreForCart(onOpenStores: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EmptyState(
            title = "Escolha um estabelecimento",
            description = "O carrinho é montado dentro de uma loja. Abra a aba Lojas e toque no estabelecimento que você quer pedir.",
        )
        PrimaryButton(
            text = "Ver estabelecimentos",
            onClick = onOpenStores,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
    }
}
