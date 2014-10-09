package analysis;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import model.SysElement;
import model.SysAdvice;
import model.SysAspect;
import model.SysClass;
import model.SysMethod;
import model.SysPackage;
import model.SysRoot;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.CodeException;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.Type;

/**
 * This class helps to analyse a method*/
public class MethodAnalysis {	

	/**This method analyses the given method and try to add its dependencies
	 * @param sm The method to be analysed
	 * @param root The SysRoot that contains the method
	 * */
	public static void analyseMethod(SysMethod sm, SysRoot root){
		System.out.println("[MethodAnalysis]: analysing method: " + sm);
		JavaClass jc = getClassFromMethod(sm, root); /* get BCEL JavaClass model */

		List<String> methodCalls = getMethodCallsFromMethodArray(sm, jc.getMethods()); /* find SysMethod in BCEL.JavaClass */

		System.out.println("[MethodAnalysis]: methodCalls: " + methodCalls);
		//for each call this method does, we need to find its called method
		for(String called : methodCalls){
			called = removeLineNumber(called);
			called = removeInvokeCommand(called);
			String sig = getSignature(called);
			System.out.println("[MethodAnalysis]: Looking for \"" + called + "\"");
			//assert(called.split(" ").length>=2);
			if(called.split(" ").length < 2) throw new RuntimeException("[MethodAnalysis] called's requirements dont match(missing return type)\n\t" + called);
			//assert(called.contains("."));
			if(!called.contains(".")) return;//BCEL error method call to { invoke* " (number) }
			if(called == null || called.equals("") || called.equals(" ")) continue;
			//now we have the called method fully qualified name and its signature.
			//a couple of good references to work with
			SysElement lastInModel = root.getMax(called, sig);
			System.out.println("[MethodAnalysis]: max: \"" + lastInModel + "\"");
			SysElement nextNotInModel = null;
			SysMethod calledMethod = null;


			if(lastInModel != null && lastInModel instanceof SysMethod){
				calledMethod = (SysMethod)lastInModel;
			}


			if(lastInModel != null && !(lastInModel instanceof SysMethod)){
				try{
					nextNotInModel = tryOption(lastInModel, getFixedString(called,lastInModel), sig.toString(), root);
					//remembering that tryOption never returns null, In such case as it doesnt find the required method it 
					//throws a PathNotFoundException
					if(!addElementToElement_notDependency(lastInModel,nextNotInModel)){
						System.err.println("[MethodAnalysis]: error when trying to add "+nextNotInModel.getName() +" to "+ lastInModel);
					}

					calledMethod = root.getMethodFromString(called, sig);
					System.out.println("[MethodAnalysis]: next: \"" + nextNotInModel + "\"");
					System.out.println("[MethodAnalysis]: called: \"" + calledMethod + "\"");

				} catch(PathNotFoundException pnfe){
					System.out.println("[MethodAnalysis]: path :"+called+" not found.");
				}
			}
			if(calledMethod != null) {
				addDependency(sm,calledMethod);
			} else {
				//bad... 
				System.out.println("[MethodAnalysis]: couldnt find \""+called+"\"");
			}
		}
		sm.setIsAnalysed(true);
	}

