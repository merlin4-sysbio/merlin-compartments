/**
 * 
 */
package pt.uminho.sysbio.common.transporters.core.transport.reactions.containerAssembly;

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
		
		for(String transportTypeID:map.keySet()) {
			
			if(map.get(transportTypeID)==value) {
				
				similar.add(transportTypeID);
				similar.add(new String(result));
			}
			
			if(map.get(transportTypeID)>value) {
				
				value = map.get(transportTypeID);
				result=transportTypeID;
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

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "GenesMetabolitesTransportType [metaboliteTransportType="
				+ metaboliteTransportType + ", geneID=" + geneID + "]";
	}

	
}
