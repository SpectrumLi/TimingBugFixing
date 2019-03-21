package uchicago.ffix;

import java.io.*;
import java.util.*;

import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SymbolTable;

import uchicago.ffix.config;
import uchicago.ffix.srcloc;

public class nodedump{

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
	conf.makecha();
	//conf.makecfg();
	conf.makeemptycfg();
	CGNode cn = conf.getsrclocCGNode(sl);
	//ArrayList<SSAInstruction> ssas = conf.getsrclocSSAInstruction(sl);
	//ArrayList<String> selist = conf.getsideeffectinstructions(cn,"");
	//System.out.println(cn);
	System.out.println("------------------------------------------------");
	SSACFG ssac = cn.getIR().getControlFlowGraph();
	IR ir = cn.getIR();
	SymbolTable st = ir.getSymbolTable();
	System.out.println("IR  = "+ir);
//	System.out.println("CFG = "+ ssac);
	System.out.println("----------------------");
	SSAInstruction[] ssas = ir.getInstructions();
//        for (Iterator <SSAInstruction> iir = ir.iterateAllInstructions(); iir.hasNext();){
	for (int ix = 0; ix < ssas.length; ix ++){
	     System.out.println("");
             SSAInstruction s = ssas[ix];
	     if (s == null) continue;
             System.out.println(s);
	     String t = conf.SSAtosrcloc(cn,s).toString();
	     
	     System.out.print("    read ");
	     for (int i = 0; i< s.getNumberOfUses() ; i++){
		//System.out.print(" " + ir.getLocalNames(ix,s.getUse(i)));
		System.out.println(" " + st.getValueString(s.getUse(i)));
	     }
	     System.out.print("\n    write ");
             for (int i = 0; i< s.getNumberOfDefs() ; i++){
                System.out.print(" " + ir.getLocalNames(ix,s.getDef(i)));
             }

		
	     System.out.println(t);
                    //Statement stt = PDG.ssaInstruction2Statement(n,s,PDG.computeInstructionIndices(ir),ir);
                    //System.out.println(pdg.getNumber(stt));
        }
    }

}

