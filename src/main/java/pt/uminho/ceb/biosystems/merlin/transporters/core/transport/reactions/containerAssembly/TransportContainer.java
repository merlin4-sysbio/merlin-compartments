package pt.uminho.ceb.biosystems.merlin.transporters.core.transport.reactions.containerAssembly;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import pt.uminho.ceb.biosystems.merlin.utilities.External.ExternalRefSource;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.Container;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.components.CompartmentCI;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.components.GeneCI;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.components.MetaboliteCI;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.components.ReactionCI;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.components.ReactionTypeEnum;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.components.StoichiometryValueCI;
import pt.uminho.ceb.biosystems.mew.utilities.datastructures.map.MapUtils;

public class TransportContainer extends Container {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private boolean keggMetabolitesReactions = false;
	protected Map<String, String> keggMiriam;
	protected Map<String, String> chebiMiriam;
	protected Map<String, ProteinFamiliesSet> genesProteins;
	private double alpha;
	private int minimalFrequency;
	private double threshold;
	private double beta;
	private boolean reactionsValidated;
	protected Map<String, TransportReactionCI> reactions; 

	/**
	 * 
	 */
	public TransportContainer() {

		this.reactions = new TreeMap<String, TransportReactionCI>();
		this.metabolites= new TreeMap<String, MetaboliteCI>();
		this.compartments= new TreeMap<String, CompartmentCI>();
		this.genes= new TreeMap<String, GeneCI>();
		this.keggMiriam= new TreeMap<String, String>();
		this.chebiMiriam= new TreeMap<String, String>();
		this.setAlpha(-1);
		this.setMinimalFrequency(-1);
		this.setThreshold(-1);
		this.setBeta(-1);
		this.setReactionsValidated(false);
	}

	/**
	 * @param alpha
	 * @param minimalFrequency
	 * @param threshold
	 * @param beta
	 */
	public TransportContainer(double alpha, int minimalFrequency, double threshold, double beta) {

		this.reactions = new TreeMap<String, TransportReactionCI>();
		this.metabolites= new TreeMap<String, MetaboliteCI>();
		this.compartments= new TreeMap<String, CompartmentCI>();
		this.genes= new TreeMap<String, GeneCI>();
		this.keggMiriam= new TreeMap<String, String>();
		this.chebiMiriam= new TreeMap<String, String>();
		this.setAlpha(alpha);
		this.setMinimalFrequency(minimalFrequency);
		this.setThreshold(threshold);
		this.setBeta(beta);
	}

	/**
	 * @param reactions
	 * @param metabolites
	 * @param compartments
	 * @param genes
	 * @param keggMiriam
	 * @param chebiMiriam
	 * @param alpha
	 * @param minimalFrequency
	 * @param threshold
	 * @param beta
	 */
	public TransportContainer(Map<String, TransportReactionCI> reactions, Map<String, MetaboliteCI> metabolites, Map<String, CompartmentCI> compartments,
			Map<String, GeneCI> genes, Map<String, String> keggMiriam, Map<String, String> chebiMiriam,
			Map<String, ProteinFamiliesSet> genesProteins, boolean keggMetabolitesReactions, boolean reactionsValidated,
			double alpha, int minimalFrequency, double threshold, double beta
			) {
		this.reactions = reactions;
		this.metabolites= metabolites;
		this.compartments= compartments;
		this.genes= genes;
		this.keggMiriam= keggMiriam;
		this.chebiMiriam= chebiMiriam;
		this.setAlpha(alpha);
		this.setMinimalFrequency(minimalFrequency);
		this.setThreshold(threshold);
		this.setBeta(beta);
		this.genesProteins = genesProteins;
		this.keggMetabolitesReactions = keggMetabolitesReactions;
		this.reactionsValidated = reactionsValidated;
	}

	/**
	 * @param geneId
	 * @param geneName
	 */
	public void addGeneCI(String geneID, String geneName) {

		GeneCI geneCI = new GeneCI(geneID, geneName);
		this.genes.put(geneID, geneCI);
	}

