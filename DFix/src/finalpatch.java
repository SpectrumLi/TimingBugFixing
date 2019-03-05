package uchicago.dfix;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import com.github.javaparser.*;
import com.github.javaparser.JavaParser.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.visitor.*;
import com.github.javaparser.ast.body.*;

public class finalpatch{

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

    String inputdir;
    String currentCN;
    boolean clonecontext = false;
    public HashMap<String , String> table;
    public ArrayList<location> wcs;
    public ArrayList<location> scs;
    public HashMap<String, CompilationUnit> cumap;
    public HashMap<String, String> result;
    public HashMap<String, ArrayList<String>> diffpatch;
    public HashMap<String, ArrayList<String>> clonepatch;

    public HashMap<String, ArrayList<String>> implpatch;

    public finalpatch(String in){
	inputdir = in;
	table = new HashMap<String, String>();
	cumap = new HashMap<String, CompilationUnit>();
	result = new HashMap<String, String>();
	diffpatch = new HashMap<String, ArrayList<String>>();
	clonepatch = new HashMap<String, ArrayList<String>>();
	implpatch = new HashMap<String, ArrayList<String>>();
	load(inputdir+"/config", "=");
	load(inputdir+"/patchGuide", ":=");
	loadcss(1,inputdir+"/css");
	loadcss(2,inputdir+"/signalcss");
	//System.out.println(table);
    }

    public void safeAdd(HashMap<String, ArrayList<String>> map, String key, String value){
	if (!map.containsKey(key))
	    map.put(key, new ArrayList<String>());
	map.get(key).add(value);
    }

    public void load(String file, String sp){
	try{
            InputStream fis = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            String s;
            while ((s= br.readLine())!=null){
		String [] ss = s.split(sp);
		table.put(ss[0],ss[1]);
            }

        }catch (Exception e){
            System.out.println("No file exception");
        }

    }
   
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

    public String lcstoscn(String s){
	//long callstack to short class name transfer
	//System.out.println("Shorten the classname "+ s);
	String [] ss = s.split("/");
	String [] ss2 = ss[ss.length-1].split(" ");
	//String [] ss3 = ss2[0].split("$");
	return ss2[0];
    }

    public static void main(String [] args){
	if (args.length < 1){
	    System.out.println("please specify the input directory");
	    return ;
	}
	finalpatch fp = new finalpatch(args[0]);
	if ((fp.table.get("zookeepapi") != null )&&(fp.table.get("zookeepapi").equals("y"))){
	    fp.setRPCRepeat();
	    fp.dump();
	    return ;
	}
	String [] ss = null;
	if (fp.table.get("messagetype").equals("unblocking")){
	    fp.cloneCss(fp.table.get("signalsite"), fp.scs );
	    if (fp.table.get("AddRel").equals("no")){
		ss = fp.table.get("RSite").split(" ",2);
	        fp.cloneCss(ss[1],fp.wcs);
	    }else{
  	        ss = fp.table.get("CSite").split(" ",2);
                fp.cloneCss(ss[1],fp.wcs);
	    }
            fp.addSignalCode();
            fp.addWaitCode();
            fp.rename(fp.table.get("signalsite"),fp.scs);
            fp.rename(ss[1],fp.wcs);
	}else{
	   
	    // redundent code for easy debug 
	    ss = fp.table.get("CSite").split(" ",2);
	    fp.cloneCss(ss[1], fp.wcs);
	    fp.cloneCss(fp.table.get("signalsite"), fp.scs);
	    fp.addEventFlag(fp.table.get("eesite"));
	    fp.addSignalCode();
	    fp.addWaitCode();
	
	    fp.setRPCRepeat();
	    fp.eventRename(fp.table.get("signalsite"),fp.scs,  fp.table.get("seventtype"));
	    fp.eventRename(ss[1] ,fp.wcs,  fp.table.get("weventtype"));
	}
	
	fp.dump();
    }

