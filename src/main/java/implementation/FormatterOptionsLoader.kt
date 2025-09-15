package implementation

import org.printscript.formatter.config.ExternalFormatterConfigLoader
import org.printscript.formatter.config.FormatterConfig
import org.printscript.formatter.config.FormatterOptions
import java.io.InputStream

object FormatterOptionsLoader {
    fun fromStream(config: InputStream?): FormatterOptions {
        return try {
            if (config == null) return FormatterConfig()
            val bytes = config.readAllBytes()
            if (bytes.isEmpty()) FormatterConfig() else ExternalFormatterConfigLoader.load(bytes)
        } catch (_: Throwable) { FormatterConfig() }
    }
}