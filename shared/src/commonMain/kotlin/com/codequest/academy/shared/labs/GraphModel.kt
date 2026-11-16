package com.codequest.academy.shared.labs

import kotlin.math.*

enum class Relation(val symbol: String) {
    Eq("="), Lt("<"), Le("≤"), Gt(">"), Ge("≥");
    val strict: Boolean get() = this == Lt || this == Gt
    val isInequality: Boolean get() = this != Eq
    fun flip() = when (this) { Lt -> Gt; Gt -> Lt; Le -> Ge; Ge -> Le; Eq -> Eq }
    /** Whether "value relation 0" holds. */
    fun holds(v: Double) = when (this) { Eq -> v == 0.0; Lt -> v < 0; Le -> v <= 0; Gt -> v > 0; Ge -> v >= 0 }
    companion object {
        fun of(op: String) = when (op) { "<" -> Lt; "<=" -> Le; ">" -> Gt; ">=" -> Ge; else -> Eq }
    }
}

sealed class GraphItem {
    object Blank : GraphItem()
    data class Invalid(val message: String) : GraphItem()
    /** A plain number such as 2^10 — shown as "= 1024". */
    data class Value(val value: Double) : GraphItem()
    /** y (relation) f(x) */
    data class FunctionOfX(val expr: Expr, val relation: Relation) : GraphItem()
    /** x (relation) g(y) — includes vertical lines x = 3 */
    data class FunctionOfY(val expr: Expr, val relation: Relation) : GraphItem()
    /** F(x, y) (relation) 0 — circles, ellipses, implicit curves, regions */
    data class Implicit(val expr: Expr, val relation: Relation) : GraphItem()
    /** (x(t), y(t)) parametric curve for t in [tMin, tMax] */
    data class Parametric(val xExpr: Expr, val yExpr: Expr, val tMin: Double = -2 * PI, val tMax: Double = 2 * PI) : GraphItem()
    data class Point(val x: Double, val y: Double) : GraphItem()
    /** a = 3 → adjustable slider */
    data class Slider(val name: String, val value: Double) : GraphItem()
    /** a = 2b + 1 → derived constant */
    data class Constant(val name: String, val value: Double) : GraphItem()
    /** f(x) = ... ; [plot] is non-null when it is a function of x that should be drawn */
    data class Definition(val name: String, val params: List<String>, val plot: FunctionOfX?) : GraphItem()
}

data class GraphLine(val item: GraphItem, val missing: List<String> = emptyList()) {
    val drawable: Boolean get() = item is GraphItem.FunctionOfX || item is GraphItem.FunctionOfY || item is GraphItem.Implicit ||
        item is GraphItem.Parametric || item is GraphItem.Point || (item is GraphItem.Definition && item.plot != null)
}

class CompiledGraph(val lines: List<GraphLine>, val context: EvalContext)

object GraphCompiler {
    private val fnDef = Regex("^([a-df-wz])\\s*\\(\\s*([a-z](?:\\s*,\\s*[a-z])*)\\s*\\)$")
    private val constDef = Regex("^([a-df-wz](?:_[a-z0-9]+)?)$")

    private data class Split(val lhs: String, val op: String?, val rhs: String)

    /** Splits "lhs op rhs" at the single top-level relation operator (outside (), {}). */
    private fun split(s: String): Split {
        var depth = 0; var found: Pair<Int, String>? = null
        var i = 0
        while (i < s.length) {
            val c = s[i]
            when (c) {
                '(', '{', '[' -> depth++
                ')', '}', ']' -> depth--
                '<', '>', '=' -> if (depth == 0) {
                    val op = if ((c == '<' || c == '>') && i + 1 < s.length && s[i + 1] == '=') "$c=" else c.toString()
                    if (found != null) throw MathSyntaxException("Use only one '=' or inequality per line")
                    found = i to op
                    i += op.length; continue
                }
            }
            i++
        }
        val f = found ?: return Split(s, null, "")
        val lhs = s.substring(0, f.first).trim(); val rhs = s.substring(f.first + f.second.length).trim()
        if (lhs.isEmpty()) throw MathSyntaxException("Missing left side of '${f.second}'")
        if (rhs.isEmpty()) throw MathSyntaxException("Missing right side of '${f.second}'")
        return Split(lhs, f.second, rhs)
    }

