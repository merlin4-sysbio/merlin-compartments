package pt.uminho.ceb.biosystems.merlin.transporters.core.transport.reactions.annotateTransporters;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import pt.uminho.ceb.biosystems.merlin.bioapis.externalAPI.ebi.uniprot.UniProtAPI;
import pt.uminho.ceb.biosystems.merlin.database.connector.databaseAPI.TransportersAPI;

/**
 * 
 */

/**
 * @author ODias
 *
 */
public class AnnotateTransporters {

	private static Map<String,String> transportDirection;

	static {

		transportDirection=new TreeMap<String, String>();
		transportDirection.put("transporter","in");
		transportDirection.put("uptake","in");
		transportDirection.put("efflux","out");
		transportDirection.put("infflux","in");
		transportDirection.put("permease","in");
		transportDirection.put("channel","in");
		transportDirection.put("import","in");
		transportDirection.put("influx","in");
		transportDirection.put("symport","in:in");
		transportDirection.put("carrier","in");
		transportDirection.put("resistance","out");
		transportDirection.put("translocase","?");
		transportDirection.put("translocating","?");
		transportDirection.put("flipping","out");
		transportDirection.put("flippase","out");
		transportDirection.put("antiport","in // out");
		transportDirection.put("exchanger","in // out");
		transportDirection.put("export","out");
		transportDirection.put("extrusion","out");
		transportDirection.put("uniport","in");
		transportDirection.put("sequestration","in");
		transportDirection.put("outward","out");
		transportDirection.put("detoxification","out");
		transportDirection.put("inward","in");
		transportDirection.put("facilitator","in");
		transportDirection.put("sensor","sensor");
	}