	/**
	 * Obtém as chamadas de método do alvo.
	 * 
	 * @param sysMethod
	 * 		método analisado inicialmente
	 * @param methodArray
	 * 		todos os métodos da classe desse {@link SysMethod}
	 * @return
	 * 		lista de chamadas de métodos
	 */
	private static List<String> getMethodCallsFromMethodArray(SysMethod sysMethod, Method[] methodArray) {
		SysMethod bcelMethod = null;
		List<String> methodCalls = null;

		for(Method method : methodArray){
			Type t = method.getReturnType();

			bcelMethod = new SysMethod(method.isStatic(), method.getName(), t.toString(), SysAnalysis.getVisibility(method));
			String sig = method.getSignature();
			sig = sig.substring(sig.indexOf("(")+1, sig.indexOf(")"));
			bcelMethod.addParameter(analyseSignature(sig));

			if(bcelMethod.isSimilar(sysMethod) && sysMethod.equalsParamList(sig)){
				
				List<String> exceptionsCatched = new ArrayList<String>();
				Code methodCode = method.getCode();
				
				if(methodCode == null) continue;
				
				methodCalls = MethodAnalysis.getMethodCalls(method.getCode().toString());
				CodeException[] exceptions = methodCode.getExceptionTable();
				
				for(int i = 0; i < exceptions.length; ++i){
					String a = exceptions[i].toString(methodCode.getConstantPool());
				
					for(int j = 0; j < 10; j++) {
						String numberAsString = Integer.toString(j);
						if(a.contains(numberAsString)) a = a.replaceAll(numberAsString, "");
					}
					
					String s = a.substring(0, a.indexOf("("));
					exceptionsCatched.add(s);
				}

				if(method.getExceptionTable()!=null){
					for (String s1 : method.getExceptionTable().getExceptionNames()){
						bcelMethod.addException(s1);
					}	
				}

				bcelMethod.addCatchedException(exceptionsCatched);
				break;
			}
		}

		/*add contents of BCELMethod in SysMethod sm*/
		if(bcelMethod != null) {
			sysMethod.addCatchedException(bcelMethod.getCatchedExceptions());
			sysMethod.addException(bcelMethod.getExceptions());
		}
		return methodCalls;
	}

