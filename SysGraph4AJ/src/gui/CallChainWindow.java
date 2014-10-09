/**
 * @ CallChainWindow.java
 * date 02/01/2012    <>      mm/dd/yyyy
 * 
 */
package gui;

import java.awt.Container;

import javax.swing.JFrame;
import javax.swing.JTextArea;

import model.IElement;
import model.SysMethod;
import model.SysRoot;
import visualization.CallChainM2G;
import analysis.MethodAnalysis;
import edu.uci.ics.jung.algorithms.layout.AggregateLayout;
import edu.uci.ics.jung.visualization.VisualizationViewer;

/**
 * @author Felipe Capodifoglio Zanichelli
 *
 */
public class CallChainWindow extends JFrame implements GUIWindowInterface {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private SysRoot root;
	private SysMethod m;
	private Container center = this.getContentPane();
	private JTextArea textArea = new JTextArea();

	/**@param m the SysMethod that will start the call chain graph
	 * @param root the SysRoot in which the SysMethod is child*/
	public CallChainWindow(SysMethod m, SysRoot root) {
		super("CallChain >>> " + m);
		this.m = m;
		this.root = root;
		CallChainM2G cc = new CallChainM2G();
		AggregateLayout<IElement, Object> al = cc.doAggregateLayout(root,m); 
		VisualizationViewer<IElement,Object> vv = new CallChainM2G().makeVV(al);
		this.add(vv);
		SysUtils.makeGoodVisual(vv, this);
		makeMenuBar(vv);
		vv.setSize(this.getSize());
		this.pack();
		SysUtils.setAtCenter(m, al, this, vv);

	}


	/**
	 * Open a new CallChainWindow analysing the given method recursively
	 * @param m the SysMethod that will start the call chain graph
	 * @param root the SysRoot in which the SysMethod is a child of
	 * */
	public CallChainWindow(SysMethod m, SysRoot root, boolean b) {
		super("CallChain >>> "+m);
		this.m=m;
		this.root=root;
		if(b){
			recur(root,m);
		}
		CallChainM2G cc = new CallChainM2G();
		AggregateLayout<IElement, Object> al = cc.doAggregateLayout(root,m); 
		VisualizationViewer<IElement,Object> vv = new CallChainM2G().makeVV(al);
		this.add(vv);
		SysUtils.makeGoodVisual(vv, this);
		makeMenuBar(vv);
		this.pack();
		SysUtils.setAtCenter(m, al, this, vv);
	}



	/**analyse the method {@param m2} recursively */
	private void recur(SysRoot root2, SysMethod m2) {
		if(m2.isAnalysed() && !m.equals(m2)) return;
		MethodAnalysis.analyseMethod(m2, root);
		for(SysMethod m : m2.getCalls()){
			recur(root2,m);
		}

	}

	
	/**gets the center panel*/
	public Container getCenter() {
		return this.center;
	}

	
	/**set a center panel*/
	public void setCenter(Container c) {
		this.remove(this.center);
		this.setContentPane(c);
		this.center = c;
	}

	
	/**return the textArea*/
	public JTextArea getTextArea() {
		return this.textArea ;
	}

	
	/**sets a center panel*/
	public void setCenterPanel(Container pane) {
		this.setCenter(pane);
	}

	
	public void makeGoodVisual(VisualizationViewer<IElement, Object> vv) {
		SysUtils.makeGoodVisual(vv, this);
	}

	
	/**makes a menu bar given a visualization viewer*/
	public void makeMenuBar(VisualizationViewer<IElement, Object> vv) {
		SysUtils.makeMenuBar(vv, this, this.root);
		MainWindow.getInstance().makeJUnitMenu();
	}

	
	/**@return boolean whether the right click is enabled*/
	public boolean rightClickEnabled() {
		return true;
	}


	/**
	 * @return the SysMethod object of this class
	 */
	public SysMethod getM() {
		return m;
	}



	
	public JFrame getFrame() {
		return this;
	}


}
