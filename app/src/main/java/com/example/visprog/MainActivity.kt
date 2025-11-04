package com.example.visprog

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.TelephonyManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import org.zeromq.ZMQ
import java.io.File
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate

private var timer: Timer? = null

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.all { it.value }) {
            startPeriodicSending(this)
        } else {
            Toast.makeText(this, "Нужны разрешения для работы", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (hasPermissions()) {
            startPeriodicSending(this)
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.READ_PHONE_STATE
                )
            )
        }

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
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPeriodicSending()
    }

    private fun hasPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }
}

fun startPeriodicSending(context: Context) {
    if (timer != null) return

    val gson = Gson()
    val file = File(context.filesDir, "pending.jsonl")

    timer = Timer()
    timer!!.scheduleAtFixedRate(0, 5000) {
        if (!hasPerms(context)) return@scheduleAtFixedRate

        val fused = LocationServices.getFusedLocationProviderClient(context)
        fused.lastLocation.addOnSuccessListener { location ->
            if (location == null) return@addOnSuccessListener

            val loc = LocationData(
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = if (location.hasAltitude()) location.altitude else null,
                timestamp = location.time,
                speed = if (location.hasSpeed()) location.speed else null,
                accuracy = location.accuracy
            )

            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val cells = mutableListOf<CellInfoLte>()
            tm.allCellInfo?.forEach { c ->
                if (c is android.telephony.CellInfoLte && c.isRegistered) {
                    val id = c.cellIdentity
                    val ss = c.cellSignalStrength
                    val identity = CellIdentityLte(
                        band = id.bands?.toList(),
                        cellIdentity = id.ci.toLong(),
                        earfcn = id.earfcn,
                        mcc = id.mccString,
                        mnc = id.mncString,
                        pci = id.pci,
                        tac = id.tac
                    )
                    val signal = CellSignalStrengthLte(
                        asuLevel = ss.asuLevel,
                        cqi = ss.cqi,
                        rsrp = ss.rsrp,
                        rsrq = ss.rsrq,
                        rssi = ss.rssi,
                        rssnr = ss.rssnr,
                        timingAdvance = ss.timingAdvance
                    )
                    cells.add(CellInfoLte(identity, signal))
                }
            }

            val data = NetworkData(loc, cells)
            val json = gson.toJson(data)
            sendData(context, json, file)
        }
    }
}

fun sendData(context: Context, json: String, file: File) {
    Thread {
        try {
            val ctx = ZMQ.context(1)
            val socket = ctx.socket(ZMQ.REQ)
            socket.connect("tcp://192.168.0.14:8080")
            socket.setSendTimeOut(2000)
            socket.setReceiveTimeOut(2000)

            if (file.exists()) {
                file.readLines().forEach {
                    socket.send(it)
                    socket.recv()
                }
                file.delete()
            }

            socket.send(json)
            socket.recv()
            socket.close()
            ctx.close()

            (context as? ComponentActivity)?.runOnUiThread {
                Toast.makeText(context, "Отправлено", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            file.appendText(json + "\n")
            (context as? ComponentActivity)?.runOnUiThread {
                Toast.makeText(context, "Сохранено локально", Toast.LENGTH_SHORT).show()
            }
        }
    }.start()
}

fun stopPeriodicSending() {
    timer?.cancel()
    timer = null
}

fun hasPerms(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
}

@Composable
fun HomeScreen(navController: NavHostController) {
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
        RequestStoragePermission { viewModel.loadAudioFiles() }
        MusicPlayerScreen(viewModel)
    }
}
