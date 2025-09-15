package implementation

import interpreter.ErrorHandler
import interpreter.PrintScriptLinter
import org.printscript.analyzer.Diagnostic
import org.printscript.analyzer.config.AnalyzerConfig
import org.printscript.common.Failure
import org.printscript.common.Position
import org.printscript.common.Result
import org.printscript.common.Span
import org.printscript.common.Success
import org.printscript.common.Version
import org.printscript.runner.ProgramIo
import org.printscript.runner.RunnerError
import org.printscript.runner.ValidationReport
import org.printscript.runner.runners.ValidateRunnerWithConfig
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class RunnerLinterAdapter: PrintScriptLinter {
    /**
     * executes a PrintScript file handling its resulting messages and errors.
     * @param src Source file.
     * @param version PrintScript version, 1.0 and 1.1 must be supported.
     * @param config config file.
     * @param handler interface where all syntax and semantic error will be reported.
     */
    override fun lint(src: InputStream, version: String, config: InputStream, handler: ErrorHandler) {
        val v: Version = VersionMapper.fromTck(version)
        val reader = InputStreamReader(src, StandardCharsets.UTF_8)
        val io = ProgramIo(reader = reader)

        val analyzerCfg: AnalyzerConfig = AnalyzerConfigLoaderFromStream.fromStream(config)

        val r: Result<ValidationReport, RunnerError> =
            ValidateRunnerWithConfig(analyzerCfg).run(v, io)

        when (r) {
            is Success -> {
                r.value.diagnostics.forEach { d -> handler.reportError(formatDiagnostic(d)) }

            }
            is Failure -> {
                val e = r.error
                handler.reportError("analyzer: ${e.message}")
            }
        }
    }

    private fun formatDiagnostic(d: Diagnostic): String {
        val sev = d.severity.name
        val rule = d.ruleId
        val msg  = d.message
        val loc  = spanToString(d.span)
        return "[$sev][$rule] $msg @ $loc"
    }

    private fun spanToString(span: Span?): String {
        if (span == null) return "(?:?:?-?:?)"
        val a: Position? = span.start
        val e: Position? = span.end
        val aStr = pos(a)
        val eStr = pos(e)
        return "$aStr-$eStr"
    }

    private fun pos(p: Position?): String =
        if (p == null) "L?:C?" else "L${p.line}:C${p.column}"
}