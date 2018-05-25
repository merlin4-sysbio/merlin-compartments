/**
 * 
 */
package pt.uminho.ceb.biosystems.merlin.transporters.core.compartments;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author ODias
 *
 */
public class ReadPSort3 implements CompartmentsInterface{

	private static int normalization=10;
	private AtomicBoolean cancel;


//	/**
//	 * @param conn
//	 * @param results
//	 * @param project_id
//	 */
//	public ReadPSort3(Connection conn, Map<String, CompartmentResult> results, int project_id) {
//		this.cancel = new AtomicBoolean(false);
//		this.loadCompartments = new LoadCompartments(conn);
//		this.results = results;
//		this.project_id = project_id;
//	}

	/**
	 * 
	 */
	public ReadPSort3() {

		this.cancel = new AtomicBoolean(false);
	}

	/**
	 * @param outFile
	 * @return
	 * @throws Exception 
	 */
	public Map<String, CompartmentResult> addGeneInformation(File outFile) throws Exception {

		Map<String, CompartmentResult> compartmentLists = this.readPSortFile(outFile);

		Map<String, CompartmentResult> compartmentResults = compartmentLists;

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

					
					//String locus_tag = line[seqID_index].split(" ")[0].split("\\|")[3];
					String locus_tag = line[seqID_index].split(" ")[0];
					
					PSort3Result pSort3Result = new PSort3Result(locus_tag);
					
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

							pSort3Result = new PSort3Result(locus_tag);
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

	@Override
	public boolean isEukaryote() {

		return false;
	}

	@Override
	public Map<String, CompartmentResult> addGeneInformation(String link) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void loadCompartmentsInformation(Map<String, CompartmentResult> results, int projectID, Statement statement)
			throws Exception {
		
		for(CompartmentResult pSORT3Result : results.values())
			LoadCompartments.loadData(pSORT3Result.getGeneID(), pSORT3Result.getCompartments(), projectID, statement);
	}

	@Override
	public Map<String, GeneCompartments> getBestCompartmentsByGene(double threshold, int projectID, Statement statement)
			throws SQLException {
		
		return LoadCompartments.getBestCompartmenForGene(threshold, ReadPSort3.normalization, projectID, statement);
	}

	@Override
	public void setPlant(boolean typePlant) {
		// TODO Auto-generated method stub
		
	}
}