    public void addEventFlag(String loc){
	if (loc == null ) return; // a hidden truth is that the eesite is always in a different class with signalsite.
	String [] loc2 = table.get("CSite").split(" ");// loc2 is the flag dec location, the loc is the use site 
        location l = new location(loc);
        location l2 = new location(loc2[1],loc2[2],loc2[3]);
        CompilationUnit cu = getCU(l2);
        String cn= lcstoscn(l2.toString());
	if (cn.contains("$")) cn = cn.split("\\$")[0];
        ClassOrInterfaceDeclaration tc = getClassByName(cu, cn );
        FieldDeclaration fd;
	String fdstring = "public static AtomicInteger dfixeventflag = new AtomicInteger();";
	safeAdd(diffpatch, cn, "+++    " + fdstring);
        fd = (FieldDeclaration) JavaParser.parseClassBodyDeclaration(fdstring);
        tc.addMember(fd);
	String exception = table.get("blockingexception");
	//System.out.println(exception.replace("/", "."));
	setCU(l2,cu);
	cu = getCU(l);
        cu.addImport(exception.replace("/", "."), false, false);
        cu.addImport(l2.cn.split("\\$")[0].replace("/", "."), false, false);
	setCU(l,cu);
	//System.out.println("Put dfix event flag to " + tc.toString());
	//String st = cu.toString();
	String st = getCodeAfterClone(l);
	String tg = getLinefromfile(l);
	String exp = lcstoscn(exception); //for the exp should implement two abstract functions
	String tgs = tg + " DFix_EventWait();";
	String itgs = "DFix_EventWait(): \n" + " int t_dfix=0;"+cn+".dfixeventflag.getAndAdd(1);\n" +
			"while (true) { \n" +
			    " try{ Thread.sleep(200); } catch (Exception e){}\n"+
			    " if ("+cn+".dfixeventflag.get() >= 1000 ) break;\n" + 
			    " t_dfix++; if (t_dfix > 20) throw new " + exp + "(\"dfix\");\n"+
			"}";
	safeAdd(implpatch,this.currentCN,itgs);
	replaceCode(st, tg, tgs, l);
        System.out.println("Set the EE-Site wait to " + loc);
    }

    public void setRPCRepeat(){
	String rpcsite = table.get("callsite"); //need to be the exption handle location not the high level rpc call
	System.out.println("setting rpc to "+rpcsite);
	location l = new location(rpcsite);
        if (table.get("blockingexception") != null){	// form throwable rpc call
	    String exp = lcstoscn(table.get("blockingexception"));
	    String st = getCodeAfterClone(l);
	    String tg = getLinefromfile(l);
	    String brk ="";
	    String tgs ="  DFix_Rpc();";
	    String itgs = "";
	    if (tg.contains("="))
 	    tgs = tg.split("=")[0] +" = DFix_Rpc();";
	    if (tg.contains("return"))
		tgs = "return DFix_Rpc();";
	    if (!tg.contains("return")) brk = "break;";
	    if (table.get("zookeepapi") == null){		
	           itgs = "int i = 0;\n" +  
			"while (true) {\n"+
			"try{\n" +
			"  if (i>100) break;\n  i++;\n" +
			tg + brk +"\n"+
			//"break;" + 
			"} catch(" + exp +  " e_e){\n" +
			    "if (!e_e.getMessage().contains(\"dfix\")) throw e;\n" + 
			"}\n" +  
		     "}\n";
	    }else{
		if (table.get("waittargetv") == null){
                    itgs = "int i = 0;\n" + 
			    "while (true) {\n"+
                        "try{\n" +
			"  if (i>100) break;\n  i++;\n" +
                        tg + brk + "\n" +
                        "} catch(" + exp +  " e_e){\n" +
                        "}\n" +
                     "}\n";
		 }else{
		     String pftgs = "String dfixcss = \"\";\n";
		     if (table.get("cssID")!= null){
			pftgs = pftgs + "for (StackTraceElement dfix_ste : Thread.currentThread().getStackTrace()) {dfixcss += dfix_ste.toString();}\n";
		     }
                     itgs = "int i = 0;\n"+
			     pftgs + "while (" + table.get("waitvariable") + "!=" + 
                                table.get("waittargetv") + ") {\n" + 
				"  if (i>100) break; \n  i++;"+
                        tg + "try{Thread.sleep(1000);}catch(Exception e_e){}\n" +
			"if (!dfixcss.contains(\""+ table.get("cssID") + "\")) break;\n" +
                     "}\n";
                }

	    }
	    if (tg.contains("="))
               itgs = itgs+"return " + tg.split("=")[0] + "\n";
	    itgs = "DFix_Rpc():\n" + itgs;
	    safeAdd(implpatch, this.currentCN, itgs);
	    replaceCode(st, tg, tgs, l);
	    System.out.println("RPC repeat added");
	}else{  // use a carry return variable for is 
	    String rv = table.get("callsitevariable");
            String st = getCodeAfterClone(l);
            String tg = getLinefromfile(l);
            String brk ="";
	    String tgs = rv + " = DFix_Socket();\n";
            if (!tg.contains("return")) brk = "break;";
            String itgs = "DFix_Socket():\n" + 
		    	  " int i = 0;\n "+
		  	  "  while (true) {\n"+
			  " if (i>100) break; \n i++;\n" +
                        tg +"\n" + 
			"if (!"+rv+".toString().equals(\"0\"))\n" + brk +
                     "}\n" +
		     "return " + rv +";\n";
	    safeAdd(implpatch, this.currentCN, itgs);
            replaceCode(st, tg, tgs, l);
            System.out.println("RPC repeat added");

	}
    }

