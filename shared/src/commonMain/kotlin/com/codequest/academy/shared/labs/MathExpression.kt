package com.codequest.academy.shared.labs

import kotlin.math.*

/** Raised for any malformed or unsupported math input. The message is meant for the learner. */
class MathSyntaxException(message: String) : IllegalArgumentException(message)

/** Abstract syntax tree for a math expression. Never evaluated through a scripting engine. */
sealed class Expr {
    data class Num(val value: Double) : Expr()
    data class Var(val name: String) : Expr()
    data class Neg(val arg: Expr) : Expr()
    data class Bin(val op: Char, val left: Expr, val right: Expr) : Expr()
    data class Call(val name: String, val args: List<Expr>, val user: Boolean = false, val derivative: Int = 0) : Expr()
    /** Chained comparison such as a < x <= b. Evaluates to 1 (true) or 0 (false). */
    data class Compare(val ops: List<String>, val terms: List<Expr>) : Expr()
    /** Desmos-style piecewise {cond: value, cond: value, else}. A missing value means 1, a missing condition means "else". */
    data class Piecewise(val branches: List<Pair<Expr?, Expr?>>) : Expr()
}

object MathText {
    private val arabicDigits = "٠١٢٣٤٥٦٧٨٩"
    private val persianDigits = "۰۱۲۳۴۵۶۷۸۹"

    /** Converts common typed/pasted math symbols into the plain ASCII grammar understood by the parser. */
    fun normalize(source: String): String {
        val sb = StringBuilder(source.length + 8)
        for (ch in source) {
            val ad = arabicDigits.indexOf(ch); val pd = persianDigits.indexOf(ch)
            when {
                ad >= 0 -> sb.append('0' + ad)
                pd >= 0 -> sb.append('0' + pd)
                else -> sb.append(
                    when (ch) {
                        '−', '–', '—', '‐' -> "-"
                        '×', '·', '∙', '⋅', '∗' -> "*"
                        '÷', '∕' -> "/"
                        '²' -> "^2"
                        '³' -> "^3"
                        '¹' -> "^1"
                        'π' -> " pi "
                        'τ' -> " tau "
                        '√' -> " sqrt "
                        '∛' -> " cbrt "
                        '≤' -> "<="
                        '≥' -> ">="
                        '٫' -> "."
                        '،', '٬' -> ","
                        '[' -> "("
                        ']' -> ")"
                        '\t', '\n', '\r', ' ' -> " "
                        else -> ch.lowercaseChar().toString()
                    }
                )
            }
        }
        return sb.toString().replace("**", "^").replace("==", "=").replace("=<", "<=").replace("=>", ">=")
    }
}

private enum class T { NUM, FUNC, CONST, VAR, USER, OP, END }
private class Tok(val type: T, val text: String, val pos: Int, val num: Double = 0.0)

private val BUILTIN_FUNCTIONS = setOf(
    "sin", "cos", "tan", "cot", "sec", "csc", "asin", "acos", "atan", "arcsin", "arccos", "arctan",
    "sinh", "cosh", "tanh", "sqrt", "cbrt", "ln", "log", "exp", "abs", "floor", "ceil", "round",
    "sign", "sgn", "min", "max", "mod"
)
private val CONSTANTS = mapOf("pi" to PI, "tau" to 2 * PI, "e" to E)
private val TRIG_INVERSE = mapOf("sin" to "asin", "cos" to "acos", "tan" to "atan")

/** Names that cannot be used for user variables or functions. */
val RESERVED_MATH_NAMES: Set<String> = BUILTIN_FUNCTIONS + CONSTANTS.keys + setOf("x", "y")

