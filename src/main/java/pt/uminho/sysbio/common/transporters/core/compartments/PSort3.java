/**
 * 
 */
package pt.uminho.sysbio.common.transporters.core.compartments;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import pt.uminho.sysbio.common.bioapis.externalAPI.ncbi.NcbiAPI;
import pt.uminho.sysbio.common.database.connector.datatypes.Connection;

/**
 * @author ODias
 *
 */
public class PSort3 implements CompartmentsInterface{

	private static int normalization=10;
	private Map<String, CompartmentResult> results;
	private LoadCompartments loadCompartments;
	private AtomicBoolean cancel;
	private boolean isNCBIGenome = true;
	private int project_id;


//	/**
//	 * @param conn
//	 * @param results
//	 * @param project_id
//	 */
//	public PSort3(Connection conn, Map<String, CompartmentResult> results, int project_id) {
//		this.cancel = new AtomicBoolean(false);
//		this.loadCompartments = new LoadCompartments(conn);
//		this.results = results;
//		this.project_id = project_id;
//	}

	/**
	 * @param conn
	 * @param results
	 * @param isNCBI
	 */
	public PSort3(Connection conn, Map<String, CompartmentResult> results, boolean isNCBI, int project_id) {

		this.isNCBIGenome = isNCBI;
		this.cancel = new AtomicBoolean(false);
		this.loadCompartments = new LoadCompartments(conn);
		this.results = results;
		this.project_id = project_id;
	}

	/**
	 * @param connection
	 */
	public PSort3(Connection connection, int project_id) {

		this.cancel = new AtomicBoolean(false);
		this.loadCompartments = new LoadCompartments(connection);
		this.project_id = project_id;
	}

	/**
	 * @param connection
	 * @param isNCBI
	 */
	public PSort3(Connection connection, boolean isNCBI, int project_id) {

		this.isNCBIGenome = isNCBI;
		this.cancel = new AtomicBoolean(false);
		this.loadCompartments = new LoadCompartments(connection);
		this.project_id = project_id;
	}

	/**
	 * 
	 */
	public PSort3() {

		this.cancel = new AtomicBoolean(false);
	}

	/**
	 * @param isNCBI
	 */
	public PSort3(boolean isNCBI, int project_id) {

		this.isNCBIGenome = isNCBI;
		this.cancel = new AtomicBoolean(false);
		this.project_id = project_id;
	}

	/* (non-Javadoc)
	 * @see compartments.CompartmentsInterface#loadCompartmentsInformation()
	 */
	public void loadCompartmentsInformation() {

		for(CompartmentResult pSORT3Result : this.results.values())
			this.loadCompartments.loadData(pSORT3Result.getGeneID(), pSORT3Result.getCompartments(), project_id);
	}

	/**
	 * @param outFile
	 * @return
	 * @throws Exception 
	 */
	public Map<String, CompartmentResult> addGeneInformation(File outFile) throws Exception {

		Map<String, CompartmentResult> compartmentLists = this.readPSortFile(outFile);

		Map<String, CompartmentResult> compartmentResults = compartmentLists;

		if(this.isNCBIGenome)
			compartmentResults = this.getLocusTags(compartmentLists);

		return compartmentResults;

	}


