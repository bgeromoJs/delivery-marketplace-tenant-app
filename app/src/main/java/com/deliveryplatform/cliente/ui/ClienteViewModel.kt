package com.deliveryplatform.cliente.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.deliveryplatform.cliente.data.AppContainer
import com.deliveryplatform.cliente.data.friendlyMessage
import com.deliveryplatform.cliente.data.isUnauthorized
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
import com.deliveryplatform.cliente.data.api.DishDto
import com.deliveryplatform.cliente.data.api.DishOption
import com.deliveryplatform.cliente.data.api.OrderAvailability
import com.deliveryplatform.cliente.data.api.OrderDto
import com.deliveryplatform.cliente.data.api.TenantTheme
import com.deliveryplatform.cliente.data.api.UpdateProfileRequest
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
    val saving: Boolean = false,
    val theme: TenantTheme? = null,
    val profile: CustomerProfile? = null,
    val addresses: List<AddressDto> = emptyList(),
    val cards: List<CardDto> = emptyList(),
    val dishes: List<DishDto> = emptyList(),
    val orders: List<OrderDto> = emptyList(),
    val cart: List<CartLine> = emptyList(),
    val preferenceFilter: Boolean = true,
    val error: String? = null,
    val message: String? = null,
) {
    val loggedIn: Boolean get() = profile != null
    val cartCount: Int get() = cart.sumOf { it.quantity }
    val cartTotal: Double get() = cart.sumOf { it.total }
    val defaultAddress: AddressDto? get() = addresses.firstOrNull { it.isDefault } ?: addresses.firstOrNull()
    val defaultCard: CardDto? get() = cards.firstOrNull { it.isDefault } ?: cards.firstOrNull()
}

class ClienteViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(ClienteUiState())
    val state: StateFlow<ClienteUiState> = _state.asStateFlow()

    private var storeSlug: String = ""

    fun bootstrap(slug: String) {
        if (slug.isBlank()) {
            _state.update { it.copy(booting = false, error = "Loja nao informada.") }
            return
        }
        storeSlug = slug
        viewModelScope.launch {
            _state.update { it.copy(booting = true, error = null) }
            try {
                val theme = container.store.theme(slug)
                _state.update { it.copy(theme = theme) }
                if (container.session.isLoggedIn) {
                    loadAccount(theme.tenantId)
                } else {
                    loadMenu(theme.tenantId, tags = null)
                }
            } catch (error: Throwable) {
                _state.update { it.copy(booting = false, error = error.friendlyMessage("Nao foi possivel abrir a loja.")) }
            }
        }
    }

    private suspend fun loadAccount(tenantId: String) {
        try {
            val profile = container.customer.profile()
            val addresses = container.customer.addresses()
            val cards = container.customer.cards()
            _state.update {
                it.copy(profile = profile, addresses = addresses, cards = cards, booting = false, error = null)
            }
            loadMenu(tenantId, tagsFor(profile))
            loadOrders()
        } catch (error: Throwable) {
            if (error.isUnauthorized()) {
                container.auth.logout()
                _state.update { it.copy(profile = null, booting = false, error = "Sua sessao expirou. Entre novamente.") }
                loadMenu(tenantId, tags = null)
            } else {
                _state.update { it.copy(booting = false, error = error.friendlyMessage()) }
            }
        }
    }

    private fun tagsFor(profile: CustomerProfile): String? {
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
                _state.update { it.copy(loadingMenu = false, error = error.friendlyMessage("Nao foi possivel carregar o cardapio.")) }
            }
        }
    }

    fun togglePreferenceFilter() {
        val enabled = !_state.value.preferenceFilter
        _state.update { it.copy(preferenceFilter = enabled) }
        val tenantId = _state.value.theme?.tenantId ?: return
        val profile = _state.value.profile
        loadMenu(tenantId, if (enabled) profile?.let(::tagsFor) else null)
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            try {
                val response = container.auth.login(email, password)
                if (!acceptCustomerAccount(response)) return@launch
                completeLogin(response)
            } catch (error: Throwable) {
                _state.update { it.copy(saving = false, error = error.friendlyMessage("Nao foi possivel entrar.")) }
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
                _state.update { it.copy(saving = false, error = error.friendlyMessage("Nao foi possivel criar a conta.")) }
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
                _state.update { it.copy(saving = false, error = error.friendlyMessage("Nao foi possivel entrar com o Google.")) }
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
        val theme = _state.value.theme
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
        if (theme != null) loadAccount(theme.tenantId)
    }

    fun logout() {
        container.auth.logout()
        _state.update {
            it.copy(profile = null, addresses = emptyList(), cards = emptyList(), orders = emptyList(), cart = emptyList())
        }
        _state.value.theme?.let { loadMenu(it.tenantId, null) }
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
            if (existing == null) current.copy(cart = current.cart + line, message = "Item adicionado")
            else current.copy(
                cart = current.cart.map { if (it.key == key) it.copy(quantity = it.quantity + quantity) else it },
                message = "Item atualizado",
            )
        }
    }

    fun changeQuantity(line: CartLine, delta: Int) {
        _state.update { current ->
            val updated = current.cart.mapNotNull { item ->
                if (item.key != line.key) item
                else if (item.quantity + delta <= 0) null
                else item.copy(quantity = item.quantity + delta)
            }
            current.copy(cart = updated)
        }
    }

    fun removeLine(line: CartLine) {
        _state.update { it.copy(cart = it.cart.filterNot { item -> item.key == line.key }) }
    }

    fun clearCart() {
        _state.update { it.copy(cart = emptyList()) }
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
                _state.update { it.copy(saving = false, error = error.friendlyMessage("Nao foi possivel salvar o perfil.")) }
            }
        }
    }

    fun saveAddress(id: String?, body: AddressRequest) {
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            try {
                if (id == null) container.customer.createAddress(body) else container.customer.updateAddress(id, body)
                val addresses = container.customer.addresses()
                _state.update { it.copy(addresses = addresses, saving = false, message = "Endereco salvo") }
            } catch (error: Throwable) {
                _state.update { it.copy(saving = false, error = error.friendlyMessage("Nao foi possivel salvar o endereco.")) }
            }
        }
    }

    fun deleteAddress(id: String) {
        viewModelScope.launch {
            try {
                container.customer.deleteAddress(id)
                _state.update { it.copy(addresses = container.customer.addresses(), message = "Endereco removido") }
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
                _state.update { it.copy(cards = container.customer.cards(), saving = false, message = "Cartao salvo") }
            } catch (error: Throwable) {
                _state.update { it.copy(saving = false, error = error.friendlyMessage("Nao foi possivel salvar o cartao.")) }
            }
        }
    }

    fun deleteCard(id: String) {
        viewModelScope.launch {
            try {
                container.customer.deleteCard(id)
                _state.update { it.copy(cards = container.customer.cards(), message = "Cartao removido") }
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
            _state.update { it.copy(error = error.friendlyMessage("Nao foi possivel calcular os horarios.")) }
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
                    it.copy(cart = emptyList(), saving = false, message = "Pedido confirmado!", orders = listOf(order) + it.orders)
                }
                onSuccess(order)
            } catch (error: Throwable) {
                _state.update { it.copy(saving = false, error = error.friendlyMessage("Nao foi possivel confirmar o pedido.")) }
            }
        }
    }

    fun loadOrders() {
        val customerId = _state.value.profile?.id ?: return
        viewModelScope.launch {
            _state.update { it.copy(loadingOrders = true) }
            try {
                val orders = container.orders.byCustomer(customerId)
                _state.update { it.copy(orders = orders, loadingOrders = false) }
            } catch (error: Throwable) {
                _state.update { it.copy(loadingOrders = false, error = error.friendlyMessage("Nao foi possivel carregar seus pedidos.")) }
            }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
    fun clearMessage() = _state.update { it.copy(message = null) }
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
