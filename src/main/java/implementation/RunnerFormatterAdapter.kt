package implementation

import interpreter.PrintScriptFormatter
import org.printscript.common.Failure
import org.printscript.common.Result
import org.printscript.common.Success
import org.printscript.common.Version
import org.printscript.runner.ProgramIo
import org.printscript.runner.RunnerError
import org.printscript.runner.runners.FormatRunnerWithOptions
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Writer
import java.nio.charset.StandardCharsets

class RunnerFormatterAdapter : PrintScriptFormatter {

    /**
     * Formatea un programa PrintScript y escribe el resultado en [writer].
     * - [src]: código fuente como InputStream (se cierra acá).
     * - [version]: "1.0" | "1.1" (mapeado con VersionMapper).
     * - [config]: JSON de configuración del formatter (puede ser null; no se cierra acá).
     * - [writer]: destino (NO se cierra, solo se escribe/flush).
     */
    override fun format(src: InputStream, version: String, config: InputStream?, writer: Writer) {
        val v: Version = VersionMapper.fromTck(version)

        InputStreamReader(src, StandardCharsets.UTF_8).use { reader ->
            val io = ProgramIo(reader = reader)

            val options = FormatterOptionsLoader.fromStream(config)

            val result: Result<String, RunnerError> =
                FormatRunnerWithOptions(options).run(v, io)

            when (result) {
                is Success -> {
                    writer.write(result.value)
                    writer.flush()
                }
                is Failure -> {
                    val e = result.error
                    val msg = "Formatter failed at ${e.stage}: ${e.message}"
                    throw RuntimeException(msg, e.cause as? Throwable)
                }
            }
        }
    }
}