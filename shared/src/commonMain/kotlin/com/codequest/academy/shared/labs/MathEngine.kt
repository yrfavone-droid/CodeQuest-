package com.codequest.academy.shared.labs

import kotlin.math.*
import kotlin.random.Random

enum class MathTopic(val title: String) {
 Algebra("Algebra"), Equations("Equations"), Inequalities("Inequalities"), Functions("Functions and graphs"), Exponents("Exponents and logarithms"), Summation("Sequences and summation"), Statistics("Statistics"), Probability("Probability"), Vectors("Vectors"), Matrices("Matrices"), MatrixProduct("Matrix multiplication"), LinearSystems("Linear systems"), Eigenvectors("Eigenvectors"), Svd("SVD"), Pca("PCA"), Derivatives("Derivatives"), Gradients("Gradients"), ChainRule("Chain rule"), Optimization("Optimization"), Loss("Loss functions")
}
enum class MathAnswerType { Number, Expression, Choice, Vector, Matrix, Explanation }
data class MathProblem(val id: String, val topic: MathTopic, val difficulty: Int, val statement: String, val formula: String, val answer: String, val type: MathAnswerType, val steps: List<String>, val choices: List<String> = emptyList(), val graph: String? = null) {
 val tolerance = 1e-6
}
data class MathVerdict(val correct: Boolean, val message: String)