    public void eventRename(String loc , ArrayList<location> cs, String et){
	//similar with rename function (but include the event clone)
	clonecontext = true;
	String cn = lcstoscn(loc);
	for (int i = 0; i < cs.size()-1; i++){
            location l = cs.get(i);
            String cn2 = lcstoscn(l.toString());

            if (cn2.equals(cn) && l.mn.equals((new location(loc)).mn)){
		clonecontext = false;
                break;
            }
	    if ((i == 0)&&(et!= null)){
		setEventCall(l, cs.get(i+1), et);
	    }else
                setClonedCall(l, cs.get(i+1));
        }
	clonecontext = false;
    }
   
    public void setEventCall(location l, location l2, String et ){
	String st = getCodeAfterClone(l);
        String tg = getLinefromfile(l);
        String tgs = tg.replaceAll("."+l2.mn+"\\(","."+l2.mn+"_dfix\\(");
	tgs = "if (event.getType().equals(\""+et+ "\")) " + tgs + " else " + tg;
        replaceCode(st, tg, tgs, l );
        System.out.println("set clone call "+ l);
    }

    public void rename(String loc , ArrayList<location> cs){
	clonecontext = true;
	String cn = lcstoscn(loc);
        for (int i = 0; i < cs.size(); i++){
            location l = cs.get(i);
            String cn2 = lcstoscn(l.toString());
            
            if (cn2.equals(cn) && l.mn.equals((new location(loc)).mn)){
		break;
            }
	    setClonedCall(l, cs.get(i+1));
        }
	clonecontext = false;
    }

    public void cloneCss(String loc, ArrayList<location> cs){
	//clonecontext = true;
	//System.out.println("CC to be true");
	String cn = lcstoscn(loc);
	for (int i = 0; i < cs.size(); i++){
	    location l = cs.get(i);
	    String cn2 = lcstoscn(l.toString());
	    if (i > 0)
                cloneOne(l);
	    
	    if (cn2.equals(cn) && l.mn.equals((new location(loc)).mn)){
		//addLastLevel(new location(loc));
		//clonecontext = false;
		//System.out.println("CC to be false");
		return ;
	    }
	    //setClonedCall(l);
	}
	//clonecontext = false;
	//System.out.println("CC to be false");
    }
    public void setCU(location l,CompilationUnit cu){
	cumap.put(l.cn,cu);
    }
    public CompilationUnit getCU(location l){
        CompilationUnit cu = cumap.get(l.cn);
        if (cu == null){
            try{
                String sloc = table.get("sourcecode");
                String cn = lcstoscn(l.toString()).split("\\$")[0];
                FileInputStream fis = new FileInputStream(sloc + "/src/"+cn+".java");
                cu = JavaParser.parse(fis);
		cu.addImport("java.util.concurrent.Semaphore", false, false);
		cu.addImport("java.util.concurrent.atomic.AtomicInteger", false, false);
		cumap.put(cn, cu);
            }catch (Exception e){
                e.printStackTrace();
            }
    	}
	return cu;
    }
    
