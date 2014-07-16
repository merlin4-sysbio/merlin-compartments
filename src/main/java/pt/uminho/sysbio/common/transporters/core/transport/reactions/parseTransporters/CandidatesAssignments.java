/**
 * 
 */
package pt.uminho.sysbio.common.transporters.core.transport.reactions.parseTransporters;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import pt.uminho.sysbio.common.transporters.core.transport.reactions.parseTransporters.MetabolitesEntry.TransportType;
import uk.ac.ebi.kraken.interfaces.uniprot.NcbiTaxon;

/**
 * @author ODias
 *
 */
public class CandidatesAssignments {

	private String gene;
	private List<TransporterEntry> transporterEntryList;
	//metabolites
	private Map<String, Double> metabolitesCounter;
	private Map<String, Integer> metabolitesTaxonomyRankSum;
	private Map<String, Double> metabolitesScore;
	//types
	private Map<String, Map<TransportType,Integer>> metabolitesTransportTypeCounter;
	private Map<String,Map<TransportType, Integer>> metabolitesTransportTypeTaxonomyRankSum;
	private Map<String, Map<TransportType, Double>> metabolitesTransportTypeScore;
	private double totalNumberOfMetabolites;
	
	/**
	 * @param gene
	 * @param comments
	 */
	public CandidatesAssignments(String gene) {
		this.setGene(gene);
		this.transporterEntryList=new ArrayList<TransporterEntry>();
		this.metabolitesCounter= new TreeMap<String, Double>();
		this.metabolitesTaxonomyRankSum= new TreeMap<String, Integer>();
		this.metabolitesScore= new TreeMap<String, Double>();
		this.totalNumberOfMetabolites=0;
		this.metabolitesTransportTypeCounter = new TreeMap<String, Map<TransportType,Integer>>();
		this.metabolitesTransportTypeTaxonomyRankSum = new TreeMap<String, Map<TransportType,Integer>>();
		this.setMetabolitesTransportTypeScore(new TreeMap<String, Map<TransportType,Double>>());
	}

	/**
	 * @param alpha value to calculate scores
	 * @param transporterEntry
	 * @param taxonomy 
	 */
	public void addTransporterEntry(double alpha, TransporterEntry transporterEntry, List<NcbiTaxon> originTaxonomy) {
		
		for(String metabolite: transporterEntry.getMetabolites()) {
			
			this.totalNumberOfMetabolites+=1;
			this.totalNumberOfMetabolites+=transporterEntry.getSimilarity();
			double counter = 0; int taxRankSum = 0;
			Map<TransportType,Integer> transportType = new TreeMap<TransportType,Integer>();
			Map<TransportType,Integer> transportTypeTaxonomyRankSum = new TreeMap<TransportType,Integer>();
		
			if(this.metabolitesCounter.containsKey(metabolite)) {
				
				counter= this.metabolitesCounter.get(metabolite);
				taxRankSum= this.metabolitesTaxonomyRankSum.get(metabolite);
				transportType=this.metabolitesTransportTypeCounter.get(metabolite);
				transportTypeTaxonomyRankSum=this.metabolitesTransportTypeTaxonomyRankSum.get(metabolite);
			}
			//metabolites counter
			counter+=1;
			counter+=transporterEntry.getSimilarity();
			this.metabolitesCounter.put(metabolite,counter);

			//tax rank counter
			taxRankSum+=transporterEntry.getTaxonomyRanking();
			this.metabolitesTaxonomyRankSum.put(metabolite, taxRankSum);
			
			//metabolite specific direction counter
			for(TransportType type:transporterEntry.getMetabolitesTransportType())
			{
				int scoreType=0;
				int transportTypeTaxRank=0;
				if(transportType.containsKey(type))
				{
					scoreType=transportType.get(type);
					transportTypeTaxRank=transportTypeTaxonomyRankSum.get(type);
				}
				scoreType+=1;
				transportType.put(type,scoreType);
				
				transportTypeTaxRank+=transporterEntry.getTaxonomyRanking();
				transportTypeTaxonomyRankSum.put(type, transportTypeTaxRank);
			}
			this.metabolitesTransportTypeTaxonomyRankSum.put(metabolite,transportTypeTaxonomyRankSum);
			this.metabolitesTransportTypeCounter.put(metabolite,transportType);
		}
		transporterEntryList.add(transporterEntry);
		this.calculateMetabolitesScore(alpha, originTaxonomy);
		this.calculateTransporterTypeScore(alpha,originTaxonomy);
	}

