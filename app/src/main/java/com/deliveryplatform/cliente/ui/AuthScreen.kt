package com.deliveryplatform.cliente.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.deliveryplatform.cliente.BuildConfig
import com.deliveryplatform.cliente.ui.components.AppTextField
import com.deliveryplatform.cliente.ui.components.CardBox
import com.deliveryplatform.cliente.ui.components.ChoiceChip
import com.deliveryplatform.cliente.ui.components.PrimaryButton
import com.deliveryplatform.cliente.ui.components.SecondaryButton
import com.deliveryplatform.cliente.ui.theme.Danger
import com.deliveryplatform.cliente.ui.theme.Muted
import com.deliveryplatform.cliente.ui.theme.Soft

/**
 * Entrada do cliente: e-mail/senha, criacao de conta ou Google (quando o client ID esta configurado).
 * O cadastro publico cria sempre CUSTOMER; nenhum outro perfil entra por aqui.
 */
@Composable
fun AuthScreen(
    storeName: String,
    saving: Boolean,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String, String) -> Unit,
    onGoogle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var registering by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

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
            onRegister(name, cleanEmail, password, phone)
        } else {
            onLogin(cleanEmail, password)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (registering) "Criar sua conta" else "Bem-vindo de volta",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                "Peça em $storeName com entrega e acompanhamento em tempo real.",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceChip("Entrar", selected = !registering, onClick = { registering = false; localError = null })
            ChoiceChip("Criar conta", selected = registering, onClick = { registering = true; localError = null })
        }

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
                label = "Senha (mínimo 8 caracteres)",
                password = true,
            )

            localError?.let {
                Text(it, color = Danger, style = MaterialTheme.typography.bodySmall)
            }

            PrimaryButton(
                text = if (registering) "Criar conta" else "Entrar",
                onClick = ::submit,
                modifier = Modifier.fillMaxWidth(),
                loading = saving,
            )
        }

        if (BuildConfig.GOOGLE_CLIENT_ID.isNotBlank()) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Soft)
                Text(" ou ", color = Muted, style = MaterialTheme.typography.bodySmall)
                HorizontalDivider(modifier = Modifier.weight(1f), color = Soft)
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
                Text("Por que criar conta?", fontWeight = FontWeight.SemiBold)
                Text(
                    "Para salvar endereços, cartões e preferências alimentares, acompanhar pedidos e repetir o que você gostou.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
            }
        }
    }
}
