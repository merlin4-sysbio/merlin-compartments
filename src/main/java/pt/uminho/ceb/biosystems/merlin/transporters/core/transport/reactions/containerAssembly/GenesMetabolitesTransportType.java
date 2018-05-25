/**
 * 
 */
package pt.uminho.ceb.biosystems.merlin.transporters.core.transport.reactions.containerAssembly;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;


/**
 * @author ODias
 *
 */
public class GenesMetabolitesTransportType {

	private Map<String, TransportTypeSelection> metaboliteTransportType;
	private String geneID;

	/**
	 * @param geneID
	 */
	public GenesMetabolitesTransportType(String geneID) {
		this.setGeneID(geneID);
		this.metaboliteTransportType = new TreeMap<String, TransportTypeSelection>();
	}

	/**
	 * @param metaboliteID
	 * @param transportTypeID
	 * @param score
	 */
	public void addMetaboliteTransportType(String metaboliteID, String transportTypeID, double score) {

		TransportTypeSelection transportTypeSelection;

		if(this.metaboliteTransportType.containsKey(metaboliteID)) {

			transportTypeSelection=this.metaboliteTransportType.get(metaboliteID);
			transportTypeSelection.addTransportType(transportTypeID, score);
		}
		else {

			transportTypeSelection= new TransportTypeSelection(transportTypeID, metaboliteID, score);
		}
		this.metaboliteTransportType.put(metaboliteID, transportTypeSelection);
	}

	/**
	 * @param metaboliteID
	 * @param transportType
	 * @return
	 */
	public boolean isHigherScoreTransportTypeID(String metaboliteID, String transportType) {
		double value=0;
		String result="";
		Set<String> similar = new HashSet<String>();

		Map<String,Double> map = this.metaboliteTransportType.get(metaboliteID).getTransportTypePercentage();

		if(transportType.equalsIgnoreCase("sensor") && map.size()>1)
			return false;

		for(String transportTypeID:map.keySet()) {

			double score = map.get(transportTypeID);

			if(score==value) {

				similar.add(transportTypeID);
				similar.add(new String(result));
			}

			if(score>value) {

				if(transportTypeID.equalsIgnoreCase("sensor") && map.size()>0)
					;
				else {
					
					value = score;
					result=transportTypeID;
				}
			}
		}

		if(result.equalsIgnoreCase(transportType)) {

			return true;
		}
		else {

			if(similar.contains(result) && similar.contains(transportType)) {

				return true;
			}
		}

		return false;
	}

	/**
	 * @param geneID the geneID to set
	 */
	public void setGeneID(String geneID) {
		this.geneID = geneID;
	}

	/**
	 * @return the geneID
	 */
	public String getGeneID() {
		return geneID;
	}


	/**
	 * @return the metaboliteTransportType
	 */
	public Map<String, TransportTypeSelection> getMetaboliteTransportType() {
		return metaboliteTransportType;
	}

	/**
	 * @param metaboliteTransportType the metaboliteTransportType to set
	 */
	public void setMetaboliteTransportType(
			Map<String, TransportTypeSelection> metaboliteTransportType) {
		this.metaboliteTransportType = metaboliteTransportType;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "GenesMetabolitesTransportType [metaboliteTransportType="
				+ metaboliteTransportType + ", geneID=" + geneID + "]";
	}


}
