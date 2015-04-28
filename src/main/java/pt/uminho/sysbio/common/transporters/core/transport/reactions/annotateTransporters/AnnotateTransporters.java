package pt.uminho.sysbio.common.transporters.core.transport.reactions.annotateTransporters;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import pt.uminho.sysbio.common.bioapis.externalAPI.uniprot.UniProtAPI;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.kraken.uuw.services.remoting.EntryRetrievalService;
import uk.ac.ebi.kraken.uuw.services.remoting.UniProtJAPI;

/**
 * 
 */

/**
 * @author ODias
 *
 */
public class AnnotateTransporters {

	private Map<String,String> transportDirection;
	private List<UnnannotatedTransportersContainer> ids=null;
	private Connection conn;


	/**
	 * @param database
	 */
	public AnnotateTransporters(Connection conn) {

		this.conn = conn;
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
	 * @throws SQLException 
	 * @throws IOException 
	 */
	public void annotate(String output) throws SQLException, IOException {


		Map<String,String[]> entry =new TreeMap<String, String[]>();
		Map<String,String[]> recordEntries =new TreeMap<String, String[]>();
		Map<String,String[]> ytpdbEntries =new TreeMap<String, String[]>();

		FileWriter fstream = new FileWriter(output);
		BufferedWriter out = new BufferedWriter(fstream);

		TCDB_Parser tp = new TCDB_Parser(new TreeSet<String>());

		out.write(
				"UniProt ID" + //2
						"\tTCDB ID" + //4
						"\tTCDB family" + //6
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

		out.write(this.getExampleAnnotation());


		for(UnnannotatedTransportersContainer id : this.ids) {

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
				else {

					System.out.println(recordEntries.keySet());
				}

				String description2="";
				if(recordEntries.get(tcnumber)[5]!=null){description2=recordEntries.get(tcnumber)[0];}

				String tc_location="";
				if(recordEntries.get(tcnumber)[5]!=null){tc_location=recordEntries.get(tcnumber)[5];}

				String tc_affinity=" ";
				if(description2.toLowerCase().contains("high affinity")||description2.toLowerCase().contains("high-affinity")){tc_affinity="high";}
				else if(description2.toLowerCase().contains("low affinity")||description2.toLowerCase().contains("low-affinity")){tc_affinity="low";}

				String tc_type="";
				for(String direction:transportDirection.keySet()) {

					if(description2.toLowerCase().contains(direction)) {

						tc_type=tc_type.concat(direction+" ");
					}
				}
				tc_type=tc_type.trim();


				////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
				//////////////////////////////////////////////family data////////////////////////////////////////
				////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

				String family = tcnumber.substring(0,(tcnumber.lastIndexOf(".")));

				if(!entry.keySet().contains(family)) {
					
					entry=tp.parseTCDB("http://www.tcdb.org/search/result.php?tc="+family+"#"+family, entry);
				}

				String tc_family = family;
				if(entry.keySet().contains(family)){tc_family+=": "+entry.get(family)[0];}
				String description1= "";
				if(entry.get(tcnumber)!=null) {

					description1= entry.get(tcnumber)[0];
					if(description1.toLowerCase().contains("high affinity")||description1.toLowerCase().contains("high-affinity")) {

						if(tc_affinity!=" " && tc_affinity.equals("low")) {

							tc_affinity="high/low???";
						}
						else {

							tc_affinity="high";
						}
					}
					else if(description1.toLowerCase().contains("low affinity")||description1.toLowerCase().contains("low-affinity")) {

						if(tc_affinity!=" " && tc_affinity.equals("high")) {

							tc_affinity="high/low???";
						}
						else {

							tc_affinity="low";
						}
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

				if(description1.equals(description2)){

					description2="";
				}

				TransporterAnnotation transporterAnnotation = new TransporterAnnotation();

				transporterAnnotation.setUniProt_ID(uniprotID);
				transporterAnnotation.setTcdb_ID(tcnumber);
				transporterAnnotation.setTcdb_family(tc_family);
				transporterAnnotation.setTcdb_description(description1);
				transporterAnnotation.setAffinity(tc_affinity);
				transporterAnnotation.setType(tc_type);
				transporterAnnotation.setTcdb_location(tc_location);
				transporterAnnotation.setYtpdb_gene(ytpdb_gene);
				transporterAnnotation.setYtpdb_description(ytpdb_description);
				transporterAnnotation.setYtpdb_type(ytpdb_type);
				transporterAnnotation.setYtpdb_metabolites(ytpdb_metabolite);
				transporterAnnotation.setYtpdb_location(ytpdb_location);

				String tcnumberfamily = new String(tcnumber);
				tcnumberfamily = tcnumberfamily.substring(0, tcnumber.lastIndexOf("."))+".#";

				transporterAnnotation.setTc_number_family(tcnumberfamily);
				transporterAnnotation.setDirection(direction);
				transporterAnnotation.setMetabolite(ytpdb_metabolite);
				transporterAnnotation.setReversibility("");
				transporterAnnotation.setReacting_metabolites("");
				transporterAnnotation.setEquation("");

				transporterAnnotation = this.getExistingAnnotation(uniprotID, transporterAnnotation);

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
	 * @return the ids
	 */
	public List<UnnannotatedTransportersContainer> getIds() {

		return ids;
	}

	/**
	 * @param ids the ids to set
	 */
	public void setIds(List<UnnannotatedTransportersContainer> ids) {
		this.ids = ids;
	}


	/**
	 * @param accessionNumber
	 * @return
	 */
	private String retrieveLocusTagIfScerevisiae(String accessionNumber) {

		//Create entry retrival service
		EntryRetrievalService entryRetrievalService = UniProtJAPI.factory.getEntryRetrievalService();

		//Retrieve UniProt entry by its accession number
		UniProtEntry entry = UniProtAPI.getUniprotEntry(entryRetrievalService, accessionNumber, 0);

		//If entry with a given accession number is not found, entry will be equal null
		if (entry != null) {

			if(entry.getOrganism().getScientificName().getValue().contains("Saccharomyces cerevisiae")) {

				//System.out.println("entry = " + entry.getUniProtId().getValue());
				if(entry.getGenes().size()>0 && entry.getGenes().get(0).getOrderedLocusNames().size()>0) {

					String locusTag = entry.getGenes().get(0).getOrderedLocusNames().get(0).getValue();
					if(locusTag!=null) {

						return locusTag;
					}
				}
			}
		}
		return "";

		//Retrieve UniRef entry by its ID
		//	    UniRefEntry uniRefEntry = entryRetrievalService.getUniRefEntry("UniRef90_Q12979-2");
		//
		//	    if (uniRefEntry != null) {
		//	      System.out.println("Representative Member Organism = " +
		//	       uniRefEntry.getRepresentativeMember().getSourceOrganism().getValue());
		//	    }
	}

	/**
	 * @return
	 */
	private String getExampleAnnotation() {

		String out = null;

		TransporterAnnotation transporterAnnotation = new TransporterAnnotation();

		transporterAnnotation.setUniProt_ID("P39003");
		transporterAnnotation.setTcdb_ID("2.A.1.1.31");
		transporterAnnotation.setTcdb_family("2.A.1.1: :The Sugar Porter (SP) Family");
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
		transporterAnnotation.setTcdb_family("3.A.1.208:  The Drug Conjugate Transporter (DCT) Family (ABCC) (Dębska et al., 2011)");
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
	private TransporterAnnotation getExistingAnnotation(String uniprot_id, TransporterAnnotation transporterAnnotation) throws SQLException {

		Statement stmt = this.conn.createStatement();

		ResultSet rs = stmt.executeQuery("SELECT directions, equation, transport_systems.reversible, direction, metabolites.name  FROM tcdb_registries " +
				" INNER JOIN tc_numbers ON (tc_numbers.tc_number = tcdb_registries.tc_number AND tc_numbers.tc_version = tcdb_registries.tc_version )" +
				" INNER JOIN general_equation ON (tc_numbers.general_equation_id = general_equation.id ) " +
				" INNER JOIN tc_numbers_has_transport_systems ON (tc_numbers_has_transport_systems.tc_number = tc_numbers.tc_number AND tc_numbers_has_transport_systems.tc_version = tc_numbers.tc_version)" +
				" INNER JOIN transport_systems ON (tc_numbers_has_transport_systems.transport_system_id = transport_systems.id)" +
				" INNER JOIN transport_types ON (transport_types.id = transport_type_id)" +
				" INNER JOIN transported_metabolites_directions ON (transported_metabolites_directions.transport_system_id = transport_systems.id) " +
				" INNER JOIN directions ON (transported_metabolites_directions.direction_id = directions.id) " +
				" INNER JOIN metabolites ON (transported_metabolites_directions.metabolite_id = metabolites.id) " +
				" WHERE uniprot_id = '"+uniprot_id+"' AND datatype = 'MANUAL'");

		String direction = "", metabolite = "", reversibility = "", reactingMetabolites = "", equation = "";

		Set<String> metabolite_added = new TreeSet<String>();
		boolean exists = false;

		while (rs.next()) {

			exists = true;
			direction = rs.getString(1);
			equation = rs.getString(2);
			reversibility = rs.getBoolean(3)+"".toUpperCase();

			if(rs.getString(4).equalsIgnoreCase("reactant") || rs.getString(4).equalsIgnoreCase("product")) {

				String separator = " || ";

				if(rs.last()) {

					separator = "";
				}

				if(!metabolite_added.contains(rs.getString(5))) {

					reactingMetabolites = reactingMetabolites.concat(rs.getString(5)+separator);
					metabolite_added.add(rs.getString(5));
				}
			}
			else {

				String separator = "; ";

				if(rs.last()) {

					separator = "";
				}


				if(!metabolite_added.contains(rs.getString(5))) {

					metabolite = metabolite.concat(rs.getString(5)+separator);
					metabolite_added.add(rs.getString(5));
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
