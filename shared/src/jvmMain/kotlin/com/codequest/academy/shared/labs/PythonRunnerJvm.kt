package com.codequest.academy.shared.labs

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

actual object PythonRunner {
 private val json=Json { ignoreUnknownKeys=true; encodeDefaults=true }
 @Volatile private var workspace:File?=null
 private val lock=Any()
 private var verifiedRuntime:File?=null
 private var verifiedHome:String?=null
 private var brokerChecksum:String?=null
 actual fun available()=System.getProperty("os.name").startsWith("Windows") && System.getProperty("os.arch") in listOf("amd64","x86_64") && (javaClass.getResource("/python-lab/runtime.zip")!=null || File("desktopApp/build/python-lab/NousPythonSandbox.exe").isFile)
 actual suspend fun run(request:PythonRequest):PythonResult=withContext(Dispatchers.IO) { synchronized(lock) {
  if(!available()) return@synchronized PythonResult("unavailable",error="The bundled Windows x64 isolated runner is unavailable on this platform.")
  if(request.source.length>200000) return@synchronized PythonResult("error",error="Source exceeds 200 KB.")
  var process:Process?=null; var work:File?=null
  try {
   val runtime=runtime(); val base=File(System.getProperty("user.home"),".nous-ai-academy/lab-runs").apply {mkdirs()}
   work=Files.createTempDirectory(base.toPath(),"run-").toFile(); workspace=work
   File(work,"request.json").writeText(json.encodeToString(request)); File(runtime,"bootstrap.py").copyTo(File(work,"bootstrap.py"))
   // Only this trusted broker launches a native process. No shell or learner command is interpolated.
   process=ProcessBuilder(File(runtime,"NousPythonSandbox.exe").absolutePath,runtime.absolutePath,work.absolutePath).directory(work).redirectErrorStream(true).redirectOutput(File(work,"broker.log")).start()
   if(!process.waitFor(45,TimeUnit.SECONDS)) { process.destroyForcibly(); process.waitFor(5,TimeUnit.SECONDS); return@synchronized PythonResult("timeout",error="The isolated run exceeded its deadline.") }
   val brokerError=File(work,"broker-error.txt"); val result=File(work,"result.json")
   when {
    brokerError.isFile -> PythonResult("stopped",error=brokerError.readText().take(4000))
    process.exitValue()!=0 -> PythonResult("unavailable",error=File(work,"broker.log").takeIf {it.isFile}?.readText()?.take(8000) ?: "Python process exited with ${process.exitValue()} (resource limit or isolation failure).")
    result.isFile && result.length()<=200000 -> json.decodeFromString<PythonResult>(result.readText())
    else -> PythonResult("error",error="The run ended without a valid result. No completion was recorded.")
   }
  } catch(e:Exception) { PythonResult("error",error=e.message?:"Python execution failed.") }
  finally { process?.takeIf {it.isAlive}?.let {it.destroyForcibly();it.waitFor(5,TimeUnit.SECONDS)}; workspace=null; work?.let { safeRemove(it) } }
 } }
 actual fun stop() { workspace?.let { File(it,"cancel").writeText("stop") } }
 private fun digest(file:File)=file.inputStream().use { input -> val md=MessageDigest.getInstance("SHA-256"); val b=ByteArray(65536); while(true) {val n=input.read(b);if(n<0)break;md.update(b,0,n)}; md.digest().joinToString("") {"%02x".format(it)} }
 private fun runtime():File {
  val home=System.getProperty("user.home")
  verifiedRuntime?.takeIf {verifiedHome==home}?.let { cached ->
   check(digest(File(cached,"NousPythonSandbox.exe"))==brokerChecksum) {"Python sandbox broker changed; restart to revalidate the runtime."}
   return cached
  }
  val bundled=javaClass.getResourceAsStream("/python-lab/runtime.zip")
  if(bundled==null) return File("desktopApp/build/python-lab").absoluteFile
  val hash=javaClass.getResourceAsStream("/python-lab/runtime.sha256")!!.bufferedReader().use {it.readText().trim()}
  val root=File(System.getProperty("user.home"),".nous-ai-academy/python/$hash"); val verified=File(root,"verified")
  if(!verified.isFile) {
   root.mkdirs(); val archive=File(root,"runtime.zip"); bundled.use { input -> archive.outputStream().use {input.copyTo(it)} }; check(digest(archive).equals(hash,true)) {"Bundled Python checksum mismatch"}
   ZipInputStream(archive.inputStream()).use { zip -> while(true) {val e=zip.nextEntry?:break;val target=File(root,e.name).canonicalFile;check(target.toPath().startsWith(root.canonicalFile.toPath())) {"Unsafe runtime path"};if(e.isDirectory)target.mkdirs() else {target.parentFile.mkdirs();target.outputStream().use {zip.copyTo(it)}}} }
   archive.delete(); verified.writeText(hash)
  } else bundled.close()
  // Full integrity validation once per application session. AppContainer cannot
  // modify these runtime files; the privileged broker is additionally hashed on every run.
  File(root,"files.sha256").readLines().filter {it.isNotBlank()}.forEach { line -> val expected=line.substring(0,64); val path=line.substring(66); check(digest(File(root,path)).equals(expected,true)) {"Python runtime changed: $path"} }
  brokerChecksum=digest(File(root,"NousPythonSandbox.exe"));verifiedHome=home;verifiedRuntime=root
  return root
 }
 private fun safeRemove(dir:File) { runCatching { Files.walk(dir.toPath()).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) } } } }
 actual fun chooseOpen():String? { var value:String?=null; onEdt { val chooser=JFileChooser().apply {dialogTitle="Open Python file"};if(chooser.showOpenDialog(null)==JFileChooser.APPROVE_OPTION) { val f=chooser.selectedFile;require(f.length()<=200000) {"File exceeds 200 KB"};value=f.readText() } };return value }
 actual fun chooseSave(source:String):Boolean {var saved=false;onEdt { val chooser=JFileChooser().apply {dialogTitle="Save Python file";selectedFile=File("main.py")};if(chooser.showSaveDialog(null)==JFileChooser.APPROVE_OPTION) {val f=chooser.selectedFile;if(!f.exists() || JOptionPane.showConfirmDialog(null,"Replace ${f.name}?","Confirm replacement",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION) {f.writeText(source);saved=true}} };return saved}
 private fun onEdt(action:()->Unit) {if(SwingUtilities.isEventDispatchThread())action() else SwingUtilities.invokeAndWait(action)}
}
