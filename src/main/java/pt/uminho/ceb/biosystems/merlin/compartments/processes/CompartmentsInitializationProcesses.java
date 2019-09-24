package pt.uminho.ceb.biosystems.merlin.compartments.processes;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import pt.uminho.ceb.biosystems.merlin.compartments.datatype.AnnotationCompartmentsGenes;
import pt.uminho.ceb.biosystems.merlin.compartments.utils.CompartmentsUtilities;
import pt.uminho.ceb.biosystems.merlin.core.containers.model.CompartmentContainer;
import pt.uminho.ceb.biosystems.merlin.services.annotation.AnnotationCompartmentsServices;
import pt.uminho.ceb.biosystems.merlin.services.model.ModelCompartmentServices;
import pt.uminho.ceb.biosystems.mew.utilities.datastructures.pair.Pair;

public class CompartmentsInitializationProcesses {


	/**
	 * @param locust_tag
	 * @param probabilities
	 * @param project_id
	 * @param statement
	 * @throws Exception 
	 */
	public static void loadData(String databaseName, String locust_tag, List<Pair<String, Double>> probabilities, Statement statement) throws Exception{

		try {

			CompartmentsInitializationProcesses.initCompartments(databaseName);

			Integer idLT = AnnotationCompartmentsServices.getIdentifierLocusTag(databaseName,locust_tag);

			for(Pair<String, Double> compartment: probabilities) {

				Double nComp = compartment.getB();

				if(nComp>=0) {

					String abb = compartment.getA();

					String name = CompartmentsUtilities.parseAbbreviation(compartment.getA());

					AnnotationCompartmentsServices.insertIntoCompartments(databaseName, idLT, name, abb, nComp);
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
	public static Map<Integer, AnnotationCompartmentsGenes> getBestCompartmenForGene(String databaseName, double threshold, int knn) throws Exception {

		Map<Integer, AnnotationCompartmentsGenes> compartments = new HashMap<>();

		List<CompartmentContainer> containers = AnnotationCompartmentsServices.getBestCompartmenForGene(databaseName);

		double score;
		for(CompartmentContainer container : containers) {

			int geneID = container.getReportID();
			String gene = container.getLocusTag();
			score = container.getScore();
			String abbreviation = container.getAbbreviation();		
			String name = container.getName();

			if(!abbreviation.contains("_")) {

				if(compartments.keySet().contains(geneID)) {

					AnnotationCompartmentsGenes geneCompartment = compartments.get(geneID);
					score=(score)/(knn)*100;

					if((geneCompartment.getPrimary_score()-score)<=threshold) {

						geneCompartment.setDualLocalisation(true);
						geneCompartment.addSecondaryLocation(name, abbreviation, score);
						compartments.put(geneID, geneCompartment);
					}
				}
				else {

					score = (score)/(knn)*100;
					AnnotationCompartmentsGenes geneCompartments = new AnnotationCompartmentsGenes(geneID, gene, name, abbreviation, score);
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
	private static boolean initCompartments(String databaseName){

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
			ModelCompartmentServices.initCompartments(databaseName, compartments);
		}
		catch (Exception e) {e.printStackTrace();return false;}
		return true;

	}

}
