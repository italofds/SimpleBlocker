package com.italofds.simpleblocker

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                BlockerScreen()
            }
        }
    }
}

@Composable
fun BlockerScreen() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("BlockerPrefs", Context.MODE_PRIVATE)

    // Estado do Switch
    var isEnabled by remember {
        mutableStateOf(prefs.getBoolean("is_active", false))
    }

    // Gerenciador de Permissões de Contato
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Necessário acesso aos contatos!", Toast.LENGTH_LONG).show()
        }
    }

    // Gerenciador de "Papel" (Definir como App de Spam padrão)
    val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Resultado da solicitação de papel
    }

    fun requestPermissionsAndRole() {
        // 1. Pedir permissão de contatos
        permissionLauncher.launch(Manifest.permission.READ_CONTACTS)

        // 2. Pedir para ser o app de Call Screening (Apenas Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                if (!roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                    roleLauncher.launch(intent)
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Bloqueador de Desconhecidos", fontSize = 24.sp)

        Spacer(modifier = Modifier.height(32.dp))

        // O Switch Principal
        Switch(
            checked = isEnabled,
            onCheckedChange = { checked ->
                isEnabled = checked
                // Salva a preferência
                prefs.edit().putBoolean("is_active", checked).apply()

                if (checked) {
                    requestPermissionsAndRole()
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isEnabled) "STATUS: ATIVADO" else "STATUS: DESATIVADO",
            color = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Nota: Ao ativar, você deve conceder permissão de contatos e definir este app como identificador de chamadas padrão.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(16.dp)
        )
    }
}