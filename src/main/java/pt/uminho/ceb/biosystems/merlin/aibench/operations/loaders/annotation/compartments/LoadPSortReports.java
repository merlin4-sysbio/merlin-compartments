package pt.uminho.ceb.biosystems.merlin.aibench.operations.loaders.annotation.compartments;

import java.io.File;
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
import pt.uminho.ceb.biosystems.merlin.processes.model.compartments.interfaces.ICompartmentsServices;
import pt.uminho.ceb.biosystems.merlin.processes.model.compartments.services.ComparmentsImportPSort3Services;
import pt.uminho.ceb.biosystems.merlin.services.ProjectServices;

@Operation(name="Load Compartments' Reports", description="Load compartments reports' to merlin.")
public class LoadPSortReports {


	private File outFile;
	private TimeLeftProgress progress = new TimeLeftProgress();
	//private long startTime;
	private String tool = "PSort";
	private ICompartmentsServices compartmentsInterface;

	/**
	 * @param file_dir
	 * @throws Exception 
	 */
	@Port(direction=Direction.INPUT, name="predictions file",description="Long Format (tab delimited)", validateMethod="checkFiles",order=2)
	public void setFile_dir(File outFile) throws Exception {

	}

	/**
	 * @param project
	 * @throws Exception 
	 */
	@Port(direction=Direction.INPUT, name="workspace",description="select workspace",validateMethod="checkProject", order=3)
	public void setProject(WorkspaceAIB project) throws Exception {

		ProjectServices.updateCompartmentsTool(project.getName(), project.getTaxonomyID(), tool);

		Map<String, ICompartmentResult> results = new HashMap<>();

		boolean error = false;

		if(outFile.getName().endsWith(".out") 
				|| outFile.getName().endsWith(".psort")
				|| outFile.getName().endsWith(".txt")) {
			
			this.compartmentsInterface = new ComparmentsImportPSort3Services(project.getName());

			Map<String, ICompartmentResult> tempResults = null;

			try {

				tempResults = compartmentsInterface.addGeneInformation(outFile);
				
			}
			catch (Exception e) {

				error=true;
				e.printStackTrace();
			}

			if(tempResults!=null)
				results.putAll(tempResults);
		}


		if(!this.compartmentsInterface.isCancel().get()) {
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
	 */
	public void checkProject(WorkspaceAIB project) {

		if(project == null)
			throw new IllegalArgumentException("no project selected!");
		
//		if(!WorkspaceProcesses.isFaaFiles(project.getName(), project.getTaxonomyID()))
//			throw new IllegalArgumentException("Please set amino acid fasta files!");
	}

	/**
	 * @param project
	 * @throws Exception 
	 */
	public void checkFiles(File outFile) {

		if(outFile == null || outFile.isDirectory())
			throw new IllegalArgumentException("please set a single file");
		else
			this.outFile = outFile;
	}
}
