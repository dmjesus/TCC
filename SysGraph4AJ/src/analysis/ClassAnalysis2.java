/**
 * @author Felipe
 * */

package analysis;

import java.io.File;
import java.lang.reflect.Constructor;

import model.SysElement;
import model.SysAdvice;
import model.SysAspect;
import model.SysClass;
import model.SysField;
import model.SysMethod;
import model.SysPointcut;
import model.SysRoot;

/**
 * A new class to analyse SysClasses
 * */
public class ClassAnalysis2 {



	/**
	 * Analyse a class given the SysClass and the SysRoot
	 * @param c the class to be analysed
	 * @param r container SysRoot
	 * @return the analysed SysClass. Caution, it really modifies the given SysClass. If you dont want to modify it, pass a parcialClone().
	 * */
	public static SysClass analyseClass(SysClass c, SysRoot r){
		String path = c.getFullyQualifiedName();
		if(path.contains(".")){//we need replace all "." by "\\", but java does not do that for us... we need to improvise
			path = path.replace('.', File.separatorChar);
		}
		path = r.getPath() + File.separator + path;
		if(path.lastIndexOf(".class") == -1)
			path += ".class";//all java classes end in .class
		if(path.contains("(default package)" + File.separator)) 
			path = path.replace("(default package)" + File.separator, ""); //default package only exists in our mind
		if(path == null || !path.endsWith(".class")){
//			System.err.println("ClassAnalysis: Path nao e um caminho valido para uma classe.");
			System.err.println("ClassAnalysis: Path isnt a valid path for one class.");
			return c;
		}
		if(c.isAnalysed()) {
			System.err.println("ClassAnalysis: This class doesnt need to see analysed!");
			return c;
		}
		/*end of check list*/

		Class<?> clazz = null;
//		This is not necessary aparentely
//		String cfully = c.getFullyQualifiedName(); 
//		if(cfully.startsWith("(default package).")){ //take care of default package
//			cfully = cfully.substring("(default package).".length());
//		}
		String cfully = c.getName();
		
		try {
			clazz = FileLoader.forName(cfully);
		} catch (ClassNotFoundException e1) { //it may be an inner class
			//e1.printStackTrace();
			SysElement owner = c.getOwner();
			if(owner instanceof SysClass){
				String cowner = c.getOwner().getFullyQualifiedName(); //cowner = class owner
				if(cowner.startsWith("(default package).")){
					cowner = cowner.substring("(default package).".length());
				}
				cowner += "$"+c.getName();
				try{
					clazz= FileLoader.forName(cowner);
				} catch(ClassNotFoundException e2){
					e2.printStackTrace();
				}
			}
		}
		
		if(clazz != null){
			java.lang.reflect.Method[] vet_met = clazz.getDeclaredMethods(); //get declared methods
			int size = vet_met.length;
			for(int i =0; i < size; i++){
				java.lang.reflect.Method meth = vet_met[i];
				String name = meth.getName();
				System.out.print("[ClassAnalysis2]: add method \"" + name + "\"?");
				if(validateMethod(meth)){
					System.out.println(" yes");
					SysMethod m = null;
					if(name.contains("ajc$")){
						if(name.contains("ajc$pointcut")){
							c.addChild(new SysPointcut(meth));//its a pointcut
						} else {
							m = new SysAdvice(meth); //its an Advice
						}
					} else if(!name.contains("jacoco")){
						m = new SysMethod(meth); //its a regular method
					}
					
					if(m != null){
						Class<?>[] exceptions = meth.getExceptionTypes(); //exceptions declared to throw
						m.addException(exceptions);
						if(c instanceof SysAspect){
							if(m instanceof SysAdvice){
								((SysAspect)c).add((SysAdvice)m);//cast to add in the right field of SysAspect
							}
						} else{
							c.add(m); //not an advice, add regular method
						}
					}
				} else {
					System.out.println(" no");
				}
			}
			
			Constructor<?>[] vet_init = clazz.getConstructors(); //constructor is a special method
			if(vet_init!=null){
				for(int i=0; i < vet_init.length; i++){
					SysMethod m = new SysMethod(false,"<init>", "void", SysAnalysis.getVisibility(vet_init[i].getModifiers())); //make the basic method
					Class<?>[] param = vet_init[i].getParameterTypes();
					if(param != null){
						for(int j = 0; j < param.length; j++){
							m.addParameter(param[j].getCanonicalName()); //create the signature
						}
					}
					c.add(m);
				}
			}
			
			java.lang.reflect.Field[] f = clazz.getDeclaredFields(); //just the declared fields.
			for(int i = 0; i < f.length; i++){
				if(!f[i].getName().contains("$jacoco"))
					c.add(new SysField(f[i]));
			}

			Class<?>[] innerClasses = clazz.getDeclaredClasses();
			c.addInnerClass(innerClasses); //add inner classes
			
			if(clazz.getSuperclass() != null){
				c.setSuperClass(new SysClass(clazz.getSuperclass().getCanonicalName())); //add the super class to this class
			} else {
				c.setSuperClass(new SysClass("Error: couldnt find super class")); //every single class must have a super class
			}
			c.setInterfaces(clazz.getInterfaces()); //add interfaces
		} else {
			System.out.println("[ClassAnalysis2]: could not analyse the class \"" + c + "\"");
		}
		return c;
	}

	/**returns whether the method name is valid or not*/
	private static boolean validateMethodName(String name){
		if(name.equalsIgnoreCase("aspectOf")  ||
				name.equalsIgnoreCase("hasAspect") ||
				name.equalsIgnoreCase("ajc$postClinit") ||
				//name.startsWith("ajc$pointcut") ||
				false) return false;
		return true;
	}

	/**returns whether the method is valid or not*/
	private static boolean validateMethod(java.lang.reflect.Method m){
		boolean synthetic = m.isSynthetic();// static?
		return validateMethodName(m.getName()) &&
				(!synthetic || (synthetic && m.getName().contains("pointcut")));
	}
	
}