private fun lex(raw: String, userFunctions: Set<String>): List<Tok> {
    val s = MathText.normalize(raw)
    if (s.length > 1000) throw MathSyntaxException("Expression is too long (max 1000 characters)")
    val names = (BUILTIN_FUNCTIONS + CONSTANTS.keys + userFunctions).sortedByDescending { it.length }
    val out = ArrayList<Tok>()
    var i = 0
    while (i < s.length) {
        val c = s[i]
        when {
            c == ' ' -> i++
            c.isDigit() || (c == '.' && i + 1 < s.length && s[i + 1].isDigit()) -> {
                val start = i; var dots = 0
                while (i < s.length && (s[i].isDigit() || s[i] == '.')) { if (s[i] == '.') dots++; i++ }
                val text = s.substring(start, i)
                if (dots > 1) throw MathSyntaxException("Invalid number '$text'")
                out += Tok(T.NUM, text, start, text.toDouble())
            }
            c in 'a'..'z' -> {
                val start = i
                while (i < s.length && s[i] in 'a'..'z') i++
                val run = s.substring(start, i)
                var j = 0
                while (j < run.length) {
                    val name = names.firstOrNull { run.startsWith(it, j) }
                    if (name != null) {
                        val type = when (name) { in userFunctions -> T.USER; in BUILTIN_FUNCTIONS -> T.FUNC; else -> T.CONST }
                        out += Tok(type, name, start + j); j += name.length
                    } else { out += Tok(T.VAR, run[j].toString(), start + j); j++ }
                }
                // Subscripted variable names such as a_1 or k_max.
                if (i + 1 < s.length && s[i] == '_' && out.last().type == T.VAR && s[i + 1].isLetterOrDigit()) {
                    val subStart = ++i
                    while (i < s.length && (s[i] in 'a'..'z' || s[i].isDigit())) i++
                    val last = out.removeAt(out.size - 1)
                    out += Tok(T.VAR, last.text + "_" + s.substring(subStart, i), last.pos)
                }
            }
            c == '<' || c == '>' -> {
                if (i + 1 < s.length && s[i + 1] == '=') { out += Tok(T.OP, "$c=", i); i += 2 } else { out += Tok(T.OP, c.toString(), i); i++ }
            }
            c in "+-*/^(),|!:_'={}" -> { out += Tok(T.OP, c.toString(), i); i++ }
            c.isLetter() -> throw MathSyntaxException("Unknown symbol '$c'. Use English letters such as x, y, a, b")
            else -> throw MathSyntaxException("Unexpected character '$c' at position ${i + 1}")
        }
    }
    out += Tok(T.END, "", s.length)
    return out
}

private val RELATIONS = setOf("<", "<=", ">", ">=", "=")

class MathParser private constructor(private val toks: List<Tok>) {
    private var p = 0
    private var absDepth = 0
    private val peek: Tok get() = toks[p]
    private fun isOp(t: String) = peek.type == T.OP && peek.text == t
    private fun eat(t: String): Boolean { if (isOp(t)) { p++; return true }; return false }
    private fun fail(msg: String): Nothing = throw MathSyntaxException(
        if (peek.type == T.END) "$msg at the end" else "$msg near '${peek.text}' (position ${peek.pos + 1})"
    )
    private fun expect(t: String, what: String) { if (!eat(t)) fail("Expected $what") }

    companion object {
        /** Parses a complete expression. [userFunctions] are single-letter names defined like f(x) = ... */
        fun parse(source: String, userFunctions: Set<String> = emptySet()): Expr {
            val toks = lex(source, userFunctions)
            if (toks.size == 1) throw MathSyntaxException("Enter an expression")
            val parser = MathParser(toks)
            val e = parser.expression()
            if (parser.peek.type != T.END) {
                if (parser.peek.text in RELATIONS) parser.fail("Only one '=' or inequality is allowed")
                if (parser.isOp(")")) parser.fail("Unmatched ')'")
                if (parser.isOp(",")) parser.fail("Unexpected ','. Points are written on their own line like (1, 2)")
                parser.fail("Unexpected input")
            }
            return e
        }
    }

