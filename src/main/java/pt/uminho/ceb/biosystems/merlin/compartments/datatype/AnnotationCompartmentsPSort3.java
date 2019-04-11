package pt.uminho.ceb.biosystems.merlin.compartments.datatype;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import pt.uminho.ceb.biosystems.merlin.core.interfaces.ICompartmentResult;
import pt.uminho.ceb.biosystems.merlin.utilities.Pair;


public class AnnotationCompartmentsPSort3 implements Serializable, ICompartmentResult {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String geneID;
	private List<Pair<String, Double>> compartments;
	
	/**
	 * @param geneID
	 */
	public AnnotationCompartmentsPSort3(String geneID) {
		this.setGeneID(geneID);
		this.setCompartments(new ArrayList<Pair<String, Double>>());
	}
	
	/**
	 * @param compartmentID
	 * @param score
	 */
	public void addCompartment(String compartmentAbbreviation, double score){
		this.compartments.add(new Pair<String, Double>(compartmentAbbreviation,score));
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
	 * @return the compartments
	 */
	public List<Pair<String, Double>> getCompartments() {
		return compartments;
	}

	/**
	 * @param compartments the compartments to set
	 */
	public void setCompartments(List<Pair<String, Double>> compartments) {
		this.compartments = compartments;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "PSort3Result ["
				+ (this.geneID != null ? "geneID=" + this.geneID + ", " : "")
				+ (this.compartments != null ? "compartments=" + this.compartments : "")
				+ "]";
	}
	
}
