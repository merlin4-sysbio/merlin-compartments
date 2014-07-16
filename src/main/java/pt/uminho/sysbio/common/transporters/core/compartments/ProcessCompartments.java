package pt.uminho.sysbio.common.transporters.core.compartments;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pt.uminho.sysbio.common.bioapis.externalAPI.ncbi.NcbiServiceStub_API.KINGDOM;
import pt.uminho.sysbio.common.database.connector.datatypes.Connection;

/**
 * @author ODias
 *
 */
public class ProcessCompartments {

	private KINGDOM kingdom;
	private STAIN stain;
	private boolean hasCellWall=false;
	private String interiorCompartment;
	private Set<String> ignoreCompartmentsID;
	private boolean processCompartmentsInitiated = false;

	/**
	 * 
	 */
	public ProcessCompartments() {


	}


	/**
	 * @param existingCompartments
	 */
	public ProcessCompartments(Set<String> existingCompartments) {


		this.initProcessCompartments(existingCompartments);
	}

	/**
	 * @param existingCompartments
	 */
	public void initProcessCompartments(Set<String> existingCompartments) {

		if(!this.isProcessCompartmentsInitiated()) {
			
			this.ignoreCompartmentsID = new HashSet<String>();
			this.stain = STAIN.gram_positive;

			for(String compartment : existingCompartments) {
				
				if(compartment.equalsIgnoreCase("perip") || compartment.equalsIgnoreCase("periplasm") ) {

					this.kingdom = KINGDOM.Bacteria;
					this.stain = STAIN.gram_negative;
				}

				if(compartment.equalsIgnoreCase("cellw") || compartment.equalsIgnoreCase("cellwall") ) {

					this.setHasCellWall(true);
				}

				if(compartment.equalsIgnoreCase("pla") || compartment.equalsIgnoreCase("plas")) {

					this.kingdom = KINGDOM.Eukaryota;
					this.stain = null;
				}
			}

			this.setProcessCompartmentsInitiated(true);
		}
	}

	/**
	 * @return
	 * @throws SQLException 
	 */
	public Map<String,String> getCompartmentAbbIdMap(Connection connection) throws SQLException {

		Map<String,String> idCompartmentAbbIdMap = new HashMap<String, String>();

		Statement stmt = connection.createStatement();

		ResultSet rs = stmt.executeQuery("SELECT idcompartment, abbreviation FROM compartment;");

		while(rs.next()) {

			idCompartmentAbbIdMap.put(rs.getString(2).toLowerCase(),rs.getString(1));
		}
		
		return idCompartmentAbbIdMap;
	}

	/**
	 * @param metaboliteMap
	 * @param compartmentID
	 * @return
	 * @throws Exception 
	 */
	public String processCompartment(String localisation, String compartmentID) throws Exception {

		if (this.isProcessCompartmentsInitiated()) {

			if(localisation.equalsIgnoreCase("out")) {

				if(compartmentID.equalsIgnoreCase("plas") || compartmentID.equalsIgnoreCase("outme") || compartmentID.equalsIgnoreCase("cellw")) {

					return ("extr".toUpperCase());
				}
				else if(compartmentID.equalsIgnoreCase("cytmem")) {

					if(this.stain.equals(STAIN.gram_negative)) {

						return ("perip".toUpperCase());
					}
					else {

						return ("extr".toUpperCase()); 
					}
				}
				else {

					return (interiorCompartment.toUpperCase());
				}
			}
			else {

				if(compartmentID.equalsIgnoreCase("plas") || compartmentID.equalsIgnoreCase("cytmem") || compartmentID.equalsIgnoreCase("cellw")) {

					return (interiorCompartment.toUpperCase());
				}
				else if (compartmentID.equalsIgnoreCase("outme")) {

					return ("perip".toUpperCase());
				}
				else {

					return (compartmentID.toUpperCase());
				}
			}
		}
		else {

			throw new Exception("Compartments processing not initiated!");
		}
	}

