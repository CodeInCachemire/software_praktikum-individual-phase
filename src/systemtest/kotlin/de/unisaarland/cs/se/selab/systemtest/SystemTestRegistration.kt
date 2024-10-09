package de.unisaarland.cs.se.selab.systemtest

import de.unisaarland.cs.se.selab.systemtest.runner.SystemTestManager

/**system test**/
object SystemTestRegistration {
    /**
     * Register your tests to run against the reference implementation!
     * This can also be used to debug our system test, or to see if we
     * understood something correctly or not (everything should work
     * the same as their reference implementation)
     * Should be exclusive with the other two methods!
     */
    fun registerSystemTestsReferenceImpl(manager: SystemTestManager) {
        // manager.registerTest(TestCorrectOrderofUnloadingDiffStuff())
        manager.registerTest(TestPurchaseFull())
        manager.registerTest(TestTyphoonEvent())
        manager.registerTest(RepairTest())
        manager.registerTest(Test3Stations())
        manager.registerTest(SimpleUnloadTest())
    }

    /**
     * Register the tests you want to run against the validation mutants here!
     * The test only check validation, so they log messages will only possibly
     * be incorrect during the parsing/validation.
     * Everything after 'Simulation start' works correctly.
     * Should be exclusive with the other two methods!
     */
    fun registerSystemTestsMutantValidation(manager: SystemTestManager) {
        manager.registerTest(ExampleTest())
    }

    /**
     * The same as above, but the log message only (possibly) become incorrect
     * from the 'Simulation start' log onwards
     * Should be exclusive with the other two methods!
     */
    fun registerSystemTestsMutantSimulation(manager: SystemTestManager) {
        manager.registerTest(TestTyphoonEvent())
        manager.registerTest(TestsPurchase())
        manager.registerTest(RepairTest())
        manager.registerTest(Test3Stations())
        manager.registerTest(SimpleUnloadTest())
    }
}
