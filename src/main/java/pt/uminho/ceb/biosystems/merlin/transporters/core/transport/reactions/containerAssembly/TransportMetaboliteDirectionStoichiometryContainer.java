package pt.uminho.ceb.biosystems.merlin.transporters.core.transport.reactions.containerAssembly;

import java.util.Set;

/**
 * @author ODias
 *
 */
public class TransportMetaboliteDirectionStoichiometryContainer implements Comparable<TransportMetaboliteDirectionStoichiometryContainer>{

	private String name;
	private String direction;
	private double stoichiometry;
	private boolean reversible;
	private String kegg_name;
	private String chebi_name;
	private Set<String> synonyms;
	private String kegg_miriam;
	private String chebi_miriam;

	/**
	 * 
	 */
	public TransportMetaboliteDirectionStoichiometryContainer() {

		super();
	}

	/**
	 * @param name
	 */
	public TransportMetaboliteDirectionStoichiometryContainer(String name) {

		super();
		this.name = name;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
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
	 * @return the stoichiometry
	 */
	public double getStoichiometry() {
		return stoichiometry;
	}

	/**
	 * @param stoichiometry the stoichiometry to set
	 */
	public void setStoichiometry(double stoichiometry) {
		this.stoichiometry = stoichiometry;
	}

	/**
	 * @return the reversible
	 */
	public boolean isReversible() {
		return reversible;
	}

	/**
	 * @param reversible the reversible to set
	 */
	public void setReversible(boolean reversible) {
		this.reversible = reversible;
	}

	/**
	 * @return the kegg_name
	 */
	public String getKegg_name() {
		return kegg_name;
	}

	/**
	 * @param kegg_name the kegg_name to set
	 */
	public void setKegg_name(String kegg_name) {
		this.kegg_name = kegg_name;
	}

	/**
	 * @return the chebi_name
	 */
	public String getChebi_name() {
		return chebi_name;
	}

	/**
	 * @param chebi_name the chebi_name to set
	 */
	public void setChebi_name(String chebi_name) {
		this.chebi_name = chebi_name;
	}

	/**
	 * @return the synonyms
	 */
	public Set<String> getSynonyms() {
		return synonyms;
	}

	/**
	 * @param synonyms the synonyms to set
	 */
	public void setSynonyms(Set<String> synonyms) {
		this.synonyms = synonyms;
	}

	/**
	 * @return the kegg_miriam
	 */
	public String getKegg_miriam() {
		return kegg_miriam;
	}

	/**
	 * @param kegg_miriam the kegg_miriam to set
	 */
	public void setKegg_miriam(String kegg_miriam) {
		this.kegg_miriam = kegg_miriam;
	}

	/**
	 * @return the chebi_miriam
	 */
	public String getChebi_miriam() {
		return chebi_miriam;
	}


	@Override
	public boolean equals(Object obj) {

		if((obj == null) || (obj.getClass() != this.getClass()))
			return false;

		TransportMetaboliteDirectionStoichiometryContainer otherTmds = (TransportMetaboliteDirectionStoichiometryContainer) obj;

		boolean sameName = false;

		String name = "";
		if(this.name != null)
			name = this.name.toLowerCase();

		String kegg = "";
		if(this.kegg_name != null)
			kegg = this.kegg_name.toLowerCase();

		String chebi = "";
		if(this.chebi_name != null)
			chebi = this.chebi_name.toLowerCase();

		String otherTmds_name = "";
		if(otherTmds.getName() != null)
			otherTmds_name = otherTmds.getName().toLowerCase();

		String otherTmds_kegg = "";
		if(otherTmds.getKegg_name() != null)
			otherTmds_kegg = otherTmds.getKegg_name().toLowerCase();

		String otherTmds_chebi = "";
		if(otherTmds.getChebi_name() != null)
			otherTmds_chebi = otherTmds.getChebi_name().toLowerCase();

		if((name.equalsIgnoreCase(otherTmds_name)) 
				|| (name.equalsIgnoreCase(otherTmds_kegg))
				|| (name.equalsIgnoreCase(otherTmds_chebi)) 
				|| (this.synonyms!=null 
				&& 
				(this.synonyms.contains(name.toLowerCase()) ||
						this.synonyms.contains(kegg.toLowerCase()) ||
						this.synonyms.contains(chebi.toLowerCase())
						))) {

			sameName = true;
		}
		
		if((otherTmds_name.equalsIgnoreCase(name)) 
				|| (otherTmds_kegg.equalsIgnoreCase(name))
				|| (otherTmds_chebi.equalsIgnoreCase(name)) 
				|| (otherTmds.getSynonyms()!=null 
				&&
				(otherTmds.getSynonyms().contains(name) ||
						otherTmds.getSynonyms().contains(kegg) ||
						otherTmds.getSynonyms().contains(chebi)
						))) {

			sameName = true;
		}

		if(sameName) {

			if(otherTmds.getDirection().equalsIgnoreCase(this.direction)) {

				if(otherTmds.getStoichiometry() == this.stoichiometry) {

					return true;
				}
			}
		}

		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "TransportMetaboliteDirectionStoichiometryContainer [name="
				+ name + ", direction=" + direction + ", stoichiometry="
				+ stoichiometry + ", reversible=" + reversible + ", kegg_name="
				+ kegg_name + ", chebi_name=" + chebi_name + ", synonyms="
				+ synonyms + ", kegg_miriam=" + kegg_miriam + ", chebi_miriam="
				+ chebi_miriam + "]";
	}

	/**
	 * @param chebi_miriam the chebi_miriam to set
	 */
	public void setChebi_miriam(String chebi_miriam) {
		this.chebi_miriam = chebi_miriam;
	}

	@Override
	public int compareTo(TransportMetaboliteDirectionStoichiometryContainer o) {

		System.err.println("Do not compare TransportMetaboliteDirectionStoichiometryContainer!\n Manually iterate it!");

		if(this.equals(o))
			return 0;

		else
			return this.getName().compareTo(o.getName());
		//return this.getName().concat("_").concat(this.getDirection()).concat("_").concat(this.getStoichiometry()+"").compareTo(o.getName().concat("_").concat(o.getDirection()).concat("_").concat(o.getStoichiometry()+""));
	}

	/**
	 * @param transportMetaboliteCodes
	 */
	public void setTransportMetaboliteCodes(TransportMetaboliteCodes transportMetaboliteCodes) {
		
		this.setKegg_name(transportMetaboliteCodes.getKegg_name());
		this.setKegg_miriam(transportMetaboliteCodes.getKegg_miriam());
		this.setChebi_name(transportMetaboliteCodes.getChebi_name());
		this.setChebi_miriam(transportMetaboliteCodes.getChebi_miriam());
		
	}

}
