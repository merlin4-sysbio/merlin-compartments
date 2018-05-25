package pt.uminho.ceb.biosystems.merlin.transporters.core.transport.reactions.parseTransporters;

import pt.uminho.ceb.biosystems.merlin.bioapis.externalAPI.ebi.uniprot.TaxonomyContainer;

/**
 * @author ODias
 *
 */
public class ParserContainer {

	private String uniprot_id;
	private String transportType;
	private String tc_number;
	private String tc_family;
	private String tc_location;
	private String affinity;
	private TaxonomyContainer taxonomyContainer;
	private String general_equation;
	private String metabolites;
	private String reactingMetabolites;
	private boolean reversibility;

	/**
	 * 
	 */
	public ParserContainer() {


	}

	/**
	 * @param uniprot_id
	 * @param tc_number
	 */
	public ParserContainer(String uniprot_id, String tc_number) {

		this.uniprot_id = uniprot_id;
		this.tc_number = tc_number;
	}

	/**
	 * @return
	 */
	public String getComplexID() {

		return this.uniprot_id+"__"+this.tc_number;
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

	/**
	 * @return the tc_family
	 */
	public String getTc_family() {
		return tc_family;
	}

	/**
	 * @param tc_family the tc_family to set
	 */
	public void setTc_family(String tc_family) {
		this.tc_family = tc_family;
	}

	/**
	 * @return the tc_location
	 */
	public String getTc_location() {
		return tc_location;
	}

	/**
	 * @param tc_location the tc_location to set
	 */
	public void setTc_location(String tc_location) {
		this.tc_location = tc_location;
	}

	/**
	 * @return the affinity
	 */
	public String getAffinity() {
		return affinity;
	}

	/**
	 * @param affinity the affinity to set
	 */
	public void setAffinity(String affinity) {
		this.affinity = affinity;
	}

	/**
	 * @return the direction
	 */
	public String getTransportType() {
		return transportType;
	}

	/**
	 * @param direction the direction to set
	 */
	public void setTransportType(String transportType) {
		this.transportType = transportType;
	}

	/**
	 * @return the metabolites
	 */
	public String getMetabolites() {
		return metabolites;
	}

	/**
	 * @param metabolites the metabolites to set
	 */
	public void setMetabolites(String metabolites) {
		this.metabolites = metabolites;
	}

	/**
	 * @return the reactingMetabolites
	 */
	public String getReactingMetabolites() {
		return reactingMetabolites;
	}

	/**
	 * @param reactingMetabolites the reactingMetabolites to set
	 */
	public void setReactingMetabolites(String reactingMetabolites) {
		this.reactingMetabolites = reactingMetabolites;
	}

	/**
	 * @return the reversibility
	 */
	public boolean getReversibility() {
		return reversibility;
	}

	/**
	 * @param reversibility the reversibility to set
	 */
	public void setReversibility(boolean reversibility) {
		this.reversibility = reversibility;
	}

	/**
	 * @return the general_equation
	 */
	public String getGeneral_equation() {
		return general_equation;
	}

	/**
	 * @param general_equation the general_equation to set
	 */
	public void setGeneral_equation(String general_equation) {
		this.general_equation = general_equation;
	}

	/**
	 * @return the taxonomyContainer
	 */
	public TaxonomyContainer getTaxonomyContainer() {
		return taxonomyContainer;
	}

	/**
	 * @param taxonomyContainer the taxonomyContainer to set
	 */
	public void setTaxonomyContainer(TaxonomyContainer taxonomyContainer) {
		this.taxonomyContainer = taxonomyContainer;
	}

	@Override
	public String toString() {
		return "ParserContainer [uniprot_id=" + uniprot_id + ", transportType="
				+ transportType + ", tc_number=" + tc_number + ", tc_family="
				+ tc_family + ", tc_location=" + tc_location + ", affinity="
				+ affinity + ", taxonomyContainer=" + taxonomyContainer
				+ ", general_equation=" + general_equation + ", metabolites="
				+ metabolites + ", reactingMetabolites=" + reactingMetabolites
				+ ", reversibility=" + reversibility + "]";
	}

}
