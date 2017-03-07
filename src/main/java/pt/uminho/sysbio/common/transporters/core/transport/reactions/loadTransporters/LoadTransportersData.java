/**
 * 
 */
package pt.uminho.sysbio.common.transporters.core.transport.reactions.loadTransporters;

import java.sql.ResultSet;
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

import pt.uminho.sysbio.common.bioapis.externalAPI.ExternalRefSource;
import pt.uminho.sysbio.common.bioapis.externalAPI.ebi.chebi.ChebiAPIInterface;
import pt.uminho.sysbio.common.bioapis.externalAPI.ebi.chebi.ChebiER;
import pt.uminho.sysbio.common.bioapis.externalAPI.ebi.uniprot.MyNcbiTaxon;
import pt.uminho.sysbio.common.bioapis.externalAPI.ebi.uniprot.TaxonomyContainer;
import pt.uminho.sysbio.common.bioapis.externalAPI.ebi.uniprot.UniProtAPI;
import pt.uminho.sysbio.common.bioapis.externalAPI.kegg.KeggAPI;
import pt.uminho.sysbio.common.bioapis.externalAPI.kegg.datastructures.KeggCompoundER;
import pt.uminho.sysbio.common.database.connector.datatypes.DatabaseUtilities;
import pt.uminho.sysbio.common.database.connector.datatypes.Enumerators.DatabaseType;
import pt.uminho.sysbio.common.transporters.core.transport.MIRIAM_Data;
import pt.uminho.sysbio.common.transporters.core.transport.reactions.containerAssembly.TransportMetaboliteDirectionStoichiometryContainer;
import pt.uminho.sysbio.common.transporters.core.transport.reactions.containerAssembly.TransportReactionCI;
import pt.uminho.sysbio.common.transporters.core.transport.reactions.parseTransporters.AlignmentResult;
import pt.uminho.sysbio.common.transporters.core.transport.reactions.parseTransporters.ParserContainer;
import pt.uminho.sysbio.common.transporters.core.transport.reactions.parseTransporters.TransportSystemContainer;
import pt.uminho.sysbio.common.transporters.core.utils.GeneProteinAnnotation;
import pt.uminho.sysbio.common.transporters.core.utils.TracebackAnnotations;
import pt.uminho.sysbio.merlin.utilities.DatabaseProgressStatus;
import uk.ac.ebi.kraken.interfaces.uniprot.NcbiTaxon;

/**
 * @author ODias
 *
 */
public class LoadTransportersData {

