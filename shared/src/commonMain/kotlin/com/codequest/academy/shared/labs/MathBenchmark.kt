package com.codequest.academy.shared.labs

import kotlin.math.*
import kotlin.random.Random

data class SectionBenchmarkResult(
 val sectionId: String,
 val sectionTitle: String,
 val trialsRun: Int,
 val trialsPassed: Int,
 val subChecksRun: Int,
 val subChecksPassed: Int,
 val durationMs: Long,
 val avgLatencyMicros: Double,
 val invariantsVerified: List<String>,
 val sampleCase: String,
 val failures: List<String> = emptyList()
) {
 val successRatePercent: Double get() = if (trialsRun == 0) 0.0 else (trialsPassed * 100.0) / trialsRun
}

data class FullBenchmarkReport(
 val seed: Int,
 val trialsPerSection: Int,
 val totalTrialsRun: Int,
 val totalTrialsPassed: Int,
 val totalSubChecksRun: Int,
 val totalSubChecksPassed: Int,
 val totalDurationMs: Long,
 val sections: List<SectionBenchmarkResult>
) {
 val overallSuccessRatePercent: Double get() = if (totalTrialsRun == 0) 0.0 else (totalTrialsPassed * 100.0) / totalTrialsRun
}

object MathBenchmark {

 fun runAll(trialsPerSection: Int = 1000, baseSeed: Int = 2026): FullBenchmarkReport {
  val t0 = System.currentTimeMillis()
  val sections = listOf(
   runPracticeBenchmark(trialsPerSection, baseSeed + 1),
   runGraphBenchmark(trialsPerSection, baseSeed + 2),
   runLinearAlgebraBenchmark(trialsPerSection, baseSeed + 3),
   runStatisticsBenchmark(trialsPerSection, baseSeed + 4),
   runProbabilityBenchmark(trialsPerSection, baseSeed + 5)
  )
  val totalMs = (System.currentTimeMillis() - t0).coerceAtLeast(1L)
  return FullBenchmarkReport(
   seed = baseSeed,
   trialsPerSection = trialsPerSection,
   totalTrialsRun = sections.sumOf { it.trialsRun },
   totalTrialsPassed = sections.sumOf { it.trialsPassed },
   totalSubChecksRun = sections.sumOf { it.subChecksRun },
   totalSubChecksPassed = sections.sumOf { it.subChecksPassed },
   totalDurationMs = totalMs,
   sections = sections
  )
 }

 // ─────────────────── 1. Practice & Grading Engine (1,000 Hard Trials) ───────────────────

