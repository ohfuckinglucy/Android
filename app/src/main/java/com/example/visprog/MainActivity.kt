package com.example.visprog

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.visprog.ui.theme.Dblue
import com.example.visprog.ui.theme.Lblue
import java.io.File

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.all { it.value }) {
            startLocationService()
        } else {
            Toast.makeText(this, "Нужны разрешения для работы", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (hasPermissions()) startLocationService()
        else requestPermissions()

        setContent {
            val navController = rememberNavController()
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Dblue
            ) { padding ->
                NavHost(
                    navController = navController,
                    startDestination = "home",
                    modifier = Modifier.padding(padding)
                ) {
                    composable("home") { HomeScreen(navController) }
                    composable("calculator") { CalculatorScreen() }
                    composable("player") { PlayerScreen() }
                    composable("settings") { SettingsScreen() }
                }
            }
        }
    }

    private fun hasPermissions(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarse = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val phone = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        val bg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        return fine && coarse && phone && bg
    }

    private fun requestPermissions() {
        val list = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            list.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        permissionLauncher.launch(list.toTypedArray())
    }

    private fun startLocationService() {
        val intent = Intent(this, LocationService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    @Composable
    fun HomeScreen(navController: NavHostController) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = { navController.navigate("calculator") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Lblue)
            ) {
                Text("Открыть калькулятор", color = Color.Black)
            }
            Button(
                onClick = { navController.navigate("player") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Lblue)
            ) {
                Text("Открыть плеер", color = Color.Black)
            }
            Button(
                onClick = { navController.navigate("settings") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Lblue)
            ) {
                Text("Настройки отправки", color = Color.Black)
            }
        }
    }

    @Composable
    fun SettingsScreen() {
        val context = LocalContext.current
        val file = File(context.filesDir, "pending.jsonl")

        var ipInput by remember { mutableStateOf("192.168.0.14:8080") }
        var statusMessage by remember { mutableStateOf<String?>(null) }
        var isSending by remember { mutableStateOf(false) }
        var isClearing by remember { mutableStateOf(false) }
        var recordCount by remember { mutableStateOf(0) }

        // Обновление количества записей каждые 1.5 секунды
        LaunchedEffect(file) {
            while (true) {
                recordCount = try {
                    if (file.exists()) file.readLines().size else 0
                } catch (_: Exception) { 0 }
                kotlinx.coroutines.delay(1500)
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Настройки отправки данных", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = ipInput,
                onValueChange = { ipInput = it },
                label = { Text("IP-адрес и порт") },
                placeholder = { Text("192.168.0.14:8080") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (!isSending) {
                        isSending = true
                        statusMessage = null
                        sendAllData(context, ipInput) { ok ->
                            statusMessage = if (ok) "Данные успешно отправлены" else "Не удалось отправить"
                            isSending = false
                        }
                    }
                },
                enabled = !isSending,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSending)
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                else Text("Отправить все данные")
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    if (!isClearing) {
                        isClearing = true
                        clearPendingData(context)
                        statusMessage = "Данные удалены"
                        isClearing = false
                    }
                },
                enabled = !isClearing,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Удалить все данные") }

            Spacer(Modifier.height(16.dp))

            statusMessage?.let {
                Text(
                    text = it,
                    color = if (it.contains("успешно")) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(16.dp))

            Text("Записей для отправки: $recordCount")
        }
    }


    @Composable
    fun CalculatorScreen() {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Калькулятор")
            CalculatorApp()
        }
    }

    @Composable
    fun PlayerScreen() {
        val context = LocalContext.current
        val vm: PlayerViewModel = viewModel(factory = PlayerViewModelFactory(context))

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Плеер")
            RequestStoragePermission { vm.loadAudioFiles() }
            MusicPlayerScreen(vm)
        }
    }
}
