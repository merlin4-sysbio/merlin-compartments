package pt.uminho.ceb.biosystems.merlin.compartments.processes;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.uminho.ceb.biosystems.merlin.compartments.datatype.AnnotationCompartmentsGenes;
import pt.uminho.ceb.biosystems.merlin.compartments.services.CompartmentsIntegrationServices;
import pt.uminho.ceb.biosystems.merlin.compartments.utils.CompartmentsUtilities;
import pt.uminho.ceb.biosystems.merlin.core.interfaces.IIntegrateData;
import pt.uminho.ceb.biosystems.merlin.database.connector.databaseAPI.CompartmentsAPI;
import pt.uminho.ceb.biosystems.merlin.database.connector.databaseAPI.HomologyAPI;
import pt.uminho.ceb.biosystems.merlin.database.connector.databaseAPI.ModelAPI;
import pt.uminho.ceb.biosystems.merlin.database.connector.databaseAPI.ProjectAPI;
import pt.uminho.ceb.biosystems.merlin.database.connector.datatypes.Connection;
import pt.uminho.ceb.biosystems.merlin.services.model.loaders.ModelDatabaseLoadingServices;
import pt.uminho.ceb.biosystems.merlin.utilities.containers.capsules.DatabaseReactionContainer;

/**
 * @author ODias
 *
 */
public class CompartmentsAnnotationIntegrationProcesses implements IIntegrateData, PropertyChangeListener {

	private static final Logger logger = LoggerFactory.getLogger(CompartmentsAnnotationIntegrationProcesses.class);

	private Connection connection;
	private Map<String,AnnotationCompartmentsGenes> geneCompartments;
	private AtomicBoolean cancel;
	private CompartmentsProcesses processCompartments;
	private AtomicInteger processingTotal;
	private AtomicInteger processingCounter;

	private PropertyChangeSupport changes;


	/**
	 * @param project
	 * @param threshold
	 */
	public CompartmentsAnnotationIntegrationProcesses(Connection connection, Map<String,AnnotationCompartmentsGenes> geneCompartments) {

		this.changes = new PropertyChangeSupport(this);
		this.connection = connection;
		this.geneCompartments = geneCompartments;
		this.cancel = new AtomicBoolean(false);
		this.processCompartments = new CompartmentsProcesses();
	}