	/**
	 * Carrega a classe do respectivo método.
	 * 
	 * @param sm
	 * 		método alvo
	 * @param root
	 * 		raiz da análise
	 * @return BCEL java class from {@link SysMethod}
	 */
	private static JavaClass getClassFromMethod(SysMethod sm, SysRoot root) {
		try {
			return Repository.lookupClass(sm.getMethod().getDeclaringClass());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}


	/**returns a HashSet contenting the calls that the given method does*/
	public static List<String> getMethodCalls(String methodCode){
		List<String> hs = new ArrayList<String>();
		String[] aux = methodCode.split("\n");
		for(int i=0;i<aux.length;i++){
			if(aux[i].contains("invokespecial") || aux[i].contains("invokevirtual") || aux[i].contains("invokestatic")){
				hs.add(aux[i].substring(aux[i].indexOf("invoke")));
			}
		}
		return hs;
	}


	/**
	 * Redirects to the right tryOption
	 */
	public static SysElement tryOption(SysElement m, String string, String signature, SysRoot sysRoot) throws PathNotFoundException {
		assert(m!=null);
		if(m instanceof SysAspect) return tryOption((SysAspect)m,string, signature, sysRoot);
		if(m instanceof SysPackage) return tryOption((SysPackage)m,string, signature, sysRoot);
		if(m instanceof SysClass) return tryOption((SysClass)m,string, signature, sysRoot);
		if(m instanceof SysMethod) return tryOption((SysMethod)m,string, signature, sysRoot);
		return null;
	}

	public static SysElement tryOption(SysAspect c, String pathToFind, String signature, SysRoot root) throws PathNotFoundException {
		if(pathToFind.equals("") || c.isAnalysed()) {
			System.err.println("[MethodAnalysis]: throwing exception because" +
					(pathToFind.equals("")?" pathToFind==nothing": c.getName()+" is analysed.")+ "\n\tClass name:"+c.getName()+"\n\tPathToFind="+pathToFind);
			throw new PathNotFoundException();
		}
		if(pathToFind.contains(" ")) pathToFind = pathToFind.substring(0,pathToFind.indexOf(" "));
		if(pathToFind.contains("\t")) pathToFind = pathToFind.substring(0,pathToFind.indexOf("\t"));
		if(signature.contains("(")) signature = signature.substring(signature.indexOf("(")+1);
		if(signature.contains(")")) signature = signature.substring(0,signature.indexOf(")"));
		// there's no method to find 
		// se o metodo estivesse dentro desta classe, seria achado nas alternativas anteriores a esta

		int dotPosition = pathToFind.indexOf(".");
		SysClass cobaia =(SysAspect) c.partialClone();


		//here, when the system discover that actually cobaia is an aspect, it removes the class and add the aspect.
		cobaia = ClassAnalysis2.analyseClass(cobaia, root);

		if(dotPosition==-1){
			SysElement e1 = cobaia.getMax(pathToFind, signature);
			if(e1 instanceof SysMethod || e1 instanceof SysAdvice){
				e1.setOwner(c);
				return e1;
			}
			//if class does not have the method, we failed in find
			System.err.println("[MethodAnalysis]: throwing exception because cobaia doesnt have a method with same name"+
					"\n\tlooking into: "+cobaia.getName()+"\n\tfor Method: "+pathToFind+" "+signature);
			throw new PathNotFoundException();
		} else {
			//find InnerClass
			String nextClass = pathToFind.substring(0, dotPosition);
			for(SysClass inner : c.getInnerClasses()){
				if(inner.getName().equals(nextClass)){
					SysElement e = MethodAnalysis.tryOption(inner, pathToFind.substring(dotPosition+1),signature, root);
					SysElement aux = inner.getOwner().getMax(inner.getName(), signature);
					if (e instanceof SysAspect){
						inner = (SysAspect)aux;
					} else if (e instanceof SysClass){
						inner = (SysClass)e;
					}
					if(e instanceof SysClass || e instanceof SysAspect){
						inner.addChild(e);
					}else{ 
						if(e instanceof SysMethod || e instanceof SysAdvice){
							inner.addChild(e);
						}
					}
					inner.setOwner(c);
					return inner;          
				}
			}
			System.err.println("[MethodAnalysis]: throwing exception because cobaia doesnt have a method with same name"+
					"\n\tlooking into: "+cobaia.getName()+"\n\tfor Method: "+pathToFind+" "+signature);
			throw new PathNotFoundException();  
		}

	}

	/**
	 * Try to find the path into the method, in this case, just checks the signature
	 * Returns null if the given method is the method you are looking for.
	 */
	public static SysElement tryOption(SysMethod m, String string, String signature, SysRoot sysRoot) throws PathNotFoundException {
		//String sig = m.getSignature().substring(1,m.getSignature().lastIndexOf(")"));
		if(string.equals("") /*&& m.getSignature().equals(signature)*/) return m;
		throw new PathNotFoundException();

	}

	/**
	 * Tries to find the path into the given class, case success it returns the
	 */
	public static SysElement tryOption(SysClass c, String pathToFind,String signature, SysRoot r) throws PathNotFoundException {
		if(pathToFind.equals("") || c.isAnalysed()) {
			System.err.println("[MethodAnalysis]: throwing exception because" +
					(pathToFind.equals("")?" pathToFind==nothing": c.getName()+" is analysed.")+ "\n\tClass name:"+c.getName()+"\n\tPathToFind="+pathToFind);
			throw new PathNotFoundException();
		}
		if(pathToFind.contains(" ")) pathToFind = pathToFind.substring(0,pathToFind.indexOf(" "));
		if(pathToFind.contains("\t")) pathToFind = pathToFind.substring(0,pathToFind.indexOf("\t"));
		if(signature.contains("(")) signature = signature.substring(signature.indexOf("(")+1);
		if(signature.contains(")")) signature = signature.substring(0,signature.indexOf(")"));
		// there's no method to find 
		// se o metodo estivesse dentro desta classe, seria achado nas alternativas anteriores a esta

		int dotPosition = pathToFind.indexOf(".");
		SysClass cobaia =(SysClass) c.partialClone();

		//here, when the system discover that actually cobaia is an aspect, it removes the class and add the aspect.
		cobaia = ClassAnalysis2.analyseClass(cobaia, r);

		if(dotPosition==-1){
			SysElement e1 = cobaia.getMax(pathToFind, signature);
			if(e1 instanceof SysMethod){
				e1.setOwner(c);
				return e1;
			}
			//find method
			//if class does not have the method, we failed in find
			System.err.println("[MethodAnalysis]: throwing exception because cobaia doesnt have a method with same name"+
					"\n\tlooking into: "+cobaia.getName()+"\n\tfor Method: "+pathToFind+" "+signature);
			throw new PathNotFoundException();
		} else {
			//find InnerClass
			String nextClass = pathToFind.substring(0, dotPosition);
			for(SysClass inner : c.getInnerClasses()){
				if(inner.getName().equals(nextClass)){
					SysElement e = MethodAnalysis.tryOption(inner, pathToFind.substring(dotPosition+1),signature, r);
					inner = (SysClass)inner.getOwner().getMax(inner.getName(), "");
					if(e instanceof SysClass){
						inner.add((SysClass)e);
					}else{ 
						if(e instanceof SysMethod){
							inner.add((SysMethod)e);
						}
					}
					inner.setOwner(c);
					return inner;          
				}
			}
			System.err.println("[MethodAnalysis]: throwing exception because cobaia doesnt have a method with same name"+
					"\n\tlooking into: "+cobaia.getName()+"\n\tfor Method: "+pathToFind+" "+signature);
			throw new PathNotFoundException();  
		}
	}

	/**
	 * Tries to find the path into the given SysPackage, if success return the next level of the hierarchy
	 * For example, if beginning from this package there are more packages to descend, returns the next package.
	 * And if beginning from this package there are only classes until the required method, returns the next class.
	 */
	public static SysElement tryOption(SysPackage p1, String pathToFind,String signature, SysRoot r) throws PathNotFoundException{
		String path = r.getPath() + File.separator +
				p1.getFullyQualifiedName().replace(".",File.separator);
		int dotPosition = pathToFind.indexOf(".");
		if(dotPosition == -1){
			System.err.println("MethodAnalysis: we are throwing an exception because we are looking for a method, " +
					"and this method does not find methods, just classes and packages" +
					"\n\tlooking into: " + p1.getName() + "\n\tfor: " + pathToFind + " " + signature);
			throw new PathNotFoundException();
		}

		String f = pathToFind.substring(0,dotPosition);
		String nextPath = pathToFind.substring(dotPosition + 1);
		dotPosition = nextPath.indexOf(".");

		SysPackage cobaia = (SysPackage) p1.partialClone();
		SysAnalysis.analysePackage(cobaia, path, false);
		SysElement e=null;

		if(dotPosition != -1){
			SysElement e2 = cobaia.getMax(f + "." + nextPath, signature);
			if(e2 instanceof SysPackage){
				SysPackage p = (SysPackage) e2;
				try{
					e = MethodAnalysis.tryOption(p, pathToFind.substring(pathToFind.indexOf(".")+1), signature, r);
					if(e!=null){
						if(e instanceof SysAspect){
							p.add((SysAspect)e);
						} else if(e instanceof SysClass){
							p.add((SysClass)e);
						} else if (e instanceof SysPackage){
							p.add((SysPackage)e);
						}
						p.setOwner(p1);
						return p;
					}
				} catch (PathNotFoundException pnfe){
					/*do nothing, wait for SysClasses*/
				}
			}
		}

		SysMethod e1=null;
		SysClass c = null;
		SysElement e2 = cobaia.getMax(f + "." + pathToFind, signature);
		if(e2 instanceof SysClass){
			c = (SysClass) e2;
			c = (SysClass)c.partialClone();
			try{
				assert(c.getMethods().size()==0);
				e1=(SysMethod)MethodAnalysis.tryOption(c, pathToFind.substring(pathToFind.indexOf(".")+1),signature, r);
				c = (SysClass) c.getOwner().get(c.getName(), "", false);   
				// this line just checks if c is a class or it was changed to an aspect
				if(e1!=null) {
					c.add(e1);
				}
				c.setOwner(p1);
				return c;
			} catch(PathNotFoundException pnfe){
				if(e==null) throw pnfe;
			}
		}
		throw new PathNotFoundException();
	}


	/**
	 * Returns the corrected string to the element
	 * For example: if we pass sjc.unifesp.ict.dct.bcc.vespertino as string and 
	 * sjc.unifesp.ict.dct as a package the return is bcc.vespertino as string.
	 * 
	 */
	public static String getFixedString(String str, SysElement e){
		String corrected=str, e_name;
		e_name=e.getFullyQualifiedName();
		if(e_name.startsWith("(default package")){
			e_name = e_name.replace("(default package).", "");
		}
		if(str.startsWith(e_name+".")){
			corrected=str.replace(e_name+".", "");
		}
		if(str.startsWith(e_name+File.separator)){
			corrected=str.replace(e_name+File.separator, "");
		}
		if(corrected.startsWith(".")) corrected = corrected.substring(1);
		if(corrected.startsWith(" ") || corrected.startsWith("\t") || corrected.startsWith("(")) return "";
		return corrected;
	}

	/**
	 * Removes the 'invoke' declaration of a string representing the bytecode
	 */
	public static String removeInvokeCommand(String withInvoke){
		if(withInvoke==null) return null;
		if(withInvoke.contains("invokevirtual")) withInvoke = withInvoke.replace("invokevirtual", "");
		else if(withInvoke.contains("invokestatic")) withInvoke = withInvoke.replace("invokestatic", "");	
		else if(withInvoke.contains("invokespecial")) withInvoke = withInvoke.replace("invokespecial", "");
		if(withInvoke.startsWith("\t")) return withInvoke.substring(1);
		return withInvoke;
	}

	/**
	 * Returns the signature of a string representing a method
	 */
	public static String getSignature(String containsTheSig){
		String first;
		if(containsTheSig==null) return "";
		if(containsTheSig.contains(" ")){
			first = containsTheSig.substring(containsTheSig.indexOf(" ")+1);
			if(first.contains(" ")){
				first = first.substring(0, first.indexOf(" "));
				return first;
			}
			return first;
		}
		return null;
	}

	/**
	 * Removes the line number at the end of an string
	 */
	public static String removeLineNumber(String withLineNumber){
		if(withLineNumber==null) return null;
		String first;
		if(withLineNumber.contains(" ")){
			first = withLineNumber.substring(0, withLineNumber.lastIndexOf(" "));
			if(first.contains(" ")){
				return first;
			}
			return withLineNumber;
		}
		return null;
	}

	/**given the string representing the path to the method, this method returns the method that corresponds to the string*/
	public static SysMethod getRequiredMethod(SysPackage e, String str){
		if(!str.contains(".") || e==null){
			return null;
		}
		SysPackage option1=null;
		SysAspect option2=null;
		SysClass option3=null;
		String next = str.substring(0, str.indexOf("."));
		for(SysPackage p : e.getPackages()){
			if(p.getName().equals(next)){
				option1 = p;
				break;
			}
		}
		next=str.substring(str.indexOf("."));
		SysMethod m = getRequiredMethod(option1,next);
		if(m!=null) return m;
		for(SysAspect c : e.getAspects()){
			if(c.getName().equals(next)){
				option2=c;
				break;
			}
		}
		m = getRequiredMethod(option2,next);
		if(m!=null) return m;
		for(SysClass c : e.getClasses()){
			if(c.getName().equals(next)){
				option3=c;
				break;
			}
		}
		return getRequiredMethod(option3,next);
	}

	/**given the string representing the path to the method, this method returns the method that corresponds to the string*/
	public static SysMethod getRequiredMethod(SysClass e, String str) {
		if(e==null) return null;
		if(!str.contains(".")){
			if(str.contains(" ")){
				str=str.substring(0, str.indexOf(" "));
				for(SysMethod m : e.getMethods()){
					if(m.getName().equals(str))
						return m;
				}
			}
		}
		else{ //look into a inner class
			String next = str.substring(0, str.indexOf("."));
			for(SysClass inner : e.getInnerClasses()){
				if(inner.getName().equals(next)){
					return getRequiredMethod(inner, str.substring(str.indexOf(".")));
				}
			}
		}
		return null;
	}

	/**returns a HashSet that contains all the parameters of a method*/
	public static Vector<String> analyseSignature(String sig){
		Vector<String> a = new Vector<String>();
		if(sig.length()>0){
			String[] splited = sig.split(";");
			for(int i=0;i<splited.length;i++)
				a.add(splited[i]);
		}
		return a;

	}

	/**add an element to another element if possible.
	 * @param lastInModel last element in model
	 * @param nextNotInModel next element that is not in model
	 * @return true if succeeded and false if not*/
	public static boolean addElementToElement_notDependency(SysElement lastInModel, SysElement nextNotInModel){
		if(lastInModel==null || nextNotInModel==null) return false;
		if(nextNotInModel instanceof SysPackage){
			if(lastInModel instanceof SysPackage){
				((SysPackage)lastInModel).add((SysPackage)nextNotInModel);
			} else {
				//we cant add a SysPackage into a SysClass or SysMethod
				return false;
			}
		} else if(nextNotInModel instanceof SysAspect){
			if(lastInModel instanceof SysPackage){
				((SysPackage)lastInModel).add((SysAspect)nextNotInModel);
			} else if(lastInModel instanceof SysClass){
				((SysClass)lastInModel).add((SysAspect)nextNotInModel);
			} else {
				//we cant add a SysClass into a SysMetho
				return false;
			} 
		} else if(nextNotInModel instanceof SysClass){
			if(lastInModel instanceof SysPackage){
				((SysPackage)lastInModel).add((SysClass)nextNotInModel);
			} else if(lastInModel instanceof SysClass){
				((SysClass)lastInModel).add((SysClass)nextNotInModel);
			} else {
				//we cant add a SysClass into a SysMetho
				return false;
			}
		} else if(nextNotInModel instanceof SysMethod){
			if(lastInModel instanceof SysClass){
				((SysClass)lastInModel).add((SysMethod)nextNotInModel); 
				//when working with class analysis via java API we don't need to care about that cause just an Aspect can own an Advice 
			} else {
				//we cant add a method in anything else than a SysClass
				return false;
			}
		}
		return true;
	}




	public static SysElement new_tryOption(SysPackage p1, String pathToFind,String signature, SysRoot r) throws PathNotFoundException{
		String path = r.getPath()+File.separator+
				p1.getFullyQualifiedName().replace(".",File.separator);
		int dotPosition = pathToFind.indexOf(".");
		if(dotPosition==-1){
			System.err.println("MethodAnalysis: we are throwing an exception because we are looking for a method, " +
					"and this method does not find methods, just classes and packages"+
					"\n\tlooking into: "+p1.getName()+"\n\tfor: "+pathToFind + " " + signature);
			throw new PathNotFoundException();
		}

		String f = pathToFind.substring(0,dotPosition);
		String nextPath = pathToFind.substring(dotPosition+1);
		dotPosition=nextPath.indexOf(".");

		SysPackage cobaia =(SysPackage) p1.partialClone();
		SysAnalysis.analysePackage(cobaia, path, false);
		SysElement e=null;
		e = cobaia.getMax(f+"."+nextPath, "");
		try{
			SysElement e1 = tryOption(e, nextPath, signature, r);
			if(e instanceof SysClass){
				((SysClass)e).addChild(e1);
			} else {
				if(e instanceof SysPackage){
					((SysPackage)e).addChild(e1);
				}
			}
			return e;
		} catch(PathNotFoundException pnfe){
			//???
		}
		return null;
	}

	public static boolean addDependency(SysMethod caller, SysMethod called){
		if(caller instanceof SysAdvice || (caller instanceof SysMethod && !(called instanceof SysAdvice))){
			System.out.println("[MethodAnalysis]: adding "+called+" to " + caller);
			caller.addDependency(called);
			System.out.println("[MethodAnalysis]: added?(true|false) "+caller.dependsOn(called));

		} else { //i'm sure that the caller is not an advice
			if(called instanceof SysAdvice){
				System.out.println("[MethodAnalysis]: adding "+caller+" to " + called);
				((SysAdvice)called).addAffected(caller);
				caller.addAffectedBy((SysAdvice)called);

				System.out.println("[MethodAnalysis]: added?(true|false) "+((SysAdvice)called).affects(caller));
			} else {
				return false;
			}
		}
		return true;
	}

	public static String fixSingleSignature(String sig){
		if(sig.startsWith("L")) sig=sig.substring(1);
		if(sig.endsWith(";")) sig = sig.substring(0,sig.lastIndexOf(";"));
		if(sig.equals("Z")) return "boolean";	//boolean
		if(sig.equals("I")) return "int"; 		//int
		if(sig.equals("D")) return "double";	//double
		if(sig.equals("F")) return "float";		//float
		if(sig.equals("S")) return "short"; 	//short
		if(sig.equals("J")) return "long";		//long
		if(sig.equals("C")) return "char";		//char
		if(sig.equals("V")) return "void";		//void
		return sig;
	}

}
