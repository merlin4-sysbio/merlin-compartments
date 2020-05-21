package pt.uminho.ceb.biosystems.merlin.compartments.integration;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import org.sing_group.gc4s.dialog.AbstractInputJDialog;
import org.sing_group.gc4s.input.InputParameter;
import org.sing_group.gc4s.input.InputParametersPanel;
import org.sing_group.gc4s.input.combobox.ExtendedJComboBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.uvigo.ei.aibench.core.Core;
import es.uvigo.ei.aibench.core.ParamSpec;
import es.uvigo.ei.aibench.core.clipboard.ClipboardItem;
import es.uvigo.ei.aibench.core.operation.OperationDefinition;
import es.uvigo.ei.aibench.workbench.InputGUI;
import es.uvigo.ei.aibench.workbench.ParamsReceiver;
import es.uvigo.ei.aibench.workbench.Workbench;
import pt.uminho.ceb.biosystems.merlin.gui.datatypes.WorkspaceAIB;
import pt.uminho.ceb.biosystems.merlin.gui.jpanels.CustomGUI;
import pt.uminho.ceb.biosystems.merlin.gui.utilities.CreateImageIcon;


public class ChooseWorkspaceGUI extends AbstractInputJDialog implements InputGUI {

	private static final long serialVersionUID = 1L;
	private ExtendedJComboBox<String> models;
	private ExtendedJComboBox<Double> thresholds;
	private ParamsReceiver rec;
	protected Object project;
	private String[] workspaces;
	final static Logger logger = LoggerFactory.getLogger(ChooseWorkspaceGUI.class);


	public ChooseWorkspaceGUI() {

		super(new JFrame());
	}

	@Override
	public JPanel getInputComponentsPane() {

		List<ClipboardItem> cl = Core.getInstance().getClipboard().getItemsByClass(WorkspaceAIB.class);                    
		
		
		// Workspace selector
		workspaces = new String[cl.size()];
		for (int i = 0; i < cl.size(); i++) {

			workspaces[i] = (cl.get(i).getName());
		}
		
		this.models = new ExtendedJComboBox<String>(workspaces);
		
		this.models.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent e) {
			}
		});
		
		
		// Threshold selector
		
		Double[] thresholdValues = new Double[] {0.0, 0.1 , 0.2 , 0.3, 0.4, 0.5};
		
		this.thresholds = new ExtendedJComboBox<Double>(thresholdValues);
		this.thresholds.setSelectedIndex(1);
		this.thresholds.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent e) {
			}
		});
		
		InputParameter[] inPar = getInputParameters();
		return new InputParametersPanel(inPar);	
	}


	@Override
	protected Component getButtonsPane() {
		
		final JPanel buttonsPanel = new JPanel(new FlowLayout());
		okButton = new JButton("proceed");
		okButton.setEnabled(true);
		okButton.setToolTipText("proceed");
		okButton.setIcon(new CreateImageIcon(new ImageIcon(getClass().getClassLoader().getResource("icons/ok.png")),0.1).resizeImageIcon());
		ActionListener listener= new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				dispose();

				rec.paramsIntroduced(
						new ParamSpec[]{
								new ParamSpec("workspace", String.class,models.getSelectedItem().toString(),null),
								new ParamSpec("threshold", Double.class,thresholds.getSelectedItem() ,null)});}
		};
		okButton.addActionListener(listener);
		cancelButton = new JButton("cancel");
		cancelButton.setToolTipText("cancel");
		cancelButton.setIcon(new CreateImageIcon(new ImageIcon(getClass().getClassLoader().getResource("icons/cancel.png")),0.1).resizeImageIcon());
		cancelButton.addActionListener(event -> {
			String[] options = new String[2];
			options[0] = "yes";
			options[1] = "no";
			int result = CustomGUI.stopQuestion("cancel confirmation", "are you sure you want to cancel the operation?", options);
			if(result == 0) {
				canceled = true;
				dispose();
			}
		});
		buttonsPanel.add(okButton);
		buttonsPanel.add(cancelButton);
		getRootPane().setDefaultButton(okButton);
		InputMap im = okButton.getInputMap();
		im.put(KeyStroke.getKeyStroke("ENTER"), "pressed");
		im.put(KeyStroke.getKeyStroke("released ENTER"), "released");
		return buttonsPanel;
	}

	private InputParameter[] getInputParameters() {
		InputParameter[] parameters = new InputParameter[2];
		
		parameters[0] = 
				new InputParameter(
						"workspace", 
						models, 
						"select the workspace"
						);
		
		parameters[1] = 
				new InputParameter(
						"threshold", 
						thresholds, 
						"Select the maximum allowed score difference (primary score - secondary score)"
						);
		return parameters;
	}


	@Override
	public void init(ParamsReceiver receiver, OperationDefinition<?> operation) {
		this.rec = receiver;
		this.setTitle(operation.getName());
		this.setVisible(true);			
	}


	@Override
	public void onValidationError(Throwable t) {
		Workbench.getInstance().error(t);
	}


	@Override
	public void finish() {
	}


	@Override
	protected String getDescription() {
		return "Integrate the compartment prediction reports' data in the model";
	}


	@Override
	protected String getDialogTitle() {
		return "Integrate the compartment prediction reports' data in the model";
	}

	@Override
	public void setVisible(boolean b) {
		this.pack();
		super.setVisible(b);
	}
}