	/**
	 * @param alpha
	 * @param originTaxonomy
	 */
	private void calculateMetabolitesScore(double alpha, List<NcbiTaxon> originTaxonomy) {
		
		for(String metabolite: this.metabolitesCounter.keySet()) {
			
			double availableHits=this.metabolitesCounter.get(metabolite);
			double frequency = availableHits/this.totalNumberOfMetabolites;
			double taxonomy =  this.metabolitesTaxonomyRankSum.get(metabolite)/(availableHits*originTaxonomy.size()*this.penaltyCost(availableHits, 2.0, 0.15));
			double score = (alpha*frequency)+((1-alpha)*taxonomy);
			this.metabolitesScore.put(metabolite,formatDoubleValue(score));
		}
	}

	/**
	 * @param alpha
	 * @param originTaxonomy
	 */
	private void calculateTransporterTypeScore(double alpha, List<NcbiTaxon> originTaxonomy) {
		
		for(String metabolite: this.metabolitesTransportTypeCounter.keySet()) {
			
			Map<TransportType,Double> transportTypeScores = new TreeMap<TransportType, Double>();
			int total = this.collectionSumInt(metabolitesTransportTypeCounter.get(metabolite).values());
			
			for(TransportType type: this.metabolitesTransportTypeCounter.get(metabolite).keySet()) {
				
				double availableTypes=this.metabolitesTransportTypeCounter.get(metabolite).get(type);
				double frequency = availableTypes/total;
				double taxonomy =  this.metabolitesTransportTypeTaxonomyRankSum.get(metabolite).get(type)/(availableTypes*originTaxonomy.size()*this.penaltyCost(availableTypes, 2.0, 0.05));
				double score = (alpha*frequency+(1-alpha)*taxonomy);
//				if(score>1)
//				{
//					System.out.println(metabolitesTransportTypeCounter);
//					System.out.println(metabolite);
//					System.out.println("tax "+ taxonomy);
//					System.out.println("rank sum "+ this.metabolitesTransportTypeTaxonomyRankSum.get(metabolite).get(type));
//					System.out.println("origin "+ originTaxonomy.size());
//					System.out.println("times freq"+ availableTypes);
//					System.out.println("score "+score);
//					System.out.println(metabolite +" frequency "+frequency);
//					System.out.println(metabolite +" taxonomy "+taxonomy);
//					System.out.println(type +" frequency "+frequency*alpha);
//					System.out.println(type +" taxonomy "+taxonomy*alpha);
//				}
				transportTypeScores.put(type, formatDoubleValue(score));
			}
			this.metabolitesTransportTypeScore.put(metabolite,transportTypeScores);
		}
	}
	
	/**
	 * @return
	 */
	public double getAverage() {
		
		double sum=0;
		for(double scores:this.getMetabolitesScore().values()) {
			
            sum+=scores;
        }
		return sum/this.getMetabolitesScore().values().size();
	}
	
	/**
	 * @return
	 */
	public Set<String> getAboveAverageMetabolites() {
		
		Set<String> aboveAverage=new TreeSet<String>();
		
		for(String metabolite:this.getMetabolitesScore().keySet()) {
			
			if(this.getMetabolitesScore().get(metabolite)>= this.getAverage()) {
				
				aboveAverage.add(metabolite);
			}
		}
		return aboveAverage;
	}
	
//	public Map<String, String> getTransportTypes(Set<String> metabolites){
//		depois de escolhidos os metabolitos escolher o modo como eles podem ser transportados e passa-los, para o mysql, VERIFICAR MYSQL
//		
//	}

	/**
	 * @return the transporterEntryList
	 */
	public List<TransporterEntry> getTransporterEntryList() {
		return transporterEntryList;
	}

	/**
	 * @param transporterEntryList the transporterEntryList to set
	 */
	public void setTransporterEntryList(List<TransporterEntry> transporterEntryList) {
		this.transporterEntryList = transporterEntryList;
	}

	/**
	 * @return the metabolitesCounter
	 */
	public Map<String, Double> getMetabolitesCounter() {
		return metabolitesCounter;
	}

	/**
	 * @param metabolitesCounter the metabolitesCounter to set
	 */
	public void setMetabolitesCounter(Map<String, Double> metabolitesCounter) {
		this.metabolitesCounter = metabolitesCounter;
	}

	/**
	 * @return the metabolitesTaxonomyRankSum
	 */
	public Map<String, Integer> getMetabolitesTaxonomyRankSum() {
		return metabolitesTaxonomyRankSum;
	}

