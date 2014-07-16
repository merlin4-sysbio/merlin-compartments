package pt.uminho.sysbio.common.transporters.core.transport.reactions;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

import pt.uminho.sysbio.common.bioapis.externalAPI.uniprot.TaxonomyContainer;
import pt.uminho.sysbio.common.bioapis.externalAPI.uniprot.UniProtAPI;
import pt.uminho.sysbio.common.database.connector.datatypes.MySQLMultiThread;
import pt.uminho.sysbio.common.transporters.core.transport.MIRIAM_Data;
import pt.uminho.sysbio.common.transporters.core.transport.reactions.annotateTransporters.AnnotateTransporters;
import pt.uminho.sysbio.common.transporters.core.transport.reactions.annotateTransporters.UnnannotatedTransportersContainer;
import pt.uminho.sysbio.common.transporters.core.transport.reactions.containerAssembly.TransportMetaboliteDirectionStoichiometryContainer;
import pt.uminho.sysbio.common.transporters.core.transport.reactions.loadTransporters.LoadTransportersData;
import pt.uminho.sysbio.common.transporters.core.transport.reactions.parseTransporters.AlignedGenesContainer;
import pt.uminho.sysbio.common.transporters.core.transport.reactions.parseTransporters.AlignmentResult;
import pt.uminho.sysbio.common.transporters.core.transport.reactions.parseTransporters.ParserContainer;
import pt.uminho.sysbio.common.transporters.core.transport.reactions.parseTransporters.TransportParsing;
import pt.uminho.sysbio.common.transporters.core.transport.reactions.parseTransporters.TransportSystemContainer;
import pt.uminho.sysbio.common.transporters.core.transport.reactions.parseTransporters.MetabolitesEntry.TransportType;
import pt.uminho.sysbio.common.utilities.io.FileUtils;
import uk.ac.ebi.kraken.interfaces.uniprot.NcbiTaxon;


/**
 * @author ODias
 *
 */
public class TransportReactionsGeneration {

	private Map<String, String[]> miriamCodes;
	private Map<String, String[]> miriamNames;
	private MySQLMultiThread msqlmt;
	private List<NcbiTaxon> originTaxonomy;
	private String originOrganism;
	private Map<String, Integer> taxonomyScore;
	private int counter;
	private Map<String, TaxonomyContainer> taxonomyMap;
	private List<String> metabolitesNotAnnotated, metabolitesToBeVerified;
	private Set<UnnannotatedTransportersContainer> unAnnotatedTransporters;
	private int initialHomolguesSize;
	private String fileLocation;
	private boolean isNCBIGenome;
	private long taxonomyID;

	/**
	 * @param msqlmt
	 */
	public TransportReactionsGeneration(MySQLMultiThread msqlmt) {

		this.msqlmt = msqlmt;
		this.originTaxonomy = null;
		this.miriamCodes = new TreeMap<String, String[]>();
		this.miriamNames = new TreeMap<String, String[]>();
		this.getExistingMetabolites();
		this.taxonomyScore= new TreeMap<String, Integer>();
		this.counter=0;
		this.metabolitesNotAnnotated = new ArrayList<String>(); 
		this.metabolitesToBeVerified = new ArrayList<String>();
		this.setUnAnnotatedTransporters(new TreeSet<UnnannotatedTransportersContainer>());
	}

	/**
	 * @param msqlmt
	 * @param isNCBIGenome
	 * @param taxonomyID
	 */
	public TransportReactionsGeneration(MySQLMultiThread msqlmt, boolean isNCBIGenome, long taxonomyID) {

		this.msqlmt = msqlmt;
		this.originTaxonomy = null;
		this.miriamCodes = new TreeMap<String, String[]>();
		this.miriamNames = new TreeMap<String, String[]>();
		this.getExistingMetabolites();
		this.taxonomyScore= new TreeMap<String, Integer>();
		this.counter=0;
		this.metabolitesNotAnnotated = new ArrayList<String>(); 
		this.metabolitesToBeVerified = new ArrayList<String>();
		this.setUnAnnotatedTransporters(new TreeSet<UnnannotatedTransportersContainer>());
		this.isNCBIGenome = isNCBIGenome;
		this.taxonomyID = taxonomyID;
	}


