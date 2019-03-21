package uchicago.dfix;

import java.io.*;
import java.util.*;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.IBytecodeMethod;
import com.ibm.wala.ssa.IR;

import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;

import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.Entrypoint;

import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;

import com.ibm.wala.util.io.FileProvider;

import uchicago.dfix.config;
import uchicago.dfix.SideEffect;
import uchicago.dfix.PatchConstructor;

public class sedetector{
    public static void main(String args[]) throws IOException, ClassHierarchyException {
        File exFile = new FileProvider().getFile(System.getenv("EF_Location"));
        if (args.length < 1){
	    System.out.println("Please give the input directory");
	    return;
        }
    
        String inputdir = args[0];
        config cf = new config(inputdir,exFile);
	IClassHierarchy cha = ClassHierarchy.make(cf.scope);

        Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(cf.scope, cha);
        
        int size = 0;
        for (Entrypoint ep : entrypoints){size ++;}
        System.out.println("EntryPoints : "+ size);

	AnalysisOptions options = new AnalysisOptions(cf.scope, entrypoints);
	CallGraphBuilder cgb = Util.makeZeroOneCFABuilder(options, new AnalysisCache(),cha,cf.scope);

        DataDependenceOptions doptions = DataDependenceOptions.FULL;
        ControlDependenceOptions coptions = ControlDependenceOptions.NO_EXCEPTIONAL_EDGES;

	try{
	    CallGraph cg = cgb.makeCallGraph(options, null);
	    SideEffect se = new SideEffect(inputdir+"/seffect",cg,cha);
	    System.out.println("Callgraph is ready");
            LinkedList<CGNode> queue = new LinkedList<CGNode>();
            HashSet<String> visited = new HashSet<String>();
	    int epsum = 0;
	    /*
            for (CGNode cn : cg.getEntrypointNodes()){
                queue.add(cn);
                visited.add(Integer.toString(System.identityHashCode(cn)));
		epsum ++;
            }
	    System.out.println(epsum + " EntryPoints exactly loaded");
	    */
	    queue.add(cg.getFakeRootNode());
	    visited.add(Integer.toString(System.identityHashCode(cg.getFakeRootNode())));
	    while (queue.size() != 0){
		CGNode n = queue.poll();
		if (se.check(n,null)) {
		    System.out.println("Gocha!!");
		    //continue;
		}
                for (Iterator<? extends CGNode> inode = cg.getSuccNodes(n); inode.hasNext();){
                    CGNode n2= inode.next();
                    String idn2=Integer.toString(System.identityHashCode(n2));
                    if (!visited.contains(idn2)){
                        queue.add(n2);
                        visited.add(idn2);
                    }
               
                }
	    }
	    se.createMissing();
	    se.toplogyFlush();
            se.dump();
	    PatchConstructor pc = new PatchConstructor(inputdir,se, cha);
	    pc.findRSite();
	    pc.findCSite();
	    pc.genAndProveMiracle();
	    pc.dump();
	} catch (Exception e){
	     System.out.println("Travel CallGraph Error");
	     e.printStackTrace();
	}
    }
    
}
