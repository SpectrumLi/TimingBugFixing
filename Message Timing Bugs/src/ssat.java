package uchicago.dfix;

import java.io.*;
import java.util.*;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ShrikeBTMethod;

import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.impl.PartialCallGraph;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;

import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;

import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.HeapStatement;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.thin.ThinSlicer;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.PDG;

import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.util.strings.Atom;
import com.ibm.wala.util.debug.Assertions;

import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.DefaultIRFactory;
import com.ibm.wala.ssa.SSACache;
import com.ibm.wala.ssa.IRFactory;

import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;

import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.ClassLoaderReference;

import uchicago.dfix.config;
import uchicago.dfix.SSALocator;

public class ssat{
    public static void main(String args[]) throws IOException, ClassHierarchyException {
	String inputdir = args[0];
        //File exFile = new FileProvider().getFile("/home/cstjygpl/DFix/Exclusion.txt");
	System.out.println(System.getenv("EF_Location"));
        File exFile = new FileProvider().getFile(System.getenv("EF_Location"));
	config cf = new config(inputdir,exFile);
	System.out.println(exFile.getAbsolutePath());
	//System.out.println(args[0]);
	//System.out.println(args[0] + " -> "+ args[1]);
	if (args.length< 1){
	    System.out.println("Please give the input directory");
	    return;
	}
	AnalysisScope scope2 = cf.scope;
	//AnalysisScope scope2 = AnalysisScopeReader.makeJavaBinaryAnalysisScope(jarpath, new FileProvider().getFile(CallGraphTestUtil.REGRESSION_EXCLUSIONS));
	//scope.addToScope(scope2);
	IClassHierarchy cha = ClassHierarchy.make(scope2);
 		
	int csum =0;
	
        for (IClass c : cha){
            String cname = c.getName().toString();
	    csum ++;
            if ((!cname.startsWith("Lf")) 
                ||(!cname.startsWith("LJLex"))
                ) continue;
	    //if (cname.contains("MRClientProtocolHandler")){ 
                System.out.println("Class:" + cname);
	    
            	for (IMethod m : c.getAllMethods()){
                    String mname = m.getName().toString();
                    System.out.println("  method:"+mname);
                }
	    //}
	    
        }	
        //System.out.println("Class size"+ csum);
	

	//IClassHierarchy chaWithExclude = ClassHierarchy.make(scopeWithExclude);
	System.out.println("ClassHierarchy is ready!");

	Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope2, cha);
	int size = 0;
	for (Entrypoint ep : entrypoints){size ++;}
	System.out.println("EntryPoints : "+ size);
	AnalysisOptions options = new AnalysisOptions(scope2, entrypoints);
	//AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scopeWithExclude, entrypoints);
	

	//CallGraphBuilder cgb = Util.makeZeroOneCFABuilder(options, new AnalysisCache(),chaWithExclude, scopeWithExclude);
	CallGraphBuilder cgb = Util.makeZeroOneCFABuilder(options, new AnalysisCache(),cha,scope2);
	//System.out.println("CallGraphBuilder is ready!");


