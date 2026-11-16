package com.codequest.academy.shared.labs

import kotlin.math.*
import kotlin.random.Random

enum class MathTopicCategory(val title: String) {
 All("All Topics"),
 AlgebraAndFunctions("Algebra & Functions"),
 LinearAlgebra("Linear Algebra"),
 CalculusAndOpt("Calculus & Optimization"),
 StatsAndProb("Statistics & Probability")
}

enum class MathTopic(val title: String, val category: MathTopicCategory) {
 Algebra("Algebra", MathTopicCategory.AlgebraAndFunctions),
 Equations("Equations", MathTopicCategory.AlgebraAndFunctions),
 Inequalities("Inequalities", MathTopicCategory.AlgebraAndFunctions),
 Functions("Functions and graphs", MathTopicCategory.AlgebraAndFunctions),
 Exponents("Exponents and logarithms", MathTopicCategory.AlgebraAndFunctions),
 Summation("Sequences and summation", MathTopicCategory.AlgebraAndFunctions),
 Statistics("Statistics", MathTopicCategory.StatsAndProb),
 Probability("Probability", MathTopicCategory.StatsAndProb),
 Vectors("Vectors", MathTopicCategory.LinearAlgebra),
 Matrices("Matrices", MathTopicCategory.LinearAlgebra),
 MatrixProduct("Matrix multiplication", MathTopicCategory.LinearAlgebra),
 LinearSystems("Linear systems", MathTopicCategory.LinearAlgebra),
 Eigenvectors("Eigenvectors", MathTopicCategory.LinearAlgebra),
 Svd("SVD", MathTopicCategory.LinearAlgebra),
 Pca("PCA", MathTopicCategory.LinearAlgebra),
 Derivatives("Derivatives", MathTopicCategory.CalculusAndOpt),
 Gradients("Gradients", MathTopicCategory.CalculusAndOpt),
 ChainRule("Chain rule", MathTopicCategory.CalculusAndOpt),
 Optimization("Optimization", MathTopicCategory.CalculusAndOpt),
 Loss("Loss functions", MathTopicCategory.CalculusAndOpt)
}

enum class MathAnswerType { Number, Expression, Choice, Vector, Matrix, Explanation }

data class MathProblem(
 val id: String,
 val topic: MathTopic,
 val difficulty: Int,
 val statement: String,
 val formula: String,
 val answer: String,
 val type: MathAnswerType,
 val steps: List<String>,
 val choices: List<String> = emptyList(),
 val graph: String? = null
) {
 val tolerance = 1e-6
}

data class MathVerdict(val correct: Boolean, val message: String)

data class DataSummary(
 val n: Int,
 val sum: Double,
 val mean: Double,
 val median: Double,
 val modes: List<Double>,
 val geometricMean: Double,
 val harmonicMean: Double,
 val popVariance: Double,
 val popStdDev: Double,
 val sampleVariance: Double,
 val sampleStdDev: Double,
 val stdError: Double,
 val min: Double,
 val q1: Double,
 val q2: Double,
 val q3: Double,
 val max: Double,
 val range: Double,
 val iqr: Double,
 val skewness: Double,
 val excessKurtosis: Double,
 val ci95Low: Double,
 val ci95High: Double,
 val outliers: List<Double>,
 val sorted: List<Double>
)

data class RegressionSummary(
 val n: Int,
 val slope: Double,
 val intercept: Double,
 val r: Double,
 val rSquared: Double,
 val covariance: Double,
 val meanX: Double,
 val meanY: Double,
 val equation: String,
 val points: List<Pair<Double, Double>>,
 val residuals: List<Double>
)

data class Eigen2x2Result(
 val trace: Double,
 val determinant: Double,
 val discriminant: Double,
 val realEigenvalues: Pair<Double, Double>?,
 val complexEigenvalueText: String?,
 val eigenvectors: List<List<Double>>
)