	/**
	 * @param bool
	 * @throws SQLException 
	 */
	public boolean initProcessCompartments() {

		Set<String> compartments = new HashSet<>();
		Statement stmt = this.connection.createStatement();

		try {
			compartments = ModelAPI.getCompartments(stmt);
			stmt.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		this.processCompartments.initProcessCompartments(compartments);
		return true;
	}

	/**
	 * @return
	 */
	public boolean performIntegration() {

		try {

			Statement statement = this.connection.createStatement();
			this.processingTotal.set(this.geneCompartments.size());

			Map<String, String> sequenceID_geneID = HomologyAPI.getSequenceID(statement);

			Map<String,String> compartmentsDatabaseIDs = new HashMap<String,String>();

			for(String sequence_id :this.geneCompartments.keySet()) {

				if(this.cancel.get()) {

					this.processingCounter = new AtomicInteger(this.geneCompartments.keySet().size());
					break;
				}
				else {

					AnnotationCompartmentsGenes geneCompartments = this.geneCompartments.get(sequence_id);
					String primaryCompartment = geneCompartments.getPrimary_location();
					String primaryCompartmentAbb = geneCompartments.getPrimary_location_abb();
					double scorePrimaryCompartment = geneCompartments.getPrimary_score();
					Map<String, Double> secondaryCompartments = geneCompartments.getSecondary_location();
					Map<String, String> secondaryCompartmentsAbb = geneCompartments.getSecondary_location_abb();

					compartmentsDatabaseIDs.putAll(ModelAPI.getCompartmentsDatabaseIDs(primaryCompartment, primaryCompartmentAbb, secondaryCompartments, secondaryCompartmentsAbb, compartmentsDatabaseIDs, statement));

					String idGene = null;
					if(sequenceID_geneID.containsKey(geneCompartments.getGene()))
						idGene = sequenceID_geneID.get(geneCompartments.getGene());

					if(idGene==null)
						logger.trace("Gene {} not found!", sequence_id);
					else						
						ModelAPI.loadGenesCompartments(idGene, compartmentsDatabaseIDs, statement, primaryCompartment, scorePrimaryCompartment, secondaryCompartments);

					this.processCompartments.initProcessCompartments(compartmentsDatabaseIDs.keySet());
				}

				this.changes.firePropertyChange("sequencesCounter", this.processingCounter.get(), this.processingCounter.incrementAndGet());
			}
			statement.close();
			return true;
		}
		catch (SQLException e) {e.printStackTrace();}
		return false;

	}


	/**
	 * @param ignoreList
	 * @return
	 */
	/**
	 * @param ignoreList
	 * @return
	 */
	public boolean assignCompartmentsToMetabolicReactions(List<String> ignoreList) {

		Statement statement;
		try {

			statement = this.connection.createStatement();

			processCompartments.setInteriorCompartment(CompartmentsIntegrationServices.autoSetInteriorCompartment(processCompartments.getInteriorCompartment(), statement));
			Map<Integer,String> compartmentsAbb_ids = ModelAPI.getIdCompartmentAbbMap(statement);
			Map<String,Integer> idCompartmentAbbIdMap = ModelAPI.getCompartmentAbbIdMap(statement);

			Map<String, List<String>> enzymesReactions = CompartmentsAPI.getEnzymesReactions2(statement);

			///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

			Map<String, List<Integer>> enzymesCompartments = CompartmentsAPI.getEnzymesCompartments(statement);

			///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

			Map<String, DatabaseReactionContainer> reactionsMap = ModelDatabaseLoadingServices.getEnzymesReactionsMap(statement, false);

			this.processingTotal.set(this.processingTotal.get()+enzymesReactions.size());

			for(String ecnumber : enzymesReactions.keySet()) {

				for(String idReaction: enzymesReactions.get(ecnumber)) {

					DatabaseReactionContainer reaction = new DatabaseReactionContainer(reactionsMap.get(idReaction));

					//reactions are in model if they were assigned to model by user
					boolean inModelFromCompartment =  reaction.isInModel();

					if(enzymesCompartments.containsKey(ecnumber)) {

						Set<Integer> parsedCompartments = this.processCompartments.parseCompartments(enzymesCompartments.get(ecnumber), compartmentsAbb_ids,idCompartmentAbbIdMap, ignoreList);

						//all compartments are assigned to the enzyme
						for(int idCompartment: parsedCompartments) {

							if(idCompartment>0) {

								if(this.processCompartments.getIgnoreCompartmentsID().contains(idCompartment))
									inModelFromCompartment = false;

								ModelDatabaseLoadingServices.loadReaction(idCompartment, inModelFromCompartment, reaction, ecnumber, statement, false);
							}
						}
					}
					else {

						int idCompartment = idCompartmentAbbIdMap.get(this.processCompartments.getInteriorCompartment().toLowerCase());

						ModelDatabaseLoadingServices.loadReaction(idCompartment, inModelFromCompartment, reaction, ecnumber, statement, false);
					}
				}

				this.changes.firePropertyChange("sequencesCounter", this.processingCounter.get(), this.processingCounter.incrementAndGet());
			}

			/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

			//if no enzyme is assigned to the reaction

			List<String> reactionsIDs = CompartmentsAPI.getReactionID(statement);

			this.processingTotal.set(this.processingTotal.get()+reactionsIDs.size());

			for(String idReaction: reactionsIDs) {

				/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
				/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

				//				ArrayList<String[]> result = HomologyAPI.getEnzymeProteinID(idReaction, statement);
				//
				//				Map<Integer,String> proteinID = new HashMap<Integer, String>();
				//				Map<Integer,String> ecNumber = new HashMap<Integer, String>();
				//
				//				for(int i=0; i<result.size(); i++){
				//					String[] list = result.get(i);
				//
				//					proteinID.put(i, list[0]);
				//					ecNumber.put(i, list[1]);
				//				}
				/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
				/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

				//List<String> pathwayID = ProjectAPI.getPathwaysIDsByReactionID(idReaction, statement); ---> never used

				/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
				/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

				logger.trace("reaction {} is in model {}", reactionsMap.get(idReaction).getName(), reactionsMap.get(idReaction).isInModel());

				int idCompartment = idCompartmentAbbIdMap.get(this.processCompartments.getInteriorCompartment().toLowerCase());

				ModelDatabaseLoadingServices.loadReaction(idCompartment, reactionsMap.get(idReaction).isInModel(), reactionsMap.get(idReaction), null, statement, false);

				this.changes.firePropertyChange("sequencesCounter", this.processingCounter.get(), this.processingCounter.incrementAndGet());

			}
			statement.close();
			return true;
		}
		catch (Exception e) { 

			e.printStackTrace();
		}

		return false;
	}


	/**
	 * @param ignoreList
	 * @return
	 * @throws Exception 
	 */
	public boolean assignCompartmentsToTransportReactions(List<String> ignoreList) throws Exception {

		Statement statement = this.connection.createStatement();

		processCompartments.setInteriorCompartment(CompartmentsIntegrationServices.autoSetInteriorCompartment(processCompartments.getInteriorCompartment(), statement));

		Map<Integer,String> idCompartmentMap = ModelAPI.getIdCompartmentAbbMap(statement);

		List<String> reactionReversibility = ModelAPI.getReactionReversibleTransporters(statement);

		Map<String,Integer> idCompartmentAbbIdMap = ModelAPI.getCompartmentAbbIdMap(statement);

		Map<String, List<String>> transportProteins_reactions = new HashMap<String, List<String>>();
		List<String> reactions_ids;

		//TODO MAKE this a static method on database loaders

		Map<String, DatabaseReactionContainer> reactionsMap = ModelAPI.getDataFromReactionForTransp(statement);

		ArrayList<String[]> result = ModelAPI.getTransportReactions(statement);

		for(int i=0; i<result.size(); i++){
			String[] list = result.get(i);

			String key = list[1].concat("_").concat(list[2]);
			reactions_ids = new ArrayList<String>();

			if(transportProteins_reactions.containsKey(key))
				reactions_ids = transportProteins_reactions.get(key);	

			reactions_ids.add(list[0]);
			transportProteins_reactions.put(key,reactions_ids);

			reactionsMap.get(list[0]).addProteins(list[2], list[1]);
		}

		///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		Map<String, Set<Integer>> transportProteinsCompartments = CompartmentsAPI.getTransportProteinsCompartments(statement);


		result = ProjectAPI.getReactionIdAndPathwayID(statement);

		for(int i=0; i<result.size(); i++){
			String[] list = result.get(i);
			reactionsMap.get(list[0]).getPathways().add(list[1]);
		}

		result = ModelAPI.getAllOriginalTransportersFromStoichiometry(statement);

		for(int i=0; i<result.size(); i++){
			String[] list = result.get(i);
			reactionsMap.get(list[1]).addEntry(Integer.parseInt(list[2]), list[4], list[5], Integer.parseInt(list[3]));
		}

		for(String transporter : transportProteins_reactions.keySet()) {

			for(String idReaction: transportProteins_reactions.get(transporter)) {

				DatabaseReactionContainer transportReaction = new DatabaseReactionContainer(reactionsMap.get(idReaction));

				Set<Integer> tpcCompartments = new HashSet<>();
				int tpcCompartmentsSize = 1;

				if(transportProteinsCompartments.containsKey(transporter)) {

					tpcCompartments = transportProteinsCompartments.get(transporter);
					tpcCompartmentsSize = transportProteinsCompartments.get(transporter).size();

				}
				else 
					tpcCompartments.add(idCompartmentAbbIdMap.get(CompartmentsUtilities.DEFAULT_MEMBRANE));


				String originalEquation = transportReaction.getEquation();

				List<Integer> originalIDCompartments = transportReaction.getCompartment_idcompartment();

				for(int idCompartment: tpcCompartments) {

					String abb = idCompartmentMap.get(idCompartment);

					//					String newAbb = abb;

					//					if(abb.toLowerCase().contains("me"))
					//						newAbb = TransportersUtilities.getOutsideMembrane(abb.toLowerCase(),  this.processCompartments.getStain());

					if(reactionReversibility.contains(idReaction) && abb.equalsIgnoreCase("extr")) {

						if(this.processCompartments.getInteriorCompartment().equalsIgnoreCase("cyto"))
							abb = "PLAS";
						else
							abb = "outme";
					}

					if(abb.toLowerCase().contains("me") || abb.toLowerCase().contains("pla")) {

						boolean inModelFromCompartment = transportReaction.isInModel();

						if(ignoreList.contains(abb.toLowerCase())) 
							inModelFromCompartment = false;

						String equation = originalEquation.replace("(out)","("+this.processCompartments.processTransportCompartments("out", abb )+")").replace("(in)",
								"("+this.processCompartments.processTransportCompartments("in", abb )+")");

						transportReaction.setEquation(equation);

						//////////////////////////////////////////////////////////////////

						List<Integer> newIDCompartments = new ArrayList<>();
						for(int j = 0 ; j < originalIDCompartments.size(); j++ ) {

							int metaboliteCompartmentID = originalIDCompartments.get(j);

							String compartment = this.processCompartments.processTransportCompartments(idCompartmentMap.get(metaboliteCompartmentID), abb );						

							if(!idCompartmentAbbIdMap.containsKey(compartment.toLowerCase())) {

								String query = "INSERT INTO compartment (name, abbreviation) VALUES('"+compartment+"','"+compartment+"')";

								idCompartmentAbbIdMap.put(compartment.toLowerCase(), ProjectAPI.executeAndGetLastInsertID(query, statement));
							}
							metaboliteCompartmentID = idCompartmentAbbIdMap.get(compartment.toLowerCase());
							newIDCompartments.add(j, metaboliteCompartmentID);
						}

						transportReaction.setCompartment_idcompartment(newIDCompartments);
						//////////////////////////////////////////////////////////////////
						ModelDatabaseLoadingServices.loadReaction(idCompartment, inModelFromCompartment, transportReaction, null, statement, true);
					}
					else if(abb.equalsIgnoreCase("cytop") || tpcCompartmentsSize==1){

						String newAbb = CompartmentsUtilities.DEFAULT_MEMBRANE;

						boolean inModel = false;

						String equation = originalEquation.replace("(out)","("+this.processCompartments.processTransportCompartments("out", newAbb )+")").replace("(in)",
								"("+this.processCompartments.processTransportCompartments("in", newAbb )+")");

						transportReaction.setEquation(equation);

						//////////////////////////////////////////////////////////////////

						List<Integer> newIDCompartments = new ArrayList<>();
						for(int j = 0 ; j < transportReaction.getCompound_idcompounds().size(); j++ ) {

							int metaboliteCompartmentID = transportReaction.getCompartment_idcompartment().get(j);
							String compartment = this.processCompartments.processTransportCompartments(idCompartmentMap.get(metaboliteCompartmentID), newAbb );

							if(!idCompartmentAbbIdMap.containsKey(compartment.toLowerCase())) {

								String query = "INSERT INTO compartment (name, abbreviation) VALUES('"+compartment+"','"+compartment+"')";

								idCompartmentAbbIdMap.put(compartment.toLowerCase(), ProjectAPI.executeAndGetLastInsertID(query, statement));
							}
							metaboliteCompartmentID = idCompartmentAbbIdMap.get(compartment.toLowerCase());
							newIDCompartments.add(j, metaboliteCompartmentID);

						}

						transportReaction.setCompartment_idcompartment(newIDCompartments);

						//////////////////////////////////////////////////////////////////
						ModelDatabaseLoadingServices.loadReaction(idCompartment, inModel, transportReaction, null, statement, true);

						//							logger.debug("Transporter compartment {}",abb);
					}
				}
			}
		}

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		List<String> reactionsIDs = CompartmentsAPI.getTransportReactionID(statement);

		int idCompartment = idCompartmentAbbIdMap.get(CompartmentsUtilities.DEFAULT_MEMBRANE.toString());

		for(String idReaction: reactionsIDs) {

			DatabaseReactionContainer transportReaction = new DatabaseReactionContainer(reactionsMap.get(idReaction));

			String originalEquation = transportReaction.getEquation();

			String newAbb = CompartmentsUtilities.DEFAULT_MEMBRANE;

			boolean inModel = false;

			String equation = originalEquation.replace("(out)","("+this.processCompartments.processTransportCompartments("out", newAbb )+")").replace("(in)",
					"("+this.processCompartments.processTransportCompartments("in", newAbb )+")");

			transportReaction.setEquation(equation);

			//////////////////////////////////////////////////////////////////

			List<Integer> newIDCompartments = new ArrayList<>();
			for(int j = 0 ; j < transportReaction.getCompound_idcompounds().size(); j++ ) {

				int metaboliteCompartmentID = transportReaction.getCompartment_idcompartment().get(j);

				String compartment = this.processCompartments.processTransportCompartments(idCompartmentMap.get(metaboliteCompartmentID), newAbb );

				if(!idCompartmentAbbIdMap.containsKey(compartment.toLowerCase())) {

					String query = "INSERT INTO compartment (name, abbreviation) VALUES('"+compartment+"','"+compartment+"')";

					idCompartmentAbbIdMap.put(compartment.toLowerCase(), ProjectAPI.executeAndGetLastInsertID(query, statement));
				}
				metaboliteCompartmentID = idCompartmentAbbIdMap.get(compartment.toLowerCase());
				newIDCompartments.add(j, metaboliteCompartmentID);

			}

			transportReaction.setCompartment_idcompartment(newIDCompartments);

			//////////////////////////////////////////////////////////////////
			ModelDatabaseLoadingServices.loadReaction(idCompartment, inModel, transportReaction, null, statement, true);

			//				logger.debug("Transporter compartment {}",abb);

		}
		statement.close();
		return true;
	}

	/**
	 * @param processingTotal
	 */
	public void setQuerySize(AtomicInteger querySize) {

		this.processingTotal = querySize; 		
	}

	public void setCancel(AtomicBoolean cancel) {

		this.cancel = cancel;
	}

	public void cancel() {

		this.cancel.set(true);
	}

	/**
	 * @return the processingCounter
	 */
	public AtomicInteger getProcessingCounter() {
		return processingCounter;
	}

	/**
	 * @param processingCounter the processingCounter to set
	 */
	public void setProcessingCounter(AtomicInteger processingCounter) {
		this.processingCounter = processingCounter;
	}

	/**
	 * @param l
	 */
	public void addPropertyChangeListener(PropertyChangeListener l) {
		changes.addPropertyChangeListener(l);
	}

	/**
	 * @param l
	 */
	public void removePropertyChangeListener(PropertyChangeListener l) {
		changes.removePropertyChangeListener(l);
	}


	@Override
	public void propertyChange(PropertyChangeEvent evt) {

		this.changes.firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());				
	}

}