    private fun expression(): Expr {
        var l = term()
        while (true) l = when { eat("+") -> Expr.Bin('+', l, term()); eat("-") -> Expr.Bin('-', l, term()); else -> return l }
    }

    private fun startsFactor(): Boolean = when (peek.type) {
        T.NUM, T.FUNC, T.CONST, T.VAR, T.USER -> true
        T.OP -> peek.text == "(" || peek.text == "{" || (peek.text == "|" && absDepth == 0)
        else -> false
    }

    private fun term(): Expr {
        var l = unary()
        while (true) l = when {
            eat("*") -> Expr.Bin('*', l, unary())
            eat("/") -> Expr.Bin('/', l, unary())
            startsFactor() -> Expr.Bin('*', l, power()) // implicit multiplication: 2x, 3sin(x), (x+1)(x-1), xy
            else -> return l
        }
    }

    private fun unary(): Expr = when {
        eat("-") -> Expr.Neg(unary())
        eat("+") -> unary()
        else -> power()
    }

    private fun power(): Expr {
        val base = postfix()
        if (eat("^")) {
            if (peek.type == T.END) fail("Missing exponent")
            return Expr.Bin('^', base, unary())
        }
        return base
    }

    private fun postfix(): Expr {
        var e = primary()
        while (eat("!")) e = Expr.Call("factorial", listOf(e))
        return e
    }

    private fun <R> nested(block: () -> R): R { val saved = absDepth; absDepth = 0; try { return block() } finally { absDepth = saved } }

    private fun primary(): Expr {
        val t = peek
        return when (t.type) {
            T.NUM -> { p++; Expr.Num(t.num) }
            T.CONST -> { p++; Expr.Num(CONSTANTS.getValue(t.text)) }
            T.VAR -> { p++; Expr.Var(t.text) }
            T.USER -> {
                p++; var d = 0
                while (eat("'")) d++
                if (!isOp("(")) fail("Write ${t.text}(...) with parentheses to use the function ${t.text}")
                p++
                Expr.Call(t.text, argList(), user = true, derivative = d)
            }
            T.FUNC -> function(t)
            T.OP -> when (t.text) {
                "(" -> { p++; val e = nested { expression() }; if (isOp(",")) fail("Points like (1, 2) must be on their own line"); expect(")", "')'"); e }
                "|" -> { p++; absDepth++; val e = expression(); absDepth--; expect("|", "closing '|'"); Expr.Call("abs", listOf(e)) }
                "{" -> { p++; nested { piecewise() } }
                ")" -> fail("Unmatched ')' or missing value")
                else -> fail(if (t.text in RELATIONS) "Missing value before '${t.text}'" else "Missing value before '${t.text}'")
            }
            T.END -> fail("Expression is incomplete")
        }
    }

    private fun argList(): List<Expr> = nested {
        if (isOp(")")) fail("Empty parentheses")
        val args = mutableListOf(expression())
        while (eat(",")) args += expression()
        expect(")", "')'")
        args
    }

    private fun function(t: Tok): Expr {
        p++
        var name = when (t.text) { "arcsin" -> "asin"; "arccos" -> "acos"; "arctan" -> "atan"; "sgn" -> "sign"; else -> t.text }
        var base: Expr? = null
        if (name == "log" && eat("_")) base = when {
            peek.type == T.NUM -> { val v = peek.num; p++; Expr.Num(v) }
            isOp("(") -> { p++; nested { expression() }.also { expect(")", "')'") } }
            peek.type == T.VAR -> { val v = peek.text; p++; Expr.Var(v) }
            else -> fail("Expected a base after log_")
        }
        var exponent: Expr? = null
        if (eat("^")) exponent = if (eat("-")) Expr.Neg(primary()) else primary()
        if (exponent == Expr.Neg(Expr.Num(1.0)) && name in TRIG_INVERSE) { name = TRIG_INVERSE.getValue(name); exponent = null }
        val args: List<Expr> = if (eat("(")) argList() else {
            // Desmos style: sin x, sin 2x, ln x  (stops before the next function name: sin x cos x)
            val negate = eat("-")
            if (!startsFactor()) fail("${t.text} needs an input, e.g. ${t.text}(x)")
            var a = power()
            while (startsFactor() && peek.type != T.FUNC) a = Expr.Bin('*', a, power())
            listOf(if (negate) Expr.Neg(a) else a)
        }
        val arity = when (name) { "min", "max" -> 1..64; "mod" -> 2..2; "round" -> 1..2; else -> 1..1 }
        if (args.size !in arity) throw MathSyntaxException(
            "$name expects ${if (arity.first == arity.last) "${arity.first}" else "${arity.first}–${arity.last}"} input${if (arity.last == 1) "" else "s"}, got ${args.size}"
        )
        var result: Expr = if (base != null) Expr.Bin('/', Expr.Call("ln", args), Expr.Call("ln", listOf(base))) else Expr.Call(name, args)
        if (exponent != null) result = Expr.Bin('^', result, exponent)
        return result
    }