    /** Returns the two components if [s] is a point literal "(a, b)". */
    private fun pointParts(s: String): Pair<String, String>? {
        if (!s.startsWith("(") || !s.endsWith(")")) return null
        var depth = 0; var comma = -1
        for (i in s.indices) {
            when (s[i]) {
                '(', '{' -> depth++
                ')', '}' -> { depth--; if (depth == 0 && i != s.length - 1) return null }
                ',' -> if (depth == 1) { if (comma >= 0) throw MathSyntaxException("Points have exactly two coordinates: (x, y)"); comma = i }
            }
        }
        if (comma < 0) return null
        return s.substring(1, comma).trim() to s.substring(comma + 1, s.length - 1).trim()
    }

    fun compile(rawLines: List<String>): CompiledGraph {
        val n = rawLines.size
        val norm = rawLines.map { MathText.normalize(it).trim() }
        val splits = arrayOfNulls<Split>(n)
        val results = arrayOfNulls<GraphLine>(n)
        norm.forEachIndexed { i, s ->
            if (s.isEmpty()) results[i] = GraphLine(GraphItem.Blank)
            else try { splits[i] = split(s) } catch (e: MathSyntaxException) { results[i] = GraphLine(GraphItem.Invalid(e.message ?: "Invalid input")) }
        }

        // 1. Function definitions f(x) = ...
        val fnLines = mutableMapOf<String, Int>()
        val fnParams = mutableMapOf<Int, List<String>>()
        for (i in 0 until n) {
            val sp = splits[i] ?: continue
            if (sp.op != "=") continue
            val m = fnDef.matchEntire(sp.lhs) ?: continue
            val name = m.groupValues[1]
            val params = m.groupValues[2].split(',').map { it.trim() }
            if (params.toSet().size != params.size) { results[i] = GraphLine(GraphItem.Invalid("Parameter names must be different")); continue }
            if (name in fnLines) { results[i] = GraphLine(GraphItem.Invalid("'$name' is defined more than once")); continue }
            fnLines[name] = i; fnParams[i] = params
        }
        val userNames = fnLines.keys

        // 2. Constant definitions a = ...
        val constLines = mutableMapOf<String, Int>()
        val constExpr = mutableMapOf<Int, Expr>()
        for (i in 0 until n) {
            val sp = splits[i] ?: continue
            if (results[i] != null || sp.op != "=" || i in fnParams) continue
            val m = constDef.matchEntire(sp.lhs) ?: continue
            val name = m.groupValues[1]
            if (name in userNames) { results[i] = GraphLine(GraphItem.Invalid("'$name' is already a function")); continue }
            try {
                val e = MathParser.parse(sp.rhs, userNames)
                val vars = e.variables()
                if ("x" in vars || "y" in vars) continue // e.g. a = x + 1 → treated as an implicit equation below
                if (name in constLines) { results[i] = GraphLine(GraphItem.Invalid("'$name' is defined more than once")); continue }
                if (name in vars) { results[i] = GraphLine(GraphItem.Invalid("'$name' cannot be defined in terms of itself")); continue }
                constLines[name] = i; constExpr[i] = e
            } catch (e: MathSyntaxException) { results[i] = GraphLine(GraphItem.Invalid(e.message ?: "Invalid input")) }
        }

        // 3. Parse function bodies
        val functions = mutableMapOf<String, UserFunction>()
        val fnBodies = mutableMapOf<Int, Expr>()
        for ((name, i) in fnLines) {
            try {
                val body = MathParser.parse(splits[i]!!.rhs, userNames)
                fnBodies[i] = body
                functions[name] = UserFunction(name, fnParams.getValue(i), body)
            } catch (e: MathSyntaxException) { results[i] = GraphLine(GraphItem.Invalid(e.message ?: "Invalid input")) }
        }
        val brokenFns = mutableSetOf<String>()
        fnLines.forEach { (name, i) -> if (results[i] != null) brokenFns += name }
        // Recursion / arity checks
        fun reaches(from: String, target: String, seen: MutableSet<String>): Boolean {
            val f = functions[from] ?: return false
            for ((callee, _) in f.body.userCalls()) {
                if (callee == target) return true
                if (seen.add(callee) && reaches(callee, target, seen)) return true
            }
            return false
        }
        for ((name, i) in fnLines) {
            if (results[i] != null) continue
            val f = functions[name] ?: continue
            if (reaches(name, name, mutableSetOf())) { results[i] = GraphLine(GraphItem.Invalid("'$name' cannot call itself")); brokenFns += name; functions.remove(name) }
        }

        // 4. Resolve constants iteratively (they may depend on each other and on functions)
        val constants = mutableMapOf<String, Double>()
        val pending = constLines.toMutableMap()
        var progress = true
        val lastError = mutableMapOf<String, String>()
        while (pending.isNotEmpty() && progress) {
            progress = false
            val ctx = EvalContext(constants.toMap(), functions)
            for ((name, i) in pending.toMap()) {
                try {
                    val v = Evaluator(ctx).eval(constExpr.getValue(i))
                    constants[name] = v; pending.remove(name); progress = true
                } catch (e: MathEvalException) { lastError[name] = e.message ?: "" }
            }
        }
        val context = EvalContext(constants, functions)
        /** Variables that are neither x, y, parameters nor successfully defined constants (slider candidates). */
        fun missingOf(e: Expr, locals: Collection<String> = emptyList()): List<String> =
            (e.variables() - setOf("x", "y") - constants.keys - locals.toSet() - constLines.keys).sorted()

        fun brokenCall(e: Expr): String? {
            (e.variables() - constants.keys).firstOrNull { it in constLines.keys }?.let { return "'$it' has an error" }
            for ((name, count) in e.userCalls()) {
                if (name in brokenFns) return "'$name' has an error"
                val f = functions[name] ?: return "'$name' is not defined"
                if (f.params.size != count) return "$name expects ${f.params.size} input${if (f.params.size == 1) "" else "s"}"
            }
            return null
        }

        for ((name, i) in constLines) {
            if (results[i] != null) continue
            val e = constExpr.getValue(i)
            val v = constants[name]
            val broken = if (v == null) brokenCall(e) else null
            if (broken != null) { results[i] = GraphLine(GraphItem.Invalid(broken)); continue }
            val missing = missingOf(e) - name
            results[i] = when {
                v == null -> if (missing.isNotEmpty()) GraphLine(GraphItem.Invalid("Define ${missing.joinToString()} first"), missing)
                    else GraphLine(GraphItem.Invalid(lastError[name] ?: "Circular definition"))
                e is Expr.Num || (e is Expr.Neg && e.arg is Expr.Num) -> GraphLine(GraphItem.Slider(name, v))
                else -> GraphLine(GraphItem.Constant(name, v))
            }
        }

        for ((name, i) in fnLines) {
            if (results[i] != null) continue
            val params = fnParams.getValue(i)
            val body = fnBodies.getValue(i)
            val broken = brokenCall(body)
            if (broken != null) { results[i] = GraphLine(GraphItem.Invalid(broken)); continue }
            val missing = missingOf(body, params)
            val plot = if (params == listOf("x") && missing.isEmpty()) GraphItem.FunctionOfX(Expr.Call(name, listOf(Expr.Var("x")), user = true), Relation.Eq) else null
            results[i] = GraphLine(GraphItem.Definition(name, params, plot), missing)
        }

        // 5. Everything else: graphs, points, values
        for (i in 0 until n) {
            if (results[i] != null) continue
            val sp = splits[i]!!
            results[i] = try { classify(sp, userNames, context, ::missingOf, ::brokenCall) }
            catch (e: MathSyntaxException) { GraphLine(GraphItem.Invalid(e.message ?: "Invalid input")) }
            catch (e: MathEvalException) { GraphLine(GraphItem.Invalid(e.message ?: "Invalid input")) }
        }
        return CompiledGraph(results.map { it!! }, context)
    }

