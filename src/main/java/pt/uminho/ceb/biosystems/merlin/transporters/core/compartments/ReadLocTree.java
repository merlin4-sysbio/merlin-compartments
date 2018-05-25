package pt.uminho.ceb.biosystems.merlin.transporters.core.compartments;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * @author Oscar Dias
 *
 */
public class ReadLocTree implements CompartmentsInterface {

	private static int normalization=100;
	private AtomicBoolean cancel;
	private boolean typePlant;

	/**
	 * @param organismType
	 */
	public ReadLocTree() {

		this.cancel = new AtomicBoolean(false);
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


	public boolean isEukaryote() {

		return false;
	}

	public void setPlant(boolean typePlant){
		this.typePlant = typePlant;
	}

	/**
	 * Method to parse a BufferedReader containing information about compartments.
	 * 
	 * @param in
	 * @return Map<String, CompartmentResult>
	 * @throws IOException
	 */
	public Map<String, CompartmentResult> readLocTreeFile(BufferedReader in) throws IOException {

		Map<String, CompartmentResult> compartmentLists = new HashMap<>();

		String inputLine;

		int protID = -1, scr = -1, loc = -1, geneOnto = -1, acc = -1, annType=-1;
		
		String proteinID = null, localization = null, geneOntologyTerms = null, accuracy = null, annotationType = null;
		
		String score = null;
		
		int count = 0;

		boolean header = true, flag = false;
		
		LocTreeResult locTR = null;
		
		boolean oldParser = false;
		
		if((inputLine = in.readLine()) !=null){
			Document doc = Jsoup.parse(inputLine);
			String str = doc.body().text();
			
			if(str.contains("#")){
				oldParser = true;
				protID = 0;
				scr = 1;
				loc = 2;
				geneOnto = 3;
			}
		}
		
		while ((inputLine = in.readLine()) != null && !this.cancel.get() && !inputLine.contains("Mouse click")) {
			
			String str = inputLine;
			
			if(!oldParser){
				Document doc = Jsoup.parse(inputLine);
				str = doc.body().text();
			}
			
//			System.out.println(str);
			
			if(oldParser && !str.contains("#")){
				
				String[] locT = str.split("\\t");
				
				String localizationString = locT[loc];

				if(!typePlant) {

					if (localizationString.equals("chloroplast") || 
							localizationString.equals("plastid"))
						localizationString = "mitochondrion";

					if (localizationString.equals("chloroplast membrane") || 
							localizationString.equals("plastid membrane"))
						localizationString = "mitochondrion membrane";
				}

				if(compartmentLists.containsKey(locT[protID])) {

					locTR = (LocTreeResult) compartmentLists.get(locT[protID]);
					locTR.addCompartment(localizationString, new Double(locT[scr]));
				}
				else {

					locTR = new LocTreeResult(locT[protID], new Double(locT[scr]), localizationString, locT[geneOnto]);


					if(acc>0 && annType>0) {

						locTR.setAnnotationType(locT[annType]);
						locTR.setExpectedAccuracy(locT[acc]);
					}
				}

				compartmentLists.put(locT[protID], locTR);
				
			}
			
			if (!flag && !oldParser){							//reads the order of the information
				
				if (str.contains("Details")){
					
					if (!header)
						flag = true;
					
					count = 0;
					header = false;
				}
					
				else if (str.contains("Protein ID"))
					protID = count;
				
				else if(str.contains("Score"))
					scr = count;
				
				else if(str.contains("Expected Accuracy"))
					acc = count;

				else if(str.contains("Localization Class"))
					loc = count;
						
				else if(str.contains("Gene Ontology Terms"))
					geneOnto = count;
						
				else if(str.contains("Annotation Type"))
					annType = count;
			
				count++;
				
//				System.out.println(protID);
//				System.out.println(scr);
//				System.out.println(acc);
//				System.out.println(loc);
//				System.out.println(geneOnto);
//				System.out.println(annType);
			}
			
			if (flag){
				
//				System.out.println();
//				System.out.println("NOVA");				
//				System.out.println(str);
//				System.out.println(!str.contains("Details"));
//				System.out.println(!str.equals(""));
//				System.out.println(count);
				
				if(!str.contains("Details") && str != null && !str.equals("")){
					
					if(count == protID)
						proteinID = str;
					
					if(count == scr) 
						score = str;
//						score = Double.parseDouble(str);
					
					else if(count == acc) 
						accuracy = str;
					else if(count == loc){
						localization = str;
						
						if(!typePlant) {
		
							if (localization.equals("chloroplast") || 
									localization.equals("plastid"))
								localization = "mitochondrion";
		
							if (localization.equals("chloroplast membrane") || 
									localization.equals("plastid membrane"))
								localization = "mitochondrion membrane";
						}
					}
						
					else if(count == geneOnto) 
						geneOntologyTerms = str;
					else if(count == annType){
						if(str.contains(";")){
							geneOntologyTerms.concat(str);
							count --;
						}
						else{
							annotationType = str;
						}
					}
					
					count ++;
				
					if (count == 7){
						
						if(!proteinID.isEmpty() && !score.isEmpty() && !localization.isEmpty() 
								&& !annotationType.isEmpty() && !geneOntologyTerms.isEmpty() && !accuracy.isEmpty()){


							try {

								if(compartmentLists.containsKey(proteinID)) {
									locTR = (LocTreeResult) compartmentLists.get(proteinID);
									locTR.addCompartment(localization, Double.parseDouble(score));
								}
								else {

									locTR = new LocTreeResult(proteinID, Double.parseDouble(score), localization, geneOntologyTerms);
									locTR.setAnnotationType(annotationType);
									locTR.setExpectedAccuracy(accuracy);
								}

								compartmentLists.put(proteinID, locTR);
							} catch (Exception e) {

							}
						}
						
						
						locTR = null;
						count = 1;
						proteinID = null;
						localization = null;
						geneOntologyTerms = null;
						accuracy = null;
						annotationType = null;
						
					}
				}
			}
		}

// ###############################################	old parser
	
	
//			if(str.startsWith("#")) {
//
//				if(str.toLowerCase().contains("Expected Accuracy".toLowerCase()) && str.toLowerCase().contains("Annotation Type".toLowerCase())) {
//
//					expectedAccuracy = 2;
//					localization = 3;
//					geneOntologyTerms = 4;
//					annotationType = 5; 
//				}
//			} 
//			else {
//
//				String[] locT = str.split("\\t");
//
//				LocTreeResult locTR = null;
//
//				String localizationString = locT[localization];
//
//				if(!typePlant) {
//
//					if (localizationString.equals("chloroplast") || 
//							localizationString.equals("plastid"))
//						localizationString = "mitochondrion";
//
//					if (localizationString.equals("chloroplast membrane") || 
//							localizationString.equals("plastid membrane"))
//						localizationString = "mitochondrion membrane";
//				}
//
//				if(compartmentLists.containsKey(locT[proteinID])) {
//
//					locTR = (LocTreeResult) compartmentLists.get(locT[proteinID]);
//					locTR.addCompartment(localizationString, new Double(locT[score]));
//				}
//				else {
//
//					locTR = new LocTreeResult(locT[proteinID], new Double(locT[score]), localizationString, locT[geneOntologyTerms]);
//					
//					
//					if(expectedAccuracy>0 && annotationType>0) {
//
//						locTR.setAnnotationType(locT[annotationType]);
//						locTR.setExpectedAccuracy(locT[expectedAccuracy]);
//					}
//				}
//
//				compartmentLists.put(locT[proteinID], locTR);
//			}
//		}
		
		in.close();
		
		return compartmentLists;
	}

	/* (non-Javadoc)
	 * @see pt.uminho.sysbio.common.transporters.core.compartments.CompartmentsInterface#addGeneInformation(java.io.File)
	 */
	public Map<String, CompartmentResult> addGeneInformation(File outFile) throws Exception {
		
		return null;
	}

	/**
	 * 
	 * 
	 * @param compartmentLists
	 * @return
	 * @throws Exception

				private Map<String, CompartmentResult> getLocusTags(Map<String, CompartmentResult> compartmentLists) throws Exception {

					Map<String, CompartmentResult> compartmentResults = new HashMap<>();

					if (!this.cancel.get()) {

						Map<String, String> idLocus = NcbiAPI.getNCBILocusTags(compartmentLists.keySet(), 500);

						for (String id : idLocus.keySet()) {

							CompartmentResult locTreeResult = compartmentLists.get(id);
							locTreeResult.setGeneID(idLocus.get(id));
							compartmentResults.put(idLocus.get(id), locTreeResult);
						}
					}
					return compartmentResults;
				}
	 */

	/**
	 * Get best compartments by gene.
	 * 
	 * @param threshold
	 * @param statement
	 * @param projectID
	 * @return
	 * @throws SQLException
	 */
	@Override
	public Map<String, GeneCompartments> getBestCompartmentsByGene(double threshold, int projectID,  Statement statement) throws SQLException {

		return LoadCompartments.getBestCompartmenForGene(threshold, ReadLocTree.normalization, projectID, statement);
	}

	/**
	 *  Load compartments from results.
	 * 
	 * @param results
	 * @param projectID
	 * @param statement
	 * @throws Exception
	 */
	public void loadCompartmentsInformation(Map<String, CompartmentResult> results, int projectID, Statement statement) throws Exception {

		for(CompartmentResult locTreeResult : results.values())
			LoadCompartments.loadData(locTreeResult.getGeneID(), locTreeResult.getCompartments(), projectID, statement);

	}

	/* (non-Javadoc)
	 * @see pt.uminho.sysbio.common.transporters.core.compartments.CompartmentsInterface#getCompartments(java.lang.String)
	 */
	public boolean getCompartments(String string) {

		return true;
	}

	@Override
	public Map<String, CompartmentResult> addGeneInformation(String link) throws Exception {
		
		BufferedReader data = RemoteCompartmentsResults.retrieveDataFromURL(link);

		Map<String, CompartmentResult> compartmentLists = this.readLocTreeFile(data); 

		return compartmentLists;
	}

}