    private fun piecewise(): Expr {
        val branches = mutableListOf<Pair<Expr?, Expr?>>()
        if (isOp("}")) fail("Empty braces")
        while (true) {
            val first = expression()
            if (peek.type == T.OP && peek.text in RELATIONS) {
                val ops = mutableListOf<String>(); val terms = mutableListOf(first)
                while (peek.type == T.OP && peek.text in RELATIONS) { ops += peek.text; p++; terms += expression() }
                val value = if (eat(":")) expression() else null
                branches += Expr.Compare(ops, terms) to value
            } else {
                if (isOp(":")) fail("A condition like x > 0 is required before ':'")
                branches += null to first
            }
            if (eat(",")) continue
            expect("}", "'}'")
            return Expr.Piecewise(branches)
        }
    }
}

class UserFunction(val name: String, val params: List<String>, val body: Expr)

class EvalContext(
    val constants: Map<String, Double> = emptyMap(),
    val functions: Map<String, UserFunction> = emptyMap()
)

class MathEvalException(message: String) : IllegalArgumentException(message)

/**
 * Evaluates expressions. Domain errors (sqrt(-1), ln(0) …) produce NaN/Infinity rather than exceptions,
 * so callers can treat them as "undefined" points. Unknown names throw [MathEvalException].
 */
class Evaluator(private val ctx: EvalContext = EvalContext()) {
    var x: Double = Double.NaN
    var y: Double = Double.NaN
    var hasX = false
    var hasY = false

    fun at(x: Double): Evaluator { this.x = x; hasX = true; return this }
    fun at(x: Double, y: Double): Evaluator { this.x = x; this.y = y; hasX = true; hasY = true; return this }

    fun eval(e: Expr): Double = ev(e, null, 0)

    private fun lookup(name: String, locals: Map<String, Double>?): Double {
        locals?.get(name)?.let { return it }
        if (locals == null) {
            if (name == "x" && hasX) return x
            if (name == "y" && hasY) return y
        }
        ctx.constants[name]?.let { return it }
        throw MathEvalException(
            if (name == "x" || name == "y") "This answer must be a number; it cannot contain the variable $name" else "'$name' is not defined"
        )
    }

    private fun ev(e: Expr, locals: Map<String, Double>?, depth: Int): Double = when (e) {
        is Expr.Num -> e.value
        is Expr.Var -> lookup(e.name, locals)
        is Expr.Neg -> -ev(e.arg, locals, depth)
        is Expr.Bin -> {
            val a = ev(e.left, locals, depth); val b = ev(e.right, locals, depth)
            when (e.op) { '+' -> a + b; '-' -> a - b; '*' -> a * b; '/' -> a / b; else -> MathFunctions.pow(a, b) }
        }
        is Expr.Compare -> if (compare(e, locals, depth)) 1.0 else 0.0
        is Expr.Piecewise -> {
            var result = Double.NaN
            for ((cond, value) in e.branches) {
                if (cond == null) { result = ev(value!!, locals, depth); break }
                val ok = if (cond is Expr.Compare) compare(cond, locals, depth) else ev(cond, locals, depth) != 0.0
                if (ok) { result = value?.let { ev(it, locals, depth) } ?: 1.0; break }
            }
            result
        }
        is Expr.Call -> if (e.user) callUser(e, locals, depth) else {
            if (e.args.size == 1) MathFunctions.apply1(e.name, ev(e.args[0], locals, depth))
            else MathFunctions.applyN(e.name, e.args.map { ev(it, locals, depth) })
        }
    }