object MathEngine {
 fun generate(topic: MathTopic, difficulty: Int, seed: Int): MathProblem {
  require(difficulty in 1..3) { "Difficulty must be 1, 2 or 3" }
  val r = Random(seed)
  val a = r.nextInt(2, 5 + difficulty)
  val b = r.nextInt(1, 5 + difficulty)
  val c = r.nextInt(1, 5)
  val d = r.nextInt(2, 4 + difficulty)
  fun p(
   text: String,
   formula: String,
   answer: String,
   type: MathAnswerType = MathAnswerType.Number,
   steps: List<String>,
   choices: List<String> = emptyList(),
   graph: String? = null
  ) = MathProblem("${topic.name}:$difficulty:$seed", topic, difficulty, text, formula, answer, type, steps, choices, graph)

  return when (topic) {
   MathTopic.Algebra -> when (difficulty) {
    1 -> p("Expand $a(x + $b).", "a(b + c) = ab + ac", "${a}x+${a * b}", MathAnswerType.Expression, listOf("Distribute $a to x and $b.", "${a}x + ${a * b}"), graph = "$a(x+$b)")
    2 -> p("Expand and simplify $a(x + $b) + $c(x - $d).", "a(x + b) + c(x - d) = (a + c)x + (ab - cd)", "${a + c}x+${a * b - c * d}", MathAnswerType.Expression, listOf("Expand first term: ${a}x + ${a * b}. Expand second term: ${c}x - ${c * d}.", "Combine like terms: (${a} + $c)x + (${a * b} - ${c * d}) = ${a + c}x + (${a * b - c * d})."), graph = "$a(x+$b)+$c(x-$d)")
    else -> p("Expand (x + $a)(x + $b).", "(x + a)(x + b) = x² + (a + b)x + ab", "x^2+${a + b}x+${a * b}", MathAnswerType.Expression, listOf("Multiply each term in the first bracket by each term in the second: x² + ${b}x + ${a}x + ${a * b}.", "Combine middle terms: x² + ${a + b}x + ${a * b}."), graph = "(x+$a)(x+$b)")
   }
   MathTopic.Equations -> when (difficulty) {
    1 -> p("Solve ${a}x + $b = ${a * c + b}.", "ax + b = d  ⇒  x = (d − b)/a", "$c", steps = listOf("Subtract $b from both sides: ${a}x = ${a * c}.", "Divide by $a: x = $c."), graph = "${a}x+$b")
    2 -> p("Solve ${a + c}x - $b = ${a}x + ${c * d - b}.", "ax + b = cx + d  ⇒  (a − c)x = d − b", "$d", steps = listOf("Subtract ${a}x from both sides: ${c}x - $b = ${c * d - b}.", "Add $b: ${c}x = ${c * d}, so x = $d."), graph = "${c}x-${c * d}")
    else -> p("Find the positive root of x² - ${a * a} = 0.", "x² = a²  ⇒  x = ±a", "$a", steps = listOf("Factor as (x - $a)(x + $a) = 0.", "The positive solution is x = $a."), graph = "x^2-${a * a}")
   }
   MathTopic.Inequalities -> when (difficulty) {
    1 -> p("Solve −${a}x < −${a * b}.", "Dividing by a negative reverses < and >.", "x > $b", MathAnswerType.Choice, listOf("Divide both sides by −$a.", "Reverse the inequality: x > $b."), listOf("x > $b", "x < $b", "x = $b", "x ≥ $b"))
    2 -> p("Solve ${a}x + $b ≥ ${a * c + b}.", "ax + b ≥ d  ⇒  x ≥ (d − b)/a (for a > 0)", "x ≥ $c", MathAnswerType.Choice, listOf("Subtract $b from both sides: ${a}x ≥ ${a * c}.", "Divide by positive $a: x ≥ $c."), listOf("x ≥ $c", "x ≤ $c", "x > $c", "x < $c"))
    else -> p("Solve $b − ${a}x ≤ ${b - a * c}.", "Subtract b, then divide by −a and flip ≤ to ≥.", "x ≥ $c", MathAnswerType.Choice, listOf("Subtract $b: −${a}x ≤ −${a * c}.", "Divide by −$a and reverse ≤: x ≥ $c."), listOf("x ≥ $c", "x ≤ $c", "x > -$c", "x < -$c"))
   }
   MathTopic.Functions -> when (difficulty) {
    1 -> p("For f(x) = ${a}x² + $b, find f($c).", "f(x) = ax² + b", "${a * c * c + b}", steps = listOf("Square $c to get ${c * c}.", "Multiply by $a and add $b: ${a * c * c + b}."), graph = "${a}x^2+$b")
    2 -> p("For f(x) = ${a}x - $b and g(x) = x² + $c, find f(g($d)).", "(f ∘ g)(x) = f(g(x))", "${a * (d * d + c) - b}", steps = listOf("First evaluate inner function: g($d) = $d² + $c = ${d * d + c}.", "Now apply f: f(${d * d + c}) = $a(${d * d + c}) - $b = ${a * (d * d + c) - b}."), graph = "$a(x^2+$c)-$b")
    else -> p("Find the x-coordinate of the vertex of f(x) = x² - ${2 * a}x + $b.", "x_vertex = −b / (2a)", "$a", steps = listOf("Here a = 1 and b = −${2 * a}.", "Vertex x-coordinate = −(−${2 * a}) / (2 × 1) = $a."), graph = "x^2-${2 * a}x+$b")
   }
   MathTopic.Exponents -> when (difficulty) {
    1 -> p("Find log₂(${1 shl a}).", "logᵦ(bⁿ) = n", "$a", steps = listOf("${1 shl a} = 2 raised to $a.", "The logarithm is the exponent: $a."), graph = "log_2(x)")
    2 -> p("Simplify and evaluate 2^$a × 2^$c.", "bᵐ × bⁿ = bᵐ⁺ⁿ", "${1 shl (a + c)}", steps = listOf("Add exponents with the same base: 2^($a + $c) = 2^${a + c}.", "2^${a + c} = ${1 shl (a + c)}."), graph = "2^x")
    else -> p("Solve for x: log₂(x) + log₂($b) = log₂(${b * c}).", "log(u) + log(v) = log(uv)", "$c", steps = listOf("Combine logs: log₂(${b}x) = log₂(${b * c}).", "Therefore ${b}x = ${b * c}, so x = $c."), graph = "log_2(${b}x)")
   }
   MathTopic.Summation -> when (difficulty) {
    1 -> p("Find 1 + 2 + … + ${a + b}.", "∑ᵢ₌₁ⁿ i = n(n + 1)/2", "${(a + b) * (a + b + 1) / 2}", steps = listOf("There are ${a + b} consecutive terms.", "(${a + b})(${a + b + 1})/2 = ${(a + b) * (a + b + 1) / 2}."))
    2 -> p("Find the sum of the arithmetic progression with ${a + 2} terms: first term $b, common difference $c.", "Sₙ = (n/2)(2a₁ + (n − 1)d)", "${(a + 2) * (2 * b + (a + 1) * c)}/2", steps = listOf("Last term aₙ = $b + ${a + 1}×$c = ${b + (a + 1) * c}.", "Sum = ${a + 2} × ($b + ${b + (a + 1) * c}) / 2 = ${formatMath((a + 2) * (2 * b + (a + 1) * c) / 2.0)}."))
    else -> p("Evaluate ∑ᵢ₌₁^$c (${a}i + $b).", "∑(ai + b) = a·n(n+1)/2 + b·n", "${a * c * (c + 1) / 2 + b * c}", steps = listOf("Split sum: $a ∑ᵢ₌₁^$c i + ∑ᵢ₌₁^$c $b.", "= $a × ${c * (c + 1) / 2} + $b × $c = ${a * c * (c + 1) / 2 + b * c}."))
   }
   MathTopic.Statistics -> when (difficulty) {
    1 -> p("Find the population variance of [$b, ${b + a}, ${b + 2 * a}].", "σ² = ∑(xᵢ − μ)² / n", "${2 * a * a}/3", steps = listOf("Mean μ = ${b + a}.", "Squared deviations are ${a * a}, 0, ${a * a}.", "Divide their sum by 3: ${2 * a * a}/3 ≈ ${formatMath(2.0 * a * a / 3, 4)}."))
    2 -> p("Find the sample variance s² (dividing by n − 1) of [$b, ${b + a}, ${b + 2 * a}].", "s² = ∑(xᵢ − x̄)² / (n − 1)", "${a * a}", steps = listOf("Sample mean x̄ = ${b + a}.", "Sum of squared deviations = ${a * a} + 0 + ${a * a} = ${2 * a * a}.", "Divide by n − 1 = 2: s² = ${a * a}."))
    else -> p("A feature has mean μ = ${b * 2} and standard deviation σ = $a. Find the Z-score of x = ${b * 2 + a * c}.", "z = (x − μ) / σ", "$c", steps = listOf("Deviation from mean: x − μ = ${b * 2 + a * c} − ${b * 2} = ${a * c}.", "Divide by σ = $a: z = ${a * c} / $a = $c."))
   }
   MathTopic.Probability -> when (difficulty) {
    1 -> p("A bag has $a white and $b black counters. What is P(white) for one uniform draw?", "P(A) = favorable outcomes / total outcomes", "$a/${a + b}", steps = listOf("There are ${a + b} equally likely counters.", "The $a white counters give probability $a/${a + b} ≈ ${formatMath(a.toDouble() / (a + b), 4)}."))
    2 -> p("Two independent events have P(A) = 1/$a and P(B) = 1/$b. Find P(A ∩ B).", "For independent events, P(A ∩ B) = P(A)P(B)", "1/${a * b}", steps = listOf("By independence, multiply the probabilities.", "P(A ∩ B) = (1/$a)(1/$b) = 1/${a * b}."))
    else -> p("In ${a + 1} fair coin flips (p = 0.5), what is the probability of getting all ${a + 1} heads?", "P(X = n) = pⁿ", "1/${1 shl (a + 1)}", steps = listOf("Each flip has probability 1/2 of heads independently.", "Multiply (1/2)^${a + 1} = 1/${1 shl (a + 1)}."))
   }
   MathTopic.Vectors -> when (difficulty) {
    1 -> p("Add vectors [$a, $b] and [$c, −$a].", "[u₁,u₂] + [v₁,v₂] = [u₁+v₁,u₂+v₂]", "${a + c},${b - a}", MathAnswerType.Vector, listOf("Add first components: $a + $c = ${a + c}.", "Add second components: $b − $a = ${b - a}."))
    2 -> p("Compute the linear combination 2[$a, $b] − [$c, $d].", "αu − v = [αu₁ − v₁, αu₂ − v₂]", "${2 * a - c},${2 * b - d}", MathAnswerType.Vector, listOf("Scale first vector: 2[$a, $b] = [${2 * a}, ${2 * b}].", "Subtract [$c, $d]: [${2 * a - c}, ${2 * b - d}]."))
    else -> p("Find the dot product of [$a, $b, $c] and [$d, −1, 2].", "u · v = ∑ uᵢvᵢ", "${a * d - b + 2 * c}", MathAnswerType.Number, listOf("Multiply corresponding components: $a×$d = ${a * d}, $b×(−1) = −$b, $c×2 = ${2 * c}.", "Sum them: ${a * d} − $b + ${2 * c} = ${a * d - b + 2 * c}."))
   }
   MathTopic.Matrices -> when (difficulty) {
    1 -> p("Transpose [[$a, $b], [$c, ${a + b}]]. Enter rows separated by semicolons.", "(Aᵀ)ᵢⱼ = Aⱼᵢ", "$a,$c;$b,${a + b}", MathAnswerType.Matrix, listOf("Rows become columns.", "Result: [[$a,$c],[$b,${a + b}]]."))
    2 -> p("Find the determinant of A = [[$a, $b], [$c, $d]].", "det([[a,b],[c,d]]) = ad − bc", "${a * d - b * c}", MathAnswerType.Number, listOf("Multiply main diagonal: $a × $d = ${a * d}.", "Subtract off-diagonal product: ${a * d} − ($b × $c) = ${a * d - b * c}."))
    else -> p("Compute 2[[$a, $b], [0, $c]] + [[1, -$b], [$d, $a]].", "(αA + B)ᵢⱼ = αAᵢⱼ + Bᵢⱼ", "${2 * a + 1},$b;$d,${2 * c + a}", MathAnswerType.Matrix, listOf("Scale first matrix by 2: [[${2 * a}, ${2 * b}], [0, ${2 * c}]].", "Add entrywise: [[${2 * a + 1}, $b], [$d, ${2 * c + a}]]."))
   }
   MathTopic.MatrixProduct -> when (difficulty) {
    1 -> p("Multiply [[$a,0],[0,$b]] by [[$c,1],[1,$c]].", "(AB)ᵢⱼ = ∑ₖ AᵢₖBₖⱼ", "${a * c},$a;$b,${b * c}", MathAnswerType.Matrix, listOf("Take each row of A dotted with each column of B.", "The diagonal scales row 1 by $a and row 2 by $b.", "Result: [[${a * c},$a],[$b,${b * c}]]."))
    2 -> p("Multiply [[1,$a],[0,1]] by [[$b,$c],[0,$d]].", "(AB)ᵢⱼ = ∑ₖ AᵢₖBₖⱼ", "$b,${c + a * d};0,$d", MathAnswerType.Matrix, listOf("Row 1: [1×$b + $a×0, 1×$c + $a×$d] = [$b, ${c + a * d}].", "Row 2: [0, $d]."))
    else -> p("Multiply matrix A = [[$a, $b], [$c, $d]] by vector v = [1, 2]. Enter the resulting 2D vector.", "Av = [a₁₁v₁ + a₁₂v₂, a₂₁v₁ + a₂₂v₂]", "${a + 2 * b},${c + 2 * d}", MathAnswerType.Vector, listOf("First component: $a(1) + $b(2) = ${a + 2 * b}.", "Second component: $c(1) + $d(2) = ${c + 2 * d}."))
   }
   MathTopic.LinearSystems -> when (difficulty) {
    1 -> p("Solve x + y = ${a + b}, x − y = ${a - b}. Enter [x,y].", "Elimination combines equations to remove a variable.", "$a,$b", MathAnswerType.Vector, listOf("Add equations: 2x = ${2 * a}, so x=$a.", "Substitute: y=${a + b}−$a=$b."))
    2 -> p("Solve 2x + y = ${2 * a + b}, x + y = ${a + b}. Enter [x,y].", "Subtract the second equation from the first.", "$a,$b", MathAnswerType.Vector, listOf("(2x + y) − (x + y) = ${2 * a + b} − ${a + b} ⇒ x = $a.", "Then y = ${a + b} − $a = $b."))
    else -> p("Solve ${a}x + y = ${a * c + d}, x = $c. Enter [x,y].", "Back-substitution into a triangular system.", "$c,$d", MathAnswerType.Vector, listOf("Given x = $c, substitute into the first equation: ${a * c} + y = ${a * c + d}.", "Subtract ${a * c}: y = $d."))
   }
   MathTopic.Eigenvectors -> when (difficulty) {
    1 -> p("For A = [[$a,0],[0,$b]] and v = [1,0], find its eigenvalue λ.", "Av = λv", "$a", steps = listOf("Av = [$a,0].", "This is $a times v, hence λ=$a."))
    2 -> p("Find the sum of the eigenvalues of A = [[$a, $c], [$d, $b]].", "∑ λᵢ = trace(A) = a₁₁ + a₂₂", "${a + b}", steps = listOf("The sum of eigenvalues of any square matrix equals its trace.", "trace(A) = $a + $b = ${a + b}."))
    else -> p("For upper-triangular A = [[$a, $c], [0, ${a + b}]], find the larger eigenvalue.", "Eigenvalues of a triangular matrix are its diagonal entries.", "${a + b}", steps = listOf("Diagonal entries are $a and ${a + b}.", "Since b = $b > 0, the larger eigenvalue is ${a + b}."))
   }
   MathTopic.Svd -> when (difficulty) {
    1 -> p("Find the largest singular value of A = [[−$a,0],[0,$b]].", "σᵢ = √λᵢ(AᵀA)", "${max(a, b)}", steps = listOf("AᵀA has diagonal ${a * a}, ${b * b}.", "Square roots are $a and $b; largest is ${max(a, b)}."))
    2 -> p("Find the smallest singular value of A = [[$a, 0], [0, −$b]].", "σ_min = min(|a|, |b|) for a diagonal matrix", "${min(a, b)}", steps = listOf("Singular values are non-negative square roots of eigenvalues of AᵀA: $a and $b.", "The smallest is ${min(a, b)}."))
    else -> p("Find the Frobenius norm squared ‖A‖_F² = ∑ σᵢ² of A = [[$a, 0], [0, $b]].", "‖A‖_F² = trace(AᵀA) = σ₁² + σ₂²", "${a * a + b * b}", steps = listOf("Singular values are $a and $b.", "Sum of squares = $a² + $b² = ${a * a + b * b}."))
   }
   MathTopic.Pca -> when (difficulty) {
    1 -> p("A centered dataset has covariance [[${a + b},0],[0,$b]]. Which direction is the first principal component?", "First PC maximizes vᵀΣv subject to ‖v‖ = 1.", "[1,0]", MathAnswerType.Choice, listOf("The eigenvalues are ${a + b} and $b.", "The larger eigenvalue corresponds to [1,0]. Sign reversal is equivalent."), listOf("[1,0]", "[0,1]", "[1,1] / √2", "No direction exists"))
    2 -> p("A 2D dataset has covariance eigenvalues λ₁ = ${a * 3} and λ₂ = $a. What fraction of total variance is explained by PC1?", "Explained variance ratio = λ₁ / (λ₁ + λ₂)", "0.75", MathAnswerType.Number, listOf("Total variance = ${a * 3} + $a = ${a * 4}.", "Ratio = ${a * 3} / ${a * 4} = 3/4 = 0.75."))
    else -> p("If covariance is Σ = [[$b, 0], [0, ${a + b}]], which unit vector is the first principal component?", "PC1 aligns with the eigenvector of the largest eigenvalue.", "[0,1]", MathAnswerType.Choice, listOf("Diagonal covariance has eigenvalues $b (along [1,0]) and ${a + b} (along [0,1]).", "Since ${a + b} > $b, PC1 is [0,1]."), listOf("[0,1]", "[1,0]", "[1,-1] / √2", "[0,0]"))
   }
   MathTopic.Derivatives -> when (difficulty) {
    1 -> p("Differentiate f(x) = ${a}x² + ${b}x.", "d(xⁿ)/dx = nxⁿ⁻¹", "${2 * a}x+$b", MathAnswerType.Expression, listOf("Differentiate each term.", "f′(x) = ${2 * a}x + $b."), graph = "${a}x^2+${b}x")
    2 -> p("Differentiate f(x) = ${a}x³ - ${b}x² + ${c}x.", "d(xⁿ)/dx = nxⁿ⁻¹", "${3 * a}x^2-${2 * b}x+$c", MathAnswerType.Expression, listOf("Power rule on each term: d(${a}x³)/dx = ${3 * a}x², d(-${b}x²)/dx = -${2 * b}x, d(${c}x)/dx = $c.", "f′(x) = ${3 * a}x² - ${2 * b}x + $c."), graph = "${a}x^3-${b}x^2+${c}x")
    else -> p("Find the second derivative f''(x) of f(x) = ${a}x³ + ${b}x² - ${c}x.", "f''(x) = d/dx [f'(x)]", "${6 * a}x+${2 * b}", MathAnswerType.Expression, listOf("First derivative: f'(x) = ${3 * a}x² + ${2 * b}x - $c.", "Differentiate again: f''(x) = ${6 * a}x + ${2 * b}."), graph = "${6 * a}x+${2 * b}")
   }
   MathTopic.Gradients -> when (difficulty) {
    1 -> p("For f(x,y) = x² + ${a}y², find ∇f at ($b,$c).", "∇f = [∂f/∂x, ∂f/∂y]", "${2 * b},${2 * a * c}", MathAnswerType.Vector, listOf("Partial derivatives: 2x and ${2 * a}y.", "At ($b,$c), gradient = [${2 * b},${2 * a * c}]."))
    2 -> p("For f(x,y) = ${a}xy + y², find ∇f at ($b, $c).", "∂f/∂x = ay,  ∂f/∂y = ax + 2y", "${a * c},${a * b + 2 * c}", MathAnswerType.Vector, listOf("∂f/∂x = ${a}y = ${a * c}.", "∂f/∂y = ${a}x + 2y = ${a * b + 2 * c}."))
    else -> p("Find the directional derivative of f(x,y) = ${a}x + ${b}y at (0,0) along vector u = [$c, $d] (unnormalized dot product ∇f · u).", "∇f · u = (∂f/∂x)u₁ + (∂f/∂y)u₂", "${a * c + b * d}", MathAnswerType.Number, listOf("Gradient is constant: ∇f = [$a, $b].", "Dot with u = [$c, $d]: $a×$c + $b×$d = ${a * c + b * d}."))
   }
   MathTopic.ChainRule -> when (difficulty) {
    1 -> p("Find the derivative of (${a}x + $b)² at x=$c.", "d[g(h(x))]/dx = g′(h(x))h′(x)", "${2 * a * (a * c + b)}", steps = listOf("Outer derivative: 2(${a}x+$b). Inner derivative: $a.", "At x=$c: 2 × ${a * c + b} × $a = ${2 * a * (a * c + b)}."), graph = "(${a}x+$b)^2")
    2 -> p("Differentiate f(x) = (${a}x + $b)². Enter the expanded derivative in x.", "d/dx (ax + b)² = 2a(ax + b) = 2a²x + 2ab", "${2 * a * a}x+${2 * a * b}", MathAnswerType.Expression, listOf("By chain rule: 2(${a}x + $b) · $a.", "Expand: ${2 * a * a}x + ${2 * a * b}."), graph = "(${a}x+$b)^2")
    else -> p("For f(x) = e^(${a}x), find f'(0).", "d/dx [e^(ax)] = a·e^(ax)", "$a", steps = listOf("By chain rule, f'(x) = $a e^(${a}x).", "At x = 0, e^0 = 1, so f'(0) = $a."), graph = "e^(${a}x)")
   }
   MathTopic.Optimization -> when (difficulty) {
    1 -> p("For L(w)=w², start w=$a. Take one gradient descent step with rate 0.1.", "wₙ₊₁ = wₙ − η∇L(wₙ)", formatMath(a * 0.8), steps = listOf("Gradient is 2w = ${2 * a}.", "New w = $a − 0.1 × ${2 * a} = ${formatMath(a * 0.8)}."), graph = "x^2")
    2 -> p("Find the global minimum w* of L(w) = (w − $a)² + $b.", "Set L'(w) = 2(w − a) = 0", "$a", steps = listOf("Derivative is L'(w) = 2(w − $a).", "Setting L'(w) = 0 gives w* = $a (minimum value $b)."), graph = "(x-$a)^2+$b")
    else -> p("For L(w) = (w − $b)², start at w = ${b + a} with learning rate η = 0.25. Find w₁ after one gradient descent step.", "w₁ = w₀ − η · 2(w₀ − b)", formatMath(b + 0.5 * a), steps = listOf("Gradient at w₀ = ${b + a} is 2(${b + a} − $b) = ${2 * a}.", "w₁ = ${b + a} − 0.25 × ${2 * a} = ${formatMath(b + 0.5 * a)}."), graph = "(x-$b)^2")
   }
   MathTopic.Loss -> when (difficulty) {
    1 -> p("Targets are [$a,$b], predictions are [${a + 1},${b - 2}]. Find mean squared error.", "MSE = (1/n)∑(ŷᵢ − yᵢ)²", "2.5", steps = listOf("Errors are 1 and −2.", "Squared errors: 1 and 4. Mean = 5/2 = 2.5."))
    2 -> p("Targets are [$a, $b, $c], predictions are [${a + 2}, ${b - 1}, ${c + 2}]. Find Mean Absolute Error (MAE).", "MAE = (1/n)∑|ŷᵢ − yᵢ|", "5/3", steps = listOf("Absolute errors are |2| = 2, |−1| = 1, |2| = 2.", "Average of [2, 1, 2] is 5/3 ≈ 1.6667."))
    else -> p("For a single residual r = $a and Huber threshold δ = 1, find the Huber loss L_δ(r) = δ(|r| − 0.5δ).", "For |r| > δ: L_δ(r) = δ(|r| − δ/2)", formatMath(a - 0.5), steps = listOf("Since |$a| > 1, we use the linear branch.", "L_1($a) = 1 × ($a − 0.5) = ${formatMath(a - 0.5)}."), graph = "{|x| <= 1: 0.5x^2, |x| - 0.5}")
   }
  }
 }

