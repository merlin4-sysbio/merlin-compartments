package pt.uminho.sysbio.common.transporters.core.compartments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import pt.uminho.sysbio.merlin.utilities.Pair;


/**
 * @author ODias
 *
 */
public class PSortResult {
	protected String entryId;
	protected String definition;
	protected Map<String, Double> probabilities;
	protected String prediction;
	protected int k;
	protected String sequence;
	protected Map<String, Object> features;
	private List<Pair<String, Double>> compartments;

	/**
	 * @param entryId
	 * @param definition
	 * @param probabilities
	 * @param prediciton
	 * @param k
	 * @param sequence
	 * @param features
	 */
	public PSortResult(String entryId, String definition,
			HashMap<String, Double> probabilities,
			String prediciton, int k, String sequence,
			HashMap<String, Object> features) {
		this.entryId = entryId;
		
		this.definition = definition;
		
		this.probabilities = probabilities;
		
		this.compartments = new ArrayList<Pair<String,Double>>();
		//this.probabilities=this.parseAbbreviationResult(this.probabilities);
		this.probabilities=this.orderMap(this.probabilities);
		
		for(String compartment : this.probabilities.keySet())
		{
			this.addCompartment(compartment, this.probabilities.get(compartment));
			
		}
		
		this.prediction = prediciton;
		//this.prediction=PSortResult.parseAbbreviation(this.prediction);
		
		this.k = k;
		
		this.sequence = sequence;
		
		this.features = features;

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

	/**
	 * @return
	 */
	public String getEntryId() {
		return entryId;
	}

	/**
	 * @return
	 */
	public String getDefinition() {
		return definition;
	}

	/**
	 * @return
	 */
	public Map<String, Double> getProbabilities() {
		return probabilities;
	}

	/**
	 * @return
	 */
	public String getPrediction() {
		return prediction;
	}

	/**
	 * @return
	 */
	public int getK() {
		return k;
	}

	/**
	 * @return
	 */
	public String getSequence() {
		return sequence;
	}

	/**
	 * @return
	 */
	public Map<String, Object> getFeatures() {
		return features;
	}
	
	
	/**
	 * @param compartmentID
	 * @param score
	 */
	public void addCompartment(String compartmentID, double score){
		this.compartments.add(new Pair<String, Double>(compartmentID,score));
	}

//	/**
//	 * @return
//	 */
//	private Map<String,Double> parseAbbreviationResult(Map<String,Double> map){
//		Map<String,Double> newMap = new TreeMap<String, Double>();
//		for(String abbreviation:map.keySet())
//		{
//			newMap.put(PSortResult.parseAbbreviation(abbreviation), map.get(abbreviation));
//		}
//		return newMap;
//	}
	
	/**
	 * @param map
	 * @return
	 */
	private Map<String,Double> orderMap(Map<String,Double> map){
		Map<String,Double> newMap = new TreeMap<String, Double>();
		List<Double> valuesSet=new ArrayList<Double>();
		valuesSet.addAll(new TreeSet<Double>(map.values()));
		Collections.sort(valuesSet);
		Collections.reverse(valuesSet);
		for(Double entry: valuesSet)
		{
			for(String key:map.keySet())
			{
				if(map.get(key).equals(entry))
				{
					newMap.put(key, entry);
				}
			}
		}
		return newMap;
	}
}
