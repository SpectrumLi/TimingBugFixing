package uchicago.ffix;

import java.io.*;
import java.util.*;

import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.ISSABasicBlock;

import uchicago.ffix.config;
import uchicago.ffix.srcloc;

public class toy{

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
	conf.makecfg();
	CGNode cn = conf.getsrclocCGNode(sl);
	ArrayList<SSAInstruction> ssas = conf.getsrclocSSAInstruction(sl);
	System.out.println(cn);
	System.out.println("------------------------------------------------");
	for (SSAInstruction ssa : ssas){
	    System.out.println(ssa);
	    System.out.println("CGNode : ");
	    CGNode cn2 = conf.invokeSSAtoCGNode(ssa);
	    if (cn2 != null){
	        //System.out.println(cn2.getIR().getControlFlowGraph());
                //System.out.println(cn2.getIR());
		for (ISSABasicBlock bb : conf.getcontrolIB(cn, ssa))
			System.out.println(bb);
      }
	}
    }

}