	/**
	 * @param outPath
	 * @param project_id
	 * @return
	 */
	public List<AlignedGenesContainer> getCandidatesFromDatabase(String outPath, int project_id) {

		try {

			Connection conn = this.msqlmt.openConnection();
			Statement stmt = conn.createStatement();
			List<AlignedGenesContainer> data = new ArrayList<AlignedGenesContainer>();
			Map<String, Integer> genes_map = new HashMap<String, Integer>();

			ResultSet rs = stmt.executeQuery("SELECT sw_reports.id, locus_tag, similarity, acc, tcdb_id FROM sw_reports " +
					"INNER JOIN sw_similarities ON sw_reports.id=sw_similarities.sw_report_id " +
					"INNER JOIN sw_hits ON sw_hits.id=sw_similarities.sw_hit_id " +
					" WHERE project_id = "+ project_id +
					" ORDER BY sw_reports.locus_tag, similarity DESC");

			int counter = 0;
			while(rs.next()) {

				AlignedGenesContainer alignedGenesContainer;
				String locusTag = rs.getString(2);

				if(genes_map.containsKey(locusTag)) {

					alignedGenesContainer = data.get(genes_map.get(locusTag));
				}
				else {

					alignedGenesContainer = new AlignedGenesContainer(locusTag);
					genes_map.put(locusTag, counter);
					data.add(counter, alignedGenesContainer);
					counter ++;
				}

				AlignmentResult alignmentResult = new AlignmentResult(rs.getString(4).toUpperCase(), rs.getDouble(3));
				alignedGenesContainer.addAlignmentResult(alignmentResult);

				UnnannotatedTransportersContainer unnannotatedTransportersContainer = new UnnannotatedTransportersContainer(rs.getString(4).toUpperCase(),rs.getString(5).toUpperCase());
				this.unAnnotatedTransporters.add(unnannotatedTransportersContainer);
			}
			
			this.setInitialHomolguesSize(this.unAnnotatedTransporters.size());

			rs = stmt.executeQuery("SELECT uniprot_id, tc_number, latest_version FROM tcdb_registries;");

			while(rs.next()) {

				UnnannotatedTransportersContainer transporter = new UnnannotatedTransportersContainer(rs.getString(1).toUpperCase(), rs.getString(2).toUpperCase());

				if(rs.getBoolean(3)) {

					this.unAnnotatedTransporters.remove(transporter);
				}
				else {

					if(this.unAnnotatedTransporters.contains(transporter)) {

						System.out.println("OLD version for UniProtID "+transporter.getUniprot_id()+", removing from unnanotated registries.");
						this.unAnnotatedTransporters.remove(transporter);
					}
				}
			}

			if(outPath!=null && this.unAnnotatedTransporters.size()>0) {

				this.generateAnnotationFile(outPath);
			}
			stmt.close();
			conn.close();
			return data;
		} 
		catch (SQLException e) {

			e.printStackTrace();
		}
		return  null;
	}

	/**
	 * @param genomeID
	 * @return
	 * @throws SQLException 
	 */
	public int createNewProject(int genome_id) throws SQLException {

		Connection conn = this.msqlmt.openConnection();
		LoadTransportersData ltd = new LoadTransportersData(conn.createStatement());
		int project_id =  ltd.createNewProject(genome_id);
		conn.close();
		return project_id;
	}

	/**
	 * @param project_id
	 * @throws SQLException 
	 */
	public void deleteGenesFromProject(int project_id) throws SQLException {

		Connection conn = this.msqlmt.openConnection();
		LoadTransportersData ltd = new LoadTransportersData(conn.createStatement());
		ltd.deleteGenesFromProject(project_id);
		conn.close();

	}

	/**
	 * @param project_id
	 * @param version
	 * @throws SQLException 
	 */
	public void deleteProject(int project_id, int version) throws SQLException {

		Connection conn = this.msqlmt.openConnection();
		LoadTransportersData ltd = new LoadTransportersData(conn.createStatement());
		ltd.deleteProject(project_id, version);
		conn.close();
	}



	/**
	 * @param genome_id
	 * @return
	 * @throws SQLException 
	 */
	public int getProject(int genome_id) throws SQLException {

		Connection conn = this.msqlmt.openConnection();
		LoadTransportersData ltd = new LoadTransportersData(conn.createStatement());
		int project_id = ltd.getProjectID(genome_id);
		conn.close();

		return project_id;
	}

	/**
	 * @param genome_id
	 * @return
	 * @throws SQLException 
	 */
	public Set<Integer> getAllProjects(int genome_id) throws SQLException {

		Connection conn = this.msqlmt.openConnection();
		LoadTransportersData ltd = new LoadTransportersData(conn.createStatement());
		conn.close();
		return ltd.getAllProjectIDs(genome_id);
	}

