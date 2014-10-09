package model;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Iterator;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;


public class Main {
  static SysRoot buildRoot = new SysRoot();
  
  
  
 
  
  public static void main(String[] args) {
    
    
    SysPackage p1 = new SysPackage("Pack_1");
    SysPackage p2 = new SysPackage("Pack_2");
    SysPackage p3 = new SysPackage("Pack_3");
    SysPackage p4 = new SysPackage("Pack_4");
    SysPackage p1p1 = new SysPackage("Pack_1_from_Pack_1");
    SysPackage p1p3 = new SysPackage("Pack_1_from_Pack_3");
    SysClass c1p1 = new SysClass("C1p1");
    SysClass c2p1 = new SysClass("C2p1");
    SysClass c3p1 = new SysClass("C3p1");
    SysClass c1p1p1 = new SysClass("C1p1p1");
    SysClass c1p2 = new SysClass("C1p2");
    SysClass c1p4 = new SysClass("C1p4");
    SysClass c2p4 = new SysClass("C2p4");
    SysClass c3p4 = new SysClass("C3p4");
    SysMethod m1c2p1 = new SysMethod(false,"Method1_C2_p1","V","public");
    SysMethod m2c2p1 = new SysMethod(false,"Method2_C2_p1","V","public");
    SysMethod m1c3p1 = new SysMethod(false,"Method1_C3_p1","V","public");
    SysMethod m1c2p4 = new SysMethod(false,"Method1_C2_p4","V","public");
    
    buildRoot.add(p1);
    buildRoot.add(p2);
    buildRoot.add(p3);
    buildRoot.add(p4);
    
    p1.add(c1p1);
    p1.add(c2p1);
    p1.add(c3p1);
    p1.add(p1p1);
    p1p1.add(c1p1p1);
    
    p2.add(c1p2);
    
    p3.add(p1p3);
    
    p4.add(c1p4);
    p4.add(c2p4);
    p4.add(c3p4);
    
    c2p1.add(m1c2p1);
    c2p1.add(m2c2p1);
    
    c3p1.add(m1c3p1);
    
    c2p4.add(m1c2p4);
    
    m1c2p1.addParameter("I");
    m1c2p1.addParameter("L["+c2p1.getFullyQualifiedName()+";");
    
    m2c2p1.add(m1c3p1);
    
    m1c3p1.add(m1c2p4);
    
    
    
    
    
    SysRoot r = new SysRoot();
    SysPackage rp1 = new SysPackage("Pack_1");
    SysPackage rp2 = new SysPackage("Pack_2");
    SysPackage rp3 = new SysPackage("Pack_3");
    SysPackage rp4 = new SysPackage("Pack_4");
    r.add(rp4);
    r.add(rp3);
    r.add(rp2);
    r.add(rp1);
    jframeFromSysRoot(r);
  }
  
  
  public static void jframeFromSysRoot(SysRoot r){
    if(r==null){JOptionPane.showMessageDialog(null, "SysRoot is a null pointer");return;}
    
    JFrame frame = new JFrame("SysRoot view");
    frame.setLayout(new GridLayout(r.getPackages().size(),2));
    Container c = frame.getContentPane();
    JButton button = null;
    ActionListener buttonListener = null;
    for(SysPackage pack : r.getPackages()){
      final SysPackage finalPack = pack; 
      //this is final because we cant use an non final object at inner classes
      button = new JButton("view");
      
      buttonListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          
          jframeFromPackage(refactor(finalPack));
        }
      };
      button.addActionListener(buttonListener);
      c.add(new JLabel(pack.getName()));
      c.add(button);
    }
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.pack();
    frame.setMinimumSize(new Dimension(300, 10));
    frame.setVisible(true);
    
  }

  public static void jframeFromPackage(SysPackage p){
    if(p==null){JOptionPane.showMessageDialog(null, "null SysPackage");return;}
    JFrame frame = new JFrame(p.getFullyQualifiedName() + " view");
    frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    HashSet<SysPackage> hp = p.getPackages();
    HashSet<SysClass> hc = p.getClasses();
    int bigger = (hp.size()>hc.size()?hp.size():hc.size());
    if(bigger==0){JOptionPane.showMessageDialog(null, "this package has no members");return;}
    frame.setLayout(new GridLayout(bigger,4));
    Container c = frame.getContentPane();
    JButton button = null;
    ActionListener buttonListener = null;
    Iterator<SysPackage> hpi = hp.iterator();
    Iterator<SysClass> hci = hc.iterator();
    for(int i = 0;i<bigger;i++){
      if(hpi.hasNext()) {
        final SysPackage finalPack = hpi.next(); 
        //this is final because we cant use an non final object at inner classes
        button = new JButton("view");
        buttonListener = new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            jframeFromPackage(refactor(finalPack));
          }
        };
        button.addActionListener(buttonListener);
        c.add(new JLabel(finalPack.getFullyQualifiedName()));
        c.add(button);
      }else{c.add(new JLabel());c.add(new JLabel());}
      
      if(hci.hasNext()) {
        final SysClass finalClass = hci.next(); 
        button = new JButton("view");
        
        buttonListener = new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            jframeFromClass(refactor(finalClass));
          }
        };
        button.addActionListener(buttonListener);
        c.add(new JLabel(finalClass.getName()));
        c.add(button);
      }else{c.add(new JLabel());c.add(new JLabel());}
    }
    frame.pack();
    frame.setMinimumSize(new Dimension(300, 10));
    frame.setVisible(true); 
  }
  
  public static void jframeFromClass(SysClass sc){
    if(sc==null){JOptionPane.showMessageDialog(null, "null SysClass");return;}
    JFrame frame = new JFrame(sc.getFullyQualifiedName() + " view");
    frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    HashSet<SysField> hf = sc.getFields();
    HashSet<SysMethod> hm = sc.getMethods();
    int bigger = (hf.size()>hm.size()?hf.size():hm.size());
    if(bigger==0){JOptionPane.showMessageDialog(null, "this class has no fields or methods");return;}
    frame.setLayout(new GridLayout(bigger,4));
    Container c = frame.getContentPane();
    
    JButton button = null;
    ActionListener buttonListener = null;
    Iterator<SysField> hfi = hf.iterator();
    Iterator<SysMethod> hmi = hm.iterator();
    for(int i = 0;i<bigger;i++){
      if(hfi.hasNext()) {
        final SysField finalField = hfi.next(); 
        //this is final because we cant use an non final object at inner classes
        button = new JButton("view");
        
        buttonListener = new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            JOptionPane.showMessageDialog(null, "Abrindo SysField \""+finalField.getName() +"\"");
          }
        };
        button.addActionListener(buttonListener);
        c.add(new JLabel(finalField.getName()));
        c.add(button);
      }else{c.add(new JLabel());c.add(new JLabel());}
      
      if(hmi.hasNext()) {
        final SysMethod finalMethod = hmi.next(); 
        //this is final because we cant use an non final object at inner classes
        button = new JButton("view");
        
        buttonListener = new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            //JOptionPane.showMessageDialog(null, "Abrindo SysMethod \""+finalMethod.getName() +"\"");
        	jframeFromMethod(finalMethod);
          }
        };
        button.addActionListener(buttonListener);
        c.add(new JLabel(finalMethod.getName()+finalMethod.getSignature()));
        c.add(button);
      }else{c.add(new JLabel());c.add(new JLabel());}
    }
    frame.pack();
    frame.setMinimumSize(new Dimension(300, 10));
    frame.setVisible(true); 
  }
  
  public static void jframeFromMethod(SysMethod m){
    if(m==null){JOptionPane.showMessageDialog(null, "null SysMethod");return;}
    JFrame frame = new JFrame(((SysElement)m.getOwner()).getFullyQualifiedName() + "."+ m.getName()+ " view");
    frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    frame.setLayout(new GridLayout(0,1));
    Container c = frame.getContentPane();
    c.add(new JLabel((m.isStatic()?"static":"") + m.getName() +" "+ m.getSignature()));
    frame.setMinimumSize(new Dimension(300, 10));
    frame.pack();
    frame.setVisible(true); 
  }
  
  public static SysPackage refactor(SysPackage p){
    System.out.println("Analising SysPackage: " + p.getFullyQualifiedName());
    SysPackage fp = findFullPack(p);
    if(fp==null || p.isAnalysed()) return p;
    if(fp.getPackages().size()!=0)
      for(SysPackage toCreate : fp.getPackages())
        p.add(new SysPackage(toCreate.getName()));
    if(fp.getClasses().size()!=0)
      for(SysClass toCreate : fp.getClasses())
        p.add(new SysClass(toCreate.getName()));
    p.setIsAnalysed(true);
    return p;
  }
  
  public static SysPackage findFullPack(SysPackage p) {
    SysPackage aux = null;
    for(SysPackage ret : buildRoot.getPackages()){
      if(ret.equals(p)) return ret;
      aux = findFullPackInPack(p,ret);
      if(aux!=null) return aux;
    }
    return null;
  }

  public static SysPackage findFullPackInPack(SysPackage p, SysPackage ret) {
    SysPackage aux1 = null;
    for(SysPackage aux : ret.getPackages()){
      if(aux.equals(p)) return aux;
      aux1=findFullPackInPack(p,aux);
      if(aux1!=null) return aux1;
    }
    return null;
  }

  public static SysClass refactor(SysClass c){
    System.out.println("Analising SysClass: "+ c.getFullyQualifiedName());
    SysClass fc = findFullClass(c);
    if(fc==null || c.isAnalysed()) return c;
    if(fc.getMethods().size()!=0)
      for(SysMethod toCreate : fc.getMethods())
        c.add(new SysMethod(toCreate.isStatic(),toCreate.getName(),toCreate.getReturnType(),toCreate.getVisibility()));
    if(fc.getFields().size()!=0)
      for(SysField toCreate : fc.getFields())
        c.add(new SysField(toCreate.isStatic(), toCreate.getType(), toCreate.getName(), toCreate.getVisibility()));
    c.setIsAnalysed(true);
    return c;
  }

  public static SysClass findFullClass(SysClass c) {
	SysClass fc = null;
	for(SysPackage p : buildRoot.getPackages()){
	  fc = findFullClassInPackage(c,p);
	  if(fc!=null)
	    return fc;
	}
	return c;
  }

  public static SysClass findFullClassInPackage(SysClass c, SysPackage p) {
	  
	for(SysClass fc : p.getClasses()){
	  if(fc.equalsIgnoreFullyQualifiedName(c))
		return fc;
	}
	SysClass fc = null;
	for(SysPackage p1 : p.getPackages()){
	  fc=findFullClassInPackage(c,p1);
	  if(fc!=null)
		return fc;
	}
	return null;
  }
  
 
}