    public void cloneOne(location l){
	CompilationUnit cu = getCU(l);
	//System.out.println("cu = " + cu);
	MethodDeclaration md = null;
	ClassOrInterfaceDeclaration tc =null;
	boolean flag_already = false;
	try{
	    String cn = lcstoscn(l.toString());
	    //if (cn.contains("$")) cn = cn.split("\\$")[1];
	    System.out.println("Cloning "+l + " -> "+cn);
	    tc = getClassByName(cu, cn);
	    NodeList<BodyDeclaration<?>> members = tc.getMembers();
            for (BodyDeclaration<?> member : members) {
                if (member instanceof MethodDeclaration) {
                    MethodDeclaration method = (MethodDeclaration) member;
                    if (method.getName().toString().equals(l.mn)) {
			md = method.clone();
			method.setName(md.getName().toString() + "_dfix");
			method.addThrownException(java.lang.Exception.class); //carefully
		    } else if (method.getName().toString().equals(l.mn+ "_dfix")){
			flag_already = true;
			throw new Exception("already cloned for " + method.getName().toString());
		    }
                }
            } 
	}catch(Exception e){
	    if (e.getMessage().startsWith("already clone for "))
		System.out.println(e.getMessage());
	    else
	        e.printStackTrace();
	}
	if (!flag_already){
	    tc.addMember(md);
	   // System.out.println(" After clone " + tc );
	    System.out.println("Clone function" + l);
	}
	//System.out.println("After clone " + cu);
	//System.out.println("After clone " + getCU(l));
	setCU(l,cu);
    }

    public String findCalledObj(location l, ArrayList<location> cs){
	//this implementation could be wrong 
	int x,y;
	for (x =0 ; x < cs.size(); x++){
	    location li = cs.get(x);
	    if (l.cn.equals(li.cn) && (l.mn.equals(li.mn))) break;
	}
	String ans = "";
	for (y =x; y< cs.size() -1; y++){
	    location li = cs.get(y);
	    String sli = getLinefromfile(li);
	    char[] cli = sli.toCharArray();
	    int st, end, fl;
	    for (st = 0 ; st < sli.length(); st++)
		if (( (cli[st]>='a') &&( cli[st]<='z'))  
		   || ((cli[st]>='A')&&( cli[st]<='Z')))
		break;
	    for (fl = 0 ; fl < sli.length(); fl++)
		if (cli[fl] == '(') break;
	    for (end = fl-1; end >=0; end--)
		if (cli[end] == '.') break;
	    ans = sli.substring( st, end); 
	}
	System.out.println("Found the callobject " + ans );
	return ans;
    }
 
