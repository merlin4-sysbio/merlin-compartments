/**
 * 
 */
package pt.uminho.ceb.biosystems.merlin.transporters.core.transport.reactions.containerAssembly;

import java.io.Serializable;

/**
 * @author ODias
 *
 */
public class MetabolitesOntology implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String metabolite_id;
	private String original_parent_metabolite_id;
	private String upper_parent_metabolite_id;
	private int generation;
	private String originalReactionID, reactionID;
	
	/**
	 * @param metabolite_id
	 * @param original_parent_metabolite_id
	 * @param upper_parent_metabolite_id
	 * @param generation
	 * @param originalReactionID
	 * @param reactionID
	 */
	public MetabolitesOntology(String metabolite_id, String original_parent_metabolite_id, String upper_parent_metabolite_id, int generation, String originalReactionID, String reactionID) {

		this.setMetabolite_id(metabolite_id);
		this.setOriginal_parent_metabolite_id(original_parent_metabolite_id);
		this.setUpper_parent_metabolite_id(upper_parent_metabolite_id);
		this.setGeneration(generation);
		this.setOriginalReactionID(originalReactionID);
		this.setReactionID(reactionID);
	}

	/**
	 * @return the metabolite_id
	 */
	public String getMetabolite_id() {
		return metabolite_id;
	}

	/**
	 * @param metabolite_id the metabolite_id to set
	 */
	public void setMetabolite_id(String metabolite_id) {
		this.metabolite_id = metabolite_id;
	}

	/**
	 * @return the original_parent_metabolite_id
	 */
	public String getOriginal_parent_metabolite_id() {
		return original_parent_metabolite_id;
	}

	/**
	 * @param original_parent_metabolite_id the original_parent_metabolite_id to set
	 */
	public void setOriginal_parent_metabolite_id(
			String original_parent_metabolite_id) {
		this.original_parent_metabolite_id = original_parent_metabolite_id;
	}

	/**
	 * @return the upper_parent_metabolite_id
	 */
	public String getUpper_parent_metabolite_id() {
		return upper_parent_metabolite_id;
	}

	/**
	 * @param upper_parent_metabolite_id the upper_parent_metabolite_id to set
	 */
	public void setUpper_parent_metabolite_id(String upper_parent_metabolite_id) {
		this.upper_parent_metabolite_id = upper_parent_metabolite_id;
	}

	/**
	 * @return the generation
	 */
	public int getGeneration() {
		return generation;
	}

	/**
	 * @param generation the generation to set
	 */
	public void setGeneration(int generation) {
		this.generation = generation;
	}
	
	/**
	 * @return the originalReactionID
	 */
	public String getOriginalReactionID() {
		return originalReactionID;
	}

	/**
	 * @param originalReactionID the originalReactionID to set
	 */
	public void setOriginalReactionID(String originalReactionID) {
		this.originalReactionID = originalReactionID;
	}

	public String getReactionID() {
		return reactionID;
	}

	public void setReactionID(String reactionID) {
		this.reactionID = reactionID;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "MetabolitesOntology [metabolite_id=" + metabolite_id
				+ ", original_parent_metabolite_id="
				+ original_parent_metabolite_id
				+ ", upper_parent_metabolite_id=" + upper_parent_metabolite_id
				+ ", generation=" + generation + ", originalReactionID="
				+ originalReactionID + ", reactionID=" + reactionID + "]";
	}

}
