package pt.uminho.sysbio.common.transporters.core.compartments;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import pt.uminho.sysbio.common.database.connector.datatypes.Connection;
import pt.uminho.sysbio.merlin.utilities.Pair;

public class LoadCompartments {


	private Connection conn;
	//private String databaseName;



	/**
	 * @param msqlmt
	 * @throws SQLException 
	 */
	public LoadCompartments(Connection connection) {
		//try {

		this.conn = connection;
		this.initCompartments();
		//this.setDatabaseName(msqlmt.get_database_name());
		//}
		//catch (SQLException e) {e.printStackTrace();}

	}


	/**
	 * @param locust_tag
	 * @param probabilities
	 */
	public void loadData(String locust_tag, List<Pair<String, Double>> probabilities, int project_id){

		try {

			java.sql.Date sqlToday = new java.sql.Date((new java.util.Date()).getTime());
			Statement stmt = this.conn.createStatement();

			ResultSet rs = stmt.executeQuery("SELECT id FROM psort_reports WHERE locus_tag='"+locust_tag+"' AND project_id = "+project_id);
			if(!rs.next())
			{
				stmt.execute("INSERT INTO psort_reports (locus_tag, project_id, date) VALUES('"+locust_tag+"', "+project_id+",'"+sqlToday+"')");
				rs = stmt.executeQuery("SELECT LAST_INSERT_ID()");
				rs.next();
			}
			String idLT = rs.getString(1);

			for(Pair<String, Double> compartment: probabilities) {

				if(compartment.getB()>0) {

					rs = stmt.executeQuery("SELECT id FROM compartments WHERE abbreviation='"+compartment.getA()+"'");

					if(!rs.next()) {

						stmt.execute("INSERT INTO compartments (name,abbreviation) VALUES('"+LoadCompartments.parseAbbreviation(compartment.getA())+"', '"+compartment.getA()+"')");
						rs = stmt.executeQuery("SELECT LAST_INSERT_ID()");
						rs.next();
					}
					String idHIT = rs.getString(1);

					rs = stmt.executeQuery("SELECT * FROM psort_reports_has_compartments WHERE psort_report_id='"+idLT+"' AND compartment_id='"+idHIT+"'");

					if(!rs.next()) {

						stmt.execute("INSERT INTO psort_reports_has_compartments (psort_report_id, compartment_id, score) VALUES("+idLT+","+idHIT+","+compartment.getB()+")");
					}
				}
			}
		} 
		catch (SQLException e)  {

			e.printStackTrace();
		}
	}

	/**
	 * @return
	 * @throws SQLException 
	 */
	public Map<String, GeneCompartments> getBestCompartmenForGene(double threshold, int knn, int project_id) throws SQLException {

		Map<String, GeneCompartments> compartments = new HashMap<String, GeneCompartments>();
		Statement stmt;
		ResultSet rs;

			stmt = this.conn.createStatement();
			rs = stmt.executeQuery("SELECT psort_report_id, locus_tag, score, abbreviation, name FROM psort_reports_has_compartments " +
					"LEFT JOIN psort_reports ON psort_reports.id=psort_report_id " +
					"LEFT JOIN compartments ON compartments.id=compartment_id " +
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
			stmt.close();
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
	private boolean initCompartments(){

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
			Statement stmt = this.conn.createStatement();
			for(String abbreviation:compartments.keySet())
			{
				ResultSet rs = stmt.executeQuery("Select * FROM compartments WHERE name='"+compartments.get(abbreviation)+"'");
				if(!rs.next())
				{
					stmt.execute("INSERT INTO compartments (name,abbreviation) VALUES('"+compartments.get(abbreviation)+"', '"+abbreviation.toUpperCase()+"')");
				}
			}
		}
		catch (SQLException e) {e.printStackTrace();return false;}
		return true;

	}

	/**
	 * @param abbreviation
	 * @return
	 */
	public static String parseAbbreviation(String abbreviation){


		if(abbreviation.equals("permem"))
		{
			return "peroxisomal_membrane";
		}
		if(abbreviation.equals("ermem"))
		{
			return "ER_membrane";
		}
		if(abbreviation.equals("vacmem"))
		{
			return "vacuolar_membrane";
		}
		if(abbreviation.equals("golmem"))
		{
			return "golgi_membrane";
		}
		if(abbreviation.equals("nucmem"))
		{
			return "nuclear_membrane";
		}
		if(abbreviation.equals("unkn"))
		{
			return "unknown";
		}
		if(abbreviation.equals("cytmem"))
		{
			return "cytoplasmic_membrane";
		}
		if(abbreviation.equals("perip"))
		{
			return "periplasmic";
		}
		if(abbreviation.equals("outme"))
		{
			return "outer_membrane";
		}
		if(abbreviation.equals("cytop"))
		{
			return "cytoplasmic";
		}
		if(abbreviation.equals("pla") || abbreviation.equals("plas"))
		{
			return "plasma_membrane";
		}
		else if(abbreviation.equals("gol") || abbreviation.equals("golg"))
		{
			return "golgi_apparatus";
		}
		else if(abbreviation.equals("vac") || abbreviation.equals("vacu"))
		{
			return "vacuolar_membrane";
		}
		else if(abbreviation.equals("mit") || abbreviation.equals("mito"))
		{
			return "mitochondria";
		}
		else if(abbreviation.equals("ves") || abbreviation.equals("vesi"))
		{
			return "vesicles_of_secretory_system";
		}
		else if(abbreviation.equals("end") || abbreviation.equals("E.R.")) 
		{
			return "endoplasmic_reticulum";
		}
		else if(abbreviation.equals("---"))
		{
			return "other";
		}
		else if(abbreviation.equals("nuc") || abbreviation.equals("nucl"))
		{
			return "nuclear";
		}
		else if(abbreviation.equals("cyt") || abbreviation.equals("cyto"))
		{
			return "cytosol";
		}
		else if(abbreviation.equals("csk") || abbreviation.equals("cysk"))
		{
			return "cytoskeleton";
		}
		else if(abbreviation.equals("exc") || abbreviation.equals("extr"))
		{
			return "extracellular";
		}
		else if(abbreviation.equals("pox") || abbreviation.equals("pero"))
		{
			return "peroxisome";
		}
		else if(abbreviation.equals("chlo"))
		{
			return "chloroplast";
		}
		else if(abbreviation.equals("lyso"))
		{
			return "lysosome";
		}
		else if(abbreviation.contains("_"))
		{
			String compartment = "";
			String[] dual_compartment = abbreviation.split("_");
			for(int i = 0; i<dual_compartment.length;i++)
			{
				if(i!=0)
				{
					compartment=compartment.concat("_");
				}
				compartment=compartment.concat(dual_compartment[i]);
			}
			return compartment;
		}
		else
		{				
			System.out.println("returning\t"+ abbreviation);
			return abbreviation;

		}
	}
}
