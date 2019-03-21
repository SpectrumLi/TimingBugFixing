package uchicago.dfix;

import java.io.*;
import java.util.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import java.io.File;

public class frontend{

    public class Operation{

	String pid;
	String tid;
	String opval;
	String optype;
	String css;

	public Operation(Element e){
	    pid = e.getElementsByTagName("PID").item(0).getTextContent();
	    tid = e.getElementsByTagName("TID").item(0).getTextContent();
	    optype = e.getElementsByTagName("OPTY").item(0).getTextContent();
	    opval = e.getElementsByTagName("OPVAL").item(0).getTextContent();
	    Element csse =(Element) e.getElementsByTagName("Stacks").item(0);
	    int csslen = Integer.parseInt(csse.getAttribute("Len"));
	    css = "";
	    for (int temp = 0; temp < csslen; temp++){
	        Element cse =(Element) csse.getElementsByTagName("Stack").item(temp);
	        String cs = "";
	        String cn = cse.getElementsByTagName("Class").item(0).getTextContent();
	        String mn = cse.getElementsByTagName("Method").item(0).getTextContent();
	        String ln = cse.getElementsByTagName("Line").item(0).getTextContent();
	        cs = cn + " " + mn + " " + ln + "\n";
	        css = css + cs;
	    }
	}

	public String toString(){
	    String s = pid + " " + tid + " " + optype + " " + opval+ "\n";
	    return s + css;
	}
    }

    public class DataRace{
	int x;
	int y;
	public DataRace(String s){
	    String ss[] = s.split(" ");
	    x = Integer.parseInt(ss[0]);
	    y = Integer.parseInt(ss[1]);
	}
    }

    public class Bug{
	int x;
	int y;
	int type; //buggy timing: 1 = x happens before y, 2 = y happens before x , 3= both
	
	public Bug(String s){
	    String [] ss= s.split(" ");
	    x = Integer.parseInt(ss[0]);
	    y = Integer.parseInt(ss[1]);
	    type = Integer.parseInt(ss[2]);
	}
	
	public String toString(){
	    return x+ " " + y + " " + type;
	}
    }

    public class CausalChain{
	String st;
	public CausalChain( String s){
	    st = s;
	}
    }

    public HashMap<String, String> table;              // config map
    public ArrayList<CausalChain> cc;                       // casuality chains
    public ArrayList<Operation> base;                  // base file
    public ArrayList<DataRace> dr;                     // data race list
    public ArrayList<Bug> buglist; // bug list
    public HashMap<Integer, ArrayList<Integer>> hblist; // for happen before relation
    int opsum;
    
    String dcreportdir; // the location for DC reports

    public frontend(String ftconfig){
	table = new HashMap<String, String>();
	load(ftconfig);
	dcreportdir = table.get("dcreportdir")+"-xmlresult";
	cc = new ArrayList<CausalChain>();
	dr = new ArrayList<DataRace>();
 	loadbase();
	loadmedian();	
    }

    public void loadmedian(){
	try{
            InputStream fis = new FileInputStream(dcreportdir + "/median");
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            String s;
            while ((s= br.readLine())!=null){
                DataRace datarace = new DataRace(s);
		dr.add(datarace);
		cc.set(datarace.x, new CausalChain( br.readLine()));
		cc.set(datarace.y, new CausalChain( br.readLine()));
            }
    
	} catch (Exception e){
	    e.printStackTrace();
	}
	System.out.println(dr.size() + " dataraces are Loaded ");
    }

    public void loadbase(){
	base = new ArrayList<Operation>();
	try{
	    File basefile = new File(dcreportdir + "/base");
	    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	    Document doc = dBuilder.parse(basefile);
	    doc.getDocumentElement().normalize();
	    NodeList nList = doc.getElementsByTagName("OPINFO");
	    opsum = 0;
	    for (int temp = 0; temp < nList.getLength(); temp++) {
		Node nNode = nList.item(temp);
		if (nNode.getNodeType() == Node.ELEMENT_NODE) {
		    Element e1 = (Element) nNode;
		    Element e  = (Element) e1.getElementsByTagName("Operation").item(0);	
		    base.add(new Operation(e)); 
		    opsum++;
		    cc.add(null);
		}
	    }
	} catch (Exception e){
	    e.printStackTrace();
	}
	System.out.println(opsum + " ops loaded!");	
    }

    public void load(String file){
	try{
            InputStream fis = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            String s;
            while ((s= br.readLine())!=null){
                String [] ss = s.split("=");
                table.put(ss[0],ss[1]);
		System.out.println(ss[0] + " = " + ss[1]);
            }

        }catch (Exception e){
            System.out.println("Frontend config file loading exception");
        }
    }

