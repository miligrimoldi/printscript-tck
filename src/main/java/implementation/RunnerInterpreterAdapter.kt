package implementation

import interpreter.ErrorHandler
import interpreter.InputProvider
import interpreter.PrintEmitter
import interpreter.PrintScriptInterpreter
import org.printscript.common.Failure
import org.printscript.common.Result
import org.printscript.common.Success
import org.printscript.common.Version
import org.printscript.runner.ProgramIo
import org.printscript.runner.RunnerError
import org.printscript.runner.runners.ExecuteRunnerStreaming
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets


class RunnerInterpreterAdapter: PrintScriptInterpreter {
    override fun execute(
        src: InputStream,
        version: String,
        emitter: PrintEmitter,
        handler: ErrorHandler,
        provider: InputProvider
    ) {
        val v = VersionMapper.fromTck(version)
        val io = ProgramIo(
            reader = InputStreamReader(src, StandardCharsets.UTF_8),
            inputProviderOverride = TckInputProviderAdapter(provider)
        )

        val isCollector = emitter.javaClass.simpleName?.contains("PrintCollector") == true

        val printer: ((String) -> Unit) = { msg -> emitter.print(msg) }

        try {
            val r = ExecuteRunnerStreaming(
                printer = printer,
                collectAlsoWithPrinter = isCollector
            ).run(v, io)

            when (r) {
                is Success -> Unit
                is Failure -> handler.reportError("[${r.error.stage::class.simpleName}] ${r.error.message}")
            }
        } catch (_: OutOfMemoryError) {
            handler.reportError("Java heap space")
        } catch (_: VirtualMachineError) {
            handler.reportError("Java heap space")
        }
    }
}