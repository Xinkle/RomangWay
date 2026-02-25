package fflog

import kotlinx.serialization.json.Json

object FFlogJson {
    val parser = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
}
