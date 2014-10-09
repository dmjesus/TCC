package model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


public class SysAspect extends SysClass {

	private HashMap<String, SysAdvice> advice = new HashMap<String,SysAdvice>();
	private HashMap<String, SysPointcut> pointcuts = new HashMap<String, SysPointcut>();

	/**
	 * Constructs an Aspect from the given class, removes the class of its owner and add the new aspect to its owner
	 * */
	public SysAspect(SysClass c){
		this(c.getName());
		//begin{remove the class and add the aspect to its owner} 
		SysElement e = c.getOwner();
		if(e instanceof SysClass && ((SysClass)e).contains(c)){
			((SysClass)e).remove(c);
			((SysClass)e).add(this);
		} else if(e instanceof SysPackage && ((SysPackage)e).contains(c)){
			((SysPackage)e).remove(c);
			((SysPackage)e).add(this);
		}
		//end{remove the class and add the aspect to its owner}


		this.setOwner(c.getOwner());//set the owner

		for(Iterator<SysField>i=c.getFields().iterator();i.hasNext();){
			SysField f = i.next();
			this.add(f);
			c.remove(f);
		}

		for(Iterator<SysMethod>i=c.getMethods().iterator();i.hasNext();){
			SysMethod m = i.next();
			this.add(m);
			c.remove(m);
		}
		for(Iterator<SysClass>i= c.getInnerClasses().iterator();i.hasNext();){
			SysClass sc = i.next();
			this.add(sc);
			c.remove(sc);
		}

		this.setIsAnalysed(c.isAnalysed());
		this.setSuperClass(c.getSuperClass());
	}

	public SysAspect(String name) {
		super(name);
	}

	/**add the given advice*/
	public void add(SysAdvice ad) {
		if(!this.contains(ad)) {
			this.advice.put(ad.getName(), ad);
			System.out.println("[SysAspect]: add advice -> name: "+ad.getName()+"toString()"+ad);
			ad.setOwner(this);
		}
	}


	/**@return true if this aspect contains an advice with same name*/
	public boolean contains(SysAdvice ad) {
		return this.advice.containsKey(ad.getName());
	}

	/**add a pointcut*/
	public void add(SysPointcut p){
		this.pointcuts.put(p.getName(), p);
	}

	/**@return true if this aspect contains a pointcut with same name*/
	public boolean contains(SysPointcut p){
		return pointcuts.containsKey(p);
	}

	public HashSet<SysAdvice> getAdvice() {
		return new HashSet<SysAdvice>(this.advice.values());
	}

	/**get a new class with same name and owner, but not add this class to the owner*/
	public SysElement partialClone(){
		SysAspect c = new SysAspect(this.getName());
		c.setOwner(this.getOwner());
		return c;

	}

	/**add a method*/
	public void add(SysMethod method){
		if(method instanceof SysAdvice){
			this.add((SysAdvice)method);
		}
		if(method.getName().contains("ajc$")){
			this.add(new SysAdvice(method));
		} else {
			super.add(method);
		}
	}

	/**@return an element that has the same name as it is asked*/
	public SysElement get(String thisName, String sig, boolean isLast){
		if(thisName==null || thisName.equals("")) return null;
		SysElement e = super.get(thisName, sig, isLast);
		if(thisName.contains(" ")){
			thisName=thisName.substring(0,thisName.indexOf(" "));
		}

		if(e==null){//try on pieces of advice
			if(sig==null) sig = "";
			e = this.advice.get(thisName);
		}
		if(e == null){
			e = this.pointcuts.get(thisName);
		}
		return e;
	}

	/**@return the max element it can found given the path and signature*/
	public SysElement getMax(String called, String sig){
		SysElement e = super.getMax(called, sig);
		if(called.contains(" ")){
			called = called.substring(0,called.indexOf(" ")); //gets just the fully qualified name
		}
		if(e==this){
			e = this.advice.get(called);
			System.out.println("SysAspect:getMax: called: "+called);
			if(e==null) e=this;
		}
		return e;
	}

	/**get all child elements, i.e., methods, pieces of advice,, attributes, etc*/
	public Set<IElement> getChildElements(){
		Set<IElement> e = super.getChildElements();
		e.addAll(this.advice.values());
		e.addAll(this.pointcuts.values());
		return e;
	}


	public String viewState(){
		String state = super.viewState();
		String aux = this.getAdvice().toString();
		state+="\nPieces of advice:"+(aux.contains(",")?aux.replaceAll(",", "\n\t"):aux);
		aux = this.pointcuts.toString();
		state+="\nPointcuts:"+(aux.contains(",")?aux.replaceAll(",", "\n\t"):aux);
		return state;
	}


}
