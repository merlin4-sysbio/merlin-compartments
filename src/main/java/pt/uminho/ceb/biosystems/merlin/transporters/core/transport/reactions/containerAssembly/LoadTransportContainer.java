/**
 * 
 */
package pt.uminho.ceb.biosystems.merlin.transporters.core.transport.reactions.containerAssembly;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import biosynth.core.components.representation.basic.graph.Graph;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.components.GeneCI;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.components.MetaboliteCI;

/**
 * @author ODias
 *
 */
public class LoadTransportContainer extends Observable implements Observer {

	private static final Logger logger = LoggerFactory.getLogger(LoadTransportContainer.class);
	private ConcurrentHashMap<String, GeneCI> geneContainer;
	private ConcurrentHashMap<String, TransportReactionCI> reactionsContainer;
	private ConcurrentHashMap<String, MetaboliteCI> metabolitesContainer;
	private ConcurrentHashMap<String, ProteinFamiliesSet> genesProteinsContainer;
	private ConcurrentHashMap<String, String> reactionsToBeReplaced;
	private Map<String, Set<TransportReaction>> genesReactions;
	private ConcurrentLinkedDeque<String> genes;
	private AtomicBoolean cancel;
	private Map<String,String> genesLocusTag;
	ConcurrentHashMap<String, String> existingReactions;
	private Map<String, GenesMetabolitesTransportType> genesMetabolitesTransportTypeMap;
	private ConcurrentHashMap<String,TransportReaction> transportReactionsList;
	private Map<String, TransportMetabolite> transportMetabolites;
	private ConcurrentHashMap<String, Set<String>> metaboliteFunctionalParent_map;
	private ConcurrentHashMap<String,Set<TransportReaction>> ontologyReactions;
	private Map<String, Set<String>> metabolites_ontology;
	private Map<String, Set<String>> selectedGenesMetabolites;
	private Map<String, ProteinFamiliesSet> genesProteins;
	private boolean saveOnlyReactionsWithKEGGmetabolites;
	private AtomicInteger geneProcessingCounter;
	private Map<String,String> metabolitesFormula;
	private Graph<String, String> graph;
	private Map<String, Map<String, Integer>> metabolite_generation;
	private  Map<String, Map<String, Map<String, MetabolitesOntology>>> reactions_metabolites_ontology;
	private Map<String, String> kegg_miriam, chebi_miriam;
	private Set<String> ignoreSymportMetabolites;
	private Map<String, Set<String>> rejectedGenesMetabolites;
	
	/**
	 * @param genesReactions
	 * @param cancel
	 * @param genesLocusTag
	 * @param genesMetabolitesTransportTypeMap
	 * @param selectedGenesMetabolites
	 * @param rejectedGenesMetabolites 
	 * @param genesProteins
	 * @param transportMetabolites
	 * @param metabolites_ontology
	 * @param metabolitesFormula
	 * @param saveOnlyReactionsWithKEGGmetabolites
	 * @param counter
	 * @param graph
	 * @param kegg_miriam
	 * @param chebi_miriam
	 * @param ignoreSymportMetabolites
	 */
	public LoadTransportContainer(Map<String, Set<TransportReaction>> genesReactions, AtomicBoolean cancel, Map<String,String> genesLocusTag,
			Map<String, GenesMetabolitesTransportType> genesMetabolitesTransportTypeMap, Map<String, Set<String>> selectedGenesMetabolites,
			Map<String, Set<String>> rejectedGenesMetabolites, Map<String, ProteinFamiliesSet> genesProteins, Map<String, TransportMetabolite> transportMetabolites, 
			Map<String, Set<String>> metabolites_ontology, Map<String,String> metabolitesFormula, 
			boolean saveOnlyReactionsWithKEGGmetabolites, AtomicInteger counter, Graph<String, String> graph,
			Map<String, String> kegg_miriam, Map<String, String> chebi_miriam, Set<String> ignoreSymportMetabolites) {
		
		this.setGenesReactions(genesReactions);
		
		this.setGenes(new ConcurrentLinkedDeque<String>(this.getGenesReactions().keySet()));

		this.setCancel(cancel);
		this.setGenesLocusTag(genesLocusTag);	
		this.setSelectedGenesMetabolites(selectedGenesMetabolites);
		this.setRejectedGenesMetabolites(rejectedGenesMetabolites);
		this.setGenesMetabolitesTransportTypeMap(genesMetabolitesTransportTypeMap);
		this.setGenesProteins(genesProteins);
		this.setTransportMetabolites(transportMetabolites);
		this.setSaveOnlyReactionsWithKEGGmetabolites(saveOnlyReactionsWithKEGGmetabolites);
		this.setGeneProcessingCounter(counter);
		this.setMetabolites_ontology(metabolites_ontology);
		
		this.setGeneContainer(new ConcurrentHashMap<String, GeneCI>());
		this.setReactionsContainer(new ConcurrentHashMap<String, TransportReactionCI>());
		this.setMetabolitesContainer(new ConcurrentHashMap<String, MetaboliteCI>());
		this.setGenesProteinsContainer(new ConcurrentHashMap<String, ProteinFamiliesSet>());
		this.setReactionsToBeReplaced(new ConcurrentHashMap<String, String>());
		this.setTransportReactionsList(new ConcurrentHashMap<String,TransportReaction>());
		this.setOntologyReactions(new ConcurrentHashMap<String,Set<TransportReaction>>());
		this.setMetaboliteFunctionalParent_map(new ConcurrentHashMap<String,Set<String>>());
		
		this.setKegg_miriam(kegg_miriam);
		this.setChebi_miriam(chebi_miriam);
		this.setMetabolitesFormula(metabolitesFormula);
		this.setGraph(graph);
		
		this.setMetabolite_generation(new ConcurrentHashMap<String, Map<String, Integer>> ()); 
		this.setReactions_metabolites_ontology(new ConcurrentHashMap<String, Map<String, Map<String, MetabolitesOntology>>>());
		this.existingReactions = new ConcurrentHashMap<>();
		this.ignoreSymportMetabolites = ignoreSymportMetabolites;
	}

