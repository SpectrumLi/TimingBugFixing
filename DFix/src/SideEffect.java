package uchicago.dfix;

import java.io.*;
import java.util.*;
import com.google.common.collect.HashBiMap;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.classLoader.IBytecodeMethod;

import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.impl.BasicCallGraph;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;

public class SideEffect{

    class cItem{
	public String cname;
	public String mname;
	//public int start;
	//public int stop;
	public cItem(String cn, String mn){
	    cname = cn;
	    mname = mn;
	}
	public cItem(CGNode cn){
	    String sts[] = cn.toString().split(",");
            cname = sts[1].substring(2);
            int x = sts[2].indexOf("(");
            mname = sts[2].substring(1,x);
	}
	public String toString(){
	    return cname.toString() + " " + mname.toString();
	}

        public boolean equals(cItem o){
	    return (this.cname == o.cname) && (this.mname == o.mname);
	}	
    }

    public ArrayList<cItem> olist; // old list
    public ArrayList<cItem> clist; // old list plus the following node
    public HashMap<String, HashMap<Integer,Integer>> map;// class -> sideeffecttable
    public HashBiMap<cItem, CGNode> mnodemap;
    public CallGraph cg;
    public IClassHierarchy cha;
    public HashSet<cItem> checked; 
    public HashSet<cItem> flushed; 
    public boolean addrel;
    public String eesite;

    public SideEffect(String file, CallGraph cgi, IClassHierarchy cha2){
	try{
	    System.out.println("load se file "+ file);
	    InputStream fis = new FileInputStream(file);
	    InputStreamReader isr = new InputStreamReader(fis);
	    BufferedReader br = new BufferedReader(isr);
	    String s;
	    clist = new ArrayList<cItem>();
	    olist = new ArrayList<cItem>();
	    map = new HashMap<String, HashMap<Integer,Integer>>();
	    flushed = new HashSet<cItem>();
	    checked = new HashSet<cItem>();
	    mnodemap = HashBiMap.create();
	    //mnodemap = new HashBiMap<cItem, CGNode>();
	    while ((s = br.readLine()) != null){
		String st[] = s.split(" ");
		cItem citem = new cItem(st[0], st[1]);
		olist.add(citem);
		System.out.println("LOAD :: "+ citem);
		//ArrayList<Integer> list = new ArrayList<Integer>();
		//for (int i= 1; i<2000; i++) list.add(-1);
		//map.put(citem, list);
	    }
	} catch (Exception e){	
	    System.out.println("Side Effect file cannot find");
	    e.printStackTrace();
	}
	cg = cgi;
	cha = cha2;
	addrel = true;
	eesite = "";
    }
    
    public boolean check (CGNode cn, cItem citem){
	if (cn == null) return false;
	IMethod imethod = cn.getMethod();
	String [] sts = imethod.toString().split(",");
	//System.out.println("CHECK :: "+cn.toString());
	if (citem == null) citem = getFunction(sts);
	if (citem == null  ) return false;
	System.out.println("CHECK :: "+cn.toString());
	LinkedList<CGNode> queue = new LinkedList<CGNode>();
        HashSet<String> visited = new HashSet<String>();
	if (checked.contains(citem)) return true;
	queue.add(cn);
	visited.add(Integer.toString(System.identityHashCode(cn)));
	while (queue.size()!= 0){
	    CGNode n = queue.poll();
	    cItem ci = getfromList(new cItem(n));
	    if (checked.contains(ci)) continue;
	    checked.add(ci);
	    if (!n.toString().contains("Application")) continue; 
	    if ((mnodemap.get(ci) == null) &&(mnodemap.inverse().get(n)==null )){
	        mnodemap.put(ci,n);
	//	System.out.println(ci +" BINJECT "+ n);
	    }else {
		/*		
		if (mnodemap.get(ci) != null){
		    mnodemap.get(ci);
		}else{
		    mnodemap.inverse.get(n);
		}
		*/
		mnodemap.forcePut(ci,n);
		//System.out.println(citem.hashCode() + " cannot be loaded to BiMap because ->");
		//System.out.println( mnodemap.get(ci) +" vs "+ mnodemap.inverse().get(n));
	    }
	    HashMap<Integer, Integer> hmap;
	    if (map.get(ci.cname) == null)
		hmap = new HashMap<Integer,Integer>();
	    else 
		hmap = map.get(ci.cname);
	    IR ir = n.getIR();
	    // add cn follower!
	    SSAInstruction [] ssaset = ir.getInstructions();

	    int sum;
	    if (hmap.get(0) == null) sum = 0;
	    else sum = hmap.get(0);

            for (int index = 0; index < ssaset.length; index++){
	        SSAInstruction s = ssaset[index];
	        if (s == null) continue;
                try{
		    int bcIndex = ((IBytecodeMethod)n.getMethod()).getBytecodeIndex(index);
                    int sln= n.getMethod().getLineNumber(bcIndex);
		    if (hasSideEffectAssignment(s,ci)) {
			if (hmap.get(sln) == null) hmap.put(sln, 1);
			else hmap.put(sln, hmap.get(sln)+1);
			sum++;
		    }
		    /** Event Enqueue check **/
		    if (s.toString().contains("Application, Lorg/apache/hadoop/yarn/event/EventHandler, handle(Lorg/apache/hadoop/yarn/event/Event;)V"))
			eesite = ci.cname + " noneedfunctionname " + sln;
		    /**/
	        } catch(Exception e){
		    //System.out.println("Process "+ index+"th SSA in "+imethod.toString() );
	            //e.printStackTrace();
		    // BIG WARNNING !!!!!! the reason of outofbound is not clear
	        }
		
	    }
	    hmap.put(0,sum);
	    map.put(ci.cname, hmap);
	    clist.add(ci);
	    System.out.println("Analyze " + ci);
	    for (Iterator<? extends CGNode> inode = cg.getSuccNodes(n); inode.hasNext();){
                    CGNode n2= inode.next();
                    String idn2=Integer.toString(System.identityHashCode(n2));
                    if (!visited.contains(idn2)){
                        queue.add(n2);
                        visited.add(idn2);
                    }

                }
	}
	return true;
    }

