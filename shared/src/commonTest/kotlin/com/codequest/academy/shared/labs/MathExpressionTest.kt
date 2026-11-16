package com.codequest.academy.shared.labs

import kotlin.math.*
import kotlin.test.*

class MathExpressionTest {
 private fun v(s: String, x: Double? = null): Double { val ev = Evaluator(); if (x != null) ev.at(x); return ev.eval(MathParser.parse(s)) }

 @Test fun implicitMultiplicationAndFunctions() {
  assertEquals(6.0, v("2x", 3.0))
  assertEquals(2 * sin(1.0), v("2sin(x)", 1.0), 1e-12)
  assertEquals(sin(2.0), v("sin 2x", 1.0), 1e-12)
  assertEquals(sin(1.0) * cos(1.0), v("sin x cos x", 1.0), 1e-12)
  assertEquals(8.0, v("(x+1)(x-1)", 3.0))
  assertEquals(2 * PI, v("2pi"), 1e-12)
  assertEquals(E, v("e"), 1e-12)
  assertEquals(3.0, v("log_2(8)"), 1e-12)
  assertEquals(2.0, v("log(100)"), 1e-12)
  assertEquals(5.0, v("|x-2|", -3.0))
  assertEquals(10.0, v("2|x|", -5.0))
  assertEquals(120.0, v("5!"), 1e-9)
  assertEquals(-2.0, v("(-8)^(1/3)"), 1e-12)
  assertEquals(sin(1.0).pow(2), v("sin^2(x)", 1.0), 1e-12)
  assertEquals(asin(0.5), v("sin^-1(0.5)"), 1e-12)
  assertEquals(4.0, v("x²", 2.0))
  assertEquals(-1.0, v("−1"))
  assertEquals(1.0, v("mod(7, 3)"))
  assertEquals(3.0, v("max(1, 3, 2)"))
 }

 @Test fun friendlyErrors() {
  assertFailsWith<MathSyntaxException> { MathParser.parse("2+") }
  assertFailsWith<MathSyntaxException> { MathParser.parse("(x+1") }
  assertFailsWith<MathSyntaxException> { MathParser.parse("x+1)") }
  assertFailsWith<MathSyntaxException> { MathParser.parse("sin()") }
  assertFailsWith<MathSyntaxException> { MathParser.parse("x $ 2") }
  assertTrue(Evaluator().eval(MathParser.parse("sqrt(-1)")).isNaN())
 }

 @Test fun piecewise() {
  assertEquals(3.0, v("{x < 0: -x, x^2}", -3.0))
  assertEquals(4.0, v("{x < 0: -x, x^2}", 2.0))
  assertTrue(v("x {x > 0}", -1.0).isNaN())
  assertEquals(2.0, v("x {0 < x < 5}", 2.0))
 }

 @Test fun graphCompilerClassifiesLines() {
  val g = GraphCompiler.compile(listOf(
   "y = 2x + 1", "x^2 + y^2 = 25", "y < x^2", "x = 3", "a = 2", "y = a x", "f(x) = x^2", "y = f(x - 1)", "y = f'(x)",
   "(1, 2)", "2^10", "y = b x", "sin(x)", "y = ", "1/x", "x = y^2", "y = sqrt(x)", "y = ln(x - 2)", "y = 1/(x - 1)"
  ))
  val items = g.lines.map { it.item }
  assertIs<GraphItem.FunctionOfX>(items[0])
  assertIs<GraphItem.Implicit>(items[1])
  assertEquals(Relation.Lt, (items[2] as GraphItem.FunctionOfX).relation)
  assertIs<GraphItem.FunctionOfY>(items[3])
  assertEquals(GraphItem.Slider("a", 2.0), items[4])
  assertIs<GraphItem.FunctionOfX>(items[5]); assertTrue(g.lines[5].missing.isEmpty())
  assertNotNull((items[6] as GraphItem.Definition).plot)
  assertIs<GraphItem.FunctionOfX>(items[7])
  val d = items[8] as GraphItem.FunctionOfX
  assertEquals(6.0, Evaluator(g.context).at(3.0).eval(d.expr), 1e-4)
  assertEquals(GraphItem.Point(1.0, 2.0), items[9])
  assertEquals(GraphItem.Value(1024.0), items[10])
  assertEquals(listOf("b"), g.lines[11].missing)
  assertIs<GraphItem.FunctionOfX>(items[12])
  assertIs<GraphItem.Invalid>(items[13])
  assertIs<GraphItem.FunctionOfX>(items[14])
  assertIs<GraphItem.FunctionOfY>(items[15])
  // These used to be rejected by the old "Plot" check at x = 1
  for (i in 16..18) assertIs<GraphItem.FunctionOfX>(items[i], "line $i")
 }

