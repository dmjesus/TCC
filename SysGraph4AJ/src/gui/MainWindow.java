package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;

import model.IElement;
import model.SysElement;
import model.SysPackage;
import model.SysRoot;
import analysis.SysAnalysis;
import edu.uci.ics.jung.algorithms.layout.AggregateLayout;
import edu.uci.ics.jung.algorithms.layout.TreeLayout;
import edu.uci.ics.jung.graph.DelegateTree;
import edu.uci.ics.jung.visualization.VisualizationViewer;

@SuppressWarnings("serial")
public class MainWindow extends JFrame implements GUIWindowInterface {

	private static MainWindow self = null;
	private static boolean window = false;
	private static VisualizationViewer<SysElement, Float> visualizationViewer = null;
	private static boolean isVisualizationViewerEnabled = false;
	private String path = "bin";
	private JTextArea textArea = new JTextArea();
	private Container center;
	private SysRoot sysRoot = new SysRoot();
	private JPanel leftPanel = new JPanel();
	private int deltaX = 100;
	private int deltaY = 80;

	/**
	 * Constructs a new window (SingleTon Instance)
	 */
	public MainWindow(String name) {
		
		super(name);
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setVisible(false);
		//take a good visual for textArea :)
		this.textArea.setEditable(false);
		this.textArea.setBackground(Color.BLACK);
		this.textArea.setForeground(Color.GREEN);
		//make the textArea scrolleable
		JButton btn = this.createChoosePathButton();
		this.leftPanel.add(btn);
		btn = this.createAnalizeButton();
		this.leftPanel.add(btn);
		this.leftPanel.setVisible(true);
		this.leftPanel.setSize(85, 170);
		this.leftPanel.setLayout(new BoxLayout(this.leftPanel, BoxLayout.PAGE_AXIS));
		this.setUpContainerProperties();
	}
	
	public static MainWindow getInstance() {
		return self;
	}

	/**
	 * Inicializa as propriedades do container do frame
	 */
	private void setUpContainerProperties() {
		Container c = getContentPane();
		c.add(this.leftPanel, BorderLayout.LINE_START);
		JScrollPane scroll = new JScrollPane(this.textArea);
		scroll.setAutoscrolls(true);
		scroll.setPreferredSize(new Dimension(0,70));
		c.add(scroll,BorderLayout.PAGE_END);
		Container jl = new JLabel("");
		c.add(jl, BorderLayout.CENTER);
		this.setCenter(jl);
		this.pack();
		c.setVisible(true);
	}
	
	public JMenu makeJUnitMenu() {
		JMenuBar bar = this.getJMenuBar();
		JMenu menu = new JMenu();
		menu.setText("JUnit");
		menu.setIcon(null);
		menu.setPreferredSize(new Dimension(50,20));
		JMenuItem menuItem = new JMenuItem();
		menuItem.setText("Import");
		menuItem.addActionListener(new JUnitActionListener());
		menu.add(menuItem);
		bar.add(menu);
		return menu;
	}
	
