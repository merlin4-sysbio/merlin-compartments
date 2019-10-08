package pt.uminho.ceb.biosystems.merlin.compartments.processes;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.sql.SQLException;
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
import pt.uminho.ceb.biosystems.merlin.core.containers.model.CompartmentContainer;
import pt.uminho.ceb.biosystems.merlin.core.containers.model.MetaboliteContainer;
import pt.uminho.ceb.biosystems.merlin.core.containers.model.ReactionContainer;
import pt.uminho.ceb.biosystems.merlin.core.interfaces.IIntegrateData;
import pt.uminho.ceb.biosystems.merlin.core.utilities.Enumerators.SourceType;
import pt.uminho.ceb.biosystems.merlin.services.model.ModelCompartmentServices;
import pt.uminho.ceb.biosystems.merlin.services.model.ModelGenesServices;
import pt.uminho.ceb.biosystems.merlin.services.model.ModelProteinsServices;
import pt.uminho.ceb.biosystems.merlin.services.model.ModelReactionsServices;
import pt.uminho.ceb.biosystems.merlin.services.model.ModelStoichiometryServices;
import pt.uminho.ceb.biosystems.merlin.services.model.loaders.ModelDatabaseLoadingServices;
import pt.uminho.ceb.biosystems.mew.utilities.datastructures.pair.Pair;

/**
 * @author ODias
 *
 */
public class CompartmentsAnnotationIntegrationProcesses implements IIntegrateData, PropertyChangeListener {

	private static final Logger logger = LoggerFactory.getLogger(CompartmentsAnnotationIntegrationProcesses.class);

	private Map<String,AnnotationCompartmentsGenes> geneCompartments;
	private AtomicBoolean cancel;
	private CompartmentsProcesses processCompartments;
	private AtomicInteger processingTotal;
	private AtomicInteger processingCounter;
	private String workspaceName;
	private PropertyChangeSupport changes;


	/**
	 * @param project
	 * @param threshold
	 */
	public CompartmentsAnnotationIntegrationProcesses(String workspaceName, Map<String,AnnotationCompartmentsGenes> geneCompartments) {

		this.workspaceName = workspaceName;
		this.changes = new PropertyChangeSupport(this);
		this.geneCompartments = geneCompartments;
		this.cancel = new AtomicBoolean(false);
		this.processCompartments = new CompartmentsProcesses();
	}

	/**
	 * @param bool
	 * @throws Exception 
	 */
	public boolean initProcessCompartments() throws Exception {

		Set<String> compartments = new HashSet<>();

		List<CompartmentContainer> containers = ModelCompartmentServices.getCompartmentsInfo(this.workspaceName);

		for(CompartmentContainer container : containers) {
			if(container.getName() != null && !container.getName().isEmpty())
				compartments.add(container.getName());
		}

		this.processCompartments.initProcessCompartments(compartments);
		return true;
	}