 /** Removes a leading "x =", "λ =", "answer:" etc. so "x = 3" is accepted for a numeric answer. */
 private fun stripLabel(input: String): String =
  input.trim().replace(Regex("^(?:[a-zA-Zλσμ]\\w*\\s*=|answer\\s*[:=])\\s*"), "")

 fun check(p: MathProblem, input: String): MathVerdict {
  if (p.type == MathAnswerType.Explanation) return MathVerdict(false, "This reasoning response requires self-review against the solution. It is not automatically graded.")
  if (input.isBlank()) return MathVerdict(false, "Enter an answer first.")
  return try {
   val correct = when (p.type) {
    MathAnswerType.Number -> numberMatches(stripLabel(input), p.answer, p.tolerance)
    MathAnswerType.Choice -> input.trim() == p.answer
    MathAnswerType.Expression -> {
     val given = input.trim().replace(Regex("^(?:f\\s*'?\\s*'?\\s*\\(\\s*x\\s*\\)|f′\\(x\\)|y)\\s*=\\s*"), "")
     if (p.topic == MathTopic.Algebra && given.contains('(')) return MathVerdict(false, "Equivalent forms with parentheses are not expanded yet. Multiply out the brackets.")
     equivalentInX(given, p.answer)
    }
    MathAnswerType.Vector, MathAnswerType.Matrix -> {
     val actual = matrix(input); val expected = matrix(p.answer)
     val flatOk = p.type == MathAnswerType.Vector && actual.flatten().size == expected.flatten().size &&
      actual.flatten().indices.all { close(actual.flatten()[it], expected.flatten()[it], p.tolerance) }
     flatOk || (actual.size == expected.size && actual.indices.all { row ->
      actual[row].size == expected[row].size && actual[row].indices.all { col -> close(actual[row][col], expected[row][col], p.tolerance) }
     })
    }
    MathAnswerType.Explanation -> false
   }
   MathVerdict(correct, if (correct) "Correct" else "Needs review. Compare your work with the solution below.")
  } catch (e: Exception) {
   MathVerdict(false, "Check the answer format: ${e.message}")
  }
 }