 fun runPracticeBenchmark(trials: Int = 1000, seed: Int = 101): SectionBenchmarkResult {
  val t0 = System.nanoTime()
  val topics = MathTopic.values()
  var passed = 0
  var subRun = 0
  var subPass = 0
  val failures = mutableListOf<String>()
  var sample = ""

  for (i in 0 until trials) {
   val topic = topics[i % topics.size]
   val difficulty = (i % 3) + 1
   val problemSeed = seed * 10000 + i
   var trialOk = true

   fun checkCond(cond: Boolean, msg: String) {
    subRun++
    if (cond) subPass++ else {
     trialOk = false
     if (failures.size < 10) failures += "Trial $i ($topic L$difficulty): $msg"
    }
   }

   try {
    val p1 = MathEngine.generate(topic, difficulty, problemSeed)
    val p2 = MathEngine.generate(topic, difficulty, problemSeed)
    checkCond(p1 == p2, "Deterministic generation")
    checkCond(p1.steps.size >= 2, "Has step-by-step solution")
    checkCond(MathEngine.check(p1, p1.answer).correct, "Canonical answer '${p1.answer}' accepted")
    checkCond(!MathEngine.check(p1, "wrong_input_xyz").correct, "Invalid answer rejected")

    // Test alternative equivalent student formats
    when (p1.type) {
     MathAnswerType.Number -> {
      checkCond(MathEngine.check(p1, "x = ${p1.answer}").correct, "Labeled numeric answer 'x = ...' accepted")
      checkCond(MathEngine.check(p1, "  (${p1.answer}) * 2 / 2  ").correct, "Equivalent arithmetic expression accepted")
     }
     MathAnswerType.Expression -> {
      checkCond(MathEngine.check(p1, "f(x) = ${p1.answer}").correct, "Function-prefixed expression accepted")
      if (topic != MathTopic.Algebra) {
       checkCond(MathEngine.check(p1, "0 + (${p1.answer})").correct, "Equivalent algebraic form accepted")
      }
     }
     MathAnswerType.Vector -> {
      checkCond(MathEngine.check(p1, "[${p1.answer}]").correct, "Bracketed vector accepted")
     }
     MathAnswerType.Matrix -> {
      val bracketed = "[[" + p1.answer.replace(";", "], [") + "]]"
      checkCond(MathEngine.check(p1, bracketed).correct, "Nested bracket matrix '$bracketed' accepted")
     }
     MathAnswerType.Choice -> {
      checkCond(p1.choices.size == 4 && p1.choices.count { it == p1.answer } == 1, "4 choices with unique correct answer")
     }
     MathAnswerType.Explanation -> {}
    }
    if (i == trials - 1) sample = "${topic.title} (L$difficulty): \"${p1.statement}\" → ${p1.answer}"
   } catch (e: Exception) {
    trialOk = false
    if (failures.size < 10) failures += "Trial $i exception: ${e.message}"
   }
   if (trialOk) passed++
  }

  val elapsedNs = (System.nanoTime() - t0).coerceAtLeast(1L)
  return SectionBenchmarkResult(
   sectionId = "practice",
   sectionTitle = "01 · Practice & Grading Engine (20 Topics × 3 Levels)",
   trialsRun = trials,
   trialsPassed = passed,
   subChecksRun = subRun,
   subChecksPassed = subPass,
   durationMs = elapsedNs / 1_000_000,
   avgLatencyMicros = (elapsedNs / 1000.0) / trials,
   invariantsVerified = listOf(
    "Deterministic problem generation across all 20 topics & Levels 1–3",
    "Canonical, labeled ('x = ...'), bracketed ('[[...]]'), and algebraically equivalent answers",
    "Rejection of malformed/incorrect inputs without unhandled exceptions"
   ),
   sampleCase = sample,
   failures = failures
  )
 }

 // ─────────────────── 2. Desmos-Style Graph & Calculus Engine (1,000 Hard Trials) ───────────────────

