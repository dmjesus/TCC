package model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

public class SysRoot implements SysElement {
	
	private String pathToBin;
	private HashMap<String, SysPackage> packages = new HashMap<String, SysPackage>();

	public SysRoot(){
		//default constructor
	}

	public SysRoot(String p){
		pathToBin=p;
	}

	public void add(HashSet<SysPackage> hp){
		for(SysPackage e : hp)
			this.packages.put(e.getName(), e);

	}

	public void add(SysPackage p){
		if(!this.contains(p)){
			this.packages.put(p.getName(),p);
		}
		else{
			SysPackage tp = this.getEquals(p);
			for(SysPackage p1 : p.getPackages()){
				tp.add(p1);
			}
			for(SysClass c1 : p.getClasses()){
				tp.add(c1);
			}
		}
	}

	public SysPackage getEquals(SysPackage p) {
		if(p!=null){
			for(SysPackage p1 : this.getPackages()){
				if(p1.equals(p)) return p1;
			}
			String fully = p.getFullyQualifiedName();
			String first = fully.substring(0,fully.indexOf("."));
			for(SysPackage p1 : this.getPackages()){
				if(p1.getName().equals(first)) return p1.getEquals(p);
			}
		}
		return null;
	}

	public boolean contains(SysPackage p) {
		return packages.containsKey(p.getName());
	}

	public boolean equals(SysRoot r){
		if(r==null) return false;
		if(this.packages.size()==r.getPackages().size()) {
			boolean contains = false;
			for(SysPackage rootP :packages.values()){
				contains = false;
				for(SysPackage rP:r.getPackages()){
					if(rootP.equals(rP)) {
						contains=true;
						break;
					}
				}
				if(contains==false) return false;
			}
			return true;
		}
		return false;
	}

	public HashSet<SysPackage> getPackages(){
		return new HashSet<SysPackage>(this.packages.values());
	}

	public String getPath(){
		return this.pathToBin;
	}

	public SysMethod getMethodFromString(String fully, String sig){
		if(fully.contains("\t")) fully = fully.substring(0,fully.indexOf("\t"));
		StringTokenizer tokenizer = new StringTokenizer(fully, ".");
		String justParameters=sig;
		if(sig.startsWith("\t")) sig=sig.replace("\t", "");
		if(sig.contains("(")) justParameters=sig.substring(sig.indexOf("(")+1);
		if(justParameters.contains(")")) justParameters=justParameters.substring(0,justParameters.indexOf(")"));
		SysElement e=this;
		while(tokenizer.hasMoreTokens() && e!=null){
			String thisName = tokenizer.nextToken();
			e=e.get(thisName, justParameters, !tokenizer.hasMoreTokens());
			//if(!tokenizer.hasMoreTokens()) break;
		}

		if(e==null){//try at default package...
			for(SysPackage p : this.getPackages()){
				if(p.getName().equals("(default package)")) {
					e=p;
					tokenizer = new StringTokenizer(fully, ".");
					while(tokenizer.hasMoreTokens() && e!=null){
						String thisName = tokenizer.nextToken();
						e=e.get(thisName, justParameters, !tokenizer.hasMoreTokens());
					}
				}
			}	  	
		}
		if(e instanceof SysMethod) return (SysMethod) e;
		return null;
	}

	public SysElement get(String thisName, String sig, boolean isLast) {
		return packages.get(thisName);
	}


	
	public String getName() {
		return null;
	}

	
	public SysElement getOwner() {
		return null;
	}

	
	public void setOwner(IElement e) {
		
	}

	
	public String getFullyQualifiedName() {
		return null;
	}

	
	public boolean equals(SysElement e) {
		return false;
	}

	
	public SysElement partialClone() {
		return null;
	}

	
	public Set<IElement> getChildElements() {
		return null;
	}

	
	public SysElement getMax(String called, String sig) {
		SysElement e = null;
		String key = called.substring(0, called.indexOf(".")); //the element we are looking for at the moment
		e = packages.get(key); //tries to get the element
		
		if(e == null){ //if not successful try in default package 
			e = packages.get("(default package)");  
		} else{ //if successful now look at next element
			called = called.substring(called.indexOf(".") + 1);
		}
		if(e!=null)
			e = e.getMax(called, sig);
		
		return e;
	}

  public void addChild(IElement e) {
		if(e instanceof SysPackage){
			add((SysPackage)e);
		}
  }

	
  public String viewState() {
	  // TODO Auto-generated method stub
	  return "";
  }



}
