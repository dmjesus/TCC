package model;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.bcel.classfile.Method;

import analysis.MethodAnalysis;
import analysis.SysAnalysis;

public class SysMethod implements SysElement {
	
	private String name;
	private String returnType;
	private String visibility;
	private boolean isStatic;
	private SysElement owner;
	private boolean isAnalysed;
	private Vector<String> parameters = new Vector<String>();

	private ArrayList<String> exceptions = new ArrayList<String>();
	private ArrayList<String> exceptionsCatched = new ArrayList<String>();
	private ArrayList<SysMethod> calls = new ArrayList<SysMethod>();
	private ArrayList<SysAdvice> affected = new ArrayList<SysAdvice>();
	
	private java.lang.reflect.Method method;

	/**@param SysAdvice the advice that affects this method*/
	public void addAffectedBy(SysAdvice a){
		affected.add(a);
	}


	/**@return a hash set containing the pieces of advice that affect this method*/
	public List<SysAdvice> getAffecters(){
		return this.affected;
	}


	/**constructor based on BCEL Method*/
	public SysMethod(Method m){
		this(m.isStatic(), m.getName(), m.getReturnType().toString(), SysAnalysis.getVisibility(m));
		this.addParameter(MethodAnalysis.analyseSignature(m.getSignature()));
	}

	/**simple constructor
	 * @param isStatic represents if the method is static or not
	 * @param name the name of the method
	 * @param returnType the return type
	 * @param visibility the visibility (default - friendly, public, protected and private)*/
	public SysMethod(boolean isStatic, String name, String returnType, String visibility) {
		this.isStatic = isStatic;
		this.name = name;
		this.returnType = returnType;
		this.visibility = visibility;
	}

	/**Constructor based on Java Method*/
	public SysMethod(java.lang.reflect.Method meth) {
		this(Modifier.isStatic(meth.getModifiers()),
				meth.getName(),
				meth.getReturnType().getCanonicalName(),
				SysAnalysis.getVisibility(meth.getModifiers()));
		Class<?>[] vet_class =  meth.getExceptionTypes();
		if(vet_class !=null && vet_class.length>0 ){
			for(int i =0;i<vet_class.length;i++){
				String s = vet_class[i].getCanonicalName();
				this.addException(s);
			}
		}
		vet_class = null;
		vet_class = meth.getParameterTypes();
		if(vet_class != null){
			for(int i=0;i<vet_class.length;i++){
				this.addParameter(vet_class[i].getCanonicalName());
			}
		}	
		this.method = meth;
	}

	/**@param s a hash set of called methods*/
	public void add(HashSet<SysMethod> s) {
		for(SysMethod a : s)
			calls.add(a);
	}

	/**@param e an exception declared to throw*/
	public void add(SysException e) {
		this.exceptions.add("L"+e.getFullyQualifiedName()+";");
	}

	/**@param sysMethod a called method*/
	public void add(SysMethod sysMethod) {
		this.calls.add(sysMethod); 
	}

	/**@param s a hash set of called methods*/
	public void addDependency(HashSet<SysMethod> s) {
		for(SysMethod a : s)
			this.calls.add(a);
	}

	/**@param sysMethod a called method*/
	public void addDependency(SysMethod sysMethod) {
		this.calls.add(sysMethod); 
	}

	/**@param exceptionsCalls exceptions declared to throw*/
	public void addException(List<String> exceptionsCalls) {
		for(String s : exceptionsCalls){
			if(!exceptions.contains(s)) exceptions.add(s);
		}
	}

	public void addException(String string) {
		this.exceptions.add("L"+string+";");
	}

	public void addException(SysException e) {
		this.exceptions.add("L"+e.getFullyQualifiedName()+";");
	}

	public void addParameter(Vector<String> param) {
		for(String s : param){
			this.addParameter(s);
		}
	}

	public void addParameter(String p) {
		if(p==null) return;
		boolean b = false;
		if(p.startsWith("[")) {
			b = true;
			p=p.substring(1);
		}
		if(p.startsWith("L")) p =p.substring(1);
		if(p.endsWith(";")) p =p.substring(0, p.length()-1);
		if(b)p="["+p;
		this.parameters.add(p);
	}

	public boolean dependsOn(String dependency) {
		for(SysMethod s : this.calls)
			if(s.equals(dependency)) 
				return true;
		return false;
	}

	public boolean dependsOn(SysClass dp) {
		for(SysMethod d : calls){
			if(d.getFullyQualifiedName().contains(dp.getFullyQualifiedName()))
				return true;
		}
		return false;
	}

