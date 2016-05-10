package pt.uminho.sysbio.common.transporters.core.compartments;

import java.util.List;

import pt.uminho.sysbio.merlin.utilities.Pair;

/**
 * @author Oscar Dias
 *
 */
public interface CompartmentResult {

	public void setGeneID(String string);
	
	public String getGeneID();
	
	public void addCompartment(String compartmentID, double score);

	public List<Pair<String, Double>> getCompartments();
	
}