	DataDependenceOptions doptions = DataDependenceOptions.FULL;
	ControlDependenceOptions coptions = ControlDependenceOptions.NO_EXCEPTIONAL_EDGES;
	try{
	    long start_t = System.currentTimeMillis();
   	    CallGraph cg = cgb.makeCallGraph(options,null);
	    long end_t = System.currentTimeMillis();
	    long time_t = end_t - start_t;
	    System.out.println("CallGraph is ready! cost time = "+time_t);
	    
	    //SDG sdg = new SDG(cg, cgb.getPointerAnalysis(), doptions, coptions);
	    //System.out.println("SystemDG is ready");
	    if (cg.getEntrypointNodes().toArray().length == 0){
		System.out.println("No EntryPoint found. exit!");
		return ;
	    } 

	    //System.out.println("PDG size is "+pdg.getNumberOfNodes());
	    SSALocator sl = new SSALocator(cg,inputdir);
	    Stateer.pkeyset = new HashSet<PointerKey>();
	    Stateer.pa = cgb.getPointerAnalysis();
	    sl.setIMethod(cha);
 	    sl.process();
	    //PointerAnalysis pa = cgb.getPointerAnalysis(); 
	    //System.out.println(pa);
	    /*
	    for (SSAInstruction s : sl.ssas)
		System.out.println(s);
	    */
	    SDG sdg = new SDG(cg, cgb.getPointerAnalysis(), doptions, coptions);
	    PDG pdg = sdg.getPDG(sl.nodes.get(sl.nodes.size() - 1 ));
	    System.out.println("SystemDG is ready");
	    //System.out.println("PDG size is "+pdg.getNumberOfNodes());
	    /* 
	    for (Statement s : sl.sts){
		System.out.println(pdg.getNumber(s)+" !! "+s.getClass().getName() + " !! "+s);
		
		if (s instanceof HeapStatement){
		    System.out.println(s+" PK:"+ ((HeapStatement)s).getLocation());
		}
	    }
	    System.out.println("Size of slicer node is " + sl.nodes.size());
	    */

	    ArrayList<Statement> answer = new ArrayList<Statement>();
	    HashSet<Integer> refset = new HashSet<Integer>();
	    for (int i = sl.nodes.size()-1; i>=0; i--){
		CGNode cn = sl.nodes.get(i);
		ArrayList<Statement> sts;
		if (i == sl.nodes.size()-1){
		    sts = sl.sts.get(i);
		    System.out.println("------"+cn.getMethod());
		}else{
		    int th = sl.nodes.get(i+1).getMethod().getNumberOfParameters()+1;
		    System.out.println("Para Length of "+ cn.getMethod()+ " is " + th);
		    refset = Stateer.SSARelParameter(refset, th);
		    sts = Stateer.SSAFilter(sl.sts.get(i), refset);
		    System.out.println(" Remove useless parameter from "+ sl.sts.get(i).size() + " to "+ sts.size());
		}
		ArrayList<Statement> pslice = SSALocator.insideSlicing(cn,sts, sdg);
		
		if (i>0){
		    refset = new HashSet<Integer>();
		    for (Statement s : pslice){
			ArrayList<Integer> ref = Stateer.SSARefNumber(s);
			if (ref != null)
			    refset.addAll(Stateer.SSARefNumber(s));
		    }
		}
		answer.addAll(pslice);	
		//System.out.println()
	    }
             
	    System.out.println("------| ANSWER |-------");
	    try {
		PrintWriter writer = new PrintWriter(inputdir +"/slicing", "UTF-8");
		
	        for (int x = 0 ; x < answer.size(); x++){
		    Statement sx = answer.get(x);
		    String mx = srclocation(sx).split(":")[0];
		    int lx =Integer.parseInt( srclocation(sx).split(":")[1]);
		    int z = x;
		    Statement sy;
		    String my;
		    for (int y = x+1; y< answer.size();y++){
			sy = answer.get(y);
			my = srclocation(sy).split(":")[0];
		        int ly =Integer.parseInt( srclocation(sy).split(":")[1]);
			if ((my.split(",")[2].equals(mx.split(",")[2])) &&
			    (my.split(",")[1].equals(mx.split(",")[1])) && 
			    (lx < ly)){
			    z = y;
			    sx = sy;
			    mx = my;
			    lx = ly;
			}
			
		    }
		    
		    Statement sm = answer.get(x);
		    answer.set(x,answer.get(z));
		    answer.set(z,sm);
		}
		    
	        for (int x =0 ; x < answer.size(); x ++){
		    Statement s = answer.get(x);
		    System.out.println(srclocation(s));
	            System.out.println(s);
		    writer.println(srclocation(s));
		    writer.println(s);
		    //writer.println(Stateer.getSSAInstruction(s));
	        }
		writer.close();
	    } catch (Exception e){
		System.out.println("File write error!");
		e.printStackTrace();
	    }
	    /*
	    ArrayList<Statement> init = sl.sts;
	    ArrayList<> touch = new ArrayList<>();  
	    for (int i = sl.nodes.size() -1; i>=0; i--){
		ArrayList<Statement> pslice = SSALocator.insideSlicing(sl.nodes.get(i),init, sdg);
		answer.addAll(pslice);
		if (i>0){
		     updatetouch(pslice,pa);
		     init = getInitForSlice(sl.nodes.get(i-1), pa, touch);
		}
	    }
	    */

            //dumpGraph(cg, pdg);
	   /* 
	    Collection<Statement> slicer = Slicer.computeBackwardSlice(sm, cg, pa, doptions, coptions);
	    for (Statement sli : slicer)
		System.out.println(sli);
	   */
	} catch (Exception e){
	    e.printStackTrace();
	}
    }

    public static String getIdentity(Statement s){
	//return s.toString();
	//return srclocation(s);
	return srclocation(s)+ " " +s.toString();
    }

    public static void dumpGraph(CallGraph cg, PDG pdg){
	//System.out.println("NodeNum "+cg.getNode().size());
	LinkedList<CGNode> queue = new LinkedList<CGNode>();
        LinkedList<Integer> parent = new LinkedList<Integer>();
        HashSet<String> visited = new HashSet<String>();
	for (CGNode cn : cg.getEntrypointNodes()){
            queue.add(cn);
	    visited.add(Integer.toString(System.identityHashCode(cn)));
	}
        //queue.add(cg.getFakeRootNode());
	
        int index = 0;      
	while (queue.size()!=0){
	    index++;
	    CGNode n = queue.poll(); 
	    IMethod imethod = n.getMethod();

	    if (!n.getMethod().toString().contains("Primordial")){
		//String cname = imethod.get
	        System.out.println(parent.poll() + " : "+imethod.toString()+" ************************" );
                IR ir = n.getIR();
                for (Iterator <SSAInstruction> iir = ir.iterateAllInstructions(); iir.hasNext();){
                    SSAInstruction s = iir.next();
		    if ( s instanceof SSAReturnInstruction)
			System.out.print(((SSAReturnInstruction) s).getResult() +" -- ");
		    System.out.println(s);
		    //Statement stt = PDG.ssaInstruction2Statement(n,s,PDG.computeInstructionIndices(ir),ir);
                    //System.out.println(pdg.getNumber(stt));
                }      


	    }
	    for (Iterator<? extends CGNode> inode = cg.getSuccNodes(n); inode.hasNext();){
	        CGNode n2= inode.next();
		String idn2=Integer.toString(System.identityHashCode(n2));
		if (!visited.contains(idn2)){
	        //if (!n2.getMethod().toString().contains("Primordial")){
		    queue.add(n2);
		    parent.add(index);
		    visited.add(idn2);
		}
		//}
	    }
	}
    }
 
	
   public static String srclocation(Statement s){
        try{
	    int bcIndex, instIndex = ((NormalStatement) s).getInstructionIndex();
	    int src_line_number = -1;
	    String methodname = s.getNode().getMethod().toString();
            bcIndex =((ShrikeBTMethod) s.getNode().getMethod()).getBytecodeIndex(instIndex);
            src_line_number = s.getNode().getMethod().getLineNumber(bcIndex);
            return methodname + ":" +Integer.toString(src_line_number);	
       }catch (Exception e){	
            System.out.println(e.getMessage());
	    return "-100";
       }
   }

     public static CGNode findMainMethod(CallGraph cg){
        Descriptor d = Descriptor.findOrCreateUTF8("([Ljava/lang/String;)V");
        Atom name = Atom.findOrCreateUnicodeAtom("main");
        for (Iterator<? extends CGNode> it = cg.getSuccNodes(cg.getFakeRootNode()); it.hasNext();) {
        CGNode n = it.next();
        if (n.getMethod().getName().equals(name) && n.getMethod().getDescriptor().equals(d)) {
          return n;
        }
      }
      Assertions.UNREACHABLE("failed to find main() method");
      return null;

    }

    public static Statement findCallTo(CGNode n, String methodName){
        IR ir = n.getIR();

        for (Iterator <SSAInstruction> it = ir.iterateAllInstructions(); it.hasNext();){
            SSAInstruction s = it.next();
            if (s instanceof com.ibm.wala.ssa.SSAAbstractInvokeInstruction){
                com.ibm.wala.ssa.SSAAbstractInvokeInstruction call = (com.ibm.wala.ssa.SSAAbstractInvokeInstruction)s;
                if (call.getCallSite().getDeclaredTarget().getName().toString().equals(methodName)) {
                    com.ibm.wala.util.intset.IntSet indices = ir.getCallInstructionIndices(call.getCallSite());
                    com.ibm.wala.util.debug.Assertions.productionAssertion(indices.size() == 1, "expected 1 but got " + indices.size());
                    System.out.println("Target is found in "+s+" "+indices.intIterator().next());
                    return new com.ibm.wala.ipa.slicer.NormalStatement(n, indices.intIterator().next());
                }
            }
        }
        Assertions.UNREACHABLE("failed to find call to " + methodName + " in " + n);
        return null;

    }

}