 /** Exact (within tolerance) match, or a correctly rounded decimal with at least 2 decimal places. */
 private fun numberMatches(input: String, answer: String, tolerance: Double): Boolean {
  val actual = ExpressionParser(input).value(); val expected = ExpressionParser(answer).value()
  if (close(actual, expected, tolerance)) return true
  val decimals = Regex("^-?\\d*\\.(\\d{2,})$").matchEntire(input.trim().replace("−", "-"))?.groupValues?.get(1)?.length ?: return false
  return abs(actual - expected) <= 0.5 * 10.0.pow(-decimals) + 1e-12
 }

 /** Compares two expressions in x by evaluating them at several points (accepts any equivalent form). */
 fun equivalentInX(a: String, b: String): Boolean {
  val ea = MathParser.parse(a); val eb = MathParser.parse(b)
  val extra = (ea.variables() - "x")
  require(extra.isEmpty()) { "Only the variable x may be used (found ${extra.joinToString()})" }
  val points = doubleArrayOf(-2.7, -1.3, -0.4, 0.6, 1.1, 2.3, 3.9)
  var compared = 0
  for (x in points) {
   val va = Evaluator().at(x).eval(ea); val vb = Evaluator().at(x).eval(eb)
   if (!va.isFinite() && !vb.isFinite()) continue
   if (!close(va, vb, 1e-7)) return false
   compared++
  }
  return compared >= 3
 }

