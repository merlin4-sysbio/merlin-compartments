package pt.uminho.ceb.biosystems.merlin.compartments.integration;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.GregorianCalendar;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.uvigo.ei.aibench.core.operation.annotation.Cancel;
import es.uvigo.ei.aibench.core.operation.annotation.Direction;
import es.uvigo.ei.aibench.core.operation.annotation.Operation;
import es.uvigo.ei.aibench.core.operation.annotation.Port;
import es.uvigo.ei.aibench.workbench.Workbench;
import pt.uminho.ceb.biosystems.merlin.core.datatypes.WorkspaceEntity;
import pt.uminho.ceb.biosystems.merlin.core.datatypes.annotation.AnnotationCompartments;
import pt.uminho.ceb.biosystems.merlin.dao.implementation.annotation.compartments.CompartmentsAnnotationReportsHasCompartmentsDAOImpl;
import pt.uminho.ceb.biosystems.merlin.entities.annotation.compartments.CompartmentsAnnotationReportsHasCompartments;
import pt.uminho.ceb.biosystems.merlin.gui.datatypes.WorkspaceAIB;
import pt.uminho.ceb.biosystems.merlin.gui.datatypes.model.ModelReactionsAIB;
import pt.uminho.ceb.biosystems.merlin.gui.jpanels.CustomGUI;
import pt.uminho.ceb.biosystems.merlin.gui.utilities.AIBenchUtils;
import pt.uminho.ceb.biosystems.merlin.gui.utilities.TimeLeftProgress;
import pt.uminho.ceb.biosystems.merlin.processes.model.compartments.services.CompartmentsAnnotationServices;
import pt.uminho.ceb.biosystems.merlin.services.ProjectServices;
import pt.uminho.ceb.biosystems.merlin.services.annotation.AnnotationCompartmentsServices;
import pt.uminho.ceb.biosystems.merlin.services.annotation.AnnotationEnzymesServices;


@Operation(name="ChooseWorkspaceOperation", description="")
public class ChooseWorkspaceOperation implements PropertyChangeListener {

	private long startTime;
	private String message;
	private AtomicBoolean cancel = new AtomicBoolean(false);
	private int dataSize;
	private WorkspaceAIB project;
	private double threshold;
	public TimeLeftProgress progress = new TimeLeftProgress();
	final static Logger logger = LoggerFactory.getLogger(ChooseWorkspaceOperation.class);


	@Port(direction=Direction.INPUT, name="Workspace",description="select the workspace", order = 1)
	public void setNewProject(String projectName) throws Exception {

		this.project = AIBenchUtils.getProject(projectName);

	}

	@Port(direction=Direction.INPUT, name="Secondary compartments allowed difference",description="Select the maximum allowed difference (primary score - secondary score)", order = 2)
	public void setThreshold(double thresh) throws Exception {
		
		this.threshold = thresh;
		this.startTime = GregorianCalendar.getInstance().getTimeInMillis();
		this.cancel = new AtomicBoolean(false);

		try {

			boolean areCompartmentsPredicted = AnnotationCompartmentsServices.areCompartmentsPredicted(this.project.getName());

			if(!this.cancel.get()) {
				if(areCompartmentsPredicted) {
					boolean isCompartmentalizedModel = ProjectServices.isCompartmentalisedModel(this.project.getName());
					if(!isCompartmentalizedModel)
						new ModelCompartmentsIntegrationGUI(project.getName(), this.threshold);
					else
						Workbench.getInstance().error("You have already integrated the compartments, if you wish to repeat this operation please clean the integration first");
				}

				else {
					Workbench.getInstance().error("Please make sure you have loaded a compartment prediction report in this workspace");
				}
			}
			else {
				Workbench.getInstance().warn("workspace selection canceled");
			}

		} 
		catch (Exception e) {
			e.printStackTrace();
			Workbench.getInstance().info("an error occurred while performing the operation.");
		}
	}


	@Override
	public void propertyChange(PropertyChangeEvent evt) {

		if(evt.getPropertyName().equalsIgnoreCase("message"))
			this.message = (String) evt.getNewValue();

		if(evt.getPropertyName().equalsIgnoreCase("size")) {
			this.dataSize = (int) evt.getNewValue();
		}

		if(evt.getPropertyName().equalsIgnoreCase("tablesCounter")) {

			int tablesCounter = (int) evt.getNewValue();
			this.progress.setTime((GregorianCalendar.getInstance().getTimeInMillis() - startTime), tablesCounter, dataSize, this.message);
		}
	}

	@Cancel
	public void cancel() {

		String[] options = new String[2];
		options[0] = "yes";
		options[1] = "no";

		int result = CustomGUI.stopQuestion("Cancel confirmation", "Are you sure you want to cancel the operation?", options);

		if (result == 0) {

			this.progress.setTime((GregorianCalendar.getInstance().getTimeInMillis()-GregorianCalendar.getInstance().getTimeInMillis()),1,1);
			this.cancel.set(true);

			Workbench.getInstance().warn("Please hold on. Your operation is being cancelled.");
		}


	}
}
