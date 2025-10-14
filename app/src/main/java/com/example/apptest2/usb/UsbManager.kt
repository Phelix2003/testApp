package com.example.apptest2.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class UsbCdcManager(private val context: Context) {

    private val usbManager by lazy { context.getSystemService(Context.USB_SERVICE) as UsbManager }
    private val ACTION_USB_PERMISSION = "com.example.apptest2.USB_PERMISSION"

    // Nouveau : Stockage des logs pour affichage dans l'UI
    private val _logMessages = mutableListOf<String>()
    val logMessages: List<String> get() = _logMessages.toList()

    private fun addLog(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val logEntry = "[$timestamp] $message"
        _logMessages.add(logEntry)
        Log.d("UsbCdcManager", message)

        // Garder seulement les 50 derniers logs
        if (_logMessages.size > 50) {
            _logMessages.removeAt(0)
        }
    }

    fun clearLogs() {
        _logMessages.clear()
    }

    suspend fun sendString(string: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                addLog("=== DÉBUT ENVOI USB ===")
                addLog("Tentative d'envoi: $string")

                // Lister tous les périphériques connectés
                val allDevices = usbManager.deviceList
                addLog("Nombre de périphériques USB détectés: ${allDevices.size}")

                allDevices.values.forEachIndexed { index, device ->
                    addLog("Périphérique $index:")
                    addLog("  - Nom: ${device.deviceName}")
                    addLog("  - Vendor ID: 0x${device.vendorId.toString(16)}")
                    addLog("  - Product ID: 0x${device.productId.toString(16)}")
                    addLog("  - Nombre d'interfaces: ${device.interfaceCount}")
                    addLog("  - Permission accordée: ${usbManager.hasPermission(device)}")

                    // Lister toutes les interfaces
                    for (i in 0 until device.interfaceCount) {
                        val intf = device.getInterface(i)
                        addLog("    Interface $i: class=${intf.interfaceClass}, subclass=${intf.interfaceSubclass}, protocol=${intf.interfaceProtocol}")
                    }
                }

                val device = findCdcDevice()
                if (device == null) {
                    addLog("❌ Aucun périphérique CDC trouvé")
                    addLog("Recherchez un périphérique avec interface class 2 (COMM) ou 10 (CDC_DATA)")
                    return@withContext false
                }

                addLog("✅ Périphérique CDC trouvé: ${device.deviceName}")

                if (!usbManager.hasPermission(device)) {
                    addLog("⚠️ Permission requise pour le périphérique")
                    val hasPermission = requestPermission(device)
                    if (!hasPermission) {
                        addLog("❌ Permission refusée par l'utilisateur")
                        return@withContext false
                    }
                    addLog("✅ Permission accordée")
                } else {
                    addLog("✅ Permission déjà accordée")
                }

                val connection = usbManager.openDevice(device)
                if (connection == null) {
                    addLog("❌ Impossible d'ouvrir le périphérique")
                    return@withContext false
                }

                addLog("✅ Connexion au périphérique établie")

                // Recherche de l'interface CDC correcte
                val cdcInterface = findCdcInterface(device)
                if (cdcInterface == null) {
                    addLog("❌ Interface CDC non trouvée")
                    connection.close()
                    return@withContext false
                }

                addLog("✅ Interface CDC trouvée: class=${cdcInterface.interfaceClass}")

                val claimResult = connection.claimInterface(cdcInterface, true)
                if (!claimResult) {
                    addLog("❌ Impossible de revendiquer l'interface")
                    connection.close()
                    return@withContext false
                }

                addLog("✅ Interface revendiquée avec succès")

                // Lister tous les endpoints de l'interface
                addLog("Endpoints disponibles:")
                for (i in 0 until cdcInterface.endpointCount) {
                    val ep = cdcInterface.getEndpoint(i)
                    val direction = if (ep.direction == UsbConstants.USB_DIR_OUT) "OUT" else "IN"
                    val type = when (ep.type) {
                        UsbConstants.USB_ENDPOINT_XFER_BULK -> "BULK"
                        UsbConstants.USB_ENDPOINT_XFER_INT -> "INTERRUPT"
                        UsbConstants.USB_ENDPOINT_XFER_CONTROL -> "CONTROL"
                        UsbConstants.USB_ENDPOINT_XFER_ISOC -> "ISOCHRONOUS"
                        else -> "UNKNOWN"
                    }
                    addLog("  Endpoint $i: $direction, $type, address=0x${ep.address.toString(16)}")
                }

                // Recherche du endpoint de sortie - Version améliorée
                addLog("Recherche endpoint de sortie...")

                // D'abord chercher un endpoint BULK OUT (standard CDC)
                var outEndpoint = (0 until cdcInterface.endpointCount).map {
                    cdcInterface.getEndpoint(it)
                }.find {
                    it.direction == UsbConstants.USB_DIR_OUT &&
                    it.type == UsbConstants.USB_ENDPOINT_XFER_BULK
                }

                // Si pas trouvé, chercher un endpoint INTERRUPT OUT
                if (outEndpoint == null) {
                    addLog("Pas d'endpoint BULK OUT, recherche INTERRUPT OUT...")
                    outEndpoint = (0 until cdcInterface.endpointCount).map {
                        cdcInterface.getEndpoint(it)
                    }.find {
                        it.direction == UsbConstants.USB_DIR_OUT &&
                        it.type == UsbConstants.USB_ENDPOINT_XFER_INT
                    }
                }

                // Si toujours pas trouvé, chercher dans toutes les interfaces
                if (outEndpoint == null) {
                    addLog("Recherche endpoint de sortie dans toutes les interfaces...")
                    for (i in 0 until device.interfaceCount) {
                        val intf = device.getInterface(i)
                        for (j in 0 until intf.endpointCount) {
                            val ep = intf.getEndpoint(j)
                            if (ep.direction == UsbConstants.USB_DIR_OUT) {
                                addLog("Trouvé endpoint OUT dans interface $i: type=${when(ep.type) {
                                    UsbConstants.USB_ENDPOINT_XFER_BULK -> "BULK"
                                    UsbConstants.USB_ENDPOINT_XFER_INT -> "INTERRUPT"
                                    UsbConstants.USB_ENDPOINT_XFER_CONTROL -> "CONTROL"
                                    UsbConstants.USB_ENDPOINT_XFER_ISOC -> "ISOCHRONOUS"
                                    else -> "UNKNOWN"
                                }}")

                                // Si c'est une interface différente, on doit la revendiquer
                                if (i != cdcInterface.id) {
                                    addLog("Changement vers interface $i...")
                                    connection.releaseInterface(cdcInterface)
                                    val newInterface = device.getInterface(i)
                                    val claimResult2 = connection.claimInterface(newInterface, true)
                                    if (!claimResult2) {
                                        addLog("❌ Impossible de revendiquer l'interface $i")
                                        continue
                                    }
                                    addLog("✅ Interface $i revendiquée")
                                }
                                outEndpoint = ep
                                break
                            }
                        }
                        if (outEndpoint != null) break
                    }
                }

                if (outEndpoint == null) {
                    addLog("❌ Aucun endpoint de sortie trouvé")
                    connection.releaseInterface(cdcInterface)
                    connection.close()
                    return@withContext false
                }

                val endpointType = when(outEndpoint.type) {
                    UsbConstants.USB_ENDPOINT_XFER_BULK -> "BULK"
                    UsbConstants.USB_ENDPOINT_XFER_INT -> "INTERRUPT"
                    UsbConstants.USB_ENDPOINT_XFER_CONTROL -> "CONTROL"
                    UsbConstants.USB_ENDPOINT_XFER_ISOC -> "ISOCHRONOUS"
                    else -> "UNKNOWN"
                }
                addLog("✅ Endpoint de sortie trouvé: $endpointType, address=0x${outEndpoint.address.toString(16)}")

                val bytes = string.toByteArray()
                addLog("Envoi de ${bytes.size} bytes: ${bytes.joinToString(" ") { "0x%02x".format(it) }}")

                // Utiliser la méthode appropriée selon le type d'endpoint
                val result = if (outEndpoint.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                    connection.bulkTransfer(outEndpoint, bytes, bytes.size, 5000)
                } else {
                    // Pour les endpoints INTERRUPT, utiliser controlTransfer ou bulkTransfer selon le périphérique
                    addLog("Utilisation d'INTERRUPT endpoint...")
                    connection.bulkTransfer(outEndpoint, bytes, bytes.size, 5000)
                }

                val success = result >= 0
                if (success) {
                    addLog("✅ Données envoyées avec succès: $result bytes")
                } else {
                    addLog("❌ Erreur lors de l'envoi: code=$result")
                    when (result) {
                        -1 -> addLog("Erreur générale ou timeout")
                        -2 -> addLog("Erreur de type ou paramètre invalide")
                        else -> addLog("Code d'erreur inconnu: $result")
                    }
                }

                connection.releaseInterface(cdcInterface)
                connection.close()
                addLog("=== FIN ENVOI USB ===")
                return@withContext success
            } catch (e: Exception) {
                addLog("❌ Exception lors de l'envoi USB: ${e.message}")
                return@withContext false
            }
        }
    }

    private suspend fun requestPermission(device: UsbDevice): Boolean = suspendCancellableCoroutine { continuation ->
        val permissionIntent = PendingIntent.getBroadcast(
            context, 0, Intent(ACTION_USB_PERMISSION),
            PendingIntent.FLAG_IMMUTABLE
        )

        val usbReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                context.unregisterReceiver(this)
                if (ACTION_USB_PERMISSION == intent.action) {
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    continuation.resume(granted)
                }
            }
        }

        context.registerReceiver(
            usbReceiver,
            IntentFilter(ACTION_USB_PERMISSION),
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                Context.RECEIVER_NOT_EXPORTED
            } else {
                0
            }
        )
        usbManager.requestPermission(device, permissionIntent)

        continuation.invokeOnCancellation {
            try {
                context.unregisterReceiver(usbReceiver)
            } catch (_: Exception) {
                // Receiver already unregistered
            }
        }
    }

    private fun findCdcDevice(): UsbDevice? {
        val devices = usbManager.deviceList.values
        Log.d("UsbCdcManager", "=== RECHERCHE PÉRIPHÉRIQUE CDC ===")

        // D'abord, essayer de trouver un vrai périphérique CDC
        var cdcDevice = devices.find { device ->
            Log.d("UsbCdcManager", "Vérification périphérique: ${device.deviceName}")
            Log.d("UsbCdcManager", "  Vendor ID: 0x${device.vendorId.toString(16)}")
            Log.d("UsbCdcManager", "  Product ID: 0x${device.productId.toString(16)}")

            // Recherche d'interfaces CDC standard
            (0 until device.interfaceCount).any { i ->
                val intf = device.getInterface(i)
                val isCdc = intf.interfaceClass == UsbConstants.USB_CLASS_CDC_DATA ||
                           intf.interfaceClass == UsbConstants.USB_CLASS_COMM

                Log.d("UsbCdcManager", "  Interface $i: class=${intf.interfaceClass}, subclass=${intf.interfaceSubclass}, protocol=${intf.interfaceProtocol}")

                if (isCdc) {
                    Log.d("UsbCdcManager", "  ✅ Interface CDC trouvée!")
                }

                isCdc
            }
        }

        // Si aucun périphérique CDC standard trouvé, chercher des dispositifs vendor-specific
        if (cdcDevice == null) {
            Log.d("UsbCdcManager", "Aucun périphérique CDC standard trouvé, recherche vendor-specific...")

            cdcDevice = devices.find { device ->
                // Recherche d'interfaces vendor-specific (class 255) qui pourraient être des CDC
                (0 until device.interfaceCount).any { i ->
                    val intf = device.getInterface(i)
                    val isVendorSpecific = intf.interfaceClass == 255 // Vendor specific

                    if (isVendorSpecific) {
                        Log.d("UsbCdcManager", "  Trouvé interface vendor-specific: ${device.deviceName}")

                        // Vérifier s'il y a des endpoints BULK IN/OUT (caractéristique des CDC)
                        val hasBulkEndpoints = (0 until intf.endpointCount).any { j ->
                            val ep = intf.getEndpoint(j)
                            ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK
                        }

                        if (hasBulkEndpoints) {
                            Log.d("UsbCdcManager", "  ✅ Interface vendor-specific avec endpoints BULK!")
                        }

                        hasBulkEndpoints
                    } else {
                        false
                    }
                }
            }
        }

        // Si toujours rien, prendre le premier périphérique avec des endpoints BULK
        if (cdcDevice == null && devices.isNotEmpty()) {
            Log.d("UsbCdcManager", "Recherche de tout périphérique avec endpoints BULK...")

            cdcDevice = devices.find { device ->
                (0 until device.interfaceCount).any { i ->
                    val intf = device.getInterface(i)
                    (0 until intf.endpointCount).any { j ->
                        val ep = intf.getEndpoint(j)
                        ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK
                    }
                }
            }

            if (cdcDevice != null) {
                Log.d("UsbCdcManager", "✅ Périphérique avec endpoints BULK trouvé: ${cdcDevice.deviceName}")
            }
        }

        return cdcDevice
    }

    private fun findCdcInterface(device: UsbDevice): android.hardware.usb.UsbInterface? {
        Log.d("UsbCdcManager", "=== RECHERCHE INTERFACE CDC ===")

        // D'abord, chercher une interface CDC standard
        var cdcInterface = (0 until device.interfaceCount).map { device.getInterface(it) }.find { intf ->
            val isCdcStandard = intf.interfaceClass == UsbConstants.USB_CLASS_CDC_DATA ||
                               (intf.interfaceClass == UsbConstants.USB_CLASS_COMM && intf.interfaceSubclass == 2)

            if (isCdcStandard) {
                Log.d("UsbCdcManager", "✅ Interface CDC standard trouvée: class=${intf.interfaceClass}")
            }

            isCdcStandard
        }

        // Si pas trouvé, chercher une interface vendor-specific avec endpoints BULK
        if (cdcInterface == null) {
            Log.d("UsbCdcManager", "Recherche interface vendor-specific avec endpoints BULK...")

            cdcInterface = (0 until device.interfaceCount).map { device.getInterface(it) }.find { intf ->
                val hasBulkEndpoints = (0 until intf.endpointCount).any { i ->
                    val ep = intf.getEndpoint(i)
                    ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK
                }

                if (hasBulkEndpoints) {
                    Log.d("UsbCdcManager", "✅ Interface avec endpoints BULK trouvée: class=${intf.interfaceClass}")
                }

                hasBulkEndpoints
            }
        }

        // Si toujours pas trouvé, prendre la première interface
        if (cdcInterface == null && device.interfaceCount > 0) {
            Log.d("UsbCdcManager", "Utilisation de la première interface disponible...")
            cdcInterface = device.getInterface(0)
        }

        return cdcInterface
    }

    fun listConnectedDevices(): List<UsbDevice> {
        return usbManager.deviceList.values.toList()
    }
}
