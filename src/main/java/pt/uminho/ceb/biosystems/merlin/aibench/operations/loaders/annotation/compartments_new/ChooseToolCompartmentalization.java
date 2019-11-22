package pt.uminho.ceb.biosystems.merlin.aibench.operations.loaders.annotation.compartments_new;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

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
import pt.uminho.ceb.biosystems.merlin.services.model.ModelGenesServices;

@Operation(name="Load Reports",description="Load reports")
public class ChooseToolCompartmentalization {

	private ICompartmentsServices compartmentsInterface;
	private String tool;
	private WorkspaceAIB workspace;
	private File file;
	private String link;
	private TimeLeftProgress progress;
	private AtomicBoolean cancel = new AtomicBoolean(false);

	@Port(direction=Direction.INPUT, name="Workspace",description="", validateMethod = "checkProject", order = 1)
	public void setWorkspace(String workspace) {

	
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
			if (!this.cancel.get() && this.file!=null)
				new LoadPSortReports(this.workspace,this.file);

		}
		else {

			UrlPopUp url = new UrlPopUp(this.tool);

			this.link=url.getLink();
			
			if (!this.cancel.get()) {
				if (this.tool.equals("LocTree3"))
					new LoadLocTreeReports(this.workspace,this.link);
				else
					new LoadWoLFPSORTReports(this.workspace,this.link);
			}

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

	/**
	 * @param project
	 * @throws Exception 
	 * @throws IOException 
	 */
	public void checkProject(String project) throws IOException, Exception {

		this.workspace = AIBenchUtils.getProject(project);
		
		if(this.workspace == null) {

			throw new IllegalArgumentException("No ProjectGUISelected!");
		}
		else {

				if(!ModelGenesServices.existGenes(this.workspace.getName())){//!Project.isFaaFiles(dbName,taxID) && !Project.isFnaFiles(dbName,taxID)) {
					
					throw new IllegalArgumentException("please set the genome fasta file ('.faa')");
					
				}
				else if(this.workspace.getTaxonomyID()<0) {

					throw new IllegalArgumentException("please enter the organism taxonomic identification from NCBI taxonomy to perform this operation");
				}

		}
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