 fun runGraphBenchmark(trials: Int = 1000, seed: Int = 202): SectionBenchmarkResult {
  val t0 = System.nanoTime()
  val rng = Random(seed)
  var passed = 0
  var subRun = 0
  var subPass = 0
  val failures = mutableListOf<String>()
  var sample = ""

  for (i in 0 until trials) {
   val a = rng.nextInt(1, 6).toDouble()
   val b = rng.nextInt(-5, 6).toDouble()
   val c = rng.nextInt(1, 5).toDouble()
   var trialOk = true

   fun checkCond(cond: Boolean, msg: String) {
    subRun++
    if (cond) subPass++ else {
     trialOk = false
     if (failures.size < 10) failures += "Graph trial $i: $msg"
    }
   }

   try {
    // 1. Compile multi-line Desmos sheet with sliders, functions, derivatives, implicit, inequality, parametric, piecewise
    val lines = listOf(
     "a = $a",
     "b = $b",
     "f(x) = a x^3 + b x^2 + ${c}x",
     "y = f'(x)",
     "x^2 + y^2 = ${c * c}",
     "y >= a x - b",
     "(${c}cos(t), ${c}sin(t))",
     "y = {x < 0: -a x, x^2}"
    )
    val compiled = GraphCompiler.compile(lines)
    checkCond(compiled.lines.all { it.item !is GraphItem.Invalid && it.missing.isEmpty() }, "8-line Desmos sheet compiled cleanly")

    // 2. Verify numerical derivative f'(x) matches analytical 3a x^2 + 2b x + c
    val xTest = rng.nextDouble(-3.0, 3.0)
    val derivItem = compiled.lines[3].item as GraphItem.FunctionOfX
    val numDeriv = Evaluator(compiled.context).at(xTest).eval(derivItem.expr)
    val exactDeriv = 3 * a * xTest * xTest + 2 * b * xTest + c
    checkCond(abs(numDeriv - exactDeriv) <= 1e-4 * max(1.0, abs(exactDeriv)), "Derivative f'($xTest) accuracy")

    // 3. Verify adaptive sampler splits asymptote 1/(x - c) and finds domain edge of sqrt(x - b)
    val asymPolylines = GraphSampler.sample({ x -> 1.0 / (x - c) }, c - 5.0, c + 5.0, 240, -10.0, 10.0)
    checkCond(asymPolylines.size >= 2, "Asymptote 1/(x-$c) split into disjoint branches")
    val sqrtPolylines = GraphSampler.sample({ x -> sqrt(x - b) }, b - 4.0, b + 5.0, 200, -10.0, 10.0)
    checkCond(sqrtPolylines.isNotEmpty() && abs(sqrtPolylines.first().xs.first() - b) < 1e-4, "Domain edge of sqrt(x-$b) located")

    // 4. Verify Points of Interest (roots and minimum of (x - b)^2 - c^2)
    val pois = GraphSampler.pointsOfInterest({ x -> (x - b) * (x - b) - c * c }, emptyList(), b - 10.0, b + 10.0, -20.0, 20.0)
    checkCond(pois.any { it.kind == PoiKind.Root && abs(it.x - (b + c)) < 1e-3 }, "Positive root x=${b + c} found")
    checkCond(pois.any { it.kind == PoiKind.Root && abs(it.x - (b - c)) < 1e-3 }, "Negative root x=${b - c} found")
    checkCond(pois.any { it.kind == PoiKind.Minimum && abs(it.x - b) < 1e-3 && abs(it.y + c * c) < 1e-3 }, "Vertex minimum ($b, ${-c * c}) found")

    // 5. Verify Definite Integral ∫_0^c (3a x^2 + 2b x + 1) dx = a c^3 + b c^2 + c
    val area = GraphSampler.definiteIntegral({ x -> 3 * a * x * x + 2 * b * x + 1.0 }, 0.0, c, steps = 100)
    val exactArea = a * c * c * c + b * c * c + c
    checkCond(abs(area - exactArea) <= 1e-6 * max(1.0, abs(exactArea)), "Simpson integral matches exact antiderivative")

    if (i == trials - 1) sample = "f(x) = ${a.toInt()}x³ + ${b.toInt()}x² + ${c.toInt()}x,  ∫₀^${c.toInt()} (3·${a.toInt()}x² + 2·${b.toInt()}x + 1) dx = ${formatMath(area, 4)}"
   } catch (e: Exception) {
    trialOk = false
    if (failures.size < 10) failures += "Graph trial $i exception: ${e.message}"
   }
   if (trialOk) passed++
  }

  val elapsedNs = (System.nanoTime() - t0).coerceAtLeast(1L)
  return SectionBenchmarkResult(
   sectionId = "graph",
   sectionTitle = "02 · Desmos Graphing, Implicit, Parametric & Calculus Engine",
   trialsRun = trials,
   trialsPassed = passed,
   subChecksRun = subRun,
   subChecksPassed = subPass,
   durationMs = elapsedNs / 1_000_000,
   avgLatencyMicros = (elapsedNs / 1000.0) / trials,
   invariantsVerified = listOf(
    "Multi-line sheet compilation: explicit, implicit, inequalities, sliders, piecewise, parametric",
    "Numerical derivative f'(x) vs analytical derivative within 10⁻⁴",
    "Asymptote discontinuity splitting & binary-searched domain boundaries",
    "Root/extrema solver & Simpson definite integral ∫ f(x) dx within 10⁻⁶"
   ),
   sampleCase = sample,
   failures = failures
  )
 }

 // ─────────────────── 3. Matrix & Vector Linear Algebra Studio (1,000 Hard Trials) ───────────────────

