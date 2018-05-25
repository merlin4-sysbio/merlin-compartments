/**
 * 
 */
package pt.uminho.ceb.biosystems.merlin.transporters.core.utils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author ODias
 *
 */
public class TracebackAnnotations implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected String reactionID;
	protected Map<String, GeneProteinAnnotation> genes_list ; 
	
	/**
	 * 
	 */
	public TracebackAnnotations(String reactionID) {

		this.setReactionID(reactionID);
	}

	/**
	 * @return the reactionID
	 */
	public String getReactionID() {
		return reactionID;
	}

	/**
	 * @param reactionID the reactionID to set
	 */
	public void setReactionID(String reactionID) {
		this.reactionID = reactionID;
	}

	/**
	 * @return the genes_list
	 */
	public Map<String, GeneProteinAnnotation> getGenes_list() {
		return genes_list;
	}

	/**
	 * @param genes_list the genes_list to set
	 */
	public void setGenes_list(Map<String, GeneProteinAnnotation> genes_list) {
		this.genes_list = genes_list;
	}
	
	/**
	 * @param geneProteinAnnotation
	 * @throws Exception 
	 */
	public void addGeneProteinAnnotation(GeneProteinAnnotation geneProteinAnnotation) throws Exception {
		
		if(this.genes_list==null)
			this.genes_list = new HashMap<String, GeneProteinAnnotation>();
		
		if(this.genes_list.containsKey(geneProteinAnnotation.getLocus_tag()))
			throw new Exception("Entity already added!");
		
		this.genes_list.put(geneProteinAnnotation.getLocus_tag(), geneProteinAnnotation);
			
	}

}
