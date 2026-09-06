package com.codequest.academy.shared.labs

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.QueryResult

/** Additive learner-owned tables. Curriculum importers never write these tables. */
class LabStore(private val driver:SqlDriver, private val user:()->String?) {
 init {
  driver.execute(null,"CREATE TABLE IF NOT EXISTS NousLabProject(user_id TEXT NOT NULL,id TEXT NOT NULL,name TEXT NOT NULL,source TEXT NOT NULL,updated_at INTEGER NOT NULL,PRIMARY KEY(user_id,id))",0)
  driver.execute(null,"CREATE TABLE IF NOT EXISTS NousLabAttempt(id INTEGER PRIMARY KEY AUTOINCREMENT,user_id TEXT NOT NULL,lab TEXT NOT NULL,topic TEXT NOT NULL,problem_id TEXT NOT NULL,answer TEXT NOT NULL,result TEXT NOT NULL,correct INTEGER NOT NULL,hints INTEGER NOT NULL,elapsed_ms INTEGER NOT NULL,created_at INTEGER NOT NULL)",0)
  driver.execute(null,"CREATE INDEX IF NOT EXISTS nous_lab_attempt_user_topic ON NousLabAttempt(user_id,lab,topic)",0)
 }
 fun saveProject(id:String,name:String,source:String) {
  val uid=user()?:return; require(source.length<=200000 && name.length in 1..100)
  driver.execute(null,"INSERT INTO NousLabProject(user_id,id,name,source,updated_at) VALUES(?,?,?,?,?) ON CONFLICT(user_id,id) DO UPDATE SET name=excluded.name,source=excluded.source,updated_at=excluded.updated_at",5) { bindString(0,uid);bindString(1,id);bindString(2,name);bindString(3,source);bindLong(4,System.currentTimeMillis()) }
 }
 fun projects():List<LabProject> {
  val uid=user()?:return emptyList()
  return driver.executeQuery(null,"SELECT id,name,source,updated_at FROM NousLabProject WHERE user_id=? ORDER BY updated_at DESC", { rows -> QueryResult.Value(buildList { while(rows.next().value) add(LabProject(rows.getString(0)!!,rows.getString(1)!!,rows.getString(2)!!,rows.getLong(3)!!)) }) },1) { bindString(0,uid) }.value
 }
 fun record(lab:String,topic:String,problem:String,answer:String,result:String,correct:Boolean,hints:Int,elapsed:Long) {
  val uid=user()?:return; require(answer.length<=200000 && result.length<=200000)
  driver.execute(null,"INSERT INTO NousLabAttempt(user_id,lab,topic,problem_id,answer,result,correct,hints,elapsed_ms,created_at) VALUES(?,?,?,?,?,?,?,?,?,?)",10) {
   bindString(0,uid);bindString(1,lab);bindString(2,topic);bindString(3,problem);bindString(4,answer);bindString(5,result);bindLong(6,if(correct) 1 else 0);bindLong(7,hints.coerceAtLeast(0).toLong());bindLong(8,elapsed.coerceAtLeast(0));bindLong(9,System.currentTimeMillis())
  }
 }
 fun summary(lab:String,topic:String):LabSummary {
  val uid=user()?:return LabSummary()
  return driver.executeQuery(null,"SELECT COUNT(*),COALESCE(SUM(correct),0),COALESCE(AVG(CASE WHEN correct=1 THEN MAX(0.25,1.0-hints*0.15) ELSE 0 END),0) FROM (SELECT correct,hints FROM (SELECT id,correct,hints,ROW_NUMBER() OVER(PARTITION BY problem_id ORDER BY id DESC) AS evidence_rank FROM NousLabAttempt WHERE user_id=? AND lab=? AND topic=?) WHERE evidence_rank=1 ORDER BY id DESC LIMIT 20)", { rows -> rows.next(); val n=rows.getLong(0)!!.toInt(); val correct=rows.getLong(1)!!.toInt(); val score=rows.getDouble(2)!!; val evidence=if(lab=="python") 1.0 else (n/5.0).coerceAtMost(1.0); QueryResult.Value(LabSummary(n,correct,(score*100*evidence).toInt())) },3) {bindString(0,uid);bindString(1,lab);bindString(2,topic)}.value
 }
}
