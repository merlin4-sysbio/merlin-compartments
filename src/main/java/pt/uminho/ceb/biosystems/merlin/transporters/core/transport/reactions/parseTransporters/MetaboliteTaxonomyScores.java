package pt.uminho.ceb.biosystems.merlin.transporters.core.transport.reactions.parseTransporters;

/**
 * @author ODias
 *
 */
public class MetaboliteTaxonomyScores {

	private String gene;
	private String metabolite;
	private double score;
	
	/**
	 * 
	 */
	public MetaboliteTaxonomyScores() {

	}

	/**
	 * @param gene
	 * @param metabolite
	 * @param score
	 */
	public MetaboliteTaxonomyScores(String gene, String metabolite, double score) {
		
		this.gene = gene;
		this.metabolite = metabolite;
		this.score = score;
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
	 * @return the metabolite
	 */
	public String getMetabolite() {
		return metabolite;
	}

	/**
	 * @param metabolite the metabolite to set
	 */
	public void setMetabolite(String metabolite) {
		this.metabolite = metabolite;
	}

	/**
	 * @return the score
	 */
	public double getScore() {
		return score;
	}

	/**
	 * @param score the score to set
	 */
	public void setScore(double score) {
		this.score = score;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "MetaboliteTaxonomyScores [gene=" + gene + ", metabolite="
				+ metabolite + ", score=" + score + "]";
	}

}
