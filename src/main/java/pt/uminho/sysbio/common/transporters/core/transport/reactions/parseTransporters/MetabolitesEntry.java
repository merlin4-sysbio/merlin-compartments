package pt.uminho.sysbio.common.transporters.core.transport.reactions.parseTransporters;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ODias
 *
 */
public class MetabolitesEntry {

	private List<String> metabolites;
	private List<String> directions;
	private TransportType transportType;
	private List<Integer> stoichiometries;

	/**
	 * @param transportType
	 */
	public MetabolitesEntry(TransportType transportType) {
		this.metabolites=new ArrayList<String>();
		this.directions=new ArrayList<String>();
		this.stoichiometries=new ArrayList<Integer>();
		this.setTransportType(transportType);
	}


	/**
	 * @param metabolite
	 * @param direction
	 */
	public void addMetabolites(String metabolite, String direction, int stoichiometry){
		metabolites.add(metabolite);
		directions.add(direction);
		stoichiometries.add(stoichiometry);
	}


	/**
	 * @return the metabolites
	 */
	public List<String> getMetabolites() {
		return metabolites;
	}


	/**
	 * @param metabolites the metabolites to set
	 */
	public void setMetabolites(List<String> metabolites) {
		this.metabolites = metabolites;
	}


	/**
	 * @return the directions
	 */
	public List<String> getDirections() {
		return directions;
	}


	/**
	 * @param directions the directions to set
	 */
	public void setDirections(List<String> directions) {
		this.directions = directions;
	}

	/**
	 * @param transportType the transportType to set
	 */
	public void setTransportType(TransportType transportType) {
		this.transportType = transportType;
	}


	/**
	 * @return the transportType
	 */
	public TransportType getTransportType() {
		return transportType;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "\n" +
		"\t\t\t\t\tTransportedMetabolitesEntry" +
		"\n\t\t\t\t\t[" +
		"\n\t\t\t\t\t\tmetabolites=" + metabolites +
		",\n\t\t\t\t\t\tdirections=" + directions + 
		",\n\t\t\t\t\t\ttransportType="+ transportType +
		"\n\t\t\t\t\t]";
	}

	/**
	 * @author ODias
	 *
	 */
	public enum TransportType
	{
		symport(0),
		antiport(0),
		//simple,
		complex(0),
		transport(2),
		influx(0),
		sensor (3),
		efflux(1);

		private int transport_type;
		private TransportType(int transport_type){
			this.transport_type = transport_type;
		}

		public int getTransport(){
			return this.transport_type;
		}
	}

	/**
	 * @param direction
	 * @return
	 */
	public static String getDirection(int direction){
		String [] directionArray=new String[6];
		directionArray[0]="in";
		directionArray[1]="out";
		directionArray[2]="in_out";
		directionArray[3]="sensor";
		directionArray[4]="reactant";
		directionArray[5]="product";
		return directionArray[direction];

	}


	/**
	 * @return the stoichiometries
	 */
	public List<Integer> getStoichiometries() {
		return stoichiometries;
	}


	/**
	 * @param stoichiometries the stoichiometries to set
	 */
	public void setStoichiometries(List<Integer> stoichiometries) {
		this.stoichiometries = stoichiometries;
	}


}
