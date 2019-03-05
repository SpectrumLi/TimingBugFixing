package uchicago.ffix;
import java.io.*;
import java.util.*;

import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.impl.ExplicitCallGraph;
import com.ibm.wala.ipa.callgraph.cha.ContextInsensitiveCHAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.rta.DefaultRTAInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.cfa.DefaultSSAInterpreter;

import com.ibm.wala.types.MethodReference;

import com.ibm.wala.ipa.callgraph.impl.BasicCallGraph;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;

import com.ibm.wala.classLoader.IBytecodeMethod;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;

import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
//import com.ibm.wala.cast.ir.ssa.AstGlobalRead;

import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.util.graph.impl.InvertedGraph;
import com.ibm.wala.util.graph.impl.GraphInverter;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;

import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.callgraph.impl.Util;
import uchicago.ffix.srcloc;
import uchicago.ffix.Gins;

public class config{
    private ArrayList<String> jarpath;
    public AnalysisScope scope;
    public IClassHierarchy cha;
    public Iterable<Entrypoint> entrypoints;
    public AnalysisOptions options;
    public CallGraphBuilder cgb;
    public CallGraph cg;
    public HashMap<String,CGNode> creatednodes;
    public HashMap<CGNode, ArrayList<Gins>> nodese;
    public HashMap<CGNode, ArrayList<String>> nodeglob;
    public HashSet<CGNode> pending; // for recursive call 
    public HashSet<String> fixfield;

    public config(String jarpathfile, File exclusion){
	jarpath = new ArrayList<String>();
	try{
	    InputStream fis = new FileInputStream(jarpathfile);
	    InputStreamReader isr = new InputStreamReader(fis);
	    BufferedReader br = new BufferedReader(isr);
	    String s ;
	    while ((s = br.readLine()) != null)
		jarpath.add(s);
	    //scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(jarpath.get(0),null);
            scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(jarpath.get(0), exclusion);
            for (int i = 1; i< jarpath.size(); i++){
		scope.addToScope(AnalysisScopeReader.makeJavaBinaryAnalysisScope(jarpath.get(i), exclusion));
	        System.out.println("Jarfile loaded !! "+i);
	    }
	}catch(Exception e){
            e.printStackTrace();
	    System.out.println("Jarfile load error");
	}
	creatednodes = new HashMap<String, CGNode>();
	nodese = new HashMap<CGNode, ArrayList<Gins>>();
	nodeglob = new HashMap<CGNode,ArrayList<String>>();
	pending = new HashSet<CGNode>();
	fixfield = new HashSet<String>();
//	cg = new ExplicitCallGraph();
    }
   