	/**
	 * @param metaboliteID
	 * @param name
	 * @param reactionID
	 * @param formula
	 */
	public void addMetaboliteCI(String metaboliteID, String name, String reactionID, String formula){

		MetaboliteCI metaboliteCI;

		if(this.metabolites.containsKey(metaboliteID)) {

			metaboliteCI=this.metabolites.get(metaboliteID);
		}
		else {

			metaboliteCI= new MetaboliteCI(metaboliteID, name);
		}

		metaboliteCI.addReaction(reactionID);

		if(formula!=null) {

			metaboliteCI.setFormula(formula);
		}
		this.metabolites.put(metaboliteID, metaboliteCI);
	}

	/**
	 * @param metaboliteID
	 * @param miriamKEGG
	 */
	public void addMetaboliteMiriamKEGG(String metaboliteID, String miriamKEGG){
		this.keggMiriam.put(metaboliteID, miriamKEGG);
	}

	/**
	 * @param metaboliteID
	 * @param miriamChEBI
	 */
	public void addMetaboliteMiriamChEBI(String metaboliteID, String miriamChEBI){
		this.chebiMiriam.put(metaboliteID, miriamChEBI);
	}


	/**
	 * @param shortName
	 * @param name
	 */
	public void addCompartments(String shortName, String name, String outside){
		CompartmentCI compartmentCI;
		if(this.compartments.containsKey(shortName))
		{
			compartmentCI=this.compartments.get(shortName);
		}
		else
		{
			compartmentCI = new CompartmentCI(shortName, name, outside);
		}
		this.compartments.put(shortName, compartmentCI);
	}

	/**
	 * @param id
	 * @param name
	 * @param reversible
	 * @param reactants
	 * @param products
	 * @param geneID
	 * @param proteinID
	 */
	public String addTransportReaction(String id, String name, boolean reversible, Map<String,StoichiometryValueCI> reactants, 
			Map<String,StoichiometryValueCI> products, String geneID, ProteinFamiliesSet proteinID){
		TransportReactionCI transportReactionCI;
		//		System.out.println("gene1: " + geneID);

		if(this.reactions.containsKey(id)) {

			transportReactionCI=this.reactions.get(id);
		}
		else {

			transportReactionCI = new TransportReactionCI(id, name, reversible, reactants, products);
			transportReactionCI.setType(ReactionTypeEnum.Transport);

			//reactionCI = this.verifyIfReactionExists(reactionCI);
		}

		transportReactionCI.addGene(geneID);
		proteinID.calculateTCfamily_score();
		
		if(proteinID.getTc_families_above_half()==null || proteinID.getTc_families_above_half().isEmpty())
			transportReactionCI.addProtein(proteinID.getMax_score_family());
		else
			for(String tc_family:proteinID.getTc_families_above_half().keySet()) 
				transportReactionCI.addProtein(tc_family);

		//		if(reactions.containsKey(id))
		//			System.err.println("ERRO");
		this.reactions.put(id, transportReactionCI);

		return transportReactionCI.getId();
	}

	/**
	 * @param reactionCI
	 * @return
	 */
	public TransportReactionCI verifyIfReactionExists(TransportReactionCI reactionCI) {


		Set<String> metabolites = reactionCI.getMetaboliteSetIds();

		Set<String> reactions = null;

		boolean firstIteration = true;

		for(String metabolite_id : metabolites) {

			if(firstIteration) {

				firstIteration = false;

				reactions = this.metabolites.get(metabolite_id).getReactionsId();
			}
			else {

				reactions.retainAll(this.metabolites.get(metabolite_id).getReactionsId());
			}
		}
		reactions.remove(reactionCI.getId());

		for(String reaction_id : reactions) {

			if(this.getReactions().containsKey(reaction_id)) {

				TransportReactionCI reaction = this.getTransportReactions().get(reaction_id);

				if(reactionCI.hasSameStoichiometry(reaction, true)) {

					return reaction;
				}
			}
		}
		return reactionCI;
	}