	/**
	 * @param tmhmmPath
	 * @param output
	 * @param unannotatedTransportersIds 
	 * @throws SQLException 
	 * @throws IOException 
	 */
	public static void annotate(String output, List<UnnannotatedTransportersContainer> unannotatedTransportersIds, Statement statement) throws SQLException, IOException {

		Map<String,String[]> entry =new TreeMap<String, String[]>();
		Map<String,String[]> recordEntries =new TreeMap<String, String[]>();
		Map<String,String[]> ytpdbEntries =new TreeMap<String, String[]>();
		
		FileWriter fstream;
		try {
			fstream = new FileWriter(output);
		} catch (Exception e) {
			
			File file = new File(output);
			file.createNewFile();
			fstream = new FileWriter(output);
		}
		
		BufferedWriter out = new BufferedWriter(fstream);

		TCDB_Parser tp = new TCDB_Parser(new TreeSet<String>());

		out.write(
				"UniProt ID" + //2
						"\tTCDB ID" + //4
						"\tTCDB family" + //6
					//	"\tTCDB family description" + 
						"\tTCDB description" + //7
						"\taffinity" + //9
						"\ttype" + //10
						"\tTCDB location" + //11
						"\tYTPDB gene" + //12
						"\tYTPDB description" + //13
						"\tYTPDB type" + //14
						"\tYTPDB metabolites" + //15
						"\tYTPDB location" + //16
						"\tTC #" +
						//"\tlocation" + //17
						"\tdirection" + //18
						"\tmetabolite" + //19
						"\treversibility" + //20
						"\treacting_metabolites" + //21
						"\tequation" + //22
				"\n");

		out.write(AnnotateTransporters.getExampleAnnotation());
		
		for(UnnannotatedTransportersContainer id : unannotatedTransportersIds) {
			
			String uniprotID = id.getUniprot_id();

			////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			//////////////////////////////////////////////tc record data////////////////////////////////////////
			////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
			//System.out.println(uniprotID+ "\t"+tcnumber);
			recordEntries=new TreeMap<String, String[]>();
			recordEntries=tp.parseTCDBrecord("http://www.tcdb.org/search/result.php?acc="+uniprotID,recordEntries);

			if(!recordEntries.containsKey("TCID")) {

				String tcnumber = null;
				if(recordEntries.keySet().size()==1)  {

					for(String key:recordEntries.keySet()) {

						tcnumber=key;
					}
				}
				
				String description="";
				if(recordEntries.get(tcnumber)[2]!=null){description=recordEntries.get(tcnumber)[0];}

				String tc_location="";
				if(recordEntries.get(tcnumber)[5]!=null){tc_location=recordEntries.get(tcnumber)[5];}

				String tc_affinity=" ";
				if(description.toLowerCase().contains("high affinity")||description.toLowerCase().contains("high-affinity"))
					tc_affinity="high";
				else if(description.toLowerCase().contains("low affinity")||description.toLowerCase().contains("low-affinity"))
					tc_affinity="low";

				String tc_type="";
				for(String direction:transportDirection.keySet()) {

					if(description.toLowerCase().contains(direction)) {

						tc_type=tc_type.concat(direction+" ");
					}
				}
				tc_type=tc_type.trim();


				////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
				//////////////////////////////////////////////family data////////////////////////////////////////
				////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
				
				String family = "";
				
				if(tcnumber!=null && !tcnumber.isEmpty())
					family=tcnumber.substring(0,(tcnumber.lastIndexOf(".")));

				//System.out.println("http://www.tcdb.org/search/result.php?tc="+family+"#"+family);
				
				if(!entry.keySet().contains(family))					
					entry=tp.parseTCDB("http://www.tcdb.org/search/result.php?tc="+family+"#"+family, entry);
				
				String tc_family = family;
				if(entry.keySet().contains(family))
					tc_family+=": "+entry.get(family)[0];
				
				String family_description= "";
				
				if(entry.get(family)!=null) {

					family_description= entry.get(family)[0];
					if(family_description.toLowerCase().contains("high affinity")||family_description.toLowerCase().contains("high-affinity")) {

						if(tc_affinity!=" " && tc_affinity.equals("low"))
							tc_affinity="high/low???";
						else 
							tc_affinity="high";
					}
					else if(family_description.toLowerCase().contains("low affinity")||family_description.toLowerCase().contains("low-affinity")) {

						if(tc_affinity!=" " && tc_affinity.equals("high"))
							tc_affinity="high/low???";
						else
							tc_affinity="low";
					}
				}

				////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
				//////////////////////////////////////////////ytpdb data////////////////////////////////////////
				////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
				String ytpdb_gene="", ytpdb_description="", ytpdb_metabolite="", ytpdb_location="", ytpdb_type="";

				String yeastLocusTag;
				if(!((yeastLocusTag=retrieveLocusTagIfScerevisiae(uniprotID)).isEmpty())) {

					if(!ytpdbEntries.containsKey(yeastLocusTag)) {

						ytpdbEntries =tp.parseYTPDBrecord("http://homes.esat.kuleuven.be/~sbrohee/ytpdb/index.php/Ytpdbgene:"+yeastLocusTag,ytpdbEntries);
					}
					if(ytpdbEntries.containsKey(yeastLocusTag)) {

						ytpdb_gene=ytpdbEntries.get(yeastLocusTag)[0];
						ytpdb_description=ytpdbEntries.get(yeastLocusTag)[1];
						ytpdb_metabolite=ytpdbEntries.get(yeastLocusTag)[2];
						ytpdb_location=ytpdbEntries.get(yeastLocusTag)[3];

						for(String direction:transportDirection.keySet()) {

							if(ytpdb_description.toLowerCase().contains(direction)) {

								ytpdb_type=ytpdb_type.concat(direction+" ");
							}
						}
						ytpdb_type=ytpdb_type.trim();
					}
				}
				////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
				//////////////////////////////////////////////direction ////////////////////////////////////////
				////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
				String direction="";
				if((ytpdb_type!="" && ytpdb_type.equals(tc_type)) || (ytpdb_type!="" && tc_type=="")) {

					if(transportDirection.containsKey(ytpdb_type)) {

						direction = transportDirection.get(ytpdb_type);
					}
					else {

						direction = "Several directions";
					}
				}
				else if(ytpdb_type=="" && tc_type!="") {

					if(transportDirection.containsKey(tc_type)) {

						direction = transportDirection.get(tc_type);
					}
					else {

						direction = "Several directions";
					}
				}
				else if(ytpdb_type!="" && tc_type!="") {

					direction = "Distinct directions";
				}

//				if(description1.equals(description2)){
//
//					description2="";
//				}

				if(tcnumber==null || tcnumber.isEmpty())
					tcnumber = id.getTcnumber();
				
				TransporterAnnotation transporterAnnotation = new TransporterAnnotation();
				transporterAnnotation.setUniProt_ID(uniprotID);
				transporterAnnotation.setTcdb_ID(tcnumber);
				transporterAnnotation.setTcdb_family(tc_family);
				transporterAnnotation.setTcdb_family_description(family_description);
				transporterAnnotation.setTcdb_description(description);
				transporterAnnotation.setAffinity(tc_affinity);
				transporterAnnotation.setType(tc_type);
				transporterAnnotation.setTcdb_location(tc_location);
				transporterAnnotation.setYtpdb_gene(ytpdb_gene);
				transporterAnnotation.setYtpdb_description(ytpdb_description);
				transporterAnnotation.setYtpdb_type(ytpdb_type);
				transporterAnnotation.setYtpdb_metabolites(ytpdb_metabolite);
				transporterAnnotation.setYtpdb_location(ytpdb_location);

				String tcnumberfamily = new String(tcnumber);
				if(tcnumber!=null && !tcnumber.isEmpty())
					tcnumberfamily = tcnumberfamily.substring(0, tcnumber.lastIndexOf("."))+".#";

				transporterAnnotation.setTc_number_family(tcnumberfamily);
				transporterAnnotation.setDirection(direction);
				transporterAnnotation.setMetabolite(ytpdb_metabolite);
				transporterAnnotation.setReversibility("");
				transporterAnnotation.setReacting_metabolites("");
				transporterAnnotation.setEquation("");

				transporterAnnotation = AnnotateTransporters.getExistingAnnotation(uniprotID, transporterAnnotation, statement);

				out.write(transporterAnnotation.toString());
			}
		}

		//Close the output stream
		out.close();
		fstream.close();
	}


