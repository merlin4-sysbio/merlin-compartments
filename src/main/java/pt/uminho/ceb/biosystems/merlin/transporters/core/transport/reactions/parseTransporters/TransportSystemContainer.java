package pt.uminho.ceb.biosystems.merlin.transporters.core.transport.reactions.parseTransporters;

import java.util.List;

import pt.uminho.ceb.biosystems.merlin.transporters.core.transport.reactions.containerAssembly.TransportMetaboliteDirectionStoichiometryContainer;

/**
 * @author ODias
 *
 */
public class TransportSystemContainer implements Comparable<TransportSystemContainer> {

	private Integer id;
	private boolean reversibility;
	private List<TransportMetaboliteDirectionStoichiometryContainer> metabolites;

	/**
	 * @param id
	 * @param reversibility
	 */
	public TransportSystemContainer(int id, boolean reversibility) {

		this.setId(id);
		this.setReversibility(reversibility);
	}
	
	/**
	 * @param metabolites
	 * @param reversibility
	 */
	public TransportSystemContainer(List<TransportMetaboliteDirectionStoichiometryContainer> metabolites, boolean reversibility) {
		
		this.metabolites = metabolites;
		this.reversibility = reversibility;
	}
	
	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * @return the metabolites
	 */
	public List<TransportMetaboliteDirectionStoichiometryContainer> getMetabolites() {
		return metabolites;
	}

	/**
	 * @param metabolites the metabolites to set
	 */
	public void setMetabolites(List<TransportMetaboliteDirectionStoichiometryContainer> metabolites) {
		this.metabolites = metabolites;
	}

	/**
	 * @return the reversibility
	 */
	public boolean isReversibility() {
		return reversibility;
	}

	/**
	 * @param reversibility the reversibility to set
	 */
	public void setReversibility(boolean reversibility) {
		this.reversibility = reversibility;
	}
	
	@Override
	public boolean equals(Object obj) {

		if((obj == null) || (obj.getClass() != this.getClass()))
			return false;

		TransportSystemContainer other_tsc = (TransportSystemContainer) obj;
		
		boolean equals = false;
		
		if(other_tsc.isReversibility() == this.reversibility) {
			
			for(TransportMetaboliteDirectionStoichiometryContainer metaboliteDirectionStoichiometryContainer : this.metabolites)
				if(!other_tsc.getMetabolites().contains(metaboliteDirectionStoichiometryContainer))					
					equals = false;
			
			for(TransportMetaboliteDirectionStoichiometryContainer metaboliteDirectionStoichiometryContainer : other_tsc.getMetabolites())				
				if(!this.metabolites.contains(metaboliteDirectionStoichiometryContainer))					
					equals = false;
		}
		return equals;
	}

	@Override
	public int compareTo(TransportSystemContainer other_tsc) {

		if(this.equals(other_tsc))
			return 0;
		
		return this.id.compareTo(other_tsc.getId());
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "TransportSystemContainer [id=" + id + ", reversibility="
				+ reversibility + ", metabolites=" + metabolites + "]";
	}
}