	/**
	 * @param id
	 * @param geneID
	 * @param proteinID
	 */
	public void addGenetoTransportReaction(String id, String geneID, ProteinFamiliesSet proteinID) {

		TransportReactionCI transportReactionCI = this.reactions.get(id);
		transportReactionCI.addGene(geneID);

		proteinID.calculateTCfamily_score();
		
		if(proteinID.getTc_families_above_half()==null || proteinID.getTc_families_above_half().isEmpty())
			transportReactionCI.addProtein(proteinID.getMax_score_family());
		else
			for(String tc_family:proteinID.getTc_families_above_half().keySet()) 
				transportReactionCI.addProtein(tc_family);
		
		this.reactions.put(id, transportReactionCI);
	}

	/**
	 * @return the genes
	 */
	public Map<String, GeneCI> getGenes() {
		return genes;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "TransportContainer [reactions=" + reactions + ", metabolites="
				+ metabolites + ", compartments=" + compartments + ", genes="
				+ genes + ", keggMiriam=" + keggMiriam + ", chebiMiriam="
				+ chebiMiriam + "]";
	}

	public void setTransportReactions(Map<String, TransportReactionCI> transportReactions) {

		this.reactions = transportReactions;
	}

	public Map<String, TransportReactionCI> getTransportReactions() {

		return this.reactions;
	}

	public void setReactions(Map<String, ReactionCI> reactions) {

		super.setReactions(reactions);
	}

	public Map<String, ReactionCI> getReactions() {

		super.setReactions(new HashMap<String, ReactionCI>());
		super.getReactions().putAll(this.reactions);
		return super.getReactions();
	}

	public Map<String, MetaboliteCI> getMetabolites() {
		return metabolites;
	}

	public Map<String, CompartmentCI> getCompartments() {
		return compartments;
	}

	public Map<String, String> getKeggMiriam() {
		return keggMiriam;
	}

	public Map<String, String> getChebiMiriam() {
		return chebiMiriam;
	}

	/**
	 * @return the genesProteins
	 */
	public Map<String, ProteinFamiliesSet> getGenesProteins() {
		return genesProteins;
	}

	/**
	 * @param genesProteins the genesProteins to set
	 */
	public void setGenesProteins(Map<String, ProteinFamiliesSet> genesProteins) {
		this.genesProteins = genesProteins;
	}

	public MetaboliteCI getMetabolite(String id){
		return metabolites.get(id);
	}

	public TransportReactionCI getReaction(String id){
		return reactions.get(id);
	}

	/**
	 * @return the alpha
	 */
	public double getAlpha() {
		return alpha;
	}

	/**
	 * @param alpha the alpha to set
	 */
	public void setAlpha(double alpha) {
		this.alpha = alpha;
	}

	public Set<String> identifyTransportReactions(String compartmentId){
		return this.reactions.keySet();
	}

	/**
	 * @return the minimalFrequency
	 */
	public int getMinimalFrequency() {
		return minimalFrequency;
	}

	/**
	 * @param minimalFrequency the minimalFrequency to set
	 */
	public void setMinimalFrequency(int minimalFrequency) {
		this.minimalFrequency = minimalFrequency;
	}

	/**
	 * @return the threshold
	 */
	public double getThreshold() {
		return threshold;
	}

