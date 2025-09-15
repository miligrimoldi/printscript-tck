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

class RunnerFormatterAdapter: PrintScriptFormatter {
    /**
     * executes a PrintScript file handling its resulting messages and errors.
     * @param src Source file.
     * @param version PrintScript version, 1.0 and 1.1 must be supported.
     * @param config config file (JSON).
     * @param writer Writer, where the formatted output should be written
     */
    override fun format(src: InputStream, version: String, config: InputStream, writer: Writer) {
        val v: Version = VersionMapper.fromTck(version)
        val reader = InputStreamReader(src, StandardCharsets.UTF_8)
        val io = ProgramIo(reader = reader)

        val options = FormatterOptionsLoader.fromStream(config)

        val r: Result<String, RunnerError> = FormatRunnerWithOptions(options).run(v, io)
        when (r) {
            is Success -> {
                try {
                    writer.write(r.value)
                    writer.flush()
                } catch (_: java.io.IOException) {
                }
            }
            is Failure -> {
                val e = r.error
                throw RuntimeException("Formatter failed at ${e.stage::class.simpleName}: ${e.message}")
            }
        }
    }
}