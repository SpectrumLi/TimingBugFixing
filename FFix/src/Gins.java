package uchicago.ffix;

import java.io.*;
import java.util.*;

import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.ssa.IR;
import uchicago.ffix.*;

class Gins{
        public ArrayList<SSAInstruction> list;
        public ArrayList<CGNode> cnlist;
        public ArrayList<String> codelist;
        public String st;
        public Gins(){
             list = new ArrayList<SSAInstruction>();
             cnlist = new ArrayList<CGNode>();
             codelist = new ArrayList<String>();
        }
        public void setST(String s){ st = s;}

        public void addSSA(SSAInstruction ssa){ list.add(ssa); }
        public void addSSA(SSAInstruction ssa, CGNode cn){ list.add(ssa); cnlist.add(cn);}

        public void addSSA(ArrayList<SSAInstruction> ss){ list.addAll(ss);}
        public void addCGNode(ArrayList<CGNode> cns){ cnlist.addAll(cns);}
        
        public void addCGNode(CGNode cn) {cnlist.add(cn);}
        public void addCodeString(String s) {codelist.add(s);}
        public String toString(){
            String s = "";
            //System.out.println(list.size()+ " vs "+cnlist.size());
            for (int i  = 0 ; i < list.size(); i ++){
                if (i < cnlist.size())
                     s = s + "\n" + list.get(i).toString() + " \n in "+cnlist.get(i).toString();
                else
                s = s + "\n" + list.get(i).toString() + " in empty";
                //System.out.println(cnlist.get(i).toString());
                s = s +"\n" +reversecode(cnlist.get(i),list.get(i));
            }
            s = s + "\n" +st;
            return s;
        }

        public String ssaString(){
            String s = "";
            //System.out.println(list.size()+ " vs "+cnlist.size());
            for (int i  = 0 ; i < list.size(); i ++){
                 s = s + "\n" + list.get(i).toString();
                 //System.out.println(cnlist.get(i).toString());
            }
            s = s + "\n" +st;
            return s;
        }

        private String reversecode(CGNode cn, SSAInstruction ssa){
            String st = NameParser.getUseName(cn.getIR(),ssa,0) + "."+funcname(ssa)+"( ";
            for (int i =1; i < ssa.getNumberOfUses(); i++){
                    String s0 = NameParser.getUseName(cn.getIR(),ssa,i);
                    if (s0 != null)
                        st = st + s0  +",";
            }
            st = st.substring(0,st.length()-1) + ");";
            return st;
        }

        String funcname(SSAInstruction ssa){
                String s = ssa.toString();
                s=s.replaceAll("<init>","init");
                //System.out.println("find fn from "+s);
                s = s.split("<")[1].split(">")[0].split(", ")[2].split("\\(")[0];
                return s;
        }

   }

