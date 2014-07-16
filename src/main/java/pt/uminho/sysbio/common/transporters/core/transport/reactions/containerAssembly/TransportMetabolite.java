
package pt.uminho.sysbio.common.transporters.core.transport.reactions.containerAssembly;

/**
 * @author ODias
 *
 */
public class TransportMetabolite {

	private String metaboliteID,  name,  keggMiriam, chEBIMiriam;
	
	/**
	 * @param metaboliteID
	 * @param name
	 * @param keggMiriam
	 * @param chEBIMiriam
	 */
	public TransportMetabolite(String metaboliteID, String name, String keggMiriam, String chEBIMiriam) {
		this.setMetaboliteID(metaboliteID);
		this.setName(name);
		this.setKeggMiriam(keggMiriam);
		this.setChEBIMiriam(chEBIMiriam);
	}

	/**
	 * @param metaboliteID the metaboliteID to set
	 */
	public void setMetaboliteID(String metaboliteID) {
		this.metaboliteID = metaboliteID;
	}

	/**
	 * @return the metaboliteID
	 */
	public String getMetaboliteID() {
		return metaboliteID;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param keggMiriam the keggMiriam to set
	 */
	public void setKeggMiriam(String keggMiriam) {
		this.keggMiriam = keggMiriam;
	}

	/**
	 * @return the keggMiriam
	 */
	public String getKeggMiriam() {
		return keggMiriam;
	}

	/**
	 * @param chEBIMiriam the chEBIMiriam to set
	 */
	public void setChEBIMiriam(String chEBIMiriam) {
		this.chEBIMiriam = chEBIMiriam;
	}

	/**
	 * @return the chEBIMiriam
	 */
	public String getChEBIMiriam() {
		return chEBIMiriam;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "TransportMetabolite [metaboliteID=" + metaboliteID + ", name="
				+ name + ", keggMiriam=" + keggMiriam + ", chEBIMiriam="
				+ chEBIMiriam + "]";
	}


}
