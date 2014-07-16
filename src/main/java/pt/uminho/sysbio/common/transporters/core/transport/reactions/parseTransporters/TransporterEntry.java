package pt.uminho.sysbio.common.transporters.core.transport.reactions.parseTransporters;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import pt.uminho.sysbio.common.transporters.core.transport.reactions.parseTransporters.MetabolitesEntry.TransportType;
import uk.ac.ebi.kraken.interfaces.uniprot.NcbiTaxon;

/**
 * 
 */

/**
 * @author ODias
 *
 */
public class TransporterEntry {

	private String tcNumber;
	private List<MetabolitesEntry> metabolitesEntryList;
	private List<TransportType> metabolitesTransportType;
	private double taxonomyRanking=0;
	private Set<String> metabolites;
	//private List<NcbiTaxon> originTaxonomy;
	private String comments;
	private double similarity;
	
	/**
	 * @param tcNumber
	 * @param accessionNumber
	 * @param originTaxonomy
	 * @param partialSimilarity
	 * @param comments
	 */
	public TransporterEntry(String tcNumber, List<NcbiTaxon> taxonomy, List<NcbiTaxon> originTaxonomy, double similarity, String comments) {
		this.setTcNumber(tcNumber);
		this.setSimilarity(similarity); 
		this.metabolites= new TreeSet<String>();
		this.metabolitesEntryList = new ArrayList<MetabolitesEntry>();
		this.metabolitesTransportType = new ArrayList<TransportType>();
		this.setComments(comments);
		
		if (taxonomy != null) 
		{
			taxonomy.retainAll(originTaxonomy);
			this.taxonomyRanking=taxonomy.size();
		}
	}

	/**
	 * @param transportedMetabolitesEntry
	 */
	public void addTransportedMetabolitesEntry(MetabolitesEntry transportedMetabolitesEntry){
		this.metabolites.addAll(transportedMetabolitesEntry.getMetabolites());
		this.metabolitesEntryList.add(transportedMetabolitesEntry);
		this.metabolitesTransportType.add(transportedMetabolitesEntry.getTransportType());
	}
	
	/**
	 * @param tcNumber the tcNumber to set
	 */
	public void setTcNumber(String tcNumber) {
		this.tcNumber = tcNumber;
	}

	/**
	 * @return the tcNumber
	 */
	public String getTcNumber() {
		return tcNumber;
	}

	/**
	 * @return the transportedMetabolitesEntryList
	 */
	public List<MetabolitesEntry> getTransportedMetabolitesEntryList() {
		return metabolitesEntryList;
	}

	/**
	 * @param transportedMetabolitesEntryList the transportedMetabolitesEntryList to set
	 */
	public void setTransportedMetabolitesEntryList(
			List<MetabolitesEntry> transportedMetabolitesEntryList) {
		this.metabolitesEntryList = transportedMetabolitesEntryList;
	}

	/**
	 * @return the taxonomyRanking
	 */
	public double getTaxonomyRanking() {
		return taxonomyRanking;
	}

	/**
	 * @param taxonomyRanking the taxonomyRanking to set
	 */
	public void setTaxonomyRanking(double taxonomyRanking) {
		this.taxonomyRanking = taxonomyRanking;
	}

	/**
	 * @return the metabolites
	 */
	public Set<String> getMetabolites() {
		return metabolites;
	}

	/**
	 * @param metabolites the metabolites to set
	 */
	public void setMetabolites(Set<String> metabolites) {
		this.metabolites = metabolites;
	}

	/**
	 * @return
	 */
	public List<MetabolitesEntry> getMetabolitesEntryList() {
		return metabolitesEntryList;
	}

	/**
	 * @param metabolitesEntryList
	 */
	public void setMetabolitesEntryList(List<MetabolitesEntry> metabolitesEntryList) {
		this.metabolitesEntryList = metabolitesEntryList;
	}

	/**
	 * @return
	 */
	public List<TransportType> getMetabolitesTransportType() {
		return metabolitesTransportType;
	}

	/**
	 * @param metabolitesTransportType
	 */
	public void setMetabolitesTransportType(
			List<TransportType> metabolitesTransportType) {
		this.metabolitesTransportType = metabolitesTransportType;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "\n\t\tTransporterEntry" +
				"\n\t\t\t[" +
				"\n\t\t\t\ttcNumber=" + tcNumber + 
				",\n\t\t\t\ttaxonomyRanking=" + taxonomyRanking +
				",\n\t\t\t\tmetabolites=" + metabolites + 
				",\n\t\t\t\ttransportedMetabolitesEntryList="+ metabolitesEntryList + 
				"\n\t\t\t]";
	}

	/**
	 * @param comments the comments to set
	 */
	public void setComments(String comments) {
		this.comments = comments;
	}

	/**
	 * @return the comments
	 */
	public String getComments() {
		return comments;
	}

	/**
	 * @param similarity the similarity to set
	 */
	public void setSimilarity(double similarity) {
		this.similarity = similarity;
	}

	/**
	 * @return the similarity
	 */
	public double getSimilarity() {
		return similarity;
	}


}
