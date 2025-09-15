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

class RunnerInterpreterAdapter : PrintScriptInterpreter {

    /**
     * executes a PrintScript file handling its resulting messages and errors.
     * @param src Source file.
     * @param version PrintScript version, 1.0 and 1.1 must be supported.
     * @param emitter interface where print statements must be called.
     * @param handler interface where all syntax and semantic error will be reported.
     * @param provider interface that provides input values during the execution.
     */
    override fun execute(
        src: InputStream,
        version: String,
        emitter: PrintEmitter,
        handler: ErrorHandler,
        provider: InputProvider
    ) {
        val v: Version = VersionMapper.fromTck(version)
        val reader = InputStreamReader(src, StandardCharsets.UTF_8)
        val io = ProgramIo(reader = reader, inputProviderOverride = TckInputProviderAdapter(provider))

        val isCollector = emitter.javaClass.simpleName.contains("PrintCollector")


        val printerFn: (String) -> Unit = { msg -> emitter.print(msg) }

        try {
            val r: Result<Unit, RunnerError> = ExecuteRunnerStreaming(printerFn).run(v, io)
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