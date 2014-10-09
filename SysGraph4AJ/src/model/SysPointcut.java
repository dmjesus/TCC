package model;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.aspectj.lang.annotation.Pointcut;


public class SysPointcut implements SysElement {
	private String name;
	private SysAdvice owner;
	private String expression;
	

	public SysPointcut(String name, String expression) {
		this.name=name;
		this.expression = expression;
	}


	public SysPointcut(Method meth) {
		Pointcut p = null;
		p = meth.getAnnotation(Pointcut.class);
		if(p !=null ){
			this.expression = p.value();
			this.name = meth.getName();
			//fix the name
			if(name.contains("ajc$pointcut$$")){
				name = name.substring(14);
				if(name.contains("$")){
					name = name.substring(0,name.indexOf("$"));
				}
			}
		} else {
			this.name = meth.getName();
			this.expression = "unknown";
		}
	}



	
	public String getName() {
		return this.name;
	}

	
	public IElement getOwner() {
		return this.owner;
	}

	
	public void setOwner(IElement e) {
		if(e instanceof SysAdvice){
			this.owner=(SysAdvice)e;
		}
	}

	
	public String getFullyQualifiedName() {
		return ((SysElement)getOwner()).getFullyQualifiedName()+"."+getName();
	}

	
	public boolean equals(SysElement e) {
		if(e instanceof SysPointcut){
			SysPointcut p = (SysPointcut)e; 
			return (p.getName().equals(this.getName()) && p.getExpression().equals(this.getExpression()));
		}
		return false;
	}

	/**gets the expression for this pointcut*/
	public String getExpression() {
		return this.expression;
	}

	/**sets this pointcut expression*/
	public void setExpression(String expression){
		this.expression=expression;
	}


	
	public SysElement partialClone() {
		SysPointcut p = new SysPointcut(this.name,this.expression);
		p.setOwner(this.getOwner());
		return p;
	}

	
	public Set<IElement> getChildElements() {
		return new HashSet<IElement>();
	}

	
	public SysElement get(String thisName, String sig, boolean isLast) {
		return null;
	}

	
	public SysElement getMax(String called, String sig) {
		return null;
	}

	
	public void addChild(IElement e) {
		return;

	}

	public String prettyName(){
		String ret = this.name + "(";
		if(expression.length()>15){
			ret+=getExpression().substring(0,15);
		} else {
			ret+=getExpression();
		}
		return ret+"...)";
	}
	
	public String toString(){
		return this.name + "(" + getExpression()+")";
	}


	
  public String viewState() {
	  String state = "";
	  state+="Fully qualified name: "+this.getFullyQualifiedName();
	  state+="\nPretty name: "+this.prettyName();
	  state+="\nExpression: "+this.getExpression();
	  return state;
  }

}
