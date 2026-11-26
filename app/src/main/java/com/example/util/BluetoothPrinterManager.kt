package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

data class PairedPrinterDevice(
    val name: String,
    val address: String
)

object BluetoothPrinterManager {
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    fun hasBluetoothPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun requiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
        } else {
            arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
        }
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(context: Context): Result<List<PairedPrinterDevice>> {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter: BluetoothAdapter = manager?.adapter
            ?: return Result.failure(IllegalStateException("Perangkat ini tidak memiliki adapter Bluetooth"))

        if (!adapter.isEnabled) {
            return Result.failure(IllegalStateException("Bluetooth belum aktif. Silakan aktifkan Bluetooth terlebih dahulu."))
        }

        if (!hasBluetoothPermission(context)) {
            return Result.failure(IllegalStateException("Izin Bluetooth diperlukan untuk mencari printer."))
        }

        val devices = adapter.bondedDevices.orEmpty().map { device: BluetoothDevice ->
            PairedPrinterDevice(
                name = device.name ?: "Perangkat Bluetooth",
                address = device.address
            )
        }.sortedBy { it.name }

        return Result.success(devices)
    }

    @SuppressLint("MissingPermission")
    suspend fun printEscPos(
        context: Context,
        deviceAddress: String,
        escPosBytes: ByteArray
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = manager?.adapter
                ?: return@withContext Result.failure(IllegalStateException("Adapter Bluetooth tidak tersedia"))

            if (!hasBluetoothPermission(context)) {
                return@withContext Result.failure(IllegalStateException("Izin Bluetooth belum diberikan"))
            }

            val device = adapter.getRemoteDevice(deviceAddress)
            adapter.cancelDiscovery()
            val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.use { s ->
                s.connect()
                s.outputStream.use { os ->
                    os.write(escPosBytes)
                    os.flush()
                }
            }
            Result.success("Struk berhasil dicetak ke ${device.name ?: deviceAddress}")
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Gagal mencetak ke printer: ${e.localizedMessage ?: "Koneksi terputus"}"))
        }
    }
}