    private fun compare(c: Expr.Compare, locals: Map<String, Double>?, depth: Int): Boolean {
        var left = ev(c.terms[0], locals, depth)
        for (i in c.ops.indices) {
            val right = ev(c.terms[i + 1], locals, depth)
            if (left.isNaN() || right.isNaN()) return false
            val ok = when (c.ops[i]) {
                "<" -> left < right; "<=" -> left <= right; ">" -> left > right; ">=" -> left >= right
                else -> abs(left - right) <= 1e-9 * max(1.0, abs(right))
            }
            if (!ok) return false
            left = right
        }
        return true
    }

    private fun callUser(e: Expr.Call, locals: Map<String, Double>?, depth: Int): Double {
        val fn = ctx.functions[e.name] ?: throw MathEvalException("Function '${e.name}' is not defined")
        if (depth > 48) throw MathEvalException("Function definitions are nested too deeply (recursive?)")
        if (e.args.size != fn.params.size) throw MathEvalException("${e.name} expects ${fn.params.size} input(s)")
        val args = e.args.map { ev(it, locals, depth) }
        fun body(vals: List<Double>): Double = ev(fn.body, fn.params.zip(vals).toMap(), depth + 1)
        if (e.derivative == 0) return body(args)
        if (fn.params.size != 1) throw MathEvalException("Derivatives ${e.name}' need a one-variable function")
        return derivative({ body(listOf(it)) }, args[0], e.derivative)
    }

    private fun derivative(f: (Double) -> Double, at: Double, order: Int): Double {
        val h = (if (order == 1) 1e-5 else 1e-3) * max(1.0, abs(at))
        if (order == 1) return (f(at + h) - f(at - h)) / (2 * h)
        return (derivative(f, at + h, order - 1) - derivative(f, at - h, order - 1)) / (2 * h)
    }
}

object MathFunctions {
    fun apply1(name: String, v: Double): Double = when (name) {
        "sin" -> sin(v); "cos" -> cos(v); "tan" -> tan(v)
        "cot" -> 1 / tan(v); "sec" -> 1 / cos(v); "csc" -> 1 / sin(v)
        "asin" -> asin(v); "acos" -> acos(v); "atan" -> atan(v)
        "sinh" -> sinh(v); "cosh" -> cosh(v); "tanh" -> tanh(v)
        "sqrt" -> sqrt(v); "cbrt" -> cbrt(v)
        "ln" -> ln(v); "log" -> log10(v); "exp" -> exp(v)
        "abs" -> abs(v); "floor" -> floor(v); "ceil" -> ceil(v)
        "round" -> sign(v) * floor(abs(v) + 0.5)
        "sign" -> sign(v)
        "min", "max" -> v
        "factorial" -> gamma(v + 1)
        else -> throw MathEvalException("Unknown function $name")
    }

    fun applyN(name: String, a: List<Double>): Double = when (name) {
        "min" -> a.minOrNull() ?: Double.NaN
        "max" -> a.maxOrNull() ?: Double.NaN
        "mod" -> if (a[1] == 0.0) Double.NaN else a[0] - a[1] * floor(a[0] / a[1])
        "round" -> { val f = 10.0.pow(a[1].roundToInt()); sign(a[0]) * floor(abs(a[0]) * f + 0.5) / f }
        else -> throw MathEvalException("$name expects 1 input")
    }