    private fun classify(
        sp: Split, userNames: Set<String>, ctx: EvalContext,
        missingOf: (Expr, Collection<String>) -> List<String>, brokenCall: (Expr) -> String?
    ): GraphLine {
        fun parse(s: String) = MathParser.parse(s, userNames).also { e -> brokenCall(e)?.let { throw MathSyntaxException(it) } }
        if (sp.op == null) {
            pointParts(sp.lhs)?.let { (a, b) ->
                val ex = parse(a); val ey = parse(b)
                val vars = ex.variables() + ey.variables()
                if ("x" in vars || "y" in vars) return GraphLine(GraphItem.Invalid("Point coordinates must be numbers (or use parameter t for a curve)"))
                if ("t" in vars && "t" !in ctx.constants) {
                    val missing = (missingOf(ex, listOf("t")) + missingOf(ey, listOf("t"))).distinct()
                    return GraphLine(GraphItem.Parametric(ex, ey), missing)
                }
                val missing = (missingOf(ex, emptyList()) + missingOf(ey, emptyList())).distinct()
                if (missing.isNotEmpty()) return GraphLine(GraphItem.Invalid("Undefined: ${missing.joinToString()}"), missing)
                val px = Evaluator(ctx).eval(ex); val py = Evaluator(ctx).eval(ey)
                if (!px.isFinite() || !py.isFinite()) return GraphLine(GraphItem.Invalid("Point is undefined"))
                return GraphLine(GraphItem.Point(px, py))
            }
            val e = parse(sp.lhs)
            val vars = e.variables()
            val missing = missingOf(e, emptyList())
            if ("y" in vars) return GraphLine(GraphItem.Invalid("Add an equation or inequality, e.g. y = ${sp.lhs.replace("y", "x")} or ${sp.lhs} = 1"))
            if ("x" in vars) return GraphLine(GraphItem.FunctionOfX(e, Relation.Eq), missing)
            if (missing.isNotEmpty()) return GraphLine(GraphItem.Invalid("Undefined: ${missing.joinToString()}"), missing)
            return GraphLine(GraphItem.Value(Evaluator(ctx).eval(e)))
        }
        val rel = Relation.of(sp.op)
        val l = parse(sp.lhs); val r = parse(sp.rhs)
        val lv = l.variables(); val rv = r.variables()
        val missing = (missingOf(l, emptyList()) + missingOf(r, emptyList())).distinct().sorted()
        val item: GraphItem = when {
            sp.lhs == "y" && "y" !in rv -> GraphItem.FunctionOfX(r, rel)
            sp.rhs == "y" && "y" !in lv -> GraphItem.FunctionOfX(l, rel.flip())
            sp.lhs == "x" && "x" !in rv -> GraphItem.FunctionOfY(r, rel)
            sp.rhs == "x" && "x" !in lv -> GraphItem.FunctionOfY(l, rel.flip())
            else -> {
                val all = lv + rv
                if ("x" !in all && "y" !in all) {
                    if (missing.isNotEmpty()) return GraphLine(GraphItem.Invalid("Undefined: ${missing.joinToString()}"), missing)
                    return GraphLine(GraphItem.Invalid("Nothing to graph: this statement has no x or y"))
                }
                GraphItem.Implicit(Expr.Bin('-', l, r), rel)
            }
        }
        return GraphLine(item, missing)
    }
}

