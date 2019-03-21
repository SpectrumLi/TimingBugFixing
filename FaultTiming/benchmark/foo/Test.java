// Java code to illustrate 
// // using label and continue 
// // instead of goto 
//
// // file name: Main.java 
 public class Test { 
     public static void main(String[] args) { 
	outer:
      	for (int _i = 0 ; _i < 2; _i ++){
    	    int j = 1;
	    if (j == 1) {
 	        System.out.println(" value = " + j*222); 
 		continue outer; 
 	    } 
 	}
     } // end of main() 
}

