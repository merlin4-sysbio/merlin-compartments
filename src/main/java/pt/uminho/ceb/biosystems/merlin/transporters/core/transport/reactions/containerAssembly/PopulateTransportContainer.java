/**
 * 
 */
package pt.uminho.ceb.biosystems.merlin.transporters.core.transport.reactions.containerAssembly;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.rpc.ServiceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import biosynth.core.components.representation.basic.graph.DefaultBinaryEdge;
import biosynth.core.components.representation.basic.graph.Graph;
import pt.uminho.ceb.biosystems.merlin.bioapis.externalAPI.ebi.uniprot.TaxonomyContainer;
import pt.uminho.ceb.biosystems.merlin.bioapis.externalAPI.ncbi.NcbiAPI;
import pt.uminho.ceb.biosystems.merlin.database.connector.databaseAPI.TransportersAPI;
import pt.uminho.ceb.biosystems.merlin.database.connector.datatypes.Connection;
import pt.uminho.ceb.biosystems.merlin.database.connector.datatypes.Enumerators.DatabaseType;
import pt.uminho.ceb.biosystems.merlin.transporters.core.transport.reactions.parseTransporters.MetaboliteTaxonomyScores;
import pt.uminho.ceb.biosystems.merlin.utilities.External.ExternalRefSource;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.components.StoichiometryValueCI;
import pt.uminho.ceb.biosystems.mew.biocomponents.validation.chemestry.BalanceValidator;

/**
 * @author ODias
 * @author Oscar
 *
 */
public class PopulateTransportContainer extends Observable implements Observer {

	private static final Logger logger = LoggerFactory.getLogger(PopulateTransportContainer.class);
	private Statement stmt;
	private Map<String, Set<String>> selectedGenesMetabolites, rejectedGenesMetabolites;
	private Map<String, Set<TransportReaction>> genesReactions;
	private Map<String, ProteinFamiliesSet> genesProteins;
	private Map<String, String> genesLocusTag;
	private Map<String, GenesMetabolitesTransportType> genesMetabolitesTransportTypeMap;
	private Map<String, TransportMetabolite> transportMetabolites;
	private String[] origin_array;
	private double threshold;
	private double beta;
	private int minimalFrequency;
	private double alpha;
	private double originTaxonomy;
	private Map<String,String> transport_type_list;
	private Map<String, Set<String>> metabolites_ontology;
	private HashMap<String, Set<String>> child_IDs_Map;
	private Map<String,String> metabolitesFormula;
	private AtomicBoolean cancel;
	private AtomicInteger geneProcessingCounter;
	private AtomicInteger querySize;
	private int project_id;
	private Graph<String, String> graph;
	private Map<String, String> kegg_miriam, chebi_miriam;
	private Set<String> ignoreSymportMetabolites;
	private DatabaseType dbType;

	/**
	 * @param conn
	 * @param alpha
	 * @param minimalFrequency
	 * @param beta
	 * @param threshold
	 * @param project_id
	 * @param ignoreSymportMetabolites
	 * @throws Exception

	public PopulateTransportContainer(Connection conn, double alpha, int minimalFrequency, double beta, double threshold, int project_id, Set<String> ignoreSymportMetabolites) throws Exception {

		this.stmt = conn.createStatement();
		this.child_IDs_Map = new HashMap<String, Set<String>>();
		this.metabolites_ontology = new HashMap<String,Set<String>>();
		this.alpha=alpha;
		this.minimalFrequency=minimalFrequency;
		this.beta=beta;
		this.threshold=threshold;
		this.transportMetabolites=new TreeMap<String, TransportMetabolite>();

		int remoteExceptionTrials = 0;
		this.originTaxonomy=this.getOriginTaxonomy(remoteExceptionTrials);

		this.metabolitesFormula = new HashMap<String,String>();
		this.cancel = new AtomicBoolean(false);
		this.geneProcessingCounter = new AtomicInteger();
		this.querySize = new AtomicInteger();
		this.project_id = project_id;
		this.graph = new Graph<String, String>();
		this.kegg_miriam = new ConcurrentHashMap<String, String>();
		this.chebi_miriam = new ConcurrentHashMap<String, String>();
		this.ignoreSymportMetabolites = ignoreSymportMetabolites;
	}
	 */

