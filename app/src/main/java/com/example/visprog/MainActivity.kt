package com.example.visprog

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.visprog.ui.theme.Dblue
import com.example.visprog.ui.theme.Lblue
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.io.File
import java.io.FileWriter
import java.io.IOException
import com.google.gson.Gson
import java.io.OutputStream
import java.io.OutputStreamWriter

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var latitude by mutableStateOf<Double?>(null)
    private var longitude by mutableStateOf<Double?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    requestLocation()
                } else {
                    Toast.makeText(this, "Разрешение не получено", Toast.LENGTH_SHORT).show()
                }
            }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            requestLocation()
        }

        setContent {
            val navController = rememberNavController()
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Dblue,
                topBar = {},
                bottomBar = {}
            ) { paddingValues ->
                NavHost(
                    navController = navController,
                    startDestination = "home",
                    modifier = Modifier.padding(paddingValues)
                ) {
                    composable("home") {
                        HomeScreen(navController, latitude, longitude)
                    }
                    composable("calculator") {
                        CalculatorScreen()
                    }
                    composable("player") {
                        PlayerScreen()
                    }
                }
            }
        }
    }

    data class Coordinates(val latitude: Double?, val longitude: Double?)

    private fun requestLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    latitude = location.latitude
                    longitude = location.longitude

                    saveCoordinates(this, Coordinates(latitude, longitude))
                } else {
                    Toast.makeText(this, "Местоположение недоступно", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "Нет разрешения на доступ к местоположению", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun saveCoordinates(context: Context, coordinates: MainActivity.Coordinates) {
    val gson = Gson()
    val json = gson.toJson(coordinates)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "coordinates.json")
            put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/")
        }

        val resolver = context.contentResolver
        val uri: Uri? = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)

        if (uri != null) {
            try {
                val outputStream: OutputStream? = resolver.openOutputStream(uri)
                if (outputStream != null) {
                    val writer = OutputStreamWriter(outputStream)
                    writer.write(json)
                    writer.close()

                    Toast.makeText(context, "Координаты сохранены в папку Загрузки", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Ошибка при открытии потока записи", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                Toast.makeText(context, "Ошибка при сохранении координат", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Не удалось создать файл", Toast.LENGTH_SHORT).show()
        }
    }
}


@Composable
fun HomeScreen(navController: NavHostController, latitude: Double?, longitude: Double?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = { navController.navigate("calculator") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Lblue)
        ) {
            Text(text = "Открыть калькулятор", color = Color.Black)
        }

        Button(
            onClick = { navController.navigate("player") },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Lblue)
        ) {
            Text(text = "Открыть плеер", color = Color.Black)
        }

        Spacer(modifier = Modifier.weight(1f))

        if (latitude != null && longitude != null) {
            Text("Широта: $latitude")
            Text("Долгота: $longitude")
        } else {
            Text("Координаты не получены")
        }
    }
}

@Composable
fun CalculatorScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Калькулятор")
        CalculatorApp()
    }
}

@Composable
fun PlayerScreen() {
    val context = LocalContext.current
    val viewModel: PlayerViewModel = viewModel(factory = PlayerViewModelFactory(context))

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Плеер")
        val viewModel: PlayerViewModel = viewModel(factory = PlayerViewModelFactory(LocalContext.current))

        Column {
            RequestStoragePermission {
                viewModel.loadAudioFiles()
            }
            MusicPlayerScreen(viewModel)
        }
    }
}
