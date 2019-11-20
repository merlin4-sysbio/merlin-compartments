package pt.uminho.ceb.biosystems.merlin.aibench.operations.loaders.annotation.compartments_new;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import es.uvigo.ei.aibench.core.Core;
import es.uvigo.ei.aibench.core.ParamSpec;
import es.uvigo.ei.aibench.core.ProgressHandler;
import es.uvigo.ei.aibench.core.operation.OperationDefinition;
import es.uvigo.ei.aibench.core.operation.annotation.Direction;
import es.uvigo.ei.aibench.core.operation.annotation.Operation;
import es.uvigo.ei.aibench.core.operation.annotation.Port;
import es.uvigo.ei.aibench.workbench.Workbench;
import pt.uminho.ceb.biosystems.merlin.aibench.datatypes.WorkspaceAIB;
import pt.uminho.ceb.biosystems.merlin.aibench.utilities.AIBenchUtils;
import pt.uminho.ceb.biosystems.merlin.core.utilities.Enumerators.SequenceType;
import pt.uminho.ceb.biosystems.merlin.services.model.ModelGenesServices;
import pt.uminho.ceb.biosystems.merlin.services.model.ModelSequenceServices;

@Operation(name="Load Reports",description="Load reports")
public class ChooseToolCompartmentalization {


	private String tool;
	private WorkspaceAIB workspace;
	private HashMap<String, String> operationsCompartments;
	private File file;
	private String link;

	@Port(direction=Direction.INPUT, name="Workspace",description="", validateMethod = "checkProject", order = 1)
	public void setWorkspace(String workspace) {

	
	}


	@Port(direction=Direction.INPUT, name="Tool",description="", order = 2)
	public void setTool(String tool) {

		this.tool = tool;

		this.operationsCompartments = new HashMap<String,String>();

		this.operationsCompartments.put("LocTree3", "operations.LoadLocTreeReports.ID");

		this.operationsCompartments.put("WoLFPSORT", "operations.LoadWoLFPSORTReports.ID");

		this.operationsCompartments.put("PSortb3", "operations.LoadPSortReports.ID");

		if (tool.equals("PSortb3")) {

			FilePathPopUp filePath = new FilePathPopUp();

			File file = filePath.getFile();

			this.file = file;

			List<OperationDefinition<?>> allOperations = Core.getInstance().getOperations();

			for (OperationDefinition<?> operation : allOperations) {
				if(operation.getID().equals(this.operationsCompartments.get(this.tool))){
					ParamSpec[] specs = new ParamSpec[]{ 
							new ParamSpec("File", File.class,this.file,null),
							new ParamSpec("workspace", WorkspaceAIB.class,workspace,null)
							};
					
					ProgressHandler handler = null;
					Core.getInstance().executeOperation(operation, handler, specs);
				}
						
			}
			
			
		}
		else {
			
			UrlPopUp url = new UrlPopUp();
			
			this.link=url.getLink();
			
			List<OperationDefinition<?>> allOperations = Core.getInstance().getOperations();

			for (OperationDefinition<?> operation : allOperations) {
				if(operation.getID().equals(this.operationsCompartments.get(this.tool))){
					ParamSpec[] specs = new ParamSpec[]{ 
							new ParamSpec("Link", String.class,this.link,null),
							new ParamSpec("workspace", WorkspaceAIB.class,workspace,null)
							};
					
					ProgressHandler handler = null;
					Core.getInstance().executeOperation(operation, handler, specs);
				}
						
			}
			
		

	}

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

}