	//	/**
	//	 * @param path
	//	 * @throws SQLException
	//	 */
	//	public void parse_and_load_candidates(String path, int project_id) {
	//
	//		this.parse_and_load_candidates(new File(path), project_id);
	//
	//	}
	//
	//	/**
	//	 * @param path
	//	 */
	//	public void parse_and_load_candidates(File path, int project_id) {
	//
	//		this.parse_and_load_candidates(this.read_transport_candidates(path), project_id);
	//	}

	/**
	 * @param outPath
	 * @throws SQLException 
	 */
	private void generateAnnotationFile(String outPath) throws SQLException {

		Connection conn = this.msqlmt.openConnection();
		AnnotateTransporters annotate = new AnnotateTransporters(conn);
		annotate.setIds(new ArrayList<UnnannotatedTransportersContainer>(this.unAnnotatedTransporters));
		annotate.annotate(outPath);
		conn.close();
	}

	/**
	 * @param alignedGenesContainer
	 * @param project_id
	 * @return
	 */	
	public boolean parse_and_load_candidates(List<AlignedGenesContainer> alignedGenesContainer, int project_id) {

		try {

			Connection conn = this.msqlmt.openConnection();
			LoadTransportersData ltd = new LoadTransportersData(conn.createStatement());

			this.setOrganismsTaxonomyScore(ltd);

			Set<String> geneSet = ltd.getLoadedGenes();

			//List<CandidatesAssignments> genome = new ArrayList<CandidatesAssignments>();

			for(AlignedGenesContainer alignedGenes: alignedGenesContainer) {

				if(!geneSet.contains(alignedGenes.getLocusTag())) {

					if(this.originTaxonomy==null) {

						TaxonomyContainer organism_data;

						if(this.isNCBIGenome) {

							organism_data = UniProtAPI.setOriginOrganism(alignedGenes.getLocusTag(),0);
						}
						else {

							//organism_data = UniProtAPI.getTaxonomyFromNCBITaxnomyID(this.taxonomyID,0);
							organism_data = UniProtAPI.getTaxonomyFromNCBI(this.taxonomyID,0);
						}

						this.originOrganism = organism_data.getSpeciesName();
						this.originTaxonomy = organism_data.getTaxonomy();
					}

					String gene_id = ltd.loadGene(alignedGenes.getLocusTag(), project_id);
					//CandidatesAssignments candidateAssignments= new CandidatesAssignments(alignedGenes.getLocusTag());

					ltd.load_genes_has_tcnumber(gene_id, alignedGenes.getAlignmentResult());

					//					for(AlignmentResult alignmentResult : alignedGenes.getAlignmentResult()) {	
					//						ltd.load_genes_has_tcnumber(genes_id, key[1],key[0],key[2]);
					//
					//						TransporterEntry transporterEntry = new TransporterEntry(key[2], taxonomy , this.originTaxonomy, (new Double(key[0])/similaritySum), key[6]);
					//
					//						if(Boolean.valueOf(tcnumber_id[1]))	//if tcnumber is not already loaded {
					//
					//							////////////////////////////////////////////////////////////////////////////////////////////////////////
					//							////////////////////////////////////////////////////////////////////////////////////////////////////////
					//							transport_system_id=ltd.load_transport_system(type_id);	//load transport system
					//						for(int j=0; j<transportParsing.getMetabolites().get(i).size(); j++) {
					//
					//							String metabolite=transportParsing.getMetabolites().get(i).get(j);
					//							String metaboliteDirection=transportParsing.getDirections().get(i).get(j);
					//							int metaboliteStoichiometry=transportParsing.getStoichiometries().get(i).get(j);
					//
					//							transportedMetabolitesEntry.addMetabolites(metabolite,metaboliteDirection,metaboliteStoichiometry);
					//
					//							String[] result = this.getMiriamCodes(metabolite);
					//							String[] names = this.getMiriamNames(metabolite,result);
					//
					//							String metabolites_id=ltd.loadMetabolite(metabolite, result, names);
					//							String direction_id=ltd.loadDirection(metaboliteDirection);
					//							ltd.load_transported_metabolites_direction(metabolites_id, direction_id, transport_system_id,metaboliteStoichiometry);
					//						}
					//
					//						////////////////////////////////////////////////////////////////////////////////////////////////////////
					//						candidateAssignments.addTransporterEntry(0.3, transporterEntry,this.originTaxonomy);
					//					}
					//				}

					if(ltd.geneIsNotProcessed(gene_id)) {

						for(AlignmentResult alignedResult : alignedGenes.getAlignmentResult()) {

							String transport_type_id = ltd.getTransportTypeID(alignedResult.getUniprot_id()) ;

							for(String metabolites_id:ltd.getMetabolitesID(alignedResult.getUniprot_id())) {

								double similarity_score= new Double(alignedResult.getSimilarity()), taxonomy_score = this.getTaxonomyScore(alignedResult.getUniprot_id());

								ltd.load_genes_has_metabolites(gene_id, metabolites_id, similarity_score, taxonomy_score);

								ltd.load_genes_has_metabolites_has_type(gene_id, metabolites_id, transport_type_id, similarity_score, taxonomy_score);
							}
						}
						//genome.add(candidateAssignments);
						ltd.setGeneLoaded(alignedGenes.getLocusTag());
					}
				}
			}
			conn.close();
			return true;

		} catch (Exception e) {

			e.printStackTrace();
			return false;
		}
	}