 private fun close(a: Double, b: Double, t: Double) = a.isFinite() && abs(a - b) <= t * max(1.0, abs(b))

 // ───────────────────────── Matrices & Linear Algebra ─────────────────────────

 /** Accepts 1,2;3,4 · [[1,2],[3,4]] · [1, 2], [3, 4] · one row per line · space-separated cells. */
 fun matrix(text: String): List<List<Double>> {
  val raw = text.trim().replace("\r\n", "\n").replace('\r', '\n').replace("\n", ";")
  var s = MathText.normalize(raw).trim()
  require(s.isNotEmpty()) { "Enter numbers separated by commas, e.g. 1,2;3,4" }
  s = s.replace(Regex("\\)\\s*,?\\s*\\("), ";")
  while (s.startsWith("(") && s.endsWith(")")) s = s.substring(1, s.length - 1).trim()
  val rows = s.split(';').map { it.trim().trim('(', ')').trim() }.filter { it.isNotEmpty() }.map { row ->
   val cells = if (row.contains(',')) row.split(',') else row.split(Regex("\\s+"))
   cells.map { cell ->
    require(cell.isNotBlank()) { "Empty cell in row \"$row\"" }
    ExpressionParser(cell.trim()).value()
   }
  }
  require(rows.isNotEmpty() && rows.all { it.size == rows[0].size }) {
   "Every row must have the same number of columns (separated by semicolons, e.g. 1,2;3,4)."
  }
  return rows
 }

 fun polynomial(text: String): Map<Int, Double> {
  val s = text.lowercase().replace(" ", "").replace("*", "").replace("−", "-").replace("²", "^2")
  require(s.isNotEmpty() && s.length < 256) { "Enter an expanded polynomial in x." }
  val terms = Regex("[+-]?[^+-]+").findAll(s).map { it.value }.toList()
  require(terms.joinToString("") == s) { "Enter an expanded polynomial in x." }
  val result = mutableMapOf<Int, Double>()
  terms.forEach { term ->
   val parts = term.split('x'); require(parts.size <= 2) { "Each term may contain x once." }
   val power = if (parts.size == 1) 0 else if (parts[1].isEmpty()) 1 else {
    require(parts[1].startsWith('^')) { "Write powers as x^2." }
    parts[1].drop(1).toInt().also { require(it in 0..8) { "Powers must be between 0 and 8." } }
   }
   val coefficient = when (parts[0]) { "", "+" -> 1.0; "-" -> -1.0; else -> ExpressionParser(parts[0]).value() }
   result[power] = (result[power] ?: 0.0) + coefficient
  }
  return result.filterValues { it != 0.0 }
 }

 fun addMatrices(a: List<List<Double>>, b: List<List<Double>>): List<List<Double>> {
  require(a.size == b.size && a[0].size == b[0].size) {
   "Matrices must have the same dimensions (${a.size}×${a[0].size} vs ${b.size}×${b[0].size})."
  }
  return a.indices.map { i -> a[i].indices.map { j -> a[i][j] + b[i][j] } }
 }

