package com.deliveryplatform.cliente.data

import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

/** Converte falhas da API em mensagens curtas para a tela. */
fun Throwable.friendlyMessage(fallback: String = "Nao foi possivel concluir agora. Tente novamente."): String {
    return when (this) {
        is HttpException -> {
            val body = runCatching { response()?.errorBody()?.string() }.getOrNull()
            val parsed = body?.let(::messageFromJson)
            when {
                parsed != null -> parsed
                code() == 401 -> "Sua sessao expirou. Entre novamente."
                code() == 403 -> "Voce nao tem acesso a esta acao."
                code() == 409 -> "Ja existe um cadastro com estes dados."
                else -> fallback
            }
        }
        is IOException -> "Sem conexao com a loja. Verifique a internet e tente novamente."
        else -> message?.takeIf { it.isNotBlank() } ?: fallback
    }
}

fun Throwable.isUnauthorized(): Boolean = this is HttpException && code() == 401

private fun messageFromJson(body: String): String? {
    return runCatching {
        val json = JSONObject(body)
        when {
            json.has("message") && !json.isNull("message") -> json.getString("message")
            json.has("detail") && !json.isNull("detail") -> json.getString("detail")
            json.has("errors") -> {
                val errors = json.getJSONArray("errors")
                if (errors.length() > 0) {
                    val first = errors.getJSONObject(0)
                    first.optString("message").takeIf { it.isNotBlank() }
                } else null
            }
            else -> null
        }
    }.getOrNull()?.takeIf { it.isNotBlank() }
}