/** Polyline in graph (math) coordinates. */
class Polyline(val xs: DoubleArray, val ys: DoubleArray) { val size get() = xs.size }

private class PolylineBuilder {
    val result = mutableListOf<Polyline>()
    private val xs = ArrayList<Double>(); private val ys = ArrayList<Double>()
    fun add(x: Double, y: Double) { xs += x; ys += y }
    fun breakLine() {
        if (xs.size >= 2) result += Polyline(xs.toDoubleArray(), ys.toDoubleArray())
        else if (xs.size == 1) result += Polyline(doubleArrayOf(xs[0]), doubleArrayOf(ys[0]))
        xs.clear(); ys.clear()
    }
}

enum class PoiKind { Root, Maximum, Minimum, YIntercept, Intersection }
data class PointOfInterest(val x: Double, val y: Double, val kind: PoiKind)

object GraphSampler {
    /**
     * Adaptive sampling of y = f(t) for t in [from, to]. Splits the curve at asymptotes and jumps,
     * refines steep sections and finds domain edges (e.g. sqrt(x) at 0, circles at their ends).
     */
    fun sample(f: (Double) -> Double, from: Double, to: Double, steps: Int, valueMin: Double, valueMax: Double): List<Polyline> {
        val span = max(valueMax - valueMin, 1e-12)
        val b = PolylineBuilder()
        fun safe(t: Double): Double = try { f(t) } catch (_: Exception) { Double.NaN }
        fun edge(good: Double, bad: Double): Double {
            var g = good; var bd = bad
            repeat(40) { val m = (g + bd) / 2; if (safe(m).isFinite()) g = m else bd = m }
            return g
        }
        fun connect(x0: Double, y0: Double, x1: Double, y1: Double, depth: Int) {
            val f0 = y0.isFinite(); val f1 = y1.isFinite()
            if (!f0 && !f1) {
                if (depth < 3) { // look for a small defined island between two undefined samples
                    val xm = (x0 + x1) / 2; val ym = safe(xm)
                    if (ym.isFinite()) { connect(x0, y0, xm, ym, depth + 1); connect(xm, ym, x1, y1, depth + 1) }
                }
                return
            }
            if (f0 && !f1) { val e = edge(x0, x1); b.add(e, safe(e)); b.breakLine(); return }
            if (!f0) { b.breakLine(); val e = edge(x1, x0); b.add(e, safe(e)); b.add(x1, y1); return }
            val jump = abs(y1 - y0)
            val outside = (y0 > valueMax && y1 > valueMax) || (y0 < valueMin && y1 < valueMin)
            if (jump <= span * 0.02 || outside && depth > 2) { b.add(x1, y1); return }
            if (depth >= 10) {
                if (jump > span * 0.2) { b.breakLine(); b.add(x1, y1) } else b.add(x1, y1)
                return
            }
            val xm = (x0 + x1) / 2; val ym = safe(xm)
            connect(x0, y0, xm, ym, depth + 1)
            connect(xm, ym, x1, y1, depth + 1)
        }
        val dx = (to - from) / steps
        var px = from; var py = safe(px)
        if (py.isFinite()) b.add(px, py)
        for (i in 1..steps) {
            val x = from + i * dx; val y = safe(x)
            connect(px, py, x, y, 0)
            px = x; py = y
        }
        b.breakLine()
        return b.result
    }