	/**
	 * @param outFile
	 * @return
	 * @throws NumberFormatException
	 * @throws IOException
	 */
	private Map<String, CompartmentResult> readPSortFile(File outFile) throws NumberFormatException, IOException {

		Map<String, CompartmentResult> compartmentLists = new HashMap<>();

		BufferedReader in = new BufferedReader(new FileReader(outFile));
		String str;
		int cytoplasmicMembrane_Score_index = -1;
		int seqID_index = -1;
		int periplasmic_Score_index = -1;
		int outerMembrane_Score_index = -1;
		int extracellular_Score_index= -1;
		int cytoplasmic_Score_index= -1;
		int final_Localization_index=-1;
		int cellwall_score_index = -1;

		boolean firstLine=true;

		while ((str = in.readLine()) != null && !this.cancel.get()) {

			if(!str.isEmpty()){

				String[] line = str.split("\t");

				if(firstLine) {

					firstLine=false;
					for(int i=0;i<line.length;i++) {

						if(line[i].trim().equalsIgnoreCase("SeqID")){seqID_index=i;}
						if(line[i].trim().replace(" ","").equalsIgnoreCase("CytoplasmicMembrane_Score")){cytoplasmicMembrane_Score_index=i;}
						if(line[i].trim().equalsIgnoreCase("periplasmic_Score")){periplasmic_Score_index=i;}
						if(line[i].trim().equalsIgnoreCase("OuterMembrane_Score")){outerMembrane_Score_index=i;}
						if(line[i].trim().equalsIgnoreCase("Extracellular_Score")){extracellular_Score_index=i;}
						if(line[i].trim().equalsIgnoreCase("Cytoplasmic_Score")){cytoplasmic_Score_index=i;}
						if(line[i].trim().equalsIgnoreCase("Final_Localization")){final_Localization_index=i;}
						if(line[i].trim().equalsIgnoreCase("Cellwall_Score")){cellwall_score_index=i;}
					}

					if(final_Localization_index<0) {

						in.close();
						return null;
					}
				}
				else {

					PSort3Result pSort3Result = new PSort3Result(line[seqID_index]);
					String locus_tag = line[seqID_index].split(" ")[0].split("\\|")[3];

					boolean unknown=true;

					Double.valueOf(line[cytoplasmicMembrane_Score_index]);
					if(Double.valueOf(line[cytoplasmicMembrane_Score_index])>3)
					{
						unknown=false;
					}

					if(periplasmic_Score_index>0 && Double.valueOf(line[periplasmic_Score_index])>3) {

						unknown=false;
					}

					if(outerMembrane_Score_index>0 && Double.valueOf(line[outerMembrane_Score_index])>3) {

						unknown=false;
					}

					if(cellwall_score_index>0 && Double.valueOf(line[cellwall_score_index])>3) {

						unknown=false;
					}

					if(Double.valueOf(line[extracellular_Score_index])>3) {

						unknown=false;
					}
					if(Double.valueOf(line[cytoplasmic_Score_index])>3)
					{
						unknown=false;
					}

					double score;
					if(line[final_Localization_index].trim().equalsIgnoreCase("Unknown")&&unknown) {

						score = 10;
						pSort3Result.addCompartment("unkn", score);	
					}
					else {

						boolean maxFound=false, returnFinalLocalisation=false;
						score = Double.valueOf(line[cytoplasmicMembrane_Score_index]);
						pSort3Result.addCompartment("cytmem", score);
						if(score==10){maxFound=true;}

						score = Double.valueOf(line[extracellular_Score_index]);
						pSort3Result.addCompartment("extr", score);
						if(score==10)
						{
							if(maxFound){returnFinalLocalisation=true;}
							else{maxFound=true;}
						}

						if(periplasmic_Score_index>0) {

							score = Double.valueOf(line[periplasmic_Score_index]);
							pSort3Result.addCompartment("perip", score);
							if(score==10) {

								if(maxFound){returnFinalLocalisation=true;}
								else{maxFound=true;}
							}
						}

						if(outerMembrane_Score_index>0) {

							score = Double.valueOf(line[outerMembrane_Score_index]);
							pSort3Result.addCompartment("outme", score);
							if(score==10) {

								if(maxFound){returnFinalLocalisation=true;}
								else{maxFound=true;}
							}
						}

						if(cellwall_score_index>0) {

							score = Double.valueOf(line[cellwall_score_index]);
							pSort3Result.addCompartment("cellw", score);
							if(score==10) {

								if(maxFound){returnFinalLocalisation=true;}
								else{maxFound=true;}
							}
						}

						score = Double.valueOf(line[cytoplasmic_Score_index]);
						pSort3Result.addCompartment("cytop", score);
						if(score==10)
						{
							if(maxFound){returnFinalLocalisation=true;}
							else{maxFound=true;}
						}

						if(returnFinalLocalisation) {

							pSort3Result = new PSort3Result(line[seqID_index]);
							String out;
							if(line[final_Localization_index].trim().equalsIgnoreCase("Cytoplasmic")){out = "cytop";}
							else if(line[final_Localization_index].trim().equalsIgnoreCase("CytoplasmicMembrane")){out = "cytmem";}
							else if(line[final_Localization_index].trim().equalsIgnoreCase("Periplasmic")){out = "perip";}
							else if(line[final_Localization_index].trim().equalsIgnoreCase("OuterMembrane")){out = "outme";}
							else if(line[final_Localization_index].trim().equalsIgnoreCase("Cellwall")){out = "cellw";}
							else{out = "extr";}

							pSort3Result.addCompartment(out,10);
						}
					}
					compartmentLists.put(locus_tag, pSort3Result);
				}
			}
		}
		in.close();
		return compartmentLists;
	}


