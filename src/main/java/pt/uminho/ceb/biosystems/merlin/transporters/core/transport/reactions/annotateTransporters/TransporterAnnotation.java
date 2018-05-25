package pt.uminho.ceb.biosystems.merlin.transporters.core.transport.reactions.annotateTransporters;

/**
 * @author ODias
 *
 */
public class TransporterAnnotation {
	
	private String id, uniProt_ID, tcdb_ID, tcdb_family, tcdb_family_description, tcdb_description, affinity, type, tcdb_location,
	ytpdb_gene, ytpdb_description, ytpdb_type ,ytpdb_metabolites, ytpdb_location, 
	tc_number_family, direction, metabolite, reversibility, reacting_metabolites, equation;

	/**
	 * 
	 */
	public TransporterAnnotation() {
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return 	uniProt_ID
				+ "\t" + tcdb_ID 
				+ "\t" + tcdb_family 
				//+ "\t" + tcdb_family_description 
				+ "\t" + tcdb_description 
				+ "\t" + affinity 
				+ "\t" + type 
				+ "\t" + tcdb_location 
				+ "\t" + ytpdb_gene 
				+ "\t" + ytpdb_description
				+ "\t" + ytpdb_type 
				+ "\t" + ytpdb_metabolites 
				+ "\t" + ytpdb_location
				+ "\t" + tc_number_family
				+ "\t" + direction 
				+ "\t" + metabolite 
				+ "\t" + reversibility
				+ "\t" + reacting_metabolites
				+ "\t" + equation
				+ "\n" ;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the uniProt_ID
	 */
	public String getUniProt_ID() {
		return uniProt_ID;
	}

	/**
	 * @param uniProt_ID the uniProt_ID to set
	 */
	public void setUniProt_ID(String uniProt_ID) {
		this.uniProt_ID = uniProt_ID;
	}

	/**
	 * @return the tcdb_ID
	 */
	public String getTcdb_ID() {
		return tcdb_ID;
	}

	/**
	 * @param tcdb_ID the tcdb_ID to set
	 */
	public void setTcdb_ID(String tcdb_ID) {
		this.tcdb_ID = tcdb_ID;
	}

	/**
	 * @return the tcdb_family
	 */
	public String getTcdb_family() {
		return tcdb_family;
	}

	/**
	 * @param tcdb_family the tcdb_family to set
	 */
	public void setTcdb_family(String tcdb_family) {
		this.tcdb_family = tcdb_family;
	}

	/**
	 * @return the tcdb_description
	 */
	public String getTcdb_description() {
		return tcdb_description;
	}

	/**
	 * @param tcdb_description the tcdb_description to set
	 */
	public void setTcdb_description(String tcdb_description) {
		this.tcdb_description = tcdb_description;
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
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @return the tcdb_location
	 */
	public String getTcdb_location() {
		return tcdb_location;
	}

	/**
	 * @param tcdb_location the tcdb_location to set
	 */
	public void setTcdb_location(String tcdb_location) {
		this.tcdb_location = tcdb_location;
	}

	/**
	 * @return the ytpdb_gene
	 */
	public String getYtpdb_gene() {
		return ytpdb_gene;
	}

	/**
	 * @param ytpdb_gene the ytpdb_gene to set
	 */
	public void setYtpdb_gene(String ytpdb_gene) {
		this.ytpdb_gene = ytpdb_gene;
	}

	/**
	 * @return the ytpdb_description
	 */
	public String getYtpdb_description() {
		return ytpdb_description;
	}

	/**
	 * @param ytpdb_description the ytpdb_description to set
	 */
	public void setYtpdb_description(String ytpdb_description) {
		this.ytpdb_description = ytpdb_description;
	}

	/**
	 * @return the ytpdb_type
	 */
	public String getYtpdb_type() {
		return ytpdb_type;
	}

	/**
	 * @param ytpdb_type the ytpdb_type to set
	 */
	public void setYtpdb_type(String ytpdb_type) {
		this.ytpdb_type = ytpdb_type;
	}

	/**
	 * @return the ytpdb_metabolites
	 */
	public String getYtpdb_metabolites() {
		return ytpdb_metabolites;
	}

	/**
	 * @param ytpdb_metabolites the ytpdb_metabolites to set
	 */
	public void setYtpdb_metabolites(String ytpdb_metabolites) {
		this.ytpdb_metabolites = ytpdb_metabolites;
	}

	/**
	 * @return the ytpdb_location
	 */
	public String getYtpdb_location() {
		return ytpdb_location;
	}

	/**
	 * @param ytpdb_location the ytpdb_location to set
	 */
	public void setYtpdb_location(String ytpdb_location) {
		this.ytpdb_location = ytpdb_location;
	}

	/**
	 * @return the tc_number_family
	 */
	public String getTc_number_family() {
		return tc_number_family;
	}

	/**
	 * @param tc_number_family the tc_number_family to set
	 */
	public void setTc_number_family(String tc_number_family) {
		this.tc_number_family = tc_number_family;
	}

	/**
	 * @return the direction
	 */
	public String getDirection() {
		return direction;
	}

	/**
	 * @param direction the direction to set
	 */
	public void setDirection(String direction) {
		this.direction = direction;
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
	 * @return the reversibility
	 */
	public String getReversibility() {
		return reversibility;
	}

	/**
	 * @param reversibility the reversibility to set
	 */
	public void setReversibility(String reversibility) {
		this.reversibility = reversibility;
	}

	/**
	 * @return the reacting_metabolites
	 */
	public String getReacting_metabolites() {
		return reacting_metabolites;
	}

	/**
	 * @param reacting_metabolites the reacting_metabolites to set
	 */
	public void setReacting_metabolites(String reacting_metabolites) {
		this.reacting_metabolites = reacting_metabolites;
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
	 * @return the tcdb_family_description
	 */
	public String getTcdb_family_description() {
		return tcdb_family_description;
	}

	/**
	 * @param tcdb_family_description the tcdb_family_description to set
	 */
	public void setTcdb_family_description(String tcdb_family_description) {
		this.tcdb_family_description = tcdb_family_description;
	}

}