    public void addWaitCode(){
	// no consideration for Async-Operation currently
	if (table.get("messagetype").equals("unblocking")){
	    if (table.get("AddRel").equals("no")){
	        String [] sts = table.get("RSite").split(" ");
 	        location l = new location(sts[1], sts[2], sts[3]);
	        String st = getCodeAfterClone(l);
	        String obj = findCalledObj(l,wcs);
	        String tg = getLinefromfile(l);
		String tgs = "DFix_Wait();" + tg;
	        String itgs = obj+".se_dfix.tryAcquire(1,7,java.util.concurrent.TimeUnit.SECONDS);";
		safeAdd(implpatch, this.currentCN, itgs);
	        replaceCode(st, tg, tgs, l);
	    } else{
		String [] stsr = table.get("RSite").split(" ");
                location lr = new location(stsr[1], stsr[2], stsr[3]);
		String [] stsc = table.get("CSite").split(" ");
		location lc = new location(stsc[1], stsc[2], stsc[3]);
		String str = getCodeAfterClone(lr);
		String tg = getLinefromfile(lr);
		String tgs = "DFix_Repeat();";
		if (tg.contains("="))
		    tgs = tg.split("=")[0] + " = DFix_Repeat();\n";
		String itgs = "DFix_Repeat():\n" +
			      "  int i = 0;\n" +
			      "  while(true){ try{\n" + 
			      "  if (i>100) break; \n i++;\n" + 
			      tg+"break;\n}catch( Exception e){}}\n";

		if (tg.contains("="))
		    itgs = itgs + "\n" + "return "+tg.split("=")[0]+";\n";
		safeAdd(implpatch, this.currentCN, itgs);
		replaceCode(str, tg, tgs, lr);
		String stc = getCodeAfterClone(lc);
		tg = getLinefromfile(lc);
		String obj = findCalledObj(lc,wcs);
		String mapcheck = "";
		tgs = "DFix_Wait(" + table.get("waitvariable") +");";
		if (obj.equals("")) mapcheck = "hm_dfix.get(key) == null";
		    else mapcheck = obj + ".hm_dfix.get(key) == null";
		String tgst = table.get("addresscode")+ "if ("+mapcheck+") throw new Exception(\"DFIX EXCEPTION \");";	
		if (stsr[0].equals("before")){
		    itgs = tgst ;
		    tgs = tgs + tg;
		}
		else{
		    itgs = tgst;
		    tgs = tg + tgs;
		}
		itgs = "DFix_Wait("+ table.get("waitvariable") +"):\n" + itgs;
		safeAdd(implpatch, this.currentCN , itgs );
		replaceCode(stc , tg, tgs, lc);

	    }
	}else {	
	   
	    //consider if the wait is in a eventHandler
	    if (table.get("weventtype") == null){
		// ToBE Continue for CA-1011 
		//System.out.println("process CA-1011");
		if (table.get("type").equals("AV")){
		    //AV meand not address related;
		    String [] loc = table.get("CSite").split(" ");
                    location l = new location(loc[1], loc[2], loc[3]);
		    String [] loc2 = table.get("signalsite").split(" ");
		    String scn = lcstoscn(loc2[0]);
		    if (scn.contains("$")) scn = scn.split("\\$")[0];
		    String st = getCodeAfterClone(l);
                    String tg = getLinefromfile(l);
		    String tgs = table.get("waitvariable")+" = DFix_Wait();";
		    String itgs = "DFix_Wait():\ntry { \n" +
			"if ("+scn+".se_dfix.tryAcquire(1,5,java.util.concurrent.TimeUnit.SECONDS) ) \n" + tg+"\n"+
		      "else " + table.get("waitvariable") + " =\"00000000000000000000000\";\n} catch (InterruptedException e_dfix) {\n}\n return " + table.get("waitvariable")+";\n"; //illeage string all 0s
		    safeAdd(implpatch, this.currentCN, itgs);
		    replaceCode(st , tg, tgs, l);
		}else{
		}
	    } else{
		//Still check the eventtype because of two clone the same functions
		location ees = new location(table.get("eesite")); // eesit must exist if weventtype is not null 
	
		if (table.get("AddRel").equals("no")){
		    location l;
		    String obj = "";
		    if ( (table.get("wehandlersite")!=null)&&(wcs.size()<=2)){
			System.out.println("Extend the unchange prove to an upper layer");
			l = new location ( table.get("wehandlersite") );
		    }else{
		        String [] loc = table.get("CSite").split(" ");
		        l = new location(loc[1], loc[2], loc[3]);
		    	obj = findCalledObj(l,wcs);
		    }
		    String st = getCodeAfterClone(l);
		    if (!obj.equals("")) obj =obj + ".";
		    String tg = getLinefromfile(l);
		    String scn = lcstoscn(ees.cn);
	            if (scn.contains("$")) scn = scn.split("\\$")[0]; 
		    String eclass = lcstoscn(table.get("CSite").split(" ")[1]);
	            if (eclass.contains("$")) eclass = eclass.split("\\$")[0];  
		    if (l.cn.endsWith(eclass)) eclass = "";// not right but easy for equal
		    if (!eclass.equals("")) eclass = eclass + ".";
		    String tgs = "DFix_EventWait(event);\n";
		    String itgs = "DFix_EventWait(event):\nif (event.getType().toString().equals(\"" +table.get("weventtype")+"\")){\n"+
			"if (" +obj +"se_dfix.availablePermits() >1) {\n"+
			    //"if (" +scn+".dfixeventflag.get() > 1000) {"+ scn+".dfixeventflag.getAndDecrement(); return ;}"+
			    "if ("+eclass+ "dfixeventflag.get() > 1000) {\n" +
			eclass + "dfixeventflag.getAndDecrement(); return ;}\n"+
			    eclass + "dfixeventflag.getAndAdd(1000);\n" + 
			    //scn+"dfixeventflag.getAndAdd(1000);" + 
			"}else{\n" +
			    //scn+".dfixeventflag.getAndDecrement(); return ;}"+
			    eclass + "dfixeventflag.getAndDecrement(); return ;}\n"+
			"}\n" + tg;
		   
		   safeAdd(implpatch , this.currentCN, itgs );
		   replaceCode(st, tg ,tgs, l);
		}else{
                    location l;
                    String obj = "";
                    if ( (table.get("wehandlersite")!=null)&&(wcs.size()<=2)){
                        System.out.println("Extend the unchange prove to an upper layer");
                        l = new location ( table.get("wehandlersite") );
                    }else{
                        String [] loc = table.get("CSite").split(" ");
                        l = new location(loc[1], loc[2], loc[3]);
                        obj = findCalledObj(l,wcs);
                    }
		   String eclass = lcstoscn(table.get("CSite").split(" ")[1]);
                   if (eclass.contains("$")) eclass = eclass.split("\\$")[0];
		   if (l.cn.endsWith(eclass)) eclass = ""; // not right but easy for eqaul
		   if (!eclass.equals("")) eclass = eclass + ".";
                   String st = getCodeAfterClone(l);
		   if (!obj.equals("")) obj =obj + ".";
                   String tg = getLinefromfile(l);
                   String scn = lcstoscn(ees.cn);
                   if (scn.contains("$")) scn = scn.split("\\$")[0];
		   String tgs = "if (!DFix_CheckDrop()) return ;" + tg;
		   String itgs = "DFix_CheckDrop():\n"+table.get("addresscode") + "if ("+obj+"hm_dfix.get(key)==null) {" +
			eclass+"dfixeventflag.getAndDecrement(); return false ;"+
		   "}else{"+
		       eclass+"dfixeventflag.getAndAdd(1000);" +
		   "}" + tg + "\n return ture;\n";
		   safeAdd(implpatch, this.currentCN, itgs);
   		   replaceCode(st, tg, tgs, l);
		}
	    }
	}
    }
    public void replaceCode(String fstring, String oldstring, String newstring, location l){
        int occur = (fstring.length() - fstring.replace(oldstring,"").length())/oldstring.length();
        if (occur >1) {
            System.out.println("Multiple occurence of "+ oldstring+" = "+ occur+"!");
        }
	if (occur == 0){
	    System.out.println("NOT found ???  what's wrong???");
	    System.out.println(oldstring + " \n" + fstring );
	}
        String scn = lcstoscn(l.cn);
        if (scn.contains("$")) scn = scn.split("\\$")[0];
        System.out.println(oldstring+ "\n-> "+ newstring);
	String ansst =null;
	if (occur>1)	
            ansst = fstring.replaceFirst(Pattern.quote(oldstring),newstring);
	else{
	    System.out.println(occur + " occurence use replace instead of replacefirst");
            ansst = fstring.replace(oldstring,newstring);
	}
        if (ansst.length() - fstring.length() == 0 )
            System.out.println("No change:" );
        result.put(scn, ansst);
        //System.out.println(ansst);	
        //System.out.println(oldstring+ "\n-> "+ newstring);	
	String st = "---  " + oldstring+"\n" + "+++  " + newstring;
	if(clonecontext == false){
	    safeAdd(diffpatch, this.currentCN, st);
	}
	else{
	    safeAdd(clonepatch, this.currentCN, st);
	    System.out.println("ADD CLONE : " + st);
	}
    }

