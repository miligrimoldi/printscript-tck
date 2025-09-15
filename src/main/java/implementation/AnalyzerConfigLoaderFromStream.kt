package implementation

import interpreter.ErrorHandler
import java.io.InputStream
import org.printscript.analyzer.config.AnalyzerConfig
import org.printscript.analyzer.config.IdentifiersConfig
import org.printscript.analyzer.loader.AnalyzerConfigLoader
import org.printscript.analyzer.loader.ConfigFormat
import org.printscript.analyzer.rules.IdentifierStyle
import org.printscript.common.Failure
import org.printscript.common.Success

object AnalyzerConfigLoaderFromStream {
    fun fromStream(config: InputStream?, handler: ErrorHandler? = null): AnalyzerConfig {
        try {
            if (config == null || config.available() == 0) return AnalyzerConfig()

            val bytes = config.readAllBytes()
            if (bytes.isEmpty()) return AnalyzerConfig()

            val txtLower = bytes.toString(Charsets.UTF_8).trim().lowercase()

            if ("identifier_format" in txtLower) {
                val snake = "snake case" in txtLower
                val mandatory = "\"mandatory\"" in txtLower && "true" in txtLower

                val style = if (snake) IdentifierStyle.SNAKE_CASE else IdentifierStyle.CAMEL_CASE
                val defaults = AnalyzerConfig()

                val ids = IdentifiersConfig(
                    /* enabled = */ true,
                    /* style   = */ style,
                    /* checkRefs = */ defaults.identifiers.checkReferences,
                    /* failOnViolation */ mandatory || defaults.identifiers.failOnViolation
                )

                return AnalyzerConfig(
                    ids,
                    defaults.printlnRule,
                    defaults.readInputRule
                )
            }

            val tmp = java.nio.file.Files.createTempFile("ps-analyzer-config", ".cfg").toFile()
            tmp.writeBytes(bytes)

            val rJson = AnalyzerConfigLoader.fromFile(tmp, ConfigFormat.JSON)
            if (rJson is Success) {
                tmp.delete()
                return rJson.getOrNull()
            }

            val rYaml = AnalyzerConfigLoader.fromFile(tmp, ConfigFormat.YAML)
            tmp.delete()
            if (rYaml is Success) {
                return rYaml.getOrNull()
            }

            if (rYaml is Failure) {
                val err = (rYaml.error)?.toString() ?: "unknown"
                handler?.reportError("config: $err")
            } else if (rJson is Failure) {
                val err = (rJson.error)?.toString() ?: "unknown"
                handler?.reportError("config: $err")
            }

            return AnalyzerConfig()
        } catch (ioe: java.io.IOException) {
            handler?.reportError("config: ${ioe.message}")
            return AnalyzerConfig()
        } catch (_: Throwable) {
            return AnalyzerConfig()
        }
    }
}