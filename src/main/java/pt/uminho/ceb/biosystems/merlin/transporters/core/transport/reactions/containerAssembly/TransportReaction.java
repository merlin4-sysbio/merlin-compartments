/**
 * 
 */
package pt.uminho.ceb.biosystems.merlin.transporters.core.transport.reactions.containerAssembly;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author ODias
 *
 */
public class TransportReaction implements Comparable<TransportReaction> {

	private Map<String, List<Double>> metaboliteStoichiometry;
	private Map<String, List<String>> metaboliteDirection;
	//private Set<String> metaboliteSet;
	private String reactionID;
	private String transportType;
	private Map<String, TransportMetabolite> metabolites;
	private boolean reversibility;
	private List<String> protein_family_IDs;
	private Map<String, Boolean> originalReaction;
	private Map<String, Map<String, String>> general_equation;
	private Map<String, String> originalReactionID;


	/**
	 * @param reDS
	 * @param reactionID
	 * @param transportType
	 * @param reversibilty
	 * @param originalReaction
	 * @param geneID
	 * @param originalReactionID
	 */
	public TransportReaction(String reactionID, String transportType, boolean reversibilty, boolean originalReaction, String geneID, String originalReactionID) {
		this.setTransportType(transportType);
		this.setReversibility(reversibilty);
		this.metaboliteDirection= new TreeMap<String, List<String>>();
		this.metaboliteStoichiometry= new TreeMap<String, List<Double>>();
		this.metabolites= new TreeMap<String, TransportMetabolite>();
		//this.metaboliteSet= new TreeSet<String>();
		this.setReactionID(reactionID);
		this.addOriginalReactionID(geneID, originalReactionID);
		this.addOriginalReaction(geneID, originalReaction);
	}

	/**
	 * @param geneID
	 * @param originalReactionIDString
	 */
	public void addOriginalReactionID(String geneID, String originalReactionIDString) {

		if(this.originalReactionID==null)
			this.originalReactionID = new HashMap<String, String>();

		this.originalReactionID.put(geneID, originalReactionIDString);

	}

	/**
	 * @param geneID
	 * @param originalReaction_string
	 */
	public void addOriginalReaction(String geneID, boolean originalReaction_string) {

		if(this.originalReaction==null)
			this.originalReaction = new HashMap<String, Boolean>();

		this.originalReaction.put(geneID, originalReaction_string);
	}

	/**
	 * @param metaboliteID
	 * @param transportMetabolite
	 * @param stoichiometry
	 * @param direction
	 */
	public void addMetabolite(String metaboliteID, TransportMetabolite transportMetabolite, Double stoichiometry, String direction){

		List<String> directions = new ArrayList<String>();
		
		if(this.metaboliteDirection.containsKey(metaboliteID))
			directions=this.metaboliteDirection.get(metaboliteID);

		directions.add(directions.size(),direction);
		
		this.metaboliteDirection.put(metaboliteID, directions);

		List<Double> stoichiometries = new ArrayList<Double>();
		
		if(this.metaboliteStoichiometry.containsKey(metaboliteID))
			stoichiometries=this.metaboliteStoichiometry.get(metaboliteID);
		

		stoichiometries.add(stoichiometries.size(),stoichiometry);
		this.metaboliteStoichiometry.put(metaboliteID, stoichiometries);

		//if(!delete_reactant_and_inside_metabolite(metaboliteID))
		{
			this.metabolites.put(metaboliteID, transportMetabolite);		
		}
	}

	/**
	 * @param metaboliteID
	 * @return

	private boolean delete_reactant_and_inside_metabolite(String metaboliteID){
		List<Integer> indexes = new ArrayList<Integer>();
		boolean exists_in = false, exists_reactant = false;
		for(int index = 0; index<this.metaboliteDirection.get(metaboliteID).size();index++)
		{
			String direction = this.metaboliteDirection.get(metaboliteID).get(index);

			if(direction.trim().equals("in"))
			{
				exists_in=true;
				indexes.add(index);
			}
			if(direction.trim().equals("reactant"))
			{
				exists_reactant=true;
				indexes.add(index);
			}
		}

		if(exists_in && exists_reactant)
		{
			Collections.sort(indexes, Collections.reverseOrder());

			for(int i:indexes)
			{
				this.metaboliteDirection.get(metaboliteID).remove(i);
				this.metaboliteStoichiometry.get(metaboliteID).remove(i);	
			}
			if(this.metaboliteDirection.get(metaboliteID).isEmpty())
			{
				this.metabolites.remove(metaboliteID);
				this.metaboliteDirection.remove(metaboliteID);
				this.metaboliteStoichiometry.remove(metaboliteID);
			}
			return true;
		}
		return false;
	}
	 */

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	public TransportReaction clone(String reactionID, String geneID, String originalReactionID, boolean originalReaction) {

		TransportReaction transportReactionClone = new TransportReaction(reactionID, this.transportType, this.reversibility, originalReaction, geneID, originalReactionID);

		for(String metaboliteID:this.metabolites.keySet())
		{
			for(int i = 0; i<this.metaboliteStoichiometry.get(metaboliteID).size(); i++)
			{
				transportReactionClone.addMetabolite(metaboliteID, this.metabolites.get(metaboliteID), this.metaboliteStoichiometry.get(metaboliteID).get(i), this.metaboliteDirection.get(metaboliteID).get(i));
			}
		}
		transportReactionClone.setProtein_family_IDs(this.getProtein_family_IDs());
		transportReactionClone.setGeneral_equation(this.getGeneral_equation());
		transportReactionClone.setReversibility(this.reversibility);
		return transportReactionClone;
	}