 fun subtractMatrices(a: List<List<Double>>, b: List<List<Double>>): List<List<Double>> {
  require(a.size == b.size && a[0].size == b[0].size) {
   "Matrices must have the same dimensions (${a.size}×${a[0].size} vs ${b.size}×${b[0].size})."
  }
  return a.indices.map { i -> a[i].indices.map { j -> a[i][j] - b[i][j] } }
 }

 fun multiply(a: List<List<Double>>, b: List<List<Double>>): List<List<Double>> {
  require(a[0].size == b.size) { "Columns of A (${a[0].size}) must equal rows of B (${b.size})." }
  return a.map { row -> b[0].indices.map { j -> row.indices.sumOf { k -> row[k] * b[k][j] } } }
 }

 fun transpose(a: List<List<Double>>): List<List<Double>> =
  a[0].indices.map { col -> a.indices.map { row -> a[row][col] } }

 fun trace(a: List<List<Double>>): Double {
  require(a.size == a[0].size) { "Trace requires a square matrix (got ${a.size}×${a[0].size})." }
  return a.indices.sumOf { a[it][it] }
 }

 fun determinant(a: List<List<Double>>): Double {
  val n = a.size
  require(n == a[0].size) { "Determinant requires a square matrix (got ${a.size}×${a[0].size})." }
  val m = Array(n) { r -> DoubleArray(n) { c -> a[r][c] } }
  var sign = 1.0
  for (i in 0 until n) {
   var pivot = i
   for (r in i + 1 until n) if (abs(m[r][i]) > abs(m[pivot][i])) pivot = r
   if (abs(m[pivot][i]) < 1e-12) return 0.0
   if (pivot != i) { val tmp = m[i]; m[i] = m[pivot]; m[pivot] = tmp; sign = -sign }
   for (r in i + 1 until n) {
    val factor = m[r][i] / m[i][i]
    for (c in i until n) m[r][c] -= factor * m[i][c]
   }
  }
  var prod = sign
  for (i in 0 until n) prod *= m[i][i]
  return if (abs(prod) < 1e-12) 0.0 else prod
 }

 fun inverse(a: List<List<Double>>): List<List<Double>> {
  val n = a.size
  require(n == a[0].size) { "Inverse requires a square matrix (got ${a.size}×${a[0].size})." }
  val aug = Array(n) { r -> DoubleArray(2 * n) { c -> if (c < n) a[r][c] else if (c - n == r) 1.0 else 0.0 } }
  for (i in 0 until n) {
   var pivot = i
   for (r in i + 1 until n) if (abs(aug[r][i]) > abs(aug[pivot][i])) pivot = r
   require(abs(aug[pivot][i]) > 1e-11) { "Matrix is singular (determinant = 0) and has no inverse." }
   val tmp = aug[i]; aug[i] = aug[pivot]; aug[pivot] = tmp
   val div = aug[i][i]
   for (c in 0 until 2 * n) aug[i][c] /= div
   for (r in 0 until n) if (r != i) {
    val factor = aug[r][i]
    for (c in 0 until 2 * n) aug[r][c] -= factor * aug[i][c]
   }
  }
  return List(n) { r -> List(n) { c -> val v = aug[r][n + c]; if (abs(v) < 1e-12) 0.0 else v } }
 }

 fun rref(a: List<List<Double>>): List<List<Double>> {
  val rows = a.size; val cols = a[0].size
  val m = Array(rows) { r -> DoubleArray(cols) { c -> a[r][c] } }
  var lead = 0
  for (r in 0 until rows) {
   if (lead >= cols) break
   var i = r
   while (abs(m[i][lead]) < 1e-11) {
    i++
    if (i == rows) { i = r; lead++; if (lead == cols) return m.map { row -> row.map { if (abs(it) < 1e-12) 0.0 else it } } }
   }
   val tmp = m[i]; m[i] = m[r]; m[r] = tmp
   val div = m[r][lead]
   for (c in 0 until cols) m[r][c] /= div
   for (k in 0 until rows) if (k != r) {
    val factor = m[k][lead]
    for (c in 0 until cols) m[k][c] -= factor * m[r][c]
   }
   lead++
  }
  return m.map { row -> row.map { if (abs(it) < 1e-12) 0.0 else it } }
 }

 fun rank(a: List<List<Double>>): Int =
  rref(a).count { row -> row.any { abs(it) > 1e-9 } }

 /** Solves A x = b where A is n×n and b is a vector of length n (entered as 1×n or n×1). */
 fun solveLinearSystem(a: List<List<Double>>, bVec: List<Double>): List<Double> {
  val n = a.size
  require(n == a[0].size) { "Coefficient matrix A must be square (got ${a.size}×${a[0].size})." }
  require(bVec.size == n) { "Right-hand side vector b must have $n components (got ${bVec.size})." }
  val aug = Array(n) { r -> DoubleArray(n + 1) { c -> if (c < n) a[r][c] else bVec[r] } }
  for (i in 0 until n) {
   var pivot = i
   for (r in i + 1 until n) if (abs(aug[r][i]) > abs(aug[pivot][i])) pivot = r
   require(abs(aug[pivot][i]) > 1e-11) { "System has no unique solution (det(A) = 0)." }
   val tmp = aug[i]; aug[i] = aug[pivot]; aug[pivot] = tmp
   val div = aug[i][i]
   for (c in i..n) aug[i][c] /= div
   for (r in 0 until n) if (r != i) {
    val factor = aug[r][i]
    for (c in i..n) aug[r][c] -= factor * aug[i][c]
   }
  }
  return List(n) { r -> val v = aug[r][n]; if (abs(v) < 1e-12) 0.0 else v }
 }

 fun eigen2x2(a: List<List<Double>>): Eigen2x2Result {
  require(a.size == 2 && a[0].size == 2) { "Exact 2×2 eigen analysis requires a 2×2 matrix." }
  val a11 = a[0][0]; val a12 = a[0][1]; val a21 = a[1][0]; val a22 = a[1][1]
  val tr = a11 + a22
  val det = a11 * a22 - a12 * a21
  val disc = tr * tr - 4 * det
  if (disc < -1e-12) {
   val re = tr / 2.0; val im = sqrt(-disc) / 2.0
   return Eigen2x2Result(tr, det, disc, null, "${formatMath(re)} ± ${formatMath(im)}i", emptyList())
  }
  val s = sqrt(max(0.0, disc))
  val l1 = (tr + s) / 2.0; val l2 = (tr - s) / 2.0
  fun vecFor(lambda: Double): List<Double> {
   val v = when {
    abs(a12) > 1e-9 -> listOf(a12, lambda - a11)
    abs(a21) > 1e-9 -> listOf(lambda - a22, a21)
    abs(a11 - lambda) < abs(a22 - lambda) -> listOf(1.0, 0.0)
    else -> listOf(0.0, 1.0)
   }
   val len = hypot(v[0], v[1]).let { if (it < 1e-12) 1.0 else it }
   return listOf(v[0] / len, v[1] / len)
  }
  val vecs = if (abs(l1 - l2) < 1e-9 && abs(a12) < 1e-9 && abs(a21) < 1e-9) {
   listOf(listOf(1.0, 0.0), listOf(0.0, 1.0))
  } else listOf(vecFor(l1), vecFor(l2))
  return Eigen2x2Result(tr, det, disc, l1 to l2, null, vecs)
 }

