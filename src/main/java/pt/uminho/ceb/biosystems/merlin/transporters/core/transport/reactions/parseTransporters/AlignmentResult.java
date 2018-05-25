/**
 * 
 */
package pt.uminho.ceb.biosystems.merlin.transporters.core.transport.reactions.parseTransporters;

/**
 * @author ODias
 *
 */
public class AlignmentResult {

	private String uniprot_id;
	private double similarity;
	
	/**
	 * 
	 */
	public AlignmentResult() {
		
	}

	/**
	 * @param uniprot_id
	 * @param similarity
	 */
	public AlignmentResult(String uniprot_id, double similarity) {
		super();
		this.uniprot_id = uniprot_id;
		this.similarity = similarity;
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

	@Override
	public String toString() {
		return "AlignmentResult [uniprot_id=" + uniprot_id + ", similarity="
				+ similarity + "]";
	}

}
