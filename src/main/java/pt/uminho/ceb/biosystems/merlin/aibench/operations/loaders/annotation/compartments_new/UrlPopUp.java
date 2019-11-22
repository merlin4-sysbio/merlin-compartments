package pt.uminho.ceb.biosystems.merlin.aibench.operations.loaders.annotation.compartments_new;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;

import es.uvigo.ei.aibench.workbench.Workbench;
import es.uvigo.ei.aibench.workbench.utilities.Utilities;

public class UrlPopUp extends javax.swing.JDialog{

	/**
	 * 
	 */
	private static final long serialVersionUID = 6479317489417299098L;
	private Object url;
	private String tool;

	public UrlPopUp(String tool) {


		super(Workbench.getInstance().getMainFrame());
		this.tool = tool;
		this.url=null;
		initGUI();
		Utilities.centerOnOwner(this);
		this.setIconImage((new ImageIcon(getClass().getClassLoader().getResource("icons/merlin.png"))).getImage());
		this.setVisible(true);		
		this.setAlwaysOnTop(true);
		this.toFront();
	}

	public void initGUI() {



		this.setModal(true);
		JPanel jPanel1;
		{
			this.setTitle("predictions url");
			jPanel1 = new JPanel();
			getContentPane().add(jPanel1, BorderLayout.CENTER);
			GridBagLayout jPanel1Layout = new GridBagLayout();
			jPanel1Layout.columnWeights = new double[] {0.0, 0.1, 0.0};
			jPanel1Layout.columnWidths = new int[] {7, 7, 7};
			jPanel1Layout.rowWeights = new double[] {0.1, 0.0, 0.1};
			jPanel1Layout.rowHeights = new int[] {7, 7, 7};
			jPanel1.setLayout(jPanel1Layout);
		}

		JPanel jPanel11;
		{
			jPanel11 = new JPanel();
			GridBagLayout jPanel11Layout = new GridBagLayout();
			jPanel11.setLayout(jPanel11Layout);
			jPanel11Layout.columnWeights = new double[] {0.0, 0.1, 0.0};
			jPanel11Layout.columnWidths = new int[] {7, 7, 7};
			jPanel11Layout.rowWeights = new double[] {0.0, 0.1, 0.0};
			jPanel11Layout.rowHeights = new int[] {7, 7, 7};

			jPanel1.add(jPanel11, new GridBagConstraints(0, 0, 3, 2, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		}

		
		JTextArea textfield = null;
		if (this.tool.equals("LocTree3")) {
			
			textfield = new JTextArea("\nIntroduce the link from LocTree3 prediction.\n"
					+ "To do that you have to go to https://rostlab.org/services/loctree3/.\n"
					+ "Put the information about the organism, including the proteome.\n"
					+ "Wait for the results.\n"
					+ "Then copy the link and paste it into the text box."
					);
			
			textfield.setBorder(jPanel11.getBorder());
			textfield.setEditable(false);
			textfield.setForeground(jPanel11.getForeground());
			textfield.setFont(UIManager.getFont("Label.font"));
			textfield.setBackground(jPanel11.getBackground());
			textfield.setVisible(true);
		}
		
		if (this.tool.equals("WoLFPSORT")) {
			
			textfield = new JTextArea("\nIntroduce the link from WoLFPSORT prediction.\n"
					+ "To do that you have to go to https://wolfpsort.hgc.jp/.\n"
					+ "Put the information about the organism, including the proteome.\n"
					+ "Wait for the results.\n"
					+ "Then copy the link and paste it into the text box"
					);
			
			// give the textfield the impression it's a label
			textfield.setBorder(jPanel11.getBorder());
			textfield.setEditable(false);
			textfield.setForeground(jPanel11.getForeground());
			textfield.setFont(UIManager.getFont("Label.font"));
			textfield.setBackground(jPanel11.getBackground());
			textfield.setVisible(true);
		}
		
		jPanel11.add(textfield, new GridBagConstraints(1, 0, 1, 2, 0.0, 5, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

		JLabel urlLabel = new JLabel("URL: ");
		jPanel11.add(urlLabel, new GridBagConstraints(0, 2, 1, 2, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		
		JTextField jTextField1 = new JTextField();
		jTextField1.setEditable(true);
		jPanel11.add(jTextField1, new GridBagConstraints(1, 2, 1, 2, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		
		
		JButton buttonProceed = new JButton("proceed");
		jPanel11.add(buttonProceed, new GridBagConstraints(1, 5, 1, 2, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.CENTER, new Insets(0, 0, 0, 0), 0, 0));
		buttonProceed.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setLink(jTextField1.getText());
				finish();
			}
		});

		this.setSize(480, 230);
	}
	private void setLink(String file) {
		this.url=file;
	}

	public String getLink() {
		return (String) this.url;
	}

	public void finish() {
		
		this.setVisible(false);
		this.dispose();
	}
}
