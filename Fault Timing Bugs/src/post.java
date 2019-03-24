package uchicago.ffix;

import java.io.*;
import java.util.*;

import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;

import uchicago.ffix.*;

public class post{

    public static void main(String[] args){
	if (args.length < 1){
            System.out.println("Please give the input directory");
            return;
	}
	File exFile = null;
	try{
	    exFile = new FileProvider().getFile("/home/haopliu/DFFix/FFix/code/FFix/Exclusion.txt");
	}catch(Exception e){
	    System.out.println("NO exclusion file");
	}
	String inputdir = args[0];
	config conf = new config(inputdir+"/jarpath",exFile);
	HashMap<String,String> map = new HashMap<String,String>();
	try{
	     InputStream fis = new FileInputStream(inputdir+"/config");
	     InputStreamReader isr = new InputStreamReader(fis);
	     BufferedReader br = new BufferedReader(isr);
	     String s ;
	     while ((s = br.readLine()) != null){
		String[] ss = s.split("=");
		map.put(ss[0],ss[1]);
	     }
    	}catch(Exception e){
	    e.printStackTrace();
	    System.out.println("ERROR in loading configure file");
	    return ;
	}
	srcloc sl = new srcloc(map.get("start"));
	srcloc slmis = new srcloc(map.get("missmsg"));
	conf.makecha();
	conf.dumpcha();
	//conf.makecfg();
	conf.makeemptycfg();
	CGNode cn = conf.getsrclocCGNode(sl);
	CGNode cnmis = conf.getsrclocCGNode(slmis);
	ArrayList<SSAInstruction> ssas = conf.getsrclocSSAInstruction(sl);
	ArrayList<SSAInstruction> ssasmis = conf.getsrclocSSAInstruction(slmis);
	//ArrayList<String> selist = conf.getsideeffectinstructions(cn,"");
	System.out.println(cn);
	System.out.println("--------------------=====---------------------");
	/*
	SSACFG ssac = cn.getIR().getControlFlowGraph();
	IR ir = cn.getIR();
        for (Iterator <SSAInstruction> iir = ir.iterateAllInstructions(); iir.hasNext();){
             SSAInstruction s = iir.next();
                    System.out.println(s);
                    //Statement stt = PDG.ssaInstruction2Statement(n,s,PDG.computeInstructionIndices(ir),ir);
                    //System.out.println(pdg.getNumber(stt));
                }
	*/
	ArrayList<Gins> dir = new ArrayList<Gins>();
	ArrayList<Gins> glob = new ArrayList<Gins>();
	SSAInstruction s1 = ssas.get(ssas.size()-1);
	SSAInstruction s2 = ssasmis.get(ssasmis.size()-1);
  /*
	for (ISSABasicBlock bb : conf.getcontrolIB(cn, s1)){
//      for (Iterator<ISSABasicBlock> ibb = ssacfg.iterator(); ibb.hasNext();){ISSABasicBlock bb = ibb.next$
	     int x,y;
	     x = bb.getFirstInstructionIndex();
	     y = bb.getLastInstructionIndex(); 
             System.out.println("BBlock----" + bb.getNumber()+ " "+x+"->"+y);
             SSAInstruction[] ss = cn.getIR().getInstructions();
             for (int z = x; z <=y ; z++){
                SSAInstruction s = ss[z];
                if (s instanceof SSAPhiInstruction) continue;
                System.out.println(s);
                String t = conf.SSAtosrcloc(cn,s).toString();
                Gins g = new Gins();
                g.addCGNode(cn);
                dir.addAll(conf.getsideeffectinstructions(s,g,t,true));
             }

         }
   System.out.println("--------DIRINS----------------");
       for (int st = 0; st < dir.size(); st ++){
       System.out.println(dir.get(st).toString()+"\n ");
   }
   System.out.println("--------DIRINS----------------");
*/
	// missing the post control IB
	conf.cleancachedse();
	for (ISSABasicBlock bb : conf.getcontrolIB(cnmis, s2)){
//      for (Iterator<ISSABasicBlock> ibb = ssacfg.iterator(); ibb.hasNext();){ISSABasicBlock bb = ibb.next$
	     int x,y;
             x = bb.getFirstInstructionIndex();
             y = bb.getLastInstructionIndex(); 
             System.out.println("BBlock----" + bb.getNumber()+ " "+x+"->"+y);
             SSAInstruction[] ss = cnmis.getIR().getInstructions();
             for (int z = x; z <=y ; z++){
                SSAInstruction s = ss[z];
                if (s instanceof SSAPhiInstruction) continue;
                System.out.println(s);
                String t = conf.SSAtosrcloc(cnmis,s).toString();
                Gins g = new Gins();
                g.addCGNode(cnmis);
                glob.addAll(conf.getsideeffectinstructions(s,g,t,false));
             }
         }

	/*
	for (SSAInstruction ssa : ssas){
	     System.out.println(ssa);
	     CGNode cn2 = conf.invokeSSAtoCGNode(ssa);
	     if (cn2 != null){
	         System.out.println(cn2.getIR().getControlFlowGraph());
                 //      System.out.println(cn2.getIR());
                 SSACFG ssacfg = cn2.getIR().getControlFlowGraph();

	         for (ISSABasicBlock bb : conf.getcontrolIB(cn, ssa)){
//      for (Iterator<ISSABasicBlock> ibb = ssacfg.iterator(); ibb.hasNext();){ISSABasicBlock bb = ibb.next();
                      System.out.println("BBlock----" + bb.getNumber());
                      for(Iterator <SSAInstruction> iir = bb.iterator(); iir.hasNext();){
          		  SSAInstruction s = iir.next();
          		  dir.addAll(conf.getsideeffectinstructions(s,"",s.toString()));
          		  System.out.println("    ~~~~~~  "+s);
         	      }
                 }
                 ArrayList<ISSABasicBlock> issbb = conf.getpostcontrolIB(cn, ssa);
                 if (issbb != null){
                     for (ISSABasicBlock bb : issbb){
//      for (Iterator<ISSABasicBlock> ibb = ssacfg.iterator(); ibb.hasNext();)$
                         System.out.println("BBlock----" + bb.getNumber());
                         for(Iterator <SSAInstruction> iir = bb.iterator(); iir.hasNext();){
                              SSAInstruction s = iir.next();
                              dir.addAll(conf.getsideeffectinstructions(s,"",s.toString()));
                              System.out.println("    ~~~~~~  "+s);
                         }
                     }
                 }
             }
	}

	*/
  System.out.println("--------GLOBINS----------------");
  for (int st = 0; st < glob.size(); st ++){
       System.out.println(glob.get(st).list.get(glob.get(st).list.size() -1)+"\n ");
       //System.out.println(glob.get(st).ssaString()+"\n ");
  }
  System.out.println("--------GLOBINS----------------");

  post obj = new post();
  //obj.idempotent(glob);
	/*//System.out.println("------------CONF---------------");
	ArrayList<Integer> l2 = new ArrayList<Integer>();
	l2.add(1);
	//conf.initfixvar();
	System.out.println("------------CONF---------------");
	//conf.fixvarset(cnmis, l2, "");*/

    }


