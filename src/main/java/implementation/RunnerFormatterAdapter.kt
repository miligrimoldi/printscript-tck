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
import org.printscript.runner.helpers.FormatterOptionsLoader
import org.printscript.runner.helpers.VersionMapper
import org.printscript.token.TokenStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Writer
import java.nio.charset.StandardCharsets

class RunnerFormatterAdapter : PrintScriptFormatter {

    override fun format(src: InputStream, version: String, config: InputStream?, writer: Writer) {
        val v = VersionMapper.parse(version)

        val code: String = try {
            src.readAllBytes().toString(StandardCharsets.UTF_8)
        } catch (_: Exception) {
            writer.write("")
            writer.flush()
            return
        } finally {
            try { src.close() } catch (_: Exception) {}
        }

        val lexerFactory = LexerFactory()
        val tokens: TokenStream = lexerFactory.tokenStream(v, code, /*normalizeTrivia=*/true)

        val options: FormatterOptions = FormatterOptionsLoader.fromStream(config)

        val formatter = GlobalFormatterFactory.forVersion(v, options)
        if (formatter == null) {
            writer.write(code)
            writer.flush()
            return
        }

        val result: Result<Unit, *> = formatter.format(tokens, writer)

        if (result is Success<Unit>) {
            writer.flush()
        } else {
            writer.write(code)
            writer.flush()
        }
    }
}