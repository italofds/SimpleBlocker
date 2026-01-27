package com.italofds.simpleblocker

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.CallScreeningService.CallResponse

class CallBlockerService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        // 1. Verifica se o bloqueador está ativado nas preferências
        val prefs = getSharedPreferences("BlockerPrefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("is_active", false)

        if (!isEnabled) {
            // Se o switch estiver desligado, permite tudo
            return
        }

        // 2. Obtém o número que está ligando
        val phoneNumber = callDetails.handle?.schemeSpecificPart ?: return

        // 3. Verifica direção da chamada (apenas recebidas)
        if (callDetails.callDirection == Call.Details.DIRECTION_INCOMING) {
            if (isContactSaved(phoneNumber)) {
                // É contato salvo: PERMITIR (não faz nada, o sistema segue normal)
            } else {
                // Não é contato: BLOQUEAR
                val response = CallResponse.Builder()
                    .setDisallowCall(true) // Impede a chamada
                    .setRejectCall(true)   // Rejeita (cai na caixa ou desliga)
                    .setSkipCallLog(false) // (Opcional) Mostra no histórico ou não
                    .setSkipNotification(true) // Não toca nem vibra
                    .build()

                respondToCall(callDetails, response)
            }
        }
    }

    // Função auxiliar para verificar se o número existe na agenda
    private fun isContactSaved(phoneNumber: String): Boolean {
        // Normalização simples do número (remove espaços, traços, etc) para comparação
        // Nota: Em um app real, recomenda-se usar libphonenumber para formatação robusta.

        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )

        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)

        val cursor: Cursor? = contentResolver.query(uri, projection, null, null, null)

        val exists = cursor?.use {
            it.moveToFirst()
        } ?: false

        return exists
    }
}