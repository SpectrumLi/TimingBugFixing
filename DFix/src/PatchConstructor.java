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

import uchicago.dfix.SideEffect;
import uchicago.dfix.PatchResult;

public class PatchConstructor {
    
    class LItem{
	public String cname;
	public String mname;
	public int lnum;
	public LItem(String c1, String m1, int l1){
	    cname = c1;
	    mname = m1;
	    lnum = l1; 
	}
	public String toString(){
	    return cname + " " + mname + " " +lnum;
	}
    }

    class slice{
	public String location;
	public String content;
	public slice (String l1, String c1){
	    location = l1;
	    content = c1;
	}
    }

    public HashMap<String, String> table;
    public SideEffect se;
    public ArrayList<slice> fullslice;
    public String inputdir;
    public int provedindex;
    public int RSite;
    public int CSite;
    public IClassHierarchy cha;
    public PatchResult pr;
    public boolean addrel;
    public String eesite;

    public PatchConstructor(String file, SideEffect se2, IClassHierarchy cha2){
	table = new HashMap<String, String>();
	se = se2;
	String s;
	inputdir = file;
	cha = cha2;
	provedindex = 0;
	try{
	    InputStream fis = new FileInputStream(file + "/config");
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
	    while ((s = br.readLine()) != null){
		String st[] = s.split("=");
		table.put(st[0], st[1]);
	    }

	    fullslice = new ArrayList<slice>();	    
	    fis = new FileInputStream(file + "/slicing");
            isr = new InputStreamReader(fis);
            br = new BufferedReader(isr);
	    while ((s = br.readLine()) != null){
		String s1 = br.readLine();
		fullslice.add(new slice(s , s1));
	    }

            fis = new FileInputStream(file + "/provedslicing");
            isr = new InputStreamReader(fis);
            br = new BufferedReader(isr);
	    int pi = 0;
            while ((s = br.readLine()) != null){
		pi ++;	
            }
	    int linenum = -1;
	    int tempi;
	    for (tempi =0; tempi<fullslice.size(); tempi++){
		LItem li = slice2litem(fullslice.get(tempi));
		if (linenum != li.lnum) {
		    pi--;
		    linenum = li.lnum;
		}
		if (pi < 0 ) {
		    break;
		}
	    }
	    provedindex = tempi;
	    System.out.println("    ProvedIndex="+provedindex);
	    pr = new PatchResult(table.get("sourcecode"), inputdir, "");
	    pr.wvariable = table.get("waitvariable");
	    if (table.get("lastcheck").equals( "address")) pr.address = true;
		else pr.address = false;
	}catch (Exception e){
	    e.printStackTrace();
	}
    }

    public void findRSite(){
	// blocking = msg callsite
	// unblocking = begin of critical section or Async enqueue
	RSite = fullslice.size();
	if (table.get("messagetype").equals("blocking")) return ;
	for (RSite = RSite -1; RSite >=0 ; RSite --){
	    LItem litem = slice2litem(fullslice.get(RSite));
	    IMethod im = getIMethod(litem.cname, litem.mname);	    
	    if (im.isSynchronized()) {
		RSite ++;
		return ;
 	    }
	    // Event Enqueue is to be implemented
	}
    }

    public void findCSite(){
	LItem l1;
	int rsite = RSite;
	if (RSite>=fullslice.size())
	    rsite = fullslice.size()-1; 

        l1 = slice2litem(fullslice.get(rsite));
	int fl = 1;
	LItem l2;
	for (CSite = rsite ; CSite >=0; CSite --){
	    l2 = slice2litem(fullslice.get(CSite));
	    if ((fl==1)&&(fullslice.get(CSite).content.contains("Application, Lorg/apache/hadoop/yarn/event/EventHandler, handle(Lorg/apache/hadoop/yarn/event/Event;)V"))){
		fl = 0;
		pr.eesite = l2.toString();
	    }
            IMethod im = getIMethod(l2.cname, l2.mname);
	    if ((l1 == null)||(!l1.cname.equals(l2.cname))||(!l1.mname.equals(l2.mname))){
	         CGNode cn = findNode(im);
		 if (findSideEffect(l2,startline(cn))) return;
	    } else{
		 if (findSideEffect(l2,l1.lnum)) return;
	    }
	    l1 = l2;
	}
	
	//if (table.get("lastcheck").equals("address")) CSite ++;
    }
   
