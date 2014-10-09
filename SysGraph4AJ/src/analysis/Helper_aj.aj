package analysis;

import java.io.PrintStream;

public aspect Helper_aj {

	private boolean printInConsole = true;
//	private boolean printInConsole = false;
	
	pointcut output(String s, PrintStream p) : call ( * java.io.PrintStream.print*(String) ) && args(s) && target(p);

	void around(PrintStream p, String s): output(s,p){
		if(printInConsole){
			proceed(p,s);
		}
	}
}
