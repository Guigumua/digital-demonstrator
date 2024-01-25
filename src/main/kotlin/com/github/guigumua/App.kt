package com.github.guigumua

import java.math.BigInteger
import java.util.*
import kotlin.collections.ArrayDeque
import kotlin.math.pow


val table = TreeMap<Int, Expression>()
var zeroExpression: Expression? = null
fun main() {
    println("输入base")
    print("> ")
    val base = readln()
    runCatching {
        base.toInt()
    }.getOrDefault(0).let {
        if (it <= 0 || it > 1e7) {
            println("base必须是小于${1e7}的正整数")
            return
        }
    }
    for (i in base.indices) {
        backtrace(base, base.length, 0, 0, IntArray(i + 1))
    }
    if (!base.startsWith('0')) {
        buildTable(intArrayOf(base.toInt(), base.toInt()))
    }
    while (!Thread.interrupted()) {
        println("输入数字")
        print("> ")
        val num = readln().toBigInteger()
        runCatching {
            val expression =
                when {
                    num == BigInteger.ZERO -> zeroExpression ?: BinaryExpression(
                        table.firstEntry().value,
                        Op.SUB,
                        table.firstEntry().value
                    )

                    num > BigInteger.ZERO -> demolish(num)
                    else -> UnaryExpression(Op.SUB, demolish(num.negate()))
                }
            println("${eval(expression)} = $expression")
        }.onFailure {
            println("No solution")
        }
    }
}


internal fun backtrace(s: String, n: Int, start: Int, index: Int, path: IntArray) {
    if (index == path.size - 1) {
        if (s[start].code == '0'.code && (s.length - start) > 1) {
            return
        }
        path[index] = s.subNumber(start, s.length - 1)
        buildTable(path)
        return
    }
    for (i in start..<s.length - 1) {
        if (s[start].code == '0'.code && (i - start) > 0) {
            return
        }
        path[index] = s.subNumber(start, i)
        backtrace(s, n - i, i + 1, index + 1, path)
    }
}

fun String.subNumber(start: Int, end: Int): Int {
    var result = 0
    for (i in start..end) {
        result = result * 10 + this[i].code - 48
    }

    return result
}

internal fun buildTable(numbers: IntArray) {
    expressionBacktrace(numbers, 0, numbers.size * 2 - 1, ArrayDeque(numbers.size), 0, table)
}

val MAX: BigInteger = BigInteger.valueOf(Int.MAX_VALUE.toLong())
internal fun demolish(num: BigInteger): Expression {
    if (num > MAX) {
        val floorEntry = table.floorEntry(Int.MAX_VALUE) ?: throw Exception("No solution")
        val key = BigInteger.valueOf(floorEntry.key.toLong())
        val times = num / key
        val remainder = num % key
        var expression: Expression = floorEntry.value
        if (times > BigInteger.ONE) {
            expression = BinaryExpression(expression, Op.MUL, demolish(times))
        }
        if (remainder != BigInteger.ZERO) {
            expression = if (times > BigInteger.ZERO) {
                BinaryExpression(expression, Op.ADD, demolish(remainder))
            } else {
                demolish(remainder)
            }
        }
        return expression
    } else {
        return demolish(num.toInt())
    }
}

internal fun demolish(num: Int): Expression {
    val floorEntry = table.floorEntry(num) ?: throw Exception("No solution")
    val times = num / floorEntry.key
    val remainder = num % floorEntry.key
    var expression: Expression = floorEntry.value
    if (times > 1) {
        expression = BinaryExpression(expression, Op.MUL, demolishRemainder(times))
    }
    if (remainder != 0) {
        expression = if (times > 0) {
            BinaryExpression(expression, Op.ADD, demolishRemainder(remainder))
        } else {
            demolishRemainder(remainder)
        }
    }
    return expression
}

private fun demolishRemainder(remainder: Int): Expression {
    val lowerEntry = table.floorEntry(remainder) ?: throw Exception("No solution")
    val expression = lowerEntry.value
    return if (remainder == lowerEntry.key) {
        expression
    } else {
        BinaryExpression(expression, Op.ADD, demolishRemainder(remainder - lowerEntry.key))
    }
}

internal fun eval(expression: Expression): BigInteger {
    if (expression is Literal) {
        return expression.value.toBigInteger()
    }
    if (expression is BinaryExpression) {
        return when (expression.op) {
            Op.ADD -> eval(expression.left) + eval(expression.right)
            Op.SUB -> eval(expression.left) - eval(expression.right)
            Op.MUL -> eval(expression.left) * eval(expression.right)
            Op.DIV -> eval(expression.left) / eval(expression.right)
            Op.EXP -> eval(expression.left).toDouble().pow(eval(expression.right).toDouble()).toInt().toBigInteger()
        }
    }
    if (expression is UnaryExpression) {
        return when (expression.op) {
            Op.ADD -> eval(expression.expression)
            Op.SUB -> eval(expression.expression).negate()
            else -> throw Exception("Unknown op")
        }
    }
    throw Exception("Unknown expression")
}
