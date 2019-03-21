package uchicago.ffix;
import java.io.*;
import java.util.*;

public class gppatch{

        String ob_roll;
        String ob_fast;
        String log_roll;
        String log_fast;
        String ob_action;
        String log_action;

        HashMap<String, String> conf;

        boolean type; //true=rollback, false=fast-forward

        public gppatch(boolean b, HashMap<String, String> conf2){

                this.type = b;
                this.conf = conf2;

                String temp = "rollback";
                if (b == false) temp = "fast-forward";
                System.out.println("This is a "+temp+ " patch");

                ob_roll = "try { "+
                        " LOG.info(\"GPL: patch is triggered\");" +
                        " File file = new File(\"/tmp/dfix/sliceresult\");" + 
                        " BufferedReader br = new BufferedReader(new FileReader(file));" +
                        " String line = \"111\";"         +
                        " while (line != null){ "         +
                        " line = br.readLine();"          +
                        " if (line == null) break;"       +
                        " String ss[] = line.split(\" \");" +
                        " File mf = new File(ss[0]); "    +
                        " if ( ss[1].equals(\"empty\")) " +
                        " mf.delete(); "                  +
                        " else{File mf2= new File(ss[1]); mf2.rename(ss[0]);}" +
                        " }}catch (Exception e){e.printStackTrace();}" +
                        " LOG.info(\"GPL: pattch finishes\");" ;


               ob_fast =  "try { "+
                          " LOG.info(\"GPL: patch is triggered\");" +
                          " File file = new File(\"/tmp/dfix/sliceresult\");" +
                          " BufferedReader br = new BufferedReader(new FileReader(file));" +
                          " String line = \"111\";"         +
                          " while (line != null){ "         +
                          " line = br.readLine();"          +
                          " if (line == null) break;"       +
                          " String ss[] = line.split(\" \");" +
                          " File mf = new File(ss[0]); "    +
                          " if ( ss[1].equals(\"zkdeleterecursively\")) " +
                          " ZKUtil.deleteNodeRecursively(this.zkHelper.getzk(),ss); " +
                          " if (ss[1].equals(\"write\")) "  +
                          " }}catch (Exception e){e.printStackTrace();}" +
                          " LOG.info(\"GPL: pattch finishes\");" ;

        }

        public static void main(String args[]){

                if (args.length <1 ){
                        System.out.println("Please point out the directory");
                }
                String inputdir = args[0];
                HashMap<String,String> map = new HashMap<String,String>();
                ArrayList<seop> ses = new ArrayList<seop>();
                try{
                      InputStream fis = new FileInputStream(inputdir+"/config");
                      InputStreamReader isr = new InputStreamReader(fis);
                      BufferedReader br = new BufferedReader(isr);
                      String s ;
                      while ((s = br.readLine()) != null){
                            String[] ss = s.split("=");
                            map.put(ss[0],ss[1]);
                      }
                      fis = new FileInputStream(inputdir+"/selist");
                      isr = new InputStreamReader(fis);
                      br = new BufferedReader(isr);
                      seop op1 = new seop();
                      while ((s = br.readLine()) != null ){
                              if (s.equals("")){
                                      ses.add(op1);
                                      op1 = new seop();
                              }
                              op1.addIns(s);
                      }
                      if (op1.st.size()>0) ses.add(op1);
                }catch(Exception e){
                      e.printStackTrace();
                      System.out.println("ERROR in loading configure file");
                      return ;
                }

                /*test input*/
                //for (seop op1:ses)
                //        System.out.println(op1);
                
                ArrayList<String> record = new ArrayList<String>();
                ArrayList<String> slices = new ArrayList<String>();
                for (seop op1 : ses){
                        System.out.println("original code = \n"+op1+"\n");
                        String r = op1.rollback();
                        System.out.println("record code = \n"+r);
                        record.add(r);
                        String ff = op1.fastforward();
                        System.out.println("fastforward code = \n"+ff);
                        slices.add(ff);
                        System.out.print("paras = ");
                        for (String s0: op1.paras) System.out.print(s0 + " ");
                        System.out.println("!");
                }

        }
}
class seop{
        public ArrayList<String>createfns;
        public ArrayList<String> st;
        public ArrayList<String> paras;
        public seop(){
               st = new ArrayList<String>();
               createfns = new ArrayList<String>();
               createfns.add("save");
        }
        void addIns(String s){
               if (s.equals("")) return ;
               st.add(s);
        }
        public String toString(){
               String s = "";
               for(String s2: st)s = s+ " "+s2;
               return s;
        }
        public String rollback(){
                String s0 = new String(st.get(st.size() -1));
                //System.out.println("s0    = "+ s0);
                //System.out.println("call  = "+ s0.split("\\( ")[0]);
                String [] sts = s0.split("\\( ")[0].split("\\.");
                String fn = sts[sts.length -1];
                //System.out.println("fname = "+ fn);
                String s = "String tempfn=" + s0.replaceAll(fn,fn+"_path") +"\n";
                if (createfns.contains(fn)){
                      /*empty class */
                      if (s0.contains("Zoo"))
                          s = s+ "dfix_write(tempfn,1 );"+"\n";

                      else
                          s = s+ "dfix_write(tempfn,0 );"+"\n";
                }else{
                      /*store original value*/
                      if (s0.contains("Zoo"))
                      s = s+ "dfix_write(tempfn,1);" + "\n";
                      else
                      s = s+ "dfix_write(tempfn,0);" + "\n";
                }
                return s;
        }
        public String fastforward(){
                String cs = "this";
                String ts = "";
                String fn = "";
                paras = new ArrayList<String>();
                try{
                for (String s: st){
                        String s0 = s;
                        //System.out.println("s0 = "+s0);
                        ts = s0.replaceAll("this", cs);
                        String cal = s0.split("\\( ")[0];
                        String [] sts = cal.split("\\.");
                        fn = sts[sts.length -1];
                        //System.out.println("current fn = "+fn+ " \n   cal = "+cal );
                        cs = cal.substring(0,cal.length() - fn.length()-1);
                        sts = s0.split("\\( ")[1].split(",");
                        for (String s2:sts){
                                if (!s2.contains("this")) paras.add(s2.split("\\)")[0]);
                        }
                }
                ts = "String tempfn=" + ts.replaceAll(fn,fn+"_path_content") +"\n";
                if (paras.size() == 2){
                        
                        ts = ts.replaceAll(" "+paras.get(1), " "+paras.get(0)); // tricky impl
                        ts = ts.replaceAll(","+paras.get(1), ","+paras.get(0)); // tricky impl
                        System.out.println("replace paras : "+ paras.get(1)
                                        + " -> "+ paras.get(0));
                        System.out.println(ts);
                }
                if (ts.contains("Zoo"))
                    ts = ts + "dfix_write(tempfn,1);" + "\n";
                else
                    ts = ts + "dfix_write(tempfn,0);" + "\n";
                }catch(Exception e){}
                return ts;
        }
}

