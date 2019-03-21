package uchicago.ffix;

import java.io.*;
import java.util.*;

public class srcloc{
    public String cname;
    public String mname;
    public String lnum;
    public srcloc(String cn, String mn, String ln){
	cname = cn;
	mname = mn;
	lnum  = ln;
    }
    public srcloc(String s){
	String[] ss = s.split(" ");
	cname = ss[0]; 
	mname = ss[1];
	lnum = ss[2];
    }
    public int getlinenum(){
	return Integer.parseInt(lnum);
    }
    public String toString(){
	return cname + " " + mname + " " + lnum;
    }
}
