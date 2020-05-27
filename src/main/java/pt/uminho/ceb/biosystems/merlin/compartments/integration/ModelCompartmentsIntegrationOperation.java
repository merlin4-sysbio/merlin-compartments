package pt.uminho.ceb.biosystems.merlin.compartments.integration;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.uvigo.ei.aibench.core.Core;
import es.uvigo.ei.aibench.core.ParamSpec;
import es.uvigo.ei.aibench.core.operation.OperationDefinition;
import es.uvigo.ei.aibench.core.operation.annotation.Cancel;
import es.uvigo.ei.aibench.core.operation.annotation.Direction;
import es.uvigo.ei.aibench.core.operation.annotation.Operation;
import es.uvigo.ei.aibench.core.operation.annotation.Port;
import es.uvigo.ei.aibench.core.operation.annotation.Progress;
import es.uvigo.ei.aibench.workbench.Workbench;
import pt.uminho.ceb.biosystems.merlin.core.containers.model.CompartmentContainer;
import pt.uminho.ceb.biosystems.merlin.core.datatypes.model.compartments.AnnotationCompartmentsGenes;
import pt.uminho.ceb.biosystems.merlin.gui.datatypes.WorkspaceAIB;
import pt.uminho.ceb.biosystems.merlin.gui.utilities.MerlinUtils;
import pt.uminho.ceb.biosystems.merlin.gui.utilities.TimeLeftProgress;
import pt.uminho.ceb.biosystems.merlin.processes.model.compartments.CompartmentsAnnotationIntegrationProcesses;
import pt.uminho.ceb.biosystems.merlin.processes.verifiers.CompartmentsVerifier;
import pt.uminho.ceb.biosystems.merlin.services.DatabaseServices;
import pt.uminho.ceb.biosystems.merlin.services.ProjectServices;
import pt.uminho.ceb.biosystems.merlin.services.annotation.AnnotationCompartmentsServices;
import pt.uminho.ceb.biosystems.merlin.services.model.ModelGenesServices;
import pt.uminho.ceb.biosystems.merlin.utilities.io.FileUtils;

@Operation(name="ModelCompartmentsIntegrationOperation", description="integrate compartments to the model reactions")
public class ModelCompartmentsIntegrationOperation implements PropertyChangeListener {

	private boolean loaded;
	private TimeLeftProgress progress = new TimeLeftProgress();
	private CompartmentsAnnotationIntegrationProcesses integration;
	private List<String> ignoreList;
	private AtomicBoolean cancel;
	private AtomicInteger processingCounter;
	private AtomicInteger querySize; 
	private long startTime;
	private boolean biochemical;
	private boolean transport;
	private WorkspaceAIB workspace;
	private CompartmentContainer membraneCompartment;
	private boolean go = true;
	private boolean cloneWorkspace;
	private String cloneWorkspaceName;
	private String directory;
	private String message;
	private String workspaceName;
	private Integer dataSize = 1;


	final static Logger logger = LoggerFactory.getLogger(ModelCompartmentsIntegrationOperation.class);

	@Port(direction=Direction.INPUT, name="biochemical", order=1)
	public void setBiochemical(boolean biochemical){

		this.biochemical = biochemical;
	};

	@Port(direction=Direction.INPUT, name="transporters", order=2)
	public void setTransporters(boolean transport){

		this.transport = transport;
	};

	@Port(direction=Direction.INPUT, name="ignore", order=3)
	public void setIgnore(List<String> ignore){

		this.ignoreList = ignore;
	};

	@Port(direction=Direction.INPUT, name="project", order=4)
	public void setProject(WorkspaceAIB project){
		try {
			checkProject(project);
		} catch (Exception e) {
			Workbench.getInstance().error(e);
			this.go=false;
			e.printStackTrace();
		}
	};

	@Port(direction=Direction.INPUT, name="compartment",description="name of the default membrane compartment", advanced=true, defaultValue = "auto", order = 5)
	public void setDefaultMembrane(String compartment) {
		try {
			checkMembraneCompartment(compartment);
		} catch (Exception e) {
			Workbench.getInstance().error(e);
			this.go=false;
			e.printStackTrace();
		}
	};

	@Port(direction=Direction.INPUT, name="clone workspace before integrating",defaultValue="false", description="clone the currently selected workspace and integrate compartments in the cloned version",order=6)
	public void setCloneWorkspace(boolean cloneWorkspace){

		this.cloneWorkspace = cloneWorkspace;
	}

