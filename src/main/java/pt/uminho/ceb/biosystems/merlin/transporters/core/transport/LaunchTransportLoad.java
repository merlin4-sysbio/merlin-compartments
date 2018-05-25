package pt.uminho.ceb.biosystems.merlin.transporters.core.transport;
/**
 * 
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import pt.uminho.ceb.biosystems.merlin.database.connector.datatypes.Connection;
import pt.uminho.ceb.biosystems.merlin.database.connector.datatypes.DatabaseAccess;
import pt.uminho.ceb.biosystems.merlin.transporters.core.transport.reactions.containerAssembly.PopulateTransportContainer;
import pt.uminho.ceb.biosystems.merlin.transporters.core.transport.reactions.containerAssembly.TransportContainer;

/**
 * @author ODias
 * 
 *
 */
public class LaunchTransportLoad extends Observable implements Observer {


	/**
	 * @param dba
	 * @param alpha
	 * @param minimalFrequency
	 * @param beta
	 * @param threshold
	 * @param validateReaction
	 * @param saveOnlyReactionsWithKEGGmetabolites
	 * @param outputObjectFileName
	 * @param outputTextReactionsfileName
	 * @param path
	 * @param project_id
	 * @param verbose
	 * @param ignoreSymportMetabolites
	 * @param generations 
	 * @return
	 * @throws Exception
	 */
	public static TransportContainer createTransportContainer(DatabaseAccess dba, double alpha, int minimalFrequency, double beta, 
			double threshold, boolean validateReaction, boolean saveOnlyReactionsWithKEGGmetabolites, String outputObjectFileName,
			String outputTextReactionsfileName, String path, int project_id, boolean verbose, Set<String> ignoreSymportMetabolites, long taxonomy, int generations) throws Exception {

		long startTime = System.currentTimeMillis();

		Connection conn = new Connection(dba);
		
		PopulateTransportContainer populateTransportContainer; 
		
		TransportContainer transportContainer = null;

		if(outputObjectFileName != null && new File(outputObjectFileName).exists()) {

			File file = new File(outputObjectFileName);
			file.createNewFile();
			FileInputStream f_in = new  FileInputStream (file);
			ObjectInputStream obj_in = new ObjectInputStream (f_in);

			try {

				transportContainer = (TransportContainer) obj_in.readObject();
			}
			catch (ClassNotFoundException e) {e.printStackTrace();}

			obj_in.close();
			f_in.close();
		}
		else {
			
			//if(this.taxonomy>0)				
			populateTransportContainer 	= new PopulateTransportContainer(conn, alpha, minimalFrequency, beta, threshold, taxonomy, project_id, ignoreSymportMetabolites, dba.get_database_type());
//			else				
//				populateTransportContainer 	= new PopulateTransportContainer(conn, alpha, minimalFrequency, beta, threshold, project_id, ignoreSymportMetabolites);

			populateTransportContainer.getDataFromDatabase();
			saveOnlyReactionsWithKEGGmetabolites = true;
			transportContainer = populateTransportContainer.loadContainer(saveOnlyReactionsWithKEGGmetabolites, generations);
			transportContainer = populateTransportContainer.containerValidation(transportContainer, verbose);
			
			if(outputTextReactionsfileName!=null) {
				
				populateTransportContainer.creatReactionsFiles(transportContainer,path+"__"+outputTextReactionsfileName);
			}
			if(outputObjectFileName!=null) {
				
				LaunchTransportLoad.saveTransportContainerFile(transportContainer, outputObjectFileName);
			}
		}

		long endTime = System.currentTimeMillis();
		System.out.println("Total elapsed time in execution of transportReactionsModelIntegration is :"+ String.format("%d min, %d sec", 
				TimeUnit.MILLISECONDS.toMinutes(endTime-startTime),TimeUnit.MILLISECONDS.toSeconds(endTime-startTime) -TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(endTime-startTime))));
		return transportContainer;
	}

	/**
	 * @param path
	 * @param transportContainer
	 * @param obj
	 * @param k
	 * @return
	 * @throws IOException
	 * @throws SQLException
	 * @throws ReactionAlreadyExistsException 
	 */
//	public TransportContainer compartmentaliseTransportContainer(String path, TransportContainer transportContainer, CompartmentsInterface obj, KINGDOM k) throws IOException, SQLException, ReactionAlreadyExistsException {
//
//		CompartmentaliseTransportContainer compartmentaliseTransportContainer = new CompartmentaliseTransportContainer(transportContainer, obj.getBestCompartmentsByGene(10, statement), k);
//		compartmentaliseTransportContainer.loadCompartmentsToContainer();
//		compartmentaliseTransportContainer.createReactionsFiles(path);
//		compartmentaliseTransportContainer.createReactionsAnnotationsTabFiles(path);
//
//		return transportContainer;
//
//		//		TransportReactionsModelIntegration transportReactionsModelIntegration = new TransportReactionsModelIntegration();
//		//		transportReactionsModelIntegration.setCompartmentaliseTransportContainer(compartmentaliseTransportContainer);
//		//		transportReactionsModelIntegration.setListFromFile(outPath);
//		//		transportReactionsModelIntegration.selectReactionToIntegrateInModel(useInternalReactions,useKeggMetabolitesOnly);
//		//		transportReactionsModelIntegration.getListToFile(directory+"transportReactionsModel.txt");
//	}

	/**
	 * @param transportContainer
	 * @param fileName
	 * @return
	 */
	private static boolean saveTransportContainerFile(TransportContainer transportContainer, String fileName) {

		try {

			File transContainer = new File(fileName);
			transContainer.createNewFile();
			FileOutputStream f_out = new  FileOutputStream(transContainer);
			ObjectOutputStream obj_out = new ObjectOutputStream (f_out);
			obj_out.writeObject(transportContainer);
			obj_out.close();
			f_out.close();
			return true;
		} 
		catch (IOException e1) {
			
			e1.printStackTrace();
		} 
		return false;
	}

	@Override
	public void update(Observable arg0, Object arg1) {

		setChanged();
		notifyObservers();
	}
}