    /** Real power that, like Desmos, gives real odd roots of negatives: (-8)^(1/3) = -2. */
    fun pow(a: Double, b: Double): Double {
        if (a >= 0 || b == floor(b) || b.isNaN() || b.isInfinite()) return a.pow(b)
        for (q in 3..99 step 2) {
            val pq = b * q
            val pr = pq.roundToLong()
            if (abs(pq - pr) < 1e-9) {
                val magnitude = (-a).pow(b)
                return if (pr % 2L == 0L) magnitude else -magnitude
            }
        }
        return Double.NaN
    }

    private val lanczos = doubleArrayOf(
        0.99999999999980993, 676.5203681218851, -1259.1392167224028, 771.32342877765313,
        -176.61502916214059, 12.507343278686905, -0.13857109526572012, 9.9843695780195716e-6, 1.5056327351493116e-7
    )

    fun gamma(x: Double): Double {
        if (x.isNaN()) return Double.NaN
        if (x == floor(x)) {
            if (x <= 0) return Double.NaN
            if (x <= 171) { var r = 1.0; var i = 2.0; while (i < x) { r *= i; i++ }; return r }
            return Double.POSITIVE_INFINITY
        }
        if (x < 0.5) return PI / (sin(PI * x) * gamma(1 - x))
        val z = x - 1
        var a = lanczos[0]
        val t = z + 7.5
        for (i in 1 until 9) a += lanczos[i] / (z + i)
        return sqrt(2 * PI) * t.pow(z + 0.5) * exp(-t) * a
    }
}

/** Free variable names used in an expression (user function parameters are not included in calls). */
fun Expr.variables(into: MutableSet<String> = mutableSetOf()): MutableSet<String> {
    when (this) {
        is Expr.Num -> {}
        is Expr.Var -> into += name
        is Expr.Neg -> arg.variables(into)
        is Expr.Bin -> { left.variables(into); right.variables(into) }
        is Expr.Call -> args.forEach { it.variables(into) }
        is Expr.Compare -> terms.forEach { it.variables(into) }
        is Expr.Piecewise -> branches.forEach { (c, v) -> c?.variables(into); v?.variables(into) }
    }
    return into
}

/** User functions called by an expression, with the number of arguments used. */
fun Expr.userCalls(into: MutableList<Pair<String, Int>> = mutableListOf()): MutableList<Pair<String, Int>> {
    when (this) {
        is Expr.Num, is Expr.Var -> {}
        is Expr.Neg -> arg.userCalls(into)
        is Expr.Bin -> { left.userCalls(into); right.userCalls(into) }
        is Expr.Call -> { if (user) into += name to args.size; args.forEach { it.userCalls(into) } }
        is Expr.Compare -> terms.forEach { it.userCalls(into) }
        is Expr.Piecewise -> branches.forEach { (c, v) -> c?.userCalls(into); v?.userCalls(into) }
    }
    return into
}

/** Formats a number for display: no floating noise like 0.30000000000000004, at most [decimals] decimals. */
fun formatMath(v: Double, decimals: Int = 6): String {
    if (v.isNaN()) return "undefined"
    if (v.isInfinite()) return if (v > 0) "∞" else "−∞"
    val a = abs(v)
    if (a != 0.0 && (a >= 1e15 || a < 10.0.pow(-decimals))) {
        val exp = floor(log10(a)).toInt()
        val mant = v / 10.0.pow(exp)
        return formatMath(mant, 4) + "×10^" + exp
    }
    val factor = 10.0.pow(decimals)
    val scaled = (a * factor).roundToLong()
    val intPart = scaled / factor.toLong()
    var frac = (scaled % factor.toLong()).toString().padStart(decimals, '0').trimEnd('0')
    val sign = if (v < 0 && scaled != 0L) "-" else ""
    return if (frac.isEmpty()) "$sign$intPart" else "$sign$intPart.$frac"
}
