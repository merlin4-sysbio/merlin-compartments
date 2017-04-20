package pt.uminho.sysbio.common.transporters.core.compartments;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import pt.uminho.sysbio.common.transporters.core.utils.TransportersUtilities;
import pt.uminho.sysbio.merlin.utilities.Pair;

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
			
			java.sql.Date sqlToday = new java.sql.Date((new java.util.Date()).getTime());
			
			ResultSet rs = statement.executeQuery("SELECT id FROM psort_reports WHERE locus_tag='"+locust_tag+"' AND project_id = "+project_id);
			if(!rs.next()) {
				
				statement.execute("INSERT INTO psort_reports (locus_tag, project_id, date) VALUES('"+locust_tag+"', "+project_id+",'"+sqlToday+"')");
				rs = statement.executeQuery("SELECT LAST_INSERT_ID()");
				rs.next();
			}
			
			String idLT = rs.getString(1);

			for(Pair<String, Double> compartment: probabilities) {

				if(compartment.getB()>=0) {

					rs = statement.executeQuery("SELECT id FROM compartments WHERE abbreviation='"+compartment.getA().toUpperCase()+"'");

					if(!rs.next()) {

						statement.execute("INSERT INTO compartments (name,abbreviation) VALUES('"+TransportersUtilities.parseAbbreviation(compartment.getA())+"', '"+compartment.getA().toUpperCase()+"')");
						rs = statement.executeQuery("SELECT LAST_INSERT_ID()");
						rs.next();
					}
					String idHIT = rs.getString(1);

					rs = statement.executeQuery("SELECT * FROM psort_reports_has_compartments WHERE psort_report_id='"+idLT+"' AND compartment_id='"+idHIT+"'");

					if(!rs.next())
						statement.execute("INSERT INTO psort_reports_has_compartments (psort_report_id, compartment_id, score) VALUES("+idLT+","+idHIT+","+compartment.getB()+")");
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
	public static Map<String, GeneCompartments> getBestCompartmenForGene(double threshold, int knn, int project_id, Statement statement) throws SQLException {

		Map<String, GeneCompartments> compartments = new HashMap<String, GeneCompartments>();
		ResultSet rs;

			rs = statement.executeQuery("SELECT psort_report_id, locus_tag, score, abbreviation, name FROM psort_reports_has_compartments " +
					"INNER JOIN psort_reports ON psort_reports.id=psort_report_id " +
					"INNER JOIN compartments ON compartments.id=compartment_id " +
					"WHERE project_id = "+project_id+" "+
					"ORDER BY psort_report_id ASC, score DESC;"
					);
			
			double score = 0;
			
			while(rs.next()) {
				
				if(!rs.getString(4).contains("_")) {
					
					String geneID = rs.getString(2);	
					if(compartments.keySet().contains(geneID)) {
						
						GeneCompartments geneCompartment = compartments.get(geneID);
						score=(rs.getDouble(3))/(knn)*100;
						
						if((geneCompartment.getPrimary_score()-score)<=threshold) {
							
							geneCompartment.setDualLocalisation(true);
							geneCompartment.addSecondaryLocation(rs.getString(5), rs.getString(4), score);
							compartments.put(geneID, geneCompartment);
						}
					}
					else {
						
						score = (rs.getDouble(3))/(knn)*100;
						GeneCompartments geneCompartments = new GeneCompartments(geneID, rs.getString(2), rs.getString(5),rs.getString(4), score);
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
			for(String abbreviation:compartments.keySet()) {
				
				ResultSet rs = statement.executeQuery("Select * FROM compartments WHERE name='"+compartments.get(abbreviation)+"'");
				
				if(!rs.next())
					statement.execute("INSERT INTO compartments (name,abbreviation) VALUES('"+compartments.get(abbreviation)+"', '"+abbreviation.toUpperCase()+"')");
			}
		}
		catch (SQLException e) {e.printStackTrace();return false;}
		return true;

	}

	
}
