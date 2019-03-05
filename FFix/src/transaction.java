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

import uchicago.ffix.config;
import uchicago.ffix.srcloc;
import uchicago.ffix.Gins;

public class transaction{

    public static void main(String[] args){
	if (args.length < 1){
            System.out.println("Please give the input directory");
            return;
	}
	File exFile = null;
	try{
	    exFile = new FileProvider().getFile(System.getenv("EF_Location"));
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
	srcloc sl2 = new srcloc(map.get("end"));
	//srcloc slmis = new srcloc(map.get("missmsg"));
	conf.makecha();
	//conf.makecfg();
	conf.makeemptycfg();
	CGNode cn = conf.getsrclocCGNode(sl);
	CGNode cn2 = conf.getsrclocCGNode(sl2);
	ArrayList<SSAInstruction> ssas = conf.getsrclocSSAInstruction(sl);
	ArrayList<SSAInstruction> ssas2 = conf.getsrclocSSAInstruction(sl2);
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
	ArrayList<String> dir = new ArrayList<String>();
	ArrayList<Gins> glob = new ArrayList<Gins>();
	SSAInstruction s1 = ssas.get(0);
	SSAInstruction s2 = ssas2.get(ssas2.size()-1);
	System.out.println("IR = " + cn2.getIR());
	for (ISSABasicBlock bb : conf.getcontrolIB(cn2, s1, s2)){
//	for (ISSABasicBlock bb : conf.getcontrolIB(cn2, s2)){
//      for (Iterator<ISSABasicBlock> ibb = ssacfg.iterator(); ibb.hasNext();){ISSABasicBlock bb = ibb.next$
             int x,y;
             x = bb.getFirstInstructionIndex();
             y = bb.getLastInstructionIndex(); 
             System.out.println("BBlock----" + bb.getNumber()+ " "+x+"->"+y);

	     //System.out.println(bb);
	     /*
             for(Iterator <SSAInstruction> iir = bb.iterator(); iir.hasNext();){
                    SSAInstruction s = iir.next();
                    if (s instanceof SSAPhiInstruction) continue;
	                  System.out.println(s);
                    String t = conf.SSAtosrcloc(cn2,s).toString();
                    dir.addAll(conf.getsideeffectinstructions(s,"",t,true));
//                    System.out.println("    ~~~~~~  "+s);
              }
	      */
	     SSAInstruction[] ss = cn2.getIR().getInstructions();
	     for (int z = x; z <=y ; z++){
		SSAInstruction s = ss[z];
		if (s == null) continue;
                if (s instanceof SSAPhiInstruction) continue;
		srcloc temp = conf.SSAtosrcloc(cn2,s);
		if (temp.getlinenum() < sl.getlinenum()) continue;
		if (temp.getlinenum() > sl2.getlinenum()) continue;
                String t = temp.toString();
		System.out.println(s);
                Gins g = new Gins();
		g.addCGNode(cn2);
                glob.addAll(conf.getsideeffectinstructions(s,g,t,false));
	     }
         }
/*
  System.out.println("--------DIRINS----------------");
	for (int st = 0; st < dir.size(); st ++){
       System.out.println(dir.get(st)+"\n ");
	}
  */
  System.out.println("--------GLOBINS----------------");

  for (int st = 0; st < glob.size(); st ++){
       System.out.println(glob.get(st).toString()+"\n ");
  }
  System.out.println(glob.size()+" GLOBINS");
 /* 
  HashMap<Integer, HashSet<Integer>> rela = new HashMap<Integer, HashSet<Integer>>();
  for (int st = 0; st < glob.size(); st ++){
	rela.put(st,conf.prefixed(st, glob));
	System.out.println(st +" = "+ rela.get(st));
  }
  */
  /*
  for (int st = 0; st < glob.size(); st ++){
	HashSet set = rela.get(st);
  }
  */
  /*
  for (String st : conf.creatednodes.keySet()){
          if (st.contains("copyQueuesFromRS")){
                  conf.dumpnode( conf.creatednodes.get(st));
                  break;
          }
  }
  */
 }
}
