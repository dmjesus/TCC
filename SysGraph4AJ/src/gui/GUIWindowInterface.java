/**
 * @ GUIWindowInterface.java
 * date 02/25/2012    <>      mm/dd/yyyy
 * 
 */

package gui;

import java.awt.Container;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JTextArea;

import model.IElement;
import edu.uci.ics.jung.visualization.VisualizationViewer;

/**
 * @author Felipe Capodifoglio Zanichelli
 *
 */

public interface GUIWindowInterface {	
	
	public Container getCenter();
	public void setCenter(Container c);
	public JTextArea getTextArea();
	public void setCenterPanel(Container pane);
	public Container getContentPane();
	public void makeGoodVisual(VisualizationViewer<IElement, Object> vv);
	public void makeMenuBar(VisualizationViewer<IElement, Object> vv);
	public void setJMenuBar(JMenuBar menuBar);
	public boolean rightClickEnabled();
	public JFrame getFrame();

}
