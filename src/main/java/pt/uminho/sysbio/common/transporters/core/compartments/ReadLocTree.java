package pt.uminho.sysbio.common.transporters.core.compartments;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import pt.uminho.sysbio.common.database.connector.datatypes.Connection;
import pt.uminho.sysbio.common.transporters.core.utils.Enumerators.OrganismType;

/**
 * @author Oscar Dias
 *
 */
public class ReadLocTree implements CompartmentsInterface {

	private static int normalization=100;
	private AtomicBoolean cancel;
	private LoadCompartments loadCompartments;
	private Map<String, CompartmentResult> results;
	private int project_id;
	private OrganismType organismType;



	/**
	 * @param organismType
	 */
	public ReadLocTree(OrganismType organismType) {

		this.cancel = new AtomicBoolean(false);
		this.organismType = organismType;
	}

	/**
	 * @param conn
	 * @param results
	 * @param project_id
	 * @param organismType
	 */
	public ReadLocTree(Connection conn, Map<String, CompartmentResult> results, int project_id, OrganismType organismType) {

		this.cancel = new AtomicBoolean(false);
		this.loadCompartments = new LoadCompartments(conn);
		this.results = results;
		this.project_id = project_id;
		this.organismType = organismType;
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



	/**
	 * @param outFile
	 * @return
	 * @throws IOException
	 */
	public Map<String, CompartmentResult> readLocTreeFile(File outFile) throws IOException {

		Map<String, CompartmentResult> compartmentLists = new HashMap<>();

		BufferedReader in = new BufferedReader(new FileReader(outFile));
		String str;

		int proteinID = 0, score = 1, localization = 2, geneOntologyTerms = 3, expectedAccuracy = -1, annotationType=-1;


		while ((str = in.readLine()) != null && !this.cancel.get()) {

			if(str.startsWith("#")) {

				if(str.toLowerCase().contains("Expected Accuracy".toLowerCase()) && str.toLowerCase().contains("Annotation Type".toLowerCase())) {

					expectedAccuracy = 2;
					localization = 3;
					geneOntologyTerms = 4;
					annotationType = 5; 
				}
			} 
			else {

				String[] locT = str.split("\\t");

				LocTreeResult locTR = null;

				String localizationString = locT[localization];

				if(!this.organismType.equals(OrganismType.plant)) {

					if (localizationString.equals("chloroplast") || 
							localizationString.equals("plastid"))
						localizationString = "mitochondrion";

					if (localizationString.equals("chloroplast membrane") || 
							localizationString.equals("plastid membrane"))
						localizationString = "mitochondrion membrane";
				}

				if(compartmentLists.containsKey(locT[0])) {

					locTR = (LocTreeResult) compartmentLists.get(locT[proteinID]);
					locTR.addCompartment(localizationString, new Double(locT[score]));
				}
				else {

					locTR = new LocTreeResult(locT[proteinID], new Double(locT[score]), localizationString, locT[geneOntologyTerms]);
					
					
					if(expectedAccuracy>0 && annotationType>0) {

						locTR.setAnnotationType(locT[annotationType]);
						locTR.setExpectedAccuracy(locT[expectedAccuracy]);
					}
				}

				compartmentLists.put(locT[proteinID], locTR);
			}
		}
		in.close();
		return compartmentLists;
	}

	/* (non-Javadoc)
	 * @see pt.uminho.sysbio.common.transporters.core.compartments.CompartmentsInterface#addGeneInformation(java.io.File)
	 */
	public Map<String, CompartmentResult> addGeneInformation(File outFile) throws Exception {

		Map<String, CompartmentResult> compartmentLists = this.readLocTreeFile(outFile);

		Map<String, CompartmentResult> compartmentResults = compartmentLists;

		return compartmentResults;
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

	@Override
	public Map<String, GeneCompartments> getBestCompartmentsByGene(double threshold) throws SQLException {

		return this.loadCompartments.getBestCompartmenForGene(threshold, ReadLocTree.normalization, this.project_id);
	}


	public void loadCompartmentsInformation() throws Exception {

		for(CompartmentResult locTreeResult : this.results.values())
			this.loadCompartments.loadData(locTreeResult.getGeneID(), locTreeResult.getCompartments(), project_id);

	}

	/* (non-Javadoc)
	 * @see pt.uminho.sysbio.common.transporters.core.compartments.CompartmentsInterface#getCompartments(java.lang.String)
	 */
	public boolean getCompartments(String string) {

		return true;
	}
}