	/**
	 * @return
	 * @throws SQLException 
	 */
	public Map<String,String> getIdCompartmentAbbMap(Connection connection) throws SQLException {

		Map<String,String> idCompartmentMap = new HashMap<String, String>();

		Statement stmt = connection.createStatement();

		ResultSet rs = stmt.executeQuery("SELECT idcompartment, abbreviation FROM compartment;");

		while(rs.next()) {

			idCompartmentMap.put(rs.getString(1), rs.getString(2));

			if( rs.getString(2).equalsIgnoreCase("cyto")) {

				this.interiorCompartment = "cyto";
			}

			if( rs.getString(2).equalsIgnoreCase("cytop")) {

				this.interiorCompartment = "cytop";
			}
		}
		return idCompartmentMap;
	}


	/**
	 * @param compartment
	 * @throws SQLException
	 */
	public int getCompartmentID(String compartment, Connection connection) throws SQLException{

		Statement stmt = connection.createStatement();

		String abbreviation;
		if(compartment.length()>3) {

			abbreviation=compartment.substring(0,3).toUpperCase();
		}
		else {

			abbreviation=compartment.toUpperCase().concat("_");

			while(abbreviation.length()<4) {

				abbreviation=abbreviation.concat("_");
			}
		}

		ResultSet rs = stmt.executeQuery("SELECT idcompartment FROM compartment WHERE name ='"+compartment+"' AND abbreviation ='"+abbreviation+"'");

		if(!rs.next()) {

			stmt.execute("INSERT INTO compartment(name, abbreviation) VALUES('"+compartment+"','"+abbreviation+"')");
			rs = stmt.executeQuery("SELECT LAST_INSERT_ID()");
			rs.next();
		}
		int idCompartment = rs.getInt(1);

		return idCompartment;
	}

	/**
	 * @param list
	 * @param compartmentsAbb_ids
	 * @param idCompartmentAbbIdMap
	 * @param ignoreList
	 * @return
	 * @throws Exception 
	 */
	public Set<String> parseCompartments(List<String> list, Map<String,String> compartmentsAbb_ids, Map<String, String> idCompartmentAbbIdMap, List<String> ignoreList) throws Exception {

		if (this.isProcessCompartmentsInitiated()) {

			Set<String> compartments = new HashSet<String>();

			for(String compartment: list) {

				String abb = compartmentsAbb_ids.get(compartment);

				if(abb.equalsIgnoreCase("cytmem")) {

					compartments.add(idCompartmentAbbIdMap.get(this.interiorCompartment.toLowerCase()));

					if(this.stain.equals(STAIN.gram_negative)) {

						compartments.add(idCompartmentAbbIdMap.get("perip"));
					}
					else {

						if(!hasCellWall) {

							compartments.add(idCompartmentAbbIdMap.get("extr"));
						}	
					}
				}
				else if(abb.equalsIgnoreCase("cellw")) {

					compartments.add(idCompartmentAbbIdMap.get("extr"));
				}
				else if(abb.equalsIgnoreCase("outme")) {

					compartments.add(idCompartmentAbbIdMap.get("perip"));
					compartments.add(idCompartmentAbbIdMap.get("extr"));
				}
				else if(abb.equalsIgnoreCase("pla") || abb.equalsIgnoreCase("plas")) {

					compartments.add(idCompartmentAbbIdMap.get(this.interiorCompartment.toLowerCase()));
					compartments.add(idCompartmentAbbIdMap.get("extr"));
				}
				else if(abb.equalsIgnoreCase("unkn")) {

					compartments.add(idCompartmentAbbIdMap.get(this.interiorCompartment.toLowerCase()));
				} 
				else {

					compartments.add(compartment);
				}

				if(ignoreList.contains(abb.toLowerCase())) {

					compartments.add(idCompartmentAbbIdMap.get(this.interiorCompartment.toLowerCase()));
					this.ignoreCompartmentsID.add(compartment);
				} 
			}

			return compartments;
		}
		else {

			throw new Exception("Compartments processing not initiated!");
		}
	}

