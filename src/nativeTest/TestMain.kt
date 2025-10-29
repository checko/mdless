import kotlin.system.exitProcess

// Import test functions from individual test files
// For now, just run the smoke test - we can expand this later
import MainSmokeKt.main as smokeMain

fun main() {
    println("Running tests...")

    try {
        // Run smoke test
        println("Running smoke test...")
        smokeMain()

        // TODO: Add other tests here
        // We would need to refactor the individual test files to export their test functions
        // rather than having main() functions

        println("All tests passed!")
        exitProcess(0)
    } catch (e: Exception) {
        println("Test failed: ${e.message}")
        exitProcess(1)
    }
}