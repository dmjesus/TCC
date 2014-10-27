/**
 * @ Utils.java
 * date 02/25/2012    <>      mm/dd/yyyy
 * 
 */
package gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;

import model.IElement;
import model.SysRoot;
import visualization.ModelToGraph;
import visualization.SysGraphMouse;
import edu.uci.ics.jung.algorithms.layout.AggregateLayout;
import edu.uci.ics.jung.algorithms.layout.TreeLayout;
import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.graph.DelegateTree;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.RenderContext;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;
import edu.uci.ics.jung.visualization.transform.MutableTransformer;

/**
 * @author Felipe Capodifoglio Zanichelli
 *
 */

public class SysUtils {

	private static final SysTransformers TRANSFORMERS = new SysTransformers();

	/**
	 * Sets the given container and puts it in the center of the GUIWindow*/
	public static void setCenter(Container center, GUIWindowInterface target) {
		target.setCenter(center);
	}

	/**@return the text area of the GUIWindow...it may be a hidden text area....*/
	public static JTextArea getTextArea(GUIWindowInterface target){
		return target.getTextArea();
	}

	/**
	 * Same thing as setCenter but this time it makes a good visual for the Visualization Viewer*/
	@SuppressWarnings("unchecked")
	public static void setCenterPanel(Container pane, GUIWindowInterface target){
		Container c = target.getContentPane();
		c.remove(target.getCenter()); 
		c.add(pane, BorderLayout.CENTER);
		target.setCenter(pane);
		if(pane instanceof VisualizationViewer){
			VisualizationViewer<IElement, Object> vv = (VisualizationViewer<IElement,Object>) pane;
			target.makeGoodVisual(vv);
			vv.updateUI();
		}
	}

	/**
	 * Makes a good visual, i.e., set transformers to put color in the graph, set names, and tooltip*/
	public static void makeGoodVisual(VisualizationViewer<IElement, Object>  vv, GUIWindowInterface target){
		RenderContext<IElement, Object> rc = vv.getRenderContext();
		rc.setVertexFillPaintTransformer(TRANSFORMERS.getVertexPaint());		//vertex color
		rc.setEdgeStrokeTransformer(TRANSFORMERS.getEdgeStrokeTransformer());		//edge type
		rc.setVertexLabelTransformer(TRANSFORMERS.getVertexToString());		//vertex label
		rc.setEdgeLabelTransformer(TRANSFORMERS.getEdgeToString());			//edge label
		rc.setEdgeShapeTransformer(TRANSFORMERS.getEdgeShape());
		vv.setVertexToolTipTransformer(TRANSFORMERS.getToolTip());
		vv.getRenderer().getVertexLabelRenderer().setPosition(Position.AUTO);
		/*care about the mouse plugin*/
		target.makeMenuBar(vv);
		}

		/**
		 * makes a menu bar for the GUI, based on the VV*/
		public static void makeMenuBar(VisualizationViewer<IElement, Object>  vv, GUIWindowInterface target, SysRoot r){
			SysGraphMouse gm = new SysGraphMouse(target, r);
			vv.setGraphMouse(gm);
			JMenuBar menuBar = new JMenuBar();
			JMenu menu = gm.getModeMenu();
			
			KeyStroke f2 = KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0);
			KeyStroke f3 = KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0);
			menu.getItem(0).setAccelerator(f2);
			menu.getItem(1).setAccelerator(f3);
			
			menu.setText("Mouse Mode");
			menu.setIcon(null);
			menu.setMinimumSize(new Dimension(90,20));
			menu.setPreferredSize(new Dimension(90,20));
			menuBar.add(menu);
			target.setJMenuBar(menuBar);
		}

		/**center the vertex in screen*/
		public static void setAtCenter(IElement vertex, AggregateLayout<IElement, Object> al, JFrame frame, VisualizationViewer<IElement, Object> vv){
			Point2D vertexPoint = al.transform(vertex);
			Rectangle frame_area = frame.getBounds();
			double calculatedDeltaY = 120.0d;
			Point2D ctr = new Point(
					(int)(frame_area.getX() + frame_area.getWidth()/2), 
					(int) (frame_area.getY() + calculatedDeltaY )); //create a logical center point X_0+width/2, Y_0+120. 
			//Note: X_0 and Y_0 are both 0, i.e., they are the left top pixel
			double deltaX = (ctr.getX() - vertexPoint.getX());
			double deltaY = (ctr.getY() - vertexPoint.getY());
			MutableTransformer layout = vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT);
			layout.translate(deltaX, deltaY);

		}

		/**
		 * Retorna um objeto de visualização {@link VisualizationViewer} representado
		 * a partir de um objeto da classe {@link SysRoot}
		 * 
		 * @param root
		 * 		Parametro que referencia a raiz do programa.
		 * @return
		 */
		public static VisualizationViewer<IElement, Object> createVisualizationViewerBySysRoot(
				SysRoot root, int deltaX, int deltaY) {
			DelegateTree<IElement, Object> delegateTree = new  DelegateTree<IElement, Object>();
			delegateTree.addVertex(root);
			delegateTree = ModelToGraph.putAllChildren_SysRoot(delegateTree, root);
			DelegateForest<IElement, Object> delegateForest = new DelegateForest<IElement, Object>(delegateTree);
			VisualizationViewer<IElement, Object> visualizationViewer = new VisualizationViewer<IElement, Object>(
					new TreeLayout<IElement, Object>(
							delegateForest, deltaX, deltaY));
			return visualizationViewer;
		}


	}
