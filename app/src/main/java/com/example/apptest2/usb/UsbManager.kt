package com.example.apptest2.usb

import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UsbCdcManager(private val context: Context) {

    private val usbManager by lazy { context.getSystemService(Context.USB_SERVICE) as UsbManager }

    suspend fun sendString(string: String) {
        withContext(Dispatchers.IO) {
            val device = findCdcDevice()
            if (device == null) {
                // Handle device not found
                return@withContext
            }

            if (!usbManager.hasPermission(device)) {
                // Handle permission not granted
                return@withContext
            }

            val connection = usbManager.openDevice(device)
            if (connection == null) {
                // Handle could not open device
                return@withContext
            }

            val cdcInterface = device.getInterface(0) // Assuming the first interface is CDC
            connection.claimInterface(cdcInterface, true)

            val outEndpoint = cdcInterface.getEndpoint(1) // Assuming the second endpoint is for output

            val bytes = string.toByteArray()
            connection.bulkTransfer(outEndpoint, bytes, bytes.size, 0)

            connection.releaseInterface(cdcInterface)
            connection.close()
        }
    }

    private fun findCdcDevice(): UsbDevice? {
        return usbManager.deviceList.values.find { device ->
            // Check for CDC class on the interface
            (0 until device.interfaceCount).any { i ->
                device.getInterface(i).interfaceClass == UsbConstants.USB_CLASS_CDC_DATA
            }
        }
    }
}
