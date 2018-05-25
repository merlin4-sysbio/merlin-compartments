package pt.uminho.ceb.biosystems.merlin.transporters.core.compartments;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pt.uminho.ceb.biosystems.merlin.bioapis.externalAPI.ncbi.EntrezLink.KINGDOM;
import pt.uminho.ceb.biosystems.merlin.database.connector.databaseAPI.CompartmentsAPI;
import pt.uminho.ceb.biosystems.merlin.database.connector.datatypes.Connection;
import pt.uminho.ceb.biosystems.merlin.transporters.core.utils.TransportersUtilities;
import pt.uminho.ceb.biosystems.merlin.transporters.core.utils.Enumerators.STAIN;

/**
 * @author ODias
 *
 */
public class ProcessCompartments {

	private KINGDOM kingdom;
	private STAIN stain;
	private boolean hasCellWall=false;
	private String interiorCompartment;
	private Set<Integer> ignoreCompartmentsID;
	private boolean processCompartmentsInitiated = false;

	/**
	 */
	public ProcessCompartments() {

	}

	/**
	 * @param interiorCompartment
	 */
	public ProcessCompartments(String interiorCompartment) {

		this.interiorCompartment = interiorCompartment;
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

			this.ignoreCompartmentsID = new HashSet<>();
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
	 * @param metaboliteMap
	 * @param compartment
	 * @return
	 * @throws Exception 
	 */
	public String processTransportCompartments(String localisation, String compartment) throws Exception {

		try {

			if (this.isProcessCompartmentsInitiated()) {

				if (localisation.equalsIgnoreCase("out")) {
					
					return TransportersUtilities.getOutsideMembrane(compartment.toLowerCase(), this.stain);

//					if (compartment.equalsIgnoreCase("plas") || compartment.equalsIgnoreCase("pla") || compartment.equalsIgnoreCase("outme")
//							|| compartment.equalsIgnoreCase("plasmem") || compartment.equalsIgnoreCase("outmem") || compartment.equalsIgnoreCase("cellw")) {
//
//						return ("extr".toUpperCase());
//					}
//					else if (compartment.equalsIgnoreCase("cytmem")) {
//
//						if (this.stain.equals(STAIN.gram_negative))
//							return ("perip".toUpperCase());
//						else {
//							return ("extr".toUpperCase());
//						} 
//					}
//					else {
//						
//						return ("extr".toUpperCase());
//					}
				} 
				else {

					return TransportersUtilities.getInsideMembrane(compartment.toLowerCase(), this.stain);
					
//					if (compartment.equalsIgnoreCase("plas") || compartment.equalsIgnoreCase("pla") || compartment.equalsIgnoreCase("plasmem") 
//							|| compartment.equalsIgnoreCase("cellw")) {
//
//						return (interiorCompartment.toUpperCase());
//					}
//					else if (compartment.equalsIgnoreCase("outme") || compartment.equalsIgnoreCase("outmem")) {
//
//						return ("perip".toUpperCase());
//					}
//					else if (compartment.contains("mem")) {
//
//						return TransportersUtilities.getOutsideMembrane(compartment.toLowerCase());
//					}
//					else {
//
//						return (compartment.toUpperCase());
//					}
				}
			} else {

				throw new Exception("Compartments processing not initiated!");
			} 
		} catch (Exception e) {

			System.out.println(localisation+" "+compartment);
			throw e;
		}
	}

	/**
	 * @return
	 * @throws SQLException 
	 */
	public String autoSetInteriorCompartment(Connection connection) throws SQLException {

		Statement stmt = connection.createStatement();

		return this.autoSetInteriorCompartment(stmt);
	}

	/**
	 * @param statement
	 * @return
	 * @throws SQLException
	 */
	public String autoSetInteriorCompartment(Statement statement){

		try {
			this.interiorCompartment = CompartmentsAPI.getCompartmentAbbreviation(this.interiorCompartment, statement);

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return interiorCompartment;
	}


	/**
	 * @param compartment
	 * @throws SQLException
	 */
	public int getCompartmentID(String compartment, Connection connection) {

		Statement stmt;
		int compartmentID = -1;

		try {
			stmt = connection.createStatement();

			String abbreviation;

			if(compartment.length()>3) {

				abbreviation=compartment.substring(0,3).toUpperCase();
			}
			else {

				abbreviation=compartment.toUpperCase().concat("_");

				while(abbreviation.length()<4)
					abbreviation=abbreviation.concat("_");
			}

			abbreviation=abbreviation.toUpperCase();

			compartmentID = CompartmentsAPI.selectCompartmentID(compartment, abbreviation, stmt);

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return compartmentID;

	}

	/**
	 * @param list
	 * @param compartmentsAbb_ids
	 * @param idCompartmentAbbIdMap
	 * @param ignoreList
	 * @return
	 * @throws Exception 
	 */
	public Set<Integer> parseCompartments(List<Integer> list, Map<Integer, String> compartmentsAbb_ids, Map<String, Integer> idCompartmentAbbIdMap, List<String> ignoreList) throws Exception {

		return ProcessCompartments.parseCompartments(list, compartmentsAbb_ids, idCompartmentAbbIdMap, ignoreList, stain, hasCellWall, ignoreCompartmentsID, interiorCompartment, this.isProcessCompartmentsInitiated());
	}

	/**
	 * Static method for parsing compartmtents for metabolic reactions
	 * 
	 * @param list
	 * @param compartmentsAbb_ids
	 * @param idCompartmentAbbIdMap
	 * @param ignoreList
	 * @param stain
	 * @param hasCellWall
	 * @param ignoreCompartmentsID
	 * @param interiorCompartment
	 * @param isProcessCompartmentsInitiated
	 * @return
	 * @throws Exception
	 */
	public static Set<Integer> parseCompartments(List<Integer> list, Map<Integer, String> compartmentsAbb_ids, Map<String, Integer> idCompartmentAbbIdMap, List<String> ignoreList,
			STAIN stain, boolean hasCellWall, Set<Integer> ignoreCompartmentsID, String interiorCompartment, boolean isProcessCompartmentsInitiated) throws Exception {

		if (isProcessCompartmentsInitiated) {

			Set<Integer> compartments = new HashSet<>();

			for(int compartment: list) {

				String abb = compartmentsAbb_ids.get(compartment).toLowerCase();

				if(abb.equalsIgnoreCase("cytmem")) {

					compartments.add(idCompartmentAbbIdMap.get(interiorCompartment.toLowerCase()));

					if(stain.equals(STAIN.gram_negative))
						compartments.add(idCompartmentAbbIdMap.get("perip"));
					else
						if(!hasCellWall)
							compartments.add(idCompartmentAbbIdMap.get("extr"));
				}
				else if(abb.equalsIgnoreCase("cellw")) {

					compartments.add(idCompartmentAbbIdMap.get("extr"));
				}
				else if(abb.equalsIgnoreCase("outme")) {

					compartments.add(idCompartmentAbbIdMap.get("perip"));
					compartments.add(idCompartmentAbbIdMap.get("extr"));
				}
				else if(abb.equalsIgnoreCase("pla") || abb.equalsIgnoreCase("plas")) {

					compartments.add(idCompartmentAbbIdMap.get(interiorCompartment.toLowerCase()));
					compartments.add(idCompartmentAbbIdMap.get("extr"));
				} 
				else if (abb.contains("me")) {

					for(String newAbb : TransportersUtilities.getOutsideMembranes(abb, stain)) 
						compartments.add(idCompartmentAbbIdMap.get(newAbb.toLowerCase()));
				}
				else if(abb.equalsIgnoreCase("unkn")) {

					compartments.add(idCompartmentAbbIdMap.get(interiorCompartment.toLowerCase()));
				} 
				else {

					compartments.add(compartment);
				}
				
				if(ignoreList.contains(abb.toLowerCase())) {

					compartments.add(idCompartmentAbbIdMap.get(interiorCompartment.toLowerCase()));
					ignoreCompartmentsID.add(compartment);
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

		if(this.isProcessCompartmentsInitiated())
			return TransportersUtilities.getOutside(this.stain, compartmentID);
		else
			throw new Exception("Compartments processing not initiated!");
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
	public Set<Integer> getIgnoreCompartmentsID() {
		return ignoreCompartmentsID;
	}

	/**
	 * @param ignoreCompartmentsID the ignoreCompartmentsID to set
	 */
	public void setIgnoreCompartmentsID(Set<Integer> ignoreCompartmentsID) {
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

}