    class Segment(val x1: Double, val y1: Double, val x2: Double, val y2: Double)

    /** Grid of F(x,y) values used by marching squares and region shading. */
    class Field(val xMin: Double, val yMin: Double, val dx: Double, val dy: Double, val cols: Int, val rows: Int, val v: DoubleArray) {
        operator fun get(i: Int, j: Int) = v[j * (cols + 1) + i]
    }

    fun field(f: (Double, Double) -> Double, xMin: Double, xMax: Double, yMin: Double, yMax: Double, cols: Int, rows: Int): Field {
        val dx = (xMax - xMin) / cols; val dy = (yMax - yMin) / rows
        val v = DoubleArray((cols + 1) * (rows + 1))
        for (j in 0..rows) { val y = yMin + j * dy; for (i in 0..cols) v[j * (cols + 1) + i] = try { f(xMin + i * dx, y) } catch (_: Exception) { Double.NaN } }
        return Field(xMin, yMin, dx, dy, cols, rows, v)
    }

    /** Marching squares contour of F = 0 with saddle disambiguation and pole rejection. */
    fun contour(field: Field, f: (Double, Double) -> Double): List<Segment> {
        val out = ArrayList<Segment>()
        for (j in 0 until field.rows) for (i in 0 until field.cols) {
            val a = field[i, j]; val bb = field[i + 1, j]; val c = field[i + 1, j + 1]; val d = field[i, j + 1]
            if (!(a.isFinite() && bb.isFinite() && c.isFinite() && d.isFinite())) continue
            val idx = (if (a > 0) 1 else 0) or (if (bb > 0) 2 else 0) or (if (c > 0) 4 else 0) or (if (d > 0) 8 else 0)
            if (idx == 0 || idx == 15) continue
            val x0 = field.xMin + i * field.dx; val y0 = field.yMin + j * field.dy
            val x1 = x0 + field.dx; val y1 = y0 + field.dy
            val center = try { f(x0 + field.dx / 2, y0 + field.dy / 2) } catch (_: Exception) { Double.NaN }
            if (!center.isFinite()) continue
            val maxCorner = maxOf(abs(a), abs(bb), abs(c), abs(d))
            if (abs(center) > maxCorner * 1.5 + 1e-12) continue // pole (e.g. 1/x = y), not a real crossing
            fun lerp(p: Double, q: Double) = if (p == q) 0.5 else (p / (p - q)).coerceIn(0.0, 1.0)
            val bottom = { x0 + lerp(a, bb) * field.dx to y0 }
            val right = { x1 to y0 + lerp(bb, c) * field.dy }
            val top = { x0 + lerp(d, c) * field.dx to y1 }
            val left = { x0 to y0 + lerp(a, d) * field.dy }
            fun seg(p: () -> Pair<Double, Double>, q: () -> Pair<Double, Double>) { val s = p(); val e = q(); out += Segment(s.first, s.second, e.first, e.second) }
            when (idx) {
                1, 14 -> seg(left, bottom)
                2, 13 -> seg(bottom, right)
                3, 12 -> seg(left, right)
                4, 11 -> seg(right, top)
                6, 9 -> seg(bottom, top)
                7, 8 -> seg(left, top)
                5 -> if (center > 0) { seg(left, top); seg(bottom, right) } else { seg(left, bottom); seg(right, top) }
                10 -> if (center > 0) { seg(left, bottom); seg(right, top) } else { seg(left, top); seg(bottom, right) }
            }
        }
        return out
    }

