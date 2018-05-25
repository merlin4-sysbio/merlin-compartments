/**
 * 
 */
package pt.uminho.ceb.biosystems.merlin.transporters.core.transport.reactions.annotateTransporters;

/**
 * @author ODias
 *
 */
public class UnnannotatedTransportersContainer implements Comparable<UnnannotatedTransportersContainer> {
	
	private String uniprot_id;
	private String tcnumber;

	/**
	 * 
	 */
	public UnnannotatedTransportersContainer() {
	}

	/**
	 * @param uniprot_id
	 * @param tcnumber
	 */
	public UnnannotatedTransportersContainer(String uniprot_id, String tcnumber) {
		this.uniprot_id = uniprot_id;
		this.tcnumber = tcnumber;
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
	 * @return the tcnumber
	 */
	public String getTcnumber() {
		return tcnumber;
	}

	/**
	 * @param tcnumber the tcnumber to set
	 */
	public void setTcnumber(String tcnumber) {
		this.tcnumber = tcnumber;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((tcnumber == null) ? 0 : tcnumber.hashCode());
		result = prime * result
				+ ((uniprot_id == null) ? 0 : uniprot_id.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		UnnannotatedTransportersContainer other = (UnnannotatedTransportersContainer) obj;
		if (tcnumber == null) {
			if (other.tcnumber != null) {
				return false;
			}
		} else if (!tcnumber.equals(other.tcnumber)) {
			return false;
		}
		if (uniprot_id == null) {
			if (other.uniprot_id != null) {
				return false;
			}
		} else if (!uniprot_id.equals(other.uniprot_id)) {
			return false;
		}
		return true;
	}

	@Override
	public int compareTo(UnnannotatedTransportersContainer obj) {

		if(this.equals(obj))
		return 0;
		
		return this.uniprot_id.compareTo(obj.getUniprot_id());
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "UnnannotatedTransportersContainer [uniprot_id=" + uniprot_id
				+ ", tcnumber=" + tcnumber + "]";
	}
	
	

}
