package pwf.xenova.tvremote

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progress: Int) : DownloadState()
    data class Done(val file: File) : DownloadState()
    data class Failed(val message: String) : DownloadState()
}

/**
 * Descarga el APK directamente desde la app (sin pasar por el navegador, evitando
 * así el aviso de "archivo dañino" de Chrome) y abre el instalador del sistema.
 */
object ApkDownloader {

    /** Llamar desde un hilo de fondo (Dispatchers.IO). Reporta progreso via [onState]. */
    fun downloadApk(context: Context, url: String, onState: (DownloadState) -> Unit) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = true
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                onState(DownloadState.Failed("Error de descarga (código ${connection.responseCode})"))
                return
            }

            val totalSize = connection.contentLength
            val outFile = File(context.cacheDir, "update.apk")

            connection.inputStream.use { input ->
                outFile.outputStream().use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    var totalRead = 0L
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (totalSize > 0) {
                            onState(DownloadState.Downloading((totalRead * 100 / totalSize).toInt()))
                        }
                    }
                }
            }

            onState(DownloadState.Done(outFile))
        } catch (e: Exception) {
            onState(DownloadState.Failed(e.message ?: "Error de red"))
        }
    }

    /** Abre el instalador del sistema para el APK ya descargado. */
    fun installApk(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
