package uchicago.dfix;
import java.io.*;
import java.util.*;

import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;

public class config{
    
    private ArrayList<String> jarpath;
    public AnalysisScope scope;
    public config(String st, File f){
	jarpath = new ArrayList<String>();
	try{
	    InputStream fis = new FileInputStream(st+"/jarscope");
	    InputStreamReader isr = new InputStreamReader(fis);
	    BufferedReader br = new BufferedReader(isr);
	    String s;
	    while ((s= br.readLine())!=null){
		jarpath.add(s);
	    }
	    
	    scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(jarpath.get(0),f);
	    for (int i=1; i<jarpath.size(); i++)
	   	scope.addToScope(AnalysisScopeReader.makeJavaBinaryAnalysisScope(jarpath.get(i), f));
	    
	}catch (Exception e){
	    e.printStackTrace();
	    System.out.println("No file exception");
	} 
    }
} 