	/**
	 * @param generations 
	 * @throws InterruptedException 
	 * 
	 */
	public void loadContainer(int generations) throws InterruptedException {
		
		int numberOfCores = Runtime.getRuntime().availableProcessors()*2;
		List<Thread> threads = new ArrayList<Thread>();
		
		if(this.getGenes().size()<numberOfCores)
			numberOfCores=this.getGenes().size();
		
		//numberOfCores=1;
		
		System.out.println("number Of threads: "+numberOfCores);
		
		for(int i=0; i<numberOfCores; i++) {

			Runnable lc	= new TransportContainerRunnable(genes, genesReactions, cancel, geneContainer, reactionsContainer, 
					metabolitesContainer, genesLocusTag, genesMetabolitesTransportTypeMap, reactionsToBeReplaced, transportReactionsList,
					transportMetabolites, metaboliteFunctionalParent_map, ontologyReactions, metabolites_ontology, selectedGenesMetabolites, this.rejectedGenesMetabolites,
					genesProteins, geneProcessingCounter, kegg_miriam, chebi_miriam, metabolitesFormula, saveOnlyReactionsWithKEGGmetabolites, this.graph,
					this.metabolite_generation, this.reactions_metabolites_ontology, existingReactions, ignoreSymportMetabolites, generations);

			((TransportContainerRunnable) lc).addObserver(this);
			Thread thread = new Thread(lc);
			threads.add(thread);
			System.out.println("Start "+i);
			thread.start();
		}

		for(Thread thread :threads)
			thread.join();
	}

	/**
	 * @return the reactionsContainer
	 */
	public ConcurrentHashMap<String, TransportReactionCI> getReactionsContainer() {
		return reactionsContainer;
	}

	/**
	 * @param reactionsContainer the reactionsContainer to set
	 */
	public void setReactionsContainer(ConcurrentHashMap<String, TransportReactionCI> reactionsContainer) {
		this.reactionsContainer = reactionsContainer;
	}

	/**
	 * @return the metabolitesContainer
	 */
	public ConcurrentHashMap<String, MetaboliteCI> getMetabolitesContainer() {
		return metabolitesContainer;
	}

	/**
	 * @param metabolitesContainer the metabolitesContainer to set
	 */
	public void setMetabolitesContainer(ConcurrentHashMap<String, MetaboliteCI> metabolitesContainer) {
		this.metabolitesContainer = metabolitesContainer;
	}

	/**
	 * @return the genesProteinsContainer
	 */
	public ConcurrentHashMap<String, ProteinFamiliesSet> getGenesProteinsContainer() {
		return genesProteinsContainer;
	}

	/**
	 * @param genesProteinsContainer the genesProteinsContainer to set
	 */
	public void setGenesProteinsContainer(ConcurrentHashMap<String, ProteinFamiliesSet> genesProteinsContainer) {
		this.genesProteinsContainer = genesProteinsContainer;
	}

