package uchicago.dfix;

import java.io.*;
import java.util.*;

import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;

public class Stateer{
    
    public static HashSet<PointerKey> pkeyset;
    public static PointerAnalysis pa;
    public static String getSSAInstruction(Statement s){
	String st = s.toString();
	String [] sts = st.split(":");
	String answer = "";
	for (int i =1; i< sts.length; i++){
	    if (sts[i].endsWith("Node")) break;
	    answer += sts[i];
	}
	return answer;
    }

    public static int SSAAssignNumber(Statement s){
	try{
	    String ssa = getSSAInstruction(s).split(" ")[0];
	    try {
		return Integer.parseInt(ssa);
	    } catch (Exception e){
		return -1;
	    }
	} catch (Exception e){
	    System.out.println(s + " process AssignNumber error ");
	    e.printStackTrace();
	}
	return -1;
    }
 
    public static ArrayList<Integer> SSARefNumber(Statement s){
	ArrayList<Integer> answer = new ArrayList<Integer>();
        try{
	    String ssa = getSSAInstruction(s);
	    //only consider the invoke instruction
	    //System.out.println("Compute the RefNum "+ssa);
	    if (ssa.startsWith("invoke") 
		|| ssa.split("<")[0].contains("invoke")
		){
		int x = ssa.indexOf('>');
		int y = ssa.indexOf('@');
		String ps = ssa.substring(x+2, y-1);
		//System.out.println( "para string from "+x +" to "+y+":"+ ps);
	        String [] pss = ps.split(",");
		//System.out.println( "para string from "+x +" to "+y+":"+ pss[0]);
		for (String st :pss){
		  //  System.out.println("PARA "+ st+"added!");
		    answer.add(Integer.parseInt(st));
		}
		return answer;
		
	    }
        } catch (Exception e){
            System.out.println(s + " process SSARefNumber error ");
	    e.printStackTrace();
        }
	return null;
    }

    public static HashSet<Integer> SSARelParameter(HashSet<Integer> list, int th){
        HashSet<Integer> list2 = new HashSet<Integer>();
        for (int x : list){
            if (x<th )
                list2.add(x);
        }
        return list2;
    }

    public static ArrayList<Statement> SSAFilter(ArrayList<Statement> list, HashSet<Integer> para){
	int i;
	for ( i = list.size()-1; i >= 0 ; i--){
	    Statement ts = list.get(i);
	    System.out.println("  search "+ ts);
	    if (ts.toString().split("<")[0].contains("invoke")) break;
	}
	if (list.size() == 0) return new ArrayList<Statement>();
	ArrayList<Statement> list2 = new ArrayList<Statement>();
	Statement ls = list.get(list.size()-1);
	if (i>=0){
	    System.out.println("********** find invoke instruction" + list.get(i));
	    ls = list.get(i);
	    list2.add(ls);
	}
	if (i<0) return list2;
	HashSet<Integer> absp = SSAAbsParameter(ls,para);
	int x =-9999;
	System.out.println("ABSP = "+ absp);
	if (SSARefNumber(ls).size() == 2 ) return list;
	if (SSARefNumber(ls).size() == 1 ) list2.add(ls);
	//if (absp.size() == 2) return list;
	//if (absp.size() ==1) for (int y : absp) x = y;
	for (Statement s : list){
	    if ( absp.contains(SSAAssignNumber(s))  ) 
		list2.add(s);
	    if (SSAAssignNumber(s) ==x)
		list2.add(s);
	}
	return list2;
    }

    public static HashSet<Integer> SSAAbsParameter(Statement s, HashSet<Integer> para){
	HashSet<Integer> answer = new HashSet<Integer>();
	ArrayList<Integer> touch = SSARefNumber(s);
	try{
	    for (int x : para){
		System.out.println(x +"th para is required for slicing +++");
	        answer.add(touch.get(x-1));
	    }
	}catch (Exception e){
	    System.out.println("s = " + s);
	    System.out.println("para = " + para);
	    System.out.println("touch = " + touch);
	    throw e;	
	}
	return answer;
    }
}
