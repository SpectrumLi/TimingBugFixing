package uchicago.dfix;

import java.io.*;
import java.util.*;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.classLoader.IBytecodeMethod;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.PDG;
import com.ibm.wala.ipa.slicer.NormalReturnCaller;
import com.ibm.wala.ipa.callgraph.impl.BasicCallGraph;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import uchicago.dfix.Stateer;

public class SSALocator{

    public CallGraph cg;
    public ArrayList<ArrayList<callstackItem>> css;
    public ArrayList<ArrayList<SSAInstruction>> ssas;
    public ArrayList<ArrayList<Statement>> sts;
    public HashMap<callstackItem, IMethod> csi2mr;
    public ArrayList<CGNode> nodes;

    public SSALocator(CallGraph cg,String dir){
	css = new ArrayList<ArrayList<callstackItem>>();
        sts = new ArrayList<ArrayList<Statement>>();
	ssas= new ArrayList<ArrayList<SSAInstruction>>();
	nodes = new ArrayList<CGNode>();
	csi2mr = new HashMap<callstackItem, IMethod>();
	this.cg = cg;
	
	try {
	    InputStream fis = new FileInputStream(dir+"/css");
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            String s;
	    ArrayList<callstackItem> temcs= new ArrayList<callstackItem>();
            while ((s= br.readLine())!=null){
		String [] tems = s.split(" ");
		if (tems.length<3){
		   css.add(temcs);
		   temcs = new ArrayList<callstackItem>(); 
		} else{
		   System.out.println("    Load "+ s);
		   temcs.add(new callstackItem(tems[0],tems[1],tems[2]));
		}
	    }
//	    System.out.println("Last added list size "+temcs.size());
	    css.add(temcs);
	}catch(Exception e){
	    System.out.println("Callstack file load error!");
	    e.printStackTrace();
	}
	
    }
    
    public void process(){
	
	for (int l = 0; l < css.size(); l++){
	    ArrayList<callstackItem> cs = css.get(l);
	    //CGNode cn = cg.getFakeRootNode();
	    CGNode cn;
	    //Collection<CGNode> cns = cg.getEntrypointNodes();
	    HashSet<CGNode> cns = new HashSet<CGNode>();
	    //System.out.println("------EntrypointSize"+ cns.size());
	    int x;
	    for (x = 0; x< cs.size(); x++){
		cns.add(cg.getFakeRootNode());
		cn = BFSFindCGNode(cg, cns, cs.get(x));
		cns = new HashSet<CGNode>();
		if (cn != null){
		    nodes.add(cn);
		    sts.add(csItem2statement(cn,cs.get(x)));
		    cns.add(cn);
		}
		cns.add(cg.getFakeRootNode());
	    }
	}
    }
   
    public void setIMethod(IClassHierarchy cha){
	for (int l=0; l < css.size(); l++){
	    ArrayList<callstackItem> cs = css.get(l);
	    for (callstackItem csi : cs){
		for (IClass c : cha){
                    String cname = c.getName().toString();
                    if (cname.equals("L"+csi.cname)){
			System.out.println("Get class:"+cname);
                        for (IMethod m : c.getAllMethods()){
                             String mname = m.getName().toString();
			     if (mname.equals(csi.mname)){
                                 System.out.println("Get method:"+mname);
				 if (csi2mr.get(csi) == null){
				     System.out.println(csi + " m:" + m);
				     csi2mr.put(csi, m);
				 }else
				     System.out.println("Multiple MR for "+ csi);
			     }
                        }
                    }
                }
		if (csi2mr.get(csi) == null){
		    System.out.println(csi + " is not in the ClassHierarchy");
		}
	    }
	}
    }

    public ArrayList<Statement> csItem2statement(CGNode cn, callstackItem cs){

	//System.out.println("   Process " + l+"th Node found. Now look for the SSA");
	IR ir = cn.getIR();
	SSAInstruction [] ssaset = ir.getInstructions();
	int index = 0;
	ArrayList<SSAInstruction> tssa = new ArrayList<SSAInstruction>();
	ArrayList<Statement> tstat = new ArrayList<Statement>();
	for (index = 0; index < ssaset.length ;index++ ){
	    SSAInstruction s = ssaset[index];
	    if (s == null) continue;
	    //System.out.println(s);
	    try {
       	        int bcIndex =((IBytecodeMethod)cn.getMethod()).getBytecodeIndex(index);
  	        int sln= cn.getMethod().getLineNumber(bcIndex);
	        //System.out.println(cs.mname+" sln "+ sln + " Ins:"+s);
	        if (sln == cs.lnum){
		     //System.out.println(s);
		     //sts.add(PDG.ssaInstruction2Statement(cn,s,PDG.computeInstructionIndices(ir),ir));
		     //ssas.add(s);
		    tstat.add(PDG.ssaInstruction2Statement(cn,s,PDG.computeInstructionIndices(ir),ir));
		    tssa.add(s);
	        }
	    }catch (Exception e){
	        e.printStackTrace();
	    }
        }
        System.out.println(tssa.size()+" SSAs loaded");
	ssas.add(tssa);
	return tstat;
   }