    public void createMissing(){
	for (cItem citem : olist){
	    if (!clist.contains(citem)){
		check(createNewNode(citem), citem);
	    }
	}
    }

    public CGNode createNewNode( cItem citem){
	IMethod im = null;
	if (citem.cname.startsWith("java")) return null;
        for (IClass c : cha){
             String cname = c.getName().toString();
             if (cname.equals("L"+citem.cname)){
   //              System.out.println("Get class:"+cname);
                 for (IMethod m : c.getAllMethods()){
                      String mname = m.getName().toString();
                      if (mname.equals(citem.mname)){
     //                      System.out.println("Get method:"+mname);
                           if (im == null){
                               im = m;
		//	       break;
                           }else
                               System.out.println("Multiple MR for "+ im);
                     }
                 }
            }
        }
	if (im == null){
	    System.out.println(im + " is not in the ClassHierarchy");
	    return null;
	}	
        MethodReference mr = im.getReference();
	try{
            CGNode cgnode = ((BasicCallGraph<CGNode>)cg).findOrCreateNode(im,Everywhere.EVERYWHERE);
	    //System.out.println("CREATE NEW NODE :: "+ cgnode);
	    if (cgnode.toString().contains("Application")){
		//dumpNode(cgnode);
		System.out.println("       NEW NODE ADDED:: "+ im);
	        return cgnode;
	    }else return null;	
	}catch (Exception e){
	    System.out.println("CREATE NEW NODE :: Failed");
	    e.printStackTrace();
	    return null;
	}
    }

    public void toplogyFlush(){
	System.out.println(" olist "+ olist.size()+" vs clist "+ clist.size());
	for (cItem ci : olist){
	    System.out.println("search "+ci);
	    int flag = 0;
	    cItem ci3 = null;
	    for (cItem ci2 : clist){
	    	System.out.println("      searched "+ci2);
		if ((ci.cname.equals(ci2.cname)) && (ci.mname.equals(ci2.mname))){
		    ci3 = ci2;
	    	    flag =1;
		    break;
		}
	    }
	    if (flag > 0)  computeInvokeSideEffect(ci3);
	    else System.out.println("a CITEM in olist cannot be found in clist");
	}
    }
   
    public void computeInvokeSideEffect(cItem c){
	flushed.add(c);
	CGNode n = mnodemap.get(c); 
	if (n == null ){
	    System.out.println(c + " has no node for invokeSideEffect");
	    return ;
	}
	for (Iterator<? extends CGNode> inode = cg.getSuccNodes(n); inode.hasNext();){
            CGNode n2= inode.next();
	    if (! flushed.contains(mnodemap.inverse().get(n2))){
//		flushed.add(mnodemap.inverse().get(n2));
		System.out.println( c +" -> " + mnodemap.inverse().get(n2));
	        computeInvokeSideEffect(mnodemap.inverse().get(n2));
	    }
	}

        HashMap<Integer, Integer> hmap = map.get(c.cname);
        IR ir = n.getIR();
        // add cn follower!
        SSAInstruction [] ssaset = ir.getInstructions();

        int sum;
        if (hmap.get(0) == null) sum = 0;
        else sum = hmap.get(0);

        for (int index = 0; index < ssaset.length; index++){
            SSAInstruction s = ssaset[index];
            if (s == null) continue;
            try{
                int bcIndex = ((IBytecodeMethod)n.getMethod()).getBytecodeIndex(index);
                int sln= n.getMethod().getLineNumber(bcIndex);
                if (hasSideEffectInvoke(s)) {
                    if (hmap.get(sln) == null) hmap.put(sln, 1);
                    else hmap.put(sln, hmap.get(sln)+1);
                    sum++;
                }
            } catch(Exception e){
            }

        }
        hmap.put(0,sum);
        map.put(c.cname, hmap);
        System.out.println("Flush " + c); 
	
    }
   
