package com.example.visprog

data class NetworkData(
    val location: LocationData,
    val cellInfoLte: List<CellInfoLte>
)

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?,
    val timestamp: Long,
    val speed: Float?,
    val accuracy: Float
)

data class CellInfoLte(
    val cellIdentity: CellIdentityLte,
    val signalStrength: CellSignalStrengthLte
)

data class CellIdentityLte(
    val band: List<Int>?,
    val cellIdentity: Long,
    val earfcn: Int,
    val mcc: String?,
    val mnc: String?,
    val pci: Int,
    val tac: Int
)

data class CellSignalStrengthLte(
    val asuLevel: Int,
    val cqi: Int,
    val rsrp: Int,
    val rsrq: Int,
    val rssi: Int,
    val rssnr: Int,
    val timingAdvance: Int
)