	/**
	 * @param path
	 * @throws SQLException
	 */
	public void parseAndLoadTransportersDatabase(String path, boolean verbose) throws SQLException {

		this.parseAndLoadTransportersDatabase(new File(path),  verbose);
	}

	/**
	 * @param file
	 * @throws SQLException
	 */
	public boolean parseAndLoadTransportersDatabase(File file, boolean verbose) {

		try {

			Connection conn = this.msqlmt.openConnection();
			LoadTransportersData ltd = new LoadTransportersData(conn.createStatement());
			//Set<String> uniprotSet =
			ltd.getLoadedTransporters();
			this.setOrganismsTaxonomyScore(ltd);

			List<ParserContainer> tc_data = this.readTCAnnotation_databases(file);

			////////////////////////////////////////////////////////////////////////////////////////////////////////
			for(ParserContainer parserContainer : tc_data) {

				////////////////////////////////////////////////////////////////////////////////////////////////////////
				String directions = TransportReactionsGeneration.selectDirection(parserContainer.getTransportType());	//transport directions

				int type_id = ltd.loadTransportType(TransportType.valueOf(directions).toString(), parserContainer.getTransportType());	//load type id
				////////////////////////////////////////////////////////////////////////////////////////////////////////

				////////////////////////////////////////////////////////////////////////////////////////////////////////
				TransportParsing transportParsing = new TransportParsing();	//parsing data

				if(!parserContainer.getMetabolites().equals("--") && !parserContainer.getMetabolites().equals("unkown")) {

					transportParsing.parseMetabolites(parserContainer.getMetabolites(), parserContainer.getTransportType());
				}

				if(parserContainer.getReactingMetabolites()!= null && !parserContainer.getReactingMetabolites().equals("--")) {

					transportParsing.parse_reacting_metabolites(parserContainer.getReactingMetabolites());
				}

				Set<Integer> transportSystemIds = new TreeSet<Integer>();
				// parse all lists of metabolites, each list of lists being a transport reaction
				for(int i=0;i<transportParsing.getTransportMetaboliteDirectionStoichiometryContainerLists().size();i++) {

					List<TransportMetaboliteDirectionStoichiometryContainer> tmdsList = transportParsing.getTransportMetaboliteDirectionStoichiometryContainerLists().get(i);

					tmdsList = this.loadMetabolites(tmdsList, ltd, verbose);

					boolean go = true;

					int transport_system_id = this.get_transporter_id_if_exists(tmdsList, ltd, parserContainer.getReversibility(), false, directions, type_id);

					if(transport_system_id<0) {

						//ignore antiporters that anti port same metabolite
						List<String> list_of_metabolites = new ArrayList<String>();
						List<String> list_of_direction = new ArrayList<String>();

						for(int j=0; j<tmdsList.size(); j++) {

							TransportMetaboliteDirectionStoichiometryContainer metaboliteContainer = tmdsList.get(j);

							if(list_of_metabolites.contains(metaboliteContainer.getName())) {

								if(list_of_direction.contains("in") && metaboliteContainer.getDirection().equalsIgnoreCase("out") && j==tmdsList.size()-1 && list_of_metabolites.size()==1) {

									//System.out.println(metabolite);
									go=false;
								}

								if(list_of_direction.contains("out") && metaboliteContainer.getDirection().equalsIgnoreCase("in") && j==tmdsList.size()-1 && list_of_metabolites.size()==1) {

									//System.out.println(metabolite);
									go=false;
								}
							}
							else {

								list_of_metabolites.add(metaboliteContainer.getName());
								list_of_direction.add(metaboliteContainer.getDirection());
							}
						}

						if(go) {

							transport_system_id = ltd.load_transport_system(type_id, parserContainer.getReversibility());	//load transport system

							for(int j=0; j<tmdsList.size(); j++) {

								TransportMetaboliteDirectionStoichiometryContainer metaboliteContainer = tmdsList.get(j);

								//String[] miriam_codes = this.getMiriamCodes(metaboliteContainer, verbose);

								//this.getMiriamNames(metaboliteContainer, miriam_codes, verbose);

								int metabolites_id = ltd.loadMetabolite(metaboliteContainer, LoadTransportersData.DATATYPE.MANUAL);

								//								if(miriam_codes[1]!=null) {
								//
								//									String metaboliteChebiID = ExternalRefSource.CHEBI.getSourceId(miriam_codes[1]);
								//
								//									// not CoA childs
								//									if(metaboliteChebiID!=null && !miriam_codes[1].equals("urn:miriam:obo.chebi:CHEBI:15346"))  {
								//
								//										Map<String, ChebiER> chebi_entity = MIRIAM_Data.get_chebi_miriam_child_metabolites(metaboliteChebiID);
								//
								//										if(chebi_entity!=null) {
								//
								//											ltd.load_metabolites_ontology(metaboliteChebiID, metabolites_id, chebi_entity,0);
								//										}
								//									}
								//								}
								String direction_id=ltd.loadDirection(metaboliteContainer.getDirection());
								ltd.load_transported_metabolites_direction(metabolites_id, direction_id, transport_system_id, metaboliteContainer.getStoichiometry());
							}
						}
						else {

							System.out.println("Jumping "+tmdsList.get(0).getName()+" transport.");
						}
					}

					if(go) {

						transportSystemIds.add(transport_system_id);
					}
				}

				ltd.loadTCnumber(parserContainer, transportSystemIds);

				//ltd.load_tc_number_has_transport_system(parserContainer.getTc_number(), transport_system_id, tc_version);

				ltd.setTCnumberLoaded(parserContainer.getUniprot_id());
			}

			conn.close();

			String filePath = FileUtils.getCurrentTempDirectory(this.msqlmt.get_database_name());
			this.metabolitesToBeVerified.removeAll(this.metabolitesNotAnnotated);
			File fileOut = new File(filePath+"/notAnnotated.txt");
			fileOut.createNewFile();
			FileWriter fstream = new FileWriter(fileOut);
			BufferedWriter out = new BufferedWriter(fstream);

			for(String met:this.metabolitesNotAnnotated) {

				out.write(met+"\n");
			}
			out.close();
			fstream.close();

			fileOut = new File(filePath+"/verify.txt");
			fstream = new FileWriter(fileOut);
			out = new BufferedWriter(fstream);

			for(String met:this.metabolitesToBeVerified) {

				out.write(met+"\n");
			}
			out.close();
			fstream.close();
			conn.close();
			return true;
		}
		catch (SQLException e) {

			e.printStackTrace();
			System.err.println("Error transport reactions generation : " + e.getMessage());

			return false;
		}
		catch (Exception e) {

			e.printStackTrace();
			System.err.println("Error transport reactions generation : " + e.getMessage());
			return false;
		}
	}