    public boolean findSideEffect(LItem li, int st){
	//System.out.println(li.cname + " " + li.mname + " "+st+"->"+li.lnum);
        HashMap<Integer,Integer> hmap = se.map.get(li.cname);
        ArrayList<Integer> list = new ArrayList(hmap.keySet());
        Collections.sort(list);
	for (int x : list) {
	    if ((st <=x) &&(x< li.lnum)) {
		System.out.println("CSite is blocked by "+ li.cname + " " + li.mname+ " "+x);
		return true;
	    }
	}
	return false;
    }
   
    public int startline(CGNode n){
	IR ir = n.getIR();
        SSAInstruction [] ssaset = ir.getInstructions();


        for (int index = 0; index < ssaset.length; index++){
            SSAInstruction s = ssaset[index];
            if (s == null) continue;
            try{
                int bcIndex = ((IBytecodeMethod)n.getMethod()).getBytecodeIndex(index);
                int sln= n.getMethod().getLineNumber(bcIndex);
		return sln-1;
                } catch(Exception e){
                }

            }
	return 9999999;

    }

    public void genAndProveMiracle(){
	if (CSite < provedindex ){
	    System.out.println("miracle is proved unchanged");
	    pr.setProveResult(true);
	}else{
	    System.out.println("miracle could change");
	    pr.setProveResult(false);
	}
	/*
	if (CSite < 0){
	    System.out.println("miracle is the computed value");	
	}*/
	System.out.println("Miracle : ");
	pr.addMiracle(fullslice.get(0).location);	
	for (int x = CSite; x > 0 ; x--){
	    System.out.println(fullslice.get(x).location);
	    pr.addMiracle(fullslice.get(x).location);	
	}
    }
	
    public void dump(){
	if (RSite == fullslice.size()) {
	    System.out.println("RSite="+RSite+" "+ table.get("callsite"));
	    pr.setRSiteCallsite(table.get("callsite"));
	} else{
	    System.out.println("RSite="+RSite+" before "+fullslice.get(RSite).location);
	    pr.setRSite(fullslice.get(RSite).location,1);
	}

	if (CSite < 0){
	    if (!table.get("lastcheck").equals( "address")){
	        System.out.println("CSite="+CSite+" after "+ fullslice.get(0).location);
	        pr.setCSite(fullslice.get(0).location, 0);
	    }else{
	        System.out.println("CSite="+CSite+" before "+ fullslice.get(0).location);
                pr.setCSite(fullslice.get(0).location, 1);
	    }
	    
	}else{
	        System.out.println("CSite="+CSite+" after "+ fullslice.get(CSite+1).location);
	        pr.setCSite(fullslice.get(CSite+1).location, 0);	
	}
	pr.process();
	pr.dump(inputdir);
    }

    public LItem slice2litem(slice s){
	String ss = s.location;
	String [] sts = ss.split(",");
        String cname = sts[1].substring(2);
        int x = sts[2].indexOf("(");
        String mname = sts[2].substring(1,x);
	int xx = ss.indexOf(">");
	int lnum = Integer.parseInt(ss.substring(xx+2));
	return new LItem(cname, mname, lnum);
    }
   
    public IMethod getIMethod(String c1, String m1){
        IMethod im = null;
        if (c1.startsWith("java")) return null;
        for (IClass c : cha){
             String cname = c.getName().toString();
             if (cname.equals("L"+c1)){
                 for (IMethod m : c.getAllMethods()){
                      String mname = m.getName().toString();
                      if (mname.equals(m1))
			  return m;
                 }
            }
        }
	return null;
    }
  
    public CGNode findNode(IMethod im ){
        HashSet<CGNode> ns = new HashSet(se.cg.getNodes(im.getReference()));
        if ((ns == null) || (ns.size() == 0)){
            try{
                CGNode cgnode = ((BasicCallGraph<CGNode>)se.cg).findOrCreateNode(im,Everywhere.EVERYWHERE);
                return cgnode;
            } catch (Exception e){
                System.out.println("CREATE NEW NODE :: Failed");
                e.printStackTrace();
                return null;
            }
        }
        for (CGNode cn : ns)
            return cn;
        return null;
    }
 

    
} 