    public boolean hasSideEffectInvoke(SSAInstruction ssa){
	//System.out.println("     ssa "+ssa);
	String [] sts = ssa.toString().split(",");
	if (!sts[0].contains("invoke")) return false;
        String cname = sts[1].substring(2);
        int x = sts[2].indexOf("(");
        String mname = sts[2].substring(1,x);
	if (specialEffect(cname,mname)) return true;
        cItem citem = getfromList(new cItem(cname, mname));
	if (! checked.contains(citem)){
	    if (! sts[0].contains("Application")){	
                System.out.println(citem +  " is not a application class ??? ");
                return false;
	    }
	    CGNode cnode = createNewNode(citem);
	    if (cnode == null){
	        System.out.println(citem +  " is not a application class ??? ");
	        return false;
	    }
	    check(cnode,citem);
  	    computeInvokeSideEffect(getfromList(citem));
	}
	System.out.println("+++++   check the se of function : "+ citem + " ->"+ ssa);
	//System.out.println("+++++   check the se of function : "+ citem + " ->"+ ssa);
	if (map.get(citem).get(0) == 0) return false;
	return true;
    }

    public boolean specialEffect(String c, String m){
	if ((c.equals("java/util/HashMap")) &&(m.equals("put"))) return true;
	if ((c.equals("java/util/HashMap")) &&(m.equals("remove"))) return true;
	if ((c.equals("java/util/Map")) &&(m.equals("put"))) return true;
	if ((c.equals("java/util/Map")) &&(m.equals("remove"))) return true;

	if ((c.equals("java/util/HashSet")) &&(m.equals("add"))) return true;
	
	return false;	
    }

    public cItem getfromList(cItem citem){
	 for (cItem ci2 : clist){
             if ((ci2.cname.equals(citem.cname)) && (ci2.mname.equals(citem.mname)))
                 return ci2;
         }
	 return citem;
    }    

    public void dump(){
	for (String cname : map.keySet()){
	    HashMap<Integer,Integer> hmap = map.get(cname);
	    if (hmap == null ) 
		System.out.println("Nothing in class "+ cname);
   	    //System.out.println(citem.cname + " " + citem.mname + " 0 "+ hmap.get(0));
	    if (hmap.get(0) == 0) 
		continue;
	    //System.out.println("dump keyset "+ hmap.keySet());
	    ArrayList<Integer> list = new ArrayList(hmap.keySet());
	    Collections.sort(list);
	    for (int x = 1 ; x< list.size(); x++){
		int y = list.get(x);
		System.out.println(cname + " "+ y + " "+ hmap.get(y));
	    }
	}
	System.out.println("--------------  OLIST  ---------------");
        for (cItem ci : olist){
	    HashMap<Integer,Integer> hmap = map.get(ci.cname);
	    System.out.println(ci.cname + " : "+ hmap.get(0));
	}
    }
   

    public cItem getFunction(String sts[]){
	if (!sts[0].contains("Application")) return null;
 	String cname = sts[1].substring(2);
	int x = sts[2].indexOf("(");
	String mname = sts[2].substring(1,x);
	//System.out.println("GETFUNCTION :: "+cname+" " + mname);
	for (cItem citem : olist) {
	    if ( (cname.equals(citem.cname)) &&( mname.equals(citem.mname))){
	        //System.out.println("GETFUNCTION :: "+cname+" " + mname);
		return citem;
	    }
	}
	return null;
    }

    public boolean hasSideEffectAssignment(SSAInstruction s, cItem ci){
	String ss = s.toString();
	if ((ss.contains("putfield") && (!ci.mname.contains("<init>")))){
//	    System.out.println("     SE :: "+ss);
  	    return true;
	}
	if ( ss.contains("putstatic")){
//	    System.out.println("     SE :: "+ss);
  	    return true;
	}
	else return false;
    }
	
    public void dumpNode(CGNode cn){
	IR ir = cn.getIR();
        System.out.println("      " +cn);
	if (ir == null) {	
	    System.out.println("IR = ?");
	    return;
	}
        for (Iterator <SSAInstruction> iir = ir.iterateAllInstructions(); iir.hasNext();){
             SSAInstruction s = iir.next();
             System.out.println(s);
         } 
     }
}
