package pt.uminho.ceb.biosystems.merlin.compartments.services;

import java.sql.Statement;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import es.uvigo.ei.aibench.core.operation.annotation.Cancel;
import es.uvigo.ei.aibench.core.operation.annotation.Operation;
import es.uvigo.ei.aibench.core.operation.annotation.Port;
import es.uvigo.ei.aibench.core.operation.annotation.Progress;
import es.uvigo.ei.aibench.workbench.Workbench;
import pt.uminho.ceb.biosystems.merlin.aibench.datatypes.ProjectGUI;
import pt.uminho.ceb.biosystems.merlin.aibench.utilities.MerlinUtils;
import pt.uminho.ceb.biosystems.merlin.core.datatypes.IntegrateCompartmentsData;
import pt.uminho.ceb.biosystems.merlin.core.datatypes.annotation.compartments.AnnotationCompartmentsGenes;
import pt.uminho.ceb.biosystems.merlin.database.connector.databaseAPI.ProjectAPI;
import pt.uminho.ceb.biosystems.merlin.database.connector.datatypes.Connection;
import pt.uminho.ceb.biosystems.merlin.process.services.ProjectServices;
import pt.uminho.ceb.biosystems.merlin.utilities.TimeLeftProgress;

@Operation(name="integrate compartments", description="integrate compartments to the model reactions")
public class IntegrateCompartmentstoDatabase implements Observer {

	private boolean loaded;
	private TimeLeftProgress progress = new TimeLeftProgress();
	private IntegrateCompartmentsData integration;
	private List<String> ignoreList;
	private AtomicBoolean cancel;
	private AtomicInteger processingCounter;
	private AtomicInteger querySize; 
	private long startTime;
	private boolean biochemical;
	private boolean transport;
	private ProjectGUI project;

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

	@Port(direction=Direction.INPUT, name="project", order=5)
	public void setProject(ProjectGUI project){

		this.checkProject(project);
	};

	@Port(direction=Direction.INPUT, name="geneCompartments", order=6)
	public void setGeneCompartments(Map<String, AnnotationCompartmentsGenes> geneCompartments){

		this.cancel = new AtomicBoolean(false);
		this.startTime = GregorianCalendar.getInstance().getTimeInMillis();

		Connection connection = new Connection(project.getDatabase().getDatabaseAccess());

		if(ProjectServices.isGeneDataAvailable(connection)) {

			this.progress = new TimeLeftProgress();
			this.querySize = new AtomicInteger();
			this.processingCounter = new AtomicInteger();
			this.cancel = new AtomicBoolean();

			this.integration = new IntegrateCompartmentsData(project, geneCompartments);
			this.integration.addObserver(this);
			this.integration.setTimeLeftProgress(progress);
			this.integration.setQuerySize(this.querySize);
			this.integration.setProcessingCounter(this.processingCounter);
			this.integration.setCancel(this.cancel);

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

			if(this.transport && ProjectServices.isTransporterLoaded(connection) && !this.cancel.get()) {

				try {

					this.progress.setTime(0, 0, 0, "processing transport reactions");

					result = integration.assignCompartmentsToTransportReactions(ignoreList);
				}
				catch (Exception e) {

					result = false;
					Workbench.getInstance().error(e);
				}
			}

			MerlinUtils.updateCompartmentsAnnotationView(project.getWorkspace());
			MerlinUtils.updateReactionsView(project.getWorkspace());

			if(result && !this.cancel.get()) {

				Workbench.getInstance().info("Compartments integration complete!");
			}
			else{

				Workbench.getInstance().error("an error occurred while performing the operation.");
			}
		}
		else {

			Workbench.getInstance().error("gene data for integration unavailable!");
		}

		connection.closeConnection();

	};

	/**
	 * @param project
	 */
	public void checkProject(ProjectGUI project){



		if(project == null) {

			throw new IllegalArgumentException("no project selected!");
		}
		else {
			Connection connection = new Connection(project.getDatabase().getDatabaseAccess());
			Statement stmt = connection.createStatement();
			
			this.project = project;

			if(!ProjectServices.areCompartmentsPredicted(stmt))
				throw new IllegalArgumentException("please perform the compartments prediction operation before integrating compartments data.");
			

			int comp_genes = ProjectAPI.countGenesInGeneHasCompartment(stmt);

			int	genes = ProjectAPI.countGenes(stmt);

			if(genes<comp_genes) {

				this.loaded = true;		
			}
			connection.closeConnection();	
		}
	}

	/**
	 * @return
	 */
	@Progress
	public TimeLeftProgress getProgress() {

		return this.progress;
	}

	@Override
	public void update(Observable o, Object arg) {

		progress.setTime(GregorianCalendar.getInstance().getTimeInMillis() - this.startTime, this.processingCounter.get(), this.querySize.get());
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