   public void makecha(){
	try{
	    cha = ClassHierarchy.make(scope);
	}catch(Exception e){
	    System.out.println("Making ClassHirerarchy error");
	    return ;
	}
	entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha);
	options = new AnalysisOptions(scope, entrypoints);
	System.out.println("ClassHirerarchy is ready!");
   }
   public void makeemptycfg(){
      AnalysisCache cache = new AnalysisCache();
      ExplicitCallGraph ecg = new ExplicitCallGraph(cha, options, cache);
      ecg.setInterpreter(new DefaultSSAInterpreter(options, cache));
      cg = ecg;
      System.out.println("EmptyCallGraph is ready!");
   }
   public void makecfg(){
	cgb = Util.makeZeroOneCFABuilder(options, new AnalysisCache(),cha,scope);
	try{
	    cg  = cgb.makeCallGraph(options,null);
	//    System.out.println("interpreter is "+ cg.get);
	}catch(Exception e){
        System.out.println("Making CallGraph error");
        return ;
	}
	System.out.println("CallGraph is ready!");
   }
   public ArrayList<ISSABasicBlock> getcontrolIB(CGNode cn, SSAInstruction s){
	return getIBchain(cn.getIR(),null,cn.getIR().getBasicBlockForInstruction(s));
   }
   public ArrayList<ISSABasicBlock> getcontrolIB(CGNode cn, SSAInstruction s1, SSAInstruction s2){
       return getIBchain(cn.getIR(),cn.getIR().getBasicBlockForInstruction(s1),
                       cn.getIR().getBasicBlockForInstruction(s2));
   }

   public ArrayList<ISSABasicBlock> getpostcontrolIB(CGNode cn, SSAInstruction s){
	// not implemented
	return null;
   }
   public ArrayList<ISSABasicBlock> getIBchain(IR ir, ISSABasicBlock startib, ISSABasicBlock endib){
	SSACFG ssacfg = ir.getControlFlowGraph();
	if (startib == null) startib = ssacfg.entry();
        //SSACFG icfg = GraphInverter.invert(ssacfg);

	ArrayList<ISSABasicBlock> resultB = BFSFindBlock(ssacfg, endib, true); // true means inversed graph
        ArrayList<ISSABasicBlock> resultA = BFSFindBlock(ssacfg, startib, false); // false means original graph
	System.out.println("SSACFG  has " + ssacfg.getNumberOfNodes() +" blocks");
	System.out.println("start ib " + startib + " has "+resultA.size()+ "reachable blocks");
	System.out.println("end ib " + endib + " has "+resultB.size()+ "previous blocks");
	ArrayList<ISSABasicBlock> result = new ArrayList<ISSABasicBlock>();
	for (ISSABasicBlock bb : resultA)
	    if(resultB.contains(bb)) result.add(bb);
	return result;
   }

   public ArrayList<ISSABasicBlock> BFSFindBlock(SSACFG cfg, ISSABasicBlock bb, boolean b){
	LinkedList<ISSABasicBlock> queue = new LinkedList<ISSABasicBlock>();
        HashSet<String> visited = new HashSet<String>();
        queue.add(bb);
	visited.add(Integer.toString(System.identityHashCode(bb)));
	ArrayList<ISSABasicBlock> re = new ArrayList<ISSABasicBlock>();
        while (queue.size()!=0){
            ISSABasicBlock n = queue.poll();
	    re.add(n);
	    Iterator<? extends ISSABasicBlock> inode = cfg.getSuccNodes(n);
	    if (b) inode = cfg.getPredNodes(n);
            for ( ; inode.hasNext();){
                ISSABasicBlock n2= inode.next();
                String idn2=Integer.toString(System.identityHashCode(n2));
                if (!visited.contains(idn2)){
                    queue.add(n2);
                    visited.add(idn2);
                }
            }
        }
	return re;
   }


   public CGNode getsrclocCGNode(srcloc sl){
	return BFSFindCGNode(sl.cname, sl.mname);
   }

   public CGNode BFSFindCGNode(String cn, String mn){
  /*
	LinkedList<CGNode> queue = new LinkedList<CGNode>();
        HashSet<String> visited = new HashSet<String>();
        HashSet<CGNode> start = new HashSet<CGNode>();
	start.add(cg.getFakeRootNode());
        for (CGNode cgnode : start){
            queue.add(cgnode);
            visited.add(Integer.toString(System.identityHashCode(cn)));
        }
        while (queue.size()!=0){
            CGNode n = queue.poll();
            MethodItem mi1 = new MethodItem(n.getMethod().toString());
            if (mi1.cname.equals(cn)&&(mi1.mname.equals(mn)))
                return n;
            for (Iterator<? extends CGNode> inode = cg.getSuccNodes(n); inode.hasNext();){
                CGNode n2= inode.next();
                String idn2=Integer.toString(System.identityHashCode(n2));
                if (!visited.contains(idn2)){
                    queue.add(n2);
                    visited.add(idn2);
                }
                MethodItem mi = new MethodItem(n2.getMethod().toString());
                if (mi.cname.equals(cn)&&(mi.mname.equals(mn)))
                    return n2;
                //if () NEED CODE IMPLEMENTAION FOR EXIT
            }
        }

        System.out.println("ERROR in find " + cn + " "+ mn + " in callgraph from the entrypoint");
   */
	return createNewNode(cn,mn);
    }
    public void dumpcha(){
	try{
	    BufferedWriter w = new BufferedWriter(new FileWriter("/tmp/dumpcha"));
	    for (IClass c : cha){
                for (IMethod m : c.getAllMethods()){
                     String sig = m.getSignature().toString();
                     w.write(sig+"\n");
		}
	    }
	    w.close();
	    System.out.println(cha.getScope());
 	}catch(Exception e){
	    e.printStackTrace();
	}

    }

    public IMethod findIM(String cn, String mn){
	for (IClass c : cha){
             String cname = c.getName().toString();
             if (cname.equals("L"+cn)){
                 //System.out.println("Get class:"+cname);
                 for (IMethod m : c.getAllMethods()){
                     String mname = m.getName().toString();
		     String sig = m.getSignature().toString();
		     if (sig.contains("<init>"))
			sig = sig.replace("<init>","init");
		     //System.out.println(mname + " "+sig+ " ->"+mn);
                     if (mname.equals(mn) || sig.endsWith(mn)){
                         //System.out.println("Get method:"+mname);
                         return m;
                     }
                 }
             }
	}
	return null;
    }

    public CGNode createNewNode(String cn, String mn){
	String id = cn + " " + mn;
	if (creatednodes.get(id) != null){
      //System.out.println("exist node for "+ id);
	    return creatednodes.get(id);
  }
	IMethod im = findIM(cn,mn);
	if (im == null){
        System.out.println("This method is not found in the class "+ cn + " " + mn);
	if (( (cn.contains("HRegionInfo")) && (mn.contains("getTableDesc")) )
	     ||(cn.contains("KeyValue"))
	     ||(cn.contains("HBaseConfiguration"))){
	    System.out.println("Ignore this method now, solve it later");
	    return null;
	}
        if (mn.equals("init")){
            System.out.println("It is fine to miss this Init func "+ cn + " " + mn);
            return null;
        }
      return null;
  }
        //System.out.println("Found IM "+im.getSignature());
        MethodReference mr = im.getReference();
        Set<CGNode> callnodes = cg.getNodes(mr);
        if (callnodes.size() == 0){
    //        System.out.println("No callnodes??? for " + cn + " " + mn);
            //return cg.findOrCreateNode(mr,Everywhere.EVERYWHERE);
            try{
                CGNode cgnode = ((BasicCallGraph<CGNode>)cg).findOrCreateNode(im,Everywhere.EVERYWHERE);
		creatednodes.put(id, cgnode);
    //            System.out.println("Create new node in the CG " + cgnode );
                return cgnode;
            }catch (Exception e){
                //System.out.println("Create new node failed!");
                return null;
            }
        }
        else if (callnodes.size() > 1){
            System.out.println("Multiple callnodes??? for " + cn + " " + mn);
            return null;
        } else
            for (CGNode cnode : callnodes)
                return cnode;
        return null;
    }

    public ArrayList<SSAInstruction>  getsrclocSSAInstruction(srcloc sl){
	CGNode cgnode = getsrclocCGNode(sl);
	ArrayList<SSAInstruction> list = new ArrayList<SSAInstruction>();
	IR ir = cgnode.getIR();
	SSAInstruction [] ssaset = ir.getInstructions();
        int index = 0;
        for (index = 0; index < ssaset.length ;index++ ){
            SSAInstruction s = ssaset[index];
            if (s == null) continue;
            //System.out.println(s);
            try {
                int bcIndex =((IBytecodeMethod)cgnode.getMethod()).getBytecodeIndex(index);
                int sln= cgnode.getMethod().getLineNumber(bcIndex);
                System.out.println(sln + " "+s);
                if (sln == sl.getlinenum()){
                    list.add(s); 
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        System.out.println(list.size()+" SSAs found for "+ sl);
        return list;
    }
    public CGNode invokeSSAtoCGNode(SSAInstruction ssa){
        if (!(ssa instanceof SSAInvokeInstruction)) return null;
        String s2 = ssa.toString();
        if (s2.contains("<init>")){
                s2 = s2.replace("<init>","init");
        //      System.out.println("Init Ins trans "+ s2);
        }
        String s = s2.split("<")[1].split(">")[0];
	String [] ss = s.split(", ");
        //System.out.println(ss[1] + " $ "+ ss[2] );
	if (! ss[0].contains("Application")) return null;
	return BFSFindCGNode(ss[1].substring(1), ss[2].substring(0,ss[2].length() -1));
        //return BFSFindCGNode(ss[1].substring(1), ss[2].split("\\(")[0]);
    }

    public ArrayList<Gins> getsideeffectinstructions(SSAInstruction s, Gins gins, String t, boolean flag){
	ArrayList<Gins> se = new ArrayList<Gins>();
	Gins g2 = new Gins();
	g2.addSSA(gins.list);
	g2.addSSA(s);
  g2.addCGNode(gins.cnlist);
	g2.setST(t);
	if (s == null) return se;
	boolean f = hassideeffect(s);
	if (!flag) f = hasglobaleffect(s);
	if (f) {
                //String temp = prefix + "\n"+ s.toString()+"\n"+t; 
		System.out.println("LALALALAL : "+ g2.toString());
                if ((se.size() == 0)||(!se.get(se.size()-1).equals(g2)))
                    se.add(g2);
                return se;
            } 
        if (terminalSSA(s, flag)) return se; 
        if (s instanceof SSAInvokeInstruction){
            CGNode cn2 = invokeSSAtoCGNode(s);
            //g2.addCGNode(cn2);
            if ((nodese.get(cn2) == null)){
                if (!pending.contains(cn2)) 
                    se.addAll(getsideeffectinstructions(cn2,g2,flag)); // ha$
            }
            else
                se.addAll(nodese.get(cn2));
        }
	return se;
    }

    public ArrayList<Gins> getsideeffectinstructions(CGNode cn, Gins gins, boolean flag){
	ArrayList<Gins> se = new ArrayList<Gins>();
	
  	if (cn == null) return se;// handle unfound init CGNode
	IR ir = cn.getIR();
  	if (ir == null) return se;// handle empty implementation
  gins.addCGNode(cn);
	pending.add(cn);
	SSAInstruction [] ssaset = ir.getInstructions();
        int index = 0;
        for (index = 0; index < ssaset.length ;index++ ){
            SSAInstruction s = ssaset[index];// repeated same SSA bug???
            if (s == null) continue;
	    String t = SSAtosrcloc(cn,index).toString();
	    se.addAll(getsideeffectinstructions(s, gins, t,flag));
	    //System.out.println("New SE size  = "+se.size());
	    //System.out.println(" processing "+ s);
	    /*
	    if (hassideeffect(s)) {
		String temp = prefix + "\n"+ s.toString()+"\n"+SSAtosrcloc(cn,index); 
		if ((se.size() == 0)||(!se.get(se.size()-1).equals(temp)))
		    se.add(temp);
		continue;
	    }
	    
	    if (terminalSSA(s)) continue; 
	    if (s instanceof SSAInvokeInstruction){
		CGNode cn2 = invokeSSAtoCGNode(s);
		if ((nodese.get(cn2) == null)){
		    if (!pending.contains(cn2)) 
		        se.addAll(getsideeffectinstructions(cn2,prefix+"\n"+ s.toString())); // handler recursive!!!!! pretty tricky here
		}
		else
		    se.addAll(nodese.get(cn2));
	    }
	    */
	}
	nodese.put(cn,se);
	System.out.println(cn + " has seeffect "+ nodese.get(cn).size());
	pending.remove(cn);
	return se;
    }
   public boolean terminalSSA(SSAInstruction s,boolean flag){
	if (s instanceof SSAInvokeInstruction){
	    String s2 = s.toString();
	    if(s2.contains("<init>")){ 
		s2 = s2.replace("<init>","init");
		if (flag) return true;
	    }
	    String[] ss = s2.toString().split(">")[0].split("<")[1].split(", ");
	    if (!ss[0].contains("Application")) return true;
        //if (ss[1].startsWith("Ljava")) return true;
	//if (ss[1].startsWith("Lorg/apache/commons/logging/Log")) return true;
        //if (ss[1].startsWith("Lcom/google")) return true;
	//if (ss[1].startsWith("Lorg/w3c")) return true;
	if ( ss[1].startsWith("Lorg/apache/hadoop/hbase/regionserver/HRegionFileSystem") &&
	     ss[2].startsWith("sleepBeforeRetry") )
		return true; 
	if ((ss[1].startsWith("Lorg/apache/zookeeper/common/AtomicFileOutputStream")) &&
        (ss[2].startsWith("flush") || ss[2].startsWith("abort")))
	        return true;

        if (ss[2].startsWith("length") 
             || ss[2].startsWith("getMetaBlock")
	     || ss[2].startsWith("copyToLocalFile")
             || ss[2].startsWith("loadFileInfo"))
                return true;


	if (ss[1].startsWith(""))
	if (ss[1].startsWith("Lorg/apache/hadoop/hbase/protobuf")) return true;
	if (ss[1].startsWith("Lorg/apache/cassandra/utils/FBUtilities") &&
	    ss[2].startsWith("get")) 
	    return true;
	 if (ss[1].startsWith("Lorg/apache/cassandra/service/AntiEntropyService$RepairSession$RepairJob") &&
            ss[2].startsWith("makeSnapshots")) 
            return true;

   
	//if (ss[1].startsWith("Lorg/cloudera")) return true;
        //if (ss[1].startsWith("Lorg/apache/hadoop") && (!ss[1].startsWith("Lorg/apache/hadoop/hbase"))) return true;
	if ((!ss[1].startsWith("Lorg/apache/hadoop/hbase")) &&
	    (!ss[1].startsWith("Lorg/apache/cassandra")) &&
      (!ss[1].startsWith("Lorg/apache/zookeeper")))
		return true;
	if (ss[2].startsWith("debugLog(")) return true;
//      if (ss[1].startsWith("Ljava/lang")) return true;
	    if (ss[1].startsWith("Lorg/apache/hadoop/hbase/util"))
	        if (ss[2].startsWith("toString(")||
		    ss[2].startsWith("toStringBinary(")||
		    ss[2].startsWith("hash("))
		    return true;
	}
	return false;
   }
   public boolean hassideeffect(SSAInstruction s){
   
	if (s instanceof SSAPutInstruction) return true;
	if (s instanceof SSAInvokeInstruction){
	    String st = s.toString();
	    if (st.contains("<init>"))
	        st = st.replace("<init>","init");
	try{
	    String[] ss = st.toString().split(">")[0].split("<")[1].split(", ");
	    if (ss[1].startsWith("Lorg/apache/hadoop/hbase/zookeeper/ZKAssign"))
	        //if (ss[2].startsWith("deleteNode")) 
		return true;
      if (ss[1].startsWith("Lorg/apache/hadoop/hbase/zookeeper/ZKUtil"))
          return true;
	    if (ss[1].startsWith("Ljava/util") &&
	        (!ss[2].startsWith("get")) &&
		(!ss[2].startsWith("contains")) &&
		(!ss[2].startsWith("toString")) &&
		(!ss[2].startsWith("hasNext")) &&
		(!ss[2].startsWith("iterator")) &&
		(!ss[2].startsWith("next")) &&
		(!ss[2].startsWith("peek")) &&
		(!ss[2].startsWith("size")) &&
		(!ss[2].startsWith("signalAll")) &&
		(!ss[2].startsWith("isEmpty")))
		return true;
    	}catch(Exception e){
        System.out.println("      " + st);
        throw e;
        }
	}
	return false;
   }
   public boolean hasglobaleffect(SSAInstruction s){
	try{
        if (s instanceof SSAInvokeInstruction){
            String st = s.toString();
            if (st.contains("<init>"))
                st = st.replace("<init>","init");

            String[] ss = st.split(">")[0].split("<")[1].split(", ");
            if (ss[1].startsWith("Lorg/apache/hadoop/hbase/zookeeper/ZKAssign") &&
                 (!ss[2].startsWith("getNodeName")))
                //if (ss[2].startsWith("deleteNode")) 
		             return true;
            if (ss[1].startsWith("Lorg/apache/hadoop/hbase/zookeeper/ZKUtil") && 
                 ( (!ss[2].startsWith("joinZNode"))  &&
                   (!ss[2].startsWith("getData"))  &&
                   (!ss[2].startsWith("checkExists"))  &&
		               (!ss[2].startsWith("logRetrievedMsg"))  &&
                   (!ss[2].startsWith("listChildrenNoWatch"))))
                 return true;
	    /*
            if (ss[1].startsWith("Ljava/io/BufferedWriter") && 
                (!ss[2].startsWith("getSnapDir")) &&
                (!ss[2].startsWith("flush")) &&
                (!ss[2].startsWith("close")) &&
                (!ss[2].startsWith("init")) )
                //if (ss[2].startsWith("deleteNode")) 
                             return true;
	    */
      //if (ss[1].startsWith("Lorg/apache/zookeeper/server/persistence/FileTxnSnapLog")
      //    && ss[2].startsWith("save")) return false;
      if (ss[1].startsWith("Lorg/apache/zookeeper/server/persistence/FileSnap") 
          && ss[2].startsWith("serialize"))
          return true;
	    if (ss[1].startsWith("Lorg/apache/hadoop/hbase/regionserver/HRegionFileSystem")
		&& ((ss[2].startsWith("get")) ||
		    (ss[2].startsWith("writeRegionInfoOnFilesystem")) ||
		    (ss[2].startsWith("cleanupTempDir")) ||
		    (ss[2].startsWith("deleteDir")) ||
		    (ss[2].startsWith("cleanupSplitsDir")) ||
		    (ss[2].startsWith("cleanupAnySplitDetritus")) ||
		    (ss[2].startsWith("cleanupMergesDir")) ||
		    (ss[2].startsWith("sleepBeforeRetry")) ||
        (ss[2].startsWith("writeRegionInfoFileContent")) ||
		    (ss[2].startsWith("checkRegionInfoOnFilesystem"))))
		return false;
      if (ss[2].startsWith("writeLongToFile")) return true;
	    if (ss[2].startsWith("length") 
		|| ss[2].startsWith("getMetaBlock")
		|| ss[2].startsWith("loadFileInfo"))
		return false;
	    if (ss[1].startsWith("Lorg/apache/hadoop/hbase/regionserver/StoreFile")
		&& (ss[2].startsWith("get")
		  ||(ss[2].startsWith("isReference"))
		  ||(ss[2].startsWith("open"))
                  ||(ss[2].startsWith("valueOf"))
                  ||(ss[2].startsWith("createReader"))
                  ||(ss[2].startsWith("loadFileInfo"))
                  ||(ss[2].startsWith("setSequenceID"))
                  ||(ss[2].startsWith("loadBloomfilter"))
		  ||(ss[2].startsWith("length"))
                  ||(ss[2].startsWith("isMajorCompaction"))
                  ||(ss[2].startsWith("toStringDetailed"))
		  ||ss[2].startsWith("isBulkLoadResult")))
		return false;
            if (ss[1].startsWith("Lorg/apache/hadoop/fs/FileSystem")
                && (ss[2].startsWith("get(") 
		   || ss[2].startsWith("exists(")
		   || ss[2].startsWith("open") //this is an input
		   || ss[2].startsWith("listStatus")
		   || ss[2].startsWith("copyToLocalFile")
		   || ss[2].startsWith("getFileStatus")))
                return false;
            if (ss[1].startsWith("Lorg/apache/hadoop/fs/FileStatus")
                && (ss[2].startsWith("getPath(") 
                   || ss[2].startsWith("exists(")
		   || ss[2].startsWith("isDir") 
                   || ss[2].startsWith("listStatus")
		   || ss[2].startsWith("getLen")))
                return false;
	    if (ss[1].startsWith("Lorg/apache/hadoop/hbase/regionserver/wal/SequenceFileLogReader")
                && (ss[2].startsWith("<init>") 
                   || ss[2].startsWith("getLen")))
		return false;
	    if ((ss[1].startsWith("Lorg/apache/zookeeper/common/AtomicFileOutputStream")) &&
		(ss[2].startsWith("close")))
		return true;
            if ((ss[1].startsWith("Lorg/apache/zookeeper/common/AtomicFileOutputStream")) &&
                (ss[2].startsWith("flush") || ss[2].startsWith("abort")))
                return false;

	    if ((ss[1].startsWith("Lorg/apache/hadoop/hbase/HColumnDescriptor")) &&(
		ss[2].startsWith("valueOf")
		))
                return false;
      if (ss[1].startsWith("Lorg/apache/hadoop/hbase/util/FSUtils") &&
           (!ss[2].startsWith("getTableDir")) &&
           (!ss[2].startsWith("isExists")) &&
           (!ss[2].startsWith("getFilePermissions")) &&
           (!ss[2].startsWith("listStatus")) &&
           (!ss[2].startsWith("getNamespaceDir")) &&
           (!ss[2].startsWith("init")) &&
           (!ss[2].startsWith("getRootDir")))
                return true;
	    if ((ss[1].contains("File")) &&
	        (!(ss[2].startsWith("init"))) &&
		(!(ss[2].startsWith("getSnapDir"))) &&
	    	(!(ss[2].startsWith("getName"))) &&
    		(!(ss[2].startsWith("getAbsoluteFile"))) &&
		(!(ss[2].startsWith("getParentFile"))) &&
    		(!(ss[2].startsWith("getChannel"))) &&
                (!(ss[2].startsWith("toURL"))) &&
                //(!(ss[2].startsWith("save"))) &&
                (!(ss[2].startsWith("getCanonicalFile"))) &&
		(!(ss[2].startsWith("abort"))))
		return true;
        }
	}catch(Exception e){
	    System.out.println("GLOB check error for "+s);
	    throw e;
	}
        return false;
   }
   public void cleancachedse(){
	nodese.clear();
   }
   public srcloc SSAtosrcloc(CGNode cn, int i){
	try {
            int bcIndex =((IBytecodeMethod)cn.getMethod()).getBytecodeIndex(i);
            int sln= cn.getMethod().getLineNumber(bcIndex);
            return new srcloc("", cn.getMethod().toString(), Integer.toString(sln));
            }catch (Exception e){
                e.printStackTrace();
            }
	return null;
   }

   public srcloc SSAtosrcloc(CGNode cn, SSAInstruction s){
	int x = getssaindex(cn,s);
    if (x < 0 ) return new srcloc("","","");
	srcloc sl = SSAtosrcloc(cn, x);
//	System.out.println("ssainstruction :"+s + " is in "+ sl);
        return sl;
   }

   public int getssaindex(CGNode cn, SSAInstruction s){
       if (s == null) return -1;
	SSAInstruction[] ssas = cn.getIR().getInstructions();
	for (int i = 0 ; i < ssas.length; i++){
	    if (ssas[i] == null) continue;
	    if (ssas[i].toString().equals(s.toString())) return i;
	}
	return -1;
   }

   public void initfixvar(){
	/*
	for (String s : creatednodes.keySet()){
	    if (s.contains("init"))
		System.out.println("fixinit "+ s);
	}
        for (String s : creatednodes.keySet()){
            if (s.contains("init")) 
                fixvarset(creatednodes.get(s), null, "");
        }
	*/
	ArrayList<String> temp = new ArrayList<String>();
	for (IClass c : cha){
	    if (c.getName().toString().startsWith("Lorg/apache/hadoop/hbase"))
                for (IMethod m : c.getAllMethods()){
                     if (m.getName().toString().contains("<init>")){
			 String t = c.getName().toString() + " "+ m.getName().toString();
		         CGNode cn = creatednodes.get(t);
			 if (cn == null)
			     try{
			         cn = ((BasicCallGraph<CGNode>)cg).findOrCreateNode(m,Everywhere.EVERYWHERE);
			     }catch (Exception e){
				 e.printStackTrace();
			     }
			 fixvarset(cn, null, "");
                     	 temp.add(t);
		     }
                }
            }
//	for (String s : temp)
//	    System.out.println("fixinit "+ s);
   }
   /*
   public HashSet<Integer> constantindex(IR ir){
	HashSet<Integer> remain = HashSet<Integer>();
	HashSet<Integer> touch  = HashSet<Integer>();
	for (Iterator <SSAInstruction> iir = ir.iterateAllInstructions(); iir.hasNext();){
            SSAInstruction s = iir.next();
	}
   }
	*/
   public ArrayList<String> fixvarset(CGNode cn, ArrayList<Integer> p, String prefix){
	pending.add(cn);
	IR ir = cn.getIR();
	HashSet<Integer> list = new HashSet<Integer>();
	//ArrayList<Variable> list = new ArrayList<Integer>();
	ArrayList<String> ans = new ArrayList<String>();
	if (p != null)	list.addAll(p);
        if (ir ==null) return new ArrayList<String>();
	//list.add(1);
	//list.add(constantindex(ir));
	SSAInstruction prev = null;
	SymbolTable st = ir.getSymbolTable();
	for (Iterator <SSAInstruction> iir = ir.iterateAllInstructions(); iir.hasNext();){
            SSAInstruction s = iir.next();
            //System.out.println(s);
            //System.out.println("\nUse = ");
	    //if (terminalSSA(s,true)) continue;
	    if (s instanceof SSAReturnInstruction) continue;
	    int count = 0;
            for (int i = 0; i < s.getNumberOfUses(); i++){ //getUse for the parameter
//                System.out.print(s.getUse(i) + " ");
		if (list.contains(s.getUse(i))) {
		    count ++;
		    continue;
		}
		if (st.isConstant(s.getUse(i))) {
		    count ++;
		    list.add(s.getUse(i));
		}
            }

	    HashSet<Integer> set = new HashSet<Integer>();
	    for (int i = 0; i < s.getNumberOfDefs(); i++){ //getDef(0) = assigned number
                set.add(s.getDef(i));
            }

	    boolean flag = count == s.getNumberOfUses();

	    // for getfield instruction
	    if (s instanceof SSAGetInstruction){
		String fname = s.toString().split(", ")[2];
		String cname = s.toString().split(", ")[1];
		String t = cname + " "+ fname;
		if (fixfield.contains(t))
		    flag = true;
		if (fname.equals("fs")) flag = true; // a very special case for reading the unchanged filesystem
		//System.out.println("CHECK FIELD " +flag+" "+t );
	    }
	
	    // for constant value 
	    if (s.toString().contains("getstatic")){
		//System.out.println("Global "+s);
		flag = true;
	    }
	    // for configure tion, it should be a spcial case of func call
	    if (s instanceof SSAInvokeInstruction){
		ArrayList<Integer> l = new ArrayList<Integer>();
		for ( int j = 0; j < s.getNumberOfUses(); j++){
		    if (list.contains(s.getUse(j)))
			l.add(j+1);
		}
		if (s.toString().contains("Configuration") && s.toString().contains("get")) flag =true;
		else if ((!terminalSSA(s,true))&&(!hassideeffect(s)) &&(!hasglobaleffect(s))){
		    CGNode cn2 = invokeSSAtoCGNode(s);
		    if ((!pending.contains(cn2)) && (cn2!=null)){
		        ArrayList<String> tl = fixvarset(cn2, l, prefix + l.toString()+ "\n "+ s.toString());
		        if (tl != null) ans.addAll(tl);
		    }
                }

	    }

	    if (flag){
		if (!(s instanceof SSAPutInstruction)){
	            if (hasglobaleffect(s)){
		        System.out.println("\nfound a unchange SSA "+prefix  +"\n" +s);
		        System.out.println(set.toString() + " is load to the list" );
  		    }
		    list.addAll(set);
		}else{
	            String fname = s.toString().split(", ")[2];
        	    String cname = s.toString().split(", ")[1];
                    String t = cname + " "+ fname;
		    if (!fixfield.contains(t) ){
		        fixfield.add(t);
		        System.out.println(t + " -> fixfield5d");
		    }
		}
		//for putfield instruction
		//String s = s.toString() + 
		ans.add(s.toString());
	    }else{
		if (hasglobaleffect(s)){
		    System.out.println("Not fixed \n" +prefix+"\n"+ s + " Because");
		    for (int i = 0; i < s.getNumberOfUses(); i++){
                	if ((!list.contains(s.getUse(i))) && (!st.isConstant(s.getUse(i))))
			    System.out.print(" "+s.getUse(i));
            	    }
		    System.out.println("\ninitp = "+p);
		    System.out.println("\nprevious "+prev);
		}
	    }
	    prev = s;
        }
	pending.remove(cn);
	return null;
   }

   public void dumpnode(CGNode cn){
       IR ir = cn.getIR();
       System.out.println("IR  = "+ir);
       for (Iterator <SSAInstruction> iir = ir.iterateAllInstructions(); iir.hasNext();){
            SSAInstruction s = iir.next();
            System.out.println(s);
           // String t = conf.SSAtosrcloc(cn,s).toString();
           // System.out.println(t);
       }
   }

   public HashSet<Integer> prefixed(int x, ArrayList<Gins> glob){
        HashSet<Integer> ans = new HashSet<Integer>();
	ans.add(x);
	Gins ginx = glob.get(x);
	SSAInstruction ssax = ginx.list.get(ginx.list.size()-1);
	CGNode cn = null;
	if (ginx.list.size()>1) 
	    cn = invokeSSAtoCGNode(ginx.list.get(ginx.list.size()-2));
	else 
	    return null;
	for (int y = x + 1; y< glob.size(); y++){
	    Gins giny = glob.get(y);
	    SSAInstruction ssay = giny.list.get(giny.list.size()-1);
	    if (!sharepregin(ginx,giny)) continue;
	    //tricky implementation here! ignore all the ginses which only have one ssa-stack
	    if (prefixedZK(cn , ssax, ssay)) ans.add(y); 
        }
	return ans;
   }

   public boolean sharepregin(Gins x, Gins y){
        if (x.list.size() != y.list.size()) return false;
	for (int z = 0 ; z < x.list.size()-1; z ++)
	    if (!x.list.get(z).equals(y.list.get(z))) return false;
	System.out.println("matching "+x+"\n"+y);
	return true;
   }

   public boolean prefixedZK(CGNode cn, SSAInstruction x, SSAInstruction y){
	IR ir = cn.getIR();
	SSAInstruction[] ssas = ir.getInstructions();
	int ix = 0;
	int iy = 0;
	int s,t;
	for ( ix = 0; ix < ssas.length; ix ++)
	    if (ssas[ix] == x ) break;
 	for ( iy = 0; iy < ssas.length; iy ++)
	    if (ssas[iy] == y ) break;
	// currently only care about zk-operation
	//if (x.toString().contains("createNodeIfNotExistsAndWatch"))  s = x.getUses(1);
	//    else s = x.getUse(2);
        //if (x.toString().contains("createNodeIfNotExistsAndWatch"))  s = x.getUses(1);
        //    else s = x.getUse(2);
	s = x.getUse(1);
	t = y.getUse(1);
	HashSet<Integer> set = new HashSet<Integer>();
	set.add(s);
	System.out.println("src = "+ s);
	for (int iz = ix + 1; iz <= iy; iz ++){
	    SSAInstruction sa = ssas[iz];
	    if (sa == null) continue;
	    if (sa.toString().contains("ZKUtil") && sa.toString().contains("joinZNode")){
		System.out.println("find a concate "+ sa);
		if (set.contains(sa.getUse(0)))
		    for (int temp = 0 ; temp < sa.getNumberOfDefs() ; temp++){
		         set.add(sa.getDef(temp));
			 System.out.println("add new prefix "+ sa.getDef(temp));
		    }
	    }
	}
	if (set.contains(t)) return true;
	return false;

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
           this.mname = sts[2].substring(1,index);
       }
   }

   class Variable{
	public String cn;
	public String field;
	public Variable(String s, String in){
	    cn = s;
	    field = in;
	}
	public String toString(){
	    return cn + " "+ field;
	}
   }
}
