package analysis;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.HashSet;

import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.Method;
import org.aspectj.lang.annotation.Aspect;

import model.SysAspect;
import model.SysClass;
import model.SysPackage;
import model.SysRoot;

public class SysAnalysis {

	

	public static void analysePackage(SysPackage p, String path){
		analysePackage(p,path, true);
	}


	public static void analysePackage(SysPackage p, String path, boolean recursively) {

		File rootFile = null; //cobaia directory
		boolean isDefaultPackage = false;
		if(path.contains("(default package)")) {
			path = path.replace("(default package)", "");
			isDefaultPackage = true;
		}
		rootFile = new File(path);
		if(!rootFile.canRead()){
			System.err.println("Can't read file \"" + path + "\".");
			return;
		}
		/*just verified if that is a valid path and can be read. Algorithms starts here*/
		File aux[] = rootFile.listFiles(); //list all files (directories and files into package) 
		
		HashSet<File> directories = new HashSet<File>(); //just directories
		HashSet<File> classes = new HashSet<File>(); //just .class files
		for(int i=0; i < aux.length; i++){
			if(aux[i].isDirectory() && !aux[i].isHidden() && aux[i].canRead() && !aux[i].getName().startsWith(".") && !isDefaultPackage) {
				directories.add(aux[i]); //insert directory
			}
			else{ 
				if(aux[i].isFile() && aux[i].getName().endsWith(".class")){
					classes.add(aux[i]); //insert dot class file
				}
			}
		}

		/*add classes and directories to the current package*/
		String name;
		int i;
		for(File f : classes){
			name = f.getName();
			if(name.contains("$")) continue; //exclude file if it's an inner class
			i = name.indexOf("."); //exclude ".class" in name 
			if(i != -1) name = name.substring(0, i);
			String fully = p.getFullyQualifiedName() + "." + name; 
			if(fully.startsWith("(default package).")){
				fully = fully.substring("(default package).".length());
			}
			Class<?> c = null;
			try {
				System.out.println("[SysAnalysis]: class for name \'" + fully + "\'");
				c = FileLoader.forName(fully); //return a reference to system classLoader - HelloWorld
				if(c.getAnnotation(Aspect.class) != null){
					p.add(new SysAspect(name));
				} else {
					p.add(new SysClass(name));
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				p.add(new SysClass(name)); //add class to package, not inner classes
			}
		}
		for(File f : directories){
			p.add(new SysPackage(f.getName()));
		}

		/*if there is just one package in this package, program will analyse it automatically*/
		/*This is really necessary?*/
//		if(p.getPackages().size()==1 && p.getClasses().size()==0 && recursively){
//			SysPackage willBeAnalysed = p.getPackages().iterator().next();
//			SysAnalysis.analysePackage(willBeAnalysed, path + File.separator + willBeAnalysed.getName());
//		}
		p.setIsAnalysed(true);
	}  

	public static String getVisibility(Field fd) {
		if(fd.isPrivate()) return "private";
		if(fd.isPublic()) return "public";
		if(fd.isProtected()) return "protected";
		return "friendly";
	}

	public static String getVisibility(Method m){
		if(m.isPrivate()) return "private";
		if(m.isPublic()) return "public";
		if(m.isProtected()) return "protected";
		return "friendly";
	}
	
	public static String getVisibility(java.lang.reflect.Method m){
		return getVisibility(m.getModifiers());
	}
	
	/**return the visibility string representation*/
	public static String getVisibility(int modifier){
		if(Modifier.isPrivate(modifier)) return "private";
		if(Modifier.isPublic(modifier)) return "public";
		if(Modifier.isProtected(modifier)) return "protected";
		return "friendly";
	}
	
	/**Constructs an initial model given a path*/
	public static SysRoot initialModel(String pathToBin) {
		File rootFile;
		rootFile = new File(pathToBin);
		if(!rootFile.canRead()){
			System.err.println("Can't read file \"" + pathToBin + "\"");
			return null;
		}
		FileLoader.add(rootFile);
		/*just verified if that is a valid path and can be read. Algorithms starts here*/
		SysRoot root = new SysRoot(pathToBin);
		File aux[] = rootFile.listFiles(); //list all files (directories and files 
		HashSet<File> directories = new HashSet<File>(); //just directories
		HashSet<File> classes = new HashSet<File>(); //just .class files
		for(int i=0; i < aux.length; i++){
			if(aux[i].isDirectory() && !aux[i].isHidden() && !aux[i].getName().startsWith("."))
				directories.add(aux[i]); //insert directory
			else 
				if(aux[i].getName().endsWith(".class"))
					classes.add(aux[i]); //insert dot class file
				else
					System.out.println("File " + aux[i].getName() + " ignored");
		}

		if(classes.size() > 0) { //this path is the default package 
			SysPackage p = new SysPackage("(default package)");
			for(File f : directories){
				root.add(new SysPackage(f.getName()));
			}
			root.add(p);
		} 
		else { //path really represents the project home
			for(File f:directories){
				root.add(new SysPackage(f.getName()));
			}
			if(directories.size()==1){
				SysPackage willBeAnalysed = root.getPackages().iterator().next(); 
				SysAnalysis.analysePackage(willBeAnalysed, pathToBin + File.separator + willBeAnalysed.getFullyQualifiedName());

			}
		}
		return root;
	}

}
