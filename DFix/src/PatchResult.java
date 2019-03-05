package uchicago.dfix;

import java.io.*;
import java.util.*;
import org.apache.commons.io.FileUtils;

public class PatchResult{
    
    class location{
	String cname;
	String mname;
	int lnum;
        public String content;

	public location(String c1, String m1, int l1){
	    cname = c1;
	    mname = m1;
	    lnum = l1;
	}

	public location(String s){
            String [] sts = s.split(",");
            cname = sts[1].substring(2);
            int x = sts[2].indexOf("(");
            mname = sts[2].substring(1,x);
            int xx = s.indexOf(">");
            lnum = Integer.parseInt(s.substring(xx+2));
	}

	public String toString(){
	    return cname + " " + mname + " " + lnum;
	}

    }
 
    public location rsite;
    public int relr; // 1 means before the location, 0 means after the location
    public location csite;
    public int relc;

    public ArrayList<location> miracle;
    public ArrayList<location> ma; // miracle answer
    public String source;
    public String target;
    public String wvariable;
    public String eesite;

    public boolean first;
    public boolean address; 
    public boolean proveresult;

    public PatchResult(String s, String t, String eesite2){
	source = s;
	target = t;
	miracle = new ArrayList<location>();
	ma = new ArrayList<location>();
	first = true;
	eesite = eesite2;
    }
   
    public void setRSite(String s, int flag){
	relr = flag;
	rsite = new location(s);	
    }
    public void setRSiteCallsite(String s){
	String [] sts = s.split(" ");
	relr = 1;
	rsite = new location(sts[0], sts[1], Integer.parseInt(sts[2]));
    }
 
    public void setCSite(String s, int flag){
	relc = flag;
	csite = new location(s);
    }

    public void setProveResult(boolean f){
	proveresult = f;
    }
   
    public void addMiracle(String s){
	location la = new location(s);
	for (location l2 : miracle){
	    if ( (la.cname.equals(l2.cname)) &&
		 (la.mname.equals(l2.mname)) &&
		 (la.lnum == l2.lnum)){
		//System.out.println("Exist : "+ s);
		return ;
	    }
	}
	miracle.add(la);
    }

    public void process(){
	for (location l :miracle){
	    String sts[] = l.cname.split("/");
	    String temp = sts[sts.length -1].split("\\$")[0];
	    System.out.println("Load "+ temp);
	    String file = source+ "/src/" + sts[sts.length -1].split("\\$")[0] + ".java";
	    try{
	        String line = getfilebyline(file,l.lnum);
	        //String line = (String) FileUtils.readLines(new File(file)).get(l.lnum);
		l.content = line;
	        //ma.add(line);
		System.out.println("Fetch "+file + " L"+l.lnum);
	    } catch(Exception e){
		e.printStackTrace();
	    }
	}
	//for (location l : miracle){
	for (int i=0 ; i<miracle.size(); i++){
	    location l = miracle.get(i);
	    if (first){
		if (address){
		    String sts[] = wvariable.split("\\+");
		    l.content = "String key =";
		    for (String st : sts){
		     l.content = l.content + "Integer.toString( System.identityHashCode("+st+")) +";
		    }
		    
		    l.content = l.content.substring(0, l.content.length()-1)+ ";"; 
		}else 
		    l.content = "String key = "+wvariable+ ".toString();";
		first = false;
		if (l.content.indexOf(';') == l.content.length()-1)
		    System.out.println("The key computation also has no side effect");
	    } else{
		
	    }
	}
	for (location l : miracle)
	    System.out.println(l.content);

    }
    
    public String getfilebyline(String f, int x){
	try{
            InputStream fis = new FileInputStream(f);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            String s;
	    int i = 0;
            while ((s= br.readLine())!=null){
		i++;
		if (i == x) return s;
            }

        }catch (Exception e){
	    System.out.println("Load "+ f + " error");
            e.printStackTrace();			
        }
	return "";

    }
    public void dump(String file){
	try {
	    PrintWriter writer = new PrintWriter(file +"/patchGuide", "UTF-8");
	    String s1,s2;
	    if (relr == 1) s1="before"; else s1 = "after";
	    if (relc == 1) s2="before"; else s2 = "after";
	    writer.println("RSite:=" + s1+" "+ rsite );
	    writer.println("CSite:=" + s2+" "+ csite );
	    writer.println("ProveResult:="+proveresult);
	    if ( (!wvariable.contains("+")) &&( !wvariable.contains("."))){
	        writer.println("AddRel:=no");
	    }else{
		writer.println("AddRel:=yes");
	    }
	    String st="";
	    for (location l : miracle)
                st= st+ " "+l.content;

	    writer.println("addresscode:="+st);
	    if (!eesite.equals(""))
		writer.println("eesite:="+eesite);
	    writer.close(); 
	}catch(Exception e){
	}
    }
}