	public boolean equals(SysElement m) {
		return this.equals((Object)m);
	}

	public SysElement get(String thisName, String sig, boolean isLast) {
		return null;
	}

	/**get all the called methods*/
	public ArrayList<SysMethod> getCalls(){
		return this.calls;
	}

	public Set<IElement> getChildElements(){
		return new HashSet<IElement>();
	}

	/**get all the exceptions declared to throw*/
	public ArrayList<String> getExceptions(){
		return this.exceptions;
	}

	public String getFullyQualifiedName() {
		if(this.owner!=null)return owner.getFullyQualifiedName()+"."+this.name;
		return this.name;
	}

	public String getName() {
		return this.name;
	}

	public IElement getOwner() {
		return owner;
	}

	public Vector<String> getParameters() {
		return this.parameters;
	}

	public String getReturnType() {
		return this.returnType;
	}

	public String getSignature() {
		String sig="";
		for(String s : this.parameters){
			boolean b = false;
			if(s.startsWith("[")){
				s = s.substring(1);
				b = true;
			}
			sig+="L"+(b?"[":"")+s+";";
		}
		if(this.parameters.size()==0) sig="";
		return "("+sig+")"+this.returnType;
	}

	public String getVisibility() {
		return this.visibility;
	}

	public boolean isAnalysed(){
		return this.isAnalysed;
	}

	public boolean isSimilar(SysMethod m) {
		if (this.isStatic != ((SysMethod) m).isStatic())
			return false;
		if (!(this.name.equals(m.getName())))
			return false; 
		if (!this.returnType.equals(((SysMethod) m).getReturnType()))
			return false; 
		return true;

	}

	public boolean isStatic() {
		return this.isStatic;
	}

	public boolean isTheSame(String methodAndSignature){ //"method1 (I)V" for example
		String name = methodAndSignature.substring(0, methodAndSignature.indexOf(" "));
		if(!this.getName().equals(name)) return false;
		//String parameters
		return true;
	}

	public boolean mayThrow(String fullyQualifiedName) {
		for(String s : exceptions)
			if(s.equals("L"+fullyQualifiedName+";")) return true;
		return false;
	}

	public boolean mayThrow(SysException e) {
		return mayThrow(e.getFullyQualifiedName());
	}

	public SysElement partialClone() {
		SysMethod m = new SysMethod(isStatic, name, returnType, visibility);
		m.setOwner(owner);
		return m;
	}

	public void setDependency(HashSet<SysMethod> s) {
		this.calls = new ArrayList<SysMethod>(s);
	}

	public void setIsAnalysed(boolean b) {
		this.isAnalysed=b;
	}