	/**
	 * @param metabolitesTaxonomyRankSum the metabolitesTaxonomyRankSum to set
	 */
	public void setMetabolitesTaxonomyRankSum(
			Map<String, Integer> metabolitesTaxonomyRankSum) {
		this.metabolitesTaxonomyRankSum = metabolitesTaxonomyRankSum;
	}

	/**
	 * @return
	 */
	public Map<String, Double> getMetabolitesScore() {
		return metabolitesScore;
	}

	/**
	 * @param metabolitesScore
	 */
	public void setMetabolitesScore(Map<String, Double> metabolitesScore) {
		this.metabolitesScore = metabolitesScore;
	}


	/**
	 * @return the metabolitesTransportType
	 */
	public Map<String, Map<TransportType, Integer>> getMetabolitesTransportType() {
		return metabolitesTransportTypeCounter;
	}

	/**
	 * @param metabolitesTransportType the metabolitesTransportType to set
	 */
	public void setMetabolitesTransportType(
			Map<String, Map<TransportType, Integer>> metabolitesTransportType) {
		this.metabolitesTransportTypeCounter = metabolitesTransportType;
	}

	/**
	 * @param metabolitesTransportTypeScore the metabolitesTransportTypeScore to set
	 */
	public void setMetabolitesTransportTypeScore(
			Map<String, Map<TransportType, Double>> metabolitesTransportTypeScore) {
		this.metabolitesTransportTypeScore = metabolitesTransportTypeScore;
	}

	/**
	 * @return the metabolitesTransportTypeScore
	 */
	public Map<String, Map<TransportType, Double>> getMetabolitesTransportTypeScore() {
		return metabolitesTransportTypeScore;
	}
	
	/**
	 * @param score
	 * @return
	 */
	public double formatDoubleValue(double score){
		DecimalFormatSymbols separator = new DecimalFormatSymbols();
		separator.setDecimalSeparator('.');
		DecimalFormat format = new DecimalFormat("##.##",separator);
		return Double.parseDouble(format.format(score));
	}

	/**
	 * @param availableHits
	 * @param definedNumberOfHits
	 * @param betaPenalty
	 * @return
	 */
	public double penaltyCost(double availableHits, double definedNumberOfHits, double betaPenalty){
		if(availableHits>definedNumberOfHits){availableHits=definedNumberOfHits;}
		return (1 - (definedNumberOfHits-availableHits)* betaPenalty);
	}

	/**
	 * @param scores
	 * @return
	 */
	private int collectionSumInt(Collection<Integer> scores){
		int sum =0;
		for(int score:scores){sum+=score;}
		return sum;
	}
	
	/**
	 * @param scores
	 * @return
	 */
	private double collectionSumDouble(Collection<Double> scores){
		double sum =0;
		for(double score:scores){sum+=score;}
		return sum;
	}

	/**
	 * @param gene the gene to set
	 */
	public void setGene(String gene) {
		this.gene = gene;
	}

	/**
	 * @return the gene
	 */
	public String getGene() {
		return gene;
	}
	
	
	/**
	 * @param valuesSet
	 * @return
	 */
	public Map<String, Double> getPercentagesMetabolite(Map<String, Double> valuesSet){
		Map<String, Double> percentages = new TreeMap<String, Double>();
		double total = this.collectionSumDouble(valuesSet.values());
		
		for(String key: valuesSet.keySet())
		{
			percentages.put(key,formatDoubleValue((valuesSet.get(key)/total)*100));
		}
		return percentages;		
	}
	
	/**
	 * @param valuesSet
	 * @return
	 */
	public Map<TransportType, Double> getPercentagesTransportType(Map<TransportType, Double> valuesSet){
		Map<TransportType, Double> percentages = new TreeMap<TransportType, Double>();
		double total = this.collectionSumDouble(valuesSet.values());
		
		for(TransportType key: valuesSet.keySet())
		{
			percentages.put(key,formatDoubleValue((valuesSet.get(key)/total)*100));
		}
		return percentages;		
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "TransportProteinAssignments\n" +
		"\t[" +
		"\n\tgene=" + gene
		+ ",\n\ttransporterEntryList=" + transporterEntryList
		+ ",\n\tmetabolitesCounter=" + metabolitesCounter
		+ ",\n\tmetabolitesTaxonomyRankSum=" + metabolitesTaxonomyRankSum
		+ ",\n\tmetabolitesScore=" + metabolitesScore
		+ "\n\t]";
	}

}
