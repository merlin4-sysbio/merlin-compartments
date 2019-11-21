package pt.uminho.ceb.biosystems.merlin.aibench.operations.loaders.annotation.compartments;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.uvigo.ei.aibench.core.operation.annotation.Cancel;
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
import pt.uminho.ceb.biosystems.merlin.processes.model.compartments.services.ComparmentsImportLocTreeServices;
import pt.uminho.ceb.biosystems.merlin.services.ProjectServices;
import pt.uminho.ceb.biosystems.merlin.services.model.ModelSequenceServices;

public class  LoadLocTreeReports {


	private String link;
	private String tool = "LocTree";
	private TimeLeftProgress progress = new TimeLeftProgress();
	private ICompartmentsServices compartmentsInterface;
	private AtomicBoolean cancel = new AtomicBoolean(false);
	boolean error=false;
	private WorkspaceAIB project;
	Map<String, ICompartmentResult> results = new HashMap<>();
	final static Logger logger = LoggerFactory.getLogger(LoadLocTreeReports.class);

	public LoadLocTreeReports(WorkspaceAIB workspace, String link) {
		
		this.link = link;
		this.project = workspace;
		try {
			run();
		} catch (Exception e) {
			logger.error(e.getMessage());
			Workbench.getInstance().error(e);
			e.printStackTrace();
		}
		
	}
	
	
	/**
	 * @param project
	 * @throws Exception 
	 */
	public void run() throws Exception {

		if(!this.cancel.get()) {
			this.compartmentsInterface = new ComparmentsImportLocTreeServices(project.getName());

			ProjectServices.updateCompartmentsTool(project.getName(), project.getTaxonomyID(), tool);
		}

		if(!this.cancel.get()) {

			String kg = project.getOrganismLineage().split(",")[0];
			if (kg.contains("Viridiplantae"))
				compartmentsInterface.setPlant(true);
			error = false;
		}

		if(!this.cancel.get()) {
			if(link != null && !link.equals("")) {

				Map<String, ICompartmentResult> tempResults = null;

				try {

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
			Workbench.getInstance().warn("compartments prediction cancelled");
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
	
	@Cancel
	public void cancel(){
		
		String[] options = new String[2];
		options[0] = "yes";
		options[1] = "no";
		
		int result = CustomGUI.stopQuestion("Cancel confirmation", "Are you sure you want to cancel the operation?", options);
		
		if(result == 0) {
			
			this.progress.setTime(0,1,1);
			this.compartmentsInterface.setCancel(new AtomicBoolean(true));
			
			
		}
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
