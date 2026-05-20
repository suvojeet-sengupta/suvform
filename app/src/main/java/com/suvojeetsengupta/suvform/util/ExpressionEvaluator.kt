package com.suvojeetsengupta.suvform.util

/**
 * Minimal recursive-descent evaluator for form calculations.
 *
 * Grammar:
 *   expr   = term ((+|-) term)*
 *   term   = unary ((*|/|%) unary)*
 *   unary  = (-)? primary
 *   primary= number | identifier | '(' expr ')'
 *
 * Identifiers resolve to numeric values from the `variables` map (field id -> Double).
 * Missing or non-numeric variables resolve to 0.
 *
 * Use [tryEval] to get null on syntax error rather than an exception.
 */
class ExpressionEvaluator(private val variables: Map<String, Double>) {

    fun tryEval(expression: String): Double? = runCatching { eval(expression) }.getOrNull()

    fun eval(expression: String): Double {
        val tokens = tokenize(expression)
        val parser = Parser(tokens, variables)
        val value = parser.parseExpr()
        parser.expectEnd()
        return value
    }

    // ---- Tokenizer ----

    private sealed class Token {
        data class Num(val value: Double) : Token()
        data class Ident(val name: String) : Token()
        data class Op(val symbol: Char) : Token()
        data object LParen : Token()
        data object RParen : Token()
        data object End : Token()
    }

    companion object {
        private fun tokenize(s: String): List<Token> {
            val out = mutableListOf<Token>()
            var i = 0
            while (i < s.length) {
                val c = s[i]
                when {
                    c.isWhitespace() -> i++
                    c.isDigit() || c == '.' -> {
                        val start = i
                        while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
                        out.add(Token.Num(s.substring(start, i).toDouble()))
                    }
                    c.isLetter() || c == '_' -> {
                        val start = i
                        while (i < s.length && (s[i].isLetterOrDigit() || s[i] == '_')) i++
                        out.add(Token.Ident(s.substring(start, i)))
                    }
                    c in "+-*/%" -> {
                        out.add(Token.Op(c)); i++
                    }
                    c == '(' -> {
                        out.add(Token.LParen); i++
                    }
                    c == ')' -> {
                        out.add(Token.RParen); i++
                    }
                    else -> throw IllegalArgumentException("Unexpected '$c' at $i")
                }
            }
            out.add(Token.End)
            return out
        }
    }

    private class Parser(private val tokens: List<Token>, private val vars: Map<String, Double>) {
        private var pos = 0
        private fun peek() = tokens[pos]
        private fun consume() = tokens[pos++]

        fun expectEnd() {
            if (peek() !is Token.End) throw IllegalArgumentException("Trailing tokens at $pos")
        }

        fun parseExpr(): Double {
            var left = parseTerm()
            while (true) {
                val t = peek()
                if (t is Token.Op && (t.symbol == '+' || t.symbol == '-')) {
                    consume()
                    val right = parseTerm()
                    left = if (t.symbol == '+') left + right else left - right
                } else break
            }
            return left
        }

        private fun parseTerm(): Double {
            var left = parseUnary()
            while (true) {
                val t = peek()
                if (t is Token.Op && (t.symbol == '*' || t.symbol == '/' || t.symbol == '%')) {
                    consume()
                    val right = parseUnary()
                    left = when (t.symbol) {
                        '*' -> left * right
                        '/' -> if (right == 0.0) 0.0 else left / right
                        else -> if (right == 0.0) 0.0 else left % right
                    }
                } else break
            }
            return left
        }

        private fun parseUnary(): Double {
            val t = peek()
            if (t is Token.Op && t.symbol == '-') {
                consume()
                return -parseUnary()
            }
            if (t is Token.Op && t.symbol == '+') {
                consume()
                return parseUnary()
            }
            return parsePrimary()
        }

        private fun parsePrimary(): Double {
            return when (val t = consume()) {
                is Token.Num -> t.value
                is Token.Ident -> vars[t.name] ?: 0.0
                Token.LParen -> {
                    val v = parseExpr()
                    if (peek() !is Token.RParen) throw IllegalArgumentException("Expected ')'")
                    consume(); v
                }
                else -> throw IllegalArgumentException("Unexpected token: $t")
            }
        }
    }
}
