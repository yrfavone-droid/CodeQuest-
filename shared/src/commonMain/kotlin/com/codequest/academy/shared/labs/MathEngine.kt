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
  require(difficulty in 1..3) { "Difficulty must be 1, 2 or 3" }
  val r=Random(seed); val a=r.nextInt(2,5+difficulty); val b=r.nextInt(1,5+difficulty); val c=r.nextInt(1,5)
  fun p(text:String, formula:String, answer:String, type:MathAnswerType=MathAnswerType.Number, steps:List<String>, choices:List<String> = emptyList(), graph:String?=null) = MathProblem("${topic.name}:$difficulty:$seed",topic,difficulty,text,formula,answer,type,steps,choices,graph)
  return when(topic) {
   MathTopic.Algebra -> p("Expand $a(x + $b).", "a(b + c) = ab + ac", "${a}x+${a*b}", MathAnswerType.Expression, listOf("Distribute $a to x and $b.", "${a}x + ${a*b}"), graph="$a(x+$b)")
   MathTopic.Equations -> p("Solve ${a}x + $b = ${a*c+b}.", "ax + b = d  ⇒  x = (d − b)/a", "$c", steps=listOf("Subtract $b: ${a}x = ${a*c}.", "Divide by $a: x = $c."))
   MathTopic.Inequalities -> p("Solve −${a}x < −${a*b}.", "Dividing by a negative reverses < and >.", "x > $b", MathAnswerType.Choice, listOf("Divide both sides by −$a.", "Reverse the inequality: x > $b."), listOf("x > $b","x < $b","x = $b","x ≥ $b"))
   MathTopic.Functions -> p("For f(x) = ${a}x² + $b, find f($c).", "f(x) = ax² + b", "${a*c*c+b}", steps=listOf("Square $c to get ${c*c}.", "Multiply by $a and add $b: ${a*c*c+b}."), graph="${a}x^2+$b")
   MathTopic.Exponents -> p("Find log₂(${1 shl a}).", "logᵦ(bⁿ) = n", "$a", steps=listOf("${1 shl a} = 2 raised to $a.", "The logarithm is the exponent: $a."), graph="log_2(x)")
   MathTopic.Summation -> p("Find 1 + 2 + … + ${a+b}.", "∑ᵢ₌₁ⁿ i = n(n + 1)/2", "${(a+b)*(a+b+1)/2}", steps=listOf("There are ${a+b} consecutive terms.", "(${a+b})(${a+b+1})/2 = ${(a+b)*(a+b+1)/2}."))
   MathTopic.Statistics -> p("Find the population variance of [$b, ${b+a}, ${b+2*a}].", "σ² = ∑(xᵢ − μ)² / n", "${2*a*a}/3", steps=listOf("Mean μ = ${b+a}.", "Squared deviations are ${a*a}, 0, ${a*a}.", "Divide their sum by 3: ${2*a*a}/3 ≈ ${formatMath(2.0*a*a/3,4)}."))
   MathTopic.Probability -> p("A bag has $a white and $b black counters. What is P(white) for one uniform draw?", "P(A) = favorable outcomes / total outcomes", "$a/${a+b}", steps=listOf("There are ${a+b} equally likely counters.", "The $a white counters give probability $a/${a+b} ≈ ${formatMath(a.toDouble()/(a+b),4)}."))
   MathTopic.Vectors -> p("Add vectors [$a, $b] and [$c, −$a].", "[u₁,u₂] + [v₁,v₂] = [u₁+v₁,u₂+v₂]", "${a+c},${b-a}", MathAnswerType.Vector, listOf("Add first components: $a + $c = ${a+c}.", "Add second components: $b − $a = ${b-a}."))
   MathTopic.Matrices -> p("Transpose [[$a, $b], [$c, ${a+b}]]. Enter rows separated by semicolons.", "(Aᵀ)ᵢⱼ = Aⱼᵢ", "$a,$c;$b,${a+b}", MathAnswerType.Matrix, listOf("Rows become columns.", "Result: [[$a,$c],[$b,${a+b}]]."))
   MathTopic.MatrixProduct -> p("Multiply [[$a,0],[0,$b]] by [[$c,1],[1,$c]].", "(AB)ᵢⱼ = ∑ₖ AᵢₖBₖⱼ", "${a*c},$a;$b,${b*c}", MathAnswerType.Matrix, listOf("Take each row of A dotted with each column of B.", "The diagonal scales row 1 by $a and row 2 by $b.", "Result: [[${a*c},$a],[$b,${b*c}]]."))
   MathTopic.LinearSystems -> p("Solve x + y = ${a+b}, x − y = ${a-b}. Enter [x,y].", "Elimination combines equations to remove a variable.", "$a,$b", MathAnswerType.Vector, listOf("Add equations: 2x = ${2*a}, so x=$a.", "Substitute: y=${a+b}−$a=$b."))
   MathTopic.Eigenvectors -> p("For A = [[$a,0],[0,$b]] and v = [1,0], find its eigenvalue λ.", "Av = λv", "$a", steps=listOf("Av = [$a,0].", "This is $a times v, hence λ=$a."))
   MathTopic.Svd -> p("Find the largest singular value of A = [[−$a,0],[0,$b]].", "σᵢ = √λᵢ(AᵀA)", "${max(a,b)}", steps=listOf("AᵀA has diagonal ${a*a}, ${b*b}.", "Square roots are $a and $b; largest is ${max(a,b)}."))
   MathTopic.Pca -> p("A centered dataset has covariance [[${a+b},0],[0,$b]]. Which direction is the first principal component?", "First PC maximizes vᵀΣv subject to ‖v‖ = 1.", "[1,0]", MathAnswerType.Choice, listOf("The eigenvalues are ${a+b} and $b.", "The larger eigenvalue corresponds to [1,0]. Sign reversal is equivalent."), listOf("[1,0]","[0,1]","[1,1] / √2","No direction exists"))
   MathTopic.Derivatives -> p("Differentiate f(x) = ${a}x² + ${b}x.", "d(xⁿ)/dx = nxⁿ⁻¹", "${2*a}x+$b", MathAnswerType.Expression, listOf("Differentiate each term.", "f′(x) = ${2*a}x + $b."), graph="${a}x^2+${b}x")
   MathTopic.Gradients -> p("For f(x,y) = x² + ${a}y², find ∇f at ($b,$c).", "∇f = [∂f/∂x, ∂f/∂y]", "${2*b},${2*a*c}", MathAnswerType.Vector, listOf("Partial derivatives: 2x and ${2*a}y.", "At ($b,$c), gradient = [${2*b},${2*a*c}]."))
   MathTopic.ChainRule -> p("Find the derivative of (${a}x + $b)² at x=$c.", "d[g(h(x))]/dx = g′(h(x))h′(x)", "${2*a*(a*c+b)}", steps=listOf("Outer derivative: 2(${a}x+$b). Inner derivative: $a.", "At x=$c: 2 × ${a*c+b} × $a = ${2*a*(a*c+b)}."))
   MathTopic.Optimization -> p("For L(w)=w², start w=$a. Take one gradient descent step with rate 0.1.", "wₙ₊₁ = wₙ − η∇L(wₙ)", formatMath(a*0.8), steps=listOf("Gradient is 2w = ${2*a}.", "New w = $a − 0.1 × ${2*a} = ${formatMath(a*0.8)}."), graph="x^2")
   MathTopic.Loss -> p("Targets are [$a,$b], predictions are [${a+1},${b-2}]. Find mean squared error.", "MSE = (1/n)∑(ŷᵢ − yᵢ)²", "2.5", steps=listOf("Errors are 1 and −2.", "Squared errors: 1 and 4. Mean = 5/2 = 2.5."))
  }
 }

 /** Removes a leading "x =", "λ =", "answer:" etc. so "x = 3" is accepted for a numeric answer. */
 private fun stripLabel(input:String):String = input.trim().replace(Regex("^(?:[a-zA-Zλσμ]\\w*\\s*=|answer\\s*[:=])\\s*"),"")

 fun check(p:MathProblem, input:String):MathVerdict {
  if(p.type==MathAnswerType.Explanation) return MathVerdict(false,"This reasoning response requires self-review against the solution. It is not automatically graded.")
  if(input.isBlank()) return MathVerdict(false,"Enter an answer first.")
  return try {
   val correct=when(p.type) {
    MathAnswerType.Number -> numberMatches(stripLabel(input),p.answer,p.tolerance)
    MathAnswerType.Choice -> input.trim()==p.answer
    MathAnswerType.Expression -> {
     val given=input.trim().replace(Regex("^(?:f\\s*'?\\s*\\(\\s*x\\s*\\)|f′\\(x\\)|y)\\s*=\\s*"),"")
     if(p.topic==MathTopic.Algebra && given.contains('(')) return MathVerdict(false,"Equivalent forms with parentheses are not expanded yet. Multiply out the brackets.")
     equivalentInX(given,p.answer)
    }
    MathAnswerType.Vector,MathAnswerType.Matrix -> {
     val actual=matrix(input); val expected=matrix(p.answer)
     val flatOk = p.type==MathAnswerType.Vector && actual.flatten().size==expected.flatten().size && actual.flatten().indices.all { close(actual.flatten()[it],expected.flatten()[it],p.tolerance) }
     flatOk || (actual.size==expected.size && actual.indices.all { row -> actual[row].size==expected[row].size && actual[row].indices.all { col -> close(actual[row][col],expected[row][col],p.tolerance) } })
    }
    MathAnswerType.Explanation -> false
   }
   MathVerdict(correct,if(correct) "Correct" else "Needs review. Compare your work with the solution below.")
  } catch(e:Exception) { MathVerdict(false,"Check the answer format: ${e.message}") }
 }

 /** Exact (within tolerance) match, or a correctly rounded decimal with at least 2 decimal places. */
 private fun numberMatches(input:String, answer:String, tolerance:Double):Boolean {
  val actual=ExpressionParser(input).value(); val expected=ExpressionParser(answer).value()
  if(close(actual,expected,tolerance)) return true
  val decimals=Regex("^-?\\d*\\.(\\d{2,})$").matchEntire(input.trim().replace("−","-"))?.groupValues?.get(1)?.length ?: return false
  return abs(actual-expected) <= 0.5*10.0.pow(-decimals)+1e-12
 }

 /** Compares two expressions in x by evaluating them at several points (accepts any equivalent form). */
 fun equivalentInX(a:String, b:String):Boolean {
  val ea=MathParser.parse(a); val eb=MathParser.parse(b)
  val extra=(ea.variables()-"x")
  require(extra.isEmpty()) { "Only the variable x may be used (found ${extra.joinToString()})" }
  val points=doubleArrayOf(-2.7,-1.3,-0.4,0.6,1.1,2.3,3.9)
  var compared=0
  for(x in points) {
   val va=Evaluator().at(x).eval(ea); val vb=Evaluator().at(x).eval(eb)
   if(!va.isFinite() && !vb.isFinite()) continue
   if(!close(va,vb,1e-7)) return false
   compared++
  }
  return compared>=3
 }

 private fun close(a:Double,b:Double,t:Double)=a.isFinite() && abs(a-b)<=t*max(1.0,abs(b))

 /** Accepts 1,2;3,4 · [[1,2],[3,4]] · [1, 2], [3, 4] · one row per line · (1, 2). */
 fun matrix(text:String):List<List<Double>> {
  var s=MathText.normalize(text).trim()
  require(s.isNotEmpty()) { "Enter numbers separated by commas" }
  // MathText maps [ ] to ( ); treat "),(" and new lines as row separators
  s=s.replace(Regex("\\)\\s*,?\\s*\\("),";").replace("\n",";")
  if(s.startsWith("(") && s.endsWith(")")) s=s.removePrefix("((").removeSuffix("))").removePrefix("(").removeSuffix(")")
  val rows=s.split(';').map { it.trim().trim('(',')').trim() }.filter { it.isNotEmpty() }.map { row ->
   row.split(',').map { cell -> require(cell.isNotBlank()) { "Empty cell in row \"$row\"" }; ExpressionParser(cell.trim()).value() }
  }
  require(rows.isNotEmpty() && rows.all { it.size==rows[0].size }) { "Use equal-length rows separated by semicolons, e.g. 1,2;3,4" }
  return rows
 }

 fun polynomial(text:String):Map<Int,Double> {
  val s=text.lowercase().replace(" ","").replace("*","").replace("−","-").replace("²","^2")
  require(s.isNotEmpty() && s.length<256) { "Enter an expanded polynomial in x." }
  val terms=Regex("[+-]?[^+-]+").findAll(s).map { it.value }.toList(); require(terms.joinToString("")==s) { "Enter an expanded polynomial in x." }
  val result=mutableMapOf<Int,Double>()
  terms.forEach { term -> val parts=term.split('x'); require(parts.size<=2) { "Each term may contain x once." }
   val power=if(parts.size==1) 0 else if(parts[1].isEmpty()) 1 else { require(parts[1].startsWith('^')) { "Write powers as x^2." }; parts[1].drop(1).toInt().also { require(it in 0..8) { "Powers must be between 0 and 8." } } }
   val coefficient=when(parts[0]) { "","+" -> 1.0; "-" -> -1.0; else -> ExpressionParser(parts[0]).value() }
   result[power]=(result[power]?:0.0)+coefficient
  }; return result.filterValues { it!=0.0 }
 }
 fun multiply(a:List<List<Double>>,b:List<List<Double>>):List<List<Double>> { require(a[0].size==b.size) { "Columns of A (${a[0].size}) must equal rows of B (${b.size})." }; return a.map { row -> b[0].indices.map { j -> row.indices.sumOf { k -> row[k]*b[k][j] } } } }

 /** Splits a list of numbers separated by commas, semicolons, spaces or new lines. */
 fun numberList(text:String):List<Double> = MathText.normalize(text).split(Regex("[,;\\s]+")).filter { it.isNotBlank() }.map { ExpressionParser(it).value() }
  .also { require(it.isNotEmpty()) { "Enter at least one number" } }

 fun statistics(values:List<Double>):String {
  require(values.isNotEmpty()) { "Enter at least one number" }
  val sorted=values.sorted(); val n=values.size; val mean=values.average(); val variance=values.sumOf { (it-mean).pow(2) }/n
  val median=if(n%2==1) sorted[n/2] else (sorted[n/2-1]+sorted[n/2])/2
  val sample=if(n>1) values.sumOf { (it-mean).pow(2) }/(n-1) else Double.NaN
  return buildString {
   appendLine("n = $n"); appendLine("Sum = ${formatMath(values.sum())}")
   appendLine("Mean = $mean"); appendLine("Median = ${formatMath(median)}")
   appendLine("Population variance = ${formatMath(variance)}"); appendLine("Standard deviation = ${formatMath(sqrt(variance))}")
   appendLine("Sample variance = ${if(n>1) formatMath(sample) else "needs 2+ values"}"); appendLine("Sample standard deviation = ${if(n>1) formatMath(sqrt(sample)) else "needs 2+ values"}")
   appendLine("Minimum = ${formatMath(sorted.first())}"); appendLine("Maximum = ${formatMath(sorted.last())}"); append("Range = ${formatMath(sorted.last()-sorted.first())}")
  }
 }
 fun vector(text:String):List<Double> {val rows=matrix(text);require(rows.size==1 || rows.all {it.size==1}) {"Enter one row or one column."};return rows.flatten()}
 fun dot(a:List<Double>,b:List<Double>):Double {require(a.size==b.size) {"Vectors must have the same dimension (${a.size} vs ${b.size})."};return a.indices.sumOf {a[it]*b[it]}}
 fun norm(a:List<Double>):Double = sqrt(dot(a,a))
 fun binomial(n:Int,k:Int,p:Double):Double {
  require(n in 0..1000) { "n must be a whole number from 0 to 1000" }
  require(k in 0..n) { "k must be a whole number from 0 to n" }
  require(p in 0.0..1.0) { "p must be between 0 and 1" }
  var comb=1.0; for(i in 1..k) comb*= (n-i+1).toDouble()/i; return comb*p.pow(k)*(1-p).pow(n-k)
 }
 /** Parses a whole number such as "10", " 10 ", "1e1"-free input, or an expression that evaluates to an integer. */
 fun wholeNumber(text:String, label:String):Int {
  val v=try { ExpressionParser(text).value() } catch(e:Exception) { throw IllegalArgumentException("$label: ${e.message}") }
  require(abs(v-v.roundToLong())<1e-9) { "$label must be a whole number" }
  return v.roundToLong().toInt()
 }
 fun binomialReport(nText:String,kText:String,pText:String):String {
  val n=wholeNumber(nText,"Trials n"); val k=wholeNumber(kText,"Successes k"); val p=ExpressionParser(pText).value()
  val exact=binomial(n,k,p); val atMost=(0..k).sumOf { binomial(n,it,p) }
  return "P(X = $k) = ${formatMath(exact,8)}\nP(X ≤ $k) = ${formatMath(atMost,8)}\nP(X ≥ $k) = ${formatMath(1-atMost+exact,8)}\nMean np = ${formatMath(n*p)}\nVariance np(1−p) = ${formatMath(n*p*(1-p))}"
 }
}

/** Number-only evaluator kept for answer checking. Uses the shared safe parser (never eval or a scripting engine). */
class ExpressionParser(private val source:String, private val x:Double?=null) {
 fun value():Double {
  require(source.isNotBlank()) { "Enter a value" }
  require(source.length<=256) { "Input is too long" }
  val e=MathParser.parse(source)
  val ev=Evaluator(); if(x!=null) ev.at(x)
  val result=ev.eval(e)
  require(result.isFinite()) { "Answer is not finite (undefined)" }
  return result
 }
}
