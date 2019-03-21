package uchicago.ffix;

import java.io.*;
import java.util.*;
//import org.apache.commons.lang3.StringUtils;
import uchicago.ffix.srcloc;

public class littleSE{

    public static void main(String[] args){
	if (args.length < 1){
             System.out.println("Please give the input directory");
 	    return;
 	}
	String inputdir = args[0];
 	HashMap<String,String> map = new HashMap<String,String>();
 	try{
 	   InputStream fis = new FileInputStream(inputdir+"/config");
 	   InputStreamReader isr = new InputStreamReader(fis);
 	   BufferedReader br = new BufferedReader(isr);
 	   String s ;
 	   while ((s = br.readLine()) != null){
 		String[] ss = s.split("=");
		map.put(ss[0],ss[1]);
	   }
	   String jcode = map.get("jcode");
	   fis = new FileInputStream(jcode);
           isr = new InputStreamReader(fis);
           br  = new BufferedReader(isr);
	   ArrayList<String> ss = new ArrayList<String>();
	   while ((s = br.readLine()) != null){
		ss.add(s);
	   }
	   srcloc sl = new srcloc(map.get("start"));
	   int sll = sl.getlinenum()-1;
	   ArrayList<Integer> ans = new ArrayList<Integer>();
	   ans.add(sll);
	   String v = "239749032750932590328353947594837534";
	   if (ss.get(sll).contains("=")) v = ss.get(sll).split("=")[0];
	   while (true){
		sll ++;
		int l = 0;
		int r = 0;
		if (ss.get(sll).contains("if") && ss.get(sll).contains("!"+v)){
		    while((r==0)||(l!= r)){
	    		l += littleSE.count(ss.get(sll),"{");
		    	r += littleSE.count(ss.get(sll),"}");
			sll++;
		    }
		}
		ans.add(sll);
		if (ss.get(sll).contains("return")) break;
	   }
	   for (int x : ans)
		System.out.println(x);
	   

        }catch(Exception e){
	    e.printStackTrace();
	      }
    }
     public static int count(final String string, final String substring){
         int count = 0;
         int idx = 0;
         while ((idx = string.indexOf(substring, idx)) != -1){
             idx++;
             count++;
         }
         return count;
     }
}
