/**
 * 
 */
package pt.uminho.ceb.biosystems.merlin.transporters.core.utils;

import java.io.Serializable;
import java.util.Set;

/**
 * @author ODias
 *
 */
public class GeneProteinAnnotation implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected String locus_tag;
	protected String uniprot_id;
	protected double similarity;
	protected String equation;
	protected Set<String> metabolites;
	protected String tc_number;
	
	/**
	 * @param locus_tag
	 */
	public GeneProteinAnnotation(String locus_tag) {

		this.setLocus_tag(locus_tag);
	}

	/**
	 * @return the locus_tag
	 */
	public String getLocus_tag() {
		return locus_tag;
	}

	/**
	 * @param locus_tag the locus_tag to set
	 */
	public void setLocus_tag(String locus_tag) {
		this.locus_tag = locus_tag;
	}

	/**
	 * @return the uniprot_id
	 */
	public String getUniprot_id() {
		return uniprot_id;
	}

	/**
	 * @param uniprot_id the uniprot_id to set
	 */
	public void setUniprot_id(String uniprot_id) {
		this.uniprot_id = uniprot_id;
	}

	/**
	 * @return the similarity
	 */
	public double getSimilarity() {
		return similarity;
	}

	/**
	 * @param similarity the similarity to set
	 */
	public void setSimilarity(double similarity) {
		this.similarity = similarity;
	}

	/**
	 * @return the equation
	 */
	public String getEquation() {
		return equation;
	}

	/**
	 * @param equation the equation to set
	 */
	public void setEquation(String equation) {
		this.equation = equation;
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
	 * @return the tc_number
	 */
	public String getTc_number() {
		return tc_number;
	}

	/**
	 * @param tc_number the tc_number to set
	 */
	public void setTc_number(String tc_number) {
		this.tc_number = tc_number;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "GeneProteinAnnotation [locus_tag=" + locus_tag
				+ ", uniprot_id=" + uniprot_id + ", similarity=" + similarity
				+ ", equation=" + equation + ", metabolites=" + metabolites
				+ ", tc_number=" + tc_number + "]";
	}
	
	

}