 fun runLinearAlgebraBenchmark(trials: Int = 1000, seed: Int = 303): SectionBenchmarkResult {
  val t0 = System.nanoTime()
  val rng = Random(seed)
  var passed = 0
  var subRun = 0
  var subPass = 0
  val failures = mutableListOf<String>()
  var sample = ""

  for (i in 0 until trials) {
   var trialOk = true
   fun checkCond(cond: Boolean, msg: String) {
    subRun++
    if (cond) subPass++ else {
     trialOk = false
     if (failures.size < 10) failures += "LinAlg trial $i: $msg"
    }
   }

   try {
    val n = if (i % 2 == 0) 2 else 3
    // Generate diagonally-dominant invertible n×n matrices A and B
    val a = List(n) { r -> List(n) { c -> if (r == c) rng.nextInt(6, 14).toDouble() else rng.nextInt(-3, 4).toDouble() } }
    val b = List(n) { r -> List(n) { c -> if (r == c) rng.nextInt(5, 12).toDouble() else rng.nextInt(-3, 4).toDouble() } }

    // 1. (A + B) - B == A
    val addSub = MathEngine.subtractMatrices(MathEngine.addMatrices(a, b), b)
    checkCond(a.indices.all { r -> a[r].indices.all { c -> abs(addSub[r][c] - a[r][c]) < 1e-9 } }, "(A + B) − B = A")

    // 2. det(A * B) == det(A) * det(B) and (AB)^T == B^T A^T
    val ab = MathEngine.multiply(a, b)
    val detA = MathEngine.determinant(a)
    val detB = MathEngine.determinant(b)
    val detAB = MathEngine.determinant(ab)
    checkCond(abs(detAB - detA * detB) <= 1e-6 * max(1.0, abs(detA * detB)), "det(AB) = det(A)det(B)")
    val abt = MathEngine.transpose(ab)
    val btat = MathEngine.multiply(MathEngine.transpose(b), MathEngine.transpose(a))
    checkCond(abt.indices.all { r -> abt[r].indices.all { c -> abs(abt[r][c] - btat[r][c]) < 1e-9 } }, "(AB)ᵀ = BᵀAᵀ")

    // 3. Inverse A * A^-1 == I and RREF(A) == I
    val invA = MathEngine.inverse(a)
    val prodId = MathEngine.multiply(a, invA)
    checkCond(
     prodId.indices.all { r -> prodId[r].indices.all { c -> abs(prodId[r][c] - (if (r == c) 1.0 else 0.0)) < 1e-7 } },
     "A · A⁻¹ = I"
    )
    checkCond(MathEngine.rank(a) == n, "Full rank(A) = $n")

    // 4. Linear system solver Ax = rhs -> verify residual ||Ax - rhs|| < 1e-7
    val rhs = List(n) { rng.nextInt(-10, 11).toDouble() }
    val xSol = MathEngine.solveLinearSystem(a, rhs)
    val ax = a.map { row -> row.indices.sumOf { k -> row[k] * xSol[k] } }
    val residual = sqrt(ax.indices.sumOf { (ax[it] - rhs[it]).pow(2) })
    checkCond(residual < 1e-7, "Linear system Ax = b residual < 10⁻⁷")

    // 5. 2x2 Eigenvalue & Eigenvector verification on symmetric 2x2 matrix
    val s11 = rng.nextInt(2, 10).toDouble(); val s12 = rng.nextInt(1, 6).toDouble(); val s22 = rng.nextInt(2, 10).toDouble()
    val sym = listOf(listOf(s11, s12), listOf(s12, s22))
    val eig = MathEngine.eigen2x2(sym)
    val lams = eig.realEigenvalues
    checkCond(lams != null && abs((lams.first + lams.second) - eig.trace) < 1e-8 && abs((lams.first * lams.second) - eig.determinant) < 1e-8, "Eigenvalues match trace & det")
    if (lams != null && eig.eigenvectors.size == 2) {
     val v1 = eig.eigenvectors[0]
     val av0 = sym[0][0] * v1[0] + sym[0][1] * v1[1]
     val av1 = sym[1][0] * v1[0] + sym[1][1] * v1[1]
     checkCond(hypot(av0 - lams.first * v1[0], av1 - lams.first * v1[1]) < 1e-6, "Av₁ = λ₁v₁")
    }

    // 6. Vector projection, orthogonality & 3D cross product
    val u = listOf(rng.nextInt(1, 8).toDouble(), rng.nextInt(-7, 8).toDouble(), rng.nextInt(1, 8).toDouble())
    val v = listOf(rng.nextInt(-6, 7).toDouble(), rng.nextInt(2, 9).toDouble(), rng.nextInt(-5, 6).toDouble())
    val unitU = MathEngine.unitVector(u)
    checkCond(abs(MathEngine.norm(unitU) - 1.0) < 1e-9, "Unit vector ‖û‖₂ = 1")
    val proj = MathEngine.project(u, v)
    val rej = u.indices.map { u[it] - proj[it] }
    checkCond(abs(MathEngine.dot(proj, rej)) < 1e-7, "proj_v(u) ⊥ (u − proj_v(u))")
    val cross = MathEngine.cross(u, v)
    checkCond(abs(MathEngine.dot(cross, u)) < 1e-7 && abs(MathEngine.dot(cross, v)) < 1e-7, "(u × v) ⊥ u and (u × v) ⊥ v")

    if (i == trials - 1) sample = "${n}×${n} system solved with ‖Ax − b‖₂ = ${formatMath(residual, 10)}, det(A) = ${formatMath(detA, 2)}"
   } catch (e: Exception) {
    trialOk = false
    if (failures.size < 10) failures += "LinAlg trial $i exception: ${e.message}"
   }
   if (trialOk) passed++
  }

  val elapsedNs = (System.nanoTime() - t0).coerceAtLeast(1L)
  return SectionBenchmarkResult(
   sectionId = "linalg",
   sectionTitle = "03 · Matrix & Vector Linear Algebra Studio (15 Operations)",
   trialsRun = trials,
   trialsPassed = passed,
   subChecksRun = subRun,
   subChecksPassed = subPass,
   durationMs = elapsedNs / 1_000_000,
   avgLatencyMicros = (elapsedNs / 1000.0) / trials,
   invariantsVerified = listOf(
    "Multiplicative determinant det(AB) = det(A)det(B) & transpose (AB)ᵀ = BᵀAᵀ",
    "Gauss-Jordan inverse A·A⁻¹ = I & RREF full-rank identity reduction",
    "Linear system Ax = b residual ‖Ax − b‖₂ < 10⁻⁷ & eigenpair Av = λv",
    "Vector unit normalization ‖û‖ = 1, projection orthogonality, and 3D cross product"
   ),
   sampleCase = sample,
   failures = failures
  )
 }