	public void setOwner(IElement owner) {
		this.owner = (SysElement) owner;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((method == null) ? 0 : method.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SysMethod other = (SysMethod) obj;
		if (method == null) {
			if (other.method != null)
				return false;
		} else if (!method.equals(other.method))
			return false;
		return this.getOwner().equals(other.getOwner());
	}


	public String toString(){
		return this.visibility + (this.isStatic?" static ":" ")+this.name+ " " + this.getSignature();
	}

	public boolean equalsParamList(List<String> l){
		Vector<String> hash = this.getParameters();
		int i = l.size();
		int j = hash.size();
		if(i==j){
			Iterator<String> iterator = l.iterator();
			while(iterator.hasNext()){
				String s = iterator.next();
				if(s.contains("/")) s = s.replaceAll("/", ".");
				if(s.endsWith(";")) s = s.replaceAll(";", "");
				if(s.endsWith("[]")){
					s=MethodAnalysis.fixSingleSignature(s.substring(0, s.indexOf("[]")));
					if(s.startsWith("L")) s = s.substring(1);
					s="["+s;
				} else {
					s=MethodAnalysis.fixSingleSignature(s);
					if(s.startsWith("L")) s = s.substring(1);
				}
				if(s.startsWith("[L")){
					s = s.replace("[L", "[");
				}
				boolean contains = false;
				for(String s1 : hash){
					if(s1.contains("/")) s1 = s1.replaceAll("/", ".");
					if(s1.endsWith(";")) s1 = s1.replaceAll(";", "");
					if(s1.endsWith("[]")){
						s1=MethodAnalysis.fixSingleSignature(s1.substring(0, s1.indexOf("[]")));
						if(s1.startsWith("L")) s1 = s1.substring(1);
						s1="["+s1;
					} else {
						s1=MethodAnalysis.fixSingleSignature(s1);
						if(s1.startsWith("L")) s1 = s1.substring(1);
					}
					if(s1.startsWith("[L")){
						s1 = s1.replace("[L", "[");
					}

					if(s1.contains(s) || s.contains(s1)) {
						contains=true;
						break;
					}
				}
				if(!contains) return false;
			}
			for(String s1 : hash){
				String s;
				boolean contains = false;
				for(iterator=l.iterator();iterator.hasNext();){
					s=iterator.next();
					if(s1.contains("/")) s1 = s1.replaceAll("/", ".");
					if(s1.endsWith(";")) s1 = s1.replaceAll(";", "");
					if(s1.endsWith("[]")){
						s1=MethodAnalysis.fixSingleSignature(s1.substring(0, s1.indexOf("[]")));
						if(s1.startsWith("L")) s1 = s1.substring(1);
						s1="["+s1;
					} else {
						s1=MethodAnalysis.fixSingleSignature(s1);
						if(s1.startsWith("L")) s1 = s1.substring(1);
					}
					if(s1.startsWith("[L")){
						s1 = s1.replace("[L", "[");
					}
					if(s.contains("/")) s = s.replaceAll("/", ".");
					if(s.endsWith(";")) s = s.replaceAll(";", "");
					if(s.endsWith("[]")){
						s=MethodAnalysis.fixSingleSignature(s.substring(0, s.indexOf("[]")));
						if(s.startsWith("L")) s = s.substring(1);
						s="["+s;
					} else {
						s=MethodAnalysis.fixSingleSignature(s);
						if(s.startsWith("L")) s = s.substring(1);
					}
					if(s.startsWith("[L")){
						s = s.replace("[L", "[");
					}
					if(s1.contains(s) || s.contains(s1)) {
						contains=true;
						break;
					}
				}
				if(!contains) return false;
			}
			return true;
		}
		return false;
	}

	public boolean equalsParamList(String sig){
		if(sig.contains("(")) sig = sig.substring(1);
		if(sig.contains(")")) sig = sig.substring(0,sig.indexOf(")"));
		List<String> l = new Vector<String>();
		String[] splited = sig.split(";");
		for(String s : splited){
			if(!s.equals(""))l.add(s+";");
		}
		return equalsParamList(l);
	}

	public SysElement getMax(String called, String sig) {
		if(this.equalsParamList(sig) && (called.equals("") || called.startsWith(name))) return this;
		return this.owner;
	}

	public boolean dependsOn(SysMethod calledMethod) {
		for(SysMethod m : calls){
			if(m.equals(calledMethod)) return true;
		}
		return false;
	}

	public void addChild(IElement e) {
		if(e instanceof SysException){
			add((SysException)e);
		} else{
			if(e instanceof SysMethod){
				add((SysMethod)e);
			}
		}
	}



	public void addCatchedException(List<String> exceptionsCalls) {
		for(String s : exceptionsCalls){
			this.addCatchedExceptions(s);
		}

	}

	public void addCatchedExceptions(String s) {
		if(!this.exceptionsCatched.contains(s)){
			this.exceptionsCatched.add(s);
		}

	}

	public String viewState() {
		String state = "";
		state+="Fully qualified name: "+this.getFullyQualifiedName();
		state+="\nSignature: "+ this.getSignature();
		String aux = this.getExceptions().toString();
		state+="\nExceptions declared to throw: "+(aux.contains(",")?aux.replaceAll(",", "\n\t"):aux);
		aux = this.exceptionsCatched.toString();
		state+="\nExceptions handled: "+(aux.contains(",")?aux.replaceAll(",", "\n\t"):aux);
		aux = this.getCalls().toString();
		state+="\nCalls: "+(aux.contains(",")?aux.replaceAll(",", "\n\t"):aux);
		aux = this.getAffecters().toString();
		state+="\nAffected by: "+(aux.contains(",")?aux.replaceAll(",", "\n\t"):aux);
		return state;
	}

	/**get all the called classes that contains exception in name*/
	public ArrayList<String> getCatchedExceptions(){
		return this.exceptionsCatched;
	}


	public void addException(Class<?>[] exceptions2) {
		for(Class<?> c : exceptions2){
			this.addException(c.getCanonicalName());
		}

	}


	public java.lang.reflect.Method getMethod() {
		// TODO Auto-generated method stub
		return this.method;
	}

}
