package com.deliveryplatform.cliente.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.deliveryplatform.cliente.BuildConfig
import com.deliveryplatform.cliente.data.api.TenantTheme
import com.deliveryplatform.cliente.ui.components.AppTextField
import com.deliveryplatform.cliente.ui.components.CardBox
import com.deliveryplatform.cliente.ui.components.PrimaryButton
import com.deliveryplatform.cliente.ui.components.SecondaryButton
import com.deliveryplatform.cliente.ui.theme.Border
import com.deliveryplatform.cliente.ui.theme.Danger
import com.deliveryplatform.cliente.ui.theme.Ink
import com.deliveryplatform.cliente.ui.theme.Muted
import com.deliveryplatform.cliente.ui.theme.Soft
import com.deliveryplatform.cliente.ui.theme.brandColor

/**
 * Porta de entrada do app do cliente, em tela cheia.
 *
 * O acesso e obrigatorio: o app so mostra estabelecimentos, cardapio e pedidos depois
 * que a sessao CUSTOMER existe. Quando o cliente chega pelo link de uma loja, o hero
 * mostra essa loja e o vinculo e criado logo apos o login. O cadastro publico cria
 * sempre CUSTOMER; nenhum outro perfil (vendedor, entregador, suporte) entra por aqui.
 */
@Composable
fun LoginScreen(
    theme: TenantTheme?,
    saving: Boolean,
    serverError: String?,
    onClearError: () -> Unit,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String, String) -> Unit,
    onGoogle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var registering by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    fun switchMode(register: Boolean) {
        registering = register
        localError = null
        onClearError()
    }

    fun submit() {
        localError = null
        val cleanEmail = email.trim()
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            localError = "Informe um e-mail válido."
            return
        }
        if (password.length < 8) {
            localError = "A senha precisa de pelo menos 8 caracteres."
            return
        }
        if (registering) {
            if (name.isBlank()) {
                localError = "Informe seu nome."
                return
            }
            onRegister(name.trim(), cleanEmail, password, phone.trim())
        } else {
            onLogin(cleanEmail, password)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 28.dp),
    ) {
        if (theme != null) StoreHero(theme) else AppHero()

        Column(
            modifier = Modifier.padding(horizontal = 20.dp).padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    if (registering) "Criar sua conta" else "Entrar na sua conta",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    when {
                        registering && theme != null ->
                            "Leva menos de um minuto. Depois é só escolher os pratos."
                        registering ->
                            "Uma conta para todos os seus pedidos e estabelecimentos."
                        theme != null ->
                            "Você chegou pelo link de ${theme.name}. Entre para abrir o cardápio."
                        else ->
                            "Entre para ver seus estabelecimentos e acompanhar seus pedidos."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
            }

            AuthModeSwitch(registering = registering, onChange = ::switchMode)

            CardBox {
                if (registering) {
                    AppTextField(value = name, onValueChange = { name = it }, label = "Nome completo")
                    AppTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = "Telefone (opcional)",
                        keyboardType = KeyboardType.Phone,
                    )
                }
                AppTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "E-mail",
                    keyboardType = KeyboardType.Email,
                )
                AppTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Senha",
                    password = !showPassword,
                    trailingIcon = {
                        TextButton(onClick = { showPassword = !showPassword }) {
                            Text(
                                if (showPassword) "Ocultar" else "Mostrar",
                                style = MaterialTheme.typography.bodySmall,
                                color = Muted,
                            )
                        }
                    },
                )

                (localError ?: serverError)?.let { message ->
                    Text(message, color = Danger, style = MaterialTheme.typography.bodySmall)
                }

                PrimaryButton(
                    text = if (registering) "Criar conta" else "Entrar",
                    onClick = ::submit,
                    modifier = Modifier.fillMaxWidth(),
                    loading = saving,
                )
            }

            if (BuildConfig.GOOGLE_CLIENT_ID.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Border)
                    Text(" ou ", color = Muted, style = MaterialTheme.typography.bodySmall)
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Border)
                }
                SecondaryButton(
                    text = "Continuar com Google",
                    onClick = {
                        signInWithGoogle(
                            context = context,
                            onToken = onGoogle,
                            onError = { localError = it },
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Surface(color = Soft, shape = MaterialTheme.shapes.medium) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("O que fica salvo na sua conta", fontWeight = FontWeight.SemiBold, color = Ink)
                    Text(
                        "Seus estabelecimentos, endereços, cartões, preferências alimentares e o histórico dos pedidos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                    )
                }
            }

            theme?.phone?.takeIf { it.isNotBlank() }?.let { phoneContact ->
                Text(
                    "Problemas para entrar? Fale com a loja: $phoneContact",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun StoreHero(theme: TenantTheme) {
    val primary = brandColor(theme.primaryColor)
    val secondary = brandColor(theme.secondaryColor, primary)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(232.dp)
            .clip(RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp))
            .background(Brush.linearGradient(listOf(primary, secondary))),
        contentAlignment = Alignment.BottomStart,
    ) {
        theme.bannerUrl?.takeIf { it.isNotBlank() }?.let { url ->
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
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                theme.name,
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                theme.description?.takeIf { it.isNotBlank() } ?: "Peça agora e receba em casa.",
                color = Color.White.copy(alpha = 0.92f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                theme.rating?.let { rating -> HeroPill(String.format("%.1f ★", rating)) }
                theme.deliveryTimeMin?.let { minutes -> HeroPill("$minutes min") }
            }
        }
    }
}

@Composable
private fun AppHero() {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(232.dp)
            .clip(RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp))
            .background(Brush.linearGradient(listOf(primary, secondary))),
        contentAlignment = Alignment.BottomStart,
    ) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                BuildConfig.APP_NAME,
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Peça nas suas lojas favoritas e acompanhe tudo em um só lugar.",
                color = Color.White.copy(alpha = 0.92f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeroPill("Cardápio")
                HeroPill("Pedidos")
                HeroPill("Entrega")
            }
        }
    }
}

@Composable
private fun HeroPill(text: String) {
    Surface(color = Color.White.copy(alpha = 0.22f), shape = RoundedCornerShape(50)) {
        Text(
            text,
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun AuthModeSwitch(registering: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Soft)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ModeSegment(
            label = "Entrar",
            selected = !registering,
            modifier = Modifier.weight(1f),
            onClick = { onChange(false) },
        )
        ModeSegment(
            label = "Criar conta",
            selected = registering,
            modifier = Modifier.weight(1f),
            onClick = { onChange(true) },
        )
    }
}

@Composable
private fun ModeSegment(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .clickable(onClick = onClick),
        color = if (selected) Color.White else Color.Transparent,
        shadowElevation = if (selected) 1.dp else 0.dp,
        shape = RoundedCornerShape(11.dp),
    ) {
        Text(
            label,
            modifier = Modifier.padding(vertical = 10.dp),
            color = if (selected) MaterialTheme.colorScheme.primary else Muted,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}