    /** Roots, extrema, y-intercept and intersections of y = f(x) with other curves inside [x0, x1]. */
    fun pointsOfInterest(f: (Double) -> Double, others: List<(Double) -> Double>, x0: Double, x1: Double, yMin: Double, yMax: Double, steps: Int = 400): List<PointOfInterest> {
        val out = mutableListOf<PointOfInterest>()
        val span = yMax - yMin
        fun safe(g: (Double) -> Double, t: Double) = try { g(t) } catch (_: Exception) { Double.NaN }
        fun bisect(g: (Double) -> Double, a0: Double, b0: Double): Double? {
            var a = a0; var b = b0; var ga = safe(g, a)
            repeat(60) { val m = (a + b) / 2; val gm = safe(g, m); if (!gm.isFinite()) return null; if ((gm > 0) == (ga > 0)) { a = m; ga = gm } else b = m }
            val r = (a + b) / 2
            return if (abs(safe(g, r)) <= 1e-7 * max(1.0, span)) r else null
        }
        fun roots(g: (Double) -> Double, kind: PoiKind) {
            val dx = (x1 - x0) / steps
            var pa = x0; var ga = safe(g, pa)
            for (k in 1..steps) {
                val xb = x0 + k * dx; val gb = safe(g, xb)
                if (ga.isFinite() && gb.isFinite()) {
                    if (ga == 0.0) out += PointOfInterest(pa, f(pa), kind)
                    else if ((ga > 0) != (gb > 0) && gb != 0.0) bisect(g, pa, xb)?.let { r -> val y = safe(f, r); if (y.isFinite()) out += PointOfInterest(r, if (kind == PoiKind.Root) 0.0 else y, kind) }
                }
                pa = xb; ga = gb
            }
        }
        roots(f, PoiKind.Root)
        others.forEach { o -> roots({ t -> f(t) - o(t) }, PoiKind.Intersection) }
        // Extrema by slope sign change + golden-section refinement
        val dx = (x1 - x0) / steps
        val ys = DoubleArray(steps + 1) { safe(f, x0 + it * dx) }
        for (k in 1 until steps) {
            val a = ys[k - 1]; val m = ys[k]; val c = ys[k + 1]
            if (!(a.isFinite() && m.isFinite() && c.isFinite())) continue
            val isMax = m > a && m >= c; val isMin = m < a && m <= c
            if (!isMax && !isMin) continue
            var lo = x0 + (k - 1) * dx; var hi = x0 + (k + 1) * dx
            val gr = (sqrt(5.0) - 1) / 2
            repeat(60) {
                val p = hi - gr * (hi - lo); val q = lo + gr * (hi - lo)
                val fp = safe(f, p); val fq = safe(f, q)
                if (if (isMax) fp > fq else fp < fq) hi = q else lo = p
            }
            val xe = (lo + hi) / 2; val ye = safe(f, xe)
            if (ye.isFinite() && abs(ye - m) < span) out += PointOfInterest(xe, ye, if (isMax) PoiKind.Maximum else PoiKind.Minimum)
        }
        if (0.0 in x0..x1) { val y = safe(f, 0.0); if (y.isFinite()) out += PointOfInterest(0.0, y, PoiKind.YIntercept) }
        // De-duplicate
        val tol = (x1 - x0) * 1e-4
        val unique = mutableListOf<PointOfInterest>()
        for (p in out.sortedBy { it.x }) if (unique.none { it.kind == p.kind && abs(it.x - p.x) < tol && abs(it.y - p.y) < span * 1e-4 }) unique += p
        return unique.filter { it.y in (yMin - span)..(yMax + span) }.take(60)
    }

