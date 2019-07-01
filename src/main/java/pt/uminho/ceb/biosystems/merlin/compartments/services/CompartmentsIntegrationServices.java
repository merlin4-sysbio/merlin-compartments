package pt.uminho.ceb.biosystems.merlin.compartments.services;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pt.uminho.ceb.biosystems.merlin.compartments.datatype.AnnotationCompartmentsGenes;
import pt.uminho.ceb.biosystems.merlin.compartments.processes.CompartmentsProcesses;
import pt.uminho.ceb.biosystems.merlin.core.containers.model.ReactionContainer;
import pt.uminho.ceb.biosystems.merlin.core.datatypes.Workspace;
import pt.uminho.ceb.biosystems.merlin.core.utilities.Enumerators.InformationType;
import pt.uminho.ceb.biosystems.merlin.database.connector.databaseAPI.CompartmentsAPI;
import pt.uminho.ceb.biosystems.merlin.database.connector.databaseAPI.ModelAPI;
import pt.uminho.ceb.biosystems.merlin.database.connector.datatypes.Connection;
import pt.uminho.ceb.biosystems.merlin.services.ProjectServices;
import pt.uminho.ceb.biosystems.merlin.services.model.loaders.ModelDatabaseLoadingServices;

public class CompartmentsIntegrationServices {

	/**
	 * @return
	 * @throws SQLException 
	 */
	public String autoSetInteriorCompartment(String interiorCompartment, Connection connection) {

		String out = null;

		try {

			Statement stmt = connection.createStatement();

			out = CompartmentsIntegrationServices.autoSetInteriorCompartment(interiorCompartment, stmt);

			stmt.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return out;
	}

	/**
	 * @param interiorCompartment
	 * @param statement
	 * @return
	 */
	public static String autoSetInteriorCompartment(String interiorCompartment, Statement statement){

		try {

			interiorCompartment = CompartmentsAPI.getCompartmentAbbreviation(interiorCompartment, statement);

		} 
		catch (SQLException e) {
			e.printStackTrace();
		}

		return interiorCompartment;
	}


	/**
	 * @param compartment
	 * @throws SQLException
	 */
	public static int getCompartmentID(String compartment, Connection connection) {

		Statement stmt;
		int compartmentID = -1;

		try {
			stmt = connection.createStatement();

			String abbreviation;

			if(compartment.length()>3) {

				abbreviation=compartment.substring(0,3).toUpperCase();
			}
			else {

				abbreviation=compartment.toUpperCase().concat("_");

				while(abbreviation.length()<4)
					abbreviation=abbreviation.concat("_");
			}

			abbreviation=abbreviation.toUpperCase();

			compartmentID = CompartmentsAPI.selectCompartmentID(compartment, abbreviation, stmt);

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return compartmentID;

	}
	
	
	@Deprecated
	/**
	 * @param locusTag
	 * @param sequence_id
	 * @param geneName
	 * @param direction
	 * @param left_end
	 * @param right_end
	 * @param ecNumbers
	 * @param proteinName
	 * @param statement
	 * @param integratePartial
	 * @param integrateFull
	 * @param insertProductNames
	 * @param project
	 * @param informationType
	 * @param genesCompartments
	 * @return
	 * @throws Exception
	 */
	public static boolean loadGeneAnnotation(String locusTag, String  sequence_id, String geneName, String direction, String left_end, String right_end, Set<String> ecNumbers, String proteinName, Statement statement,
			boolean integratePartial, boolean integrateFull, boolean insertProductNames, Workspace project, InformationType informationType, Map<String, AnnotationCompartmentsGenes> genesCompartments) throws Exception {

		Map<String, List<Integer>> enzymesReactions = null;

		String idGene = ModelDatabaseLoadingServices.loadGene(locusTag, sequence_id, geneName, direction, left_end, right_end, statement, informationType);

		boolean isCompartmentalisedModel = ProjectServices.isCompartmentalisedModel(null); 
		
		if (! ecNumbers.isEmpty())			
			enzymesReactions = ModelAPI.loadEnzymeGetReactions(idGene, ecNumbers, proteinName, statement, integratePartial, integrateFull, insertProductNames, isCompartmentalisedModel);

		if(isCompartmentalisedModel && !ModelAPI.isGeneCompartmentLoaded(idGene, statement)) {

			if(genesCompartments!=null && !genesCompartments.isEmpty()) {

				AnnotationCompartmentsGenes geneCompartments = genesCompartments.get(locusTag);

				Map<String, String> compartmentsDatabaseIDs = new HashMap<>();
				String primaryCompartment = geneCompartments.getPrimary_location();
				String primaryCompartmentAbb = geneCompartments.getPrimary_location_abb();
				double scorePrimaryCompartment = geneCompartments.getPrimary_score();
				Map<String, Double> secondaryCompartments = geneCompartments.getSecondary_location();
				Map<String, String> secondaryCompartmentsAbb = geneCompartments.getSecondary_location_abb();

				compartmentsDatabaseIDs = ModelAPI.getCompartmentsDatabaseIDs(primaryCompartment, primaryCompartmentAbb, secondaryCompartments, secondaryCompartmentsAbb, compartmentsDatabaseIDs, statement);
				//associate gene to compartments

				ModelAPI.loadGenesCompartments(idGene, compartmentsDatabaseIDs, statement, primaryCompartment, scorePrimaryCompartment, secondaryCompartments);
			}

			Map<Integer,String> compartmentsAbb_ids = ModelAPI.getIdCompartmentAbbMap(statement);
			Map<String,Integer> idCompartmentAbbIdMap = ModelAPI.getCompartmentAbbIdMap(statement);

			CompartmentsProcesses processCompartments = new CompartmentsProcesses();
			CompartmentsIntegrationServices.autoSetInteriorCompartment(processCompartments.getInteriorCompartment(), statement);

			for(String ecNumber : enzymesReactions.keySet()) {
				//Compartmentalize reactions
				List<Integer> idReactions = enzymesReactions.get(ecNumber);
				for(Integer idReaction : idReactions) {

					Map<String, Object> subMap = ModelAPI.getDatabaseReactionContainer(idReaction, statement);

					ReactionContainer databaseReactionContainer = ModelDatabaseLoadingServices.getDatabaseReactionContainer(idReaction, subMap, statement);
					List<Integer> enzymeCompartments = ModelAPI.getEnzymeCompartments(ecNumber, statement);
					if(compartmentsAbb_ids.size()>0)
						processCompartments.setProcessCompartmentsInitiated(true);
					Set<Integer> parsedCompartments = processCompartments.parseCompartments(enzymeCompartments, compartmentsAbb_ids,idCompartmentAbbIdMap, null);

					boolean inModelFromCompartment = databaseReactionContainer.isInModel();
					//all enzyme compartments are assigned to the reactions
					for(int idCompartment: parsedCompartments) {

						if(idCompartment>0) {

							if(processCompartments.getIgnoreCompartmentsID().contains(idCompartment))
								inModelFromCompartment = false;

							ModelDatabaseLoadingServices.loadReaction(idCompartment, inModelFromCompartment, databaseReactionContainer, ecNumber, statement, false);
						}
					}
				}
			}
		}

		return true;
	}
	

}
