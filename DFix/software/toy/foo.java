package com.toy;
import java.util.*;
import java.io.*;
class foo {
    int i;
    static int xi;
    public static void main(String args[]){
	foo.xi = 2;
        foo f = new foo ();
	f.i = 1;
        f.doIt();
	foo2 f2 = new foo2();
	f2.doIt();
	
    }
    
    void doIt() {
        try {
            System.out.println("-1-DOIT--"+ foo.xi+ this.i);
            }
            catch (Exception e){
                e.printStackTrace();
            }
    }
}
 class foo2 {
	public foo2()
	{
	}
    void doIt() {
        try {
            System.out.println("-2-DOIT--");
  //          Thread.sleep(3222);
            }
            catch (Exception e){
                e.printStackTrace();
            }
    }
   }
	