 @Test fun samplerBreaksAtAsymptotes() {
  val lines = GraphSampler.sample({ 1 / it }, -10.0, 10.0, 400, -10.0, 10.0)
  assertTrue(lines.size >= 2, "1/x must be split at x = 0")
  assertTrue(lines.none { l -> (1 until l.size).any { l.xs[it - 1] < 0 && l.xs[it] > 0 } })
  val sqrtLines = GraphSampler.sample({ sqrt(it) }, -10.0, 10.0, 400, -10.0, 10.0)
  assertEquals(1, sqrtLines.size)
  assertTrue(sqrtLines[0].xs.first() < 1e-6, "sqrt must start at its domain edge")
 }

 @Test fun pointsOfInterest() {
  val pois = GraphSampler.pointsOfInterest({ it * it - 4 }, emptyList(), -10.0, 10.0, -10.0, 10.0)
  assertTrue(pois.any { it.kind == PoiKind.Root && abs(it.x - 2) < 1e-6 })
  assertTrue(pois.any { it.kind == PoiKind.Root && abs(it.x + 2) < 1e-6 })
  assertTrue(pois.any { it.kind == PoiKind.Minimum && abs(it.x) < 1e-4 && abs(it.y + 4) < 1e-6 })
 }

 @Test fun answerCheckingIsForgiving() {
  val eq = MathEngine.generate(MathTopic.Equations, 1, 3)
  assertTrue(MathEngine.check(eq, "x = ${eq.answer}").correct)
  val d = MathEngine.generate(MathTopic.Derivatives, 1, 1)
  val parts = d.answer.split("x+")
  assertTrue(MathEngine.check(d, "${parts[1]} + ${parts[0]}*x").correct)
  val stats = MathEngine.generate(MathTopic.Statistics, 1, 1)
  val value = ExpressionParser(stats.answer).value()
  assertTrue(MathEngine.check(stats, formatMath(value, 3)).correct)
  assertEquals(listOf(listOf(1.0, 2.0), listOf(3.0, 4.0)), MathEngine.matrix("[[1, 2], [3, 4]]"))
  assertEquals(listOf(listOf(1.0, 2.0), listOf(3.0, 4.0)), MathEngine.matrix("[1, 2], [3, 4]"))
  assertEquals(listOf(1.0, 2.0, 3.0), MathEngine.numberList("1, 2  3;"))
  assertEquals("0.3", formatMath(0.1 + 0.2))
 }

 @Test fun linearAlgebraAndProbabilitySuite() {
  val a = MathEngine.matrix("1, 2; 3, 4")
  assertEquals(-2.0, MathEngine.determinant(a), 1e-9)
  assertEquals(5.0, MathEngine.trace(a), 1e-9)
  assertEquals(2, MathEngine.rank(a))
  val inv = MathEngine.inverse(a)
  val id = MathEngine.multiply(a, inv)
  assertEquals(1.0, id[0][0], 1e-9)
  assertEquals(0.0, id[0][1], 1e-9)
  assertEquals(0.0, id[1][0], 1e-9)
  assertEquals(1.0, id[1][1], 1e-9)
  val sol = MathEngine.solveLinearSystem(a, listOf(5.0, 11.0))
  assertEquals(1.0, sol[0], 1e-9)
  assertEquals(2.0, sol[1], 1e-9)
  val eig = MathEngine.eigen2x2(MathEngine.matrix("4, 0; 0, 2"))
  assertEquals(4.0 to 2.0, eig.realEigenvalues)
  assertEquals(listOf(0.0, 0.0, 1.0), MathEngine.cross(listOf(1.0, 0.0, 0.0), listOf(0.0, 1.0, 0.0)))
  assertEquals(90.0, MathEngine.angleDegrees(listOf(1.0, 0.0), listOf(0.0, 1.0)), 1e-9)
  assertEquals(listOf(3.0, 0.0), MathEngine.project(listOf(3.0, 4.0), listOf(1.0, 0.0)))

  val reg = MathEngine.linearRegression(listOf(1.0, 2.0, 3.0, 4.0), listOf(3.0, 5.0, 7.0, 9.0))
  assertEquals(2.0, reg.slope, 1e-9)
  assertEquals(1.0, reg.intercept, 1e-9)
  assertEquals(1.0, reg.rSquared, 1e-9)

  assertEquals(0.5, MathEngine.normalCdf(0.0, 0.0, 1.0), 1e-6)
  assertEquals(120.0, MathEngine.combinations(10, 3), 1e-9)
  assertEquals(720.0, MathEngine.permutations(10, 3), 1e-9)
  assertEquals(8.0 / 3.0, GraphSampler.definiteIntegral({ it * it }, 0.0, 2.0), 1e-6)

  val param = GraphCompiler.compile(listOf("(cos(t), sin(t))"))
  assertIs<GraphItem.Parametric>(param.lines[0].item)
 }
}
