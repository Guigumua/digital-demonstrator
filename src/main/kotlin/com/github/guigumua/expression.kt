package com.github.guigumua

import com.github.guigumua.Op.*
import java.util.TreeMap
import kotlin.math.pow

internal fun expressionBacktrace(
    numbers: IntArray,
    numberIndex: Int,
    n: Int,
    stack: ArrayDeque<Expression>,
    index: Int,
    results: TreeMap<Int, Expression>
) {
    if (index == n) {
        val expression = stack.last()
        when {
            expression.value == 0 -> zeroExpression = expression
            expression.value < 0 -> return
            else -> {
                results.compute(expression.value) { _, v ->
                    if (v == null || v.toString().length > expression.toString().length) {
                        expression
                    } else {
                        v
                    }
                }
            }
        }
        return
    }
    if (numberIndex < numbers.size) {
        stack.add(Literal(numbers[numberIndex]))
        expressionBacktrace(numbers, numberIndex + 1, n, stack, index + 1, results)
        stack.removeLast()

        stack.add(Literal(-numbers[numberIndex]))
        expressionBacktrace(numbers, numberIndex + 1, n, stack, index + 1, results)
        stack.removeLast()
    }
    if (stack.size >= 2) {
        val right = stack.removeLast()
        val left = stack.removeLast()

        if (right !is Literal || right.value >= 0) {
            stack.add(BinaryExpression(left, ADD, right))
            expressionBacktrace(numbers, numberIndex, n, stack, index + 1, results)
            stack.removeLast()
        }

        if (right !is Literal || right.value >= 0) {
            stack.add(BinaryExpression(left, SUB, right))
            expressionBacktrace(numbers, numberIndex, n, stack, index + 1, results)
            stack.removeLast()
        }

        if (left.value >= 0 || right.value >= 0) {
            stack.add(BinaryExpression(left, MUL, right))
            expressionBacktrace(numbers, numberIndex, n, stack, index + 1, results)
            stack.removeLast()
        }

        if (right.value != 0 && left.value % right.value == 0) {
            stack.add(BinaryExpression(left, DIV, right))
            expressionBacktrace(numbers, numberIndex, n, stack, index + 1, results)
            stack.removeLast()
        }

        if (
            right.value >= 0
            && (31 - left.value.countLeadingZeroBits() * right.value) <= 31
            && (31 - left.value.countLeadingZeroBits() * right.value) >= 0
            && (left.value >= 0 || right.value % 2 != 0)
        ) {
            stack.add(BinaryExpression(left, EXP, right))
            expressionBacktrace(numbers, numberIndex, n, stack, index + 1, results)
            stack.removeLast()
        }

        stack.add(left)
        stack.add(right)
    }
}

interface Expression {
    val precedence: Int
    val value: Int
}

internal class Literal(override val value: Int) : Expression {
    override fun toString(): String {
        return value.toString()
    }

    override val precedence: Int
        get() = Int.MAX_VALUE
}

internal enum class Op {
    ADD, SUB, MUL, DIV, EXP;

    override fun toString(): String {
        return when (this) {
            ADD -> "+"
            SUB -> "-"
            MUL -> "*"
            DIV -> "/"
            EXP -> "^"
        }

    }

}

internal class BinaryExpression(val left: Expression, val op: Op, val right: Expression) : Expression {
    override val value: Int by lazy(LazyThreadSafetyMode.NONE) { eval() }

    private fun eval(): Int {
        return when (op) {
            ADD -> left.value + right.value

            SUB -> left.value - right.value

            MUL -> left.value * right.value

            DIV -> left.value / right.value

            EXP -> left.value.toDouble().pow(right.value.toDouble()).toInt()
        }
    }

    private val _string: String by lazy(LazyThreadSafetyMode.NONE) {
        val sb = StringBuilder()
        if (left.precedence < precedence || (left.value < 0 && op == EXP)) {
            sb.append("($left)")
        } else {
            sb.append(left)
        }
        sb.append(op)
        when (op) {
            ADD -> sb.append(right)
            SUB -> if (right.precedence <= precedence) {
                sb.append("($right)")
            } else {
                sb.append(right)
            }

            MUL -> if (right.precedence < precedence) {
                sb.append("($right)")
            } else {
                sb.append(right)
            }

            DIV -> if (right.precedence <= precedence) {
                sb.append("($right)")
            } else {
                sb.append(right)
            }

            EXP -> if (right.precedence < precedence) {
                sb.append("($right)")
            } else {
                sb.append(right)
            }
        }
        sb.toString()
    }

    override fun toString(): String {
        return _string
    }

    override val precedence: Int
        get() = when (op) {
            ADD, SUB -> 1
            MUL, DIV -> 2
            EXP -> 3
        }
}

internal class UnaryExpression(val op: Op, val expression: Expression) : Expression {
    override val value: Int by lazy(LazyThreadSafetyMode.NONE) { eval() }

    private fun eval(): Int {
        return when (op) {
            ADD -> expression.value
            SUB -> -expression.value
            else -> throw Exception("Invalid unary operator")
        }
    }

    override fun toString(): String {
        return "$op($expression)"
    }

    override val precedence: Int
        get() = Int.MAX_VALUE
}
