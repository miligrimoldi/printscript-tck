package implementation

import interpreter.PrintScriptFormatter
import org.printscript.common.Failure
import org.printscript.common.Result
import org.printscript.common.Success
import org.printscript.common.Version
import org.printscript.formatter.config.FormatterOptions
import org.printscript.formatter.factories.GlobalFormatterFactory
import org.printscript.lexer.config.LexerFactory
import org.printscript.runner.ProgramIo
import org.printscript.runner.RunnerError
import org.printscript.runner.runners.FormatRunnerWithOptions
import org.printscript.token.TokenStream
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
        // 1) Parsear versión del TCK ("1.0" | "1.1") a enum interno
        val v: Version = VersionMapper.fromTck(version)

        // 2) Leer el source completo como String (requerido por tu LexerFactory API)
        val code: String = try {
            src.readAllBytes().toString(StandardCharsets.UTF_8)
        } catch (_: Exception) {
            // Si no puedo leer el source, no rompo: escribo vacío y salgo
            writer.write("")
            writer.flush()
            return
        } finally {
            // Cerramos el src acá como pedías
            try { src.close() } catch (_: Exception) {}
        }

        // 3) Construir TokenStream directo desde tu LexerFactory (¡línea clave!)
        val lexerFactory = LexerFactory()
        val tokens: TokenStream = lexerFactory.tokenStream(v, code, /*normalizeTrivia=*/true)

        // 4) Cargar opciones desde el InputStream de config (si viene nulo, defaults)
        val options: FormatterOptions = FormatterOptionsLoader.fromStream(config)

        // 5) Obtener el formatter para la versión + opciones
        val formatter = GlobalFormatterFactory.forVersion(v, options)
        if (formatter == null) {
            // Si no hay formatter para esa versión, devolvés el código original
            writer.write(code)
            writer.flush()
            return
        }

        // 6) Formatear DIRECTO al Writer (Writer es Appendable) → cero String intermedio
        val result: Result<Unit, *> = formatter.format(tokens, writer)

        // 7) Si salió bien, flush. Si falló, fallback: escribir el código original
        if (result is Success<Unit>) {
            writer.flush()
        } else {
            writer.write(code)
            writer.flush()
        }
    }
}