object MathEngine {
 fun generate(topic: MathTopic, difficulty: Int, seed: Int): MathProblem {
  require(difficulty in 1..3)
  val r=Random(seed); val a=r.nextInt(2,5+difficulty); val b=r.nextInt(1,5+difficulty); val c=r.nextInt(1,5)
  fun p(text:String, formula:String, answer:String, type:MathAnswerType=MathAnswerType.Number, steps:List<String>, choices:List<String> = emptyList(), graph:String?=null) = MathProblem("${topic.name}:$difficulty:$seed",topic,difficulty,text,formula,answer,type,steps,choices,graph)
  return when(topic) {
   MathTopic.Algebra -> p("Expand $a(x + $b).", "a(b + c) = ab + ac", "${a}x+${a*b}", MathAnswerType.Expression, listOf("Distribute $a to x and $b.", "${a}x + ${a*b}"))
   MathTopic.Equations -> p("Solve ${a}x + $b = ${a*c+b}.", "ax + b = d  ⇒  x = (d − b)/a", "$c", steps=listOf("Subtract $b: ${a}x = ${a*c}.", "Divide by $a: x = $c."))
   MathTopic.Inequalities -> p("Solve −${a}x < −${a*b}.", "Dividing by a negative reverses < and >.", "x > $b", MathAnswerType.Choice, listOf("Divide both sides by −$a.", "Reverse the inequality: x > $b."), listOf("x > $b","x < $b","x = $b","x ≥ $b"))
   MathTopic.Functions -> p("For f(x) = ${a}x² + $b, find f($c).", "f(x) = ax² + b", "${a*c*c+b}", steps=listOf("Square $c to get ${c*c}.", "Multiply by $a and add $b: ${a*c*c+b}."), graph="${a}*x^2+$b")
   MathTopic.Exponents -> p("Find log₂(${1 shl a}).", "logᵦ(bⁿ) = n", "$a", steps=listOf("${1 shl a} = 2 raised to $a.", "The logarithm is the exponent: $a."))
   MathTopic.Summation -> p("Find 1 + 2 + … + ${a+b}.", "∑ᵢ₌₁ⁿ i = n(n + 1)/2", "${(a+b)*(a+b+1)/2}", steps=listOf("There are ${a+b} consecutive terms.", "(${a+b})(${a+b+1})/2 = ${(a+b)*(a+b+1)/2}."))
   MathTopic.Statistics -> p("Find the population variance of [$b, ${b+a}, ${b+2*a}].", "σ² = ∑(xᵢ − μ)² / n", "${2.0*a*a/3}", steps=listOf("Mean μ = ${b+a}.", "Squared deviations are ${a*a}, 0, ${a*a}.", "Divide their sum by 3: ${2.0*a*a/3}."))
   MathTopic.Probability -> p("A bag has $a white and $b black counters. What is P(white) for one uniform draw?", "P(A) = favorable outcomes / total outcomes", "$a/${a+b}", steps=listOf("There are ${a+b} equally likely counters.", "The $a white counters give probability $a/${a+b}."))
   MathTopic.Vectors -> p("Add vectors [$a, $b] and [$c, −$a].", "[u₁,u₂] + [v₁,v₂] = [u₁+v₁,u₂+v₂]", "${a+c},${b-a}", MathAnswerType.Vector, listOf("Add first components: $a + $c = ${a+c}.", "Add second components: $b − $a = ${b-a}."))
   MathTopic.Matrices -> p("Transpose [[$a, $b], [$c, ${a+b}]]. Enter rows separated by semicolons.", "(Aᵀ)ᵢⱼ = Aⱼᵢ", "$a,$c;$b,${a+b}", MathAnswerType.Matrix, listOf("Rows become columns.", "Result: [[$a,$c],[$b,${a+b}]]."))
   MathTopic.MatrixProduct -> p("Multiply [[$a,0],[0,$b]] by [[$c,1],[1,$c]].", "(AB)ᵢⱼ = ∑ₖ AᵢₖBₖⱼ", "${a*c},$a;$b,${b*c}", MathAnswerType.Matrix, listOf("Take each row of A dotted with each column of B.", "The diagonal scales row 1 by $a and row 2 by $b.", "Result: [[${a*c},$a],[$b,${b*c}]]."))
   MathTopic.LinearSystems -> p("Solve x + y = ${a+b}, x − y = ${a-b}. Enter [x,y].", "Elimination combines equations to remove a variable.", "$a,$b", MathAnswerType.Vector, listOf("Add equations: 2x = ${2*a}, so x=$a.", "Substitute: y=${a+b}−$a=$b."))
   MathTopic.Eigenvectors -> p("For A = [[$a,0],[0,$b]] and v = [1,0], find its eigenvalue λ.", "Av = λv", "$a", steps=listOf("Av = [$a,0].", "This is $a times v, hence λ=$a."))
   MathTopic.Svd -> p("Find the largest singular value of A = [[−$a,0],[0,$b]].", "σᵢ = √λᵢ(AᵀA)", "${max(a,b)}", steps=listOf("AᵀA has diagonal ${a*a}, ${b*b}.", "Square roots are $a and $b; largest is ${max(a,b)}."))
   MathTopic.Pca -> p("A centered dataset has covariance [[${a+b},0],[0,$b]]. Which direction is the first principal component?", "First PC maximizes vᵀΣv subject to ‖v‖ = 1.", "[1,0]", MathAnswerType.Choice, listOf("The eigenvalues are ${a+b} and $b.", "The larger eigenvalue corresponds to [1,0]. Sign reversal is equivalent."), listOf("[1,0]","[0,1]","[1,1] / √2","No direction exists"))
   MathTopic.Derivatives -> p("Differentiate f(x) = ${a}x² + ${b}x.", "d(xⁿ)/dx = nxⁿ⁻¹", "${2*a}x+$b", MathAnswerType.Expression, listOf("Differentiate each term.", "f′(x) = ${2*a}x + $b."))
   MathTopic.Gradients -> p("For f(x,y) = x² + ${a}y², find ∇f at ($b,$c).", "∇f = [∂f/∂x, ∂f/∂y]", "${2*b},${2*a*c}", MathAnswerType.Vector, listOf("Partial derivatives: 2x and ${2*a}y.", "At ($b,$c), gradient = [${2*b},${2*a*c}]."))
   MathTopic.ChainRule -> p("Find the derivative of (${a}x + $b)² at x=$c.", "d[g(h(x))]/dx = g′(h(x))h′(x)", "${2*a*(a*c+b)}", steps=listOf("Outer derivative: 2(${a}x+$b). Inner derivative: $a.", "At x=$c: 2 × ${a*c+b} × $a = ${2*a*(a*c+b)}."))
   MathTopic.Optimization -> p("For L(w)=w², start w=$a. Take one gradient descent step with rate 0.1.", "wₙ₊₁ = wₙ − η∇L(wₙ)", "${a*.8}", steps=listOf("Gradient is 2w = ${2*a}.", "New w = $a − 0.1 × ${2*a} = ${a*.8}."), graph="x^2")
   MathTopic.Loss -> p("Targets are [$a,$b], predictions are [${a+1},${b-2}]. Find mean squared error.", "MSE = (1/n)∑(ŷᵢ − yᵢ)²", "2.5", steps=listOf("Errors are 1 and −2.", "Squared errors: 1 and 4. Mean = 5/2 = 2.5."))
  }
 }
 fun check(p:MathProblem, input:String):MathVerdict {
  if(p.type==MathAnswerType.Explanation) return MathVerdict(false,"This reasoning response requires self-review against the solution. It is not automatically graded.")
  return try {
  val correct=when(p.type) {
   MathAnswerType.Number -> close(ExpressionParser(input).value(),ExpressionParser(p.answer).value(),p.tolerance)
   MathAnswerType.Choice -> input.trim()==p.answer
   MathAnswerType.Expression -> polynomial(input)==polynomial(p.answer)
   MathAnswerType.Vector,MathAnswerType.Matrix -> {
    val actual=matrix(input); val expected=matrix(p.answer)
    actual.size==expected.size && actual.indices.all { row -> actual[row].size==expected[row].size && actual[row].indices.all { col -> close(actual[row][col],expected[row][col],p.tolerance) } }
   }
   MathAnswerType.Explanation -> false
  }
  MathVerdict(correct,if(correct) "Correct" else "Needs review. Compare your work with the solution below.")
 } catch(e:Exception) { MathVerdict(false,"Check the answer format: ${e.message}") }
 }
 private fun close(a:Double,b:Double,t:Double)=a.isFinite() && abs(a-b)<=t*max(1.0,abs(b))
 fun matrix(text:String):List<List<Double>> = text.trim().replace("],[",";").replace("] , [",";").replace("[", "").replace("]", "").split(';').map { row -> row.split(',').map { ExpressionParser(it.trim()).value() } }.also { rows -> require(rows.isNotEmpty() && rows.all { it.size==rows[0].size }) { "Use equal-length rows separated by semicolons." } }
 fun polynomial(text:String):Map<Int,Double> {
  val s=text.lowercase().replace(" ","").replace("*","").replace("−","-").replace("²","^2")
  require(s.isNotEmpty() && s.length<256) { "Enter an expanded polynomial in x." }
  val terms=Regex("[+-]?[^+-]+").findAll(s).map { it.value }.toList(); require(terms.joinToString("")==s)
  val result=mutableMapOf<Int,Double>()
  terms.forEach { term -> val parts=term.split('x'); require(parts.size<=2)
   val power=if(parts.size==1) 0 else if(parts[1].isEmpty()) 1 else { require(parts[1].startsWith('^')); parts[1].drop(1).toInt().also { require(it in 0..8) } }
   val coefficient=when(parts[0]) { "","+" -> 1.0; "-" -> -1.0; else -> ExpressionParser(parts[0]).value() }
   result[power]=(result[power]?:0.0)+coefficient
  }; return result.filterValues { it!=0.0 }
 }
 fun multiply(a:List<List<Double>>,b:List<List<Double>>):List<List<Double>> { require(a[0].size==b.size) { "Columns of A must equal rows of B." }; return a.map { row -> b[0].indices.map { j -> row.indices.sumOf { k -> row[k]*b[k][j] } } } }
 fun statistics(values:List<Double>):String { require(values.isNotEmpty()); val mean=values.average(); val variance=values.sumOf { (it-mean).pow(2) }/values.size; return "n = ${values.size}\nMean = $mean\nPopulation variance = $variance\nStandard deviation = ${sqrt(variance)}\nMinimum = ${values.minOrNull()}\nMaximum = ${values.maxOrNull()}" }
 fun vector(text:String):List<Double> {val rows=matrix(text);require(rows.size==1 || rows.all {it.size==1}) {"Enter one row or one column."};return rows.flatten()}
 fun dot(a:List<Double>,b:List<Double>):Double {require(a.size==b.size) {"Vectors must have the same dimension."};return a.indices.sumOf {a[it]*b[it]}}
 fun norm(a:List<Double>):Double = sqrt(dot(a,a))
 fun binomial(n:Int,k:Int,p:Double):Double { require(n in 0..100 && k in 0..n && p in 0.0..1.0); var comb=1.0; for(i in 1..k) comb*= (n-i+1).toDouble()/i; return comb*p.pow(k)*(1-p).pow(n-k) }
}

