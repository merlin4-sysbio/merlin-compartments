package pt.uminho.ceb.biosystems.merlin.transporters.core.transport.reactions.containerAssembly;

import java.util.Set;

/**
 * @author Oscar
 *
 */
public class TransportMetaboliteCodes {
	
	private String name;
	private String kegg_name;
	private String chebi_name;
	private Set<String> synonyms;
	private String kegg_miriam;
	private String chebi_miriam;
	
	
	
	/**
	 * @param name
	 * @param kegg_name
	 * @param chebi_name
	 * @param synonyms
	 * @param kegg_miriam
	 * @param chebi_miriam
	 */
	public TransportMetaboliteCodes(String name, String kegg_name,
			String chebi_name, Set<String> synonyms, String kegg_miriam,
			String chebi_miriam) {
		super();
		this.name = name;
		this.kegg_name = kegg_name;
		this.chebi_name = chebi_name;
		this.synonyms = synonyms;
		this.kegg_miriam = kegg_miriam;
		this.chebi_miriam = chebi_miriam;
	}
	
	/**
	 * @param name
	 */
	public TransportMetaboliteCodes(String name) {
	
		this.setName(name);	
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
	/**
	 * @param chebi_miriam the chebi_miriam to set
	 */
	public void setChebi_miriam(String chebi_miriam) {
		this.chebi_miriam = chebi_miriam;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "TransportMetaboliteCodes [name=" + name + ", kegg_name="
				+ kegg_name + ", chebi_name=" + chebi_name + ", synonyms="
				+ synonyms + ", kegg_miriam=" + kegg_miriam + ", chebi_miriam="
				+ chebi_miriam + "]";
	}

}