    public void dumpmedian(){
	try{
    	    PrintWriter writer = new PrintWriter(dcreportdir+"/MOPS", "UTF-8");
            for (DataRace d : dr){
            writer.println("----------------------------------------------");
            writer.println(d.x + " " + d.y);
            writer.println(base.get(d.x).toString());
            writer.println("");
            writer.println(base.get(d.y).toString());
            writer.println("----------------------------------------------");            }
	    writer.close();
	    System.out.println("MOPS information is dumped");
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    public void loadbugandhb(){

	buglist = new ArrayList<Bug>();
	hblist  = new HashMap<Integer,ArrayList<Integer>>();
	try {
	    String file = dcreportdir + "/bugs";
	    InputStream fis = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            String s;
            while ((s= br.readLine())!=null){
		if (s.startsWith("//")) continue;
		buglist.add(new Bug(s));
            }
	} catch (Exception e){
	    e.printStackTrace();
	}
	System.out.println(buglist.size() + " bugs are loaded");

        try {
            String file = dcreportdir + "/hbresult";
            InputStream fis = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            String s;
            while ((s= br.readLine())!=null){
		if (s.contains(" -> ")){
			String [] ss = s.split(" -> " );
			int x = Integer.parseInt(ss[0]); 
			int y = Integer.parseInt(ss[1]);
			if (hblist.get(x) == null)
			    hblist.put(x, new ArrayList<Integer>());
			hblist.get(x).add(y);
		}
		if (s.contains(" <- ")){
			String [] ss = s.split(" <- " );
                        int x = Integer.parseInt(ss[0]); 
                        int y = Integer.parseInt(ss[1]);
                        if (hblist.get(y) == null)
                            hblist.put(y, new ArrayList<Integer>());
                        hblist.get(y).add(x);
		}
            }
        } catch (Exception e){
            e.printStackTrace();
        }
	
			
    }

    public void work(){
    // basically three types of bugs
    // undomed order violation 
    // p,c,r
    // p1,c1 vs p2,c2
	System.out.println("  Detecting ... ");
	for (Bug bug : buglist){
	    if (bug.type != 3){
		//System.out.println(" BUG = "+ bug);
                int xx,yy; // correct order is xx -> yy
                int xx2, yy2; // correct order is xx2 -> yy2
                if (bug.type ==1 ){
                    xx = bug.y;
                    yy = bug.x;
                }else{
                    xx = bug.x;
                    yy = bug.y;
                }
		int sum =0;
		for (Bug b2 : buglist){
		    if (b2 != bug){
			if (b2.type == 3) continue;
			if (b2.type ==1 ){
			    xx2 = b2.y;
			    yy2 = b2.x;
			}else{
			    xx2 = b2.x;
			    yy2 = b2.y;
			}
			if ((hblist.get(yy2) != null) && (xx == xx2) && hblist.get(yy2).contains(yy)){
			    System.out.println( bug +" no need to fix because of "+ b2);
			    break;
			}
                        if ((hblist.get(xx) != null) && (yy == yy2) && hblist.get(xx).contains(xx2)){
                            System.out.println( bug +" no need to fix because of "+ b2);
                            break;
                        }
	
                        if ((hblist.get(xx) != null ) && (hblist.get(yy2) != null) && hblist.get(xx).contains(xx2) && hblist.get(yy2).contains(yy)){
                            System.out.println( bug +" no need to fix because of "+ b2);
                            break;
                        }

			if ((hblist.get(yy2) != null) && (yy  == xx2) && hblist.get(yy2).contains(xx)){
			    System.out.println("AV : (" + yy2+ ","+ xx + "," + xx2 + ")");
			    break;
			}
			if ((hblist.get(xx2) != null) && (xx == yy2) && hblist.get(yy).contains(xx2)){
			    System.out.println("AV : (" + yy+ ","+ xx2 + "," + xx + ")");
			    break;
			}
		    }
	  	    sum ++;
		}
		if (buglist.size() > sum ) continue;
		System.out.println("Find an order violation " + bug);
	    }else {
		int xx,yy,xx2,yy2,xx3,yy3,xx4,yy4;
		xx = bug.x;
		yy = bug.y;
		for (Bug bug2: buglist){
                    if (bug2.type ==3 ){
                        xx2 = bug2.x;
                        yy2 = bug2.y;
			for (Bug bug3 : buglist){
			    if (bug3.type != 3){	
	                        if (bug3.type ==1 ){
                                    xx3 = bug3.y;
         	                    yy3 = bug3.x;
                 	        }else{
                         	    xx3 = bug3.x;
	                            yy3 = bug3.y;
        	                }
			        for (Bug bug4 : buglist){
				    if (bug4.type != 3){
	                                if (bug4.type ==1 ){
	                                    xx4 = bug4.y;
        	                            yy4 = bug4.x;
                	                }else{
                        	            xx4 = bug4.x;
                                	    yy4 = bug4.y;
                                	}
				    if ( (hblist.get(xx) != null) && (hblist.get(yy) !=null) && hblist.get(xx).contains(xx2) && hblist.get(yy).contains(yy2) && (yy2 ==xx3) && (xx == yy3) && (xx4 == xx2) && (yy4 == yy)  ){
					System.out.println("AV : Region1 ("+xx+","+xx2+")"   + "Region2 ("+yy+"," + yy2+")");
				    }
				    }
			        }

			    }
			}
		    }
		}
	    }
	}
    }
   
    public static void main(String [] args){
	if (args.length < 1){
	    System.out.println("Please specify the frontend config directory!");
	    return ;
	}
	frontend ft = new frontend(args[0]+"/ftconfig");
	ft.dumpmedian();
	ft.loadbugandhb(); // load the bug details and happen before relation
	ft.work();	   // detect the bug
    }
}