	private final class JUnitActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			JUnitDialog j = new JUnitDialog(MainWindow.this);
			j.pack();
			j.setLocationRelativeTo(MainWindow.this);
		    j.setVisible(true);
		}
	}

	/**
	 * Cria o botão 'Analisar'
	 * 
	 * @return
	 * 		o botão criado
	 */
	private JButton createAnalizeButton() {
		JButton btn = new JButton("Analyse");
		btn.addActionListener(new AnalysePathActionListener());
		btn.setSize(new Dimension(80,30));
		btn.setMaximumSize(new Dimension(80,30));
		return btn;
	}

	/**
	 * Cria o botão 'Escolha o diretório bin'
	 * 
	 * @return
	 * 		o botão criado
	 */
	private JButton createChoosePathButton() {
		JButton btn = new JButton("Choose path");
		btn.addActionListener(new ChoosePathActionListener());
		btn.setSize(new Dimension(80,30));
		btn.setMaximumSize(new Dimension(80,30));
		return btn;
	}

	public static void main(String[] args) throws Exception {
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		System.out.println("Start: " + System.currentTimeMillis());

		Thread mainWindowThread = getMainWindowThread();	
		Thread visualizationViewerThread = getVisualizationViewerThread();

		System.out.println("Starting threads: " + System.currentTimeMillis());

		mainWindowThread.start();
		visualizationViewerThread.start();

		System.out.println("Threads started: " + System.currentTimeMillis());

		try{
			while(!(MainWindow.isVisualizationViewerEnabled && MainWindow.window)){
				Thread.sleep(10);
			}
			MainWindow.self.setCenterPanel(visualizationViewer);
			MainWindow.visualizationViewer.updateUI();
		} catch (InterruptedException e) {
			System.out.println("Error: " + System.currentTimeMillis());
			e.printStackTrace();
			MainWindow.self = null;
			MainWindow.visualizationViewer = null;
			System.out.println("Starting single core: " + System.currentTimeMillis());
			System.err.println("Starting mainWindow in single core mode...");
			MainWindow mainWindow = new MainWindow("Main Window");
			mainWindow.setExtendedState(MAXIMIZED_BOTH);
		}
		System.out.println("Launching: " + System.currentTimeMillis());
		MainWindow.self.setVisible(true);
	}

	/**
	 * 
	 * @return A thread responsável pela construção da tela principal
	 */
	private static Thread getMainWindowThread() {
		return new Thread() {
			public void run(){
				MainWindow.self = new MainWindow("Main window");
				MainWindow.self.setExtendedState(MainWindow.MAXIMIZED_BOTH);
				MainWindow.window=true;
			}
		};
	}

	/**
	 * 
	 * @return A thread responsável pela construção dos grafos
	 */
	private static Thread getVisualizationViewerThread() {
		return new Thread(){
			@SuppressWarnings({ "rawtypes", "unchecked" })
			public void run(){
				DelegateTree delegateTree = new DelegateTree();
				SysRoot sysRoot = new SysRoot();
				sysRoot.add(new SysPackage("null"));
				delegateTree.addVertex(sysRoot);
				MainWindow.visualizationViewer = new VisualizationViewer(new AggregateLayout(new TreeLayout(delegateTree)));
				MainWindow.isVisualizationViewerEnabled = true;
			}
		};
	}

	/**
	 * Makes an initial and special analysis given the path
	 * */
	public void analyse() {
		if(this.path == null) {
			this.textArea.append("That is not a valid path\n");
		}
		else {
			this.textArea.append("Beginning analysis\n");
			SysRoot root = SysAnalysis.initialModel(this.path); // do the initial model
			this.sysRoot = root;
			VisualizationViewer<IElement, Object> visualizationViewer 
				= SysUtils.createVisualizationViewerBySysRoot(root, this.deltaX, this.deltaY);
			this.setCenterPanel(visualizationViewer);
			visualizationViewer.updateUI();
			this.textArea.append(root.getPackages().toString() + "\n");
			this.makeGoodVisual(visualizationViewer);
		}
	}

	/**
	 * Listener responsável por analisar o pacote escolhido.
	 * 
	 * @author Robson
	 *
	 */
	private final class AnalysePathActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			//here goes the magic...
			MainWindow.this.analyse();
		}
	}

	/**
	 * Listener responsável por abrir uma janela onde nesta será escolhida as pastas e os arquivos a serem analisados. 
	 * 
	 * @author Robson
	 *
	 */
	private final class ChoosePathActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e){
			JFileChooser fc = new JFileChooser("." + File.separator + "..");
			fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			if(fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION){
				//just show the selected path into the textArea
				MainWindow.this.path = fc.getSelectedFile().getAbsolutePath();
				MainWindow.this.textArea.append("You choose: " + MainWindow.this.path + "\n");
				int cont = JOptionPane.showConfirmDialog(null, "Analyse path: " + MainWindow.this.path + " ?");
				if(cont == JOptionPane.OK_OPTION){
					MainWindow.this.analyse();
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void setCenterPanel(Container pane){
		Container container = getContentPane();
		container.remove(getCenter()); 
		container.add(pane, BorderLayout.CENTER);
		this.setCenter(pane);
		if(pane instanceof VisualizationViewer){
			VisualizationViewer<IElement, Object> visualizationViewer = ((VisualizationViewer<IElement,Object>) pane);
			this.makeGoodVisual(visualizationViewer);
			visualizationViewer.updateUI();
		}
	}

	public void makeGoodVisual(VisualizationViewer<IElement, Object> visualizationViewer){
		SysUtils.makeGoodVisual(visualizationViewer, this);
	}

	public void makeMenuBar(VisualizationViewer<IElement, Object> visualizationViewer){
		SysUtils.makeMenuBar(visualizationViewer, this, this.sysRoot);
		this.makeJUnitMenu();
	}

	public boolean rightClickEnabled() {
		return true;
	}

	public JFrame getFrame() {
		return this;
	}

	public void setCenter(Container center) {
		this.center = center;
	}

	public Container getCenter() {
		return center;
	}

	public JTextArea getTextArea(){
		return this.textArea;
	}
}