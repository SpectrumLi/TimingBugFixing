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

public class rollback{

    public static void main(String[] args){
	if (args.length < 1){
            System.out.println("Please give the input directory");
            return;
	}
	File exFile = null;
	try{
	    //exFile = new FileProvider().getFile("/home/haopliu/DFFix/FFix/code/FFix/Exclusion.txt");
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
	ArrayList<Gins> dir = new ArrayList<Gins>();
	ArrayList<String> glob = new ArrayList<String>();
	SSAInstruction s1 = ssas.get(0);
	SSAInstruction s2 = ssas2.get(ssas2.size()-1);
	System.out.println("IR = " + cn2.getIR());
	for (ISSABasicBlock bb : conf.getcontrolIB(cn2, s2)){
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
                if (s instanceof SSAPhiInstruction) continue;
                System.out.println(s);
                String t = conf.SSAtosrcloc(cn2,s).toString();
		Gins g = new Gins();
		g.addCGNode(cn2);
                dir.addAll(conf.getsideeffectinstructions(s,g,t,true));
	     }
         }
	// missing the post control IB
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

  System.out.println("--------DIRINS----------------");
	for (int st = 0; st < dir.size(); st ++){
       System.out.println(dir.get(st).toString()+"\n ");
	}
  System.out.println("--------GLOBINS----------------");
  for (int st = 0; st < glob.size(); st ++){
       System.out.println(glob.get(st)+"\n ");
  }

  
  rollback rb = new rollback();
  for (int st = 0; st < dir.size(); st ++){
    String line = dir.get(st).toString();
    int lNum = Integer.parseInt(line.substring(line.lastIndexOf(' ')+1));

    line = line.substring(line.lastIndexOf('<'), line.lastIndexOf('>'));
    line = line.substring(line.indexOf('L')+1, line.lastIndexOf(','));
    String cc = line;

    System.out.println(rb.getLineContent(inputdir+"/srcpath", cc, lNum, lNum).trim());
    Gins gin = dir.get(st);
    SSAInstruction inst = gin.list.get(gin.list.size()-1);
    IR ir = gin.cnlist.get(gin.list.size()-1).getIR();
    System.out.println("Inst: " + inst);
    System.out.println("1st use: " + NameParser.getUseName(ir, inst, 0));
    System.out.println();
  }

    }

  public String getLineContent(String srcPath, String cName, int beginLine, int endLine) {
    String rt = "";
    if (cName.indexOf('$') >= 0) cName = cName.substring(0, cName.indexOf('$'));
    cName = cName + ".java";
    try {
      BufferedReader br = new BufferedReader(new FileReader(srcPath));
      File fp = new File(br.readLine());

      ArrayList<String> files = new ArrayList<String>();
      getFiles(files, fp);
      String path = "", line = "";
      for (String s : files) if (s.endsWith(cName)) path = s;

      br = new BufferedReader(new FileReader(path));
      for (int i=1; i <= endLine; i++) {
        line = br.readLine();
        if (i >= beginLine) return line;
      }
    } catch  (Exception e) {}
    return "";
  }

  public void getFiles(ArrayList<String> files, File fp) {
    for (File f : fp.listFiles()) {
      if (f.isDirectory()) getFiles(files, f);
      else if (f.isFile()) files.add(f.getAbsolutePath());
    }
  }
}