	@Port(direction=Direction.INPUT, name="cloned workspace name", description="name of the cloned workspace which will be created",order=7)
	public void setCloneWorkspaceName(String cloneWorkspaceName){

		this.cloneWorkspaceName = cloneWorkspaceName;
	}


	@Port(direction=Direction.INPUT, name="geneCompartments", order=8)
	public void setGeneCompartments(Map<Integer, AnnotationCompartmentsGenes> geneCompartments) throws Exception{

		this.cancel = new AtomicBoolean(false);
		this.startTime = GregorianCalendar.getInstance().getTimeInMillis();
		this.workspaceName = workspace.getName();

		if(this.cloneWorkspace == true)
		{
			try {

				checkIfValidWorkspaceName(this.cloneWorkspaceName);
				initCloning(this.cloneWorkspaceName);
				this.workspaceName = this.cloneWorkspaceName;
			}

			catch(Exception e) {

				Workbench.getInstance().error(e);
				e.printStackTrace();
				this.go = false;

			}
		}


		if(this.go && ProjectServices.isGeneDataAvailable(this.workspaceName)) {

			this.progress = new TimeLeftProgress();
			this.querySize = new AtomicInteger(geneCompartments.size());
			this.processingCounter = new AtomicInteger();
			this.message = "Compartments integration";		


			this.integration = new CompartmentsAnnotationIntegrationProcesses(this.workspaceName, geneCompartments);
			this.integration.addPropertyChangeListener(this);
			this.integration.setQuerySize(this.querySize);
			this.integration.setProcessingCounter(this.processingCounter);
			this.integration.setCancel(this.cancel);
			this.integration.setDefaultMembraneCompartment(this.membraneCompartment);

			boolean result = false;

			if(!this.cancel.get()){
				if(this.loaded)				
					result = integration.initProcessCompartments();
				else
					result = integration.performIntegration();
			}

			this.progress.setTime(0, 0, 0, "processing biochemical reactions");

			if(this.biochemical && !this.cancel.get())
				result = integration.assignCompartmentsToMetabolicReactions(ignoreList);

			System.out.println(result + " outcome of biochemical");

			if(this.transport && ProjectServices.isTransporterLoaded(this.workspaceName) && !this.cancel.get()) {

				try {

					this.progress.setTime(0, 0, 0, "processing transport reactions");

					result = integration.assignCompartmentsToTransportReactions(ignoreList, false);

					System.out.println(result + " outcome of transporters");
				}
				catch (Exception e) {

					result = false;

					System.out.println(result + "error during transporters");

					e.printStackTrace();

					Workbench.getInstance().error(e);
				}
			}

			MerlinUtils.updateCompartmentsAnnotationView(this.workspaceName);
			MerlinUtils.updateReactionsView(this.workspaceName);

			if(result && !this.cancel.get()) {

				Workbench.getInstance().info("Compartments integration complete!");
			}
			else{

				Workbench.getInstance().error("an error occurred while performing the operation.");
			}
		}
		else if(this.go) {
			Workbench.getInstance().error("gene data for integration unavailable!");
		}

	};

	/**
	 * @param workspace
	 */
	public void checkProject(WorkspaceAIB workspace) throws Exception{


		if(workspace == null) {

			throw new IllegalArgumentException("no project selected!");
		}
		else {
			try {

				this.workspace = workspace;

				if(!AnnotationCompartmentsServices.areCompartmentsPredicted(workspace.getName()))
					throw new IllegalArgumentException("please perform the compartments prediction operation before integrating compartments data.");

				int comp_genes = ModelGenesServices.countGenesInGeneHasCompartment(workspace.getName());
				int genes = ModelGenesServices.countEntriesInGene(workspace.getName());

				this.loaded = genes == comp_genes;

			} 
			catch (Exception e) {
				Workbench.getInstance().error(e);
				e.printStackTrace();
			}	
		}
	}


	/**
	 * @param compartment
	 * @throws Exception
	 */
	public void checkMembraneCompartment(String compartment) throws Exception {

		this.membraneCompartment = CompartmentsVerifier.checkMembraneCompartment(compartment, this.workspace.getName(), this.workspace.isEukaryoticOrganism());

		if(this.membraneCompartment == null) {
			//            Workbench.getInstance().warn("No membrane compartmentID defined!");
			logger.warn("No membrane compartmentID defined!");
		}

	}


