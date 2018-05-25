/**
 * 
 */
package pt.uminho.ceb.biosystems.merlin.transporters.core.transport.reactions.loadTransporters;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.ws.WebServiceException;

import pt.uminho.ceb.biosystems.merlin.bioapis.externalAPI.ebi.chebi.ChebiAPIInterface;
import pt.uminho.ceb.biosystems.merlin.bioapis.externalAPI.ebi.chebi.ChebiER;
import pt.uminho.ceb.biosystems.merlin.bioapis.externalAPI.ebi.uniprot.MyNcbiTaxon;
import pt.uminho.ceb.biosystems.merlin.bioapis.externalAPI.ebi.uniprot.TaxonomyContainer;
import pt.uminho.ceb.biosystems.merlin.bioapis.externalAPI.ebi.uniprot.UniProtAPI;
import pt.uminho.ceb.biosystems.merlin.bioapis.externalAPI.kegg.KeggAPI;
import pt.uminho.ceb.biosystems.merlin.bioapis.externalAPI.kegg.datastructures.KeggCompoundER;
import pt.uminho.ceb.biosystems.merlin.database.connector.databaseAPI.TransportersAPI;
import pt.uminho.ceb.biosystems.merlin.database.connector.datatypes.DatabaseUtilities;
import pt.uminho.ceb.biosystems.merlin.database.connector.datatypes.Enumerators.DatabaseType;
import pt.uminho.ceb.biosystems.merlin.transporters.core.transport.MIRIAM_Data;
import pt.uminho.ceb.biosystems.merlin.transporters.core.transport.reactions.containerAssembly.TransportMetaboliteDirectionStoichiometryContainer;
import pt.uminho.ceb.biosystems.merlin.transporters.core.transport.reactions.containerAssembly.TransportReactionCI;
import pt.uminho.ceb.biosystems.merlin.transporters.core.transport.reactions.parseTransporters.AlignmentResult;
import pt.uminho.ceb.biosystems.merlin.transporters.core.transport.reactions.parseTransporters.ParserContainer;
import pt.uminho.ceb.biosystems.merlin.transporters.core.transport.reactions.parseTransporters.TransportSystemContainer;
import pt.uminho.ceb.biosystems.merlin.transporters.core.utils.GeneProteinAnnotation;
import pt.uminho.ceb.biosystems.merlin.transporters.core.utils.TracebackAnnotations;
import pt.uminho.ceb.biosystems.merlin.utilities.DatabaseProgressStatus;
import pt.uminho.ceb.biosystems.merlin.utilities.External.ExternalRefSource;
import uk.ac.ebi.kraken.interfaces.uniprot.NcbiTaxon;

/**
 * @author ODias
 *
 */
public class LoadTransportersData {

	//	private Map<String,Integer> keggMiriam, chebiMiriam;
	private Map<String,String> directionMap;
	private Map<String,Integer> transportDirectionsMap;
	private Map<String, Integer>  organism_id;
	private Statement statement;
	private Map <String, Integer> local_database_id;
	private Map<String, Set<String>> genes_uniprot;
	private Map<String, Integer> uniprot_latest_version;
	//private Set<String> synonyms;
	private DatabaseType databaseType;


	/**
	 * @param statement
	 */
	public LoadTransportersData(Statement statement, DatabaseType databaseType) {

		this.statement=statement;
		this.databaseType = databaseType;
		this.directionMap=new HashMap<String, String>();
		this.transportDirectionsMap=new HashMap<String, Integer>();
		this.organism_id=new HashMap<String, Integer>();
		this.local_database_id = new HashMap<String, Integer>();
		//this.getCodesMiriam();
		this.getUniprotVersions();
		this.deleteProcessingGenes();
		this.deleteProcessingRegistries();
	}


	/**
	 * 
	 */
	//	public void getCodesMiriam() {
	//
	//		this.keggMiriam = new HashMap<String, Integer>();
	//		this.chebiMiriam = new HashMap<String, Integer>();
	//		this.synonyms = new HashSet<String>();
	//
	//		try {
	//
	//			ResultSet rs = this.statement.executeQuery("SELECT * FROM metabolites");
	//
	//			while(rs.next()) {
	//
	//				if(!rs.getString(3).isEmpty() && !rs.getString(3).equals("null"))
	//					this.keggMiriam.put(rs.getString(3), rs.getInt(1));
	//
	//				if(!rs.getString(5).isEmpty() && !rs.getString(5).equals("null"))
	//					this.chebiMiriam.put(rs.getString(5), rs.getInt(1));
	//
	//				//this.metabolites_id_map.put(rs.getString(2).toLowerCase(), rs.getInt(1));
	//			}
	//
	//			rs = this.statement.executeQuery("SELECT name, metabolite_id FROM synonyms;");
	//			while(rs.next())
	//				//this.metabolites_id_map.put(rs.getString(1).toLowerCase(), rs.getInt(2));
	//				this.synonyms.add(rs.getString(1).toLowerCase());
	//
	//		}
	//		catch (SQLException e) {e.printStackTrace();}
	//	}