    public static ArrayList<Statement> insideSlicing(CGNode node, ArrayList<Statement> ori,SDG sdg){
	LinkedList<Statement> queue = new LinkedList<Statement>();
        ArrayList<Statement> pslicer = new ArrayList<Statement>();
        LinkedList<Integer> parent = new LinkedList<Integer>();
        HashSet<String> visited = new HashSet<String>();
	PDG pdg = sdg.getPDG(node);
	System.out.println("PDG SIZE ->" + pdg.getMaxNumber());
	String slnum = " ";
	for (Statement sm : ori ){
	    queue.add(sm);
	    //System.out.println("############################### Init Slicing "  + sm);
	    int smindex = Stateer.SSAAssignNumber(sm);
	    if (smindex > -1){
		PointerKey pkey = Stateer.pa.getHeapModel().getPointerKeyForLocal(node, smindex);	
	        Stateer.pkeyset.add(pkey);
		//System.out.println("Load PointerKey -> "+pkey);
	    }
	    slnum = ssat.srclocation(sm);
            visited.add(getIdentity(sm));
	}
        while (queue.size()!=0){
                Statement st = queue.poll();
                pslicer.add(st);
                //System.out.println(parent.poll() + " : "+getIdentity(st));
                //index ++;
                System.out.println(st);
                try{
                    for (Iterator <Statement> its = pdg.getPredNodes(st); its.hasNext();){
                        Statement s = its.next();
			//System.out.println("     #####    "+s.getKind()+"-"+s);
			if (s.getKind().toString().equals("NORMAL_RET_CALLER")){ 
			    s =PDG.ssaInstruction2Statement(node, ((NormalReturnCaller)s).getInstruction(), PDG.computeInstructionIndices(node.getIR()),node.getIR());
			};
			System.out.println("     #####    "+s.getKind()+"-"+s);
                        if (s.getNode().getMethod().getDeclaringClass().getClassLoader().getReference().equals(ClassLoaderReference.Application)){
			    
                            //if ((s.getKind().toString().equals("NORMAL"))&&(!visited.contains(getIdentity(s))) && (!pdg.isControlDependend(s,st))){
                            if ((s.getKind().toString().equals("NORMAL"))&&(!visited.contains(getIdentity(s))) && (!slnum.equals(ssat.srclocation(s)))){
				System.out.println("ADD  #####    "+s.getKind()+"-"+s);
                                queue.add(s);
                                visited.add(getIdentity(s));
                  //              parent.add(index);
                            }
                        }
                    }
		    if (isNewObjectInvokation(st)){
			String linenum = ssat.srclocation(st);
			//System.out.println("------- looking for lnum "+ linenum);
			IR ir = node.getIR();
			for (Iterator <SSAInstruction> iir = ir.iterateAllInstructions(); iir.hasNext();){
                            SSAInstruction ss = iir.next();
			    Statement s = PDG.ssaInstruction2Statement(node,ss,PDG.computeInstructionIndices(ir),ir);
                            String lnum = ssat.srclocation(s);
			    //System.out.println("              meet lnum "+ lnum);
		
                            if ((s.getKind().toString().equals("NORMAL"))&&(!visited.contains(getIdentity(s))) && (linenum.equals(lnum))){
                                queue.add(s);
                                visited.add(getIdentity(s));
                  //              parent.add(index);
                            }
                        }
		    }
                }catch (IllegalArgumentException e){
                    System.out.println("One node is not in the PDG!");
                    e.printStackTrace();
                }
            }
                IR ir = node.getIR();
                for (Iterator <SSAInstruction> it = ir.iterateAllInstructions(); it.hasNext();){
                    Statement s =PDG.ssaInstruction2Statement(node,it.next(),PDG.computeInstructionIndices(ir),ir);
                    if (!pslicer.contains(s)){
                        int snum = Stateer.SSAAssignNumber(s);
                        //System.out.println("Check PointerKey -> "+snum);
                        if (snum > -1){
                            PointerKey pk = Stateer.pa.getHeapModel().getPointerKeyForLocal(node, snum);
                            //System.out.println("Check PointerKey -> "+pk);
                            if (Stateer.pkeyset.contains(pk ))
                                pslicer.add(s);
                        }
                    }
                }

	System.out.println("    A size "+pslicer.size() + " is found");
	return pslicer;
    }
    public static String getIdentity(Statement s){
        //return s.toString();
        //return srclocation(s);
        return ssat.srclocation(s)+ " " +s.toString();
    }

