package visualization;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import model.IElement;
import model.SysClass;
import model.SysElement;
import model.SysMethod;
import model.SysPackage;
import model.SysRoot;
import analysis.ClassAnalysis2;
import analysis.MethodAnalysis;
import analysis.SysAnalysis;
import cfg.gui.CFGModelToGraph;
import edu.uci.ics.jung.algorithms.layout.AggregateLayout;
import edu.uci.ics.jung.algorithms.layout.GraphElementAccessor;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import gui.CallChainWindow;
import gui.GUIWindowInterface;
import gui.MainWindow;

/**
 * Classe responsável por tratar os eventos de click pelo mouse no grafo.
 * 
 * @author felipe
 * @author robson
 *
 */
public class SysGraphMouse extends DefaultModalGraphMouse<IElement, Object> {

	private static final int CALLCHAIN_INDICATOR = 1;
	private static final int MAINWINDOW_INDICATOR = 2;
	private long lastTimeClicked=0l;
	private long doubleClickTime = 400l;
	private SysRoot root;
	private GUIWindowInterface windowInterface;

	public SysGraphMouse(GUIWindowInterface f, SysRoot r){
		this.root = r;
		this.windowInterface = f;
	}

	/**
	 * right click action
	 */
	@SuppressWarnings({ "unchecked", "rawtypes", "serial" })
	private void rightClick(MouseEvent e) {
		final VisualizationViewer<IElement,Object> visualizationViewer = (VisualizationViewer<IElement,Object>)e.getSource();
		Point2D p = e.getPoint();
		GraphElementAccessor<IElement,Object> pickSupport = visualizationViewer.getPickSupport();
		if(pickSupport != null) {
			final Layout l = visualizationViewer.getModel().getGraphLayout();
			IElement vertex = (IElement) pickSupport.getVertex(l, p.getX(), p.getY());
			JPopupMenu popup = new JPopupMenu();
			final IElement el = vertex;
			if(vertex instanceof SysMethod){
				final SysMethod m = (SysMethod)vertex;
				popup.add(new AbstractAction("View Call Chain"){
					public void actionPerformed(ActionEvent arg0) {
						CallChainWindow w = new CallChainWindow(m, (SysRoot) root);
						w.setVisible(true);
					}
				});
				popup.add(new AbstractAction("View Call Chain analysing methods recursively"){

					public void actionPerformed(ActionEvent arg0) {
						CallChainWindow w2 = new CallChainWindow(m,(SysRoot) root,true);
						w2.setVisible(true);
					}
				});
				AbstractAction viewControlFlowGraphScreen = this.getViewControlFlowGraphScreen((SysMethod)el);
				if(viewControlFlowGraphScreen != null) {
					popup.add(viewControlFlowGraphScreen);
				}
			
			} 

			if(vertex instanceof SysElement && vertex != null){
				popup.add(this.getViewPropertiesScreen((SysElement)el));
			}
			popup.show(visualizationViewer, e.getX(), e.getY());
		}
	}

	@SuppressWarnings("serial")
	private AbstractAction getViewPropertiesScreen(final SysElement el) {
		return new AbstractAction("View Properties"){
			public void actionPerformed(ActionEvent arg0) {
				JFrame info = new JFrame(el.getFullyQualifiedName());
				JTextArea area = new JTextArea(el.viewState());
				JScrollPane scroll = new JScrollPane(area);
				info.add(scroll);
				area.setEditable(false);
				info.pack();
				info.setVisible(true);
			}
		};
	}

