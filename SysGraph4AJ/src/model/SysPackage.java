package model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import analysis.MethodAnalysis;

public class SysPackage implements SysElement{

	private String name;
	private HashMap<String, SysClass> classes = new HashMap<String, SysClass>();
	private HashMap<String, SysPackage> packages = new HashMap<String, SysPackage>();
	private SysPackage owner;
	private boolean isAnalysed = false;
	private HashMap<String, SysAspect> aspects = new HashMap<String, SysAspect>();



	public SysPackage(String name){
		this.name=name;
	}

	public void add(SysAspect aspect) {
		if (!this.contains(aspect)) {
			this.aspects.put(aspect.getName(),aspect);
			aspect.setOwner(this);
		}
	}

	public void add(SysClass c) {
		if(!this.contains(c)){
			//this.getClasses().add(c);
			this.classes.put(c.getName(), c);
			c.setOwner(this);
		}else{
			SysClass thisinner=null;
			/*find c in this package*/

			thisinner=this.classes.get(c.getName());
			if(thisinner!=null) thisinner=this.aspects.get(c.getName());
			//			for(SysClass inner : this.getClasses()){
			//				if(inner.getName().equals(c.getName())){
			//					thisinner=inner;
			//					break;
			//				}
			//			}

			//			for(SysAspect a : this.getAspects()){
			//				if(a.getName().equals(c.getName())){
			//					thisinner=a;
			//					break;
			//				}
			//			}
			if(thisinner != null){
				for(SysClass inner : c.getInnerClasses()){
					thisinner.add(inner);
				}
				for(SysMethod m : c.getMethods()){
					thisinner.add(m);
				}
				for(SysField f : c.getFields()){
					thisinner.add(f);
				}
			}
		}
	}

	public void add(SysPackage inner) {
		if(!this.contains(inner)){
			//			this.getPackages().add(inner);
			this.packages.put(inner.getName(), inner);
			inner.setOwner(this);
		}else{
			SysPackage thisinner=null;
			/*find inner package into this package*/

			thisinner=packages.get(inner.getName());
			//			for(SysPackage p : this.getPackages()){
			//				if(p.getName().equals(inner.getName())){
			//					thisinner=p;
			//					break;
			//				}
			//			}
			for(SysPackage p : inner.getPackages()){
				thisinner.add(p);
			}
			for(SysClass c : inner.getClasses()){
				thisinner.add(c);
			}
		}
	}

	public boolean contains(SysClass c) {
		//		Iterator<SysClass> i = classes.values().iterator();
		//		while(i.hasNext()) 
		//			if (i.next().equals(c)) 
		//				return true;
		//
		//		Iterator<SysAspect> ia = this.getAspects().iterator(); //cannot contain an aspect with same name too
		//		while(ia.hasNext()) 
		//			if (ia.next().getName().equals(c.getName())) 
		//				return true;
		//		return false;
		return classes.containsKey(c.getName());
	}

	public boolean contains(SysAspect c) {
		//		Iterator<SysAspect> i = this.getAspects().iterator();
		//		while(i.hasNext()) 
		//			if (i.next().getName().equals(c.getName())) 
		//				return true;
		//		return false;
		return aspects.containsKey(c.getName());
	}

	public boolean contains(SysPackage inner) {
		if(inner==null) return false;
		return this.packages.containsKey(inner.getName());
		//		for(SysPackage p : packages)
		//			if(p.equalsIgnoreFullyQualifiedName(inner)) 
		//				return true;
		//				return false;
	}

	public boolean equals(SysElement p){
		if(p==null) return false;
		return this.getFullyQualifiedName().equalsIgnoreCase(p.getFullyQualifiedName());

	}

	public boolean equalsIgnoreFullyQualifiedName(SysPackage p){
		if(p==null) return false;
		return this.name.equalsIgnoreCase(p.getName());
	}

	public HashSet<SysClass> getClasses() {
		return new HashSet<SysClass>(this.classes.values());
	}

	public String getFullyQualifiedName() {
		if(owner!=null)return owner.getFullyQualifiedName()+"."+name;
		return name;
	}

