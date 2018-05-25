package pt.uminho.ceb.biosystems.merlin.transporters.core.compartments;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import pt.uminho.ceb.biosystems.merlin.database.connector.databaseAPI.CompartmentsAPI;
import pt.uminho.ceb.biosystems.merlin.database.connector.databaseAPI.TransportersAPI;
import pt.uminho.ceb.biosystems.merlin.transporters.core.utils.TransportersUtilities;
import pt.uminho.ceb.biosystems.merlin.utilities.Pair;

public class LoadCompartments {


	/**
	 * @param locust_tag
	 * @param probabilities
	 * @param project_id
	 * @param statement
	 */
	public static void loadData(String locust_tag, List<Pair<String, Double>> probabilities, int project_id, Statement statement){

		try {
			
			LoadCompartments.initCompartments(statement);
			
			String idLT = TransportersAPI.getIdLocusTag(locust_tag, project_id, statement);
			
			for(Pair<String, Double> compartment: probabilities) {
				
				Double nComp = compartment.getB();
				
				if(nComp>=0) {
					
					String abb = compartment.getA();
					
					String name = TransportersUtilities.parseAbbreviation(compartment.getA());
					
					TransportersAPI.insertIntoCompartments(idLT, name, abb, nComp, statement);
				}
			}
		} 
		catch (SQLException e)  {

			e.printStackTrace();
		}
	}

	/**
	 * @param threshold
	 * @param knn
	 * @param project_id
	 * @return
	 * @throws SQLException
	 */
	public static Map<String, GeneCompartments> getBestCompartmenForGene(double threshold, int knn, int projectID, Statement statement) throws SQLException {
		
		Map<String, GeneCompartments> compartments = new HashMap<String, GeneCompartments>();

		ArrayList<String[]> result = TransportersAPI.getBestCompartmenForGene(projectID, statement);
		
			double score;
			
			for(int i=0; i<result.size(); i++){
				String[] list = result.get(i);
				
				String geneID = list[0];
				String gene = list[1];
				score = Double.parseDouble(list[2]);  
				String abbreviation = list[3];		
				String name = list[4];				

				if(!abbreviation.contains("_")) {
					
					if(compartments.keySet().contains(geneID)) {
						
						GeneCompartments geneCompartment = compartments.get(geneID);
						score=(score)/(knn)*100;
						
						if((geneCompartment.getPrimary_score()-score)<=threshold) {
							
							geneCompartment.setDualLocalisation(true);
							geneCompartment.addSecondaryLocation(name, abbreviation, score);
							compartments.put(geneID, geneCompartment);
						}
					}
					else {
						
						score = (score)/(knn)*100;
						GeneCompartments geneCompartments = new GeneCompartments(geneID, gene, name, abbreviation, score);
						compartments.put(geneID, geneCompartments);
					}
				}
			}
			return compartments;
	}

	/**
	 * @return
	 *  'csk' => 'cytoskeletal',
	 *  'cyt' | 'cyto' => 'cytoplasmic', 
	 *  'nuc' | 'nucl' => 'nuclear', 
	 *  'mit' => 'mitochondrial', 
	 *  'ves' => 'vesicles of secretory system', 
	 *  'end' => 'endoplasmic reticulum', 
	 *  'gol' => 'Golgi',
	 *  'vac' => 'vacuolar',
	 *  'pla' => 'plasma membrane', 
	 *  'pox' => 'peroxisomal', 
	 *  'exc' => 'extracellular, including cell wall', 
	 *  '---' => 'other' 
	 */
	private static boolean initCompartments(Statement statement){

		Map<String, String> compartments = new TreeMap<String, String>();
		compartments.put("plas","plasma_membrane");
		compartments.put("golg","golgi");
		compartments.put("vaco","vacuolar");
		compartments.put("mito","mitochondrial");
		compartments.put("ves","vesicles_of_secretory_system");
		compartments.put("E.R.","endoplasmic_reticulum");
		compartments.put("---","other");
		compartments.put("nucl","nuclear");
		compartments.put("cyto","cytoplasmic");
		compartments.put("cysk","cytoskeletal");
		compartments.put("extr","extracellular");
		compartments.put("pero","peroxisomal");

		try
		{
			CompartmentsAPI.initCompartments(compartments, statement);
		}
		catch (SQLException e) {e.printStackTrace();return false;}
		return true;

	}

}