	/**
	 * @param tmdsList
	 * @param ltd
	 * @param verbose
	 * @throws SQLException
	 */
	private List<TransportMetaboliteDirectionStoichiometryContainer> loadMetabolites(List<TransportMetaboliteDirectionStoichiometryContainer> tmdsList, LoadTransportersData ltd, boolean verbose) throws SQLException {

		for(int j=0; j<tmdsList.size(); j++) {

			TransportMetaboliteDirectionStoichiometryContainer metaboliteContainer = tmdsList.get(j);

			String[] miriam_codes = this.getMiriamCodes(metaboliteContainer, verbose);

			this.getMiriamNames(metaboliteContainer, miriam_codes, verbose);

			metaboliteContainer.setKegg_miriam(miriam_codes[0]);
			metaboliteContainer.setChebi_miriam(miriam_codes[1]);

			ltd.loadMetabolite(metaboliteContainer, LoadTransportersData.DATATYPE.MANUAL);

			tmdsList.set(j, metaboliteContainer);
		}

		return tmdsList;
	}

	/**
	 * @param metabolite
	 * @param result
	 * @return
	 */
	private String[] getMiriamNames(TransportMetaboliteDirectionStoichiometryContainer metabolite, String[] result, boolean verbose) {

		String[] names = new String[2];

		try {

			if(this.miriamNames.containsKey(metabolite.getName())) {

				this.counter=0;
				return this.miriamNames.get(metabolite.getName());
			}
			else {

				names=MIRIAM_Data.getMIRIAM_Names(result,0, verbose);

				if(names==null) {

					names = new String[2];
					names[0]=null;
					names[1]=null;
					this.counter=0;
					this.miriamNames.put(metabolite.getName(), names);
					return names;
				}
				else {

					metabolite.setKegg_name(names[0]);
					metabolite.setChebi_name(names[1]);

					if(metabolite.getKegg_miriam()!=null) {

						metabolite.setKegg_miriam(result[0]);
					}

					if(metabolite.getChebi_miriam()!=null) {

						metabolite.setChebi_miriam(result[1]);
					}

					this.miriamNames.put(metabolite.getName(), names);
					this.counter=0;
					return names;
				}
			}
		}			
		catch(Exception u) {

			if(this.counter<10) {

				names = getMiriamNames(metabolite, result, verbose);this.counter++;
			}
			else {

				if(verbose) {

					u.printStackTrace();
				}
				else {

					System.err.println("Error retrieving miriam names for "+metabolite+"\n\n.");
				}
			}
		}
		return names;
	}