	/**
	 * @param conn
	 * @param alpha
	 * @param minimalFrequency
	 * @param beta
	 * @param threshold
	 * @param taxonomy_id
	 * @param project_id
	 * @param ignoreSymportMetabolites
	 * @param databaseType
	 * @throws Exception
	 */
	public PopulateTransportContainer(Connection conn, double alpha, int minimalFrequency, double beta, double threshold, long taxonomy_id, int project_id, Set<String> ignoreSymportMetabolites, DatabaseType databaseType) throws Exception {

		this.stmt = conn.createStatement();
		this.child_IDs_Map = new HashMap<String, Set<String>>();
		this.metabolites_ontology = new HashMap<String,Set<String>>();
		this.alpha=alpha;
		this.minimalFrequency=minimalFrequency;
		this.beta=beta;
		this.threshold=threshold;
		this.transportMetabolites=new TreeMap<String, TransportMetabolite>();

		int remoteExceptionTrials = 0;
		this.originTaxonomy=this.getOriginTaxonomy(taxonomy_id, remoteExceptionTrials);

		this.metabolitesFormula = new HashMap<String,String>();
		this.cancel = new AtomicBoolean(false);
		this.geneProcessingCounter = new AtomicInteger();
		this.querySize = new AtomicInteger();
		this.project_id = project_id;

		this.graph = new Graph<String, String>();
		this.kegg_miriam = new ConcurrentHashMap<String, String>();
		this.chebi_miriam = new ConcurrentHashMap<String, String>();
		this.ignoreSymportMetabolites = ignoreSymportMetabolites;

		this.dbType = databaseType;
	}

	/**
	 * @param conn
	 * @throws SQLException 
	 */
	public PopulateTransportContainer (Connection conn) throws SQLException {

		this.stmt = conn.createStatement();
	}


	/**
	 * @return
	 * @throws SQLException 
	 */
	public boolean getDataFromDatabase() throws Exception {

		long startTime = System.currentTimeMillis();
		this.transport_type_list = getTransportTypeList(stmt);
		long endTime = System.currentTimeMillis();

		this.populateGraph();
		System.out.println("Total elapsed time in execution of populate Graph is :"+ String.format("%d min, %d sec", 
				TimeUnit.MILLISECONDS.toMinutes(endTime-startTime),TimeUnit.MILLISECONDS.toSeconds(endTime-startTime)
				- TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(endTime-startTime))));