	/**
	 * @return the reactionsToBeReplaced
	 */
	public ConcurrentHashMap<String, String> getReactionsToBeReplaced() {
		return reactionsToBeReplaced;
	}

	/**
	 * @param reactionsToBeReplaced the reactionsToBeReplaced to set
	 */
	public void setReactionsToBeReplaced(ConcurrentHashMap<String, String> reactionsToBeReplaced) {
		this.reactionsToBeReplaced = reactionsToBeReplaced;
	}

	/**
	 * @return the genesReactions
	 */
	public Map<String, Set<TransportReaction>> getGenesReactions() {
		return genesReactions;
	}

	/**
	 * @param genesReactions the genesReactions to set
	 */
	public void setGenesReactions(Map<String, Set<TransportReaction>> genesReactions) {
		this.genesReactions = genesReactions;
	}

	/**
	 * @return the genes
	 */
	public ConcurrentLinkedDeque<String> getGenes() {
		return genes;
	}

	/**
	 * @param genes the genes to set
	 */
	public void setGenes(ConcurrentLinkedDeque<String> genes) {
		this.genes = genes;
	}

	/**
	 * @return the cancel
	 */
	public AtomicBoolean getCancel() {
		return cancel;
	}

	/**
	 * @param cancel the cancel to set
	 */
	public void setCancel(AtomicBoolean cancel) {
		this.cancel = cancel;
	}

	@Override
	public void update(Observable arg0, Object arg1) {

		logger.debug("Counter on {}: {}", this.getClass(), this.geneProcessingCounter.get());
		setChanged();
		notifyObservers();
	}

	/**
	 * @return the transportReactionsList
	 */
	public ConcurrentHashMap<String,TransportReaction> getTransportReactionsList() {
		return transportReactionsList;
	}

	/**
	 * @param transportReactionsList the transportReactionsList to set
	 */
	public void setTransportReactionsList(ConcurrentHashMap<String,TransportReaction> transportReactionsList) {
		this.transportReactionsList = transportReactionsList;
	}

	/**
	 * @return the ontologyReactions
	 */
	public ConcurrentHashMap<String,Set<TransportReaction>> getOntologyReactions() {
		return ontologyReactions;
	}

	/**
	 * @param ontologyReactions the ontologyReactions to set
	 */
	public void setOntologyReactions(ConcurrentHashMap<String,Set<TransportReaction>> ontologyReactions) {
		this.ontologyReactions = ontologyReactions;
	}

	/**
	 * @return the metaboliteFunctionalParent_map
	 */
	public ConcurrentHashMap<String, Set<String>> getMetaboliteFunctionalParent_map() {
		return metaboliteFunctionalParent_map;
	}

	/**
	 * @param metaboliteFunctionalParent_map the metaboliteFunctionalParent_map to set
	 */
	public void setMetaboliteFunctionalParent_map(
			ConcurrentHashMap<String, Set<String>> metaboliteFunctionalParent_map) {
		this.metaboliteFunctionalParent_map = metaboliteFunctionalParent_map;
	}

	/**
	 * @return the geneContainer
	 */
	public ConcurrentHashMap<String, GeneCI> getGeneContainer() {
		return geneContainer;
	}

	/**
	 * @param geneContainer the geneContainer to set
	 */
	public void setGeneContainer(ConcurrentHashMap<String, GeneCI> geneContainer) {
		this.geneContainer = geneContainer;
	}

	/**
	 * @return the genesLocusTag
	 */
	public Map<String,String> getGenesLocusTag() {
		return genesLocusTag;
	}

	/**
	 * @param genesLocusTag the genesLocusTag to set
	 */
	public void setGenesLocusTag(Map<String,String> genesLocusTag) {
		this.genesLocusTag = genesLocusTag;
	}

	/**
	 * @return the genesMetabolitesTransportTypeMap
	 */
	public Map<String, GenesMetabolitesTransportType> getGenesMetabolitesTransportTypeMap() {
		return genesMetabolitesTransportTypeMap;
	}

	/**
	 * @param genesMetabolitesTransportTypeMap the genesMetabolitesTransportTypeMap to set
	 */
	public void setGenesMetabolitesTransportTypeMap(
			Map<String, GenesMetabolitesTransportType> genesMetabolitesTransportTypeMap) {
		this.genesMetabolitesTransportTypeMap = genesMetabolitesTransportTypeMap;
	}

	/**
	 * @return the transportMetabolites
	 */
	public Map<String, TransportMetabolite> getTransportMetabolites() {
		return transportMetabolites;
	}

