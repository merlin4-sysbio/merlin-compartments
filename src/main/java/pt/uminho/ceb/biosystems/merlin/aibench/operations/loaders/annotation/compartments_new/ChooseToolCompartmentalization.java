package pt.uminho.ceb.biosystems.merlin.aibench.operations.loaders.annotation.compartments_new;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import es.uvigo.ei.aibench.core.Core;
import es.uvigo.ei.aibench.core.ParamSpec;
import es.uvigo.ei.aibench.core.ProgressHandler;
import es.uvigo.ei.aibench.core.operation.OperationDefinition;
import es.uvigo.ei.aibench.core.operation.annotation.Cancel;
import es.uvigo.ei.aibench.core.operation.annotation.Direction;
import es.uvigo.ei.aibench.core.operation.annotation.Operation;
import es.uvigo.ei.aibench.core.operation.annotation.Port;
import es.uvigo.ei.aibench.core.operation.annotation.Progress;
import es.uvigo.ei.aibench.workbench.Workbench;
import pt.uminho.ceb.biosystems.merlin.aibench.datatypes.WorkspaceAIB;
import pt.uminho.ceb.biosystems.merlin.aibench.gui.CustomGUI;
import pt.uminho.ceb.biosystems.merlin.aibench.operations.loaders.annotation.compartments.LoadLocTreeReports;
import pt.uminho.ceb.biosystems.merlin.aibench.operations.loaders.annotation.compartments.LoadPSortReports;
import pt.uminho.ceb.biosystems.merlin.aibench.operations.loaders.annotation.compartments.LoadWoLFPSORTReports;
import pt.uminho.ceb.biosystems.merlin.aibench.utilities.AIBenchUtils;
import pt.uminho.ceb.biosystems.merlin.aibench.utilities.TimeLeftProgress;
import pt.uminho.ceb.biosystems.merlin.processes.model.compartments.interfaces.ICompartmentsServices;
import pt.uminho.ceb.biosystems.merlin.services.annotation.AnnotationCompartmentsServices;

@Operation(name="Load Reports",description="Load reports")
public class ChooseToolCompartmentalization {

	private ICompartmentsServices compartmentsInterface;
	private String tool;
	private WorkspaceAIB workspace;
	private HashMap<String, String> operationsCompartments;
	private File file;
	private String link;
	private TimeLeftProgress progress;
	private AtomicBoolean cancel = new AtomicBoolean(false);

	@Port(direction=Direction.INPUT, name="Workspace",description="", order = 1)
	public void setWorkspace(String workspace) {

		this.workspace = AIBenchUtils.getProject(workspace);
	}


	@Port(direction=Direction.INPUT, name="Tool",description="", order = 2)
	public void setTool(String tool) throws Exception {

		this.tool = tool;
		
		boolean go = AnnotationCompartmentsServices.areCompartmentsPredicted(this.workspace.getName());

		if (!go) {
		if (this.tool.equals("PSortb3")) {

			FilePathPopUp filePath = new FilePathPopUp();

			File file = filePath.getFile();

			this.file = file;

			new LoadPSortReports(this.workspace,this.file);

		}
		else {

			UrlPopUp url = new UrlPopUp();

			this.link=url.getLink();

			if (this.tool.equals("LocTree3")) 
				new LoadLocTreeReports(this.workspace,this.link);
			else
				new LoadWoLFPSORTReports(this.workspace,this.link);
			
			
		}
		}
		else
			Workbench.getInstance().warn("Compartments have been predicted already. \n"
					+ "Please go to \"Workspace\" menu, press \"Clean\".\n"
					+ "Select the correct workspace and then, in \"select information\", select \"compartment annotation\". Press \"OK\" ");


	}
	
	@Progress
	public TimeLeftProgress getProgress() {

		return this.progress;
	}
	
	@Cancel
	public void setCancel(){
		
		String[] options = new String[2];
		options[0] = "yes";
		options[1] = "no";
		
		int result = CustomGUI.stopQuestion("Cancel confirmation", "Are you sure you want to cancel the operation?", options);
		
		if (result == 0) {
			
			//this.progress.setTime(0,1,1);
			this.cancel.set(true);
			this.compartmentsInterface.setCancel(new AtomicBoolean(true));
		}
	}
}