	/**
	 * @return the metaboliteStoichiometry
	 */
	public Map<String, List<Double>> getMetaboliteStoichiometry() {
		return metaboliteStoichiometry;
	}

	/**
	 * @return the metaboliteDirection
	 */
	public Map<String, List<String>> getMetaboliteDirection() {
		return metaboliteDirection;
	}

	/**
	 * @param reactionID the reactionID to set
	 */
	public void setReactionID(String reactionID) {
		this.reactionID = reactionID;
	}

	/**
	 * @return the reactionID
	 */
	public String getReactionID() {
		return reactionID;
	}

	/**
	 * @return the metabolite
	 */
	public Map<String, TransportMetabolite> getMetabolites() {
		return metabolites;
	}

	/**
	 * @return the transportType
	 */
	public String getTransportType() {
		return transportType;
	}

	/**
	 * @param transportType the transportType to set
	 */
	public void setTransportType(String transportType) {
		this.transportType = transportType;
	}

	@Override
	public int compareTo(TransportReaction transportReaction) {

		return this.reactionID.compareTo(transportReaction.getReactionID());

	}

	public String toString(){
		return reactionID;
	}

	/**
	 * @return the reversibility
	 */
	public boolean isReversibility() {
		return reversibility;
	}

	/**
	 * @param reversibility the reversibility to set
	 */
	public void setReversibility(boolean reversibility) {
		this.reversibility = reversibility;
	}

	/**
	 * @param proteinFamily
	 */
	public void addProteinFamilyID(String proteinFamily){
		if(proteinFamily!=null)
		{
			if(this.protein_family_IDs==null)
			{
				this.setProtein_family_IDs(new ArrayList<String>());
			}
			if(!this.protein_family_IDs.contains(proteinFamily))
			{
				this.protein_family_IDs.add(proteinFamily);
			}
		}
	}

	/**
	 * @return the protein_family_IDs
	 */
	public List<String> getProtein_family_IDs() {
		return protein_family_IDs;
	}

	/**
	 * @param protein_family_IDs the protein_family_IDs to set
	 */
	public void setProtein_family_IDs(List<String> protein_family_IDs) {
		this.protein_family_IDs = protein_family_IDs;
	}


	/**
	 * @param geneID
	 * @return
	 */
	public Map<String , Boolean> isReversibilityConfirmed(String locus_tag) {

		Map<String , Boolean> result = new HashMap<String, Boolean>();

		if(this.general_equation == null)
			return null;

		Set<String> directions = new HashSet<String>();

		for(List<String> dir : this.getMetaboliteDirection().values()) {

			directions.addAll(dir);
		}

		if(!this.isReversibility() || directions.contains("product") || directions.contains("reactant")) {

			result.put(locus_tag, true);
		}
		else {

			Map<String, String> equations = general_equation.get(locus_tag);

			int size = equations.keySet().size();
			int notConfirmedfrequency = 0;

			if(equations.values().contains("--"))

				notConfirmedfrequency = Collections.frequency(equations.values(), "--");
			int confirmedfrequency = size-notConfirmedfrequency; 

			result.put(locus_tag, confirmedfrequency > notConfirmedfrequency);
		}

		return result;
	}

	/**
	 * @param geneID
	 * @param tcnumber
	 * @param general_equation
	 */
	public void addGeneral_equation(String geneID, String tcnumber, String general_equation) {

		if(this.general_equation==null) 
			this.general_equation = new HashMap<String, Map<String, String>>();

		Map<String, String> addEquation = new HashMap<String, String>();

		if(this.general_equation.containsKey(geneID))
			addEquation= this.getGeneral_equation().get(geneID);

		addEquation.put(tcnumber, general_equation);

		this.general_equation.put(geneID, addEquation);
	}


	/**
	 * @return the originalReaction
	 */
	public Map<String, Boolean> getOriginalReaction() {
		return originalReaction;
	}

	/**
	 * @param originalReaction the originalReaction to set
	 */
	public void setOriginalReaction(Map<String, Boolean> originalReaction) {
		this.originalReaction = originalReaction;
	}

	/**
	 * @return the general_equation
	 */
	public Map<String, Map<String, String>> getGeneral_equation() {
		return general_equation;
	}

	/**
	 * @return the originalReactionID
	 */
	public Map<String, String> getOriginalReactionID() {
		return originalReactionID;
	}

	/**
	 * @param originalReactionID the originalReactionID to set
	 */
	public void setOriginalReactionID(Map<String, String> originalReactionID) {
		this.originalReactionID = originalReactionID;
	}

	/**
	 * @param general_equation the general_equation to set
	 */
	public void setGeneral_equation(
			Map<String, Map<String, String>> general_equation) {
		this.general_equation = general_equation;
	}

}