    /** "Nice" grid step (1, 2 or 5 × 10ⁿ) close to [raw]. */
    fun niceStep(raw: Double): Double {
        val exp = floor(log10(raw)); val base = 10.0.pow(exp); val m = raw / base
        return base * when { m < 1.5 -> 1.0; m < 3.5 -> 2.0; m < 7.5 -> 5.0; else -> 10.0 }
    }

    /** Samples a parametric curve (x(t), y(t)) for t in [tMin, tMax]. */
    fun sampleParametric(fx: (Double) -> Double, fy: (Double) -> Double, tMin: Double, tMax: Double, steps: Int = 600): List<Polyline> {
        val b = PolylineBuilder()
        val dt = (tMax - tMin) / steps
        for (i in 0..steps) {
            val t = tMin + i * dt
            val x = try { fx(t) } catch (_: Exception) { Double.NaN }
            val y = try { fy(t) } catch (_: Exception) { Double.NaN }
            if (x.isFinite() && y.isFinite()) b.add(x, y) else b.breakLine()
        }
        b.breakLine()
        return b.result
    }

    /** Numerical definite integral ∫_a^b f(x) dx using composite Simpson's rule. */
    fun definiteIntegral(f: (Double) -> Double, a: Double, b: Double, steps: Int = 400): Double {
        if (!a.isFinite() || !b.isFinite()) return Double.NaN
        if (a == b) return 0.0
        val sign = if (b >= a) 1.0 else -1.0
        val lo = min(a, b); val hi = max(a, b)
        val n = if (steps % 2 == 0) steps else steps + 1
        val h = (hi - lo) / n
        var sum = 0.0
        for (i in 0..n) {
            val y = try { f(lo + i * h) } catch (_: Exception) { Double.NaN }
            if (!y.isFinite()) return Double.NaN
            val w = when { i == 0 || i == n -> 1.0; i % 2 == 1 -> 4.0; else -> 2.0 }
            sum += w * y
        }
        return sign * (h / 3.0) * sum
    }
}
