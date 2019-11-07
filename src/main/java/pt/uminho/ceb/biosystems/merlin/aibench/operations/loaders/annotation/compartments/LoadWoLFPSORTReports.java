package pt.uminho.ceb.biosystems.merlin.aibench.operations.loaders.annotation.compartments;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import es.uvigo.ei.aibench.core.operation.annotation.Cancel;
import es.uvigo.ei.aibench.core.operation.annotation.Direction;
import es.uvigo.ei.aibench.core.operation.annotation.Operation;
import es.uvigo.ei.aibench.core.operation.annotation.Port;
import es.uvigo.ei.aibench.core.operation.annotation.Progress;
import es.uvigo.ei.aibench.workbench.Workbench;
import pt.uminho.ceb.biosystems.merlin.aibench.datatypes.WorkspaceAIB;
import pt.uminho.ceb.biosystems.merlin.aibench.datatypes.annotation.AnnotationCompartmentsAIB;
import pt.uminho.ceb.biosystems.merlin.aibench.gui.CustomGUI;
import pt.uminho.ceb.biosystems.merlin.aibench.utilities.MerlinUtils;
import pt.uminho.ceb.biosystems.merlin.aibench.utilities.TimeLeftProgress;
import pt.uminho.ceb.biosystems.merlin.core.interfaces.ICompartmentResult;
import pt.uminho.ceb.biosystems.merlin.core.utilities.Enumerators.SequenceType;
import pt.uminho.ceb.biosystems.merlin.processes.WorkspaceProcesses;
import pt.uminho.ceb.biosystems.merlin.processes.model.compartments.interfaces.ICompartmentsServices;
import pt.uminho.ceb.biosystems.merlin.processes.model.compartments.services.ComparmentsImportWolfPsortServices;
import pt.uminho.ceb.biosystems.merlin.services.ProjectServices;
import pt.uminho.ceb.biosystems.merlin.services.model.ModelSequenceServices;

@Operation(name="Load Compartments' Reports", description="Load compartments reports' to merlin.")
public class LoadWoLFPSORTReports {


	private String link;
	private String tool = "WoLFPSORT";
	private TimeLeftProgress progress = new TimeLeftProgress();
	private ICompartmentsServices compartmentsInterface;
	private AtomicBoolean cancel = new AtomicBoolean(false);

	/**
	 * @param file_dir
	 * @throws Exception 
	 */
	@Port(direction=Direction.INPUT, name="predictions link",description="predictions website link", validateMethod="checkFiles",order=2)
	public void setFile_dir(String link) throws Exception {

	}

	/**
	 * @param project
	 * @throws Exception 
	 */
	@Port(direction=Direction.INPUT, name="workspace",description="select workspace",validateMethod="checkProject", order=3)
	public void setProject(WorkspaceAIB project) throws Exception {

		if(!this.cancel.get()) {
			ProjectServices.updateCompartmentsTool(project.getName(), project.getTaxonomyID(), tool);
		}
		Map<String, ICompartmentResult> results = new HashMap<>();

		boolean error = false;

		if(!this.cancel.get()) {
			if(link != null && !link.equals("")) {

				Map<String, ICompartmentResult> tempResults = null;

				try {
					this.compartmentsInterface = new ComparmentsImportWolfPsortServices(project.getName());

					tempResults = compartmentsInterface.addGeneInformation(link);

				}
				catch (Exception e) {

					error=true;
					e.printStackTrace();
				}

				if(tempResults!=null)
					results.putAll(tempResults); 				
			}
		}

		if(!this.cancel.get()) {

			if(error) {

				Workbench.getInstance().error("An error occurred when performing the operation!");
			}
			else {

				if(results.isEmpty()) {

					Workbench.getInstance().warn("merlin could not find any compartments information, skipping results loading!");
				}
				else {

					AnnotationCompartmentsAIB.loadPredictions(project.getName(), project.getOrganismLineage(), tool, results);

					MerlinUtils.updateCompartmentsAnnotationView(project.getName());
					Workbench.getInstance().info("compartments prediction loaded.");
				}
			}
		}else {
			Workbench.getInstance().warn("compartments prediction cancelled.");
		}
	}

	/**
	 * @return
	 */
	@Progress
	public TimeLeftProgress getProgress() {

		return this.progress;
	}

	/**
	 * 
	 */
	@Cancel
 	public void setCancel(){
		
		String[] options = new String[2];
		options[0] = "yes";
		options[1] = "no";
		
		int result = CustomGUI.stopQuestion("Cancel confirmation", "Are you sure you want to cancel the operation?", options);
		
		if (result == 0) {
			
			this.progress.setTime(0,1,1);
			this.cancel.set(true);
			
		}
	}


	/**
	 * @param project
	 * @throws Exception 
	 */
	public void checkProject(WorkspaceAIB project) throws Exception {

		if(project == null)
			throw new IllegalArgumentException("no project selected!");

		if(!ModelSequenceServices.checkGenomeSequences(project.getName(), SequenceType.PROTEIN)) {
			throw new IllegalArgumentException("please set the project fasta ('.faa' or '.fna') files");
		}
		if(project.getTaxonomyID()<0) {

			throw new IllegalArgumentException("please enter the taxonomic identification from NCBI taxonomy");
		}
		
		WorkspaceProcesses.createFaaFile(project.getName(), project.getTaxonomyID()); // method creates ".faa" files only if they do not exist 
	}

	/**
	 * @param project
	 * @throws Exception 
	 */
	public void checkFiles(String link) {

		if(link == null)
			throw new IllegalArgumentException("please set valid link");
		else
			this.link = link;
	}
}
