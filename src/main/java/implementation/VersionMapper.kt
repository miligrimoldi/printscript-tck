package implementation

import org.printscript.common.Version
object VersionMapper {
    fun fromTck(raw: String?): Version =
        when (raw?.trim()) {
            null, "", "1.0", "v1.0" -> Version.V0
            "1.1", "v1.1"           -> Version.V1
            else -> error("version desconocida: $raw (use 1.0 o 1.1)")
        }
}