/** Bounded arithmetic parser, never eval or a scripting engine. */
class ExpressionParser(private val source:String, private val x:Double?=null) {
 private val s=source.replace(" ","").replace("−","-").replace("²","^2"); private var at=0
 fun value():Double { require(s.length in 1..256); val result=add(); require(at==s.length) { "Unexpected input at ${at+1}" }; require(result.isFinite()) { "Answer is not finite." }; return result }
 private fun eat(c:Char):Boolean { if(at<s.length && s[at]==c) {at++; return true}; return false }
 private fun add():Double { var v=mul(); while(true) v=when {eat('+')->v+mul(); eat('-')->v-mul(); else->return v} }
 private fun mul():Double { var v=unary(); while(true) v=when {eat('*')->v*unary(); eat('/')->v/unary(); else->return v} }
 private fun unary():Double = when {eat('+')->unary(); eat('-')->-unary(); else->power()}
 private fun power():Double { val v=atom(); return if(eat('^')) v.pow(unary()) else v }
 private fun atom():Double {
  if(eat('(')) { val v=add(); require(eat(')')); return v }
  if(at<s.length && s[at].isLetter()) {
   val start=at; while(at<s.length && s[at].isLetter()) at++; val name=s.substring(start,at)
   if(name=="x") return requireNotNull(x) { "A numeric answer cannot contain an unknown variable." }; if(name=="pi") return PI; if(name=="e") return E
   require(eat('(')); val v=add(); require(eat(')')); return when(name) {"sin"->sin(v);"cos"->cos(v);"sqrt"->sqrt(v);"ln","log"->ln(v);"exp"->exp(v);"abs"->abs(v);else->error("Unknown function $name")}
  }
  val start=at; while(at<s.length && (s[at].isDigit() || s[at]=='.')) at++
  require(at>start) { "Expected a number." }; return s.substring(start,at).toDouble()
 }
}