	public String getName(){
		return this.name;
	}

	public SysElement getOwner() {
		return this.owner;
	}

	public HashSet<SysPackage> getPackages() {
		return new HashSet<SysPackage>(this.packages.values());
	}

	public boolean isAnalysed(){
		return isAnalysed;
	}

	public void setIsAnalysed(boolean b) {
		this.isAnalysed = b;
	}

	public void setOwner(IElement q) {
		this.owner=(SysPackage)q;
	}

	public String toString(){
		return this.getFullyQualifiedName();
	}

	public SysElement partialClone() {
		SysPackage p = new SysPackage(name);
		p.setOwner(owner);
		return p;
	}

	public Set<IElement> getChildElements(){
		HashSet<IElement> hs = new HashSet<IElement>();
		for(SysElement e : packages.values()){
			hs.add(e);
		}
		for(SysElement e : classes.values()){
			hs.add(e);
		}
		for(SysElement e : aspects.values()){
			hs.add(e);
		}
		return hs;
	}

	public SysElement getEquals(String toFind){
		SysElement e = this.packages.get(toFind);
		if(e==null){
			e = this.aspects.get(toFind);
			if(e==null){
				e = this.classes.get(toFind);
			}
		}
		return e;
	}
	
	public SysPackage getEquals(SysPackage p) {
		if(p!=null){
			if(this.equals(p)) return this;
			String fully = p.getFullyQualifiedName();
			fully = MethodAnalysis.getFixedString(fully, this);
			String first;
			if(fully.contains("."))
				first = fully.substring(0, fully.indexOf("."));
			first = fully;
			//			for(SysPackage p1:this.getPackages()){
			//				if(p1.getName().equals(first)) return p1.getEquals(p);

			//			}
			return this.packages.get(first).getEquals(p);
		}
		return null;
	}

	public SysElement get(String thisName, String sig, boolean isLast){
		if(isLast) return null;
		if(this.packages.containsKey(thisName)) return this.packages.get(thisName);
		if(this.aspects.containsKey(thisName)) return this.aspects.get(thisName);
		if(this.classes.containsKey(thisName)) return this.classes.get(thisName);
		return null;
	}

	public HashSet<SysAspect> getAspects() {
		return new HashSet<SysAspect>(this.aspects.values());
	}

	public void remove(SysClass c) {
		if(contains(c))classes.remove(c.getName());

	}

	
	public SysElement getMax(String called, String sig) {
		assert(called.contains("."));
		SysElement e = null;
		String key = called.substring(0, called.indexOf(".")); //the element we are looking for at the moment
	  //the following bit of source tries to get the next element into this package
		{
			e = packages.get(key); //tries to get the element as a package
			if(e == null){
				e = aspects.get(key); //tries to get the element as an aspect
			}
			if(e == null){
				e = classes.get(key); //tries to get the element as a class
			}
		}

		if(e != null){
			return e.getMax(called.substring(called.indexOf(".")+1), sig);
		}
		else{
			return this;
		}
	}

	
	public void addChild(IElement e) {
	  if(e instanceof SysPackage){
	  	add((SysPackage) e);
	  } else{
	  	if(e instanceof SysAspect){
		  	add((SysAspect) e);
		  } else{
		  	if(e instanceof SysClass){
			  	add((SysClass) e);
			  }
		  }
	  }
	  
  }

	
  public String viewState() {
	  String state = "";
	  state+="Fully qualified name: "+this.getFullyQualifiedName();
	  String aux = this.getPackages().toString();
	  state+="\nPackages in it: "+(aux.contains(",")?aux.replaceAll(",", "\n\t"):aux);
	  aux = this.getClasses().toString();
	  state+="\nClasses in it: "+(aux.contains(",")?aux.replaceAll(",", "\n\t"):aux);
	  aux = this.getAspects().toString();
	  state+="\nAspectss in it: "+(aux.contains(",")?aux.replaceAll(",", "\n\t"):aux);
	  return state;
  }
}