	/**
	 * Retorna um item do popup para geração do CFG a partir do {@link SysMethod} alvo
	 *  
	 * @param sysMethod
	 * 		método que será utilizado na construção do CFG
	 * @return executa a operação de construção do CFG a partir do {@link SysMethod}
	 */
	@SuppressWarnings("serial")
	private AbstractAction getViewControlFlowGraphScreen(final SysMethod sysMethod) {
		return sysMethod.getMethod() == null ? null : new AbstractAction("View Control Flow Graph") {
			
			public void actionPerformed(ActionEvent arg0) {
				CFGModelToGraph.addCFGToWindowInterface(root, sysMethod, SysGraphMouse.this.windowInterface);
			}
		};
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void mouseClicked(MouseEvent e) {
		long clickedNow = System.currentTimeMillis();
		if(e.getButton()==MouseEvent.BUTTON3 && windowInterface.rightClickEnabled()) {
			this.rightClick(e);
			return;
		} 

		if(clickedNow - this.lastTimeClicked <= this.doubleClickTime) {
			this.lastTimeClicked = clickedNow;
			VisualizationViewer<IElement,Object> visualizationViewer = (VisualizationViewer<IElement,Object>) e.getSource();
			Point2D p = e.getPoint();
			GraphElementAccessor<IElement,Object> pickSupport = visualizationViewer.getPickSupport();
			if(pickSupport != null) {
				SysElement vertex = (SysElement)pickSupport.getVertex(visualizationViewer.getModel().getGraphLayout(), p.getX(), p.getY());
				boolean isWorkingOnVisualizationViewer = false;
				long t1 = System.nanoTime();
				this.windowInterface.getTextArea().append("analysing "+vertex.getFullyQualifiedName()+"\n");
				if(vertex instanceof SysPackage && !((SysPackage) vertex).isAnalysed()) {
					String fullPath = File.separatorChar + ((SysPackage)vertex).getFullyQualifiedName();
					fullPath = fullPath.replace(".", File.separator);
					fullPath = ((SysRoot)this.root).getPath() + fullPath;
					if(fullPath.contains(File.separator + File.separator)) {
						JOptionPane.showMessageDialog(null, "filePath has two File.separator.");
					}	
					SysAnalysis.analysePackage((SysPackage)vertex, fullPath, true);
//					((SysPackage)vertex).setIsAnalysed(true);// this is not necessary, because this state is changed inside last method
					isWorkingOnVisualizationViewer = true;
				} else if(vertex instanceof SysClass && !((SysClass) vertex).isAnalysed()){
					// verificar adição do método $jacoco
					SysClass c = ((SysClass)vertex);
					c = ClassAnalysis2.analyseClass(c, (SysRoot) this.root);
					c.setIsAnalysed(true);
					isWorkingOnVisualizationViewer = true;
				} else {
					if(vertex instanceof SysMethod && !((SysMethod)vertex).isAnalysed()){
						SysMethod m = ((SysMethod)vertex);
						MethodAnalysis.analyseMethod(m, (SysRoot) this.root);
						m.setIsAnalysed(true);
						isWorkingOnVisualizationViewer = true;
					}
				}
				
				long t2 = System.nanoTime();
				if(isWorkingOnVisualizationViewer) {
					this.windowInterface.getTextArea().append("Took " + (t2-t1)/1000000.0d+ "ms to analyse " + vertex.getName() + "\n");
					Component c = this.windowInterface.getContentPane();
					int indicator = MAINWINDOW_INDICATOR;
					while(!(c instanceof JFrame)){
						c = c.getParent();
						if(c instanceof CallChainWindow){
							indicator = CALLCHAIN_INDICATOR;
							break;
						} else if (c instanceof MainWindow){
							indicator = MAINWINDOW_INDICATOR;
							break;
						}
					}
					if(indicator == CALLCHAIN_INDICATOR) { // que tipo de componente é esse?
						CallChainM2G cc = new CallChainM2G();
						AggregateLayout al = cc.doAggregateLayout((SysRoot) root,((CallChainWindow)c).getM());
						VisualizationViewer<IElement, Object> vv_callchain = cc.makeVV(al);
						this.windowInterface.setCenterPanel(vv_callchain);
						this.windowInterface.makeGoodVisual(vv_callchain);

					} else {
						CFGModelToGraph.reloadMainGraphWithCFGInformations(this.root, this.windowInterface, vertex);
					}
				}
			}
		}
		else {
			this.lastTimeClicked = clickedNow;
			super.mouseClicked(e);
		}
	}

}
