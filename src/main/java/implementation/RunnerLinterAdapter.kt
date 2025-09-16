package implementation

import interpreter.ErrorHandler
import interpreter.PrintScriptLinter
import org.printscript.analyzer.Diagnostic
import org.printscript.analyzer.DiagnosticEmitter
import org.printscript.analyzer.Severity
import org.printscript.analyzer.config.AnalyzerConfig
import org.printscript.common.Failure
import org.printscript.common.Position
import org.printscript.common.Span
import org.printscript.common.Success
import org.printscript.common.Version
import org.printscript.runner.LanguageWiringFactory
import org.printscript.runner.ProgramIo
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import org.printscript.analyzer.TooManyDiagnosticsException

class RunnerLinterAdapter : PrintScriptLinter {

    // Emitter que reenvía al handler con límite y corta con TooManyDiagnostics.
    private class CappedForwardingEmitter(
        private val handler: ErrorHandler,
        private val maxErrors: Int = 2_000,
        private val maxTotal: Int = 100_000,
    ) : DiagnosticEmitter {
        private var errors = 0
        private var total  = 0

        override fun report(diagnostic: Diagnostic) {
            total++
            if (diagnostic.severity == Severity.ERROR) errors++

            if (errors < maxErrors && total < maxTotal) {
                handler.reportError(formatDiagnostic(diagnostic))
            } else {
                throw TooManyDiagnosticsException(maxErrors, total)
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
            return pos(a) + "-" + pos(e)
        }
        private fun pos(p: Position?): String = if (p == null) "L?:C?" else "L${p.line}:C${p.column}"
    }

    override fun lint(src: InputStream, version: String, config: InputStream, handler: ErrorHandler) {
        val v: Version = VersionMapper.fromTck(version)
        val io = ProgramIo(reader = InputStreamReader(src, StandardCharsets.UTF_8)) // Lo pasa a reader.
        val analyzerCfg: AnalyzerConfig = AnalyzerConfigLoaderFromStream.fromStream(config)

        val w  = LanguageWiringFactory.forVersion(v)
        val ts = w.tokenStreamFromReader(io.reader!!)
        val stmts = w.parser.parse(ts)

        val bridge = CappedForwardingEmitter(handler, maxErrors = 2_000, maxTotal = 100_000)

        when (val ar = w.analyzer.analyze(stmts, analyzerCfg, bridge)) {
            is Success -> { /* emitido en streaming */ }
            is Failure -> handler.reportError("analyzer: ${ar.error.message}")
        }
    }
}