package com.example.visprog

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.telephony.TelephonyManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.gson.Gson
import java.io.File
import java.util.*

class LocationService : Service() {

    private lateinit var fused: FusedLocationProviderClient
    private val gson = Gson()
    private var timer: Timer? = null

    override fun onCreate() {
        super.onCreate()
        fused = LocationServices.getFusedLocationProviderClient(this)
        startForegroundNotification()
        startPeriodicCollection()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundNotification() {
        val id = "loc_channel"
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(id, "Location", NotificationManager.IMPORTANCE_LOW)
            mgr.createNotificationChannel(channel)
        }

        val notif = NotificationCompat.Builder(this, id)
            .setContentTitle("Сбор локации")
            .setContentText("Сервис работает…")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        startForeground(1, notif)
    }

    private fun startPeriodicCollection() {
        if (timer != null) return
        timer = Timer()

        timer!!.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                collectLocation()
            }
        }, 0, 5000) // каждые 5 секунд
    }

    private fun collectLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        fused.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) saveRecord(loc)
        }
    }

    private fun saveRecord(loc: Location) {
        val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        val cells = mutableListOf<Map<String, Any?>>()
        tm.allCellInfo?.forEach { c ->
            if (c is android.telephony.CellInfoLte && c.isRegistered) {
                val id = c.cellIdentity
                val ss = c.cellSignalStrength

                val identity = mapOf(
                    "band" to id.bands?.toList(),
                    "cellIdentity" to id.ci.toLong(),
                    "earfcn" to id.earfcn,
                    "mcc" to id.mccString,
                    "mnc" to id.mncString,
                    "pci" to id.pci,
                    "tac" to id.tac
                )

                val signal = mapOf(
                    "asuLevel" to ss.asuLevel,
                    "cqi" to ss.cqi,
                    "rsrp" to ss.rsrp,
                    "rsrq" to ss.rsrq,
                    "rssi" to ss.rssi,
                    "rssnr" to ss.rssnr,
                    "timingAdvance" to ss.timingAdvance
                )

                cells.add(mapOf("cellIdentity" to identity, "signalStrength" to signal))
            }
        }

        val record = mapOf(
            "location" to mapOf(
                "latitude" to loc.latitude,
                "longitude" to loc.longitude,
                "altitude" to if (loc.hasAltitude()) loc.altitude else null,
                "timestamp" to loc.time,
                "speed" to if (loc.hasSpeed()) loc.speed else null,
                "accuracy" to loc.accuracy
            ),
            "cellInfoLte" to cells
        )

        val file = File(filesDir, "pending.jsonl")
        file.appendText(gson.toJson(record) + "\n")
    }

    override fun onDestroy() {
        timer?.cancel()
        timer = null
        super.onDestroy()
    }
}
