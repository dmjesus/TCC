package model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class SysClass implements SysElement {

	private HashSet<SysClass> interfaces = new HashSet<SysClass>();
	private HashMap<String, SysField> fields = new HashMap<String, SysField>();
	private HashMap<String, SysClass> innerClasses = new HashMap<String, SysClass>();
	private boolean isAnalysed = false;
	private HashMap<String, SysMethod> methods = new HashMap<String,SysMethod>();
	private String name;
	private SysElement owner;
	private SysClass superClass;

	/**create a single class*/
	public SysClass(String name) {
		this.name = name;
	}


	public void addChild(IElement e) {
		if(e instanceof SysClass){
			add((SysClass)e);
		} else {
			if(e instanceof SysMethod){
				add((SysMethod)e);
			} else{
				if(e instanceof SysField){
					add((SysField)e);
				} else{
					if (e instanceof SysPointcut && this instanceof SysAspect){
						SysAspect a = (SysAspect)this;
						a.add((SysPointcut)e);
					}
				}
			}
		}
	}

	/**add a Set of inner classes to this Class*/
	public void add(HashSet<SysClass> e) {
		for(SysClass c : e){
			this.add(c);
			c.setOwner(this);
		}
	}

	/**Add a single class to it's inner classes*/
	public void add(SysClass e) {
		this.innerClasses.put(e.getName(), e);
		e.setOwner(this);
	}

	/**add a valid field*/
	public void add(SysField sysField) {
		if(!this.contains(sysField) && !sysField.getName().contains("ajc$")){
			fields.put(sysField.getName(), sysField);
			sysField.setOwner(this);
		}
	}

	/**add a method*/
	public void add(SysMethod sysMethod) {
		if(!this.contains(sysMethod)){
			methods.put(sysMethod.toString(), sysMethod);
			sysMethod.setOwner(this);
		}


	}

	/**add a set of fields*/
	public void addFields(HashSet<SysField> fields) {
		for(SysField f : fields){
			this.add(f);
			f.setOwner(this);
		}
	}

	/**add a set of methods*/
	public void addMethods(HashSet<SysMethod> methods) {
		for(SysMethod m : methods){
			this.add(m);
			m.setOwner(this);
		}
	}

	/**verify if there is a inner class equals the passed class*/
	public boolean contains(SysClass e) {
		return innerClasses.containsKey(e.getName());
	}

	/**verify if there is a filed equals the passed field*/
	public boolean contains(SysField sysField) {
		return this.fields.containsKey(sysField.getName());
	}

	/**verify if there is a method equals the passed method*/
	public boolean contains(SysMethod sysMethod) {
		return methods.containsKey(sysMethod.toString());
	}

	/**return true if the passed element has the same fully qualified name and if it's a class*/
	public boolean equals(SysElement d) { 
		if(d instanceof SysClass)
			return this.getFullyQualifiedName().equalsIgnoreCase(d.getFullyQualifiedName());
		return false;
	}

	/**return true if the passed class has the same name*/
	public boolean equalsIgnoreFullyQualifiedName(SysClass o) {
		return this.name.equalsIgnoreCase(o.getName());
	}

	/**get the element that is represented by the passed string*/
	public SysElement get(String thisName, String sig, boolean isLast) {
		if(isLast && thisName.contains(" ")) thisName=thisName.substring(0,thisName.indexOf(" "));
		if(isLast) {
			for(SysMethod m : this.getMethods()){
				if(m.getName().equals(thisName) && ((SysMethod)m).equalsParamList(sig)) return m;
			}
		}
		else{
			return innerClasses.get(thisName);
		}
		return null;
	}

	/**get all child elements, it's inner classes, fileds and methods*/
	public Set<IElement> getChildElements(){
		Set<IElement> hs = new HashSet<IElement>();
		hs.addAll(fields.values());
		hs.addAll(innerClasses.values());
		hs.addAll(methods.values());
		return hs;
	}

	/**get a HashSet containing the fields of this class*/
	public HashSet<SysField> getFields() {
		return new HashSet<SysField>(this.fields.values());
	}

	/**get this class fully qualified name*/
	public String getFullyQualifiedName() {
		if(owner!=null)return owner.getFullyQualifiedName()+"."+this.name;
		return name;
	}

	/**get a HashSet containing this class's inner classes*/
	public HashSet<SysClass> getInnerClasses() {
		return new HashSet<SysClass>(innerClasses.values());
	}

	/**get a HashSet containing this class's methods*/
	public HashSet<SysMethod> getMethods() {
		return new HashSet<SysMethod>(this.methods.values());
	}

	/**get the class name*/
	public String getName() {
		return name;
	}

	/**get the owner of this class, could be a package or a class*/
	public SysElement getOwner() {
		return owner;
	}
	
	/**if there's a super class, get it*/
	public SysClass getSuperClass() {
		return this.superClass;
	}

	/**return true if the class was already analysed*/
	public boolean isAnalysed() {
		return isAnalysed;
	}

	/**get a new class with same name and owner, but not add this class to the owner*/
	public SysElement partialClone(){
		SysClass c = new SysClass(this.name);
		c.owner=owner;
		return c;

	}

	/**sets the flag is analysed*/
	public void setIsAnalysed(boolean b){
		isAnalysed=b;
	}

	/**sets the owner of this SysClass*/
	public void setOwner(IElement e) {
		owner = (SysElement) e;
	}

	/**Sets the superClass*/
	public void setSuperClass(SysClass sc) {
		this.superClass = sc;
	}

	/**returns this name*/
	public String toString(){
		return this.name;
	}

	/**remove an inner class from this class*/
	public void remove(SysClass c) {
		if(contains(c)) innerClasses.remove(c.getName());
	}

	/**removes a field from this class*/
	public void remove(SysField f) {
		if(contains(f)) fields.remove(f.getName());	 
	}

	/**removes a Method from this class*/
	public void remove(SysMethod m) {
		if(contains(m)) methods.remove(m.getName());		  
	}

	/**gets the maximum element in the tree*/
	public SysElement getMax(String called, String sig) {
		assert(!called.equals(""));
		SysElement e = null;
		if(called.contains(".")){//we are looking for a inner class
			e = this.innerClasses.get(called.substring(0, called.indexOf(".")));
			if(e!=null){ //if this class contains an inner class with same name we must get the max element in it
				e = e.getMax(called.substring(called.indexOf(".")+1), sig);
				if(e!=null) return e; //if this class doesnt contains the required inner class or the inner class doesnt contains the required path, we will return in next "return this" statement
			}


		} else{  //we are looking for a Method
			e=this.get(called, sig, true);
			if(e!=null) return e;
		}

		return this;
	}


	/**add inner classes from java class Class*/
	public void addInnerClass(Class<?>[] innerClasses2) {
		for(Class<?> c : innerClasses2){
			this.add(new SysClass(c.getSimpleName()));
		}

	}


	public void setInterfaces(Class<?>[] interfaces) {
		for(Class<?> c : interfaces){
			this.addInterfaces(new SysClass(c.getCanonicalName()));
		}
	}


	public void addInterfaces(SysClass sysClass) {
		if(!this.interfaces.contains(sysClass))
			this.interfaces.add(sysClass);
	}

	/**Get a hashSet of the interfaces this class implements*/
	public HashSet<SysClass> getInterfaces(){
		return this.interfaces;
	}


	public String viewState() {
		String state = "";
		state+="Fully qualified name: "+this.getFullyQualifiedName();
		String aux = this.getInterfaces().toString();
		state+="\nInterfaces: "+ (aux.contains(",")?aux.replaceAll(",", "\n\t"):aux);
		aux =  this.getInnerClasses().toString();
		state+="\nInner classes: "+ (aux.contains(",")?aux.replaceAll(",", "\n\t"):aux);
		if(this.getSuperClass()!=null)
			state+="\nSuper Class: " + this.getSuperClass().getFullyQualifiedName();
		aux = this.getFields().toString();
		state+="\nFields: "+  (aux.contains(",")?aux.replaceAll(",", "\n\t"):aux);;
		aux = this.getMethods().toString();
		state+="\nMethods: "+  (aux.contains(",")?aux.replaceAll(",", "\n\t"):aux);
		return state;
	}


}