	//	/**
	//	 * @param accessionNumber
	//	 * @return
	//	 */
	//	private String retrieveLocusTag(String accessionNumber) {
	//
	//		//Create entry retrival service
	//		EntryRetrievalService entryRetrievalService = UniProtJAPI.factory.getEntryRetrievalService();
	//
	//		//Retrieve UniProt entry by its accession number
	//		UniProtEntry entry = UniProtAPI.getUniprotEntry(entryRetrievalService, accessionNumber, 0);
	//
	//		//If entry with a given accession number is not found, entry will be equal null
	//		if (entry != null) 
	//		{
	//			if(entry.getGenes().size()>0 && entry.getGenes().get(0).getOrderedLocusNames().size()>0)
	//			{
	//				String locusTag = entry.getGenes().get(0).getOrderedLocusNames().get(0).getValue();
	//				if(locusTag!=null)
	//				{
	//					return locusTag;
	//				}
	//			}
	//		}
	//		return accessionNumber;
	//	}


	/**
	 * @param accessionNumber
	 * @return
	 */
	private static String retrieveLocusTagIfScerevisiae(String accessionNumber) {
	
		return UniProtAPI.retrieveLocusTagIfOrganism(accessionNumber, "Saccharomyces cerevisiae");
	}

	/**
	 * @return
	 */
	private static String getExampleAnnotation() {

		String out = null;

		TransporterAnnotation transporterAnnotation = new TransporterAnnotation();

		transporterAnnotation.setUniProt_ID("P39003");
		transporterAnnotation.setTcdb_ID("2.A.1.1.31");
		transporterAnnotation.setTcdb_family("2.A.1.1");
		transporterAnnotation.setTcdb_family_description("The Sugar Porter (SP) Family");
		transporterAnnotation.setTcdb_description("High affinity, glucose-repressible, glucose (hexose) uniporter (Hxt6).");
		transporterAnnotation.setAffinity("high");
		transporterAnnotation.setType("uniport");
		transporterAnnotation.setTcdb_location("Membrane");
		transporterAnnotation.setYtpdb_gene("HXT6");
		transporterAnnotation.setYtpdb_description("High-affinity hexose facilitator");
		transporterAnnotation.setYtpdb_type("facilitator");
		transporterAnnotation.setYtpdb_metabolites("fructose,glucose,mannose");
		transporterAnnotation.setYtpdb_location("plasma membrane");
		transporterAnnotation.setTc_number_family("2.A.1.1.#");
		transporterAnnotation.setDirection("in");
		transporterAnnotation.setMetabolite("Fruit sugar; D-glucopyranose; D-mannopyranose");
		transporterAnnotation.setReversibility("TRUE");
		transporterAnnotation.setReacting_metabolites("--");
		transporterAnnotation.setEquation("Uniport: S (out) <=> S (in) || Symport: S (out) + [H+ or Na+] (out) <=> S (in) + [H+ or Na+] (in) || Antiport: S1 (out) + S2 (in) <=> S1 (in) + S2 (out) (S1 may be H+ or a solute)");

		out = transporterAnnotation.toString();

		transporterAnnotation.setUniProt_ID("P39109");
		transporterAnnotation.setTcdb_ID("3.A.1.208.11");
		transporterAnnotation.setTcdb_family("3.A.1.208");
		transporterAnnotation.setTcdb_family_description("The Drug Conjugate Transporter (DCT) Family (ABCC) (Dębska et al., 2011)");
		transporterAnnotation.setTcdb_description("Vacuolar metal resistance and drug detoxification protein, yeast cadmium factor (YCF1); transports cadmium-glutathione conjugates, glutathione S-conjugated leucotriene C4, organic glutathione S-conjugates, selenodigluthatione, unconjugated bilirubin, reduced glutathione, and diazaborine (Lazard et al., 2011). ");
		transporterAnnotation.setAffinity("");
		transporterAnnotation.setType("detoxification resistance");
		transporterAnnotation.setTcdb_location("Vacuole membrane");
		transporterAnnotation.setYtpdb_gene("YCF1");
		transporterAnnotation.setYtpdb_description("Vacuolar full-size ABC transporter responsible for vacuolar sequestration of glutathione-S-conjugates ");
		transporterAnnotation.setYtpdb_type("transporter");
		transporterAnnotation.setYtpdb_metabolites("arsenite, bilirubin, diazaborine, glutathione-S-conjugate");
		transporterAnnotation.setYtpdb_location("vacuolar membrane");
		transporterAnnotation.setTc_number_family("3.A.1.208.#");
		transporterAnnotation.setDirection("out");
		transporterAnnotation.setMetabolite("arsenite; bilirubin; diazaborine; glutathione conjugate");
		transporterAnnotation.setReversibility("FALSE");
		transporterAnnotation.setReacting_metabolites("1:ATP; 1:water || 1:ADP; 1:orthophosphate");
		transporterAnnotation.setEquation("Substrate (in) + ATP -> Substrate (out) + ADP + Pi");

		out = out.concat(transporterAnnotation.toString());

		return out;

	}

