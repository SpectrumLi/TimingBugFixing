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

public class ProveRecord{
	
    public IClassHierarchy cha;
    public boolean lastflag; // true = address, false = value
   
    public HashMap< String, HashSet<Integer>> result;
    public HashMap< String, HashSet<Integer>> map;
    public HashMap< String, IMethod> mfmap;
    public ArrayList<String> porder;
    public HashSet< String> hset;
    public CallGraph cg;

    public ProveRecord(String file, IClassHierarchy cha2, CallGraph cg2, String flag){
	cg  = cg2;
	cha = cha2;

	if (flag.equals("V"))
	    lastflag = false;
	else
	    lastflag = true;
	
	try{
            System.out.println("load to-be-proved file "+ file);
            InputStream fis = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
	    result = new HashMap< String, HashSet<Integer>>();
	    map = new HashMap< String, HashSet<Integer>>();
	    mfmap = new HashMap<String, IMethod>();
	    porder = new ArrayList<String>();
	    hset = new HashSet<String>();
            String s;
	    while ((s = br.readLine()) != null){
                String st[] = s.split(">");
		int lnum = Integer.parseInt(st[1].substring(1));
	  	String sts[] = st[0].split(",");
	        String cname = sts[1].substring(2);
        	int x = sts[2].indexOf("(");
        	String mname = sts[2].substring(1,x);
		String id = cname + ":"+ mname;
		if (! porder.contains(id)){
		    porder.add(id);
		    mfmap.put(id , getMf(cname , mname));
		}
		if (map.get(id) == null)
		    map.put(id,new HashSet<Integer>());
		map.get(id).add(lnum);
		s = br.readLine();
		hset.add(s);
		
            }
	    for (String st : porder){
		List l = new ArrayList(map.get(st));
		Collections.sort(l);
		for (int i = l.size()-1; i >=0 ; i--)
		    System.out.println(st + " " + l.get(i));
	    }

	}catch (Exception e){
            System.out.println("Orignal Slicing file cannot find");
            e.printStackTrace();
	}
    }
    public void prove(){
       for (String st : porder){
            ArrayList<Integer> l = new ArrayList(map.get(st));
            Collections.sort(l);
	    result.put(st, new HashSet<Integer>());
            for (int i = l.size()-1; i >=0 ; i--){
		int test = testProvable(st, l.get(i));
		if (test == 1) return;
		System.out.println(st + " " + l.get(i)  + " is provable");
		result.get(st).add(l.get(i));
	    }
                    
       }	
    }

    public int testProvable(String st, Integer lnum){
	IMethod im = mfmap.get(st);
	CGNode cn = findNode(im);
	if (cn == null ) return 0;
	IR ir = cn.getIR();
        SSAInstruction [] ssaset = ir.getInstructions();

        for (int index = 0; index < ssaset.length; index++){
             SSAInstruction s = ssaset[index];
             if (s == null) continue;
             try{
                    int bcIndex = ((IBytecodeMethod)cn.getMethod()).getBytecodeIndex(index);
                    int sln= cn.getMethod().getLineNumber(bcIndex);
		    if (sln == lnum){
			if (!insideTargets(s.toString())) continue;
			if (lastflag){ 	
			    if (isUnprovableAddressSSA(s,im,cn)) {
			        lastflag = false;
			        return 1;
			    }
			}else{
		            if (isUnprovableValueSSA(s,im,cn)) {
                                lastflag = false;
                                return 1;
                            }			
		        }
			System.out.println(s + " is provable");
		    }
                } catch(Exception e){
		    
            	    e.printStackTrace();
                }

         }
	lastflag= false;
	return 0;
    }
    public boolean insideTargets(String s){
	for (String t : hset)
	    if (t.contains(s)) return true;
	return false;
    }
    public boolean isUnprovableValueSSA(SSAInstruction ss, IMethod im, CGNode cn){
	// if the written value no change
	String s = ss.toString();
	if ((s.contains("getstatic"))
	   || (s.contains("= invoke"))
	   || (s.contains("getfield"))){
	    System.out.println("Changable Value " + ss);
	    return true; 
	}
	return false;
    }	
    
    public boolean isUnprovableAddressSSA(SSAInstruction ss, IMethod im, CGNode cn){
	// if the written address no change
	String s = ss.toString();
	//if (s.contains("putstatic")) return false;
	//if (s.contains("putfield 1")) return false;
	if ((s.contains("putfield")) && (!s.contains("putfield 1"))){
	     System.out.println("Changable Address " + ss);
	     return true;
	}
	return false;
    }

    public void dump(String file){
	System.out.println("------------  Proved Slicing  ------------");
	try{
	    PrintWriter writer = new PrintWriter(file +"/provedslicing", "UTF-8");
	    for (String st : porder){
	        if (result.get(st) == null ) {
		    writer.close();
		    return ;
		}
	        ArrayList<Integer> l = new ArrayList(result.get(st));
                Collections.sort(l);
	        for (int i : l){
		    System.out.println(st + " " + i);
		    writer.println(st + " " + i);
                }

            }
	    writer.close();
	}catch (Exception e){
	}
	
    }
    
    public CGNode findNode(IMethod im ){
	HashSet<CGNode> ns = new HashSet(cg.getNodes(im.getReference()));
	if ((ns == null) || (ns.size() == 0)){
	    try{
		CGNode cgnode = ((BasicCallGraph<CGNode>)cg).findOrCreateNode(im,Everywhere.EVERYWHERE);
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

    public IMethod getMf (String cname1, String mname1){
	for (IClass c : cha){
             String cname = c.getName().toString();
             if (cname.equals("L"+cname1)){
                 for (IMethod m : c.getAllMethods()){
                      String mname = m.getName().toString();
                      if (mname.equals(mname1))
                          return m; 
                 }
             }
        }
	return null;	
    }
}
