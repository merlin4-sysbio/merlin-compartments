package pt.uminho.ceb.biosystems.merlin.compartments.services;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import pt.uminho.ceb.biosystems.merlin.bioapis.externalAPI.ncbi.EntrezLink.KINGDOM;
import pt.uminho.ceb.biosystems.merlin.compartments.datatype.AnnotationCompartmentsGenes;
import pt.uminho.ceb.biosystems.merlin.compartments.interfaces.ICompartmentsServices;
import pt.uminho.ceb.biosystems.merlin.core.datatypes.WorkspaceDataTable;
import pt.uminho.ceb.biosystems.merlin.core.datatypes.WorkspaceGenericDataTable;
import pt.uminho.ceb.biosystems.merlin.core.interfaces.ICompartmentResult;
import pt.uminho.ceb.biosystems.merlin.core.utilities.Enumerators.CompartmentsTool;
import pt.uminho.ceb.biosystems.merlin.database.connector.databaseAPI.CompartmentsAPI;
import pt.uminho.ceb.biosystems.merlin.database.connector.databaseAPI.HomologyAPI;
import pt.uminho.ceb.biosystems.merlin.database.connector.databaseAPI.ProjectAPI;
import pt.uminho.ceb.biosystems.merlin.database.connector.datatypes.Connection;
import pt.uminho.ceb.biosystems.merlin.services.ProjectServices;
import pt.uminho.ceb.biosystems.merlin.utilities.Utilities;

public class CompartmentsAnnotationServices {


	/* (non-Javadoc)
	 * @see datatypes.metabolic_regulatory.Entity#getStats()
	 */
	public static String[][] getStats(Connection connection) {
		
		String[][] res = new String[2][];

		int num = 0, num_comp = 0;

		try {
			
			Statement stmt = connection.createStatement();
			
			num = HomologyAPI.getNumberOfGenes(stmt);
			
			num_comp = CompartmentsAPI.getNumberOfCompartments(stmt);

			res[0] = new String[] {"Number of genes", ""+num};
			res[1] = new String[] {"Number of distinct compartments ", ""+num_comp};
			
			stmt.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return res;

	}

	/**
	 * @return
	 */
	public static WorkspaceGenericDataTable getInfo(double threshold, Connection connection) {

//		ids = new TreeMap<Integer,String>();
		
		ArrayList<String> columnsNames = new ArrayList<String>();

		columnsNames.add("info");
		columnsNames.add("genes");
		columnsNames.add("primary compartment");
		columnsNames.add("score");
		columnsNames.add("secondary compartments");
		columnsNames.add("scores");
//		columnsNames.add("Edit");

		WorkspaceGenericDataTable qrt = new WorkspaceGenericDataTable(columnsNames, "genes", "gene data"){
			private static final long serialVersionUID = 1L;
			@Override
			public boolean isCellEditable(int row, int col) {

				if (col==0 || col==4) {

					return true;
				}
				else return false;
			}
		};

		try {
			Statement statement = connection.createStatement();

			Map<String, AnnotationCompartmentsGenes> geneCompartments = runCompartmentsInterface(threshold, statement);
			
			if(geneCompartments != null){

//				int g = 0;

				List<String> collection = new ArrayList<>(geneCompartments.keySet());

				Collections.sort(collection);

				Map<String, String> allLocusTag = CompartmentsAPI.getAllLocusTag(statement);

				for(String query : collection) {
					
					AnnotationCompartmentsGenes geneCompartment = geneCompartments.get(query);
//					String id = geneCompartment.getGeneID();
					
					ArrayList<Object> ql = new ArrayList<Object>();
					ql.add("");
//					this.ids.put(g, id);

					String locusTag = allLocusTag.get(query);
					
					if(locusTag != null){
//						this.names.put(g, locusTag);
						ql.add(locusTag);
					}
					else{
//						this.names.put(g, query);
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

					//				ql.add("");
					qrt.addLine(ql, geneCompartment.getGeneID());

//					g+=1;
				}
			}
			statement.close();
		} 
		catch (SQLException ex) {

			ex.printStackTrace();
		}

		return qrt;
	}

	public static WorkspaceDataTable[] getRowInfo(String id, Connection connection) {

		WorkspaceDataTable[] results = new WorkspaceDataTable[1];

		List<String> columnsNames = new ArrayList<String>();
		columnsNames.add("compartment");
		columnsNames.add("score");
		results[0] = new WorkspaceDataTable(columnsNames, "compartments");

		Statement stmt;

		try {

			stmt = connection.createStatement();

			ArrayList<String[]> data = ProjectAPI.getRowInfo(id, stmt);

			for(int i=0; i<data.size(); i++){
				String[] list = data.get(i);
				
				ArrayList<String> resultsList = new ArrayList<>();

				resultsList.add(list[0]);
				Double score = Double.parseDouble(list[1])/10;
				resultsList.add(score.toString());

				results[0].addLine(resultsList);
			}
			stmt.close();
		} 
		catch (SQLException ex) {

			ex.printStackTrace();
		}
		return results;
	}
	
	/**
	 * @param projectLineage
	 * @param tool
	 * @param results
	 * @param statement
	 * @throws Exception
	 */
	public static void loadPredictions(String projectLineage, String tool, Map<String, ICompartmentResult> results, Statement statement) throws Exception {

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

				if(ProjectServices.areCompartmentsPredicted(statement))
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

				if(ProjectServices.areCompartmentsPredicted(statement))
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
	
	public static Map<String, AnnotationCompartmentsGenes> runCompartmentsInterface(double threshold, Statement statement){
		
		Map<String, AnnotationCompartmentsGenes> geneCompartments = null;
		
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
