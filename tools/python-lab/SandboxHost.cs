using System;
using System.IO;
using System.Text;
using System.Threading;
using System.Diagnostics;
using System.Runtime.InteropServices;
using System.Security.AccessControl;
using System.Security.Principal;

// The broker is trusted application code. Learner code runs in a capability-free
// Windows AppContainer assigned to a Job before its first instruction executes.
class SandboxHost {
 [StructLayout(LayoutKind.Sequential)] struct SI { public int cb; public IntPtr reserved,desktop,title; public int x,y,xs,ys,xc,yc,fill,flags; public short show,reserved2; public IntPtr reservedPtr,stdin,stdout,stderr; }
 [StructLayout(LayoutKind.Sequential)] struct SX { public SI si; public IntPtr attrs; }
 [StructLayout(LayoutKind.Sequential)] struct PI { public IntPtr process,thread; public int pid,tid; }
 [StructLayout(LayoutKind.Sequential)] struct Caps { public IntPtr sid,capabilities; public int count,reserved; }
 [DllImport("userenv.dll",CharSet=CharSet.Unicode)] static extern int CreateAppContainerProfile(string name,string display,string description,IntPtr caps,int count,out IntPtr sid);
 [DllImport("userenv.dll",CharSet=CharSet.Unicode)] static extern int DeleteAppContainerProfile(string name);
 [DllImport("advapi32.dll")] static extern IntPtr FreeSid(IntPtr sid);
 [DllImport("kernel32.dll",SetLastError=true)] static extern bool InitializeProcThreadAttributeList(IntPtr list,int count,int flags,ref IntPtr size);
 [DllImport("kernel32.dll",SetLastError=true)] static extern bool UpdateProcThreadAttribute(IntPtr list,int flags,IntPtr attribute,IntPtr value,IntPtr size,IntPtr old,IntPtr returned);
 [DllImport("kernel32.dll")] static extern void DeleteProcThreadAttributeList(IntPtr list);
 [DllImport("kernel32.dll",CharSet=CharSet.Unicode,SetLastError=true)] static extern bool CreateProcess(string app,StringBuilder args,IntPtr pa,IntPtr ta,bool inherit,uint flags,IntPtr env,string cwd,ref SX si,out PI pi);
 [DllImport("kernel32.dll",SetLastError=true)] static extern IntPtr CreateJobObject(IntPtr sa,string name);
 [DllImport("kernel32.dll",SetLastError=true)] static extern bool SetInformationJobObject(IntPtr job,int cls,IntPtr info,int length);
 [DllImport("kernel32.dll",SetLastError=true)] static extern bool AssignProcessToJobObject(IntPtr job,IntPtr process);
 [DllImport("kernel32.dll")] static extern bool TerminateJobObject(IntPtr job,uint code);
 [DllImport("kernel32.dll")] static extern bool TerminateProcess(IntPtr process,uint code);
 [DllImport("kernel32.dll")] static extern uint ResumeThread(IntPtr thread);
 [DllImport("kernel32.dll")] static extern uint WaitForSingleObject(IntPtr handle,uint ms);
 [DllImport("kernel32.dll")] static extern bool GetExitCodeProcess(IntPtr process,out uint code);
 [DllImport("kernel32.dll")] static extern bool CloseHandle(IntPtr h);
 static void Check(bool value,string step) { if(!value) throw new Exception(step+": Windows error "+Marshal.GetLastWin32Error()); }
 static long WorkspaceSize(string root) {
   long bytes=0; int count=0;
   var pending=new System.Collections.Generic.Stack<DirectoryInfo>(); pending.Push(new DirectoryInfo(root));
   while(pending.Count>0) foreach(var entry in pending.Pop().EnumerateFileSystemInfos()) {
     if((entry.Attributes & FileAttributes.ReparsePoint)!=0) throw new Exception("Workspace links are not allowed");
     if(++count>10000) throw new Exception("Workspace file-count limit exceeded");
     var directory=entry as DirectoryInfo;
     if(directory!=null) pending.Push(directory); else bytes+=((FileInfo)entry).Length;
     if(bytes>16*1024*1024) return bytes;
   }
   return bytes;
 }
 static void Grant(string path,SecurityIdentifier sid,FileSystemRights rights,bool add) {
   var rule=new FileSystemAccessRule(sid,rights,InheritanceFlags.ContainerInherit|InheritanceFlags.ObjectInherit,PropagationFlags.None,AccessControlType.Allow);
   var acl=Directory.GetAccessControl(path); if(add) acl.AddAccessRule(rule); else acl.RemoveAccessRuleSpecific(rule); Directory.SetAccessControl(path,acl);
 }
 static int Main(string[] args) {
  if(args.Length!=2) return 64;
  string runtime=Path.GetFullPath(args[0]), work=Path.GetFullPath(args[1]);
  string name="Nous.Lab."+Guid.NewGuid().ToString("N"); IntPtr sid=IntPtr.Zero,job=IntPtr.Zero,attrs=IntPtr.Zero,capPtr=IntPtr.Zero,env=IntPtr.Zero; PI pi=new PI(); SecurityIdentifier identity=null;
  try {
   if(!File.Exists(Path.Combine(runtime,"python.exe")) || !File.Exists(Path.Combine(work,"bootstrap.py"))) throw new Exception("Verified runtime or run request missing");
   int hr=CreateAppContainerProfile(name,name,"Temporary offline Nous learning run",IntPtr.Zero,0,out sid); if(hr<0) Marshal.ThrowExceptionForHR(hr);
   identity=new SecurityIdentifier(sid);
   // Runtime bytes are public, immutable application dependencies, not learner data.
   // A read-only package-group ACE avoids rewriting thousands of inherited ACLs
   // for every run. The workspace still grants only this run's unique SID.
   var readers=new SecurityIdentifier("S-1-15-2-1");
   bool readable=false;
   foreach(FileSystemAccessRule rule in Directory.GetAccessControl(runtime).GetAccessRules(true,true,typeof(SecurityIdentifier))) {
     if(rule.IdentityReference.Equals(readers) && rule.AccessControlType==AccessControlType.Allow && (rule.FileSystemRights & FileSystemRights.ReadAndExecute)==FileSystemRights.ReadAndExecute) readable=true;
   }
   if(!readable) Grant(runtime,readers,FileSystemRights.ReadAndExecute,true);
   Grant(work,identity,FileSystemRights.Modify,true);
   job=CreateJobObject(IntPtr.Zero,null); Check(job!=IntPtr.Zero,"Create job");
   // x64 JOBOBJECT_EXTENDED_LIMIT_INFORMATION: one process, 512 MiB, kill on broker exit.
   IntPtr limits=Marshal.AllocHGlobal(144); for(int i=0;i<144;i++) Marshal.WriteByte(limits,i,0);
   Marshal.WriteInt64(limits,0,10L*10000000); // PerProcessUserTimeLimit: ten seconds of CPU time
   Marshal.WriteInt32(limits,16,0x2000|0x100|0x8|0x2); Marshal.WriteInt32(limits,40,1); Marshal.WriteInt64(limits,112,512L*1024*1024);
   Check(SetInformationJobObject(job,9,limits,144),"Set resource limits"); Marshal.FreeHGlobal(limits);
   // Deny desktop/window/clipboard/global-atom interactions.
   IntPtr ui=Marshal.AllocHGlobal(4); Marshal.WriteInt32(ui,255); Check(SetInformationJobObject(job,4,ui,4),"Set UI restrictions"); Marshal.FreeHGlobal(ui);
   IntPtr size=IntPtr.Zero; InitializeProcThreadAttributeList(IntPtr.Zero,1,0,ref size); attrs=Marshal.AllocHGlobal(size); Check(InitializeProcThreadAttributeList(attrs,1,0,ref size),"Initialize capabilities");
   Caps caps=new Caps{sid=sid}; capPtr=Marshal.AllocHGlobal(Marshal.SizeOf(typeof(Caps))); Marshal.StructureToPtr(caps,capPtr,false);
   Check(UpdateProcThreadAttribute(attrs,0,new IntPtr(0x20009),capPtr,new IntPtr(Marshal.SizeOf(typeof(Caps))),IntPtr.Zero,IntPtr.Zero),"Set AppContainer");
   SX startup=new SX(); startup.si.cb=Marshal.SizeOf(typeof(SX)); startup.attrs=attrs;
   string system=Environment.GetFolderPath(Environment.SpecialFolder.Windows);
   env=Marshal.StringToHGlobalUni("ALLUSERSPROFILE="+Environment.GetFolderPath(Environment.SpecialFolder.CommonApplicationData)+"\0APPDATA="+work+"\0LOCALAPPDATA="+work+"\0OPENBLAS_NUM_THREADS=1\0PATH="+runtime+";"+system+"\\System32\0SystemRoot="+system+"\0TEMP="+work+"\0TMP="+work+"\0USERPROFILE="+work+"\0windir="+system+"\0\0");
   string exe=Path.Combine(runtime,"python.exe");
   Check(CreateProcess(exe,new StringBuilder("\""+exe+"\" -I -B \""+Path.Combine(work,"bootstrap.py")+"\""),IntPtr.Zero,IntPtr.Zero,false,0x00080000|0x08000000|0x4|0x400,env,work,ref startup,out pi),"Start isolated Python");
   Check(AssignProcessToJobObject(job,pi.process),"Assign Python to job"); ResumeThread(pi.thread);
   var clock=Stopwatch.StartNew(); string reason=null;
   while(WaitForSingleObject(pi.process,40)==258) {
    if(File.Exists(Path.Combine(work,"cancel"))) { reason="Stopped"; break; }
    if(clock.ElapsedMilliseconds>30000) { reason="Timeout: 30 second wall-clock limit"; break; }
    long bytes=WorkspaceSize(work);
    if(bytes>16*1024*1024) { reason="Workspace limit: 16 MiB"; break; }
   }
   if(reason!=null) { TerminateJobObject(job,124); File.WriteAllText(Path.Combine(work,"broker-error.txt"),reason); return 124; }
   uint code; GetExitCodeProcess(pi.process,out code); return (int)code;
  } catch(Exception e) { Console.Error.WriteLine("Isolation unavailable: "+e.ToString()); if(job!=IntPtr.Zero) TerminateJobObject(job,125); if(pi.process!=IntPtr.Zero) TerminateProcess(pi.process,125); return 125; }
  finally {
   if(job!=IntPtr.Zero) CloseHandle(job); if(pi.thread!=IntPtr.Zero) CloseHandle(pi.thread); if(pi.process!=IntPtr.Zero) CloseHandle(pi.process);
   if(attrs!=IntPtr.Zero) { DeleteProcThreadAttributeList(attrs); Marshal.FreeHGlobal(attrs); } if(capPtr!=IntPtr.Zero) Marshal.FreeHGlobal(capPtr); if(env!=IntPtr.Zero) Marshal.FreeHGlobal(env);
   DeleteAppContainerProfile(name); if(sid!=IntPtr.Zero) FreeSid(sid);
  }
 }
}
