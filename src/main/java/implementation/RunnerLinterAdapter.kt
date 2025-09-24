package implementation

import interpreter.ErrorHandler
import interpreter.PrintScriptLinter
import org.printscript.analyzer.Severity
import org.printscript.common.Failure
import org.printscript.common.Success
import org.printscript.runner.ProgramIo
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import org.printscript.runner.helpers.DiagnosticStringFormatter
import org.printscript.runner.helpers.VersionMapper
import org.printscript.runner.runners.Runner

class RunnerLinterAdapter : PrintScriptLinter {

    private val maxErrors = 2_000
    private val maxTotal  = 100_000

    override fun lint(src: InputStream, version: String, config: InputStream, handler: ErrorHandler) {
        val v  = VersionMapper.parse(version)
        val io = ProgramIo(reader = InputStreamReader(src, StandardCharsets.UTF_8))

        try {
            val res = Runner.analyzeWithConfigStream(v, io, config) { msg ->
                handler.reportError(msg)
            }

            when (res) {
                is Success -> {
                    var errors = 0
                    var total  = 0
                    for (d in res.value) {
                        total++
                        if (d.severity == Severity.ERROR) errors++
                        if (errors < maxErrors && total < maxTotal) {
                            handler.reportError(DiagnosticStringFormatter.format(d))
                        } else {
                            handler.reportError("Too many diagnostics: errors=$errors total=$total")
                            break
                        }
                    }
                }
                is Failure -> handler.reportError("[${res.error.stage::class.simpleName}] ${res.error.message}")
            }
        } finally {
            try { src.close() } catch (_: Exception) {}
            try { config.close() } catch (_: Exception) {}
        }
    }
}