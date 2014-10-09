package model;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;

import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

/**remember, VERY IMPORTANT.: A piece of advice never has the same name of another one. */
public class SysAdvice extends SysMethod{
	
	private static final String[] kinds = {"$after$", "$afterReturning", "$afterThrowing",
		"$before", "$around", "$interMethod", "$interMethodDispatch"};
	private static final int unknown = kinds.length;
	private static final int AFTER = 0;
	private static final int AFTER_RETURNNING = 1; 
	private static final int AFTER_THROWING = 2;
	private static final int BEFORE = 3;
	private static final int AROUND = 4;
	private static final int INTER_METHOD = 5; 
	private static final int INTER_METHOD_DISPATCH = 6;
	
	
	private int adviceType=unknown;
	private String value="";
	
	
	private HashMap<String, SysMethod> affects = new HashMap<String,SysMethod>();
	
	
	public SysAdvice(java.lang.reflect.Method m){
		super(m);
		Annotation[] ann = m.getDeclaredAnnotations();
		if(ann!=null){
			for(int i=0;i<ann.length;i++){
				System.out.println("[SysAdvice]:ann[i] = "+ann[i]);
				if(ann[i] instanceof Pointcut){
					this.value = ((Pointcut)ann[i]).value();
				} else {
					String value = annotationValue(ann[i]);
					if(!value.equals("")){
						this.value = value;
					}
				}
			}
		}
		System.out.println("[SysAdvice]: this.value = "+this.value);
	}
	
	
	public SysAdvice(boolean isStatic, String name, String returnType, String visibility) {
	  super(isStatic, name, returnType, visibility);
  }
	
	public SysAdvice(SysMethod method){
		super(method.isAnalysed(),method.getName(),method.getReturnType(),method.getVisibility());
		this.setOwner(method.getOwner());
	}
	
	public boolean equals(SysElement e){
		boolean equals = false;
		if(e instanceof SysAdvice){
			equals=true;
			if(!this.getName().equals(e.getName())) equals=false;
		}
		return equals;
	}
	
	public void addAffected(SysMethod m){
		this.affects.put(m.getFullyQualifiedName()+" "+m.getSignature(),m);
	}
	
	public HashSet<SysMethod> getAffecteds(){
		return new HashSet<SysMethod>(this.affects.values());
	}
	
	public String getAdviceType(){
		if(adviceType==unknown){
			makeAdvice();
		}
		
		switch (this.adviceType) {
		case AFTER: return "after";
		case AFTER_RETURNNING: return "after_r";
		case AFTER_THROWING: return "after_t";
		case BEFORE: return "before";
		case AROUND: return "around";
		case INTER_METHOD: return "inter_method";
		case INTER_METHOD_DISPATCH: return "inter_method_dispatch";
		default: return "unknow";
		}
	}

	public boolean affects(SysMethod m) {
	  return this.affects.containsKey(m.getFullyQualifiedName()+" "+m.getSignature());
  }

	public void makeAdvice(){
		for(int i=0 ;i<kinds.length;i++){
			if(this.getName().contains(kinds[i])){
				this.adviceType=i;
				
			}
		}
	}
	
	/**@return super.toString()*/
	public String toString(){
		if(adviceType==unknown){
			makeAdvice();
		}
		return super.toString();
	}
	
	
	/**@return true if and only if a given method is actually an advice*/
	public static boolean isAdvice(SysMethod dependency) {
		if(dependency!=null && dependency.getName().contains("ajc$")) return true;
		IElement e=dependency;
		while(e!=null){
			e = e.getOwner();
			if(e instanceof SysAspect) return true;
		}
	  return false;
  }

	
	
	public String prettyName() {
		System.out.println("[SysAdvice]: this.value => "+this.value);
		String represent = (this.value.length()>15?this.value.substring(0,15)+"...":this.value);
	  return this.getAdviceType()+"("+ represent+")";
  }


	private static String annotationValue(Annotation ann) {
	    String value = "";
	    System.out.println("[SysAdvice]: annotationValue => " + ann.toString());
	    if (ann instanceof Before) {
	      value = ((Before) ann).value();
	    } else if (ann instanceof AfterReturning) {
	      AfterReturning ar = ((AfterReturning) ann);
	      value = ar.pointcut().substring(1, ar.pointcut().length() - 1) + ", "
	          + "ret=" + (ar.returning().equals("") ? "any" : ar.returning())
	          + ", " + "args=" + (ar.argNames().equals("") ? "any" : ar.argNames());
	    } else if (ann instanceof AfterThrowing) {
	      AfterThrowing at = ((AfterThrowing) ann);
	      value = at.pointcut().substring(1, at.pointcut().length() - 1) + ", "
	          + "ret=" + (at.throwing().equals("") ? "any" : at.throwing()) + ", "
	          + "args=" + (at.argNames().equals("") ? "any" : at.argNames());
	    } else if (ann instanceof After) {
	      value = ((After) ann).value();
	    } else if (ann instanceof Around) {
	      value = ((Around) ann).value();
	    }
	    System.out.println("[SysAdvice]: value => " + value);
	    return value;
	  }
	

}
