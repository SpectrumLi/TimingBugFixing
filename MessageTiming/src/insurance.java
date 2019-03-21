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

public class insurance{

    class location {
        String cn;
        String mn;
        String l;
        public location (String s){
            String [] ss = s.split(" ");
            cn = ss[0]; mn = ss[1]; l = ss[2];
        }
        public location (String s1, String s2, String s3){
            cn = s1; mn = s2; l = s3;
        }
        public String toString(){
            return cn + " " + mn + " " +l;
        }
    }

    String dir;
    public ArrayList<location> wcs;
    public ArrayList<location> scs;
    CallGraph cg;
    public insurance(String s){
	dir = s;
	loadcss(1,s+"/css");
	loadcss(2,s+"/signalcss");
	
    }
    public void setCG(CallGraph cg2){ cg =cg2;}

    public void loadcss(int x, String file){
        ArrayList<location> cs = new ArrayList<location>();
        try{
            InputStream fis = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            String s;
            while ((s= br.readLine())!=null){
                if (s.equals(""))
                    cs = new ArrayList<location>();
                else
                    cs.add(new location(s));
            }

        }catch (Exception e){
            System.out.println("No file exception");
        }
        System.out.println(cs.size() + " css loaded");
        if (x == 1) wcs = cs;
        else scs = cs;
    }

    public boolean zkprove(){
	return false;
    }
    public boolean mainprove(){
	return false;
    }
    public boolean dominateprove(){
	return false;
    }

    public static void main (String args[]) throws IOException, ClassHierarchyException {
	File exFile = new FileProvider().getFile("/home/cstjygpl/DFix/Exclusion.txt");
	if (args.length < 1 ){
	    System.out.println("Please give the input directory");
	    return ;
	}
	String inputdir = args[0];
	config cf = new config(inputdir, exFile);
	insurance ins = new insurance(inputdir);
	AnalysisScope scope = cf.scope;
	IClassHierarchy cha = ClassHierarchy.make(scope);
	System.out.println("ClassHierarchy is ready");
	Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha);
        int size = 0;
        for (Entrypoint ep : entrypoints){size ++;}
        System.out.println("EntryPoints : "+ size);
        AnalysisOptions options = new AnalysisOptions(scope, entrypoints);
	CallGraphBuilder cgb = Util.makeZeroOneCFABuilder(options, new AnalysisCache(),cha,scope);
        DataDependenceOptions doptions = DataDependenceOptions.FULL;
        ControlDependenceOptions coptions = ControlDependenceOptions.NO_EXCEPTIONAL_EDGES;
	try{
	    CallGraph cg = cgb.makeCallGraph(options,null);
	    ins.setCG(cg);
	    if (ins.zkprove()){
		System.out.println("The insurance is proved by zk manual");
	    }else if (ins.mainprove()){
		System.out.println("The insurance is proved by direct path from main function");
	    }else if (ins.dominateprove()){
		System.out.println("The insurance is proved by domination analysis");
	    } else{
		System.out.println("Sorry about that this is unprovable");
	    }
	}catch (Exception e){
	    e.printStackTrace();
	}
    }
}






















