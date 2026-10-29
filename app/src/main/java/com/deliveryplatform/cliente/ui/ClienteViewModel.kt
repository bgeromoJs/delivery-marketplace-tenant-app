package com.deliveryplatform.cliente.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.deliveryplatform.cliente.data.AppContainer
import com.deliveryplatform.cliente.data.friendlyMessage
import com.deliveryplatform.cliente.data.isUnauthorized
import com.deliveryplatform.cliente.data.storeSlugFromLink
import com.deliveryplatform.cliente.data.api.AddressDto
import com.deliveryplatform.cliente.data.api.AddressRequest
import com.deliveryplatform.cliente.data.api.AuthResponse
import com.deliveryplatform.cliente.data.api.AvailabilityItem
import com.deliveryplatform.cliente.data.api.AvailabilityRequest
import com.deliveryplatform.cliente.data.api.CardDto
import com.deliveryplatform.cliente.data.api.CardRequest
import com.deliveryplatform.cliente.data.api.CreateOrderItem
import com.deliveryplatform.cliente.data.api.CreateOrderRequest
import com.deliveryplatform.cliente.data.api.CustomerProfile
import com.deliveryplatform.cliente.data.api.CustomerStoreDto
import com.deliveryplatform.cliente.data.api.DishDto
import com.deliveryplatform.cliente.data.api.DishOption
import com.deliveryplatform.cliente.data.api.OrderAvailability
import com.deliveryplatform.cliente.data.api.OrderDto
import com.deliveryplatform.cliente.data.api.TenantSummary
import com.deliveryplatform.cliente.data.api.TenantTheme
import com.deliveryplatform.cliente.data.api.UpdateProfileRequest
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray

data class CartLine(
    val key: String,
    val dish: DishDto,
    val quantity: Int,
    val notes: String?,
    val options: List<DishOption>,
) {
    val unitPrice: Double get() = dish.price + options.sumOf { it.price }
    val total: Double get() = unitPrice * quantity
}

data class ClienteUiState(
    val booting: Boolean = true,
    val loadingMenu: Boolean = false,
    val loadingOrders: Boolean = false,
    val loadingStores: Boolean = false,
    val linking: Boolean = false,
    val saving: Boolean = false,
    val profile: CustomerProfile? = null,
    /** Estabelecimentos vinculados, na ordem em que o cliente abriu os links. */
    val stores: List<CustomerStoreDto> = emptyList(),
    /** Tema ao vivo de cada loja vinculada (nome, taxa, avaliação). */
    val storeThemes: Map<String, TenantTheme> = emptyMap(),
    /** Lojas do ambiente; atalho de debug para validar sem link. */
    val availableStores: List<TenantSummary> = emptyList(),
    val loadingAvailable: Boolean = false,
    val activeSlug: String? = null,
    val theme: TenantTheme? = null,
    /** Link aberto antes do login: vira vínculo assim que o cliente entra. */
    val pendingLinkSlug: String? = null,
    val pendingStoreTheme: TenantTheme? = null,
    val addresses: List<AddressDto> = emptyList(),
    val cards: List<CardDto> = emptyList(),
    val dishes: List<DishDto> = emptyList(),
    val orders: List<OrderDto> = emptyList(),
    val cart: List<CartLine> = emptyList(),
    /** Loja dona dos itens do carrinho; troca de loja limpa o carrinho. */
    val cartTenantId: String? = null,
    val preferenceFilter: Boolean = true,
    val error: String? = null,
    /** Falha do último vínculo por link; o diálogo de link mostra na própria caixa. */
    val linkError: String? = null,
    val message: String? = null,
) {
    val loggedIn: Boolean get() = profile != null
    val cartCount: Int get() = cart.sumOf { it.quantity }
    val cartTotal: Double get() = cart.sumOf { it.total }
    val defaultAddress: AddressDto? get() = addresses.firstOrNull { it.isDefault } ?: addresses.firstOrNull()
    val defaultCard: CardDto? get() = cards.firstOrNull { it.isDefault } ?: cards.firstOrNull()

    /** Tema vivo da loja de um pedido, inclusive quando ela não é a loja ativa. */
    fun storeTheme(tenantId: String?): TenantTheme? = when {
        tenantId == null -> null
        tenantId == theme?.tenantId -> theme
        else -> stores.firstOrNull { it.tenantId == tenantId }
            ?.let { store -> storeThemes[store.slug] }
    }

    /** Nome da loja de um pedido, inclusive quando ela não está mais na lista. */
    fun storeName(tenantId: String?): String? = when {
        tenantId == null -> null
        tenantId == theme?.tenantId -> theme?.name
        else -> stores.firstOrNull { it.tenantId == tenantId }?.let { store ->
            storeThemes[store.slug]?.name ?: store.name
        }
    }
}

class ClienteViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(ClienteUiState())
    val state: StateFlow<ClienteUiState> = _state.asStateFlow()

    /** Rotação de tela recria a composição; a abertura da conta deve rodar uma vez só. */
    private var bootstrapped = false

    /**
     * Abertura do app. Sem sessão o cliente vê a tela de login; com sessão, a conta,
     * os estabelecimentos já vinculados e os pedidos são carregados.
     */
    fun bootstrap() {
        if (bootstrapped) return
        bootstrapped = true
        if (!container.session.isLoggedIn) {
            _state.update { it.copy(booting = false) }
            return
        }
        viewModelScope.launch { loadAccount() }
    }

    fun retry() {
        if (!_state.value.loggedIn) return
        viewModelScope.launch { loadAccount() }
    }

    // --- Estabelecimentos -----------------------------------------------------

    /**
     * Chamado pelo link do estabelecimento (deep link ou campo "colar link").
     * Sem sessão o tema público é carregado para a tela de login e o vínculo fica
     * pendente até o cliente entrar.
     */
    fun openStoreLink(raw: String) {
        val slug = storeSlugFromLink(raw)
        if (slug == null) {
            val message = "Link inválido. Use o link que a loja enviou, como deliveryhub://loja/vera-marmitex."
            _state.update { it.copy(error = message, linkError = message) }
            return
        }
        if (!_state.value.loggedIn) {
            viewModelScope.launch {
                _state.update { it.copy(linking = true, error = null, linkError = null) }
                val theme = runCatching { container.store.theme(slug) }.getOrNull()
                if (theme == null) {
                    val message = "Não encontramos a loja deste link. Confira com o estabelecimento."
                    _state.update {
                        it.copy(linking = false, error = message, linkError = message)
                    }
                } else {
                    _state.update {
                        it.copy(linking = false, pendingLinkSlug = slug, pendingStoreTheme = theme, error = null)
                    }
                }
            }
            return
        }
        viewModelScope.launch { linkAndOpen(slug) }
    }

    /** Abre uma loja já vinculada (toque na lista de estabelecimentos). */
    fun selectStore(slug: String) {
        if (slug == _state.value.activeSlug) return
        val cached = _state.value.storeThemes[slug]
        if (cached != null) {
            activateStore(cached)
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loadingMenu = true, error = null) }
            try {
                val theme = container.store.theme(slug)
                _state.update {
                    it.copy(storeThemes = it.storeThemes + (slug to theme), loadingMenu = false)
                }
                activateStore(theme)
            } catch (error: Throwable) {
                _state.update {
                    it.copy(loadingMenu = false, error = error.friendlyMessage("Não foi possível abrir esta loja."))
                }
            }
        }
    }

    /** Volta para a lista inicial de estabelecimentos. */
    fun showStoreList() {
        _state.update { it.copy(activeSlug = null, theme = null, dishes = emptyList(), error = null) }
    }

    fun unlinkStore(slug: String) {
        viewModelScope.launch {
            try {
                container.customer.unlinkStore(slug)
                val wasActive = _state.value.activeSlug == slug
                _state.update {
                    it.copy(
                        stores = it.stores.filterNot { store -> store.slug == slug },
                        storeThemes = it.storeThemes - slug,
                        activeSlug = if (wasActive) null else it.activeSlug,
                        theme = if (wasActive) null else it.theme,
                        dishes = if (wasActive) emptyList() else it.dishes,
                        cart = if (wasActive && it.cartTenantId == it.theme?.tenantId) emptyList() else it.cart,
                        cartTenantId = if (wasActive) null else it.cartTenantId,
                        message = "Loja removida dos seus estabelecimentos",
                    )
                }
            } catch (error: Throwable) {
                _state.update { it.copy(error = error.friendlyMessage("Não foi possível remover a loja.")) }
            }
        }
    }

    /** Lista as lojas do ambiente: atalho de debug para validar sem link. */
    fun loadAvailableStores() {
        if (_state.value.loadingAvailable) return
        viewModelScope.launch {
            _state.update { it.copy(loadingAvailable = true) }
            try {
                val stores = container.store.tenants()
                _state.update { it.copy(availableStores = stores, loadingAvailable = false) }
            } catch (_: Throwable) {
                _state.update { it.copy(loadingAvailable = false) }
            }
        }
    }

    private suspend fun linkAndOpen(slug: String) {
        _state.update { it.copy(linking = true, error = null, linkError = null) }
        try {
            val link = container.customer.linkStore(slug)
            val theme = runCatching { container.store.theme(link.slug) }.getOrNull()
            _state.update { current ->
                val stores = if (current.stores.any { it.slug == link.slug }) {
                    current.stores.map { if (it.slug == link.slug) link else it }
                } else {
                    current.stores + link
                }
                current.copy(
                    stores = stores,
                    storeThemes = if (theme != null) current.storeThemes + (link.slug to theme) else current.storeThemes,
                    linking = false,
                    pendingLinkSlug = null,
                    pendingStoreTheme = null,
                    error = null,
                    linkError = null,
                )
            }
            if (theme != null) {
                activateStore(theme)
                _state.update { it.copy(message = "Você entrou em ${theme.name}") }
            } else {
                val message = "Loja vinculada, mas não foi possível abrir o cardápio agora."
                _state.update { it.copy(error = message, linkError = message) }
            }
        } catch (error: Throwable) {
            val message = error.friendlyMessage("Não foi possível abrir o link da loja.")
            _state.update { it.copy(linking = false, error = message, linkError = message) }
        }
    }

    private fun activateStore(theme: TenantTheme) {
        val previousTenant = _state.value.theme?.tenantId
        val changingStore = previousTenant != null && previousTenant != theme.tenantId
        val lostCart = changingStore && _state.value.cart.isNotEmpty()
        _state.update {
            it.copy(
                activeSlug = theme.slug,
                theme = theme,
                dishes = emptyList(),
                cart = if (changingStore) emptyList() else it.cart,
                cartTenantId = if (changingStore) null else it.cartTenantId,
                error = null,
                message = if (lostCart) "Carrinho limpo porque você trocou de loja." else it.message,
            )
        }
        loadMenu(theme.tenantId, tagsFor(_state.value.profile))
    }

    private suspend fun loadAccount() {
        try {
            _state.update { it.copy(booting = true, error = null) }
            val profile = container.customer.profile()
            val addresses = container.customer.addresses()
            val cards = container.customer.cards()
            _state.update {
                it.copy(
                    profile = profile,
                    addresses = addresses,
                    cards = cards,
                    booting = false,
                    error = null,
                )
            }
            loadStores()
            openPendingOrSingleStore()
            loadOrders()
        } catch (error: Throwable) {
            if (error.isUnauthorized()) {
                expireSession()
            } else {
                _state.update { it.copy(booting = false, error = error.friendlyMessage()) }
            }
        }
    }

    private suspend fun loadStores() {
        _state.update { it.copy(loadingStores = true) }
        try {
            val stores = container.customer.stores()
            val themes = loadStoreThemes(stores)
            _state.update {
                it.copy(
                    stores = stores,
                    storeThemes = themes,
                    loadingStores = false,
                    activeSlug = it.activeSlug?.takeIf { slug -> stores.any { store -> store.slug == slug } },
                )
            }
        } catch (error: Throwable) {
            if (error.isUnauthorized()) {
                expireSession()
            } else {
                _state.update {
                    it.copy(loadingStores = false, error = error.friendlyMessage("Não foi possível carregar seus estabelecimentos."))
                }
            }
        }
    }

    private suspend fun loadStoreThemes(stores: List<CustomerStoreDto>): Map<String, TenantTheme> =
        coroutineScope {
            stores.map { store ->
                async { runCatching { container.store.theme(store.slug) }.getOrNull()?.let { store.slug to it } }
            }.awaitAll().filterNotNull().toMap()
        }

    /**
     * Depois do login: o link pendente manda; sem link, uma única loja abre direto o
     * cardápio (decisão de produto) e duas ou mais deixam a lista de estabelecimentos.
     */
    private suspend fun openPendingOrSingleStore() {
        val pending = _state.value.pendingLinkSlug
        if (pending != null) {
            linkAndOpen(pending)
            return
        }
        val current = _state.value
        if (current.activeSlug == null && current.theme == null && current.stores.size == 1) {
            val only = current.stores.first()
            val theme = current.storeThemes[only.slug] ?: runCatching { container.store.theme(only.slug) }.getOrNull()
            if (theme != null) activateStore(theme)
        }
    }

    private fun expireSession() {
        container.auth.logout()
        _state.update {
            it.copy(
                profile = null,
                booting = false,
                stores = emptyList(),
                storeThemes = emptyMap(),
                activeSlug = null,
                theme = null,
                dishes = emptyList(),
                orders = emptyList(),
                cart = emptyList(),
                cartTenantId = null,
                pendingLinkSlug = null,
                pendingStoreTheme = null,
                linkError = null,
                error = "Sua sessão expirou. Entre novamente.",
            )
        }
    }

    // --- Login / conta --------------------------------------------------------

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            try {
                val response = container.auth.login(email, password)
                if (!acceptCustomerAccount(response)) return@launch
                completeLogin(response)
            } catch (error: Throwable) {
                _state.update { it.copy(saving = false, error = error.friendlyMessage("Não foi possível entrar.")) }
            }
        }
    }

    fun register(name: String, email: String, password: String, phone: String) {
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            try {
                val response = container.auth.register(name, email, password, phone)
                if (!acceptCustomerAccount(response)) return@launch
                _state.update { it.copy(message = "Conta criada. Bem-vindo!") }
                completeLogin(response)
            } catch (error: Throwable) {
                _state.update { it.copy(saving = false, error = error.friendlyMessage("Não foi possível criar a conta.")) }
            }
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            try {
                val response = container.auth.google(idToken)
                if (!acceptCustomerAccount(response)) return@launch
                completeLogin(response)
            } catch (error: Throwable) {
                _state.update { it.copy(saving = false, error = error.friendlyMessage("Não foi possível entrar com o Google.")) }
            }
        }
    }

    /**
     * O app do cliente aceita apenas contas CUSTOMER. Vendedor, entregador, suporte e
     * administrador entram pelos aplicativos/painel correspondentes.
     */
    private fun acceptCustomerAccount(response: AuthResponse): Boolean {
        if (response.user.role != "CUSTOMER") {
            container.auth.logout()
            _state.update {
                it.copy(
                    saving = false,
                    profile = null,
                    error = "Esta conta não é de cliente. Use o app do seu perfil.",
                )
            }
            return false
        }
        return true
    }

    private suspend fun completeLogin(response: AuthResponse) {
        _state.update {
            it.copy(
                saving = false,
                profile = CustomerProfile(
                    id = response.user.id,
                    name = response.user.name,
                    email = response.user.email,
                    phone = response.user.phone,
                ),
            )
        }
        loadAccount()
    }

    fun logout() {
        container.auth.logout()
        _state.update {
            it.copy(
                profile = null,
                addresses = emptyList(),
                cards = emptyList(),
                orders = emptyList(),
                cart = emptyList(),
                cartTenantId = null,
                dishes = emptyList(),
                stores = emptyList(),
                storeThemes = emptyMap(),
                activeSlug = null,
                theme = null,
                pendingLinkSlug = null,
                pendingStoreTheme = null,
                linkError = null,
                error = null,
                message = null,
            )
        }
    }

    fun saveProfile(name: String, phone: String, preferences: List<String>) {
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            try {
                val profile = container.customer.updateProfile(
                    UpdateProfileRequest(name = name, phone = phone.takeIf { it.isNotBlank() }, foodPreferences = preferences),
                )
                _state.update { it.copy(profile = profile, saving = false, message = "Perfil atualizado") }
                _state.value.theme?.let { loadMenu(it.tenantId, tagsFor(profile)) }
            } catch (error: Throwable) {
                _state.update { it.copy(saving = false, error = error.friendlyMessage("Não foi possível salvar o perfil.")) }
            }
        }
    }

    // --- Cardápio e carrinho --------------------------------------------------

    private fun tagsFor(profile: CustomerProfile?): String? {
        if (profile == null) return null
        if (!_state.value.preferenceFilter || profile.foodPreferences.isEmpty()) return null
        return profile.foodPreferences.joinToString(",")
    }

    fun loadMenu(tenantId: String, tags: String? = null) {
        viewModelScope.launch {
            _state.update { it.copy(loadingMenu = true) }
            try {
                val dishes = container.store.dishes(tenantId, tags)
                _state.update { it.copy(dishes = dishes, loadingMenu = false) }
            } catch (error: Throwable) {
                _state.update {
                    it.copy(loadingMenu = false, error = error.friendlyMessage("Não foi possível carregar o cardápio."))
                }
            }
        }
    }

    fun togglePreferenceFilter() {
        val enabled = !_state.value.preferenceFilter
        _state.update { it.copy(preferenceFilter = enabled) }
        val tenantId = _state.value.theme?.tenantId ?: return
        val profile = _state.value.profile
        loadMenu(tenantId, if (enabled) tagsFor(profile) else null)
    }

    fun addToCart(dish: DishDto, quantity: Int, notes: String?, options: List<DishOption>) {
        val key = buildString {
            append(dish.id)
            append(options.joinToString("|") { it.id ?: it.name })
            append(notes.orEmpty())
        }
        val line = CartLine(key, dish, quantity, notes?.takeIf { it.isNotBlank() }, options)
        _state.update { current ->
            val existing = current.cart.firstOrNull { it.key == key }
            if (existing == null) {
                current.copy(
                    cart = current.cart + line,
                    cartTenantId = current.theme?.tenantId ?: current.cartTenantId,
                    message = "Item adicionado",
                )
            } else {
                current.copy(
                    cart = current.cart.map { if (it.key == key) it.copy(quantity = it.quantity + quantity) else it },
                    message = "Item atualizado",
                )
            }
        }
    }

    fun changeQuantity(line: CartLine, delta: Int) {
        _state.update { current ->
            val updated = current.cart.mapNotNull { item ->
                if (item.key != line.key) item
                else if (item.quantity + delta <= 0) null
                else item.copy(quantity = item.quantity + delta)
            }
            current.copy(
                cart = updated,
                cartTenantId = if (updated.isEmpty()) null else current.cartTenantId,
            )
        }
    }

    fun removeLine(line: CartLine) {
        _state.update { current ->
            val updated = current.cart.filterNot { item -> item.key == line.key }
            current.copy(cart = updated, cartTenantId = if (updated.isEmpty()) null else current.cartTenantId)
        }
    }

    fun clearCart() {
        _state.update { it.copy(cart = emptyList(), cartTenantId = null) }
    }

    suspend fun availability(date: String? = null): OrderAvailability? {
        val current = _state.value
        val tenantId = current.theme?.tenantId ?: return null
        return try {
            container.orders.availability(
                AvailabilityRequest(
                    tenantId = tenantId,
                    items = current.cart.map {
                        AvailabilityItem(it.dish.id, it.quantity, it.dish.effortMinutes)
                    },
                ),
            )
        } catch (error: Throwable) {
            _state.update { it.copy(error = error.friendlyMessage("Não foi possível calcular os horários.")) }
            null
        }
    }

    fun placeOrder(
        address: AddressDto,
        paymentMethod: String,
        notes: String?,
        scheduledDate: String?,
        scheduledWindow: String?,
        onSuccess: (OrderDto) -> Unit,
    ) {
        val current = _state.value
        val theme = current.theme ?: return
        val profile = current.profile ?: return
        val items = current.cart.map {
            CreateOrderItem(
                dishId = it.dish.id,
                dishName = it.dish.name,
                quantity = it.quantity,
                unitPrice = it.unitPrice,
                optionsJson = optionsToJson(it.options),
                effortMinutes = it.dish.effortMinutes,
            )
        }
        val paymentDescription = when {
            paymentMethod == "CREDIT_CARD" -> current.defaultCard?.let { "Cartao ${it.brand} final ${it.last4}" } ?: "Cartao de credito"
            else -> "Pix"
        }
        val orderNotes = listOfNotNull(
            notes?.takeIf { it.isNotBlank() },
            "Pagamento: $paymentDescription",
        ).joinToString(" | ")

        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            try {
                val order = container.orders.create(
                    CreateOrderRequest(
                        tenantId = theme.tenantId,
                        customerId = profile.id,
                        customerName = profile.name,
                        customerPhone = profile.phone,
                        deliveryAddress = address.formatted ?: "${address.street}, ${address.number}",
                        paymentMethod = paymentMethod,
                        notes = orderNotes,
                        deliveryFee = theme.deliveryFee,
                        scheduledDate = scheduledDate,
                        scheduledWindow = scheduledWindow,
                        items = items,
                    ),
                )
                _state.update {
                    it.copy(
                        cart = emptyList(),
                        cartTenantId = null,
                        saving = false,
                        message = "Pedido confirmado!",
                        orders = listOf(order) + it.orders,
                    )
                }
                onSuccess(order)
            } catch (error: Throwable) {
                _state.update { it.copy(saving = false, error = error.friendlyMessage("Não foi possível confirmar o pedido.")) }
            }
        }
    }

    // --- Pedidos --------------------------------------------------------------

    fun loadOrders() {
        val customerId = _state.value.profile?.id ?: return
        viewModelScope.launch {
            _state.update { it.copy(loadingOrders = true) }
            try {
                // Histórico de todas as lojas, do mais recente para o mais antigo.
                val orders = container.orders.byCustomer(customerId)
                    .sortedByDescending { it.createdAt.orEmpty() }
                _state.update { it.copy(orders = orders, loadingOrders = false) }
            } catch (error: Throwable) {
                _state.update {
                    it.copy(loadingOrders = false, error = error.friendlyMessage("Não foi possível carregar seus pedidos."))
                }
            }
        }
    }

    // --- Endereços e cartões --------------------------------------------------

    fun saveAddress(id: String?, body: AddressRequest) {
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            try {
                if (id == null) container.customer.createAddress(body) else container.customer.updateAddress(id, body)
                val addresses = container.customer.addresses()
                _state.update { it.copy(addresses = addresses, saving = false, message = "Endereço salvo") }
            } catch (error: Throwable) {
                _state.update { it.copy(saving = false, error = error.friendlyMessage("Não foi possível salvar o endereço.")) }
            }
        }
    }

    fun deleteAddress(id: String) {
        viewModelScope.launch {
            try {
                container.customer.deleteAddress(id)
                _state.update { it.copy(addresses = container.customer.addresses(), message = "Endereço removido") }
            } catch (error: Throwable) {
                _state.update { it.copy(error = error.friendlyMessage()) }
            }
        }
    }

    fun setDefaultAddress(id: String) {
        viewModelScope.launch {
            try {
                container.customer.defaultAddress(id)
                _state.update { it.copy(addresses = container.customer.addresses()) }
            } catch (error: Throwable) {
                _state.update { it.copy(error = error.friendlyMessage()) }
            }
        }
    }

    fun saveCard(body: CardRequest) {
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            try {
                container.customer.createCard(body)
                _state.update { it.copy(cards = container.customer.cards(), saving = false, message = "Cartão salvo") }
            } catch (error: Throwable) {
                _state.update { it.copy(saving = false, error = error.friendlyMessage("Não foi possível salvar o cartão.")) }
            }
        }
    }

    fun deleteCard(id: String) {
        viewModelScope.launch {
            try {
                container.customer.deleteCard(id)
                _state.update { it.copy(cards = container.customer.cards(), message = "Cartão removido") }
            } catch (error: Throwable) {
                _state.update { it.copy(error = error.friendlyMessage()) }
            }
        }
    }

    fun setDefaultCard(id: String) {
        viewModelScope.launch {
            try {
                container.customer.defaultCard(id)
                _state.update { it.copy(cards = container.customer.cards()) }
            } catch (error: Throwable) {
                _state.update { it.copy(error = error.friendlyMessage()) }
            }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
    fun clearMessage() = _state.update { it.copy(message = null) }

    /** Fecha o aviso do diálogo de link (cancelar ou tentar de novo). */
    fun clearLinkError() = _state.update { it.copy(linkError = null) }
}

class ClienteViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ClienteViewModel(container) as T
    }
}

fun parseOptions(json: String?): List<DishOption> {
    if (json.isNullOrBlank()) return emptyList()
    return runCatching {
        val array = JSONArray(json)
        (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val name = item.optString("name")
            if (name.isBlank()) return@mapNotNull null
            DishOption(
                id = item.optString("id").takeIf { it.isNotBlank() },
                name = name,
                price = item.optDouble("price", 0.0),
                required = item.optBoolean("required", false),
                maxChoices = item.optInt("maxChoices", 1),
            )
        }
    }.getOrDefault(emptyList())
}

fun optionsToJson(options: List<DishOption>): String? {
    if (options.isEmpty()) return null
    val array = JSONArray()
    options.forEach {
        array.put(
            org.json.JSONObject()
                .put("id", it.id)
                .put("name", it.name)
                .put("price", it.price),
        )
    }
    return array.toString()
}

fun tags(dish: DishDto): List<String> {
    val json = dish.tagsJson ?: return emptyList()
    return runCatching {
        val array = JSONArray(json)
        (0 until array.length()).map { array.optString(it) }.filter { it.isNotBlank() }
    }.getOrDefault(emptyList())
}