	/**
	 * @param metabolite
	 * @return
	 */
	private String[] getMiriamCodes(TransportMetaboliteDirectionStoichiometryContainer metabolite, boolean verbose) {

		String[] result = new String[2];

		try {

			if(this.miriamCodes.containsKey(metabolite.getName())) {

				this.counter=0;
				return this.miriamCodes.get(metabolite.getName());
			}
			else {

				result=MIRIAM_Data.getMIRIAM_codes(metabolite.getName(), this.metabolitesToBeVerified, verbose);

				if(result==null) {

					result = new String[2];
					result[0]=null;
					result[1]=null;
					this.counter=0;
					this.miriamCodes.put(metabolite.getName(), result);
					return new String[2];
				}
				else {

					if(result[0]==null && result[1]==null) {

						this.metabolitesNotAnnotated.add(metabolite.getName());
					}

					metabolite.setKegg_miriam(result[0]);
					metabolite.setChebi_miriam(result[1]);

					this.miriamCodes.put(metabolite.getName(), result);
					this.counter=0;
					return result;
				}
			}
		}			
		catch(Exception u) {

			if(this.counter<10) {

				result = getMiriamCodes(metabolite, verbose);
				this.counter++;
			}
			else {

				if(verbose) {

					u.printStackTrace();
				}
				else {

					System.err.println("Error retrieving miriam codes for "+metabolite+"\n\n.");
				}
			}
		}
		return result;
	}

	//	/**
	//	 * @param metabolite
	//	 * @param out
	//	 * @param out2
	//	 * @throws IOException
	//	 */
	//	private void processMetabolites(String metabolite, BufferedWriter out, BufferedWriter out2) throws IOException{
	//		String[] result;
	//		if(!(this.assignments.containsKey(metabolite)||this.noResult.contains(metabolite)))
	//		{
	//			result=AssignMIRIAMCodes.getMIRIAM(metabolite);
	//			if(result==null)
	//			{
	//				this.noResult.add(metabolite);
	//				out2.append(metabolite+"\n");
	//			}
	//			else
	//			{
	//				this.assignments.put(metabolite, result);
	//				out.append(metabolite+"\t"+result[0]+"\t"+result[1]+"\n");
	//			}
	//		}
	//
	//	}

