/**
 * 
 */
package pt.uminho.ceb.biosystems.merlin.transporters.core.transport.reactions.containerAssembly;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author ODias
 *
 */
public class TransportTypeSelection {

	private Map<String, Double>transportTypePercentage;
	private Map<String, Double>transportTypeScore;
	private String  metabolitesID;

	/**
	 * @param transportTypeID
	 * @param metabolitesID
	 * @param percentage
	 */
	public TransportTypeSelection(String transportTypeID,String metabolitesID,Double score) {
		this.setMetabolitesID(metabolitesID);
		this.transportTypeScore= new TreeMap<String, Double>();
		this.transportTypeScore.put(transportTypeID,score);
		this.transportTypePercentage= new TreeMap<String, Double>();
		this.transportTypePercentage.put(transportTypeID, 100.0);
	}
	
	/**
	 * @param transportType
	 * @param score
	 */
	public void addTransportType(String transportTypeID, Double score){
		this.transportTypeScore.put(transportTypeID, score);
		double sum = this.sumCollection(this.transportTypeScore.values());
		this.transportTypePercentage = new TreeMap<String, Double>();
		for(String key: this.transportTypeScore.keySet())
		{
			this.transportTypePercentage.put(key, this.transportTypeScore.get(key)/sum);
		}
	}
	
	
	/**
	 * @param metabolitesID the metabolitesID to set
	 */
	public void setMetabolitesID(String metabolitesID) {
		this.metabolitesID = metabolitesID;
	}

	/**
	 * @return the metabolitesID
	 */
	public String getMetabolitesID() {
		return metabolitesID;
	}
	
	/**
	 * @return the transportTypePercentage
	 */
	public Map<String, Double> getTransportTypePercentage() {
		return transportTypePercentage;
	}

	/**
	 * @param transportTypePercentage the transportTypePercentage to set
	 */
	public void setTransportTypePercentage(
			Map<String, Double> transportTypePercentage) {
		this.transportTypePercentage = transportTypePercentage;
	}

	/**
	 * @param collection
	 * @return
	 */
	private double sumCollection(Collection<Double> collection){
		double sum=0;
		for(double value:collection){sum+=value;}
		return sum;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "TransportTypeSelection [transportTypePercentage="
				+ transportTypePercentage + ", transportTypeScore="
				+ transportTypeScore + ", metabolitesID=" + metabolitesID + "]";
	}

}