	/**
	 * @param locus_tag
	 * @return
	 */
	public String loadGene(String locusTag, int projectID) {

		try {

			
			String query = "SELECT id FROM genes WHERE locus_tag='"+locusTag+"' " +
					"AND status='"+DatabaseProgressStatus.PROCESSED+"' AND project_id = "+projectID+";";
			
			String query2 = "INSERT INTO genes (project_id, locus_tag, status) "
					+ "VALUES("+projectID+",'"+locusTag+"', '"+DatabaseProgressStatus.PROCESSING+"')";

			String result = TransportersAPI.loadGene(query, query2, locusTag, projectID, statement);

			return result;
		}
		catch (SQLException e) {

			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @return
	 */
	public Map<String, TaxonomyContainer> get_taxonomy_map() {

		Map<String, TaxonomyContainer> taxonomyMap = new TreeMap<String, TaxonomyContainer>();
		Set<String> uniprot_ids = this.get_uniprot_database_ids();

		for(String uniprot_id : uniprot_ids) {

			TaxonomyContainer data = UniProtAPI.get_uniprot_entry_organism(uniprot_id);

			taxonomyMap.put(uniprot_id, data);
		}
		return taxonomyMap;
	}

	/**
	 * @throws SQLException
	 */
	private Set<String> get_uniprot_database_ids() {

		Set<String> uniprot_ids = new TreeSet<String>();

		try {

			uniprot_ids = TransportersAPI.getUniprotDatabaseIDs(statement);
		}
		catch (SQLException e) {e.printStackTrace();}

		return uniprot_ids;
	}


	/**
	 * @param parserContainer
	 * @param newTransportSystemIds
	 * @return
	 * @throws SQLException 
	 */
	public void loadTCnumber(ParserContainer parserContainer, Set<Integer> newTransportSystemIds) throws SQLException {

		String taxonomyString="";

		if(parserContainer.getTaxonomyContainer() != null && parserContainer.getTaxonomyContainer().getTaxonomy() !=null)
			taxonomyString=parserContainer.getTaxonomyContainer().getTaxonomy().toString();

		String uniprot_id = parserContainer.getUniprot_id();
		String tc_number = parserContainer.getTc_number();

		//verify reactions associated to uniprot id 

		Set<Integer> loadedTransportSystemIds = this.getTransportSystems(uniprot_id, tc_number); 

		// process transporters with new tc numbers

		Set<Integer> loadedClone = new TreeSet<Integer>(loadedTransportSystemIds);
		Set<Integer> newClone = new TreeSet<Integer>(newTransportSystemIds);

		loadedClone.removeAll(newTransportSystemIds);
		newClone.removeAll(loadedTransportSystemIds);

		// verify tc numbers associated to uniprot registry

		//	String old_tcnumber = this.getUniprotLastestVersionTCnumber(uniprot_id);

		int tc_version = -1;

		tc_version = this.getTC_version(tc_number, uniprot_id);

		// tc version is updated if tc is new or if the existing reaction set does not match exactly the new reaction set
		boolean //update = true, 
		addTC = false;

		//	if(old_tcnumber == null || !old_tcnumber.equalsIgnoreCase(tc_number)) {

		if(tc_version<0) {

			int taxonomy_data_id = this.getOrganismID(parserContainer.getTaxonomyContainer().getSpeciesName(), DatabaseUtilities.databaseStrConverter(taxonomyString,this.databaseType));
			int general_equation_id = this.load_general_equation(parserContainer.getGeneral_equation());
			tc_version = this.addTC_number(parserContainer, taxonomy_data_id, general_equation_id);
			//update = false;
			addTC = true;
		}
		//	}

		//	if(update) {

		if(!loadedClone.isEmpty())
			tc_version = this.updateTC_version(tc_number, parserContainer.getUniprot_id());
		else
			if(!newClone.isEmpty() && loadedTransportSystemIds.size()>0)
				tc_version = this.updateTC_version(tc_number, parserContainer.getUniprot_id());
		//	}

		if(addTC)
			this.add_tcdb_registry(parserContainer, tc_version);


		for(int transport_system_id : newTransportSystemIds)
			this.load_tc_number_has_transport_system(tc_number, transport_system_id, tc_version);
	}


	/**
	 * @param uniprot_id
	 * @param tc_number 
	 * @return
	 * @throws SQLException
	 */
	private Set<Integer> getTransportSystems(String uniprotID, String tcNumber){

		Set<Integer> loadedTransportSystemIds = new TreeSet<Integer>();
		
		try {
			loadedTransportSystemIds = TransportersAPI.getTransportSystems(uniprotID, tcNumber, statement);
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return loadedTransportSystemIds;
	}
	//
	//	/**
	//	 * @param uniprot_id
	//	 * @return
	//	 * @throws SQLException 
	//	 */
	//	private String getUniprotLastestVersionTCnumber(String uniprot_id) throws SQLException {
	//
	//		String tc_number = null;
	//
	//		ResultSet rs = this.statement.executeQuery("SELECT tc_number FROM tcdb_registries " +
	//				" WHERE uniprot_id='"+uniprot_id+"' AND latest_version");
	//
	//		if (rs.next())
	//			tc_number = rs.getString(1);
	//
	//		return tc_number;
	//	}

	/**
	 * @param tc_number
	 * @param uniprot_id 
	 * @return
	 * @throws SQLException 
	 */
	private int getTC_version(String tcNumber, String uniprotID) {

		int tcVersion = -1;
		
		try {
			tcVersion = TransportersAPI.getTC_version(uniprotID, tcNumber, statement);
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return tcVersion;
	}

	/**
	 * @param parserContainer
	 * @param taxonomy_data_id
	 * @param general_equation_id
	 * @return
	 * @throws SQLException
	 */
	private int addTC_number(ParserContainer parserContainer, int taxonomy_data_id, int general_equation_id) throws SQLException {

		int tc_version = this.getTC_version(parserContainer.getTc_number(), parserContainer.getUniprot_id());

		if(tc_version<0) {

			tc_version = 1;

			this.addTC_number(parserContainer.getTc_number(), tc_version, parserContainer.getTc_family(), parserContainer.getTc_location(), parserContainer.getAffinity(), taxonomy_data_id, general_equation_id);
		}

		return tc_version;
	}


	/**
	 * @param tc_number
	 * @param tc_version
	 * @param tc_family
	 * @param tc_location
	 * @param affinity
	 * @param taxonomy_data_id
	 * @param general_equation_id
	 * @return
	 * @throws SQLException
	 */
	private int addTC_number(String tc_number, int tc_version, String tc_family, String tc_location, String affinity, int taxonomy_data_id, int general_equation_id){

		String query1 = "SELECT * FROM tc_numbers WHERE tc_number='"+tc_number+"' AND tc_version = "+tc_version+";";
		
		String query2 = "INSERT INTO tc_numbers (tc_number, tc_version, tc_family, tc_location, affinity, taxonomy_data_id, general_equation_id)" +
				" VALUES('"+tc_number+"', "+tc_version+", '"+tc_family+"','"+tc_location+"'," + "'"+affinity+"', "+taxonomy_data_id+", "+general_equation_id+")";
		
		try {
			TransportersAPI.addTC_number(query1, query2, statement);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return tc_version;
	}


	/**
	 * @param parserContainer
	 * @param tc_version
	 * @throws SQLException
	 */
	private void add_tcdb_registry(ParserContainer parserContainer, int tc_version) throws SQLException {

		this.add_tcdb_registry(parserContainer.getUniprot_id(), parserContainer.getTc_number(), tc_version, DatabaseProgressStatus.PROCESSING);

	}

	/**
	 * @param parserContainer
	 * @param tc_version
	 * @throws SQLException
	 */
	private void add_tcdb_registry(String uniprotID, String tcNumber, int tcVersion, DatabaseProgressStatus processing){

		int currentVersion = 1;

		try {
			currentVersion = TransportersAPI.select_tcdb_registry(uniprotID, statement);
			
			String query1 = "UPDATE tcdb_registries SET latest_version = false, loaded_at=loaded_at " +
					" WHERE uniprot_id = '"+uniprotID+"' AND tc_number = '"+tcNumber+"';";
			
			TransportersAPI.executeQuery(query1, statement);

			String query2 = "INSERT INTO tcdb_registries (uniprot_id, version, tc_number, tc_version, status, latest_version) " +
					" VALUES('"+uniprotID+"', "+currentVersion+", '"+tcNumber+"', "+tcVersion+", '"+processing+"', true)";
			
			TransportersAPI.executeQuery(query2, statement);
			
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}


	/**
	 * @param tc_number
	 * @param uniprot
	 * @return
	 * @throws SQLException
	 */
	private int updateTC_version(String tcNumber, String uniprot) throws SQLException {

		int tc_version = -1;
		String[] list;

		ArrayList<String[]> result = TransportersAPI.getTaxonomyData(tcNumber, statement);
		
		String tc_family, tc_location, affinity;
		int taxonomy_data_id, general_equation_id;

		if(result.size()>0){
			for(int i=0; i<result.size(); i++){
				list = result.get(i);
				if(Integer.parseInt(list[0])>0) {
		
					tc_version = 1 + Integer.parseInt(list[0]);
					taxonomy_data_id = Integer.parseInt(list[1]);
					tc_family = list[2];
					tc_location = list[3];
					affinity = list[4];
					general_equation_id = Integer.parseInt(list[5]);
		
					this.addTC_number(tcNumber, tc_version, tc_family, tc_location, affinity, taxonomy_data_id, general_equation_id);
					this.add_tcdb_registry(uniprot, tcNumber, tc_version, DatabaseProgressStatus.PROCESSED);
				}
			}
		}
		else {

			throw new SQLException(" No TC number available!");
		}

		return tc_version;
	}

	/**
	 * update all registries for a tc number
	 * 
	 * @param tc_number
	 * @param version
	 * @return
	 * @throws SQLException
	 */
	public void updateUniprotRegistries(String tcNumber, int tc_version) throws SQLException {

		Set<String> uniprotIDs = TransportersAPI.selectUniprotIDs(tcNumber, statement);

		if(uniprotIDs.size()>0) {

			for(String uniprot : uniprotIDs)
				this.add_tcdb_registry(uniprot, tcNumber, tc_version, DatabaseProgressStatus.PROCESSED);
		}

	}

	/**
	 * @param organism
	 * @param taxonomy
	 * @return
	 */
	public int getOrganismID(String organism, String taxonomy){

		organism = DatabaseUtilities.databaseStrConverter(organism,this.databaseType);

		int result=-1;

		if(this.organism_id.containsKey(organism)) {

			result=this.organism_id.get(organism);
			return result;
		}
		else {

			try {

				result = TransportersAPI.selectTaxonomyID(organism, statement);
				if(result!=-1) {

					return result;
				}
				
				result = TransportersAPI.insertTaxonomyID(organism, taxonomy, statement);
				
				this.organism_id.put(organism, result);

				return result;
			}
			catch (SQLException e) {

				e.printStackTrace();
			}
		}
		return result;
	}

	/**
	 * @param genes_id
	 * @param tcnumber_id
	 * @param similarity
	 * @return
	 * @throws SQLException 
	 */
	public void load_genes_has_tcnumber(String genes_id, List<AlignmentResult> alignmentResults) throws SQLException {

		for(AlignmentResult alignmentResult : alignmentResults) {

			if(this.genes_uniprot.containsKey(genes_id) && this.genes_uniprot.get(genes_id).contains(alignmentResult.getUniprot_id())) {

				System.out.println("Uniprot_id already loaded.");

				//double similarity_int = alignmentResult.getSimilarity() + rs.getDouble(3);
				//this.statement.execute("UPDATE genes_has_tcnumber SET similarity='"+similarity_int+"'"+
				//" WHERE genes_id = '"+genes_id+"' AND uniprot_id='"+alignmentResult.getUniprot_id()+"' AND version = "+uniprot_version.get(alignmentResult.getUniprot_id())+"");
			}
			else {

				if(this.uniprot_latest_version.containsKey(alignmentResult.getUniprot_id().toUpperCase())){
					String query ="INSERT INTO genes_has_tcdb_registries (gene_id, version, uniprot_id, similarity)" +
							" VALUES('"+genes_id+"', "+this.uniprot_latest_version.get(alignmentResult.getUniprot_id().toUpperCase())+
							", '"+alignmentResult.getUniprot_id()+"','"+alignmentResult.getSimilarity()+"')";
					
					TransportersAPI.executeQuery(query, statement);
				}
				else
					System.out.println("Uniprot record "+alignmentResult.getUniprot_id()+" not available in database!");
			}
		}
	}

	/**
	 * @param gene_id
	 * @param metabolites_id
	 * @param similarity_score
	 * @param taxonomy_score
	 */
	public void load_genes_has_metabolites(String geneID, String metabolitesID, double similarity_score, double taxonomy_score) {

		try {

			ArrayList<String[]> result = TransportersAPI.getSimilarityAndTaxonomyScore(geneID, metabolitesID, statement);
			String[] list = new String[6];
			
			if(result.size()>0){
				for(int i = 0; i<result.size(); i++){
					list = result.get(i);
					
					similarity_score += Double.parseDouble(list[0]);
					taxonomy_score += Double.parseDouble(list[1]);
					int frequency = Integer.parseInt(list[2])+1;
					this.statement.execute("UPDATE genes_has_metabolites SET similarity_score_sum='"+similarity_score+"'" +
							", taxonomy_score_sum='"+taxonomy_score+"' " +
							", frequency='"+frequency+"' " +
							"WHERE metabolite_id='"+metabolitesID+"' AND gene_id='"+geneID+"'");
				}
			}
			else {

				String query = "INSERT INTO genes_has_metabolites (gene_id, metabolite_id, similarity_score_sum, taxonomy_score_sum, frequency)" +
						" VALUES('"+geneID+"','"+metabolitesID+"','"+similarity_score+"','"+taxonomy_score+"','1')";
				
				TransportersAPI.executeQuery(query, statement);
			}

		}
		catch (SQLException e) {

			System.err.println("Gene id "+geneID);
			System.err.println("Metabolites id "+metabolitesID);
			e.printStackTrace();}
	}

	/**
	 * @param genes_id
	 * @param metabolites_id
	 * @param type_id
	 * @param score
	 */
	public void load_genes_has_metabolites_has_type(String genesID, String metabolitesID, String typeID, double transport_type_score_sum, double taxonomy_score) {

		try {

			ArrayList<String[]> result = TransportersAPI.selectGeneHasMetaboliteHasType(genesID, metabolitesID, typeID, statement);
			String[] list = new String[3];

			if(result.size()>0) {
				
				for(int i = 0; i<result.size(); i++){
					list = result.get(i);

				transport_type_score_sum += Double.parseDouble(list[0]);
				taxonomy_score += Double.parseDouble(list[1]);
				int frequency = Integer.parseInt(list[2])+1;

				String query = "UPDATE genes_has_metabolites_has_type SET transport_type_score_sum='"+transport_type_score_sum+"'" +
						", taxonomy_score_sum='"+taxonomy_score+"' " +
						", frequency='"+frequency+"' " +
						"WHERE gene_id='"+genesID+"' AND metabolite_id='"+metabolitesID+"' AND transport_type_id='"+typeID+"'" ;
				
				TransportersAPI.executeQuery(query, statement);
			
				}
			}
			else {

				String query = "INSERT INTO genes_has_metabolites_has_type (gene_id, metabolite_id, transport_type_id, transport_type_score_sum, taxonomy_score_sum,frequency)" +
						" VALUES('"+genesID+"','"+metabolitesID+"','"+typeID+"','"+transport_type_score_sum+"','"+taxonomy_score+"','1')";
				
				TransportersAPI.executeQuery(query, statement);
			}

		}
		catch (SQLException e) {e.printStackTrace();}
	}

	/**
	 * @param tcnumber_id
	 * @param transport_system_id
	 */
	public void load_tc_number_has_transport_system(String tcNumberID, int transportSystemID, int tcVersion) {

		try {

			ArrayList<String[]> result = TransportersAPI.loadTcNumberHasTransportSystem(tcNumberID, transportSystemID, tcVersion, statement);


			if(result.size() == 0){
				String query = "INSERT INTO tc_numbers_has_transport_systems (tc_number, tc_version, transport_system_id)" +
						" VALUES('"+tcNumberID+"', "+tcVersion+", "+transportSystemID+")";
			
				TransportersAPI.executeQuery(query, statement);
			}
		}
		catch (SQLException e) {

			System.out.println("tc_number\t"+tcNumberID);
			System.out.println("transport_system_id\t"+transportSystemID);

			e.printStackTrace();
		}
	}


	/**
	 * @param transport_type_id
	 * @return
	 */
	public int load_transport_system(int transport_type_id, boolean reversibility) {

		try {

			int result = TransportersAPI.loadTransportSystem(transport_type_id, reversibility, statement);
			return result;
			
		}
		catch (SQLException e) {

			System.out.println("INSERT INTO transport_systems (transport_type_id, reversible) VALUES("+transport_type_id+","+reversibility+")");
			e.printStackTrace();
		}
		return -1;
	}

	/**
	 * @param metabolites_id
	 * @param direction_id
	 * @param transport_system_id
	 * @param metaboliteStoichiometry
	 */
	public void load_transported_metabolites_direction(int metabolitesID, String directionID, int transportSystemID, double metaboliteStoichiometry) {

		try {

			ArrayList<String> result = TransportersAPI.loadTransportedMetabolitesDirection(metabolitesID, directionID, transportSystemID, statement);
			
			if(result.size()>0) {

				System.err.println("WRONG STOICHIOMETRIES for transport system "+transportSystemID);
				//System.out.println("UPDATE transported_metabolites_directions SET stoichiometry ="+metaboliteStoichiometry+" " +
				//	"WHERE metabolites_id = '"+metabolites_id+"' AND transport_system_id='"+transport_system_id+"' AND direction_id='"+direction_id+"'");
			}
			else {

				String query = "INSERT INTO transported_metabolites_directions (metabolite_id,transport_system_id,direction_id, stoichiometry)" +
						" VALUES('"+metabolitesID+"','"+transportSystemID+"','"+directionID+"',"+metaboliteStoichiometry+")";
				
				TransportersAPI.executeQuery(query, statement);
			}

		}
		catch (SQLException e) {e.printStackTrace();}
	}

	/**
	 * @param direction
	 * @return
	 */
	public String loadDirection(String direction){

		if(this.directionMap.containsKey(direction)) {

			return this.directionMap.get(direction);
		}

		try {

			String result = "";
			result = TransportersAPI.getDirection(direction, statement);

			if(!result.equals("")) { 
				this.directionMap.put(direction, result);
				return result;
			}

			result = TransportersAPI.insertDirection(direction, statement);
			this.directionMap.put(direction, result);

			return result;
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param transportType
	 * @param directions
	 * @return
	 */
	public int loadTransportType(String transportType, String directions){

		if(this.transportDirectionsMap.containsKey(directions)) {

			return this.transportDirectionsMap.get(directions);
		}

		try {

			int result = TransportersAPI.selectTransportType(transportType, directions, statement);
			
			if (result == -1) 
				result= TransportersAPI.insertTransportType(transportType, directions, statement);
			
			this.transportDirectionsMap.put(directions, result);

			return result;
		}
		catch (SQLException e) {e.printStackTrace();}
		return -1;
	}


	/**
	 * @author odias
	 *
	 */
	public enum DATATYPE {

		MANUAL,
		AUTO,
	}

	/**
	 * @param metabolite
	 * @param datatype
	 * @return
	 * @throws SQLException
	 */
	public int loadMetabolite(TransportMetaboliteDirectionStoichiometryContainer metabolite, LoadTransportersData.DATATYPE datatype) throws SQLException {

		String kegg=metabolite.getKegg_miriam(),
				chebi=metabolite.getChebi_miriam();

		String kegg_name = metabolite.getKegg_name(), 
				chebi_name=metabolite.getChebi_name();

		String kegg_formula="", chebi_formula="";

		String kegg__name ="";

		String kegg_miriam ="";
		if(kegg!=null) {

			kegg_miriam = kegg;
			kegg__name=kegg_name;
			kegg_formula = this.getKeggFormula(ExternalRefSource.KEGG_CPD.getSourceId(kegg), 0);
		}

		String chebi_miriam = "";
		if(chebi!=null) {

			chebi_miriam = chebi;
			chebi_formula = this.getChebiFormula(ExternalRefSource.CHEBI.getSourceId(chebi), 0);
		}

		String name = metabolite.getName().toLowerCase();

		if(kegg!=null && chebi!= null) {

			ArrayList<String> data = TransportersAPI.getDataFromMetabolites(kegg, chebi_miriam, statement);
			
			if(data.size()>0){

				String datatypeInDatabase = data.get(1);
				int result = Integer.parseInt(data.get(2));

				if(datatype.equals(DATATYPE.MANUAL) && datatypeInDatabase.equalsIgnoreCase(DATATYPE.AUTO.toString())){
					String query ="UPDATE metabolites SET datatype='"+DATATYPE.MANUAL+"' WHERE kegg_miriam='"+kegg+"';";
					TransportersAPI.executeQuery(query, statement);
				}				

				return result;
			}

			data = TransportersAPI.getDataFromMetabolites2(kegg, statement);
			
			if(data.size()>0){
			
				String nameInDatabase= data.get(0);
				String chebiInDatabase = data.get(1);
				String datatypeInDatabase = data.get(2);
				int result = Integer.parseInt(data.get(3));

				if(nameInDatabase.equalsIgnoreCase(name) && chebi.equalsIgnoreCase(chebiInDatabase)) {

					if(datatype.equals(DATATYPE.MANUAL) && datatypeInDatabase.equalsIgnoreCase(DATATYPE.AUTO.toString())){
						String query ="UPDATE metabolites SET datatype='"+DATATYPE.MANUAL+"' WHERE kegg_miriam='"+kegg+"';";
						TransportersAPI.executeQuery(query, statement);
					}
				}
				else {

					String query = "INSERT INTO metabolites (name,kegg_miriam,chebi_miriam,kegg_name,chebi_name,datatype,kegg_formula,chebi_formula) " +
							"VALUES('"+DatabaseUtilities.databaseStrConverter(name,this.databaseType).toLowerCase()+"','"+kegg_miriam+"','"+chebi_miriam+"','"+
							DatabaseUtilities.databaseStrConverter(kegg__name,this.databaseType)+"','"+
							DatabaseUtilities.databaseStrConverter(chebi_name,this.databaseType)+
							"','"+datatype+"','"+kegg_formula+"','"+chebi_formula+"')";
					
					result = TransportersAPI.insertIntoMetabolites(query, statement);
					
					if(chebi!=null)
						this.loadOntologies(result, chebi);
				}

				return result;
			}

			String query = "INSERT INTO metabolites (name,kegg_miriam,chebi_miriam,kegg_name,chebi_name,datatype,kegg_formula,chebi_formula) " +
					"VALUES('"+DatabaseUtilities.databaseStrConverter(name,this.databaseType).toLowerCase()+"','"+kegg_miriam+"','"+chebi_miriam+"','"+
					DatabaseUtilities.databaseStrConverter(kegg__name,this.databaseType)+"','"+
					DatabaseUtilities.databaseStrConverter(chebi_name,this.databaseType)+
					"','"+datatype+"','"+kegg_formula+"','"+chebi_formula+"')";
			
			int result = TransportersAPI.insertIntoMetabolites(query, statement);

			if(chebi!=null)
				this.loadOntologies(result, chebi);

			return result;
		}

		if(kegg!=null && chebi == null) {

			ArrayList<String> data = TransportersAPI.getDataFromMetabolites3(kegg_miriam, statement);
			
			if(data.size()>0){

				String nameInDatabase = data.get(0);
				int result = Integer.parseInt(data.get(1));
				String datatypeInDatabase = data.get(2);

				if(datatypeInDatabase.equals(DATATYPE.AUTO.toString()))
					if(datatype.equals(DATATYPE.MANUAL)){
						String query = "UPDATE metabolites SET datatype='"+DATATYPE.MANUAL+"' WHERE kegg_miriam = '"+kegg+"';";
						TransportersAPI.executeQuery(query, statement);
					}

				data = TransportersAPI.getSynonyms(nameInDatabase, statement);
				
				if(data.size()==0) {

					if(!nameInDatabase.equalsIgnoreCase(name)){
						String query = "INSERT INTO synonyms (metabolite_id, name, datatype) "
								+ "VALUES("+result+",'"+DatabaseUtilities.databaseStrConverter(name,this.databaseType)+"','"+datatype+"')";
						
						TransportersAPI.executeQuery(query, statement);
					}
				}
				return result;
			}
		}

		if(chebi!=null && kegg == null) {

			ArrayList<String> data = TransportersAPI.getDataFromMetabolites4(chebi, statement);
			
			if(data.size()>0){

				String nameInDatabase = data.get(0);
				int result=Integer.parseInt(data.get(1));
				String datatypeInDatabase = data.get(2);

				if(datatypeInDatabase.equals(DATATYPE.AUTO.toString()))
					if(datatype.equals(DATATYPE.MANUAL)){
						String query ="UPDATE metabolites SET datatype='"+DATATYPE.MANUAL+"' WHERE chebi_miriam = '"+chebi+"';";
						
						TransportersAPI.executeQuery(query, statement);
					}

				int synonymOriginalID = LoadTransportersData.existsSynonym(nameInDatabase, datatype, databaseType, statement);
				if(synonymOriginalID < 0) {

					if(!nameInDatabase.equalsIgnoreCase(name))
						LoadTransportersData.insertSynonym(result, nameInDatabase, datatype, databaseType, statement);
				}
				else {

					if(synonymOriginalID != result)
						System.err.println("two mets with same synonym!!! "+synonymOriginalID+" AND "+result);
				}

				return result;
			}
		}

		if(name!= null) {

			String query = "SELECT id, datatype FROM metabolites WHERE name = '"+DatabaseUtilities.databaseStrConverter(name,this.databaseType)+"';";
			
			ArrayList<String> data = TransportersAPI.getDataFromMetabolites5(query, statement);

			if(data.size()>0) {

				int result = Integer.parseInt(data.get(0));
				String datatypeInDatabase = data.get(1);

				if(datatypeInDatabase.equals(DATATYPE.AUTO.toString()))
					if(datatype.equals(DATATYPE.MANUAL)){
						query = "UPDATE metabolites SET datatype='"+DATATYPE.MANUAL+"' WHERE name = '"+DatabaseUtilities.databaseStrConverter(name,this.databaseType)+"';";
						TransportersAPI.executeQuery(query, statement);
					}
				

				return result;
			}
			else {

				int synonymOriginalID = LoadTransportersData.existsSynonym(name, datatype, databaseType, statement);
				if(synonymOriginalID > 0)
					return synonymOriginalID;
			}
		}

		if(kegg_name!= null && !kegg_name.equalsIgnoreCase(name)) {

			kegg_name = kegg_name.toLowerCase();

			String query = "SELECT id, datatype FROM metabolites WHERE name = '"+DatabaseUtilities.databaseStrConverter(kegg__name,this.databaseType)+"';";

			ArrayList<String> data = TransportersAPI.getDataFromMetabolites5(query, statement);

			if(data.size()>0) {

				int result = Integer.parseInt(data.get(0));
				String datatypeInDatabase = data.get(1);

				if(datatypeInDatabase.equals(DATATYPE.AUTO.toString()))
					if(datatype.equals(DATATYPE.MANUAL)){
						query = "UPDATE metabolites SET datatype='"+DATATYPE.MANUAL+"' WHERE name = '"+DatabaseUtilities.databaseStrConverter(kegg__name,this.databaseType)+"';";
						TransportersAPI.executeQuery(query, statement);
					}
				
				return result;
			}
			else {

				int synonymOriginalID = LoadTransportersData.existsSynonym(kegg_name, datatype, databaseType, statement);
				if(synonymOriginalID > 0)
					return synonymOriginalID;
			}
		}

		if(chebi_name!=null && !chebi_name.equalsIgnoreCase(name)) {

			chebi_name = chebi_name.toLowerCase();

			String query = "SELECT id, datatype FROM metabolites WHERE name = '"+DatabaseUtilities.databaseStrConverter(chebi_name,this.databaseType)+"';";

			ArrayList<String> data = TransportersAPI.getDataFromMetabolites5(query, statement);

			if(data.size()>0) {

				int result = Integer.parseInt(data.get(0));
				String datatypeInDatabase = data.get(1);

				if(datatypeInDatabase.equals(DATATYPE.AUTO.toString()))
					if(datatype.equals(DATATYPE.MANUAL)){
						query = "UPDATE metabolites SET datatype='"+DATATYPE.MANUAL+"' WHERE name = '"+DatabaseUtilities.databaseStrConverter(chebi_name,this.databaseType)+"';";
						TransportersAPI.executeQuery(query, statement);
					}
				return result;
			}
			else {

				int synonymOriginalID = LoadTransportersData.existsSynonym(chebi_name, datatype, databaseType, statement);
				if(synonymOriginalID > 0)
					return synonymOriginalID;
			}
		}

		if(name.matches("\\d{4,9}")) {

			if(kegg_name!=null)
				name=kegg__name;
			else if(chebi_name!=null)
				name=chebi_name;
		}

		int result=-1;

		//		result = this.existsSynonym(metabolite,datatype);
		//
		//		if(result>0) {
		//
		//			return result;
		//		}

		//		if(kegg!=null && chebi!=null) {
		//
		//			rs = this.statement.executeQuery("SELECT id, datatype FROM metabolites WHERE kegg_miriam='"+kegg+"' AND chebi_miriam='"+chebi_miriam+"';");
		//
		//			if(rs.next()) {
		//
		//				result=rs.getInt(1);
		//
		//				if(rs.getString(2).equals(DATATYPE.AUTO.toString()))
		//					if(datatype.equals(DATATYPE.MANUAL))
		//						this.statement.execute("UPDATE metabolites SET datatype='"+DATATYPE.MANUAL+"' WHERE id ="+result+";");
		//
		//				rs.close();
		//				return result;
		//			}
		//		}

		//		if(kegg!=null && chebi==null) {
		//
		//			rs = this.statement.executeQuery("SELECT id, datatype FROM metabolites WHERE kegg_miriam='"+kegg+"';");
		//
		//			if(rs.next()) {
		//
		//				result=rs.getInt(1);
		//
		//				if(rs.getString(2).equals(DATATYPE.AUTO.toString()))
		//					if(datatype.equals(DATATYPE.MANUAL))
		//						this.statement.execute("UPDATE metabolites SET datatype='"+DATATYPE.MANUAL+"' WHERE id ="+result+";");
		//
		//				rs.close();
		//				return result;
		//			}
		//		}

		//		if(chebi!=null && kegg==null) {
		//
		//			rs = this.statement.executeQuery("SELECT id, datatype FROM metabolites WHERE chebi_miriam='"+chebi_miriam+"';");
		//			if(rs.next()) {
		//
		//				result=rs.getInt(1);
		//
		//				if(rs.getString(2).equals(DATATYPE.AUTO.toString()))
		//					if(datatype.equals(DATATYPE.MANUAL))
		//						this.statement.execute("UPDATE metabolites SET datatype='"+DATATYPE.MANUAL+"' WHERE id ="+result+";");
		//
		//				rs.close();
		//				return result;
		//			}
		//		}


		String query = "INSERT INTO metabolites (name,kegg_miriam,chebi_miriam,kegg_name,chebi_name,datatype,kegg_formula,chebi_formula) " +
				"VALUES('"+DatabaseUtilities.databaseStrConverter(name,this.databaseType).toLowerCase()+"','"+kegg_miriam+"','"+chebi_miriam+"','"+
				DatabaseUtilities.databaseStrConverter(kegg__name,this.databaseType)+"','"+
				DatabaseUtilities.databaseStrConverter(chebi_name,this.databaseType)+
				"','"+datatype+"','"+kegg_formula+"','"+chebi_formula+"')";

		result =  TransportersAPI.insertIntoMetabolites(query, statement);

		//		if(kegg!=null)
		//			this.keggMiriam.put(kegg, result);

		if(chebi!=null)
			this.loadOntologies(result, chebi);

		return result;
	}

	/**
	 * @param metaboliteID
	 * @param chebi_miriam
	 */
	private void loadOntologies(int metaboliteID, String chebi_miriam) {

		//this.chebiMiriam.put(chebi, result);

		String metaboliteChebiID = ExternalRefSource.CHEBI.getSourceId(chebi_miriam);

		// not CoA childs
		if(metaboliteChebiID!=null && !chebi_miriam.equalsIgnoreCase("urn:miriam:obo.chebi:CHEBI:15346"))  {

			Map<String, ChebiER> chebi_entity = MIRIAM_Data.get_chebi_miriam_child_metabolites(metaboliteChebiID);
			if(chebi_entity!=null)
				this.loadMetabolitesOntology(metaboliteChebiID, metaboliteID, chebi_entity,0);
		}

	}


	/**
	 * @param chebiID
	 * @param counter
	 * @return
	 */
	private String getChebiFormula(String chebiID, int counter) {

		try {

			ChebiER chebiER = ChebiAPIInterface.getExternalReference(chebiID);
			if(chebiER!=null)
				return chebiER.getFormula();
			return "";
		} 
		catch (WebServiceException e) {

			if(counter<30) {

				counter = counter+1;
				return this.getChebiFormula(chebiID, counter);
			}
			//e.printStackTrace();
			return "";

		} catch (Exception e) {

			if(counter<10) {

				counter = counter+1;
				return this.getChebiFormula(chebiID, counter);
			}
			//e.printStackTrace();
			return "";
		}
	}

	/**
	 * @param chebiID
	 * @param counter
	 * @return
	 */
	private String getKeggFormula(String keggID, int counter){

		try  {

			KeggCompoundER met = KeggAPI.getCompoundByKeggId(keggID);
			
			if(met!=null)
				return met.getFormula();

			return "";
		} 
		catch (WebServiceException e) {
			
			if(counter<10) {
				
				counter = counter+1;
				return this.getKeggFormula(keggID, counter);
			}
			e.printStackTrace();
			return "";

		}
		catch (Exception e) {

			if(counter<10) {
				
				counter = counter+1;
				return this.getKeggFormula(keggID, counter);
			}
			else {

				System.out.println("No KEgg  formula "+ keggID);
			}
			//e.printStackTrace();
			return "";
		}
	}




	/**
	 * @param metabolite
	 * @param datatype
	 * @param databaseType
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
	public static int existsSynonym(String name, DATATYPE datatype, DatabaseType databaseType, Statement statement) throws SQLException{

		String query ="SELECT metabolite_id, datatype, name FROM synonyms "
				+ "WHERE name='"+DatabaseUtilities.databaseStrConverter(name,databaseType).toLowerCase()+"';";
		
		ArrayList<String> data = TransportersAPI.existsSynonym(query, statement);

		if(data.size()>0) {

			int result = Integer.parseInt(data.get(0));
			String datatypeInDatabase = data.get(1);

			if(datatypeInDatabase.equals(DATATYPE.AUTO.toString()))
				if(datatype.equals(DATATYPE.MANUAL)){
					query = "UPDATE synonyms SET datatype='"+DATATYPE.MANUAL+"' WHERE metabolite_id = "+result+";";
					TransportersAPI.executeQuery(query, statement);
				}
			return result;
		}
		return -1;
	}

	/**
	 * @param metabolite_id
	 * @param name
	 * @param datatype
	 * @param databaseType
	 * @param statement
	 * @throws SQLException
	 */
	public static void insertSynonym(int metabolite_id, String name, DATATYPE datatype, DatabaseType databaseType, Statement statement) throws SQLException {

		String query = "SELECT * FROM synonyms WHERE name='"+DatabaseUtilities.databaseStrConverter(name,databaseType).toLowerCase()+"';";
		ArrayList<String> data = TransportersAPI.existsSynonym(query, statement);

		if(data.size()>0) {
			if(datatype.equals(DATATYPE.MANUAL)){
				query = "UPDATE metabolites SET datatype='"+DATATYPE.MANUAL+"' WHERE metabolites.id="+metabolite_id+";";
				TransportersAPI.executeQuery(query, statement);
			}
		}
		else {
			query = "INSERT INTO synonyms (metabolite_id, name, datatype) VALUES("+metabolite_id+",'"+DatabaseUtilities.databaseStrConverter(name, databaseType).toLowerCase()+"','"+datatype+"')";
			TransportersAPI.executeQuery(query, statement);
		}
	}


	/**
	 * @param metabolite
	 * @return
	 */
	public int getMetaboliteID(String metabolite) {

		try {

			int result = -1;

			String query = "SELECT id FROM metabolites WHERE name='"+DatabaseUtilities.databaseStrConverter(metabolite,this.databaseType)+"';";
			ArrayList<String> data = TransportersAPI.getMetaboliteIDs(query, statement);

			if(data.size()>0) {
				result=Integer.parseInt(data.get(0));
			}
			else {

				query = "SELECT metabolite_id FROM synonyms WHERE name='"+DatabaseUtilities.databaseStrConverter(metabolite,this.databaseType)+"';";
				result=TransportersAPI.getMetaboliteID(query, statement);
			}
			return result;
		}
		catch (SQLException e) {e.printStackTrace();}
		return -1;
	}


	/**
	 * @param uniprot_id
	 * @return
	 */
	public Set<String> getTransportTypeID(String uniprotID) {

		try {
			return TransportersAPI.getTransportTypeID(uniprotID, statement);
		}
		catch (SQLException e) {e.printStackTrace();}
		return null;
	}

	/**
	 * @param uniprot_id
	 * @return
	 */
	public Set<String> getMetabolitesID(String uniprotID){

		try {
			return TransportersAPI.getMetabolitesID(uniprotID, statement);
		}
		catch (SQLException e) {e.printStackTrace();}
		return null;
	}

	/**
	 * @param metabolites_id
	 * @return
	 */
	public Set<String> getTransportTypesID(String metabolitesID){

		try
		{
			return TransportersAPI.getTransportTypesID(metabolitesID, statement);
		}
		catch (SQLException e) {e.printStackTrace();}
		return null;
	}


	/**
	 * @param transporterIds
	 * @return
	 */
	public List<TransportSystemContainer> get_transported_metabolites_direction_stoichiometry(Set<Integer> transporterIds) {

		List<TransportSystemContainer> result = new ArrayList<TransportSystemContainer>();

		try {

			for(int transportSystemID: transporterIds) {

				boolean reversibility = TransportersAPI.getReversibility(transportSystemID, statement);

				TransportSystemContainer ts = new TransportSystemContainer(transportSystemID, reversibility);
				List<TransportMetaboliteDirectionStoichiometryContainer> metabolites_data= new ArrayList<TransportMetaboliteDirectionStoichiometryContainer>();
				Map<String, Integer> metabolite_name_index = new HashMap<>();
				int counter=0;
				
				ArrayList<String[]> data = TransportersAPI.getTmdscData(transportSystemID, statement);
				String[] list;

				for(int i = 0; i < data.size(); i++){
					list = data.get(i);

					TransportMetaboliteDirectionStoichiometryContainer tmds = new TransportMetaboliteDirectionStoichiometryContainer(list[0]);
					tmds.setDirection(list[1]);
					tmds.setStoichiometry(Double.parseDouble(list[2]));
					tmds.setReversible(Boolean.valueOf(list[3]));
					tmds.setKegg_name(list[4]);
					tmds.setChebi_name(list[5]);

					if(metabolite_name_index.containsKey(list[0])) {

						Set<String> synonyms = metabolites_data.get(metabolite_name_index.get(list[0])).getSynonyms();
						synonyms.add(list[6]);
						metabolites_data.get(metabolite_name_index.get(list[0])).setSynonyms(synonyms);
					}
					else {

						Set<String> synonyms = new HashSet<String>();
						synonyms.add(list[6]);
						tmds.setSynonyms(synonyms);
						metabolites_data.add(counter, tmds);

						metabolite_name_index.put(list[0], counter);
					}
				}
				ts.setMetabolites(metabolites_data);
				result.add(ts);
			}
			return result;
		}
		catch (SQLException e) {e.printStackTrace();}
		return null;
	}

	/**
	 * @param type_id 
	 * @param metabolites_id
	 * @return
	 */
	public Set<Integer> get_transporter_ids(String metabolitesName, int typeID) {

		try {

			//			ResultSet rs = this.statement.executeQuery("SELECT transport_systems.id FROM transport_systems" +
			//					" INNER JOIN transported_metabolites_directions ON (transport_systems.id = transport_system_id )" +
			//					" INNER JOIN metabolites ON metabolites.id= metabolite_id " +
			//					" WHERE UPPER(metabolites.name) = UPPER('"+metabolites_name.replace("'", "\\'")+"') AND transport_type_id = "+type_id);
			//			
			//			while(rs.next())
			//				result.add(rs.getInt(1));
			//
			//			rs = this.statement.executeQuery("SELECT transport_systems.id FROM transport_systems" +
			//					" INNER JOIN transported_metabolites_directions ON (transport_systems.id = transport_system_id )" +
			//					" INNER JOIN synonyms ON transported_metabolites_directions.metabolite_id= synonyms.metabolite_id " +
			//					" WHERE UPPER(synonyms.name) = UPPER('"+metabolites_name.replace("'", "\\'")+"') AND transport_type_id = "+type_id);
			//
			//			while(rs.next())
			//				result.add(rs.getInt(1));
			//
			//			rs = this.statement.executeQuery("SELECT transport_systems.id FROM transport_systems" +
			//					" INNER JOIN transported_metabolites_directions ON (transport_systems.id = transport_system_id )" +
			//					" INNER JOIN metabolites ON metabolites.id= metabolite_id " +
			//					" WHERE UPPER(kegg_name)= UPPER('"+metabolites_name.replace("'", "\\'")+"') AND transport_type_id = "+type_id);
			//
			//			while(rs.next())
			//				result.add(rs.getInt(1));
			//
			//			rs = this.statement.executeQuery("SELECT transport_systems.id FROM transport_systems" +
			//					" INNER JOIN transported_metabolites_directions ON (transport_systems.id = transport_system_id )" +
			//					" INNER JOIN metabolites ON metabolites.id= metabolite_id " +
			//					" WHERE UPPER(chebi_name) = UPPER('"+metabolites_name.replace("'", "\\'")+"') AND transport_type_id = "+type_id);
			//
			//			while(rs.next())
			//				result.add(rs.getInt(1));

			metabolitesName = DatabaseUtilities.databaseStrConverter(metabolitesName,this.databaseType);

			return TransportersAPI.getTransporterIDs(metabolitesName, typeID, statement);
		}
		catch (SQLException e) {

			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param metabolite_chebi_id
	 * @param metabolite_id
	 * @param chebi_entity_map
	 */
	public void loadMetabolitesOntology(String metabolite_chebi_id, int metabolite_id, Map<String, ChebiER> chebi_entity_map, int counter) {

		try {

			this.local_database_id.put(metabolite_chebi_id, metabolite_id);

			for(String key:chebi_entity_map.keySet()) {

				ChebiER chebi_entity = chebi_entity_map.get(key);

				if(chebi_entity.getFunctional_children().size()>0) {

					if(!this.local_database_id.containsKey(key)) {

						int id = getMetaboliteID(key, this.statement);
						if(id>0) {

							this.local_database_id.put(key, id);
						}
					}

					for(String child:chebi_entity.getFunctional_children()) {

						if(!this.local_database_id.containsKey(child)) {

							int id = getMetaboliteID(child, this.statement);
							if(id>0) {

								this.local_database_id.put(child, id);
							}

						}

						if(this.local_database_id.get(key) != 0 &&  this.local_database_id.containsKey(child) &&
								this.local_database_id.get(child) > 0 && this.local_database_id.get(child) != 0 &&
								this.local_database_id.get(key) != this.local_database_id.get(child)) {

							String query = "SELECT id FROM metabolites_ontology " +
									"WHERE metabolite_id="+local_database_id.get(key)+" AND " +
									"child_id="+local_database_id.get(child)+"";

							String query2 = "SELECT id FROM metabolites_ontology "
									+ "WHERE metabolite_id="+local_database_id.get(child)+" AND child_id="+local_database_id.get(key)+"";

							String query3 = "INSERT INTO metabolites_ontology (metabolite_id, child_id) "
									+ "VALUES("+local_database_id.get(key)+","+local_database_id.get(child)+")";
							
							TransportersAPI.selectIdFromMetabolitesOntology(query, query2, query3, statement);
						}
					}
				}
			}

		}
		catch (Exception e) {

			counter= counter+1;

			if(counter<30){

				this.loadMetabolitesOntology(metabolite_chebi_id, metabolite_id, chebi_entity_map, counter);
			}
			else {

				System.out.println("exception ontology "+this.local_database_id);
				e.printStackTrace();
			}
		}
	}

	/**
	 * @param metabolite_external_id
	 * @param this.statement
	 * @return
	 * @throws SQLException 
	 */
	private int getMetaboliteID(String metabolite_external_id, Statement statement) throws SQLException{

		String query = "SELECT id FROM metabolites WHERE chebi_miriam='"+ExternalRefSource.CHEBI.getMiriamCode(metabolite_external_id)+"'";

		int result = TransportersAPI.getMetaboliteID(query, statement);
		
		if(result != -1) {

			return result;
		}
		ChebiER child_entity = this.getChebiER(metabolite_external_id,0);

		if(child_entity!=null) {

			String chebi_name = child_entity.getName();
			String kegg_code = child_entity.getKegg_id();

			TransportMetaboliteDirectionStoichiometryContainer metabolite = new TransportMetaboliteDirectionStoichiometryContainer();

			String kegg_name = null;
			if(kegg_code!=null) {

				kegg_name = this.getKeggName(kegg_code,0);
				metabolite.setKegg_name(kegg_name);
				metabolite.setKegg_miriam(ExternalRefSource.KEGG_CPD.getMiriamCode(kegg_code));
			}

			metabolite.setChebi_name(chebi_name);
			metabolite.setChebi_miriam(child_entity.getMiriamCode());

			String metabolite_name;
			if(kegg_name==null) {

				metabolite_name=chebi_name;
				metabolite.setKegg_name(null);
			}
			else {

				metabolite_name=kegg_name;
			}
			metabolite.setName(metabolite_name);

			return this.loadMetabolite(metabolite, LoadTransportersData.DATATYPE.AUTO);
		}
		else {

			System.err.println("Null entity for "+metabolite_external_id);
		}

		return -1;
	}

	/**
	 * @param id
	 * @param errorCount
	 * @return
	 */
	private ChebiER getChebiER(String id, int errorCount) {

		try  {

			return ChebiAPIInterface.getChildElements(id);
		}
		catch (NullPointerException ne) {

			if (errorCount<30) {

				errorCount = errorCount+1;
				this.getChebiER(id, errorCount);
			}
		}
		catch (Exception e) {

			if (errorCount<100) {

				errorCount = errorCount+1;
				this.getChebiER(id, errorCount);
			}
		}

		System.out.println("Returning null child elements for "+id);
		return null;
	}

	/**
	 * @param kegg_code
	 * @param errorCount
	 * @return
	 */
	private String getKeggName(String kegg_code, int errorCount){

		try {

			KeggCompoundER res = KeggAPI.getCompoundByKeggId(kegg_code);
			if(res != null) {

				return res.getName().replace(";", "");
			} 
		}
		catch (Exception e)  {

			while (errorCount<10) {

				errorCount = errorCount+1;
				this.getKeggName(kegg_code, errorCount);
			}
		}

		return null;
	}

	/**
	 * @param equation
	 * @return
	 */
	public int load_general_equation(String equation) {

		try {

			int result = -1;

			String query = "SELECT id FROM general_equation WHERE equation='"+DatabaseUtilities.databaseStrConverter(equation,this.databaseType)+"'";
			result = TransportersAPI.loadGeneralEquation(result, query, statement);
			
			if (result == -1) {

				query ="INSERT INTO general_equation (equation) VALUES('"+DatabaseUtilities.databaseStrConverter(equation,this.databaseType)+"')";
				result = TransportersAPI.insertIntoGeneralEquation(query, statement);
			}

			return result;
		}
		catch (SQLException e) {

			e.printStackTrace();
		}
		return -1;
	}

	/**
	 * @param uniprot_id
	 */
	public void setTCnumberLoaded(String uniprot_id) {

		try {

			String query = "UPDATE tcdb_registries SET status='"+DatabaseProgressStatus.PROCESSED+"' WHERE uniprot_id = '" +uniprot_id+"'";
			TransportersAPI.executeQuery(query, statement);
		}
		catch (SQLException e) {

			e.printStackTrace();
		}		
	}

	/**
	 * @param uniprot_id_maps
	 */
	public void setGeneLoaded(String locusTag) {

		try {

			String query = "UPDATE genes SET status='"+DatabaseProgressStatus.PROCESSED+"' WHERE locus_tag = '" +locusTag+"'";
			TransportersAPI.executeQuery(query, statement);
		}
		catch (SQLException e) {

			e.printStackTrace();
		}		
	}

	/**
	 * @return
	 */
	public Set<String> getLoadedGenes() {

		Set<String> result = new HashSet<String>();

		try {
			this.deleteProcessingGenes();

			String query = "SELECT locus_tag FROM genes WHERE status='"+DatabaseProgressStatus.PROCESSED+"'";
			result = TransportersAPI.getLoadedGenes(query, statement);
		}
		catch (SQLException e) {

			e.printStackTrace();
		}
		return result;
	}

	/**
	 * @return
	 * @throws SQLException 
	 */
	public Set<String> getLoadedTransporters() throws SQLException {

		this.deleteProcessingRegistries();

		String query = "SELECT uniprot_id, tc_number  FROM tcdb_registries " +
				"WHERE status='"+DatabaseProgressStatus.PROCESSED+"'";
		
		return TransportersAPI.getLoadedTransporters(query, statement);
	}

	/**
	 * @param genomeID
	 * @throws SQLException 
	 */
	public int createNewProject(int genomeID) throws SQLException {

		int version = 1;
		version = TransportersAPI.updateProjectVersion(version, genomeID, statement);

		java.sql.Date sqlToday = new java.sql.Date((new java.util.Date()).getTime());

		String query = "INSERT INTO projects (organism_id, latest_version, date, version) VALUES ("+genomeID+", true, '"+sqlToday+"', "+version+")";
		
		return TransportersAPI.insertIntoProjects(query, statement);
	}

	/**
	 * @param project_id
	 * @param version
	 * @throws SQLException 
	 */
	public void deleteProject(int project_id, int version) throws SQLException {

		if(version <0) {

			String query = "DELETE FROM projects WHERE id = "+project_id;
			TransportersAPI.executeQuery(query, statement);
		}
		else if(version == 0) {

			String query = "DELETE FROM projects WHERE id = "+project_id+" AND latest_version";
			TransportersAPI.executeQuery(query, statement);
		}
		else {

			String query = "DELETE FROM projects WHERE id = "+project_id+ " AND version = "+version;
			TransportersAPI.executeQuery(query, statement);
		}
	}

	/**
	 * @param project_id
	 * @param version
	 * @throws SQLException 
	 */
	public void deleteGenesFromProject(int project_id) throws SQLException {

		String query = "DELETE FROM genes WHERE project_id = "+project_id;
		TransportersAPI.executeQuery(query, statement);
	}


	/**
	 * @param reaction
	 * @param project_id
	 * @return
	 * @throws Exception
	 */
	public TracebackAnnotations tracebackReactionAnnotation(TransportReactionCI reaction, int projectID) throws Exception {

		Map<String, Map<String, Double>> geneProtein = new HashMap<String, Map<String, Double>>();
		Map<String, String[]> protein_tcnumber = new HashMap<String, String[]>();
		Map<String, Set<String>> protein_metabolites = new HashMap<String, Set<String>>();

		for(String locusTag : reaction.getGenesIDs()) {

			try {

				ArrayList<String[]> data = TransportersAPI.tracebackReactionAnnotation(projectID, locusTag, statement);
				String[] list;

				for(int i = 0; i<data.size(); i++){
					list = data.get(i);

					Map<String, Double> proteins = new HashMap<String, Double>();
					if(geneProtein.containsKey(locusTag)) {

						proteins = geneProtein.get(locusTag);
					}
					proteins.put(list[0], Double.parseDouble(list[3]));

					geneProtein.put(locusTag, proteins);

					if(!protein_tcnumber.containsKey(list[0])) {

						protein_tcnumber.put(list[0], new String[] {list[1], list[4]});
					}

					Set<String> metabolites = new HashSet<String>();
					if(protein_metabolites.containsKey(list[0])) {

						metabolites = protein_metabolites.get(list[0]);
					}
					metabolites.add(list[2]);
					protein_metabolites.put(list[0], metabolites);
				}

			} catch (SQLException e) {

				e.printStackTrace();
				throw e;
			}
		}

		TracebackAnnotations tracebackAnnotations = new TracebackAnnotations(reaction.getId());

		for(String locus_tag : geneProtein.keySet()) {

			GeneProteinAnnotation geneProteinAnnotation = new GeneProteinAnnotation(locus_tag);

			Map<String, Double> protein = geneProtein.get(locus_tag);

			for(String uniprot_id : protein.keySet()) {

				geneProteinAnnotation.setUniprot_id(uniprot_id);
				geneProteinAnnotation.setSimilarity(protein.get(uniprot_id));
				geneProteinAnnotation.setMetabolites(protein_metabolites.get(uniprot_id));
				geneProteinAnnotation.setTc_number(protein_tcnumber.get(uniprot_id)[0]);
				geneProteinAnnotation.setEquation(protein_tcnumber.get(uniprot_id)[1]);
			}
			tracebackAnnotations.addGeneProteinAnnotation(geneProteinAnnotation);
		}

		return tracebackAnnotations;
	}


	/**
	 * @param genome_id
	 * @return
	 * @throws SQLException 
	 */
	public int getProjectID(int genomeID) throws SQLException {

		int projectID = -1;

		projectID = TransportersAPI.getProjectID(projectID, genomeID, statement);
		
		if(projectID == -1) {

			projectID = this.createNewProject(genomeID);			
		}

		return projectID;
	}

	/**
	 * @param genome_id
	 * @return
	 * @throws SQLException 
	 */
	public Set<Integer> getAllProjectIDs(int genomeID) throws SQLException {

		return TransportersAPI.getAllProjectIDs(genomeID, statement);
	}


	/**
	 * @param gene_id
	 * @return
	 */
	public boolean geneIsNotProcessed(String geneID) {

		boolean result = true;

		try {

			String status = TransportersAPI.getGeneStatus(geneID, statement);

			if(!status.equals("")) {

				if(status.equalsIgnoreCase(DatabaseProgressStatus.PROCESSING.toString())) {
					
					//probably something missing here

				}
				result = true;
			}
			else {

				result = false;
			}
		}
		catch (SQLException e) {

			e.printStackTrace();
		}		

		return result;
	}

	/**
	 * 
	 */
	private void deleteProcessingGenes() {

		try {
			String query = "DELETE FROM genes WHERE status='"+DatabaseProgressStatus.PROCESSING+"'";
			TransportersAPI.executeQuery(query, statement);
		} 
		catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 */
	private void deleteProcessingRegistries() {

		try {

			String query = "DELETE FROM tcdb_registries WHERE status='"+DatabaseProgressStatus.PROCESSING+"'";
			TransportersAPI.executeQuery(query, statement);
		} 
		catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void getUniprotVersions() {

		this.genes_uniprot = new HashMap<String, Set<String>>();
		this.uniprot_latest_version = new HashMap<String, Integer>();

		try {

			ArrayList<String[]> data = TransportersAPI.selectGeneIdAndUniprotId(statement);

			for(int i = 0; i<data.size(); i++){
				String[] list = data.get(i);
				
				Set<String> uni = new HashSet<String>();

				if(this.genes_uniprot.containsKey(list[0])) {

					uni = this.genes_uniprot.get(list[0]);
				}

				uni.add(list[1]);

				this.genes_uniprot.put(list[0], uni);
			}

			data = TransportersAPI.getUniprotVersion(statement);
			
			for(int i = 0; i<data.size(); i++){
				String[] list = data.get(i);
				this.uniprot_latest_version.put(list[0].toUpperCase(), Integer.parseInt(list[1]));
			}


		} catch (SQLException e) {

			e.printStackTrace();
		}
	}


	/**
	 * @return
	 * @throws SQLException 
	 */
	public Map<String, TaxonomyContainer> getOrganismsTaxonomyScore() throws SQLException {

		Map<String, TaxonomyContainer> result = new TreeMap<String, TaxonomyContainer>();

		ArrayList<String[]> table = TransportersAPI.getOrganismsTaxonomyScore(statement);

		for(int i = 0; i<table.size(); i++){
			String[] data = table.get(i);

			TaxonomyContainer container = new TaxonomyContainer(data[1]);

			List<NcbiTaxon> list = new ArrayList<NcbiTaxon>();

			String taxonomy = data[2].replace("[", "").replace("]", "");

			StringTokenizer st = new StringTokenizer(taxonomy,",");

			while(st.hasMoreTokens()) {

				MyNcbiTaxon ncbiTaxon = new MyNcbiTaxon(st.nextToken().trim());
				list.add(ncbiTaxon);
			}

			container.setTaxonomy(list);
			result.put(data[0], container);
		}
		return result;
	}
}
