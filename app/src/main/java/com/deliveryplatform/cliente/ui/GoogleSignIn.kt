package com.deliveryplatform.cliente.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.deliveryplatform.cliente.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Login com Google via Credential Manager. O client ID e publico; nenhum google-services.json
 * e necessario. Sem GOOGLE_CLIENT_ID configurado, o botao nem aparece na tela.
 */
fun signInWithGoogle(
    context: Context,
    onToken: (String) -> Unit,
    onError: (String) -> Unit,
) {
    if (BuildConfig.GOOGLE_CLIENT_ID.isBlank()) {
        onError("Login com Google nao configurado neste aplicativo.")
        return
    }
    val activity = context.findActivity() ?: run {
        onError("Nao foi possivel abrir o login com Google.")
        return
    }
    val credentialManager = CredentialManager.create(context)
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(
            GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(BuildConfig.GOOGLE_CLIENT_ID)
                .setAutoSelectEnabled(false)
                .build(),
        )
        .build()

    CoroutineScope(Dispatchers.Main).launch {
        try {
            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                onToken(googleCredential.idToken)
            } else {
                onError("Nao foi possivel obter a conta Google.")
            }
        } catch (_: GetCredentialCancellationException) {
            // Usuario cancelou: sem mensagem.
        } catch (error: GetCredentialException) {
            onError(error.message ?: "Falha no login com Google.")
        } catch (error: Throwable) {
            onError(error.message ?: "Falha no login com Google.")
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