	/**
	 * 
	 * 
	 * @param compartmentLists
	 * @return
	 * @throws Exception
	 */
	private Map<String, CompartmentResult> getLocusTags(Map<String, CompartmentResult> compartmentLists) throws Exception {

		Map<String, CompartmentResult> compartmentResults = new HashMap<>();

		if (!this.cancel.get()) {
			
			Map<String, String> idLocus = NcbiAPI.getNCBILocusTags(compartmentLists.keySet(), 500);
			
			for (String id : idLocus.keySet()) {

				CompartmentResult pSort3Result = compartmentLists.get(id);
				pSort3Result.setGeneID(idLocus.get(id));
				compartmentResults.put(idLocus.get(id), pSort3Result);
			}
			
		}
		return compartmentResults;
	}

	/* (non-Javadoc)
	 * @see compartments.CompartmentsInterface#getBestCompartmentsByGene(double, int)
	 */
	public Map<String,GeneCompartments> getBestCompartmentsByGene(double threshold) throws SQLException{

		return loadCompartments.getBestCompartmenForGene(threshold, PSort3.normalization, this.project_id);
	}

	//	/**
	//	 * @param args
	//	 * @throws IOException
	//	 * @throws ParseException
	//	 */
	//	public static void main(String[] args) throws IOException, ParseException{
	//		String database_name= 
	//				"psort_ecoli";
	//		//"psort_hpylori";
	//		MySQLMultiThread mysql = new MySQLMultiThread("root","password","localhost",3306,database_name);
	//
	//		//PSort3 obj = new PSort3(mysql,PSort3.addGeneInformation(new File("C:/Users/ODias/Desktop/transport_eco_hpy/ecoli")));
	//		//obj.loadCompartmentsInformation();
	//		//obj.loadDatabase("C:/Users/ODias/Desktop/transport_eco_hpy/hpy");
	//		
	//		PSort3 obj = new PSort3(mysql);
	//		
	//		String path = FileUtils.getCurrentTempDirectory(database_name);
	//		FileWriter fstream = new FileWriter(path+database_name+"_compartments.xls");  
	//		System.out.println(path);
	//		BufferedWriter out = new BufferedWriter(fstream);  
	//		StringBuffer buffer = new StringBuffer();
	//		buffer.append("gene\tlocation\tscore\tis dual\tlocation\tscore\n");
	//
	//		Map<String,GeneCompartments> temp = obj.getBestCompartmentsForGene(10);
	//		for(String gene : temp.keySet())
	//		{
	//			if(temp.get(gene).isDualLocalisation())
	//			{
	//				buffer.append(temp.get(gene).getGene()+"\t"+temp.get(gene).getPrimary_location()+"\t"+temp.get(gene).getPrimary_score()+"\t"+temp.get(gene).isDualLocalisation()+"\t"+temp.get(gene).getSecondary_location()+"\t"+"\n");
	//			}
	//			else
	//			{
	//				buffer.append(temp.get(gene).getGene()+"\t"+temp.get(gene).getPrimary_location()+"\t"+temp.get(gene).getPrimary_score()+"\n");
	//			}
	//		}
	//		out.append(buffer);
	//		out.close();
	//		fstream.close();
	//	}

	@Override
	public boolean getCompartments(String string) {
		return true;
	}

	/**
	 * @return the cancel
	 */
	public AtomicBoolean isCancel() {
		return cancel;
	}

	/**
	 * @param cancel the cancel to set
	 */
	public void setCancel(AtomicBoolean cancel) {
		this.cancel = cancel;
	}

	/**
	 * @return the isNcbiGenome
	 */
	public boolean isNCBIGenome() {
		return isNCBIGenome;
	}

	/**
	 * @param isNCBIGenome the isNcbiGenome to set
	 */
	public void setNCBIGenome(boolean isNCBIGenome) {
		this.isNCBIGenome = isNCBIGenome;
	}

	@Override
	public boolean isEukaryote() {

		return false;
	}
}
