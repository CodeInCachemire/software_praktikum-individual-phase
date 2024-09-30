package de.unisaarland.cs.se.selab.parser

/**
 * This is a custom exception used in the parser to encapsulate
 * error messages in the result type.
 */
class ParserException(private val msg: String) : Exception() {
    override val message: String
        get() = "\uD83D\uDDFF Parser Exception >:( \n $msg"
}