	/**
	 * @param transportMetabolites the transportMetabolites to set
	 */
	public void setTransportMetabolites(Map<String, TransportMetabolite> transportMetabolites) {
		this.transportMetabolites = transportMetabolites;
	}

	/**
	 * @return the metabolites_ontology
	 */
	public Map<String, Set<String>> getMetabolites_ontology() {
		return metabolites_ontology;
	}

	/**
	 * @param metabolites_ontology the metabolites_ontology to set
	 */
	public void setMetabolites_ontology(Map<String, Set<String>> metabolites_ontology) {
		this.metabolites_ontology = metabolites_ontology;
	}

	/**
	 * @return the selectedGenesMetabolites
	 */
	public Map<String, Set<String>> getSelectedGenesMetabolites() {
		return selectedGenesMetabolites;
	}

	/**
	 * @param selectedGenesMetabolites the selectedGenesMetabolites to set
	 */
	public void setSelectedGenesMetabolites(Map<String, Set<String>> selectedGenesMetabolites) {
		this.selectedGenesMetabolites = selectedGenesMetabolites;
	}

	/**
	 * @return the saveOnlyReactionsWithKEGGmetabolites
	 */
	public boolean isSaveOnlyReactionsWithKEGGmetabolites() {
		return saveOnlyReactionsWithKEGGmetabolites;
	}

	/**
	 * @param saveOnlyReactionsWithKEGGmetabolites the saveOnlyReactionsWithKEGGmetabolites to set
	 */
	public void setSaveOnlyReactionsWithKEGGmetabolites(
			boolean saveOnlyReactionsWithKEGGmetabolites) {
		this.saveOnlyReactionsWithKEGGmetabolites = saveOnlyReactionsWithKEGGmetabolites;
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

	/**
	 * @return the metabolitesFormula
	 */
	public Map<String,String> getMetabolitesFormula() {
		return metabolitesFormula;
	}

	/**
	 * @param metabolitesFormula the metabolitesFormula to set
	 */
	public void setMetabolitesFormula(Map<String,String> metabolitesFormula) {
		this.metabolitesFormula = metabolitesFormula;
	}

	/**
	 * @return the counter
	 */
	public AtomicInteger getGeneProcessingCounter() {
		return geneProcessingCounter;
	}

	/**
	 * @param counter the counter to set
	 */
	public void setGeneProcessingCounter(AtomicInteger counter) {
		this.geneProcessingCounter = counter;
	}

	/**
	 * @return the graph
	 */
	public Graph<String, String> getGraph() {
		return graph;
	}

	/**
	 * @param graph the graph to set
	 */
	public void setGraph(Graph<String, String> graph) {
		this.graph = graph;
	}

	/**
	 * @return the metabolite_generation
	 */
	public Map<String, Map<String, Integer>> getMetabolite_generation() {
		return metabolite_generation;
	}

	/**
	 * @param metabolite_generation the metabolite_generation to set
	 */
	public void setMetabolite_generation(Map<String, Map<String, Integer>> metabolite_generation) {
		this.metabolite_generation = metabolite_generation;
	}

	/**
	 * @return the reactions_metabolites_ontology
	 */
	public  Map<String, Map<String, Map<String, MetabolitesOntology>>> getReactions_metabolites_ontology() {
		return reactions_metabolites_ontology;
	}

	/**
	 * @param reactions_metabolites_ontology the reactions_metabolites_ontology to set
	 */
	public void setReactions_metabolites_ontology(
			 Map<String, Map<String, Map<String, MetabolitesOntology>>> reactions_metabolites_ontology) {
		this.reactions_metabolites_ontology = reactions_metabolites_ontology;
	}

	/**
	 * @return the kegg_miriam
	 */
	public Map<String, String> getKegg_miriam() {
		return kegg_miriam;
	}

	/**
	 * @param kegg_miriam the kegg_miriam to set
	 */
	public void setKegg_miriam(Map<String, String> kegg_miriam) {
		this.kegg_miriam = kegg_miriam;
	}

	/**
	 * @return the chebi_miriam
	 */
	public Map<String, String> getChebi_miriam() {
		return chebi_miriam;
	}

	/**
	 * @param chebi_miriam the chebi_miriam to set
	 */
	public void setChebi_miriam(Map<String, String> chebi_miriam) {
		this.chebi_miriam = chebi_miriam;
	}
	
	private void setRejectedGenesMetabolites(Map<String, Set<String>> rejectedGenesMetabolites) {
		
		this.rejectedGenesMetabolites = rejectedGenesMetabolites;
	}

	
}