	/**
	 * @return
	 */
	public boolean performIntegration() {

		try {

			this.processingTotal.set(this.geneCompartments.size());

			Map<Integer, String> sequenceID_geneID = ModelGenesServices.getQueriesByGeneId(this.workspaceName);

			Map<String,Integer> compartmentsDatabaseIDs = new HashMap<>();


			for(Map.Entry<String, AnnotationCompartmentsGenes> entry : this.geneCompartments.entrySet())
			{

				if(this.cancel.get()) {

					this.processingCounter = new AtomicInteger(this.geneCompartments.keySet().size());
					break;
				}
				else {

					AnnotationCompartmentsGenes geneCompartments = this.geneCompartments.get(entry.getKey());
					String primaryCompartment = geneCompartments.getPrimary_location();
					String primaryCompartmentAbb = geneCompartments.getPrimary_location_abb();
					double scorePrimaryCompartment = geneCompartments.getPrimary_score();
					Map<String, Double> secondaryCompartments = geneCompartments.getSecondary_location();
					Map<String, String> secondaryCompartmentsAbb = geneCompartments.getSecondary_location_abb();

					compartmentsDatabaseIDs.putAll(ModelCompartmentServices.getCompartmentsDatabaseIDs(this.workspaceName, primaryCompartment, primaryCompartmentAbb, secondaryCompartments, secondaryCompartmentsAbb, compartmentsDatabaseIDs));

					Integer idGene = null;
					if(sequenceID_geneID.containsKey(geneCompartments.getGeneID()))
						idGene = geneCompartments.getGeneID();

					if(idGene==null)
						logger.trace("Gene {} not found!", entry.getKey());
					else						
						ModelGenesServices.loadGenesCompartments(this.workspaceName, idGene, compartmentsDatabaseIDs, primaryCompartment, scorePrimaryCompartment, secondaryCompartments);

					this.processCompartments.initProcessCompartments(compartmentsDatabaseIDs.keySet());
				}

				this.changes.firePropertyChange("sequencesCounter", this.processingCounter.get(), this.processingCounter.incrementAndGet());
			}
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
	 */
	/**
	 * @param ignoreList
	 * @return
	 */
	public boolean assignCompartmentsToMetabolicReactions(List<String> ignoreList) {

		try {

			processCompartments.setInteriorCompartment(CompartmentsIntegrationServices.autoSetInteriorCompartment(this.workspaceName, processCompartments.getInteriorCompartment()));
			Map<Integer,String> compartmentsAbb_ids = ModelCompartmentServices.getModelCompartmentIdAndAbbreviation(this.workspaceName);
			Map<String,Integer> idCompartmentAbbIdMap = ModelCompartmentServices.getAbbreviationAndCompartmentId(this.workspaceName);

			Map<Integer, List<Integer>> enzymesReactions = ModelReactionsServices.getEnzymesReactions2(this.workspaceName);

			///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

			Map<Integer, List<Integer>> enzymesCompartments = ModelProteinsServices.getEnzymesCompartments(this.workspaceName);
			///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

			Map<Integer, ReactionContainer> reactionsMap = ModelDatabaseLoadingServices.getEnzymesReactionsMap(this.workspaceName, false);

			this.processingTotal.set(this.processingTotal.get()+enzymesReactions.size());

			for(Integer proteinId : enzymesReactions.keySet()) {

				for(Integer idReaction: enzymesReactions.get(proteinId)) {

					ReactionContainer reaction = new ReactionContainer(reactionsMap.get(idReaction));

					if(enzymesCompartments.containsKey(proteinId)) {

						Set<Integer> parsedCompartments = this.processCompartments.parseCompartments(enzymesCompartments.get(proteinId), compartmentsAbb_ids,idCompartmentAbbIdMap, ignoreList);

						//all compartments are assigned to the enzyme
						for(int idCompartment: parsedCompartments) {

							if(idCompartment>0) {

								if(this.processCompartments.getIgnoreCompartmentsID().contains(idCompartment))
									reaction.setInModel(false);

								reaction.setLocalisation(idCompartment);
								ModelDatabaseLoadingServices.loadReaction(this.workspaceName, reaction, proteinId, false);
							}
						}
					}
					else {

						int idCompartment = idCompartmentAbbIdMap.get(this.processCompartments.getInteriorCompartment().toLowerCase());
						reaction.setLocalisation(idCompartment);
						ModelDatabaseLoadingServices.loadReaction(this.workspaceName, reaction, proteinId, false);
					}
				}

				this.changes.firePropertyChange("sequencesCounter", this.processingCounter.get(), this.processingCounter.incrementAndGet());
			}

			/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

			//if no enzyme is assigned to the reaction

			List<Integer> reactionsIDs = ModelReactionsServices.getDistinctReactionIdWhereSourceTransporters(this.workspaceName, false);

			this.processingTotal.set(this.processingTotal.get()+reactionsIDs.size());

			for(int idReaction: reactionsIDs) {

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

				logger.trace("reaction {} is in model {}", reactionsMap.get(idReaction).getExternalIdentifier(), reactionsMap.get(idReaction).isInModel());

				int idCompartment = idCompartmentAbbIdMap.get(this.processCompartments.getInteriorCompartment().toLowerCase());
				reactionsMap.get(idReaction).setLocalisation(idCompartment);
				ModelDatabaseLoadingServices.loadReaction(this.workspaceName, reactionsMap.get(idReaction), null, false);

				this.changes.firePropertyChange("sequencesCounter", this.processingCounter.get(), this.processingCounter.incrementAndGet());

			}
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

		processCompartments.setInteriorCompartment(CompartmentsIntegrationServices.autoSetInteriorCompartment(this.workspaceName, processCompartments.getInteriorCompartment()));

		Map<Integer,String> idCompartmentMap = ModelCompartmentServices.getModelCompartmentIdAndAbbreviation(this.workspaceName);
		
		Map<Integer, ReactionContainer> reactionsMap = ModelReactionsServices.getAllModelReactionAttributesbySource(this.workspaceName, false, SourceType.TRANSPORTERS);
		
		List<Integer> reactionReversibility = new ArrayList<>();
		
		for(int reactId : reactionsMap.keySet()) {
			if(reactionsMap.get(reactId).isReversible())
				reactionReversibility.add(reactId);
			
		}

		Map<String,Integer> idCompartmentAbbIdMap = ModelCompartmentServices.getAbbreviationAndCompartmentId(this.workspaceName);

		Map<String, List<String>> transportProteins_reactions = new HashMap<String, List<String>>();
		List<String> reactions_ids;

		//TODO MAKE this a static method on database loaders

		List<String[]> result = ModelReactionsServices.getReacIdEcNumProtIdWhereSourceEqualTransporters(this.workspaceName);

		for(int i=0; i<result.size(); i++){
			String[] list = result.get(i);

			String key = list[1].concat("_").concat(list[2]);
			reactions_ids = new ArrayList<String>();

			if(transportProteins_reactions.containsKey(key))
				reactions_ids = transportProteins_reactions.get(key);	

			reactions_ids.add(list[0]);
			transportProteins_reactions.put(key,reactions_ids);

			reactionsMap.get(Integer.valueOf(list[0])).addProteinPair(new Pair<Integer, String>(Integer.valueOf(list[2]), list[1]));
		}

		///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		Map<String, Set<Integer>> transportProteinsCompartments = ModelCompartmentServices.getCompartIdAndEcNumbAndProtId(this.workspaceName);

		Map<Integer, ReactionContainer> reactionsPathways = ModelReactionsServices.getReactionIdAndPathwayID(this.workspaceName, SourceType.TRANSPORTERS, false);

		for(Integer reactionId : reactionsPathways.keySet()){
			reactionsMap.get(reactionId).setPathways(reactionsPathways.get(reactionId).getPathways());
		}

		Map<Integer, ReactionContainer> reactionsMetabolites = ModelStoichiometryServices.getAllOriginalTransportersFromStoichiometry(this.workspaceName);
		
		for(Integer reactionId : reactionsMetabolites.keySet()){

			for(MetaboliteContainer container : reactionsMetabolites.get(reactionId).getProductsStoichiometry()) 
				reactionsMap.get(reactionId).addProduct(container);
				
			for(MetaboliteContainer container : reactionsMetabolites.get(reactionId).getReactantsStoichiometry()) 
				reactionsMap.get(reactionId).addReactant(container);
		}

		for(String transporter : transportProteins_reactions.keySet()) {

			for(String idReaction: transportProteins_reactions.get(transporter)) {

				ReactionContainer transportReaction = reactionsMap.get(Integer.valueOf(idReaction));

				Set<Integer> tpcCompartments = new HashSet<>();
				int tpcCompartmentsSize = 1;

				if(transportProteinsCompartments.containsKey(transporter)) {

					tpcCompartments = transportProteinsCompartments.get(transporter);
					tpcCompartmentsSize = transportProteinsCompartments.get(transporter).size();

				}
				else 
					tpcCompartments.add(idCompartmentAbbIdMap.get(CompartmentsUtilities.DEFAULT_MEMBRANE));


				String originalEquation = transportReaction.getEquation();
				int originalIDCompartment = transportReaction.getLocalisation().getCompartmentID();

				for(int idCompartment: tpcCompartments) {

					String abb = idCompartmentMap.get(idCompartment);

					if(reactionReversibility.contains(Integer.valueOf(idReaction)) && abb.equalsIgnoreCase("extr")) {

						if(this.processCompartments.getInteriorCompartment().equalsIgnoreCase("cyto"))
							abb = "PLAS";
						else
							abb = "outme";
					}

					if(abb.toLowerCase().contains("me") || abb.toLowerCase().contains("pla")) {

						if(ignoreList.contains(abb.toLowerCase())) 
							transportReaction.setInModel(false);

						String equation = originalEquation.replace("(out)","("+this.processCompartments.processTransportCompartments("out", abb )+")").replace("(in)",
								"("+this.processCompartments.processTransportCompartments("in", abb )+")");

						transportReaction.setEquation(equation);

						//////////////////////////////////////////////////////////////////

						String compartment = idCompartmentMap.get(originalIDCompartment);						

						if(!idCompartmentAbbIdMap.containsKey(compartment.toLowerCase())) {

							ModelCompartmentServices.insertNameAndAbbreviation(this.workspaceName, compartment, compartment);

						}
						originalIDCompartment = idCompartmentAbbIdMap.get(compartment.toLowerCase());

						//						transportReaction.setCompartment_idcompartment(newIDCompartments);

						//						CompartmentContainer container = new CompartmentContainer(originalIDCompartment, compartment, compartment);

						transportReaction.setLocalisation(originalIDCompartment);  //BEFORE THE METHOD WAS DIFFERENT AND THE VARIABLE 'idcompartment' WAS BEING SAVED HERE!!!!

						//////////////////////////////////////////////////////////////////
						ModelDatabaseLoadingServices.loadReaction(this.workspaceName, transportReaction, null, true);
					}
					else if(abb.equalsIgnoreCase("cytop") || tpcCompartmentsSize==1){

						String newAbb = CompartmentsUtilities.DEFAULT_MEMBRANE;

						String equation = originalEquation.replace("(out)","("+this.processCompartments.processTransportCompartments("out", newAbb )+")").replace("(in)",
								"("+this.processCompartments.processTransportCompartments("in", newAbb )+")");

						transportReaction.setEquation(equation);

						//////////////////////////////////////////////////////////////////


						String compartment = this.processCompartments.processTransportCompartments(idCompartmentMap.get(transportReaction.getLocalisation().getCompartmentID()), newAbb );

						if(!idCompartmentAbbIdMap.containsKey(compartment.toLowerCase())) {

							Integer id = ModelCompartmentServices.insertNameAndAbbreviation(this.workspaceName, compartment, compartment);

							idCompartmentAbbIdMap.put(compartment.toLowerCase(), id);
						}

						transportReaction.setLocalisation(idCompartmentAbbIdMap.get(compartment.toLowerCase()));
						transportReaction.setInModel(false);

						//////////////////////////////////////////////////////////////////
						ModelDatabaseLoadingServices.loadReaction(this.workspaceName, transportReaction, null, true);

						//							logger.debug("Transporter compartment {}",abb);
					}
				}
			}
		}

		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		List<Integer> reactionsIDs = ModelReactionsServices.getDistinctReactionIdWhereSourceTransporters(this.workspaceName, true);

		String defaultAbb = CompartmentsUtilities.DEFAULT_MEMBRANE.toString();
		Integer idCompartment = null;

		if(!idCompartmentAbbIdMap.containsKey(defaultAbb)) {
			String name = CompartmentsUtilities.parseAbbreviation(defaultAbb);

			idCompartment = ModelCompartmentServices.insertNameAndAbbreviation(this.workspaceName, name, defaultAbb);
			idCompartmentAbbIdMap.put(defaultAbb, idCompartment);

		}
		idCompartment = idCompartmentAbbIdMap.get(defaultAbb);

		for(Integer idReaction: reactionsIDs) {

			ReactionContainer transportReaction = reactionsMap.get(idReaction);

			String originalEquation = transportReaction.getEquation();

			String newAbb = CompartmentsUtilities.DEFAULT_MEMBRANE;

			transportReaction.setInModel(false);

			String equation = originalEquation.replace("(out)","("+this.processCompartments.processTransportCompartments("out", newAbb )+")").replace("(in)",
					"("+this.processCompartments.processTransportCompartments("in", newAbb )+")");

			transportReaction.setEquation(equation);

			//////////////////////////////////////////////////////////////////

			String compartment = this.processCompartments.processTransportCompartments(idCompartmentMap.get(transportReaction.getLocalisation().getCompartmentID()), newAbb);

			if(!idCompartmentAbbIdMap.containsKey(compartment.toLowerCase())) {

				Integer id = ModelCompartmentServices.insertNameAndAbbreviation(this.workspaceName, compartment, compartment);

				idCompartmentAbbIdMap.put(compartment.toLowerCase(), id);
			}

			transportReaction.setLocalisation(idCompartmentAbbIdMap.get(compartment.toLowerCase()));

			transportReaction.setLocalisation(idCompartment);

			//////////////////////////////////////////////////////////////////
			ModelDatabaseLoadingServices.loadReaction(this.workspaceName, transportReaction, null, true);

			//				logger.debug("Transporter compartment {}",abb);

		}
		return true;
	}

	public void processCompoundsCompartments() {


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