    public static boolean isNewObjectInvokation(Statement s){
	String [] ss = s.toString().split("=");
	if (ss.length <2) return false;
	if (ss[1].startsWith(" new") ){
//	    System.out.println(s + "is a new");
	    return true;
	}else{
	    return false;
	}
    }
    
    public CGNode BFSFindCGNode(CallGraph cg, Collection<CGNode> start, callstackItem csi){
	LinkedList<CGNode> queue = new LinkedList<CGNode>();
        HashSet<String> visited = new HashSet<String>();
	for (CGNode cn : start){
	    queue.add(cn);
	    visited.add(Integer.toString(System.identityHashCode(cn)));
	}
	while (queue.size()!=0){
	    CGNode n = queue.poll();
	    MethodItem mi1 = new MethodItem(n.getMethod().toString());
	    /*
	    if (mi1.cname.contains("MRClientProtocolHandler")){
	        System.out.println(n.getMethod().toString());
	    }*/
	    //System.out.println(n.getMethod().toString());
            if (mi1.cname.equals(csi.cname)&&(mi1.mname.equals(csi.mname)))
                return n;
            for (Iterator<? extends CGNode> inode = cg.getSuccNodes(n); inode.hasNext();){
                CGNode n2= inode.next();
                String idn2=Integer.toString(System.identityHashCode(n2));
                if (!visited.contains(idn2)){
                    queue.add(n2);
                    visited.add(idn2);
                }
	  	MethodItem mi = new MethodItem(n2.getMethod().toString());
		if (mi.cname.equals(csi.cname)&&(mi.mname.equals(csi.mname))) 
		    return n2;	
		//if () NEED CODE IMPLEMENTAION FOR EXIT
	    }
	}
	
	System.out.println("ERROR in find "+csi.cname+" "+csi.mname+ " " + csi.lnum + " in callgraph from the entrypoint");
	IMethod im = csi2mr.get(csi);
	if (im == null)
	    System.out.println("This method is not found in the class "+ csi);
	MethodReference mr = im.getReference();
	Set<CGNode> callnodes = cg.getNodes(mr);
	if (callnodes.size() == 0){
	    System.out.println("No callnodes??? for " +csi);
	    //return cg.findOrCreateNode(mr,Everywhere.EVERYWHERE);
	    try{
	        CGNode cgnode = ((BasicCallGraph<CGNode>)cg).findOrCreateNode(im,Everywhere.EVERYWHERE);
		dumpNode(cgnode);
		System.out.println("Create new node in the CG " + cgnode );
		return cgnode;
	    }catch (Exception e){
		System.out.println("Create new node failed!");
	        return null;
	    }
	    //return null;
	}
	else if (callnodes.size() > 1){
	    System.out.println("Multiple callnodes??? for " +csi);
	    return null;
	} else
	    for (CGNode cn : callnodes)
		return cn;	
  	return null;	
    }
   public void dumpNode(CGNode n){
	System.out.println(" SELF NODE "+ n);
	IR ir = n.getIR();
        for (Iterator <SSAInstruction> iir = ir.iterateAllInstructions(); iir.hasNext();){
             SSAInstruction s = iir.next();
	     if (s instanceof SSANewInstruction){
		//System.out.println("@@@@@@@@@@ New instuction found");
		HashSet<Integer> para = newinstpara(s);
		for (int x : para) System.out.print(x+" ");
	     }
             System.out.println(s);
                
        } 
   }
   public HashSet<Integer> newinstpara(SSAInstruction s){
        SSANewInstruction sn = (SSANewInstruction) s;
	HashSet answer = new HashSet<Integer>();
	//System.out.println("@@@@@@@@@@ " + sn.getNumberOfUses()+ " paras for NEW");
	for (int i = 0; i< sn.getNumberOfUses() ; i++){
	    try {
		answer.add(s.getUse(i));
	    }catch (Exception e){
	        return answer;
	    }
	}
	return answer;
	
   }

}
class MethodItem{
    public String type;
    public String cname;
    public String mname;

    public MethodItem(String s){
	String [] sts = s.split(",");
	this.type = sts[0].substring(2);
	this.cname = sts[1].substring(2);
	int index = sts[2].indexOf("(",1);
	//System.out.println("( in "+s +" is "+index);
	this.mname = sts[2].substring(1,index);
	//System.out.println(this.type +":"+" "+this.cname+" "+this.mname+" ");
    }

}
class callstackItem{
    public String cname;
    public String mname;
    public int lnum;
    public callstackItem(String s1, String s2, String s3){
	this.cname = s1;
	this.mname = s2;
	this.lnum  = Integer.parseInt(s3);
    }
    public String toString(){
	return cname + " " + mname + " " + lnum;
    }
}