	/**
	 * @param threshold the threshold to set
	 */
	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}

	/**
	 * @return the beta
	 */
	public double getBeta() {
		return beta;
	}

	/**
	 * @param beta the beta to set
	 */
	public void setBeta(double beta) {
		this.beta = beta;
	}

	/**
	 * @return
	 */
	public boolean isKeggMetabolitesReactions() {
		return this.keggMetabolitesReactions;
	}

	/**
	 * @param reactionsValidated
	 */
	public void setKeggMetabolitesReactions(boolean keggMetabolitesReactions){
		this.keggMetabolitesReactions = keggMetabolitesReactions;
	}


	/**
	 * @return
	 */
	public boolean isReactionsValidated() {
		return this.reactionsValidated;
	}

	/**
	 * @param reactionsValidated
	 */
	public void setReactionsValidated(boolean reactionsValidated){
		this.reactionsValidated = reactionsValidated;
	}

	/**
	 * @param keggMiriam the keggMiriam to set
	 */
	public void setKeggMiriam(Map<String, String> keggMiriam) {
		this.keggMiriam = keggMiriam;
	}

	/**
	 * @param chebiMiriam the chebiMiriam to set
	 */
	public void setChebiMiriam(Map<String, String> chebiMiriam) {
		this.chebiMiriam = chebiMiriam;
	}


	/**
	 * @param modelExtraInfo
	 * @return
	 * @throws Exception
	 */
	public TransportContainer filterTransportContainer (Map<String, String> modelExtraInfo) throws Exception {

		Map<String, TransportReactionCI> reactions_map = new TreeMap<String, TransportReactionCI>();
		Map<String, String> keggMiriam_map = new TreeMap<String, String>();
		Map<String, String> chebiMiriam_map = new TreeMap<String, String>();

		this.keggMiriam.keySet().retainAll(this.metabolites.keySet());

		Map<String, Set<String>> reverse_keggMiriam_map = MapUtils.revertMap(this.keggMiriam);

		Map<String, Set<String>> reverse_kegg_map = new HashMap<String,Set<String>>();

		for(String miriam : reverse_keggMiriam_map.keySet()) {

			String kegg_id = ExternalRefSource.KEGG_CPD.getSourceId(miriam);
			reverse_kegg_map.put(kegg_id, reverse_keggMiriam_map.get(miriam));
		}

		Map<String, String> filtered_keys = new TreeMap<String, String>();

		for(String key : reverse_kegg_map.keySet()) {

			if(reverse_kegg_map.get(key).size()==1){

				for(String m : reverse_kegg_map.get(key))
					filtered_keys.put(key, m);
			}
		}

		Map<String, Set<String>> reverse_modelExtraInfo = MapUtils.revertMap(modelExtraInfo);

		Set<String> model_metabolites_set = new TreeSet<String>();

		for(String key : reverse_modelExtraInfo.keySet()) {

			if(reverse_modelExtraInfo.get(key).size()==1){

				model_metabolites_set.add(key);
			}
		}

		filtered_keys.keySet().retainAll(model_metabolites_set);

		for(String kegg_id : filtered_keys.keySet()) {

			for(String metabolite_id : reverse_kegg_map.get(kegg_id)) {

				keggMiriam_map.put(metabolite_id, ExternalRefSource.KEGG_CPD.getMiriamCode(kegg_id));
				chebiMiriam_map.put(metabolite_id, this.chebiMiriam.get(metabolite_id));
				//metaboliteFormula.put(metabolite_id, this.metaboliteFormula.get(reverse_keggMiriam_map.get(kegg_id)));

				Set<String> newReactions = new HashSet<String>();

				MetaboliteCI metabolite = this.getMetabolite(metabolite_id);

				for(String reactionID : metabolite.getReactionsId()) {

					TransportReactionCI oldReaction = this.getTransportReactions().get(reactionID);

					if(oldReaction!=null && filtered_keys.values().containsAll(oldReaction.getMetaboliteSetIds())) {

						newReactions.add(oldReaction.getId());
					}
				}

				if(newReactions.size()>0) {

					for(String reactionID : newReactions) {

						reactions_map.put(reactionID, this.reactions.get(reactionID));

						//					for (String gene : this.reactions.get(reactionID).getGenesIDs()) {
						//						
						//						genes_map.put(gene, this.getGene(gene));
						//					}
					}

					//metabolite.getReactionsId().retainAll(newReactions);

					//metabolites_map.put(metabolite_id, metabolite);
				}
			}
		}

		TransportContainer returnTransportContainer = new TransportContainer(reactions_map, this.metabolites, this.compartments, this.genes, keggMiriam_map, chebiMiriam_map, 
				this.genesProteins, this.keggMetabolitesReactions, this.reactionsValidated,
				alpha, minimalFrequency, threshold, beta); 

		returnTransportContainer.verifyDepBetweenClass();

		return returnTransportContainer;
	}
	
}
