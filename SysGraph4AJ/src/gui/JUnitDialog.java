

package gui;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import analysis.CoverageAnalysis;
import analysis.FileLoader;

class JUnitDialog extends JDialog implements ActionListener,
		PropertyChangeListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String typedText = null;
	private JTextField junitTF;
	private JButton junitFileChooser;
	private JButton run;
	private JButton cancel;
	private JTextField classPathTF;
	private JTextField classNameTF;

	/**
	 * Returns null if the typed string was invalid; otherwise, returns the
	 * string as the user entered it.
	 */
	public String getValidatedText() {
		return typedText;
	}

	/** Creates the reusable dialog. */
	public JUnitDialog(Frame aFrame) {
		super(aFrame, true);
		setTitle("Importing JUnit Test Cases");

		junitFileChooser = createChooseJUnitButton();
		run = createGoButton();
		cancel = createCancelButton();

		junitTF = new JTextField(50);
		classPathTF = new JTextField(50);
		classNameTF = new JTextField(50);

		// Create an array of the text and components to be displayed.
		String junitMsg = "Choose the JUnit test cases class file and set the additional class paths.";
		String classNameMsg = "Name of the class to get coverage info: ";
		String classPathMsg = "Additional class paths:";

		JPanel panel = new JPanel();

		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		panel.add(new JLabel(junitMsg));
		panel.add(junitTF);
		panel.add(junitFileChooser);
		panel.add(new JLabel(classPathMsg));
		panel.add(classPathTF);
		panel.add(new JLabel(classNameMsg));
		panel.add(classNameTF);

		panel.add(Box.createHorizontalGlue());
		panel.add(run);
		panel.add(Box.createRigidArea(new Dimension(10, 0)));
		panel.add(cancel);

		setContentPane(panel);

		// Handle window closing correctly.
		setDefaultCloseOperation(HIDE_ON_CLOSE);

		// Ensure the text field always gets the first focus.
		addComponentListener(new ComponentAdapter() {
			public void componentShown(ComponentEvent ce) {
				junitTF.requestFocusInWindow();
			}
		});

		// Register an event handler that puts the text into the option pane.
		junitTF.addActionListener(this);

	}

	/** This method clears the dialog and hides it. */
	public void clearAndHide() {
		junitTF.setText(null);
		setVisible(false);
	}

	private JButton createChooseJUnitButton() {
		JButton btn = new JButton("Choose JUnit class");
		btn.addActionListener(new ChoosePathActionListener());
		btn.setSize(new Dimension(80, 30));
		btn.setMaximumSize(new Dimension(80, 30));
		return btn;
	}

	private final class ChoosePathActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			JFileChooser fc = new JFileChooser("." + File.separator + "..");
			fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
				junitTF.setText(fc.getSelectedFile().getAbsolutePath());
			}
		}
	}

	private JButton createGoButton() {
		JButton btn = new JButton("Run");
		btn.addActionListener(new RunActionListener(this));
		btn.setSize(new Dimension(80, 30));
		btn.setMaximumSize(new Dimension(80, 30));
		return btn;
	}

	private final class RunActionListener implements ActionListener {

		JDialog jd;

		public RunActionListener(JDialog j) {
			jd = j;
		}

		public void actionPerformed(ActionEvent e) {
			try {
				FileLoader.getRuntime().startup(FileLoader.getData());
			} catch (Exception e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
				JOptionPane.showMessageDialog(jd, "Cannot startup coverage analysis runtime.");
				return;
			}
			JUnitCore junitCore = new JUnitCore();
			ClassLoader cl = FileLoader.getClassLoader();
			Class<?> test;
			try {
				test = cl.loadClass(junitTF.getText());
			} catch (ClassNotFoundException e1) {
				e1.printStackTrace();
				JOptionPane.showMessageDialog(jd, "JUnit class not found.");
				return;
			}
			long start = System.currentTimeMillis();
			Result r = junitCore.run(test);
			String str = "";
			if (r.getFailureCount() > 0) {
				int failureCount = 0;
				for (int i = 0; i < r.getFailureCount(); i++) {
					String failureMessage = r.getFailures().get(i).getMessage();
					if(failureMessage.startsWith("expected")) {
						str += "\n" + failureMessage;
						failureCount++;
					}
				}
				str = "Failure count = " + failureCount + str;
			}
			str += "\n" + CoverageAnalysis.getCoverage(classNameTF.getText());
			long end = System.currentTimeMillis();
			long overhead = end - start;
			JOptionPane.showMessageDialog(jd, str + "Time: " + overhead + "ms.");
		}
	}

	private JButton createCancelButton() {
		JButton btn = new JButton("Cancel");
		btn.addActionListener(new CancelActionListener(this));
		btn.setSize(new Dimension(80, 30));
		btn.setMaximumSize(new Dimension(80, 30));
		return btn;
	}

	private final class CancelActionListener implements ActionListener {
		JDialog jd;

		public CancelActionListener(JDialog d) {
			jd = d;
		}

		public void actionPerformed(ActionEvent e) {
			jd.setVisible(false);
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		// TODO Auto-generated method stub

	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub

	}
}