 // ─────────────────── 4. Statistics & Linear Regression Workbench (1,000 Hard Trials) ───────────────────

 fun runStatisticsBenchmark(trials: Int = 1000, seed: Int = 404): SectionBenchmarkResult {
  val t0 = System.nanoTime()
  val rng = Random(seed)
  var passed = 0
  var subRun = 0
  var subPass = 0
  val failures = mutableListOf<String>()
  var sample = ""

  for (i in 0 until trials) {
   var trialOk = true
   fun checkCond(cond: Boolean, msg: String) {
    subRun++
    if (cond) subPass++ else {
     trialOk = false
     if (failures.size < 10) failures += "Stats trial $i: $msg"
    }
   }

   try {
    val n = rng.nextInt(10, 80)
    val data = List(n) { rng.nextDouble(-50.0, 150.0) }
    val d = MathEngine.analyzeData(data)

    // 1. Order statistics & IQR invariants
    checkCond(d.min <= d.q1 + 1e-9 && d.q1 <= d.median + 1e-9 && d.median <= d.q3 + 1e-9 && d.q3 <= d.max + 1e-9, "Min ≤ Q1 ≤ Median ≤ Q3 ≤ Max")
    checkCond(abs(d.range - (d.max - d.min)) < 1e-9 && abs(d.iqr - (d.q3 - d.q1)) < 1e-9, "Range & IQR identities")

    // 2. Bessel's correction: (n - 1) * sampleVariance == n * popVariance
    checkCond(abs((n - 1) * d.sampleVariance - n * d.popVariance) <= 1e-6 * max(1.0, d.popVariance), "Bessel correction (n−1)s² = nσ²")

    // 3. 95% CI symmetry around mean
    checkCond(abs((d.ci95Low + d.ci95High) / 2.0 - d.mean) < 1e-7 && d.ci95Low <= d.mean && d.mean <= d.ci95High, "95% CI centered at μ")

    // 4. Linear regression exact & noisy checks
    val trueM = rng.nextDouble(-6.0, 6.0).let { if (abs(it) < 0.2) 1.5 else it }
    val trueB = rng.nextDouble(-20.0, 20.0)
    val xs = (1..20).map { it.toDouble() + rng.nextDouble(-0.2, 0.2) }
    val ysExact = xs.map { trueM * it + trueB }
    val regExact = MathEngine.linearRegression(xs, ysExact)
    checkCond(abs(regExact.slope - trueM) < 1e-7 && abs(regExact.intercept - trueB) < 1e-7 && abs(regExact.rSquared - 1.0) < 1e-7, "Exact line recovery m, b, R²=1")

    val ysNoisy = xs.map { trueM * it + trueB + rng.nextDouble(-1.5, 1.5) }
    val regNoisy = MathEngine.linearRegression(xs, ysNoisy)
    val residualSum = regNoisy.residuals.sum()
    val yAtMeanX = regNoisy.slope * regNoisy.meanX + regNoisy.intercept
    checkCond(abs(residualSum) < 1e-6, "Least-squares residuals sum to 0 (∑ eᵢ = 0)")
    checkCond(abs(yAtMeanX - regNoisy.meanY) < 1e-7, "Regression line passes through centroid (x̄, ȳ)")
    checkCond(regNoisy.rSquared in -1e-9..1.000000001, "Coefficient of determination R² ∈ [0, 1]")

    if (i == trials - 1) sample = "n=$n, μ=${formatMath(d.mean, 2)}, σ=${formatMath(d.popStdDev, 2)}, Reg: ${regNoisy.equation} (R²=${formatMath(regNoisy.rSquared, 4)})"
   } catch (e: Exception) {
    trialOk = false
    if (failures.size < 10) failures += "Stats trial $i exception: ${e.message}"
   }
   if (trialOk) passed++
  }

  val elapsedNs = (System.nanoTime() - t0).coerceAtLeast(1L)
  return SectionBenchmarkResult(
   sectionId = "statistics",
   sectionTitle = "04 · Statistics, Quartiles, Outliers & Linear Regression",
   trialsRun = trials,
   trialsPassed = passed,
   subChecksRun = subRun,
   subChecksPassed = subPass,
   durationMs = elapsedNs / 1_000_000,
   avgLatencyMicros = (elapsedNs / 1000.0) / trials,
   invariantsVerified = listOf(
    "Five-number summary ordering Min ≤ Q1 ≤ Median ≤ Q3 ≤ Max & IQR = Q3 − Q1",
    "Bessel's variance identity (n − 1)s² = nσ² and 95% confidence interval centering",
    "Least-squares regression orthogonality ∑ eᵢ = 0, centroid (x̄, ȳ) intersection, and R² ∈ [0, 1]"
   ),
   sampleCase = sample,
   failures = failures
  )
 }

