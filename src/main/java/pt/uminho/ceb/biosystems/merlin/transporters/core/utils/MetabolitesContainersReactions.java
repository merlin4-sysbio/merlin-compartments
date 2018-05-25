/**
 * 
 */
package pt.uminho.ceb.biosystems.merlin.transporters.core.utils;

import java.util.Map;
import java.util.Set;

/**
 * @author ODias
 *
 */
public class MetabolitesContainersReactions {
	
	protected String metaboliteID;
	protected boolean hasMatchingReaction;
	protected Map<String, String> matchedReactions;
	protected Set<String> modelTransportReactionsWithMetabolites;
	protected Set<String> transportContainerReactionsWithMetabolite;
	
	/**
	 * 
	 */
	public MetabolitesContainersReactions(String metaboliteID) {

		this.setMetaboliteID(metaboliteID);
	}

	/**
	 * @return the metaboliteID
	 */
	public String getMetaboliteID() {
		return metaboliteID;
	}

	/**
	 * @param metaboliteID the metaboliteID to set
	 */
	public void setMetaboliteID(String metaboliteID) {
		this.metaboliteID = metaboliteID;
	}

	/**
	 * @return the hasMatchingReaction
	 */
	public boolean isHasMatchingReaction() {
		return hasMatchingReaction;
	}

	/**
	 * @param hasMatchingReaction the hasMatchingReaction to set
	 */
	public void setHasMatchingReaction(boolean hasMatchingReaction) {
		this.hasMatchingReaction = hasMatchingReaction;
	}

	/**
	 * @return the matchedReactions
	 */
	public Map<String, String> getMatchedReactions() {
		return matchedReactions;
	}

	/**
	 * @param matchedReactions the matchedReactions to set
	 */
	public void setMatchedReactions(Map<String, String> matchedReactions) {
		this.matchedReactions = matchedReactions;
	}

	/**
	 * @return the modelTransportReactionsWithMetabolites
	 */
	public Set<String> getModelTransportReactionsWithMetabolites() {
		return modelTransportReactionsWithMetabolites;
	}

	/**
	 * @param modelTransportReactionsWithMetabolites the modelTransportReactionsWithMetabolites to set
	 */
	public void setModelTransportReactionsWithMetabolites(
			Set<String> modelTransportReactionsWithMetabolites) {
		this.modelTransportReactionsWithMetabolites = modelTransportReactionsWithMetabolites;
	}

	/**
	 * @return the transportContainerReactionsWithMetabolite
	 */
	public Set<String> getTransportContainerReactionsWithMetabolite() {
		return transportContainerReactionsWithMetabolite;
	}

	/**
	 * @param transportContainerReactionsWithMetabolite the transportContainerReactionsWithMetabolite to set
	 */
	public void setTransportContainerReactionsWithMetabolite(
			Set<String> transportContainerReactionsWithMetabolite) {
		this.transportContainerReactionsWithMetabolite = transportContainerReactionsWithMetabolite;
	}

}
