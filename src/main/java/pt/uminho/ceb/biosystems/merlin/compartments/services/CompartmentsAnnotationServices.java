package pt.uminho.ceb.biosystems.merlin.compartments.services;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.uminho.ceb.biosystems.merlin.bioapis.externalAPI.ncbi.EntrezLink.KINGDOM;
import pt.uminho.ceb.biosystems.merlin.compartments.datatype.AnnotationCompartmentsGenes;
import pt.uminho.ceb.biosystems.merlin.compartments.interfaces.ICompartmentsServices;
import pt.uminho.ceb.biosystems.merlin.core.interfaces.ICompartmentResult;
import pt.uminho.ceb.biosystems.merlin.core.utilities.Enumerators.CompartmentsTool;
import pt.uminho.ceb.biosystems.merlin.database.connector.databaseAPI.CompartmentsAPI;
import pt.uminho.ceb.biosystems.merlin.database.connector.databaseAPI.HomologyAPI;
import pt.uminho.ceb.biosystems.merlin.database.connector.databaseAPI.ProjectAPI;
import pt.uminho.ceb.biosystems.merlin.database.connector.datatypes.Connection;
import pt.uminho.ceb.biosystems.merlin.services.ProjectServices;
import pt.uminho.ceb.biosystems.merlin.services.annotation.AnnotationCompartmentsServices;
import pt.uminho.ceb.biosystems.merlin.utilities.Utilities;

public class CompartmentsAnnotationServices {

	/**
	 * @param threshold
	 * @param names
	 * @param identifiers
	 * @param connection
	 * @return
	 */
	public static Map<Integer, ArrayList<Object>> getMainTableData(double threshold, Map<Integer, String> names, Map<Integer,Integer> identifiers, Connection connection) {
		
		Map<Integer, ArrayList<Object>> dataTable = new HashMap<>();
		
		try {
			
			Statement statement = connection.createStatement();

			Map<Integer, AnnotationCompartmentsGenes> geneCompartments = runCompartmentsInterface(threshold, statement);
			
			if(geneCompartments != null) {

				int gene = 0;

				List<Integer> collection = new ArrayList<>(geneCompartments.keySet());

				Collections.sort(collection);

				Map<Integer, String> allLocusTag = CompartmentsAPI.getAllLocusTag(statement);

				for(int query : collection) {
					
					AnnotationCompartmentsGenes geneCompartment = geneCompartments.get(query);
					int id = geneCompartment.getGeneID();
					
					ArrayList<Object> ql = new ArrayList<Object>();
					ql.add("");
					identifiers.put(gene, id);
					
					String locusTag = allLocusTag.get(query);
					names.put(gene, locusTag);
					
					if(locusTag != null){

						ql.add(locusTag);
					}
					else {
						
						names.put(gene, locusTag);
						ql.add(query);
					}
					
					ql.add(geneCompartment.getPrimary_location());
					double maxScore = geneCompartment.getPrimary_score()/100; 
					ql.add(Utilities.round(maxScore,2)+"");

					String secondaryCompartments = "", secondaryScores = "";

					for(String key : geneCompartment.getSecondary_location().keySet()) {

						secondaryCompartments += key + ", ";
						secondaryScores += Utilities.round(geneCompartment.getSecondary_location().get(key)/100, 2)+ ", ";
					}

					if (secondaryCompartments.length() != 0) {

						secondaryCompartments = secondaryCompartments.substring(0, secondaryCompartments.length()-2);
						secondaryScores = secondaryScores.substring(0, secondaryScores.length()-2);
					}

					ql.add(secondaryCompartments);
					ql.add(secondaryScores);

					dataTable.put(geneCompartment.getGeneID(), ql);
					
					gene+=1;
				}
			}
			statement.close();
		} 
		catch (SQLException ex) {

			ex.printStackTrace();
		}

		return dataTable;
	}
	
	/**
	 * @param projectLineage
	 * @param tool
	 * @param results
	 * @param statement
	 * @throws Exception
	 */
	public static void loadPredictions(String databaseName, String projectLineage, String tool, Map<String, ICompartmentResult> results,
			Statement statement) throws Exception {

		try {

			boolean type = false;
			String projectKingdom = projectLineage.split(";")[0];
			KINGDOM kingdom = KINGDOM.valueOf(projectKingdom);
			if (projectLineage.contains("Viridiplantae"))
				type = true;
			ICompartmentsServices compartmentsInterface = null;

			boolean go=false;

			if(kingdom.equals(KINGDOM.Eukaryota)) {

				compartmentsInterface = new ComparmentsImportLocTreeServices();
				((ComparmentsImportLocTreeServices) compartmentsInterface).setPlant(type);

				if(AnnotationCompartmentsServices.areCompartmentsPredicted(databaseName))
					go = false;
				else
					go = compartmentsInterface.getCompartments(null);
			}
			else {
				CompartmentsTool compartmentsTool = CompartmentsTool.valueOf(tool);
				if(compartmentsTool.equals(CompartmentsTool.PSort))
					compartmentsInterface = new ComparmentsImportPSort3Services();
				if(compartmentsTool.equals(CompartmentsTool.LocTree))
					compartmentsInterface = new ComparmentsImportLocTreeServices();
				if(compartmentsTool.equals(CompartmentsTool.WoLFPSORT))
					compartmentsInterface = new ComparmentsImportWolfPsortServices();

				if(AnnotationCompartmentsServices.areCompartmentsPredicted(databaseName))
					go=false;
				else							
					go = compartmentsInterface.getCompartments(null);
			}
			
			if(go)
				compartmentsInterface.loadCompartmentsInformation(results, statement);

		} 
		catch (Exception e1) {

//			Workbench.getInstance().error("An error occurred while loading compartments prediction.");
			throw new Exception("An error occurred while loading compartments prediction.");
		}
	}
	
	/**
	 * @param threshold
	 * @param statement
	 * @return
	 */
	public static Map<Integer, AnnotationCompartmentsGenes> runCompartmentsInterface(double threshold, Statement statement){
		
		Map<Integer, AnnotationCompartmentsGenes> geneCompartments = null;
		
		try {
			
			ICompartmentsServices compartmentsInterface = null;
			String cTool = ProjectAPI.getCompartmentsTool(statement);
			
			if(cTool!=null) {
				
				CompartmentsTool compartmentsTool = CompartmentsTool.valueOf(cTool);
				
				if(compartmentsTool.equals(CompartmentsTool.PSort))
					compartmentsInterface = new ComparmentsImportPSort3Services();
				if(compartmentsTool.equals(CompartmentsTool.LocTree))
					compartmentsInterface = new ComparmentsImportLocTreeServices();
				if(compartmentsTool.equals(CompartmentsTool.WoLFPSORT))
					compartmentsInterface = new ComparmentsImportWolfPsortServices();
				
				geneCompartments = compartmentsInterface.getBestCompartmentsByGene(threshold, statement);
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return geneCompartments;
	}
	
}