 // ───────────────────────── Vectors ─────────────────────────

 fun vector(text: String): List<Double> {
  val rows = matrix(text)
  require(rows.size == 1 || rows.all { it.size == 1 }) { "Enter one row (e.g. 3, 4) or one column (e.g. 3; 4)." }
  return rows.flatten()
 }

 fun dot(a: List<Double>, b: List<Double>): Double {
  require(a.size == b.size) { "Vectors must have the same dimension (${a.size} vs ${b.size})." }
  return a.indices.sumOf { a[it] * b[it] }
 }

 fun norm(a: List<Double>): Double = sqrt(dot(a, a))
 fun norm1(a: List<Double>): Double = a.sumOf { abs(it) }
 fun normInf(a: List<Double>): Double = a.maxOfOrNull { abs(it) } ?: 0.0

 fun unitVector(a: List<Double>): List<Double> {
  val len = norm(a)
  require(len > 1e-12) { "The zero vector cannot be normalized." }
  return a.map { it / len }
 }

 fun cosineSimilarity(a: List<Double>, b: List<Double>): Double {
  val denom = norm(a) * norm(b)
  require(denom > 1e-12) { "Angle and cosine similarity are undefined for a zero vector." }
  return (dot(a, b) / denom).coerceIn(-1.0, 1.0)
 }

 fun angleRadians(a: List<Double>, b: List<Double>): Double = acos(cosineSimilarity(a, b))
 fun angleDegrees(a: List<Double>, b: List<Double>): Double = angleRadians(a, b) * 180.0 / PI

 fun project(a: List<Double>, ontoB: List<Double>): List<Double> {
  val bb = dot(ontoB, ontoB)
  require(bb > 1e-12) { "Cannot project onto the zero vector." }
  val scale = dot(a, ontoB) / bb
  return ontoB.map { it * scale }
 }

 fun cross(a: List<Double>, b: List<Double>): List<Double> {
  require(a.size in 2..3 && b.size == a.size) { "Cross product requires two 2D or 3D vectors of the same dimension." }
  val ax = a[0]; val ay = a[1]; val az = if (a.size == 3) a[2] else 0.0
  val bx = b[0]; val by = b[1]; val bz = if (b.size == 3) b[2] else 0.0
  return listOf(ay * bz - az * by, az * bx - ax * bz, ax * by - ay * bx)
 }

 // ───────────────────────── Statistics & Regression ─────────────────────────

 /** Splits a list of numbers separated by commas, semicolons, spaces or new lines. */
 fun numberList(text: String): List<Double> {
  val cleaned = text.trim().removePrefix("[").removeSuffix("]")
  return cleaned.split(Regex("[,;\\s]+")).filter { it.isNotBlank() }.map { ExpressionParser(it).value() }
   .also { require(it.isNotEmpty()) { "Enter at least one number" } }
 }

 private fun percentile(sorted: List<Double>, p: Double): Double {
  if (sorted.size == 1) return sorted[0]
  val pos = p * (sorted.size - 1)
  val lo = floor(pos).toInt(); val hi = ceil(pos).toInt()
  val frac = pos - lo
  return sorted[lo] + frac * (sorted[hi] - sorted[lo])
 }