    public void addSignalCode(){
	String loc;
	if (table.get("sehandlersite")!=null)
	    loc = table.get("sehandlersite"); 
	else
	    loc = table.get("signalsite");
	location l = new location(loc);
	System.out.println("Add signal to " + loc);
	CompilationUnit cu = getCU(l);
	//cu.addImport("java.util.concurrent.Semaphore", false, false);
	String cn= lcstoscn(l.toString());
	this.currentCN = cn;
        //if (cn.contains("$")) cn = cn.split("\\$")[1];
	ClassOrInterfaceDeclaration tc = getClassByName(cu, cn );
	//System.out.println("Class update "+  tc.toString());
	FieldDeclaration fd;
	String release = "";
	String obj = ""; // no need for callobject.As is always in the signalsite class.
	String tgs = "";
	String tg = getLinefromfile(l);
	String vss = "";
	int relcount= 9999;
	if (table.get("type").equals("AV")) relcount = 1;
	if (!obj.equals("")) obj = obj + ".";
	if (table.get("AddRel").equals("yes")){

            String fdstring = "public static HashMap<String, Semaphore > hm_dfix = new HashMap<String,Semaphore>();";
	    safeAdd(diffpatch, cn, "+++    "+ fdstring);

	    fd = (FieldDeclaration) JavaParser.parseClassBodyDeclaration(fdstring);
	    String sv = table.get("signalvariable");
	    if (!sv.contains("+")){
	        release = "String dfixkey = Integer.toString( System.identityHashCode("+sv +"));"+obj+"hm_dfix.put(dfixkey, new Semaphore("+relcount+"));";
	    	tgs = "DFix_Signal(" + sv + ");\n";
	    }
	    else{
		String key = "";
	        String [] svs = sv.split("\\+");
		tgs = "DFix_Signal(";
		for (int i=0; i< svs.length; i++){
		    key = key + "Integer.toString( System.identityHashCode("+svs[i]+")) +";
		    tgs = tgs + svs[i] + ",";
		    vss = vss + svs[i] + ",";
		}
		tgs = tgs.substring(0, tgs.length() -1);
		key = key.substring(0, key.length() -1);
		vss = vss.substring(0, vss.length() -1);
		release = "String dfixkey = "+ key +";"+obj+"hm_dfix.put(dfixkey, new Semaphore("+relcount+"));";
		tgs = tg + tgs +");\n";
	    }
	}else{
	    String fdstring = "public Semaphore se_dfix = new Semaphore(0);";
	    safeAdd(diffpatch, cn, "+++    " + fdstring);

	    fd = (FieldDeclaration) JavaParser.parseClassBodyDeclaration(fdstring);
	    if (table.get("type").equals("AV"))
		fd = (FieldDeclaration) JavaParser.parseClassBodyDeclaration("public static Semaphore se_dfix = new Semaphore(1);");
	    release = obj+"se_dfix.release("+relcount+");";
	    tgs = tg + " DFix_Signal();\n";
	}
	tc.addMember(fd);
	System.out.println("~~~ add semaphore to " + cn);
	//System.out.println("Class update "+  tc.toString());
  	//if (cu ==null ) System.out.println("     ~~~~~~~ error cu = null");	
	String st = cu.toString();
	/*
	String [] sts = st.split("\n");
	int index = Integer.parseInt(l.l);
	sts[index+1] = sts[index+1] + release;
	*/
	String itgs = "";
	if (table.get("seventtype") != null)
	    itgs = " if (event.getType().toString().equals(\"" + table.get("seventtype") +"\" ))" + release + "\n ";
	else    itgs =  release +"\n";
	itgs = "DFix_Signal(" + vss + "):\n" + itgs;
	/*
	int occur = (st.length() - st.replace(tg,"").length())/tg.length();
	if (occur >1) {
	    System.out.println("Multiple occurence of "+ tg+" = "+ occur+"!!!!!!!");
	}
	String scn = lcstoscn(l.cn);
	if (scn.contains("$")) scn = scn.split("$")[0];
	String ansst = st.replaceFirst(Pattern.quote(tg),tgs);
	if (ansst.length() - st.length() == 0 ) 
	    System.out.println("No change:" );
	result.put(scn, ansst);
	System.out.println(tg+ " -> "+ tgs);
	System.out.println("SignalPatch added to "+ ansst);
	//System.out.println(st);
	*/
	safeAdd(implpatch, this.currentCN, itgs);
	replaceCode(st, tg, tgs, l);
    }
    public void setClonedCall(location l, location l2){
	String st = getCodeAfterClone(l);
	/*
        try{
            String cn;
            if (l.cn.contains("$")) cn = l.cn.split("$")[1];
                else cn = l.cn;
            ClassOrInterfaceDeclaration tc = cu.getClassByName(cn).get();
            NodeList<BodyDeclaration<?>> members = tc.getMembers();
            for (BodyDeclaration<?> member : members) {
                if (member instanceof MethodDeclaration) {
                    MethodDeclaration method = (MethodDeclaration) member;
                    if (method.getName().toString().equals(l.mn)) md = method.clone();
                    if (method.getName().toString().equals(l.mn + "_dfix")) mddfix = method.clone();
                }
            } 
        }catch(Exception e){
            e.printStackTrace();
        }
	*/
	String tg = getLinefromfile(l);
        String tgs = tg.replaceAll("."+l2.mn+"\\(","."+l2.mn+"_dfix\\(");
	replaceCode(st, tg, tgs, l );
	System.out.println("set clone call "+ l);
    }

