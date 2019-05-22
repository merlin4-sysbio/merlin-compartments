package pt.uminho.ceb.biosystems.merlin.compartments.datatype;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import pt.uminho.ceb.biosystems.merlin.core.interfaces.ICompartmentResult;
import pt.uminho.ceb.biosystems.mew.utilities.datastructures.pair.Pair;

/**
 * @author Oscar Dias
 *
 */
public class AnnotationCompartmentsLocTree implements Serializable, ICompartmentResult {

	private static final long serialVersionUID = 1L;
	private String geneID;
	private double score; 
	private String compartment;
	private String goTerms;
	private String expectedAccuracy;
	private String annotationType;
	private List<Pair<String, Double>> compartments;
	
	/**
	 * @param geneID
	 */
	public AnnotationCompartmentsLocTree (String geneID) {
	
		this.setGeneID(geneID);
		this.compartments = new ArrayList<>();
	}

	/**
	 * @param geneID
	 * @param score
	 * @param compartment
	 * @param goTerms
	 */
	public AnnotationCompartmentsLocTree(String geneID, double score, String compartment, String goTerms) {
		super();
		this.setGeneID(geneID);
		this.setScore(score);
		this.setCompartment(compartment);
		this.setGoTerms(goTerms);
		this.compartments = new ArrayList<>();
		this.addCompartment(compartment, score);
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


	/* (non-Javadoc)
	 * @see pt.uminho.sysbio.common.transporters.core.compartments.CompartmentResult#addCompartment(java.lang.String, double)
	 */
	@Override
	public void addCompartment(String compartmentAbbreviation, double score) {
		
		Pair<String, Double> out = new Pair<String,Double>(compartmentAbbreviation, this.getScore());
		this.compartments.add(out);
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


	/**
	 * @return the compartment
	 */
	public String getCompartment() {
		return compartment;
	}


	/**
	 * @param compartment the compartment to set
	 */
	public void setCompartment(String compartment) {
		this.compartment = compartment;
	}


	/**
	 * @return the goTerms
	 */
	public String getGoTerms() {
		return goTerms;
	}


	/**
	 * @param goTerms the goTerms to set
	 */
	public void setGoTerms(String goTerms) {
		this.goTerms = goTerms;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "LocTreeResult [geneID=" + geneID + ", score=" + score + ", compartment=" + compartment + ", goTerms="
				+ goTerms + "]";
	}


	/* (non-Javadoc)
	 * @see pt.uminho.sysbio.common.transporters.core.compartments.CompartmentResult#getCompartments()
	 */
	@Override
	public List<Pair<String, Double>> getCompartments() {
		
		return compartments;
	}


	/**
	 * @param compartments the compartments to set
	 */
	public void setCompartments(List<Pair<String, Double>> compartments) {
		this.compartments = compartments;
	}


	/**
	 * @return the expectedAccuracy
	 */
	public String getExpectedAccuracy() {
		return expectedAccuracy;
	}


	/**
	 * @param expectedAccuracy the expectedAccuracy to set
	 */
	public void setExpectedAccuracy(String expectedAccuracy) {
		this.expectedAccuracy = expectedAccuracy;
	}


	/**
	 * @return the annotationType
	 */
	public String getAnnotationType() {
		return annotationType;
	}


	/**
	 * @param annotationType the annotationType to set
	 */
	public void setAnnotationType(String annotationType) {
		this.annotationType = annotationType;
	}
	
}
