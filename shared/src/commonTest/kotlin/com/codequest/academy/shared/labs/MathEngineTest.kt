package com.codequest.academy.shared.labs

import kotlin.test.*

class MathEngineTest {
 @Test fun arithmeticAndFractions() {
  assertEquals(2.5,ExpressionParser("5/2").value())
  assertEquals(-4.0,ExpressionParser("-2^2").value())
  assertEquals(512.0,ExpressionParser("2^3^2").value())
  assertEquals(3.0,ExpressionParser("sqrt(9)").value())
  assertFails {ExpressionParser("1/0").value()}
  assertFails {ExpressionParser("import os").value()}
  assertFails {ExpressionParser("1+2 garbage").value()}
 }
 @Test fun matrixAndStatistics() {
  val product=MathEngine.multiply(MathEngine.matrix("1,2;3,4"),MathEngine.matrix("5,6;7,8"))
  assertEquals(listOf(listOf(19.0,22.0),listOf(43.0,50.0)),product)
  assertFails {MathEngine.matrix("1,2;3")}
  assertEquals(.375,MathEngine.binomial(4,2,.5))
  assertEquals(11.0,MathEngine.dot(listOf(1.0,2.0),listOf(3.0,4.0)))
  assertEquals(5.0,MathEngine.norm(MathEngine.vector("3,4")))
  assertFails {MathEngine.vector("1,2;3,4")}
  assertTrue(MathEngine.statistics(listOf(1.0,2.0,3.0)).contains("Mean = 2.0"))
 }
 @Test fun polynomialCheckingIsBoundedAndExactForSupportedTerms() {
  assertEquals(MathEngine.polynomial("2*x + 4"),MathEngine.polynomial("4+ x+x"))
  assertNotEquals(MathEngine.polynomial("2x+4"),MathEngine.polynomial("2x+5"))
  assertFails {MathEngine.polynomial("sin(x)")}
  assertFails {MathEngine.polynomial("x^999999")}
 }
 @Test fun deterministicTopicsAndGrading() {
  for(topic in MathTopic.values()) for(difficulty in 1..3) for(seed in 0..49) {
   val p=MathEngine.generate(topic,difficulty,seed)
   assertEquals(p,MathEngine.generate(topic,difficulty,seed))
   assertTrue(p.steps.size>=2)
   assertTrue(MathEngine.check(p,p.answer).correct,"$topic/$seed: ${p.answer}")
   assertFalse(MathEngine.check(p,"wrong").correct)
   if(p.type==MathAnswerType.Choice) {assertEquals(4,p.choices.size);assertEquals(1,p.choices.count {it==p.answer})}
  }
 }
 @Test fun numericToleranceAndMatrixDimensions() {
  val p=MathEngine.generate(MathTopic.Loss,1,1)
  assertTrue(MathEngine.check(p,"5/2").correct)
  assertTrue(MathEngine.check(p,"2.5000001").correct)
  assertFalse(MathEngine.check(p,"2.51").correct)
  assertFalse(MathEngine.check(p,"NaN").correct)
  assertFalse(MathEngine.check(p,"x+2.5").correct)
  val matrix=MathEngine.generate(MathTopic.Matrices,1,1)
  assertFalse(MathEngine.check(matrix,"1,2,3,4").correct)
 }
}