	/**
	 * @param metabolitesContainerList
	 * @param ltd
	 * @param reversibility
	 * @param invertDirections
	 * @param type_id 
	 * @return
	 */
	private int get_transporter_id_if_exists(List<TransportMetaboliteDirectionStoichiometryContainer> metabolitesContainerList, LoadTransportersData ltd, boolean reversibility, boolean invertDirections, String transport_type, int type_id) {

		if(metabolitesContainerList.size()>0) {

			for(TransportMetaboliteDirectionStoichiometryContainer tmds : metabolitesContainerList) {

				// retrieve reactions ids associated to this metabolite 
				Set<Integer> transporter_ids=ltd.get_transporter_ids(tmds.getName(), type_id);

				// retrieve Transport Systems associated to  each reaction id
				List<TransportSystemContainer> data = ltd.get_transported_metabolites_direction_stoichiometry(transporter_ids);

				for(int index=0; index<data.size(); index++) {

					TransportSystemContainer ts = data.get(index);

					List<TransportMetaboliteDirectionStoichiometryContainer> metabolitesContainerClone = new ArrayList<TransportMetaboliteDirectionStoichiometryContainer>(metabolitesContainerList);

					//System.out.println("Initial number of metabolites in container " + metabolitesContainerClone.size());

					boolean transportSystemEmpty = true;

					for(int j=0; j<ts.getMetabolites().size();j++) {

						TransportMetaboliteDirectionStoichiometryContainer metaboliteData = ts.getMetabolites().get(j);

						if(invertDirections) {

							if(metaboliteData.getDirection().equalsIgnoreCase("in")) {

								metaboliteData.setDirection("out");
							}
							else if(metaboliteData.getDirection().equalsIgnoreCase("out")) {

								metaboliteData.setDirection("in");
							}
						}

						boolean existsInClone = false;

						for (int t = 0; t < metabolitesContainerClone.size(); t++) {

							TransportMetaboliteDirectionStoichiometryContainer metaboliteDataClone = metabolitesContainerClone.get(t);

							if(metaboliteDataClone.equals(metaboliteData)) {

								existsInClone = true;
								metaboliteData = metaboliteDataClone;

							}
						}

						if(existsInClone) {

							metabolitesContainerClone.remove(metaboliteData);
						}
						else {

							transportSystemEmpty = false;
						}

						//System.out.println("\tNumber of remaining metabolites " + metabolitesContainerClone.size());
					}

					//System.out.println("Number of remaining metabolites " + metabolitesContainerClone.size());

					if(metabolitesContainerClone.isEmpty() && transportSystemEmpty && ts.isReversibility()==reversibility) {

						//System.out.println("returning id  "+ts.getId());
						//System.out.println("#####################################################################################################################");
						return ts.getId();
					}
				}
				//				System.out.println("no id");
				//				System.out.println("#####################################################################################################################");
			}

			if((transport_type.equalsIgnoreCase("influx") || transport_type.equalsIgnoreCase("efflux")) && reversibility && !invertDirections) {

				//System.out.println("Reversing directions on reversible reactions to check reaction.");
				return this.get_transporter_id_if_exists(metabolitesContainerList, ltd, reversibility, true, transport_type, type_id);
			}
		}		
		return -1;
	}


	/**
	 * @param file
	 * @return
	 */
	private List<ParserContainer> readTCAnnotation_databases(File file) {

		List<ParserContainer> data = new ArrayList<ParserContainer>();

		BufferedReader reader = null;

		try {

			reader = new BufferedReader(new FileReader(file));
			String text = null;

			while ((text = reader.readLine()) != null) {

				if(text.trim().length()>0 && !text.contains("homologue ID") && !text.contains("UniProt ID") && !text.contains("TCDB description")) {

					StringTokenizer st = new StringTokenizer(text,"\t");

					ParserContainer parserContainer = new ParserContainer();

					parserContainer.setUniprot_id(st.nextToken().replace("\"", "").trim().toUpperCase()); // 1st

					if(!this.taxonomyMap.containsKey(parserContainer.getUniprot_id())) {

						TaxonomyContainer organism_data= UniProtAPI.get_uniprot_entry_organism(parserContainer.getUniprot_id(),0);

						this.taxonomyMap.put(parserContainer.getUniprot_id(), organism_data);
					}

					parserContainer.setTaxonomyContainer(this.taxonomyMap.get(parserContainer.getUniprot_id()));

					parserContainer.setTc_number(st.nextToken().replace("\"", "").trim()); // 2nd 
					parserContainer.setTc_family(st.nextToken().replace("\"", "").trim()); // 3rd
					parserContainer.setTransportType(st.nextToken().replace("\"", "").trim().toLowerCase()); // 4th
					parserContainer.setMetabolites(st.nextToken().replace("\"", "").trim().toLowerCase()); // 5th
					parserContainer.setReversibility(Boolean.valueOf(st.nextToken().replace("\"", "").trim())); // 6th
					parserContainer.setReactingMetabolites(st.nextToken().replace("\"", "").trim().toLowerCase()); // 7th
					parserContainer.setGeneral_equation(st.nextToken().replace("\"", "").trim()); // 8th

					parserContainer.setAffinity(null);
					parserContainer.setTc_location(null);

					data.add(parserContainer);
				}
			}
			reader.close();
		} 
		catch (FileNotFoundException e) {e.printStackTrace();}
		catch (IOException e) {e.printStackTrace();}
		return data;
	}


