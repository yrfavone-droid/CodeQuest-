package com.codequest.academy.desktop

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.codequest.academy.shared.labs.LabStore
import java.nio.file.Files
import kotlin.test.*

class LabStoreJvmTest {
 @Test fun restartUserIsolationAndAdditiveSchema() {
  val db=Files.createTempFile("nous-lab-store-",".db").toFile()
  try {
   var user="alice"
   JdbcSqliteDriver("jdbc:sqlite:${db.absolutePath}").use {driver->
    driver.execute(null,"CREATE TABLE ExistingNote(body TEXT)",0);driver.execute(null,"INSERT INTO ExistingNote VALUES ('keep me')",0)
    val store=LabStore(driver){user};store.saveProject("p1","A project","print(42)")
    repeat(5) {store.record("math","Probability","same-problem","answer","correct",true,0,300)}
    assertEquals(20,store.summary("math","Probability").mastery,"Repeating one item cannot manufacture independent evidence")
    repeat(5) {store.record("math","Algebra","problem:$it","answer","correct",true,0,300)}
    assertEquals(100,store.summary("math","Algebra").mastery)
    store.record("math","Algebra","bad","wrong","incorrect",false,1,500)
    assertTrue(store.summary("math","Algebra").mastery<100)
    user="bob";assertTrue(store.projects().isEmpty());assertEquals(0,store.summary("math","Algebra").attempts)
   }
   JdbcSqliteDriver("jdbc:sqlite:${db.absolutePath}").use {driver->
    val store=LabStore(driver){"alice"};assertEquals("print(42)",store.projects().single().source);assertEquals(6,store.summary("math","Algebra").attempts)
    val notes=driver.executeQuery(null,"SELECT body FROM ExistingNote",{r->r.next();app.cash.sqldelight.db.QueryResult.Value(r.getString(0))},0).value
    assertEquals("keep me",notes)
   }
  } finally {db.delete()}
 }
}
