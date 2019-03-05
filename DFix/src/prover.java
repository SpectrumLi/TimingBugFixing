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

public class prover{
    public static void main (String args[]) throws IOException, ClassHierarchyException {
	File exFile = new FileProvider().getFile(System.getenv("EF_Location"));
	System.out.println(exFile.getAbsolutePath());
	
	if (args.length <2 ){
	    System.out.println("Please give the input directory");
            return;
	}
	
	String inputdir = args[0];
	String valueoradd = args[1];
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
	    ProveRecord pr = new ProveRecord(inputdir + "/slicing", cha, cg, valueoradd);
	    pr.prove();
	    pr.dump(inputdir);
	}catch (Exception e){
	    System.out.println("");
            e.printStackTrace();
	}
    }    

}
