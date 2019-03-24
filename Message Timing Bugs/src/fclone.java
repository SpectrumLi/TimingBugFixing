package uchicago.dfix;

import java.io.*;
import java.util.*;

import com.github.javaparser.*;
import com.github.javaparser.JavaParser.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.visitor.*;
import com.github.javaparser.ast.body.*;



public class fclone{
    public static MethodDeclaration md;
    public static void main(String[] args){
	
	if (args.length < 1){
	    System.out.println("please specify the input directory");
	}
	try{
	    FileInputStream fis = new FileInputStream(args[0]);        
            CompilationUnit cu = JavaParser.parse(fis);
	    ClassOrInterfaceDeclaration tc = cu.getClassByName("foo2").get();
	    FileInputStream fis2 = new FileInputStream("/home/cstjygpl/DFix/software/mr-4637/src/StateMachineFactory.java");
	    CompilationUnit cu2 = JavaParser.parse(fis2);
	    ClassOrInterfaceDeclaration tc2 = cu2.getClassByName("StateMachineFactory").get();

	    NodeList<BodyDeclaration<?>> types =tc2.getMembers();
            for (BodyDeclaration<?> type : types) {
		if ( type instanceof ClassOrInterfaceDeclaration){
		    ClassOrInterfaceDeclaration cid = (ClassOrInterfaceDeclaration) type;
		    System.out.println(cid.getClass().toString() + " " + cid.getName().toString());
		    
		}
            }

	    fclone f = new fclone();
	    new MethodVisitor().visit(cu, null);
	    md.setName(md.getName().toString()+"_dfix");
	    
	    
	    FieldDeclaration fd = (FieldDeclaration) JavaParser.parseClassBodyDeclaration("public static HashMap<Integer, String > hm_dfix = new HashMap<Integer,String>();");
	    tc.addMember(fd);
	    tc.addMember(md);
	    //System.out.println(tc);
	    try {	
 	        //System.out.println(cu.getPackageDeclaration().get());
	    } catch (Exception e){
		e.printStackTrace();
	    }
	    //System.out.println(cu.getImports());
	    //System.out.println(cu.toString());
	}catch (Exception e){
	    e.printStackTrace();
	}
    }
   

    private static class MethodVisitor extends VoidVisitorAdapter<Void> {
        public void visit(MethodDeclaration n, Void f) {
            /* here you can access the attributes of the method.
             this method will be called for all methods in this 
             CompilationUnit, including inner class methods */
	    if (n.getName().toString().equals( "doIt"))
		md = n.clone();
            //System.out.println(n.getBody());
            //n.addParameter("int", "value");
            super.visit(n, f);
        }
    } 
}