    public String getCodeAfterClone(location l){
	String cn = lcstoscn(l.toString()).split("\\$")[0];
	this.currentCN = cn;
	if (result.get(cn) == null){
	    System.out.println("    get CU to replace for "+cn);
	    return getCU(l).toString();
	}else{
	    System.out.println("    get AnSString to replace for "+ cn);
	    return result.get(cn);
	}
    }
    public void dump(){
        String sloc = table.get("sourcecode");
	try{
	    PrintWriter pr = new PrintWriter(sloc + "/details");
	    System.out.println("--------  PATCH DETAILS  ---------\n");
	    pr.println("--------  PATCH DETAILS  --------\n");
 	    for(String cn : this.diffpatch.keySet()){
	        System.out.println("In the " + cn + ".java");
	        pr.println("In the " + cn + ".java");
	        for(String c : this.diffpatch.get(cn)){
		    System.out.println();
	            System.out.println(c);
		    pr.println();
		    pr.println(c);
	        }
	        System.out.println();
		pr.println();
	    }
	    
	    pr.println("--------  IMPL  DETAILS  --------\n");
            for(String cn : this.implpatch.keySet()){
                System.out.println("In the " + cn + ".java");
                pr.println("In the " + cn + ".java");
                for(String c : this.implpatch.get(cn)){
                    System.out.println();
                    System.out.println(c);
                    pr.println();
                    pr.println(c);
                }
                System.out.println();
                pr.println();
            } 
	    pr.println("--------  CLONE DETAILS  --------");
            for(String cn : this.clonepatch.keySet()){
                System.out.println("In the " + cn + ".java");
                pr.println("In the " + cn + ".java");
                for(String c : this.clonepatch.get(cn)){
                    System.out.println();
                    System.out.println(c);
                    pr.println();
                    pr.println(c);
                }
                System.out.println();
                pr.println();
            }

	    pr.close();
	}catch(Exception e){
	    e.printStackTrace();
	}
	try{
            for (String cn : result.keySet()){
                //System.out.println(result.get(cn));
		PrintWriter writer = new PrintWriter( sloc + "/patch/"+cn+".java" , "UTF-8");
		writer.print(result.get(cn));
		writer.close();
	    }

        }catch (Exception e){
            e.printStackTrace();
        }

    }
    public ClassOrInterfaceDeclaration getClassByName(CompilationUnit cu ,String cn){
	System.out.println("Fetch "+cn + " from the CUTABLE");
	if (cn.contains("$")){
	    String [] cns = cn.split("\\$");
	    ClassOrInterfaceDeclaration tc = cu.getClassByName(cns[0]).get();
	    NodeList<BodyDeclaration<?>> types =tc.getMembers();
            for (BodyDeclaration<?> type : types) {
                if ( type instanceof ClassOrInterfaceDeclaration){
                    ClassOrInterfaceDeclaration cid = (ClassOrInterfaceDeclaration) type;
                    if (cid.getName().toString().equals(cns[1])){
			//System.out.println(cid);
			return cid;
		    }

                }
            }
   
	}
        return cu.getClassByName(cn).get();
    }
    public String getLinefromfile(location l){
	int ln = Integer.parseInt(l.l);
	try{
            String sloc = table.get("sourcecode");
            String cn = lcstoscn(l.toString()).split("\\$")[0];
            FileInputStream fis = new FileInputStream(sloc + "/src/"+cn+".java");
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            String s;
	    int li = 0;
            while ((s= br.readLine())!=null){
		li ++ ;
		if (ln == li) return s;
            }

        }catch (Exception e){
            e.printStackTrace();
        }
	return "";
    }
}