	public void checkIfValidWorkspaceName(String workspaceName) throws Exception {


		if(workspaceName == null || workspaceName.isEmpty())
			throw new Exception("If you wish to clone the workspace, please enter a name for the clone!");
		else {

			List<String> names = DatabaseServices.getDatabasesAvailable();

			if(names.contains(workspaceName))
				throw new Exception("That workspace name is already in use, please enter a different one!");
			else
				this.cloneWorkspaceName = workspaceName;
		}
	}

	public void initCloning(String cloneWorkspaceName) throws Exception {

		try {

			this.startTime = GregorianCalendar.getInstance().getTimeInMillis();
			boolean cloneSuccess = true;

			String tempDirectory = FileUtils.getCurrentTempDirectory().concat(this.workspaceName).concat("/");

			File tempFile = new File(tempDirectory);

			if(tempFile.exists())
				org.apache.commons.io.FileUtils.deleteDirectory(tempFile);

			this.directory = tempDirectory;

			tempDirectory = tempDirectory.concat("/tables/");

			tempFile = new File(tempDirectory);

			tempFile.mkdirs();

			backupWorkspaceFolder();

			this.message = "exporting database...";
			logger.info(this.message);

			DatabaseServices.databaseToXML(this.workspaceName, tempFile.getAbsolutePath().concat("/"), this);

			try {

				importWorkspaceFolder();

				this.message = "creating new database...";		

				DatabaseServices.generateDatabase(this.cloneWorkspaceName);
				DatabaseServices.dropConnection(this.cloneWorkspaceName);

				DatabaseServices.readxmldb(this.cloneWorkspaceName, tempFile.getAbsolutePath().concat("/"), this.cancel, this);

				File folderDelete = new File(this.directory);
				org.apache.commons.io.FileUtils.deleteDirectory(folderDelete);


			} catch (Exception e) {
				cloneSuccess = false;

				Workbench.getInstance().error(e);
				e.printStackTrace();
			}

			if(cloneSuccess) {

				Integer taxId = ProjectServices.getOrganismID(this.cloneWorkspaceName);

				if(taxId != null) {

					ParamSpec[] paramsSpec = new ParamSpec[]{
							new ParamSpec("Database",String.class,this.cloneWorkspaceName,null),
							new ParamSpec("TaxonomyID",long.class,Long.parseLong(taxId.toString()),null)	
					};

					for (@SuppressWarnings("rawtypes") OperationDefinition def : Core.getInstance().getOperations()){
						if (def.getID().equals("operations.NewWorkspace.ID")){

							Workbench.getInstance().executeOperation(def, paramsSpec);
						}
					}
				}

				Workbench.getInstance().info("Workspace successfully cloned as: '" + cloneWorkspaceName + "'.");
				logger.info("Workspace successfully cloned as " + cloneWorkspaceName);
			}



		} catch (Exception e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Error while cloning the workspace.");
		}
	}


	private void backupWorkspaceFolder() throws IOException {

		logger.info("Copying workspace folder files...");
		String path;
		String destination;

		path = FileUtils.getWorkspaceFolderPath(this.workspace.getName());
		destination = this.directory;
		new File(destination).mkdirs();

		File p = new File(path);
		File d = new File(destination);

		org.apache.commons.io.FileUtils.copyDirectory(p, d);

	}


	private void importWorkspaceFolder() throws Exception {

		List <String> foldercontent = FileUtils.getFilesFromFolder(this.directory, false);
		String folder = foldercontent.get(0);
		String importWS = this.directory.concat(folder);
		logger.info("Starting the ws folder files import...");
		File copy = new File(importWS);
		String pst = FileUtils.getWorkspacesFolderPath().concat(this.cloneWorkspaceName);

		File paste = new File(pst);

		if(paste.exists())
			org.apache.commons.io.FileUtils.deleteDirectory(paste);

		pst = pst.concat("/").concat(folder);
		paste = new File(pst);

		org.apache.commons.io.FileUtils.copyDirectory(copy, paste);

	}

	/**
	 * @return
	 */
	@Progress(progressDialogTitle = "Compartments integration", modal = false, workingLabel = "integrating compartments", preferredWidth = 400, preferredHeight=300)
	public TimeLeftProgress getProgress() {

		return progress;
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

	/**
	 * 
	 */
	@Cancel
	public void cancel() {

		Workbench.getInstance().warn("operation canceled!");

		this.progress.setTime(0,1,1);
		this.integration.cancel();
	}
}
