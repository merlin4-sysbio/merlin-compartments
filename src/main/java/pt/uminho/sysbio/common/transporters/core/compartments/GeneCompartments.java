/**
 * 
 */
package pt.uminho.sysbio.common.transporters.core.compartments;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author ODias
 *
 */
public class GeneCompartments implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -5935372133844353935L;
	private String geneID, gene;
	private boolean dualLocalisation;
	private String primary_location, primary_location_abb;
	private Map<String,Double> secondary_location;
	private Map<String,String> secondary_location_abb;
	private double primary_score;
	
	/**
	 * 
	 */
	public GeneCompartments() {
	}
	
	/**
	 * @param geneID
	 * @param gene
	 * @param primary_location
	 * @param primary_location_abb
	 * @param primary_score
	 */
	public GeneCompartments(String geneID, String gene, String primary_location, String primary_location_abb, double primary_score) {
		super();
		this.setPrimary_location_abb(primary_location_abb);
		this.setGene(gene);
		this.setGeneID(geneID);
		this.setDualLocalisation(false);
		this.setPrimary_location(primary_location);
		this.setPrimary_score(primary_score);
		this.setSecondary_location(new HashMap<String, Double>());
		this.setSecondary_location_abb(new HashMap<String, String>());
	}

	
	/**
	 * @param secondary_location
	 * @param secondary_location_abb
	 * @param secondary_score
	 */
	public void addSecondaryLocation(String secondary_location, String secondary_location_abb, double secondary_score){
		this.setDualLocalisation(true);
		this.secondary_location.put(secondary_location, secondary_score);
		this.secondary_location_abb.put(secondary_location, secondary_location_abb);
	}

	/**
	 * @return the geneID
	 */
	public String getGeneID() {
		return geneID;
	}

	/**
	 * @param geneID the geneID to set
	 */
	public void setGeneID(String geneID) {
		this.geneID = geneID;
	}

	/**
	 * @return the gene
	 */
	public String getGene() {
		return gene;
	}

	/**
	 * @param gene the gene to set
	 */
	public void setGene(String gene) {
		this.gene = gene;
	}

	/**
	 * @return the dualLocalisation
	 */
	public boolean isDualLocalisation() {
		return dualLocalisation;
	}

	/**
	 * @param dualLocalisation the dualLocalisation to set
	 */
	public void setDualLocalisation(boolean dualLocalisation) {
		this.dualLocalisation = dualLocalisation;
	}

	/**
	 * @return the primary_location
	 */
	public String getPrimary_location() {
		return primary_location;
	}

	/**
	 * @param primary_location the primary_location to set
	 */
	public void setPrimary_location(String primary_location) {
		this.primary_location = primary_location;
	}

	/**
	 * @return the primary_location_abb
	 */
	public String getPrimary_location_abb() {
		return primary_location_abb;
	}

	/**
	 * @param primary_location_abb the primary_location_abb to set
	 */
	public void setPrimary_location_abb(String primary_location_abb) {
		this.primary_location_abb = primary_location_abb;
	}

	/**
	 * @return the secondary_location
	 */
	public Map<String, Double> getSecondary_location() {
		return secondary_location;
	}

	/**
	 * @param secondary_location the secondary_location to set
	 */
	public void setSecondary_location(Map<String, Double> secondary_location) {
		this.secondary_location = secondary_location;
	}

	/**
	 * @return the secondary_location_abb
	 */
	public Map<String, String> getSecondary_location_abb() {
		return secondary_location_abb;
	}

	/**
	 * @param secondary_location_abb the secondary_location_abb to set
	 */
	public void setSecondary_location_abb(Map<String, String> secondary_location_abb) {
		this.secondary_location_abb = secondary_location_abb;
	}

	/**
	 * @return the primary_score
	 */
	public double getPrimary_score() {
		return primary_score;
	}

	/**
	 * @param primary_score the primary_score to set
	 */
	public void setPrimary_score(double primary_score) {
		this.primary_score = primary_score;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "GeneCompartments ["
				+ (geneID != null ? "geneID=" + geneID + ", " : "")
				+ (gene != null ? "gene=" + gene + ", " : "")
				+ "dualLocalisation="
				+ dualLocalisation
				+ ", "
				+ (primary_location != null ? "primary_location="
						+ primary_location + ", " : "")
				+ (primary_location_abb != null ? "primary_location_abb="
						+ primary_location_abb + ", " : "")
				+ (secondary_location != null ? "secondary_location="
						+ secondary_location + ", " : "")
				+ (secondary_location_abb != null ? "secondary_location_abb="
						+ secondary_location_abb + ", " : "")
				+ "primary_score=" + primary_score + "]";
	}
	
	

}