	//	/**
	//	 * @param string
	//	 * @return
	//	 */
	//	private boolean hasCapital(String string){  
	//		int ascii = (int)string.charAt(0);  
	//		if(ascii >= 65 && ascii <= 90){return true;}
	//		else {return false;}
	//	}

	/**
	 * @param ltd 
	 * @throws SQLException 
	 * 
	 */
	private void setOrganismsTaxonomyScore(LoadTransportersData ltd) throws SQLException {

		this.taxonomyMap = ltd.getOrganismsTaxonomyScore();
	}

	/**
	 * @param uniprot_id
	 * @return
	 */
	private double getTaxonomyScore(String uniprot_id) {

		if(this.taxonomyScore.containsKey(uniprot_id)) {

			return 	this.taxonomyScore.get(uniprot_id);
		}

		if(!this.taxonomyMap.containsKey(uniprot_id)) {

			TaxonomyContainer organism_data = UniProtAPI.get_uniprot_entry_organism(uniprot_id,0);

			this.taxonomyMap.put(uniprot_id, organism_data);
		}

		if(this.taxonomyMap.get(uniprot_id)==null || this.originTaxonomy==null) {

			return 0;	
		}
		else {

			this.taxonomyMap.get(uniprot_id).getTaxonomy().retainAll(originTaxonomy);
		}

		int size = this.taxonomyMap.get(uniprot_id).getTaxonomy().size();

		if(this.taxonomyMap.get(uniprot_id).getSpeciesName().equals(this.originOrganism)) {

			size+=1;
		}

		this.taxonomyScore.put(uniprot_id, size);
		return size;
	}

	/**
	 * 
	 */
	public void getExistingMetabolites(){
		try
		{
			Connection conn = this.msqlmt.openConnection();
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM metabolites");
			while(rs.next())
			{
				String[] data1=new String[2], data2=new String[2];

				data1[0]=rs.getString(3);
				data1[1]=rs.getString(5);

				data2[0]=rs.getString(4);
				data2[1]=rs.getString(6);

				this.miriamCodes.put(rs.getString(2), data1);
				this.miriamNames.put(rs.getString(2), data2);
			}

			rs = stmt.executeQuery("SELECT * FROM synonyms JOIN metabolites ON (metabolite_id=metabolites.id);");
			while(rs.next())
			{
				String[] data1=new String[2], data2=new String[2];

				data1[0]=rs.getString(6);
				data1[1]=rs.getString(8);

				data2[0]=rs.getString(7);
				data2[1]=rs.getString(9);

				this.miriamCodes.put(rs.getString(3), data1);
				this.miriamNames.put(rs.getString(3), data2);
			}
			stmt.close();
			conn.close();
		}
		catch (SQLException e) {e.printStackTrace();}
	}

	/**
	 * @param tc_direction_data
	 * @return
	 */
	public static String selectDirection(String tc_direction_data) {

		if(tc_direction_data.equals("in:in"))
			return "symport";

		if(tc_direction_data.equals("in // out"))
			return "antiport";

		if(tc_direction_data.equals("in/out"))
			return "transport";

		if(tc_direction_data.equals("out"))
			return "efflux";

		if(tc_direction_data.equals("in"))
			return "influx";

		if(tc_direction_data.equals("sensor"))
			return "sensor";

		return "complex";
	}

	/**
	 * @return the unAnnotatedTransporters
	 */
	public Set<UnnannotatedTransportersContainer> getUnAnnotatedTransporters() {
		return unAnnotatedTransporters;
	}

	/**
	 * @param unAnnotatedTransporters the unAnnotatedTransporters to set
	 */
	public void setUnAnnotatedTransporters(Set<UnnannotatedTransportersContainer> unAnnotatedTransporters) {
		this.unAnnotatedTransporters = unAnnotatedTransporters;
	}

	/**
	 * @return the initialHomolguesSize
	 */
	public int getInitialHomolguesSize() {
		return initialHomolguesSize;
	}

	/**
	 * @param initialHomolguesSize the initialHomolguesSize to set
	 */
	public void setInitialHomolguesSize(int initialHomolguesSize) {
		this.initialHomolguesSize = initialHomolguesSize;
	}

	/**
	 * @return the fileLocation
	 */
	public String getFileLocation() {
		return fileLocation;
	}

	/**
	 * @param fileLocation the fileLocation to set
	 */
	public void setFileLocation(String fileLocation) {
		this.fileLocation = fileLocation;
	}

}