		this.setMetaboltitesFormulas();
		System.out.println("Total elapsed time in execution of setMetaboltitesFormulas is :"+ String.format("%d min, %d sec", 
				TimeUnit.MILLISECONDS.toMinutes(endTime-startTime),TimeUnit.MILLISECONDS.toSeconds(endTime-startTime)
				- TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(endTime-startTime))));

		this.getMetabolitesAboveThreshold();
		endTime = System.currentTimeMillis();
		System.out.println("Total elapsed time in execution of getMetabolitesAboveThreshold is :"+ String.format("%d min, %d sec", 
				TimeUnit.MILLISECONDS.toMinutes(endTime-startTime),TimeUnit.MILLISECONDS.toSeconds(endTime-startTime)
				- TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(endTime-startTime))));

		this.getTransportTypeTaxonomyScore();
		endTime = System.currentTimeMillis();
		System.out.println("Total elapsed time in execution of getTransportTypeTaxonomyScore is :"+ String.format("%d min, %d sec", 
				TimeUnit.MILLISECONDS.toMinutes(endTime-startTime),TimeUnit.MILLISECONDS.toSeconds(endTime-startTime)
				- TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(endTime-startTime))));

		this.getReactions();
		endTime = System.currentTimeMillis();
		System.out.println("Total elapsed time in execution of getReactions is :"+ String.format("%d min, %d sec", 
				TimeUnit.MILLISECONDS.toMinutes(endTime-startTime),TimeUnit.MILLISECONDS.toSeconds(endTime-startTime)
				- TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(endTime-startTime))));

		return true;
	}


	/**
	 * @throws SQLException
	 */
	private void populateGraph() throws SQLException {

		ArrayList<String[]> result = TransportersAPI.selectMetabolitesOntology(stmt);

		for(int i=0; i<result.size(); i++){
			String[] list = result.get(i);
			
			this.graph.addEdge(new DefaultBinaryEdge<String, String>(list[0], list[1], list[2]));
			
		}
		
		result = TransportersAPI.selectChebiMiriam(stmt);
		
		for(int i=0; i<result.size(); i++){
			String[] list = result.get(i);
			
			if(list[2]!=null  && !list[2].equalsIgnoreCase("null"))
				this.chebi_miriam.put(list[0], list[2]);

			//if(rs.getString(2)!=null  && !rs.getString(2).equalsIgnoreCase("null")) {

			//this.kegg_miriam.put(rs.getString(1), rs.getString(2));
			//}
		}
	}
	
	/**
	 * @throws SQLException 
	 * 
	 */
	public static Map<String, String> getTransportTypeList(Statement stmt) throws SQLException {

		return TransportersAPI.getTransportTypeList(stmt);
	}
	
	/**
	 * @throws SQLException 
	 * 
	 */
	private void getMetabolitesAboveThreshold() throws SQLException {

		this.selectedGenesMetabolites = new TreeMap<String, Set<String>>();
		this.rejectedGenesMetabolites = new TreeMap<String, Set<String>>();

		Map<Integer, MetaboliteTaxonomyScores> metaboliteTaxonomyScoresMap = this.getMetabolitesGeneScore();

		for(int i: metaboliteTaxonomyScoresMap.keySet()) {

			String metabolite = metaboliteTaxonomyScoresMap.get(i).getMetabolite();
			String gene = metaboliteTaxonomyScoresMap.get(i).getGene();

			if(metaboliteTaxonomyScoresMap.get(i).getScore()>=this.threshold) {

				Set<String> metabolitesList = new TreeSet<String>();

				if(this.selectedGenesMetabolites.containsKey(gene))
					metabolitesList=this.selectedGenesMetabolites.get(gene);

				metabolitesList.add(metabolite);

				if(!this.metabolites_ontology.containsKey(metabolite)) {

					Set<String> data = new HashSet<String>();

					data = this.getOntologyMetabolites(metabolite, data);
					this.metabolites_ontology.put(metabolite, data);
				}

				this.selectedGenesMetabolites.put(gene, metabolitesList);
			}
			else {

				Set<String> metabolitesList = new TreeSet<String>();

				if(this.rejectedGenesMetabolites.containsKey(gene))
					metabolitesList=this.rejectedGenesMetabolites.get(gene);

				metabolitesList.add(metabolite);
				this.rejectedGenesMetabolites.put(gene, metabolitesList);
			}
		}
	}

	/**
	 * @param ignoreMetabolites
	 * @return
	 * @throws SQLException
	 */
	private Map<Integer, MetaboliteTaxonomyScores> getMetabolitesGeneScore() throws SQLException {

		Map<Integer, MetaboliteTaxonomyScores> temp = new HashMap<Integer, MetaboliteTaxonomyScores>();
		int counter = 0;

		//logger.debug("getMetabolitesGeneScore project id {}", this.project_id);

		if (this.dbType.equals(DatabaseType.MYSQL)){
			//		System.out.println("CALL getMetaboliteTaxonomyScores("+this.originTaxonomy+","+this.minimalFrequency+","+this.alpha+","+this.beta+","+this.project_id+");");
			String query = "CALL getMetaboliteTaxonomyScores("+this.originTaxonomy+","+this.minimalFrequency+","+this.alpha+","+this.beta+","+this.project_id+");";

			ArrayList<String[]> result = TransportersAPI.getMetabolitesGeneScore(query, stmt);
			
			for(int i = 0; i<result.size(); i++){
				
				String[] list = result.get(i);

				MetaboliteTaxonomyScores data = new MetaboliteTaxonomyScores(list[1], list[0], Double.parseDouble(list[2]));
				counter ++;
				temp.put(counter, data);
			}
		}
		else {

			Map<String, Map<String, Double>> procedure_data = procedure_getMetabolitesGeneScore(this.project_id);

			for (String geneID : procedure_data.keySet()) {

				for (String metaboliteID : procedure_data.get(geneID).keySet()) {	

					double score =  procedure_data.get(geneID).get(metaboliteID);

					MetaboliteTaxonomyScores data = new MetaboliteTaxonomyScores(geneID, metaboliteID, score);
					counter ++;

					temp.put(counter, data);
				}
			}
		}
		return temp;
	}

	/**
	 * @param metabolite_id
	 * @throws SQLException 
	 */
	private Set<String> getOntologyMetabolites(String metabolite_id, Set<String> child_ids_results) throws SQLException {

		try {

			Set<String> child_ids = new TreeSet<String>();
			child_ids = this.getChildId(metabolite_id);

			Set<String> iterator = new HashSet<String>(child_ids);
			iterator.removeAll(child_ids_results);

			child_ids_results.addAll(child_ids);

			if(child_ids.size()>0) {

				for(String child_id:iterator) {

					child_ids_results.addAll(this.getOntologyMetabolites(child_id,child_ids_results));
				}
			}
			return child_ids_results;
		}
		catch (StackOverflowError e) {

			System.out.println(metabolite_id);
			e.printStackTrace();
		}
		return null;
	}


	/**
	 * @param child_ids
	 * @return
	 * @throws SQLException 
	 */
	private Set<String> getChildId(String id) throws SQLException {

		if(this.child_IDs_Map.containsKey(id))
			return this.child_IDs_Map.get(id);

		Set<String> child_ids_results = new TreeSet<String>();
		
		ArrayList<String[]> result = TransportersAPI.getChildID(id, stmt);

		for(int i=0; i<result.size(); i++){
			
			String[] list = result.get(i);

			child_ids_results.add(list[0]);

			TransportMetabolite transportMetabolite;

			if(this.transportMetabolites.containsKey(list[0])) {

				transportMetabolite= this.transportMetabolites.get(list[0]);
			}
			else {

				if(Integer.parseInt(list[0])>0) {

					String metaboliteName=list[3];

					if(metaboliteName.equals("null")) {

						metaboliteName=list[5];
					}

					transportMetabolite=new TransportMetabolite(list[0],metaboliteName,list[2],list[4]);
					this.transportMetabolites.put(list[0],transportMetabolite);
				}
			}
		}
		this.child_IDs_Map.put(id, child_ids_results);
		return child_ids_results;
	}


	/**
	 * @param originTaxonomy
	 * @param alpha
	 * @param minimalFrequency
	 * @param beta
	 * @return
	 * @throws SQLException 
	 */
	private void getTransportTypeTaxonomyScore() throws SQLException {

		this.genesMetabolitesTransportTypeMap = new TreeMap<String, GenesMetabolitesTransportType>();

		if (this.dbType.equals(DatabaseType.MYSQL)){
			
			String query = "CALL getTransportTypeTaxonomyScore("+this.originTaxonomy+","+this.minimalFrequency+","+this.alpha+","+this.beta+","+this.project_id+");";

			ArrayList<String[]> result = TransportersAPI.getTransportTypeTaxonomyScore(query, stmt);
			
			for(int i=0; i<result.size(); i++){
				String[] list = result.get(i);
				
				String geneID = list[2];
				String metaboliteID = list[1];
				String transportType = list[0];

				double score=Double.parseDouble(list[3]);
				GenesMetabolitesTransportType genesMetabolitesTransportType;

				if(this.genesMetabolitesTransportTypeMap.containsKey(geneID))
					genesMetabolitesTransportType= this.genesMetabolitesTransportTypeMap.get(geneID);
				else
					genesMetabolitesTransportType= new GenesMetabolitesTransportType(geneID);

				transportType = this.transport_type_list.get(transportType);
				genesMetabolitesTransportType.addMetaboliteTransportType(metaboliteID, transportType, score);
				this.genesMetabolitesTransportTypeMap.put(geneID,genesMetabolitesTransportType);
			}
		}
		else {

			Map<String, Map<String, Map<String, Double>>> procedure_data = procedure_getTransportTypeTaxonomyScore(this.project_id);

			for (String geneID : procedure_data.keySet()) {

				for (String metaboliteID : procedure_data.get(geneID).keySet()) {	

					for (String transportType : procedure_data.get(geneID).get(metaboliteID).keySet()) {	

						double score =  procedure_data.get(geneID).get(metaboliteID).get(transportType) ;

						GenesMetabolitesTransportType genesMetabolitesTransportType;

						if(this.genesMetabolitesTransportTypeMap.containsKey(geneID))
							genesMetabolitesTransportType= this.genesMetabolitesTransportTypeMap.get(geneID);
						else
							genesMetabolitesTransportType= new GenesMetabolitesTransportType(geneID);

						transportType = this.transport_type_list.get(transportType);
						genesMetabolitesTransportType.addMetaboliteTransportType(metaboliteID, transportType, score);
						this.genesMetabolitesTransportTypeMap.put(geneID,genesMetabolitesTransportType);
					}
				}
			}
		}
	}


	/**
	 * @throws SQLException 
	 * 
	 */
	private void getReactions() throws Exception {

		this.genesReactions = new HashMap<String, Set<TransportReaction>>();
		this.genesLocusTag= new HashMap<String, String>();
		this.genesProteins= new HashMap<String, ProteinFamiliesSet>();
		Map<Integer, Double> organismsTaxonomyScore = TransportersAPI.getOrganismsTaxonomyScore(this.origin_array, stmt);
		String previousUniprotEntry="";
		boolean openReaction=true;
		
		Map<String, String> tc_numbers_equations = TransportersAPI.getTcNumersEquations(stmt);
		
		ArrayList<String[]> result = TransportersAPI.getGeneToMetaboliteDirectionData(this.project_id, stmt);
		
		for(int i=0; i<result.size(); i++){
			String[] list = result.get(i);
			
			String geneID = list[0];
			String reactionID = "T"+ idConverter(list[3].trim());
			String metaboliteID = list[4];
			String direction = list[6];
			double stoichiometry=Double.parseDouble(list[5]);
			this.genesLocusTag.put(geneID, list[1]);
			ProteinFamiliesSet proteins;

			if(this.genesProteins.containsKey(geneID))
				proteins=this.genesProteins.get(geneID);
			else
				proteins=new ProteinFamiliesSet(originTaxonomy, alpha, beta, minimalFrequency);

			ProteinFamily proteinFamily = proteins.get_protein_family(list[2]);
			proteinFamily.add_tc_number(list[16], list[11], Double.parseDouble(list[12]), list[13], organismsTaxonomyScore.get(Integer.parseInt(list[13])));
			//sum+=rs.getDouble(13);

			proteins.calculateTCfamily_score();
			this.genesProteins.put(geneID, proteins);

			////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

			Set<TransportReaction> transportReactionSet;
			if(this.genesReactions.containsKey(geneID))
				transportReactionSet = this.genesReactions.get(geneID);
			else
				transportReactionSet = new TreeSet<TransportReaction>();

			boolean isNew=true;
			boolean existsReactionInCurrentSet = false;
			boolean sameUniprotID = previousUniprotEntry.equalsIgnoreCase(list[16]);
			TransportReaction transportReaction=null;

			for(TransportReaction existingtransportReaction:transportReactionSet) {

				existsReactionInCurrentSet = existingtransportReaction.getReactionID().equals(reactionID);
				if(existsReactionInCurrentSet) {

					isNew=false;
					if(sameUniprotID && openReaction) {

						transportReaction=existingtransportReaction;
					}
					else {

						// dummy reaction  for going on
						transportReaction = new TransportReaction("dummy", "", true, true, list[1], "");
						openReaction=false;
					}
				}
			}

			if(isNew) {

				openReaction=true;
				transportReaction = new TransportReaction(reactionID, list[14], Boolean.parseBoolean(list[15]), true, list[1], reactionID);
				transportReactionSet.add(transportReaction);
			}

			TransportMetabolite transportMetabolite;
			if(this.transportMetabolites.containsKey(metaboliteID)) {

				transportMetabolite = this.transportMetabolites.get(metaboliteID);
			}
			else {

				String metaboliteName = list[10];
				if(metaboliteName.equals("null"))
					metaboliteName=list[7];

				transportMetabolite = new TransportMetabolite(metaboliteID, metaboliteName, list[8], list[9]);
				this.transportMetabolites.put(metaboliteID,transportMetabolite);
			}

			transportReaction.addMetabolite(metaboliteID,transportMetabolite, stoichiometry, direction);
			transportReaction.addGeneral_equation(list[1], list[11], tc_numbers_equations.get(list[11]));
			transportReaction.addProteinFamilyID(list[2]);
			this.genesReactions.put(geneID,transportReactionSet);

			previousUniprotEntry=list[16];
		}
	}


	/**
	 * @param generations 
	 * @throws Exception 
	 * @throws IOException 
	 * 
	 */
	public TransportContainer loadContainer(boolean saveOnlyReactionsWithKEGGmetabolites, int generations) throws Exception {

		TransportContainer transportContainer = new TransportContainer(this.alpha,this.minimalFrequency,this.threshold,this.beta);
		transportContainer.setKeggMetabolitesReactions(saveOnlyReactionsWithKEGGmetabolites);
		transportContainer.addCompartments("out", "outside","");
		transportContainer.addCompartments("in", "inside","");

		this.genesProteins.keySet().retainAll(this.selectedGenesMetabolites.keySet());
		this.genesLocusTag.keySet().retainAll(this.selectedGenesMetabolites.keySet());
		this.genesReactions.keySet().retainAll(this.selectedGenesMetabolites.keySet());

		this.querySize.set(new Integer(this.genesReactions.keySet().size()));
		setChanged();
		notifyObservers();
		System.out.println(genesReactions);
		LoadTransportContainer ltc = new LoadTransportContainer(this.genesReactions, this.cancel, this.genesLocusTag, this.genesMetabolitesTransportTypeMap, 
				this.selectedGenesMetabolites, this.rejectedGenesMetabolites, this.genesProteins, this.transportMetabolites, this.metabolites_ontology, 
				this.metabolitesFormula, saveOnlyReactionsWithKEGGmetabolites, this.geneProcessingCounter, this.graph, this.kegg_miriam, this.chebi_miriam, this.ignoreSymportMetabolites);
		ltc.addObserver(this);
		ltc.loadContainer(generations);
		transportContainer.setMetabolites(ltc.getMetabolitesContainer());
		transportContainer.setTransportReactions(ltc.getReactionsContainer());
		transportContainer.setGenes(ltc.getGeneContainer());
		this.replaceGeneProteinsIds();
		this.genesProteins.keySet().retainAll(ltc.getGeneContainer().keySet());
		transportContainer.setGenesProteins(this.genesProteins);
		transportContainer.setKeggMiriam(this.kegg_miriam);
		transportContainer.setChebiMiriam(this.chebi_miriam);

		return transportContainer;
	}


	private void replaceGeneProteinsIds() {

		for(String id : this.genesLocusTag.keySet()) {

			this.genesProteins.put(this.genesLocusTag.get(id), this.genesProteins.get(id));
		}

		this.genesProteins.keySet().retainAll(this.genesLocusTag.values());
	}


	/**
	 * @throws IOException 
	 * 
	 */
	public void	creatReactionsFiles(TransportContainer transportContainer, String path) throws IOException {

		//new File(path+"_transport_reactions.log");
		FileWriter fstream = new FileWriter(path+"transport_reactions_unvalidated.log");  
		BufferedWriter out = new BufferedWriter(fstream);

		int reactionsCounter=0;

		for(String reaction:transportContainer.getReactions().keySet()) {

			boolean has_kegg_id=true;
			for(String key :transportContainer.getReactions().get(reaction).getReactants().keySet())
				if(transportContainer.getKeggMiriam().get(key).equals("null"))
					has_kegg_id=false;

			if(has_kegg_id)
				for(String key :transportContainer.getReactions().get(reaction).getReactants().keySet())
					if(transportContainer.getKeggMiriam().get(key).equals("null"))
						has_kegg_id=false;

			if(has_kegg_id) {

				out.write("genes:\t" + transportContainer.getReactions().get(reaction).getGenesIDs()+"\n");
				out.write("transportReaction id:\t"+ reaction+"\t"+"\n");
				out.write("TC Number:\t" + transportContainer.getReactions().get(reaction).getProteinIds()+"\n");

				Map<String, StoichiometryValueCI> reactants = transportContainer.getReactions().get(reaction).getReactants();
				Map<String, StoichiometryValueCI> products = transportContainer.getReactions().get(reaction).getProducts();
				String concat  = "";

				for(String key :reactants.keySet())
					concat=concat.concat(reactants.get(key).getStoichiometryValue()+" "+transportContainer.getMetabolites().get(reactants.get(key).getMetaboliteId()).getName()+" ("+reactants.get(key).getCompartmentId())+") + ";

				concat=concat.substring(0, concat.lastIndexOf("+")-1);

				if(transportContainer.getReactions().get(reaction).getReversible())
					concat=concat.concat(" <=> ");
				else
					concat=concat.concat(" => ");

				for(String key :products.keySet())
					concat=concat.concat(products.get(key).getStoichiometryValue()+" "+transportContainer.getMetabolites().get(products.get(key).getMetaboliteId()).getName()+" ("+products.get(key).getCompartmentId())+") + ";

				concat=concat.substring(0, concat.lastIndexOf("+")-1);
				out.write(concat+"\n\n");
				reactionsCounter++;
			}
		}
		out.write("reactions counter:\t"+reactionsCounter+"\n");
		out.write("transport container reactions size:\t"+transportContainer.getReactions().size()+"\n");

		out.close();

	}


	/**
	 * @param transportContainer
	 * @return
	 * @throws ServiceException 
	 * @throws NullPointerException 
	 */
	public TransportContainer containerValidation(TransportContainer transportContainer, boolean verbose) throws Exception {

		Map<String,String> keggIDs = new HashMap<String, String>();

		for(String compound:transportContainer.getKeggMiriam().keySet())
			if(!transportContainer.getKeggMiriam().get(compound).equals("null"))
				keggIDs.put(compound, ExternalRefSource.KEGG_CPD.getSourceId(transportContainer.getKeggMiriam().get(compound)));

		BalanceValidator balanceValidator = new BalanceValidator(transportContainer);

		boolean formulasFromContainer = balanceValidator.setFormulasFromContainer();

		if(verbose)
			System.out.println("Setting formula from container\t"+formulasFromContainer);

		Set<String> balancedReactions = balanceValidator.getAllBalancedReactions(null);

		for(String reactionID : new HashSet<String> (transportContainer.getReactions().keySet())) {

			if(!balancedReactions.contains(reactionID)) {

				if(verbose) {

					System.out.println("Reaction "+reactionID+" removed.\t"+transportContainer.getReactions().get(reactionID).getReactants());
				}
				transportContainer.getReactions().remove(reactionID);
			}
		}

		return transportContainer;
	}



	/**
	 * @param remoteExceptionTrials
	 * @return
	 * @throws Exception

	private double getOriginTaxonomy(int remoteExceptionTrials) throws Exception {

		String firstLocusTag = this.getFirstLocusTag();

		if(firstLocusTag!=null) {

			try {

				UniProtEntry entry  = UniProtAPI.getEntry(firstLocusTag, 0);

				if (entry != null) {

					this.origin_array = new String[entry.getTaxonomy().size()+1];

					for(int i=0; i<entry.getTaxonomy().size();i++) {

						System.out.println(entry.getTaxonomy().get(i).getValue());

						this.origin_array[i]=entry.getTaxonomy().get(i).getValue();
					}
					this.origin_array[this.origin_array.length-1]=entry.getOrganism().getScientificName().getValue();

					int res = entry.getTaxonomy().size()+1;

					System.out.println("Origin taxonomy "+res);

					return res;
				}
			}
			catch(Exception ex) {

				if(remoteExceptionTrials<10) {

					System.out.println(firstLocusTag);

					remoteExceptionTrials = remoteExceptionTrials+1;
					return this.getOriginTaxonomy(remoteExceptionTrials);
				}
				throw new Exception("Network error! "+ex.getMessage());
			}

		}
		else {

			System.err.println("PLEASE LOAD GENE INFORMATION!");
		}
		return 0;
	}
	 */
	/**
	 * @param remoteExceptionTrials
	 * @return
	 * @throws Exception
	 */
	public double getOriginTaxonomy(long taxonomy_id, int remoteExceptionTrials) throws Exception {

		TaxonomyContainer result = NcbiAPI.getTaxonomyFromNCBI(taxonomy_id, remoteExceptionTrials);

		this.origin_array = new String[result.getTaxonomy().size()+1];

		for(int i=0; i<result.getTaxonomy().size();i++) {

			this.origin_array[i]=result.getSpeciesName();
		}
		this.origin_array[this.origin_array.length-1]=result.getSpeciesName();
		return result.getTaxonomy().size()+1;
	}


	/**
	 * @return
	 * @throws SQLException 
	 */
	private boolean setMetaboltitesFormulas(){

		try {
			this.metabolitesFormula = TransportersAPI.setMetaboltitesFormulas(stmt);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return true;
	}

	/**
	 * @param id
	 * @return
	 */
	private String idConverter(String id) {

		String result = id;

		if(id.contains("_")) {

			result = id.split("_")[0];
		}

		if(result.length()>4) {

			return id;
		}

		if(result.length()>3) {

			return "0"+id;
		}

		if(result.length()>2) {

			return "00"+id;
		}
		if(result.length()>1) {

			return "000"+id;
		}

		return "0000"+id;
	}

	/**
	 * @return the transportMetabolites
	 */
	public Map<String, TransportMetabolite> getTransportMetabolites() {
		return transportMetabolites;
	}


	/**
	 * @param transportMetabolites the transportMetabolites to set
	 */
	public void setTransportMetabolites(
			Map<String, TransportMetabolite> transportMetabolites) {
		this.transportMetabolites = transportMetabolites;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "PopulateTransportContainer ["
				+ ", selectedGenesMetabolites=" + selectedGenesMetabolites
				+ ", genesReactions=" + genesReactions 
				+ ", genesProteins=" + genesProteins 
				+ ", genesLocusTag=" + genesLocusTag
				+ ", genesMetabolitesTransporTypeMap=" + genesMetabolitesTransportTypeMap 
				+ ", transportMetabolites=" + transportMetabolites + "]";
	}


	/**
	 * @param counter the counter to set
	 */
	public void setGeneProcessingCounter(AtomicInteger counter) {
		this.geneProcessingCounter = counter;
	}

	/**
	 * @return the counter
	 */
	public AtomicInteger getGeneProcessingCounter() {
		return geneProcessingCounter;
	}

	/**
	 * @param querySize the querySize to set
	 */
	public void setQuerySize(AtomicInteger querySize) {

		this.querySize = querySize;
	}

	/**
	 * @param cancel the cancel to set
	 */
	public void setCancel(AtomicBoolean cancel) {
		this.cancel = cancel;
	}


	@Override
	public void update(Observable arg0, Object arg1) {

		logger.debug("Counter on {}: {}", this.getClass(), this.geneProcessingCounter.get());
		setChanged();
		notifyObservers();
	}


	/**
	 * @param frequency
	 * @param minimalFrequency
	 * @return
	 */
	public static int func_getFrequency(int frequency, int minimalFrequency) {

		//		-- CREATE FUNCTION getFrequency(frequency INT, minimal_hits INT)
		//		--   RETURNS INT
		//		--   BEGIN
		//		--     DECLARE result INT(11);
		//
		//		--     IF frequency > minimal_hits THEN SET result = minimal_hits;
		//		--     ELSE SET result = frequency;
		//		--     END IF;
		//
		//		--     RETURN result;
		//		--   END
		if (frequency > minimalFrequency)
			return minimalFrequency;
		else
			return frequency;
	}

	private Map<String, Map<String, Double>> procedure_getMetabolitesGeneScore(int projectID) throws SQLException {

		Map<String, Map<String, Double>> procedure_data = new HashMap<>();
		
		Map<String, Integer> similarities = TransportersAPI.getSimilarities(stmt);
		
		for(String key : similarities.keySet())
			logger.trace("gene id {}\t sum {}", key, similarities.get(key));
		
		Map<String, ArrayList<String>> result = TransportersAPI.getMetabolitesScore(stmt);
		
		ArrayList<String> list = new ArrayList<>();

		
		for (String metaboliteID : result.keySet()){
			
			list = result.get(metaboliteID);
			
			String geneID = list.get(0);
			double similarity_score_sum = Double.parseDouble(list.get(1)), taxonomy_score_sum = Double.parseDouble(list.get(2));
			int frequency = Integer.parseInt(list.get(3));
			double frequencyScore = similarity_score_sum/(similarities.get(geneID));
			double taxonomyScore = (taxonomy_score_sum*(1-(this.minimalFrequency-func_getFrequency(frequency, this.minimalFrequency))*this.beta)/(this.originTaxonomy*frequency));

			double final_score = frequencyScore*this.alpha+(1-this.alpha)*taxonomyScore;

			Map<String, Double> metaboliteScore = new HashMap<>();
			if(procedure_data.containsKey(geneID))
				metaboliteScore = procedure_data.get(geneID); 

			metaboliteScore.put(metaboliteID, final_score);
			procedure_data.put(geneID, metaboliteScore);
		}

		//logger.debug("Procedure procedure_getMetabolitesGeneScore {} ", procedure_data.keySet().toString());

		return procedure_data;
	}

	private Map<String, Map<String, Map<String, Double>>> procedure_getTransportTypeTaxonomyScore(int projectID) throws SQLException {
		//		-- CREATE PROCEDURE getTransportTypeTaxonomyScore (IN originTaxonomy BIGINT UNSIGNED, IN minimal_hits BIGINT UNSIGNED, IN alpha FLOAT UNSIGNED, IN beta_penalty FLOAT UNSIGNED, IN idproject BIGINT UNSIGNED)
		//		-- BEGIN
		//		--    SELECT transport_type_id, metabolite_id, gene_id, transport_type_score_sum/(
		//		--        SELECT SUM(similarity)
		//		--        FROM genes_has_tcdb_registries
		//		--        INNER JOIN genes_has_metabolites ON genes_has_tcdb_registries.gene_id=genes_has_metabolites.gene_id
		//		-- 	   	INNER JOIN genes ON genes.id = genes_has_tcdb_registries.gene_id
		//		--        INNER JOIN tcdb_registries ON (genes_has_tcdb_registries.uniprot_id=tcdb_registries.uniprot_id AND genes_has_tcdb_registries.version=tcdb_registries.version)
		//		--        WHERE project_id = idproject AND latest_version AND genes_has_tcdb_registries.gene_id = genes_has_metabolites_has_type.gene_id
		//		--        AND genes_has_metabolites.metabolite_id=genes_has_metabolites_has_type.metabolite_id)
		//		-- 	   *alpha+(1-alpha)*
		//		-- 	  (genes_has_metabolites_has_type.taxonomy_score_sum*(1-(minimal_hits-getFrequency(genes_has_metabolites_has_type.frequency,minimal_hits))*beta_penalty)/(originTaxonomy*genes_has_metabolites_has_type.frequency)) as final_score
		//		--    FROM genes_has_metabolites_has_type
		//		--    ORDER BY gene_id , metabolite_id , transport_type_id;
		//		--   END
		Map<String, Map<String, Map<String, Double>>> procedure_data = new HashMap<>();
		//		ResultSet query1=this.stmt.executeQuery( "SELECT genes.id, SUM(similarity) FROM genes_has_tcdb_registries "
		//				+ "INNER JOIN genes_has_metabolites ON genes_has_tcdb_registries.gene_id=genes_has_metabolites.gene_id "
		//				+ "INNER JOIN genes ON genes.id = genes_has_tcdb_registries.gene_id "
		//				+ "INNER JOIN tcdb_registries ON (genes_has_tcdb_registries.uniprot_id=tcdb_registries.uniprot_id AND genes_has_tcdb_registries.version=tcdb_registries.version) "
		//				+ "WHERE (project_id = "+projectID+" AND latest_version AND genes_has_tcdb_registries.gene_id = genes_has_metabolites_has_type.gene_id AND genes_has_metabolites.metabolite_id=genes_has_metabolites_has_type.metabolite_id)");

		ArrayList<String[]> result = TransportersAPI.geneHasTcdbRegestries(stmt);
	
		Map<String, Map<String, Double>> transportType = new HashMap<>();

		for(int i=0; i<result.size(); i++){
			
			String[] list = result.get(i);

			Map<String, Double>  similarities = new HashMap<>();

			if(transportType.containsKey(list[0]))
				similarities = transportType.get(list[0]);
			
			similarities.put(list[1], Double.parseDouble(list[2]));
			transportType.put(list[0], similarities);
		}

		result = TransportersAPI.transportTypeID(stmt);
		
		for(int i=0; i<result.size(); i++){
			
			String[] list = result.get(i);

			Double final_score = Double.parseDouble(list[3])/(transportType.get(list[2]).get(list[1])*this.alpha+(1-this.alpha)*
					(Double.parseDouble(list[4])*(1-(this.minimalFrequency-func_getFrequency(Integer.parseInt(list[5]), this.minimalFrequency))*this.beta)/
							(this.originTaxonomy*Integer.parseInt(list[5]))));

			Map<String, Map<String, Double>> metaboliteScores = new HashMap<>();
			if(procedure_data.containsKey(list[2]))
				metaboliteScores = procedure_data.get(list[2]);

			Map<String, Double> tranportTypeScores = new HashMap<>();
			if(metaboliteScores.containsKey(list[1]))
				tranportTypeScores = metaboliteScores.get(list[1]);

			tranportTypeScores.put(list[0], final_score);


			metaboliteScores.put(list[1], tranportTypeScores);
			procedure_data.put(list[2], metaboliteScores);
		}

		//logger.debug("Procedure procedure_getTransportTypeTaxonomyScore {} ", procedure_data.keySet().toString());

		return procedure_data;
	}
}