	/**
	 * @param UniprotId
	 * @param transporterAnnotation
	 * @return
	 * @throws SQLException
	 */
	private static TransporterAnnotation getExistingAnnotation(String uniprotID, TransporterAnnotation transporterAnnotation, Statement statement) throws SQLException {

		String direction = "", reversibility = "", reactingMetabolites = "", equation = "", metabolite = "";
		
		ArrayList<String[]> result = TransportersAPI.getExistingAnnotation(uniprotID, statement);
		
		Set<String> metabolite_added = new TreeSet<String>();
		boolean exists = false;
		
		for(int i=0; i<result.size(); i++){
			String[] list = result.get(i);
			
			direction = list[0];
			equation = list[1];
			reversibility = list[2].toUpperCase();
			metabolite = list[4];

			exists = true;
			
			if(list[3].equalsIgnoreCase("reactant") || list[3].equalsIgnoreCase("product")) {

				String separator = " || ";

				if(i == result.size()-1) {

					separator = "";
				}

				if(!metabolite_added.contains(metabolite)) {

					reactingMetabolites = reactingMetabolites.concat(metabolite+separator);
					metabolite_added.add(metabolite);
				}
			}
			else {

				String separator = "; ";

				if(i == result.size()-1) {

					separator = "";
				}


				if(!metabolite_added.contains(metabolite)) {

					metabolite = metabolite.concat(metabolite+separator);
					metabolite_added.add(metabolite);
				}
			}

		}

		if(exists) {

			transporterAnnotation.setDirection(direction);
			transporterAnnotation.setMetabolite(metabolite);
			transporterAnnotation.setReversibility(reversibility);
			transporterAnnotation.setReacting_metabolites(reactingMetabolites);
			transporterAnnotation.setEquation(equation);
		}

		return transporterAnnotation;
	}

}
