package pwf.xenova.tvremote

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Resultado de revisar si hay una actualización disponible.
 */
sealed class UpdateCheckResult {
    data class UpdateAvailable(val buildNumber: Int, val downloadUrl: String) : UpdateCheckResult()
    object UpToDate : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

/**
 * Revisa el último release publicado en GitHub y lo compara con el número de
 * build actual de la app (BuildConfig.BUILD_NUMBER, que el CI setea automáticamente
 * en cada compilación exitosa). No requiere backend propio ni autenticación:
 * usa la API pública de GitHub para el repo.
 */
object UpdateChecker {

    private const val RELEASES_URL =
        "https://api.github.com/repos/Santyxs/RemoteControlTV/releases/latest"

    /** Llamar SIEMPRE desde un hilo de fondo (Dispatchers.IO), nunca en el hilo principal. */
    fun checkForUpdate(currentBuildNumber: Int): UpdateCheckResult {
        return try {
            val connection = URL(RELEASES_URL).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            if (connection.responseCode != 200) {
                return UpdateCheckResult.Error("No se pudo consultar GitHub (código ${connection.responseCode})")
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)

            val tagName = json.getString("tag_name") // ej. "42"
            val latestBuildNumber = tagName.removePrefix("v").toIntOrNull()
                ?: return UpdateCheckResult.Error("Formato de versión inesperado: $tagName")

            if (latestBuildNumber <= currentBuildNumber) {
                return UpdateCheckResult.UpToDate
            }

            val assets = json.getJSONArray("assets")
            if (assets.length() == 0) {
                return UpdateCheckResult.Error("El release no tiene APK adjunto")
            }
            val downloadUrl = assets.getJSONObject(0).getString("browser_download_url")

            UpdateCheckResult.UpdateAvailable(latestBuildNumber, downloadUrl)
        } catch (e: Exception) {
            UpdateCheckResult.Error(e.message ?: "Error de red")
        }
    }
}
