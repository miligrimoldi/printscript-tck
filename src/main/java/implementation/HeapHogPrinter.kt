package implementation

import interpreter.PrintEmitter

class HeapHogPrinter(
    private val delegate: PrintEmitter,
    private val blockBytes: Int = 8 * 1024 * 1024,
    private val everyNPrints: Int = 2048
) {
    private val bag = ArrayList<ByteArray>()
    private var count = 0

    fun accept(msg: String) {
        delegate.print(msg)
        count++
        if (count % everyNPrints == 0) {
            bag.add(ByteArray(blockBytes))
        }
    }
}