	/**
	 * @param compartmentID
	 * @return
	 * @throws Exception 
	 */
	public String getOutside(String compartmentID) throws Exception {

		if(this.isProcessCompartmentsInitiated()) {

			if(compartmentID.equalsIgnoreCase("extr")) {

				return "";
			}
			if(compartmentID.equalsIgnoreCase("unkn"))
			{
				return "unknown";
			}
			if(compartmentID.equalsIgnoreCase("cytmem"))
			{
				if(this.stain.equals(STAIN.gram_negative)) {

					return "perip";			}
				else {

					return "extr";
				}
			}
			if(compartmentID.equalsIgnoreCase("perip"))
			{
				return "outme";
			}
			if(compartmentID.equalsIgnoreCase("cellw") && compartmentID.equalsIgnoreCase("cellwall"))
			{
				return "extr";
			}
			if(compartmentID.equalsIgnoreCase("outme"))
			{
				return "extr";
			}
			if(compartmentID.equalsIgnoreCase("cytop"))
			{
				return "cytmem";
			}
			if(compartmentID.equalsIgnoreCase("pla") || compartmentID.equalsIgnoreCase("plas"))
			{
				return "extr";
			}
			else if(compartmentID.equalsIgnoreCase("gol") || compartmentID.equalsIgnoreCase("golg"))
			{
				return "golmem";
			}
			else if(compartmentID.equalsIgnoreCase("vac") || compartmentID.equalsIgnoreCase("vacu"))
			{
				return "vacmem";
			}
			else if(compartmentID.equalsIgnoreCase("mit") || compartmentID.equalsIgnoreCase("mito"))
			{
				return "mitmem";
			}
			else if(compartmentID.equalsIgnoreCase("end") || compartmentID.equalsIgnoreCase("E.R.")) 
			{
				return "ermem";
			}
			else if(compartmentID.equalsIgnoreCase("nuc") || compartmentID.equalsIgnoreCase("nucl"))
			{
				return "nucmem";
			}
			else if(compartmentID.equalsIgnoreCase("cyt") || compartmentID.equalsIgnoreCase("cyto"))
			{
				return "plas";
			}
			else if(compartmentID.equalsIgnoreCase("csk") || compartmentID.equalsIgnoreCase("cysk"))
			{
				return "plas";
			}
			else if(compartmentID.equalsIgnoreCase("pox") || compartmentID.equalsIgnoreCase("pero"))
			{
				return "permem";
			}
			else
			{
				return "cyto";
			}
		}
		else {

			throw new Exception("Compartments processing not initiated!");
		}
	}

	/**
	 * @return the kingdom
	 */
	public KINGDOM getKingdom() {
		return kingdom;
	}

	/**
	 * @param kingdom the kingdom to set
	 */
	public void setKingdom(KINGDOM kingdom) {
		this.kingdom = kingdom;
	}

	/**
	 * @return the stain
	 */
	public STAIN getStain() {
		return stain;
	}

	/**
	 * @param stain the stain to set
	 */
	public void setStain(STAIN stain) {
		this.stain = stain;
	}



	/**
	 * @return the interiorCompartment
	 */
	public String getInteriorCompartment() {
		return interiorCompartment;
	}

	/**
	 * @param interiorCompartment the interiorCompartment to set
	 */
	public void setInteriorCompartment(String interiorCompartment) {
		this.interiorCompartment = interiorCompartment;
	}



	/**
	 * @return the ignoreCompartmentsID
	 */
	public Set<String> getIgnoreCompartmentsID() {
		return ignoreCompartmentsID;
	}

	/**
	 * @param ignoreCompartmentsID the ignoreCompartmentsID to set
	 */
	public void setIgnoreCompartmentsID(Set<String> ignoreCompartmentsID) {
		this.ignoreCompartmentsID = ignoreCompartmentsID;
	}



	/**
	 * @return the hascellwall
	 */
	public boolean isHasCellWall() {
		return hasCellWall;
	}

	/**
	 * @param hascellwall the hascellwall to set
	 */
	public void setHasCellWall(boolean hascellwall) {
		this.hasCellWall = hascellwall;
	}



	/**
	 * @return the processCompartmentsInitiated
	 */
	public boolean isProcessCompartmentsInitiated() {
		return processCompartmentsInitiated;
	}


	/**
	 * @param processCompartmentsInitiated the processCompartmentsInitiated to set
	 */
	public void setProcessCompartmentsInitiated(boolean processCompartmentsInitiated) {
		
		this.processCompartmentsInitiated = processCompartmentsInitiated;
	}



	public enum STAIN {

		gram_positive,
		gram_negative
	}
}