 fun analyzeData(values: List<Double>): DataSummary {
  require(values.isNotEmpty()) { "Enter at least one number" }
  val sorted = values.sorted()
  val n = sorted.size
  val sum = sorted.sum()
  val mean = sum / n
  val q2 = if (n % 2 == 1) sorted[n / 2] else (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0
  val lowerHalf = sorted.subList(0, n / 2)
  val upperHalf = sorted.subList((n + 1) / 2, n)
  val q1 = if (lowerHalf.isEmpty()) sorted.first() else if (lowerHalf.size % 2 == 1) lowerHalf[lowerHalf.size / 2] else (lowerHalf[lowerHalf.size / 2 - 1] + lowerHalf[lowerHalf.size / 2]) / 2.0
  val q3 = if (upperHalf.isEmpty()) sorted.last() else if (upperHalf.size % 2 == 1) upperHalf[upperHalf.size / 2] else (upperHalf[upperHalf.size / 2 - 1] + upperHalf[upperHalf.size / 2]) / 2.0
  val iqr = q3 - q1
  val popVar = sorted.sumOf { (it - mean).pow(2) } / n
  val popSd = sqrt(popVar)
  val sampVar = if (n > 1) sorted.sumOf { (it - mean).pow(2) } / (n - 1) else Double.NaN
  val sampSd = if (n > 1) sqrt(sampVar) else Double.NaN
  val sem = if (n > 1) sampSd / sqrt(n.toDouble()) else Double.NaN
  val ci95 = if (n > 1) 1.959964 * sem else Double.NaN
  val skew = if (n >= 3 && popSd > 1e-12) (sorted.sumOf { ((it - mean) / popSd).pow(3) } / n) else 0.0
  val kurt = if (n >= 4 && popSd > 1e-12) (sorted.sumOf { ((it - mean) / popSd).pow(4) } / n) - 3.0 else 0.0
  val geom = if (sorted.all { it > 0 }) exp(sorted.sumOf { ln(it) } / n) else Double.NaN
  val harm = if (sorted.all { it > 0 }) n / sorted.sumOf { 1.0 / it } else Double.NaN
  val counts = sorted.groupingBy { (it * 1e9).roundToLong() / 1e9 }.eachCount()
  val maxFreq = counts.values.maxOrNull() ?: 1
  val modes = if (maxFreq > 1) counts.filterValues { it == maxFreq }.keys.sorted() else emptyList()
  val fenceLo = q1 - 1.5 * iqr; val fenceHi = q3 + 1.5 * iqr
  val outliers = if (n >= 4 && iqr > 0) sorted.filter { it < fenceLo || it > fenceHi } else emptyList()
  return DataSummary(
   n = n, sum = sum, mean = mean, median = q2, modes = modes,
   geometricMean = geom, harmonicMean = harm,
   popVariance = popVar, popStdDev = popSd, sampleVariance = sampVar, sampleStdDev = sampSd, stdError = sem,
   min = sorted.first(), q1 = q1, q2 = q2, q3 = q3, max = sorted.last(),
   range = sorted.last() - sorted.first(), iqr = iqr,
   skewness = skew, excessKurtosis = kurt,
   ci95Low = mean - ci95, ci95High = mean + ci95,
   outliers = outliers, sorted = sorted
  )
 }

 fun statistics(values: List<Double>): String {
  val d = analyzeData(values)
  return buildString {
   appendLine("n = ${d.n}")
   appendLine("Sum = ${formatMath(d.sum)}")
   appendLine("Mean = ${d.mean}")
   appendLine("Median = ${formatMath(d.median)}")
   appendLine("Mode = ${if (d.modes.isEmpty()) "none (all values distinct)" else d.modes.joinToString { formatMath(it) }}")
   appendLine("Population variance = ${formatMath(d.popVariance)}")
   appendLine("Standard deviation = ${formatMath(d.popStdDev)}")
   appendLine("Sample variance = ${if (d.n > 1) formatMath(d.sampleVariance) else "needs 2+ values"}")
   appendLine("Sample standard deviation = ${if (d.n > 1) formatMath(d.sampleStdDev) else "needs 2+ values"}")
   appendLine("Quartiles (Q1, Q2, Q3) = ${formatMath(d.q1)}, ${formatMath(d.q2)}, ${formatMath(d.q3)}  (IQR = ${formatMath(d.iqr)})")
   appendLine("Minimum = ${formatMath(d.min)}")
   appendLine("Maximum = ${formatMath(d.max)}")
   append("Range = ${formatMath(d.range)}")
  }
 }

 fun linearRegression(xs: List<Double>, ys: List<Double>): RegressionSummary {
  require(xs.size == ys.size && xs.size >= 2) { "Provide at least 2 paired (x, y) values of equal length (got ${xs.size} x's and ${ys.size} y's)." }
  val n = xs.size
  val mx = xs.average(); val my = ys.average()
  val sxx = xs.sumOf { (it - mx).pow(2) }
  val syy = ys.sumOf { (it - my).pow(2) }
  val sxy = xs.indices.sumOf { (xs[it] - mx) * (ys[it] - my) }
  require(sxx > 1e-12) { "All x values are identical; a vertical line cannot be fit by y = mx + b." }
  val slope = sxy / sxx
  val intercept = my - slope * mx
  val r = if (syy > 1e-12) (sxy / sqrt(sxx * syy)).coerceIn(-1.0, 1.0) else 1.0
  val cov = sxy / (n - 1)
  val sign = if (intercept >= 0) "+ ${formatMath(intercept, 4)}" else "− ${formatMath(-intercept, 4)}"
  val eq = "ŷ = ${formatMath(slope, 4)}x $sign"
  val res = xs.indices.map { ys[it] - (slope * xs[it] + intercept) }
  return RegressionSummary(n, slope, intercept, r, r * r, cov, mx, my, eq, xs.zip(ys), res)
 }

 // ───────────────────────── Probability & Distributions ─────────────────────────

 fun binomial(n: Int, k: Int, p: Double): Double {
  require(n in 0..1000) { "n must be a whole number from 0 to 1000" }
  require(k in 0..n) { "k must be a whole number from 0 to n" }
  require(p in 0.0..1.0) { "p must be between 0 and 1" }
  if (p == 0.0) return if (k == 0) 1.0 else 0.0
  if (p == 1.0) return if (k == n) 1.0 else 0.0
  var logComb = 0.0
  for (i in 1..k) logComb += ln((n - i + 1).toDouble()) - ln(i.toDouble())
  return exp(logComb + k * ln(p) + (n - k) * ln(1.0 - p))
 }

 fun poissonPmf(lambda: Double, k: Int): Double {
  require(lambda > 0 && lambda <= 1000) { "Rate λ must be positive (0 < λ ≤ 1000)" }
  require(k in 0..2000) { "Count k must be a non-negative integer" }
  var logFact = 0.0
  for (i in 2..k) logFact += ln(i.toDouble())
  return exp(k * ln(lambda) - lambda - logFact)
 }

 fun poissonCdf(lambda: Double, k: Int): Double = (0..k).sumOf { poissonPmf(lambda, it) }.coerceIn(0.0, 1.0)

 fun geometricPmf(p: Double, k: Int): Double {
  require(p > 0.0 && p <= 1.0) { "Success probability p must be in (0, 1]" }
  require(k >= 1) { "Trial number k must be ≥ 1" }
  return (1.0 - p).pow(k - 1) * p
 }

 fun geometricCdf(p: Double, k: Int): Double {
  require(p > 0.0 && p <= 1.0 && k >= 1)
  return 1.0 - (1.0 - p).pow(k)
 }

 /** Error function erf(x) with max error < 1.5e-7 (Abramowitz & Stegun 7.1.26). */
 fun erf(x: Double): Double {
  val sign = if (x < 0) -1.0 else 1.0
  val a = abs(x)
  val t = 1.0 / (1.0 + 0.3275911 * a)
  val poly = (((((1.061405429 * t - 1.453152027) * t) + 1.421413741) * t - 0.284496736) * t + 0.254829592) * t
  return sign * (1.0 - poly * exp(-a * a))
 }

 fun normalPdf(x: Double, mu: Double = 0.0, sigma: Double = 1.0): Double {
  require(sigma > 0) { "Standard deviation σ must be > 0" }
  val z = (x - mu) / sigma
  return exp(-0.5 * z * z) / (sigma * sqrt(2.0 * PI))
 }

 fun normalCdf(x: Double, mu: Double = 0.0, sigma: Double = 1.0): Double {
  require(sigma > 0) { "Standard deviation σ must be > 0" }
  val z = (x - mu) / (sigma * sqrt(2.0))
  return (0.5 * (1.0 + erf(z))).coerceIn(0.0, 1.0)
 }

 fun combinations(n: Int, r: Int): Double {
  require(n in 0..1000 && r in 0..n) { "Require 0 ≤ r ≤ n ≤ 1000" }
  val k = min(r, n - r)
  var res = 1.0
  for (i in 1..k) res *= (n - i + 1).toDouble() / i
  return res
 }

 fun permutations(n: Int, r: Int): Double {
  require(n in 0..1000 && r in 0..n) { "Require 0 ≤ r ≤ n ≤ 1000" }
  var res = 1.0
  for (i in 0 until r) res *= (n - i).toDouble()
  return res
 }

 /** Parses a whole number such as "10", " 10 ", or an expression that evaluates to an integer. */
 fun wholeNumber(text: String, label: String): Int {
  val v = try { ExpressionParser(text).value() } catch (e: Exception) { throw IllegalArgumentException("$label: ${e.message}") }
  require(abs(v - v.roundToLong()) < 1e-9) { "$label must be a whole number" }
  return v.roundToLong().toInt()
 }

 fun binomialReport(nText: String, kText: String, pText: String): String {
  val n = wholeNumber(nText, "Trials n"); val k = wholeNumber(kText, "Successes k"); val p = ExpressionParser(pText).value()
  val exact = binomial(n, k, p); val atMost = (0..k).sumOf { binomial(n, it, p) }
  return "P(X = $k) = ${formatMath(exact, 8)}\nP(X ≤ $k) = ${formatMath(atMost, 8)}\nP(X ≥ $k) = ${formatMath((1 - atMost + exact).coerceIn(0.0, 1.0), 8)}\nMean np = ${formatMath(n * p)}\nVariance np(1−p) = ${formatMath(n * p * (1 - p))}"
 }
}

/** Number-only evaluator kept for answer checking. Uses the shared safe parser (never eval or a scripting engine). */
class ExpressionParser(private val source: String, private val x: Double? = null) {
 fun value(): Double {
  require(source.isNotBlank()) { "Enter a value" }
  require(source.length <= 256) { "Input is too long" }
  val e = MathParser.parse(source)
  val ev = Evaluator(); if (x != null) ev.at(x)
  val result = ev.eval(e)
  require(result.isFinite()) { "Answer is not finite (undefined)" }
  return result
 }
}