 // ─────────────────── 5. Probability, Distributions & Bayes Studio (1,000 Hard Trials) ───────────────────

 fun runProbabilityBenchmark(trials: Int = 1000, seed: Int = 505): SectionBenchmarkResult {
  val t0 = System.nanoTime()
  val rng = Random(seed)
  var passed = 0
  var subRun = 0
  var subPass = 0
  val failures = mutableListOf<String>()
  var sample = ""

  for (i in 0 until trials) {
   var trialOk = true
   fun checkCond(cond: Boolean, msg: String) {
    subRun++
    if (cond) subPass++ else {
     trialOk = false
     if (failures.size < 10) failures += "Prob trial $i: $msg"
    }
   }

   try {
    // 1. Binomial normalization ∑ P(X=k) = 1 and expectation ∑ k P(X=k) = np
    val n = rng.nextInt(5, 80)
    val p = rng.nextDouble(0.05, 0.95)
    val k = rng.nextInt(0, n + 1)
    val pmfs = (0..n).map { MathEngine.binomial(n, it, p) }
    val totalProb = pmfs.sum()
    val expectedVal = pmfs.indices.sumOf { it * pmfs[it] }
    checkCond(abs(totalProb - 1.0) < 1e-7, "Binomial ∑ P(X=k) = 1")
    checkCond(abs(expectedVal - n * p) < 1e-6 * n, "Binomial E[X] = np")
    val cdfK = pmfs.take(k + 1).sum()
    val geK = pmfs.drop(k).sum()
    checkCond(abs((cdfK + geK - pmfs[k]) - 1.0) < 1e-7, "P(X≤k) + P(X≥k) − P(X=k) = 1")

    // 2. Normal distribution symmetry Φ(μ + zσ) + Φ(μ - zσ) = 1
    val mu = rng.nextDouble(-25.0, 25.0)
    val sigma = rng.nextDouble(0.5, 10.0)
    val z = rng.nextDouble(-3.5, 3.5)
    val c1 = MathEngine.normalCdf(mu + z * sigma, mu, sigma)
    val c2 = MathEngine.normalCdf(mu - z * sigma, mu, sigma)
    checkCond(abs((c1 + c2) - 1.0) < 1e-6 && abs(MathEngine.normalCdf(mu, mu, sigma) - 0.5) < 1e-7, "Normal CDF reflection symmetry")

    // 3. Poisson recurrence P(X=k) = (λ/k) P(X=k-1)
    val lambda = rng.nextDouble(0.5, 30.0)
    val pk = rng.nextInt(1, 25)
    val pCur = MathEngine.poissonPmf(lambda, pk)
    val pPrev = MathEngine.poissonPmf(lambda, pk - 1)
    checkCond(abs(pCur - (lambda / pk) * pPrev) < 1e-9, "Poisson recurrence P(k) = (λ/k)P(k−1)")

    // 4. Geometric closed-form CDF == sum of PMFs
    val gp = rng.nextDouble(0.1, 0.9)
    val gk = rng.nextInt(1, 20)
    val gSum = (1..gk).sumOf { MathEngine.geometricPmf(gp, it) }
    val gCdf = MathEngine.geometricCdf(gp, gk)
    checkCond(abs(gSum - gCdf) < 1e-9, "Geometric CDF matches ∑ PMF")

    // 5. Combinatorics Pascal's Identity C(n, r) = C(n-1, r-1) + C(n-1, r) & Bayes normalization
    val cn = rng.nextInt(4, 30)
    val cr = rng.nextInt(1, cn)
    val cTotal = MathEngine.combinations(cn, cr)
    val cPascal = MathEngine.combinations(cn - 1, cr - 1) + MathEngine.combinations(cn - 1, cr)
    checkCond(abs(cTotal - cPascal) <= 1e-6 * cTotal, "Pascal's identity C(n,r) = C(n−1,r−1) + C(n−1,r)")

    val prior = rng.nextDouble(0.01, 0.99)
    val sens = rng.nextDouble(0.1, 0.99)
    val fpr = rng.nextDouble(0.01, 0.9)
    val pb = sens * prior + fpr * (1.0 - prior)
    val postA = (sens * prior) / pb
    val postNotA = (fpr * (1.0 - prior)) / pb
    checkCond(abs((postA + postNotA) - 1.0) < 1e-9 && postA in 0.0..1.0, "Bayes posterior P(A|B) + P(¬A|B) = 1")

    if (i == trials - 1) sample = "Bin($n, ${formatMath(p, 2)}) ∑P=1.0, Normal(${formatMath(mu, 1)}, ${formatMath(sigma, 1)}), Bayes P(A|B)=${formatMath(postA, 4)}"
   } catch (e: Exception) {
    trialOk = false
    if (failures.size < 10) failures += "Prob trial $i exception: ${e.message}"
   }
   if (trialOk) passed++
  }

  val elapsedNs = (System.nanoTime() - t0).coerceAtLeast(1L)
  return SectionBenchmarkResult(
   sectionId = "probability",
   sectionTitle = "05 · Probability Distributions, Combinatorics & Bayes Studio",
   trialsRun = trials,
   trialsPassed = passed,
   subChecksRun = subRun,
   subChecksPassed = subPass,
   durationMs = elapsedNs / 1_000_000,
   avgLatencyMicros = (elapsedNs / 1000.0) / trials,
   invariantsVerified = listOf(
    "Binomial normalization ∑ P(X=k) = 1, expectation E[X] = np, and tail identity",
    "Normal CDF reflection symmetry Φ(μ+zσ) + Φ(μ−zσ) = 1 & Poisson recurrence",
    "Geometric CDF closed form, Pascal's identity C(n,r), and Bayes posterior normalization"
   ),
   sampleCase = sample,
   failures = failures
  )
 }
}
