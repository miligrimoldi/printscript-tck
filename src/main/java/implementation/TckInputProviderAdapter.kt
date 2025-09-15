package implementation

import interpreter.InputProvider as TckInputProvider
import org.printscript.interpreter.InputProvider as InternalInputProvider

class TckInputProviderAdapter(private val tck: TckInputProvider) : InternalInputProvider {
    override fun read(prompt: String): String = tck.input(prompt)
}