	private Map<String,Integer> keggMiriam, chebiMiriam;
	private Map<String,String> directionMap;
	private Map<String,Integer> transportDirectionsMap;
	private Map<String, Integer>  organism_id;
	private Statement statement;
	private Map <String, Integer> local_database_id;
	private Map<String, Set<String>> genes_uniprot;
	private Map<String, Integer> uniprot_latest_version;
	private Map<String, Integer> metabolites_id_map;
	private Set<String> synonyms;
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
		this.get_codes_miriam();
		this.getUniprotVersions();
		this.deleteProcessingGenes();
		this.deleteProcessingRegistries();
	}


	/**
	 * 
	 */
	public void get_codes_miriam() {

		this.keggMiriam = new HashMap<String, Integer>();
		this.chebiMiriam = new HashMap<String, Integer>();
		this.metabolites_id_map = new HashMap<String, Integer>();
		this.synonyms = new HashSet<String>();

		try {

			ResultSet rs = this.statement.executeQuery("SELECT * FROM metabolites");

			while(rs.next()) {

				if(!rs.getString(3).equals("null")) {

					this.keggMiriam.put(rs.getString(3), rs.getInt(1));

				}

				if(!rs.getString(5).equals("null")) {

					this.chebiMiriam.put(rs.getString(5), rs.getInt(1));
				}

				this.metabolites_id_map.put(rs.getString(2).toLowerCase(), rs.getInt(1));
			}

			rs = this.statement.executeQuery("SELECT name, metabolite_id FROM synonyms;");

			while(rs.next()) {

				this.metabolites_id_map.put(rs.getString(1).toLowerCase(), rs.getInt(2));
				this.synonyms.add(rs.getString(1).toLowerCase());
			}

		}
		catch (SQLException e) {e.printStackTrace();}
	}


	/**
	 * @param locus_tag
	 * @return
	 */
	public String loadGene(String locus_tag, int project_id) {

		try {

			String result;
			ResultSet rs = this.statement.executeQuery("SELECT id FROM genes WHERE locus_tag='"+locus_tag+"' " +
					"AND status='"+DatabaseProgressStatus.PROCESSED+"' AND project_id = "+project_id+";");

			if(!rs.next()) {

				this.statement.clearWarnings();
				this.statement.execute("INSERT INTO genes (project_id, locus_tag, status) VALUES("+project_id+",'"+locus_tag+"', '"+DatabaseProgressStatus.PROCESSING+"')");
				rs = this.statement.executeQuery("SELECT LAST_INSERT_ID()");
				rs.next();
			}
			result=rs.getString(1);
			rs.close();

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

			ResultSet rs = this.statement.executeQuery("SELECT DISTINCT(uniprot_id) FROM tcdb_registries;");

			while(rs.next()) {

				uniprot_ids.add(rs.getString(1));
			}
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
	private Set<Integer> getTransportSystems(String uniprot_id, String tc_number) throws SQLException {

		Set<Integer> loadedTransportSystemIds = new TreeSet<Integer>(); 

		ResultSet rs = this.statement.executeQuery("SELECT transport_system_id FROM tcdb_registries " +
				" INNER JOIN tc_numbers_has_transport_systems " +
				" ON (tc_numbers_has_transport_systems.tc_version = tcdb_registries.tc_version " +
				"AND tc_numbers_has_transport_systems.tc_number = tcdb_registries.tc_number)" +
				" WHERE uniprot_id='"+uniprot_id+"' AND tcdb_registries.tc_number='"+tc_number+"' AND latest_version");

		while (rs.next())
			loadedTransportSystemIds.add(rs.getInt(1));

		rs.close();

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
	private int getTC_version(String tc_number, String uniprot_id) throws SQLException {

		int tc_version = -1;

//		ResultSet rs = this.statement.executeQuery("SELECT MAX(tc_version) " +
//				" FROM tc_numbers WHERE tc_number='"+tc_number+"';");
		
		ResultSet rs = this.statement.executeQuery("SELECT tc_version " +
				" FROM tcdb_registries WHERE tc_number='"+tc_number+"' AND uniprot_id = '"+uniprot_id+"' AND latest_version;");
		
		if(rs.next())
			if(rs.getInt(1)>0)
				tc_version = rs.getInt(1);
		
		return tc_version;
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
	private int addTC_number(String tc_number, int tc_version, String tc_family, String tc_location, String affinity, int taxonomy_data_id, int general_equation_id) throws SQLException {

		ResultSet rs = this.statement.executeQuery("SELECT * FROM tc_numbers WHERE tc_number='"+tc_number+"' AND tc_version = "+tc_version+";");

		if(!rs.next())
			this.statement.execute("INSERT INTO tc_numbers (tc_number, tc_version, tc_family, tc_location, affinity, taxonomy_data_id, general_equation_id)" +
				" VALUES('"+tc_number+"', "+tc_version+", '"+tc_family+"','"+tc_location+"'," + "'"+affinity+"', "+taxonomy_data_id+", "+general_equation_id+")");

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
	private void add_tcdb_registry(String uniprot_id, String tc_number, int tc_version, DatabaseProgressStatus processing) throws SQLException {

		int currentVersion = 1;

		ResultSet rs = this.statement.executeQuery("SELECT MAX(version) " +
				" FROM tcdb_registries WHERE uniprot_id='"+uniprot_id+"';");

		if(rs.next())
			if(rs.getInt(1)>0)
				currentVersion = currentVersion + rs.getInt(1);
		rs.close();

		this.statement.execute("UPDATE tcdb_registries SET latest_version = false, loaded_at=loaded_at " +
				" WHERE uniprot_id = '"+uniprot_id+"' AND tc_number = '"+tc_number+"';");

		this.statement.execute("INSERT INTO tcdb_registries (uniprot_id, version, tc_number, tc_version, status, latest_version) " +
				" VALUES('"+uniprot_id+"', "+currentVersion+", '"+tc_number+"', "+tc_version+", '"+processing+"', true)");

	}


	/**
	 * @param tc_number
	 * @param uniprot
	 * @return
	 * @throws SQLException
	 */
	private int updateTC_version(String tc_number, String uniprot) throws SQLException {

		int tc_version = -1;

		ResultSet rs = this.statement.executeQuery("SELECT MAX(tc_version), taxonomy_data_id, tc_family, tc_location, affinity, general_equation_id FROM tc_numbers WHERE tc_number='"+tc_number+"';");

		String tc_family, tc_location, affinity;
		int taxonomy_data_id, general_equation_id;

		if(rs.next() && rs.getInt(1)>0) {

			tc_version = 1 + rs.getInt(1);
			taxonomy_data_id = rs.getInt(2);
			tc_family = rs.getString(3);
			tc_location = rs.getString(4);
			affinity = rs.getString(5);
			general_equation_id = rs.getInt(6);

			this.addTC_number(tc_number, tc_version, tc_family, tc_location, affinity, taxonomy_data_id, general_equation_id);
			//this.updateUniprotRegistries(tc_number, tc_version);
			System.out.println("updating annotations for "+uniprot);
			this.add_tcdb_registry(uniprot, tc_number, tc_version, DatabaseProgressStatus.PROCESSED);
			
		}
		else {

			throw new SQLException(" No TC number available!");
		}
		rs.close();

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
	public void updateUniprotRegistries(String tc_number, int tc_version) throws SQLException {

		Set<String> uniprot_ids = new HashSet<String>();
		ResultSet rs = this.statement.executeQuery("SELECT uniprot_id FROM tcdb_registries WHERE tc_number='"+tc_number+"' AND latest_version");

		while(rs.next())
			uniprot_ids.add(rs.getString(1));
		rs.close();

		if(uniprot_ids.size()>0) {

			System.out.println("updating annotations for "+uniprot_ids);

			for(String uniprot : uniprot_ids)
				this.add_tcdb_registry(uniprot, tc_number, tc_version, DatabaseProgressStatus.PROCESSED);
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

				ResultSet rs = this.statement.executeQuery("SELECT id FROM taxonomy_data WHERE organism='"+organism+"';");
				if(rs.next()) {

					result = rs.getInt(1);

					return result;
				}
				this.statement.execute("INSERT INTO taxonomy_data (organism,taxonomy)" +
						" VALUES('"+organism+"','"+taxonomy+"')");
				rs = this.statement.executeQuery("SELECT LAST_INSERT_ID()");
				rs.next();

				result = rs.getInt(1);
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

				if(this.uniprot_latest_version.containsKey(alignmentResult.getUniprot_id().toUpperCase()))
					this.statement.execute("INSERT INTO genes_has_tcdb_registries (gene_id, version, uniprot_id, similarity)" +
							" VALUES('"+genes_id+"', "+this.uniprot_latest_version.get(alignmentResult.getUniprot_id().toUpperCase())+", '"+alignmentResult.getUniprot_id()+"','"+alignmentResult.getSimilarity()+"')");
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
	public void load_genes_has_metabolites(String gene_id, String metabolites_id, double similarity_score, double taxonomy_score) {

		try {

			ResultSet rs = this.statement.executeQuery("SELECT similarity_score_sum, taxonomy_score_sum, frequency FROM genes_has_metabolites" +
					" WHERE gene_id='"+gene_id+"' AND metabolite_id='"+metabolites_id+"';");

			if(rs.next()) {

				similarity_score += rs.getDouble(1);
				taxonomy_score += rs.getDouble(2);
				int frequency = rs.getInt(3)+1;
				this.statement.execute("UPDATE genes_has_metabolites SET similarity_score_sum='"+similarity_score+"'" +
						", taxonomy_score_sum='"+taxonomy_score+"' " +
						", frequency='"+frequency+"' " +
						"WHERE metabolite_id='"+metabolites_id+"' AND gene_id='"+gene_id+"'");
			}
			else {

				this.statement.execute("INSERT INTO genes_has_metabolites (gene_id, metabolite_id, similarity_score_sum, taxonomy_score_sum, frequency)" +
						" VALUES('"+gene_id+"','"+metabolites_id+"','"+similarity_score+"','"+taxonomy_score+"','1')");
			}

		}
		catch (SQLException e) {

			System.err.println("Gene id "+gene_id);
			System.err.println("Metabolites id "+metabolites_id);
			e.printStackTrace();}
	}

	/**
	 * @param genes_id
	 * @param metabolites_id
	 * @param type_id
	 * @param score
	 */
	public void load_genes_has_metabolites_has_type(String genes_id, String metabolites_id, String type_id, double transport_type_score_sum, double taxonomy_score) {

		try {

			ResultSet rs = this.statement.executeQuery("SELECT transport_type_score_sum,taxonomy_score_sum,frequency FROM genes_has_metabolites_has_type" +
					" WHERE gene_id='"+genes_id+"' AND transport_type_id='"+type_id+"' AND metabolite_id='"+metabolites_id+"';");

			if(rs.next()) {

				transport_type_score_sum += rs.getDouble(1);
				taxonomy_score += rs.getDouble(2);
				int frequency = rs.getInt(3)+1;

				this.statement.execute("UPDATE genes_has_metabolites_has_type SET transport_type_score_sum='"+transport_type_score_sum+"'" +
						", taxonomy_score_sum='"+taxonomy_score+"' " +
						", frequency='"+frequency+"' " +
						"WHERE gene_id='"+genes_id+"' AND metabolite_id='"+metabolites_id+"' AND transport_type_id='"+type_id+"'" );
			}
			else {

				this.statement.execute("INSERT INTO genes_has_metabolites_has_type (gene_id, metabolite_id, transport_type_id, transport_type_score_sum, taxonomy_score_sum,frequency)" +
						" VALUES('"+genes_id+"','"+metabolites_id+"','"+type_id+"','"+transport_type_score_sum+"','"+taxonomy_score+"','1')");
			}

		}
		catch (SQLException e) {e.printStackTrace();}
	}

	/**
	 * @param tcnumber_id
	 * @param transport_system_id
	 */
	public void load_tc_number_has_transport_system(String tcnumber_id, int transport_system_id, int tc_version) {

		try {

			ResultSet rs = this.statement.executeQuery("SELECT * FROM tc_numbers_has_transport_systems" +
					" WHERE tc_number='"+tcnumber_id+"' AND transport_system_id="+transport_system_id+" AND tc_version = "+tc_version+";");

			if(!rs.next())
				this.statement.execute("INSERT INTO tc_numbers_has_transport_systems (tc_number, tc_version, transport_system_id)" +
						" VALUES('"+tcnumber_id+"', "+tc_version+", "+transport_system_id+")");
		}
		catch (SQLException e) {

			System.out.println("tc_number\t"+tcnumber_id);
			System.out.println("transport_system_id\t"+transport_system_id);

			e.printStackTrace();
		}
	}


	/**
	 * @param transport_type_id
	 * @return
	 */
	public int load_transport_system(int transport_type_id, boolean reversibility) {

		try {

			int result;
			this.statement.execute("INSERT INTO transport_systems (transport_type_id, reversible) VALUES("+transport_type_id+","+reversibility+")");
			ResultSet rs = this.statement.executeQuery("SELECT LAST_INSERT_ID()");
			rs.next();
			result=rs.getInt(1);
			rs.close();

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
	public void load_transported_metabolites_direction(int metabolites_id, String direction_id, int transport_system_id, double metaboliteStoichiometry) {

		try {

			ResultSet rs = this.statement.executeQuery("SELECT stoichiometry FROM transported_metabolites_directions " +
					"WHERE metabolite_id='"+metabolites_id+"' " +
					"AND transport_system_id='"+transport_system_id+"' " +
					"AND direction_id='"+direction_id+"';");

			if(rs.next()) {

				System.err.println("WRONG STOICHIOMETRIES for transport system "+transport_system_id);
				//System.out.println("UPDATE transported_metabolites_directions SET stoichiometry ="+metaboliteStoichiometry+" " +
				//	"WHERE metabolites_id = '"+metabolites_id+"' AND transport_system_id='"+transport_system_id+"' AND direction_id='"+direction_id+"'");
			}
			else {

				this.statement.execute("INSERT INTO transported_metabolites_directions (metabolite_id,transport_system_id,direction_id, stoichiometry)" +
						" VALUES('"+metabolites_id+"','"+transport_system_id+"','"+direction_id+"',"+metaboliteStoichiometry+")");
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

			String result;
			ResultSet rs = this.statement.executeQuery("SELECT id FROM directions WHERE direction='"+direction+"';");

			if(rs.next()) { 

				result=rs.getString(1);
				this.directionMap.put(direction, result);
				return result;
			}

			this.statement.execute("INSERT INTO directions (direction) VALUES('"+direction+"')");
			rs = this.statement.executeQuery("SELECT LAST_INSERT_ID()");
			rs.next();
			result=rs.getString(1);
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

			int result;
			ResultSet rs = this.statement.executeQuery("SELECT id FROM transport_types WHERE name='"+transportType+"' AND directions='"+directions+"';");

			if(rs.next()) {

				result=rs.getInt(1); 
			}
			else {

				this.statement.execute("INSERT INTO transport_types	 (name,directions) VALUES('"+transportType+"','"+directions+"')");
				rs = this.statement.executeQuery("SELECT LAST_INSERT_ID()");
				rs.next();
				result=rs.getInt(1);
			}
			this.transportDirectionsMap.put(directions, result);

			rs.close();

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

		String name = metabolite.getName().toLowerCase();

		if(this.metabolites_id_map.containsKey(name)) {

			if(datatype.equals(DATATYPE.MANUAL))
				this.statement.execute("UPDATE metabolites SET datatype='"+DATATYPE.MANUAL+"' WHERE name='"+DatabaseUtilities.databaseStrConverter(name,this.databaseType)+"';");

			return this.metabolites_id_map.get(name);
		}

		if(kegg_name!= null && this.metabolites_id_map.containsKey(kegg_name.toLowerCase())) {

			kegg_name = kegg_name.toLowerCase();

			if(!this.synonyms.contains(name)) {

				if(chebi_name!=null) {

					this.statement.execute("UPDATE metabolites SET chebi_name = '"+DatabaseUtilities.databaseStrConverter(chebi_name,this.databaseType)+"' , chebi_miriam = '"+chebi+"' WHERE id="+this.metabolites_id_map.get(chebi_name));
				}
				this.statement.execute("INSERT INTO synonyms (metabolite_id, name, datatype) VALUES("+this.metabolites_id_map.get(kegg_name)+",'"+DatabaseUtilities.databaseStrConverter(name,this.databaseType)+"','"+datatype+"')");
				this.synonyms.add(name);
				this.metabolites_id_map.put(name, this.metabolites_id_map.get(kegg_name));
			}
			return this.metabolites_id_map.get(name);
		}

		if(chebi_name!=null && this.metabolites_id_map.containsKey(chebi_name.toLowerCase())) {

			chebi_name = chebi_name.toLowerCase();

			if(!this.synonyms.contains(name)) {

				if(kegg_name!=null)
					this.statement.execute("UPDATE metabolites SET kegg_name = '"+DatabaseUtilities.databaseStrConverter(kegg_name,this.databaseType)+"' , kegg_miriam = '"+kegg+"' WHERE id="+this.metabolites_id_map.get(chebi_name));

				this.statement.execute("INSERT INTO synonyms (metabolite_id, name, datatype) VALUES("+this.metabolites_id_map.get(chebi_name)+",'"+DatabaseUtilities.databaseStrConverter(name,this.databaseType)+"','"+datatype+"')");
				this.synonyms.add(name);
				this.metabolites_id_map.put(name, this.metabolites_id_map.get(chebi_name));
			}
			return this.metabolites_id_map.get(name);
		}



		if(name.matches("\\d{4,9}")) {

			if(kegg_name!=null) {

				name=kegg_name;
			}
			else if(chebi_name!=null) {

				name=chebi_name;
			}
		}

		int result=-1;
		ResultSet rs = this.statement.executeQuery("SELECT id, datatype FROM metabolites WHERE name='"+DatabaseUtilities.databaseStrConverter(name,this.databaseType)+"';");

		if(rs.next()) {

			result=rs.getInt(1); 

			if(rs.getString(2).equals(DATATYPE.AUTO.toString())) {

				if(datatype.equals(DATATYPE.MANUAL)) {

					this.statement.execute("UPDATE metabolites SET datatype='"+DATATYPE.MANUAL+"' WHERE name='"+DatabaseUtilities.databaseStrConverter(name,this.databaseType)+"';");
				}
			}
			rs.close();
			this.metabolites_id_map.put(name, result);
			return result;
		}

		result = this.existsSynonym(metabolite,datatype);

		if(result>0) {

			this.metabolites_id_map.put(name, result);
			return result;
		}

		if(kegg!=null && chebi!=null) {

			rs = this.statement.executeQuery("SELECT id FROM metabolites WHERE kegg_miriam='"+kegg+"' AND chebi_miriam='"+chebi+"';");

			if(rs.next()) {

				result=rs.getInt(1);
				rs.close();
				this.metabolites_id_map.put(name, result);
				return result;
			}
		}

		if(kegg!=null && chebi==null) {

			rs = this.statement.executeQuery("SELECT id FROM metabolites WHERE kegg_miriam='"+kegg+"';");

			if(rs.next()) {

				result=rs.getInt(1); 
				rs.close();
				this.metabolites_id_map.put(name, result);
				return result;
			}
		}

		if(chebi!=null && kegg==null) {

			rs = this.statement.executeQuery("SELECT id FROM metabolites WHERE chebi_miriam='"+chebi+"';");
			if(rs.next()) {

				result=rs.getInt(1);
				rs.close();
				this.metabolites_id_map.put(name, result);
				return result;
			}
		}

		//System.err.println("metabolites\t"+name+"\tdoes not exist! loading new metabolite");

		if(kegg_name!=null) {

			kegg_name=kegg_name.replace("'", "\\'");
		}

		if(chebi_name!=null) {

			chebi_name=chebi_name.replace("'", "\\'");
		}

		String kegg_formula="", chebi_formula="";

		if(kegg!=null) {

			kegg_formula = this.getKeggFormula(ExternalRefSource.KEGG_CPD.getSourceId(kegg), 0);
		}

		if(chebi!=null) {

			chebi_formula = this.getChebiFormula(ExternalRefSource.CHEBI.getSourceId(chebi), 0);
		}

		this.statement.execute("INSERT INTO metabolites (name,kegg_miriam,chebi_miriam,kegg_name,chebi_name,datatype,kegg_formula,chebi_formula) " +
				"VALUES('"+DatabaseUtilities.databaseStrConverter(name,this.databaseType).toLowerCase()+"','"+kegg+"','"+chebi+"','"+
				DatabaseUtilities.databaseStrConverter(kegg_name,this.databaseType)+"','"+
				DatabaseUtilities.databaseStrConverter(chebi_name,this.databaseType)+
				"','"+datatype+"','"+kegg_formula+"','"+chebi_formula+"')");
		rs = this.statement.executeQuery("SELECT LAST_INSERT_ID()");
		rs.next();
		result =  rs.getInt(1);
		rs.close();

		if(kegg!=null) {

			this.keggMiriam.put(kegg, result);
		}

		if(chebi!=null) {

			this.chebiMiriam.put(chebi, result);

			String metaboliteChebiID = ExternalRefSource.CHEBI.getSourceId(chebi);

			// not CoA childs
			if(metaboliteChebiID!=null && !chebi.equalsIgnoreCase("urn:miriam:obo.chebi:CHEBI:15346"))  {

				Map<String, ChebiER> chebi_entity = MIRIAM_Data.get_chebi_miriam_child_metabolites(metaboliteChebiID);

				if(chebi_entity!=null) {

					this.load_metabolites_ontology(metaboliteChebiID, result, chebi_entity,0);
				}
			}
		}
		this.metabolites_id_map.put(name, result);

		return result;
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
			{
				return chebiER.getFormula();
			}
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
		try 
		{
			KeggCompoundER met = KeggAPI.getCompoundByKeggId(keggID);
			if(met!=null)
			{
				return met.getFormula();
			}
			return "";
		} 
		catch (WebServiceException e) {
			if(counter<10)
			{
				counter = counter+1;
				return this.getKeggFormula(keggID, counter);
			}
			e.printStackTrace();
			return "";

		} catch (Exception e) {

			if(counter<10)
			{
				counter = counter+1;
				return this.getKeggFormula(keggID, counter);
			}
			else {

				System.out.println("No KEgg  formula "+ keggID);
			}
			e.printStackTrace();
			return "";
		}
	}

	/**
	 * @param name
	 * @param miriam
	 * @return
	 * @throws SQLException 
	 */
	private int existsSynonym(TransportMetaboliteDirectionStoichiometryContainer metabolite, DATATYPE datatype) throws SQLException{

		int metabolite_id = -1;

		if(metabolite.getKegg_miriam()!=null) {

			if(this.keggMiriam.containsKey(metabolite.getKegg_miriam())) {

				metabolite_id = this.keggMiriam.get(metabolite.getKegg_miriam());
			}
		}

		if(metabolite.getChebi_miriam()!=null) {

			if(this.chebiMiriam.containsKey(metabolite.getChebi_miriam())) {

				metabolite_id = this.chebiMiriam.get(metabolite.getChebi_miriam());
			}
		}

		if(metabolite_id > 0) {

			ResultSet rs = this.statement.executeQuery("SELECT * FROM synonyms WHERE metabolite_id="+metabolite_id+" AND name='"+DatabaseUtilities.databaseStrConverter(metabolite.getName(),this.databaseType).toLowerCase()+"';");

			if(!rs.next()) {

				this.statement.execute("INSERT INTO synonyms (metabolite_id, name, datatype) VALUES("+metabolite_id+",'"+DatabaseUtilities.databaseStrConverter(metabolite.getName(),this.databaseType).toLowerCase()+"','"+datatype+"')");
				rs.close();
			}

			if(datatype.equals(DATATYPE.MANUAL)) {

				this.statement.execute("UPDATE metabolites SET datatype='"+DATATYPE.MANUAL+"' WHERE metabolites.id="+metabolite_id);
			}
		}
		return metabolite_id;
	}


	/**
	 * @param metabolite
	 * @return
	 */
	public int getMetaboliteID(String metabolite) {

		try {

			int result = -1;

			ResultSet rs = this.statement.executeQuery("SELECT id FROM metabolites WHERE name='"+DatabaseUtilities.databaseStrConverter(metabolite,this.databaseType)+"';");

			if(rs.next()) {

				result=rs.getInt(1);
			}
			else {

				rs = this.statement.executeQuery("SELECT metabolite_id FROM synonyms WHERE name='"+DatabaseUtilities.databaseStrConverter(metabolite,this.databaseType)+"';");
				rs.next();
				result=rs.getInt(1);
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
	public Set<String> getTransportTypeID(String uniprot_id) {

		try {
			
			Set<String> result = new TreeSet<String>();

			ResultSet rs = this.statement.executeQuery("SELECT transport_types.id FROM transport_types " +
					"INNER JOIN transport_systems ON transport_types.id = transport_type_id " +
					"INNER JOIN tc_numbers_has_transport_systems ON transport_systems.id = tc_numbers_has_transport_systems.transport_system_id " +
					"INNER JOIN tcdb_registries ON (tcdb_registries.tc_number = tc_numbers_has_transport_systems.tc_number AND tcdb_registries.tc_version = tc_numbers_has_transport_systems.tc_version)" +
					"WHERE uniprot_id='"+uniprot_id+"' AND latest_version");

			while(rs.next())
				result.add(rs.getString(1));
			
			return result;
		}
		catch (SQLException e) {e.printStackTrace();}
		return null;
	}

	/**
	 * @param uniprot_id
	 * @return
	 */
	public Set<String> getMetabolitesID(String uniprot_id){

		Set<String> result = new TreeSet<String>();

		try {

			ResultSet rs = this.statement.executeQuery("SELECT metabolite_id FROM transported_metabolites_directions " +
					"INNER JOIN tc_numbers_has_transport_systems ON transported_metabolites_directions.transport_system_id = tc_numbers_has_transport_systems.transport_system_id " +
					"INNER JOIN tcdb_registries ON (tcdb_registries.tc_number = tc_numbers_has_transport_systems.tc_number AND tcdb_registries.tc_version = tc_numbers_has_transport_systems.tc_version)" +
					"WHERE uniprot_id='"+uniprot_id+"' AND latest_version");

			while(rs.next())
				result.add(rs.getString(1));
			
			return result;
		}
		catch (SQLException e) {e.printStackTrace();}
		return null;
	}

	/**
	 * @param metabolites_id
	 * @return
	 */
	public Set<String> getTransportTypesID(String metabolites_id){
		Set<String> result = new TreeSet<String>();
		try
		{

			ResultSet rs = this.statement.executeQuery("SELECT transport_type.id FROM transported_metabolites_direction " +
					"INNER JOIN transport_system ON transport_system_id=transport_system.id " +
					"INNER JOIN transport_type ON transport_type_id=transport_type.id " +
					"WHERE metabolites_id='"+metabolites_id+"'");

			while(rs.next())
			{
				result.add(rs.getString(1));
			}

			return result;
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

			for(int transport_system_id: transporterIds) {

				ResultSet rs = this.statement.executeQuery("SELECT reversible FROM transport_systems WHERE id = "+transport_system_id);
				rs.next();
				boolean reversibility = rs.getBoolean(1);
				rs.close();

				TransportSystemContainer ts = new TransportSystemContainer(transport_system_id, reversibility);
				List<TransportMetaboliteDirectionStoichiometryContainer> metabolites_data= new ArrayList<TransportMetaboliteDirectionStoichiometryContainer>();
				Map<String, Integer> metabolite_name_index = new HashMap<>();
				int counter=0;

				rs = this.statement.executeQuery("SELECT metabolites.name, direction, stoichiometry, reversible, kegg_name, chebi_name, synonyms.name FROM transported_metabolites_directions " +
						"INNER JOIN metabolites ON metabolites.id = transported_metabolites_directions.metabolite_id " +
						"INNER JOIN synonyms ON metabolites.id = synonyms.metabolite_id " +
						"INNER JOIN directions ON directions.id = direction_id " +
						"INNER JOIN transport_systems ON transport_systems.id = transport_system_id " +
						"WHERE transport_system_id ="+transport_system_id);

				while(rs.next()) {

					TransportMetaboliteDirectionStoichiometryContainer tmds = new TransportMetaboliteDirectionStoichiometryContainer(rs.getString(1));
					tmds.setDirection(rs.getString(2));
					tmds.setStoichiometry(rs.getDouble(3));
					tmds.setReversible(rs.getBoolean(4));
					tmds.setKegg_name(rs.getString(5));
					tmds.setChebi_name(rs.getString(6));

					if(metabolite_name_index.containsKey(rs.getString(1))) {

						Set<String> synonyms = metabolites_data.get(metabolite_name_index.get(rs.getString(1))).getSynonyms();
						synonyms.add(rs.getString(7));
						metabolites_data.get(metabolite_name_index.get(rs.getString(1))).setSynonyms(synonyms);
					}
					else {

						Set<String> synonyms = new HashSet<String>();
						synonyms.add(rs.getString(7));
						tmds.setSynonyms(synonyms);
						metabolites_data.add(counter, tmds);
						
						metabolite_name_index.put(rs.getString(1), counter);
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
	public Set<Integer> get_transporter_ids(String metabolites_name, int type_id) {

		Set<Integer> result = new TreeSet<Integer>();

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

			metabolites_name = DatabaseUtilities.databaseStrConverter(metabolites_name,this.databaseType);
			
			ResultSet rs = this.statement.executeQuery("SELECT transport_systems.id FROM transport_systems " +
					" INNER JOIN transported_metabolites_directions ON (transport_systems.id = transport_system_id ) " +
					" INNER JOIN metabolites ON metabolites.id= transported_metabolites_directions.metabolite_id " +
					" INNER JOIN synonyms ON transported_metabolites_directions.metabolite_id= synonyms.metabolite_id " +
					" INNER JOIN directions on transported_metabolites_directions.direction_id=directions.id " +
					" WHERE (" +
					" UPPER(metabolites.name) = UPPER('"+metabolites_name+"') OR " +
					" UPPER(synonyms.name) = UPPER('"+metabolites_name+"') OR " +
					" UPPER(kegg_name) = UPPER('"+metabolites_name+"') OR " +
					" UPPER(chebi_name) = UPPER('"+metabolites_name+"')" +
					")" +
					" AND direction <> 'reactant' " +
					" AND direction <> 'product' " +
					" AND transport_type_id = "+type_id );
			
			while(rs.next())
				result.add(rs.getInt(1));

			return result;
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
	public void load_metabolites_ontology(String metabolite_chebi_id, int metabolite_id, Map<String, ChebiER> chebi_entity_map, int counter) {

		try {

			ResultSet rs;

			this.local_database_id.put(metabolite_chebi_id, metabolite_id);

			for(String key:chebi_entity_map.keySet()) {

				ChebiER chebi_entity = chebi_entity_map.get(key);

				if(chebi_entity.getFunctional_children().size()>0) {

					if(!this.local_database_id.containsKey(key)) {

						int id = get_metabolite_id(key, this.statement);
						if(id>0) {

							this.local_database_id.put(key, id);
						}
					}

					for(String child:chebi_entity.getFunctional_children()) {

						if(!this.local_database_id.containsKey(child)) {

							int id = get_metabolite_id(child, this.statement);
							if(id>0) {

								this.local_database_id.put(child, id);
							}

						}

						if(this.local_database_id.get(key) != 0 &&  this.local_database_id.containsKey(child) &&
								this.local_database_id.get(child) > 0 && this.local_database_id.get(child) != 0 &&
								this.local_database_id.get(key) != this.local_database_id.get(child)) {

							rs = this.statement.executeQuery("SELECT id FROM metabolites_ontology " +
									"WHERE metabolite_id="+local_database_id.get(key)+" AND " +
									"child_id="+local_database_id.get(child)+"");

							if(!rs.next()) {

								//avoid loops
								rs = this.statement.executeQuery("SELECT id FROM metabolites_ontology WHERE metabolite_id="+local_database_id.get(child)+" AND child_id="+local_database_id.get(key)+"");

								if(!rs.next()) {

									this.statement.execute("INSERT INTO metabolites_ontology (metabolite_id, child_id) VALUES("+local_database_id.get(key)+","+local_database_id.get(child)+")");
								}
							}
						}
					}
				}
			}

		}
		catch (Exception e) {

			counter= counter+1;

			if(counter<30){

				this.load_metabolites_ontology(metabolite_chebi_id, metabolite_id, chebi_entity_map, counter);
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
	private int get_metabolite_id(String metabolite_external_id, Statement statement) throws SQLException{

		ResultSet rs = this.statement.executeQuery("SELECT id FROM metabolites WHERE chebi_miriam='"+ExternalRefSource.CHEBI.getMiriamCode(metabolite_external_id)+"'");

		if(rs.next()) {

			return rs.getInt(1);
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

			int result;

			ResultSet rs = this.statement.executeQuery("SELECT id FROM general_equation WHERE equation='"+DatabaseUtilities.databaseStrConverter(equation,this.databaseType)+"'");

			if(rs.next()) {

				result=rs.getInt(1);
			}
			else {

				this.statement.execute("INSERT INTO general_equation (equation) VALUES('"+DatabaseUtilities.databaseStrConverter(equation,this.databaseType)+"')");
				rs = this.statement.executeQuery("SELECT LAST_INSERT_ID()");
				rs.next();
				result=rs.getInt(1);
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

			this.statement.execute("UPDATE tcdb_registries SET status='"+DatabaseProgressStatus.PROCESSED+"' WHERE uniprot_id = '" +uniprot_id+"'");
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

			this.statement.execute("UPDATE genes SET status='"+DatabaseProgressStatus.PROCESSED+"' WHERE locus_tag = '" +locusTag+"'");
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

		ResultSet rs;
		try {

			this.deleteProcessingGenes();

			rs = this.statement.executeQuery("SELECT locus_tag FROM genes WHERE status='"+DatabaseProgressStatus.PROCESSED+"'");

			while(rs.next()) {

				result.add(rs.getString(1));
			}

			rs.close();
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

		Set<String> result = new HashSet<String>();

		ResultSet rs;

		this.deleteProcessingRegistries();

		rs = this.statement.executeQuery("SELECT uniprot_id, tc_number  FROM tcdb_registries " +
				"WHERE status='"+DatabaseProgressStatus.PROCESSED+"'");

		while(rs.next())
			result.add(rs.getString(1)+"__"+rs.getString(2));

		rs.close();
		return result;
	}

	/**
	 * @param genomeID
	 * @throws SQLException 
	 */
	public int createNewProject(int genome_id) throws SQLException {

		int version = 1;
		ResultSet rs = this.statement.executeQuery("SELECT id, version FROM projects WHERE latest_version AND organism_id="+genome_id+";");

		if(rs.next()) {

			version = rs.getInt(2)+1;
			this.statement.execute("UPDATE projects SET latest_version=false WHERE id= "+rs.getString(1));
		}
		rs.close();

		java.sql.Date sqlToday = new java.sql.Date((new java.util.Date()).getTime());

		this.statement.execute("INSERT INTO projects (organism_id, latest_version, date, version) VALUES ("+genome_id+", true, '"+sqlToday+"', "+version+")");
		rs = this.statement.executeQuery("SELECT LAST_INSERT_ID()");
		rs.next();

		int projectID = rs.getInt(1);
		rs.close();

		return projectID;
	}

	/**
	 * @param project_id
	 * @param version
	 * @throws SQLException 
	 */
	public void deleteProject(int project_id, int version) throws SQLException {

		if(version <0) {

			this.statement.execute("DELETE FROM projects WHERE id = "+project_id);
		}
		else if(version == 0) {

			this.statement.execute("DELETE FROM projects WHERE id = "+project_id+" AND latest_version");
		}
		else {

			this.statement.execute("DELETE FROM projects WHERE id = "+project_id+ " AND version = "+version);
		}
	}

	/**
	 * @param project_id
	 * @param version
	 * @throws SQLException 
	 */
	public void deleteGenesFromProject(int project_id) throws SQLException {

		this.statement.execute("DELETE FROM genes WHERE project_id = "+project_id);
	}


	/**
	 * @param reaction
	 * @param project_id
	 * @return
	 * @throws Exception
	 */
	public TracebackAnnotations tracebackReactionAnnotation(TransportReactionCI reaction, int project_id) throws Exception {

		Map<String, Map<String, Double>> geneProtein = new HashMap<String, Map<String, Double>>();
		Map<String, String[]> protein_tcnumber = new HashMap<String, String[]>();
		Map<String, Set<String>> protein_metabolites = new HashMap<String, Set<String>>();

		for(String locus_tag : reaction.getGenesIDs()) {

			ResultSet rs;
			try {

				String query = "SELECT genes_has_tcdb_registries.uniprot_id, tc_numbers_has_transport_systems.tc_number, metabolites.name, similarity, equation "+
								"FROM genes "+
								"INNER JOIN genes_has_tcdb_registries ON gene_id = genes.id "+
								"INNER JOIN tcdb_registries ON genes_has_tcdb_registries.uniprot_id = tcdb_registries.uniprot_id AND genes_has_tcdb_registries.version = tcdb_registries.version "+
								"INNER JOIN tc_numbers_has_transport_systems ON tcdb_registries.tc_number = tc_numbers_has_transport_systems.tc_number AND tcdb_registries.tc_version = tc_numbers_has_transport_systems.tc_version "+
								"INNER JOIN tc_numbers ON tcdb_registries.tc_number = tc_numbers.tc_number AND tcdb_registries.tc_version = tc_numbers.tc_version "+
								"INNER JOIN general_equation ON tc_numbers.general_equation_id = general_equation.id "+
								"INNER JOIN transported_metabolites_directions ON transported_metabolites_directions.transport_system_id = tc_numbers_has_transport_systems.transport_system_id "+
								"INNER JOIN metabolites ON metabolite_id = metabolites.id "+
								"WHERE project_id = "+project_id+" AND locus_tag = '"+locus_tag+"';";
				
				
				rs = this.statement.executeQuery(query);

				System.out.println(query);
				
				while(rs.next()) {

					Map<String, Double> proteins = new HashMap<String, Double>();
					if(geneProtein.containsKey(locus_tag)) {

						proteins = geneProtein.get(locus_tag);
					}
					proteins.put(rs.getString(1), rs.getDouble(4));

					geneProtein.put(locus_tag, proteins);

					if(!protein_tcnumber.containsKey(rs.getString(1))) {

						protein_tcnumber.put(rs.getString(1), new String[] {rs.getString(2), rs.getString(5)});
					}

					Set<String> metabolites = new HashSet<String>();
					if(protein_metabolites.containsKey(rs.getString(1))) {

						metabolites = protein_metabolites.get(rs.getString(1));
					}
					metabolites.add(rs.getString(3));
					protein_metabolites.put(rs.getString(1), metabolites);
				}
				rs.close();

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
	public int getProjectID(int genome_id) throws SQLException {

		int project_id;

		ResultSet rs = this.statement.executeQuery("SELECT id FROM projects WHERE latest_version AND organism_id="+genome_id+";");

		if(rs.next()) {

			project_id = rs.getInt(1);
		}
		else {

			project_id = this.createNewProject(genome_id);			
		}
		rs.close();

		return project_id;
	}

	/**
	 * @param genome_id
	 * @return
	 * @throws SQLException 
	 */
	public Set<Integer> getAllProjectIDs(int genome_id) throws SQLException {

		Set<Integer> project_ids = new TreeSet<Integer>();

		ResultSet rs = this.statement.executeQuery("SELECT id FROM projects WHERE organism_id="+genome_id+";");

		while(rs.next()) {

			project_ids.add(rs.getInt(1));
		}

		rs.close();

		return project_ids;
	}


	/**
	 * @param gene_id
	 * @return
	 */
	public boolean geneIsNotProcessed(String gene_id) {

		boolean result = true;

		try {

			ResultSet rs = this.statement.executeQuery("SELECT status FROM genes WHERE id = "+gene_id+";");

			if(rs.next()) {

				if(rs.getString(1).equalsIgnoreCase(DatabaseProgressStatus.PROCESSING.toString())) {


				}
				result = true;
				rs.close();
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

			this.statement.execute("DELETE FROM genes WHERE status='"+DatabaseProgressStatus.PROCESSING+"'");
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

			this.statement.execute("DELETE FROM tcdb_registries WHERE status='"+DatabaseProgressStatus.PROCESSING+"'");
		} 
		catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void getUniprotVersions() {

		this.genes_uniprot = new HashMap<String, Set<String>>();
		this.uniprot_latest_version = new HashMap<String, Integer>();

		ResultSet rs;

		try {

			rs = this.statement.executeQuery("SELECT gene_id, uniprot_id FROM genes_has_tcdb_registries;");

			while(rs.next()) {

				Set<String> uni = new HashSet<String>();

				if(this.genes_uniprot.containsKey(rs.getString(1))) {

					uni = this.genes_uniprot.get(rs.getString(1));
				}

				uni.add(rs.getString(2));

				this.genes_uniprot.put(rs.getString(1), uni);
			}

			rs = this.statement.executeQuery("SELECT uniprot_id, version FROM tcdb_registries WHERE latest_version;");


			while(rs.next()) {

				this.uniprot_latest_version.put(rs.getString(1).toUpperCase(), rs.getInt(2));
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

		ResultSet rs = this.statement.executeQuery("SELECT uniprot_id, organism, taxonomy FROM tcdb_registries " +
				" INNER JOIN tc_numbers ON (tcdb_registries.tc_number = tc_numbers.tc_number AND tcdb_registries.tc_version = tc_numbers.tc_version)" +
				" INNER JOIN taxonomy_data ON (taxonomy_data.id = taxonomy_data_id)" +
				" WHERE latest_version;");


		while (rs.next()) {

			TaxonomyContainer container = new TaxonomyContainer(rs.getString(2));

			List<NcbiTaxon> list = new ArrayList<NcbiTaxon>();

			String taxonomy = rs.getString(3).replace("[", "").replace("]", "");

			StringTokenizer st = new StringTokenizer(taxonomy,",");

			while(st.hasMoreTokens()) {

				MyNcbiTaxon ncbiTaxon = new MyNcbiTaxon(st.nextToken().trim());
				list.add(ncbiTaxon);
			}

			container.setTaxonomy(list);
			result.put(rs.getString(1), container);
		}
		return result;
	}
}