    public void idempotent(ArrayList<Gins> globs) {
      System.out.println("HP");
      for (Gins glob : globs) {
        SSAInstruction last = glob.list.get(glob.list.size()-1);
        IR lastIR = glob.cnlist.get(glob.list.size()-1).getIR();
        System.out.println(last);
        if (last instanceof SSAInvokeInstruction) {
          com.ibm.wala.types.MemberReference methodRef = ((SSAInvokeInstruction)last).getCallSite().getDeclaredTarget();
          String invokeCC = methodRef.getDeclaringClass().getName().toString();
          String invokeMethod = methodRef.getName().toString();
          System.out.println(invokeCC + "::" + invokeMethod);
          if (invokeCC.equals("Lorg/apache/hadoop/fs/FileSystem")) {
            if (invokeMethod.equals("delete")) {
              deleteIdempotent((SSAInvokeInstruction)last, lastIR, invokeCC, invokeMethod);
            }
            else { System.out.println("todo"); }
          }
          else { System.out.println("todo"); }
        }
        System.out.println();

      }

    }

    public boolean deleteIdempotent(SSAInvokeInstruction delete, IR ir, String targetCC, String targetM) {
      boolean res = false;
      Iterator<CallSiteReference> it = ir.iterateCallSites();
      while (it.hasNext()) {
        CallSiteReference callsite = it.next();
        com.ibm.wala.types.MemberReference invokeRef = callsite.getDeclaredTarget();
        String invokeCC = invokeRef.getDeclaringClass().getName().toString();
        String invokeM = invokeRef.getName().toString();

        if (targetCC.equals(invokeCC) && invokeM.equals("exists") && targetM.equals("delete")) {
          System.out.println("callsite in m: " + invokeRef.getDeclaringClass().getName() + "::" + invokeRef.getName());
          if (ir.getCalls(callsite).length > 1) {
            System.out.println("one callsite has multiple calls. double check.");
            System.exit(-1);
          }
          SSAAbstractInvokeInstruction existInvoke = ir.getCalls(callsite)[0];
          System.out.println("call instruction: " + existInvoke + ";  2nd use: " + existInvoke.getUse(1));

          if (delete.getUse(1) == existInvoke.getUse(1)) {

            ISSABasicBlock deleteBB = ir.getBasicBlockForInstruction(delete);
            ISSABasicBlock existBB = ir.getBasicBlockForInstruction(existInvoke);

            //exception handling case
            Queue<ISSABasicBlock> queue = new LinkedList<>();
            Set<ISSABasicBlock> visited = new HashSet<>();
            for (ISSABasicBlock bb : ir.getControlFlowGraph().getExceptionalSuccessors(deleteBB)) {
              queue.offer(bb); visited.add(bb);
            }
            while (!queue.isEmpty()) {
              ISSABasicBlock cur = queue.poll();
              if (cur == existBB) {
                System.out.println("Yes, protected in the exception handler.");
                res = true;
                break;
              }
              for (ISSABasicBlock bb : ir.getControlFlowGraph().getNormalSuccessors(cur)) {
                if (!visited.contains(bb)) queue.offer(bb);
              }
            }

            //if case
          }
        }
      }
      return true;
    }

}
