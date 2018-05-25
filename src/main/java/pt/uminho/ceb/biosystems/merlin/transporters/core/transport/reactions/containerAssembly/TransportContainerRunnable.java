/**
 * 
 */
package pt.uminho.ceb.biosystems.merlin.transporters.core.transport.reactions.containerAssembly;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import biosynth.core.algorithm.graph.Dijkstra;
import biosynth.core.components.representation.basic.graph.Graph;
import pt.uminho.ceb.biosystems.merlin.bioapis.externalAPI.ebi.chebi.ChebiAPIInterface;
import pt.uminho.ceb.biosystems.merlin.transporters.core.utils.TransportersUtilities;
import pt.uminho.ceb.biosystems.merlin.utilities.External.ExternalRefSource;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.components.GeneCI;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.components.MetaboliteCI;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.components.ReactionTypeEnum;
import pt.uminho.ceb.biosystems.mew.biocomponents.container.components.StoichiometryValueCI;

/**
 * @author ODias
 *
 */
public class TransportContainerRunnable extends Observable implements Runnable  {

	private static final Logger logger = LoggerFactory.getLogger(TransportContainerRunnable.class);
	private static boolean verbose = false;
	private ConcurrentLinkedDeque<String> genes;
	private Map<String, Set<TransportReaction>> genesReactions;
	private AtomicBoolean cancel;
	private ConcurrentHashMap<String, GeneCI> genesContainer;
	private ConcurrentHashMap<String, TransportReactionCI> reactionsContainer;
	private ConcurrentHashMap<String, MetaboliteCI> metabolitesContainer;
	private Map<String,String> genesLocusTag;
	private ConcurrentHashMap<String,String> existingReactions;
	private Map<String, GenesMetabolitesTransportType> genesMetabolitesTransportTypeMap;
	private ConcurrentHashMap<String, String> reactionsToBeReplaced;
	private ConcurrentHashMap<String,TransportReaction> transportReactionsList;
	private Map<String, TransportMetabolite> transportMetabolites;
	private ConcurrentHashMap<String, Set<String>> metaboliteFunctionalParent_map;
	private ConcurrentHashMap<String,Set<TransportReaction>> ontologyReactions;
	private Map<String, Set<String>> metabolites_ontology;
	private Map<String, Set<String>> selectedGenesMetabolites;
	private Map<String, ProteinFamiliesSet> genesProteins;
	private boolean saveOnlyReactionsWithKEGGmetabolites;
	private AtomicInteger counter;
	private Map<String, String>  keggMiriam;
	private Map<String, String>  chebiMiriam;
	private Map<String,String> metabolitesFormula;
	private Graph<String, String> graph;
	private Map<String, Map<String, Map<String, MetabolitesOntology>>> reactionsMetabolitesOntology;
	private Set<String> ignoreSymportMetabolites;
	private Map<String, Set<String>> rejectedGenesMetabolites;
	private int generations;

	/**
	 * @param genes
	 * @param genesReactions
	 * @param cancel
	 * @param genesContainer
	 * @param reactionsContainer
	 * @param metabolitesContainer
	 * @param genesLocusTag
	 * @param genesMetabolitesTransportTypeMap
	 * @param reactionsToBeReplaced
	 * @param transportReactionsList
	 * @param transportMetabolites
	 * @param metaboliteFunctionalParent_map
	 * @param ontologyReactions
	 * @param metabolites_ontology
	 * @param selectedGenesMetabolites
	 * @param rejectedGenesMetabolites
	 * @param genesProteins
	 * @param counter
	 * @param keggMiriam
	 * @param chebiMiriam
	 * @param metabolitesFormula
	 * @param saveOnlyReactionsWithKEGGmetabolites
	 * @param graph
	 * @param metabolite_generation
	 * @param reactions_metabolites_ontology
	 * @param existingReactions
	 * @param ignoreSymportMetabolites
	 * @param generations 
	 */
	public TransportContainerRunnable(ConcurrentLinkedDeque<String> genes, Map<String, Set<TransportReaction>> genesReactions, AtomicBoolean cancel, 
			ConcurrentHashMap<String,GeneCI> genesContainer, ConcurrentHashMap<String, TransportReactionCI> reactionsContainer, 
			ConcurrentHashMap<String, MetaboliteCI> metabolitesContainer,
			Map<String,String> genesLocusTag, Map<String, GenesMetabolitesTransportType> genesMetabolitesTransportTypeMap,
			ConcurrentHashMap<String, String> reactionsToBeReplaced, ConcurrentHashMap<String,TransportReaction> transportReactionsList,
			Map<String, TransportMetabolite> transportMetabolites, ConcurrentHashMap<String, Set<String>> metaboliteFunctionalParent_map,
			ConcurrentHashMap<String,Set<TransportReaction>> ontologyReactions, Map<String, Set<String>> metabolites_ontology,
			Map<String, Set<String>> selectedGenesMetabolites, Map<String, Set<String>> rejectedGenesMetabolites, Map<String, ProteinFamiliesSet> genesProteins,
			AtomicInteger counter, Map<String, String>  keggMiriam, Map<String, String>  chebiMiriam,
			Map<String,String> metabolitesFormula, boolean saveOnlyReactionsWithKEGGmetabolites, Graph<String, String> graph,
			Map<String, Map<String, Integer>> metabolite_generation, Map<String, Map<String, Map<String, MetabolitesOntology>>> reactions_metabolites_ontology,
			ConcurrentHashMap<String, String> existingReactions, Set<String> ignoreSymportMetabolites, int generations) {

		this.genes = genes;
		this.genesReactions = genesReactions;
		this.cancel = cancel;
		this.genesContainer = genesContainer;
		this.reactionsContainer = reactionsContainer;
		this.metabolitesContainer = metabolitesContainer;
		this.genesLocusTag = genesLocusTag;
		this.genesMetabolitesTransportTypeMap = genesMetabolitesTransportTypeMap;
		this.transportReactionsList = transportReactionsList;
		this.reactionsToBeReplaced = reactionsToBeReplaced;
		this.transportMetabolites = transportMetabolites;
		this.metaboliteFunctionalParent_map = metaboliteFunctionalParent_map;
		this.ontologyReactions = ontologyReactions;
		this.metabolites_ontology = metabolites_ontology;
		this.selectedGenesMetabolites = selectedGenesMetabolites;
		this.rejectedGenesMetabolites = rejectedGenesMetabolites;
		this.counter = counter;
		this.metabolitesFormula = metabolitesFormula;
		this.keggMiriam = keggMiriam;
		this.chebiMiriam = chebiMiriam;
		this.genesProteins = genesProteins;
		this.saveOnlyReactionsWithKEGGmetabolites = saveOnlyReactionsWithKEGGmetabolites;
		this.graph = graph;
		this.reactionsMetabolitesOntology = reactions_metabolites_ontology;
		this.existingReactions = existingReactions;
		this.ignoreSymportMetabolites = ignoreSymportMetabolites;
		this.generations = generations;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {

		int g = this.genes.size();
		while (g>0) {

			String gene = null;

			synchronized (genes) {

				gene = genes.poll();
			}

			if(gene != null) {

				try {

					this.processGene(gene);
				}
				catch (Exception e) {

					e.printStackTrace();
					System.out.println("Gene " + gene);
					this.genes.add(gene);
				}

				this.counter.incrementAndGet();
				logger.debug("Counter on transport container runnable {}", this.counter.get());
				setChanged();
				notifyObservers();
			}

			g = this.genes.size();
			if(this.cancel.get())
				g=0;

		}
	}

	/**
	 * @param geneID
	 * @throws Exception 
	 */
	public void processGene(String geneID) throws Exception {

		String locus_tag = this.genesLocusTag.get(geneID);

		if(!this.cancel.get()) {

			this.addGeneCI(locus_tag);

			for(TransportReaction originalTransportReaction : this.genesReactions.get(geneID)) {
				
				if(verbose)
					logger.info("reaction {}\toriginal metabolites {}", originalTransportReaction.getReactionID(), originalTransportReaction.getMetabolites());

				//removing metabolites not transported
				Set<String> reactionSelectedMetabolites = this.removeReactantsAndProducts(originalTransportReaction.getMetaboliteDirection(), this.selectedGenesMetabolites.get(geneID), originalTransportReaction.getProtein_family_IDs());
				//same for rejected
				if(this.rejectedGenesMetabolites.containsKey(geneID))
					this.rejectedGenesMetabolites.put(geneID, this.removeReactantsAndProducts(originalTransportReaction.getMetaboliteDirection(), this.rejectedGenesMetabolites.get(geneID), originalTransportReaction.getProtein_family_IDs()));
				if(verbose)
					logger.info("reaction {}\tselected metabolites {}", originalTransportReaction.getReactionID(), reactionSelectedMetabolites);

				//Ignore symport metabolites
				if(originalTransportReaction.getTransportType().equalsIgnoreCase("symport") && this.ignoreSymportMetabolites!=null && !this.ignoreSymportMetabolites.isEmpty()) {
					
					reactionSelectedMetabolites = this.processIgnoreSymportMetabolites(ignoreSymportMetabolites, reactionSelectedMetabolites);
					//same for rejected
					if(this.rejectedGenesMetabolites.containsKey(geneID))
						this.rejectedGenesMetabolites.put(geneID, this.processIgnoreSymportMetabolites(ignoreSymportMetabolites, this.rejectedGenesMetabolites.get(geneID)));
				}
				if(verbose)
					logger.info("reaction {}\tselected metabolites removing ignored {}", originalTransportReaction.getReactionID(), reactionSelectedMetabolites);

				//at least one selected metabolite is on reaction
				if(reactionSelectedMetabolites.size()>0 && this.hasAtLeastOne(originalTransportReaction.getMetaboliteStoichiometry().keySet(), reactionSelectedMetabolites)) {

					if(verbose)
						logger.info("reaction {}\tmets {} has at least one selected metabolite from {}", originalTransportReaction.getReactionID(), originalTransportReaction.getMetaboliteStoichiometry(), reactionSelectedMetabolites);

					// transport type classification
					boolean go=false;

					for(String metaboliteID : originalTransportReaction.getMetaboliteStoichiometry().keySet())
						if(reactionSelectedMetabolites.contains(metaboliteID) && this.genesMetabolitesTransportTypeMap.get(geneID).isHigherScoreTransportTypeID(metaboliteID,originalTransportReaction.getTransportType()))
							go=true;

					if(verbose)
						logger.info("reaction {}\tusing max score transport type {}", originalTransportReaction.getReactionID(), go);

					//if using max score transport type
					if(go) {

						Set<MetaboliteCI> meta_CI = new HashSet<>();

						//get transport reactions from metabolites ontology
						String originalReactionID = originalTransportReaction.getReactionID();

						Set<TransportReaction> transportReactionsFromOntology = this.getTransportReactionFromOntology(originalTransportReaction, locus_tag, geneID);

						if(verbose)
							logger.info("reaction {}\tget transport reactions from metabolites ontology {}", originalTransportReaction.getReactionID(), transportReactionsFromOntology);

						//////////////////////////////////////////////depois daqui confirmar as reações ontologia, marcar sempre a original

						List<String> originalBifunctionalMetabolites = this.getOriginalBifunctionalMetabolitesList(originalTransportReaction);

						for(TransportReaction transportReaction : transportReactionsFromOntology) {
							
							if(saveOnlyReactionsWithKEGGmetabolites)
								go = TransportersUtilities.areAllMetabolitesKEGG(transportReaction);

							if(verbose)
								logger.debug("reaction {}\t are all from KEGG {}", transportReaction.getReactionID(), go);

							if(go) {

								boolean addReaction=true;

								if(this.reactionsContainer.containsKey(transportReaction.getReactionID())) {

									synchronized(this.reactionsMetabolitesOntology) {

										Map<String, Map<String, MetabolitesOntology>> metOn = this.reactionsMetabolitesOntology.get(transportReaction.getReactionID());

										synchronized(this.reactionsContainer) {

											if(verbose)
												logger.debug("updating reaction\t{}\toriginal\tmets\t{}",transportReaction, transportReaction.getOriginalReaction(), transportReaction.getMetabolites());

											this.updateTransportReactionCI_information(transportReaction, locus_tag, geneID, metOn);
										}
									}
								}
								else {

									Map<String,StoichiometryValueCI> reactants = new TreeMap<>(), products = new TreeMap<>();

									for(String metaboliteID:transportReaction.getMetaboliteStoichiometry().keySet()) {

										if(!this.metabolitesContainer.containsKey(metaboliteID)) {

											String formula = null;

											if(this.metabolitesFormula.containsKey(metaboliteID))
												formula = this.metabolitesFormula.get(metaboliteID);

											meta_CI.add(this.addMetaboliteCI(metaboliteID, transportReaction.getMetabolites().get(metaboliteID).getName(), formula));
											this.addMetaboliteMiriamKEGG(metaboliteID, transportReaction.getMetabolites().get(metaboliteID).getKeggMiriam());
											this.addMetaboliteMiriamChEBI(metaboliteID, transportReaction.getMetabolites().get(metaboliteID).getChEBIMiriam());
										}

										for(int i=0;i<transportReaction.getMetaboliteDirection().get(metaboliteID).size();i++) {

											String reactant_compartment = null;
											String product_compartment = null;

											if(transportReaction.getMetaboliteDirection().get(metaboliteID).get(i).equals("in")) {

												if(reactants.containsKey(metaboliteID)) {

													if(reactants.get(metaboliteID).getCompartmentId().equalsIgnoreCase("in") &&
															reactants.get(metaboliteID).getMetaboliteId().equalsIgnoreCase(metaboliteID) &&
															reactants.get(metaboliteID).getStoichiometryValue() ==
															transportReaction.getMetaboliteStoichiometry().get(metaboliteID).get(i)) {

														//if ATP
														if(transportReaction.getMetabolites().get(metaboliteID).getKeggMiriam().equals("urn:miriam:kegg.compound:C00002")) {

															addReaction=false;
														}
														else {

															System.err.print("ALREADY ADDED metaboliteID: "+metaboliteID);
															System.err.print("\t metabolite "+this.transportMetabolites.get(metaboliteID).getName());
															System.err.print("\t reaction "+transportReaction.getReactionID());
															System.err.print("\tgene ID "+locus_tag);
															double value = reactants.get(metaboliteID).getStoichiometryValue()+new Double(transportReaction.getMetaboliteStoichiometry().get(metaboliteID).get(i));
															System.err.print(": Setting stoichiometry from "+reactants.get(metaboliteID).getStoichiometryValue()); 
															reactants.get(metaboliteID).setStoichiometryValue(value);
															System.err.println(" to "+reactants.get(metaboliteID).getStoichiometryValue());
														}
													}
													else {

														reactant_compartment = "out";
														product_compartment = "in";
													}
												}
												else {

													reactant_compartment = "out";
													product_compartment = "in";
												}
											}
											else if(transportReaction.getMetaboliteDirection().get(metaboliteID).get(i).equals("out")) {

												reactant_compartment = "in";
												product_compartment = "out";
											}
											else if(transportReaction.getMetaboliteDirection().get(metaboliteID).get(i).equals("reactant")) {

												if(reactants.containsKey(metaboliteID) && !originalBifunctionalMetabolites.contains(metaboliteID))
													addReaction= false;

												if(products.containsKey(metaboliteID)  && !originalBifunctionalMetabolites.contains(metaboliteID))
													addReaction= false;

												reactant_compartment = "in";

												if(this.addExternalReactant(originalTransportReaction.getProtein_family_IDs(), metaboliteID))
													reactant_compartment = "out";

											}
											else if(transportReaction.getMetaboliteDirection().get(metaboliteID).get(i).equals("product")) {

												if(reactants.containsKey(metaboliteID) && !originalBifunctionalMetabolites.contains(metaboliteID))
													addReaction= false;

												if(products.containsKey(metaboliteID) && !originalBifunctionalMetabolites.contains(metaboliteID))
													addReaction= false;

												product_compartment = "in";

												if(is5A3(originalTransportReaction.getProtein_family_IDs()) && !this.transportMetabolites.get(metaboliteID).getKeggMiriam().equals("urn:miriam:kegg.compound:C00390") && 
														!this.transportMetabolites.get(metaboliteID).getKeggMiriam().equals("urn:miriam:kegg.compound:C00399"))
													product_compartment = "out";
											}
											else {

												reactant_compartment = "out";
												product_compartment = "in";
											}

											if(reactant_compartment!=null) {

												double stoichiometry = transportReaction.getMetaboliteStoichiometry().get(metaboliteID).get(i);
												String newCompartment = reactant_compartment;

												if(reactants.containsKey(metaboliteID)) {

													if(reactants.get(metaboliteID).getCompartmentId().equals(reactant_compartment))
														stoichiometry += reactants.get(metaboliteID).getStoichiometryValue();
													else
														logger.warn("different compartments on reactants for metabolite {}", metaboliteID);
												}

												StoichiometryValueCI reactant = new StoichiometryValueCI(metaboliteID, stoichiometry, newCompartment);
												reactants.put(metaboliteID, reactant);
											}

											if(product_compartment!=null) {

												double stoichiometry = transportReaction.getMetaboliteStoichiometry().get(metaboliteID).get(i);

												if(products.containsKey(metaboliteID)) {													

													if(products.get(metaboliteID).getCompartmentId().equals(product_compartment)) {

														stoichiometry += products.get(metaboliteID).getStoichiometryValue();
													}
													else {

														logger.debug("different compartments on products for metabolite {}", metaboliteID);

														stoichiometry += stoichiometry + products.get(metaboliteID).getStoichiometryValue();
														String newCompartment = products.get(metaboliteID).getCompartmentId();

														if(reactants.get(metaboliteID).getCompartmentId().equals(product_compartment))
															product_compartment = newCompartment;
													}
												}

												StoichiometryValueCI product = new StoichiometryValueCI(metaboliteID, stoichiometry, product_compartment);
												products.put(metaboliteID, product);
											}
										}
									}


									if(addReaction) {

										String reactionId=null;

										Map<String, Map<String, String>> general_equation_byGene = new HashMap<String, Map<String, String>> ();
										general_equation_byGene.put(locus_tag, transportReaction.getGeneral_equation().get(locus_tag));

										Map<String, Boolean> originalReaction_byGene = transportReaction.getOriginalReaction(),
												reversibilityConfirmed = transportReaction.isReversibilityConfirmed(locus_tag);

										Map<String, String>	originalReactionID_byGene = new HashMap<String, String>();
										originalReactionID_byGene.put(locus_tag, originalReactionID);

										Map<String, Map<String, MetabolitesOntology>> chebi_ontology_byGene = null;

										if(!originalReaction_byGene.get(locus_tag)) {

											synchronized (reactionsMetabolitesOntology) {

												if(reactionsMetabolitesOntology.containsKey(transportReaction.getReactionID())) {

													Map<String, MetabolitesOntology> map =  reactionsMetabolitesOntology.get(transportReaction.getReactionID()).get(locus_tag);

													if(map==null) {

														//TODO DEBUG?!?!?!?!?!?!
														//													System.out.println("Null ontologies for reaction "+transportReaction.getReactionID()+" for gene "+locus_tag);
														//													System.out.println(transportReaction.getReactionID());											
														//													for(String metid : transportReaction.getMetabolites().keySet())														
														//														System.out.println(transportReaction.getMetabolites().get(metid));
													}
													else  {

														Map<String, MetabolitesOntology> chebi_ontology = this.replaceIDForChebis(map);
														chebi_ontology_byGene = new HashMap<String, Map<String, MetabolitesOntology>>();
														chebi_ontology_byGene.put(locus_tag, chebi_ontology);
													}
												}
											}
										}

										synchronized(this.reactionsContainer) {

											String transportType = transportReaction.getTransportType();
											
											reactionId = this.addTransportReaction(transportReaction.getReactionID(), transportReaction.getReactionID(), transportReaction.isReversibility(), 
													reactants, products, locus_tag, this.genesProteins.get(geneID), originalReaction_byGene, chebi_ontology_byGene,
													general_equation_byGene, transportType, reversibilityConfirmed, originalReactionID_byGene);

											this.addGenetoTransportReaction(this.reactionsContainer.get(reactionId), locus_tag, this.genesProteins.get(geneID));

											for(MetaboliteCI m : meta_CI)
												synchronized(this.metabolitesContainer) {
													m.addReaction(reactionId);
													this.metabolitesContainer.put(m.getId(), m);
												}
										}

										//System.out.println(ContainerUtils.getReactionToString(this.reactionsContainer.get(reactionId)));
									}
								}
							}
						}
					}
				}
				//				else {
				//
				//					either there are no selected metabolites or the selected metabolites for this gene are not available in this reaction, after filtering for removing symport metabolites.
				//				}

			}
		}

		logger.debug("Gene {} processed!", locus_tag);
		
		if(this.genesContainer.containsKey(locus_tag) && this.genesContainer.get(locus_tag).getReactionIds().isEmpty())
			this.genesContainer.remove(locus_tag);
	}

	/**
	 * @param transportType
	 * @param ignoreSymportMetabolites
	 * @param selectedMetabolites
	 * @return
	 */
	private Set<String> processIgnoreSymportMetabolites(Set<String> ignoreSymportMetabolites, Set<String> selectedMetabolites) {

		Set<String> ret = new HashSet<String>();

		for(String metaboliteID : selectedMetabolites) {

			String id = ExternalRefSource.KEGG_CPD.getSourceId(this.transportMetabolites.get(metaboliteID).getKeggMiriam()); 

			if(!ignoreSymportMetabolites.contains(id))
				ret.add(metaboliteID);
		}

		return ret;
	}

	/**
	 * @param transportReaction
	 * @param locus_tag
	 * @param geneID
	 * @param chebi_ontology_byGene
	 */
	private void updateTransportReactionCI_information(TransportReaction transportReaction, String locus_tag, String geneID,
			Map<String, Map<String, MetabolitesOntology>> chebi_ontology_byGene) {

		TransportReactionCI transportReactionCI = this.reactionsContainer.get(transportReaction.getReactionID());

		if(transportReactionCI.getIsOriginalReaction_byGene()==null) 
			transportReactionCI.setIsOriginalReaction_byGene(new HashMap<String, Boolean>());
		
		if(transportReactionCI.getIsOriginalReaction_byGene().containsKey(locus_tag)) {

			boolean existingOriginal = transportReactionCI.getIsOriginalReaction_byGene().get(locus_tag);
			boolean newOriginal = transportReaction.getOriginalReaction().get(locus_tag);
			
				if(!existingOriginal && newOriginal)
					transportReactionCI.getIsOriginalReaction_byGene().put(locus_tag, newOriginal);
		}
		else {
			
			transportReactionCI.getIsOriginalReaction_byGene().put(locus_tag,transportReaction.getOriginalReaction().get(locus_tag));
		}

		if(transportReactionCI.getChebi_ontology_byGene()==null)
			transportReactionCI.setChebi_ontology_byGene(new HashMap<String, Map<String, MetabolitesOntology>>());
		if(chebi_ontology_byGene!=null)
			transportReactionCI.getChebi_ontology_byGene().putAll(chebi_ontology_byGene);

		if(transportReactionCI.getGeneral_equation()==null)
			transportReactionCI.setGeneral_equation(new HashMap<String, Map<String, String>>());
		transportReactionCI.getGeneral_equation().put(locus_tag, transportReaction.getGeneral_equation().get(locus_tag));

		if(transportReactionCI.getReversibilityConfirmed_byGene()==null)
			transportReactionCI.setReversibilityConfirmed_byGene(new HashMap<String, Boolean>());
		transportReactionCI.getReversibilityConfirmed_byGene().putAll(transportReaction.isReversibilityConfirmed(locus_tag));

		if(transportReactionCI.getOriginalReactionID_byGene()==null)
			transportReactionCI.setOriginalReactionID_byGene(new HashMap<String, String>());
		transportReactionCI.getOriginalReactionID_byGene().put(locus_tag, transportReaction.getOriginalReactionID().get(locus_tag));

		this.addGenetoTransportReaction(transportReactionCI, locus_tag, this.genesProteins.get(geneID));
	}

	/**
	 * @param list
	 * @param metaboliteID
	 * @return
	 */
	private boolean addExternalReactant(List<String> list, String metaboliteID) {

		boolean fourA = false,fourB = false, fourC = false, fiveAthree = false;//, threeDFourTen = false;

		for(String f : list) {

			if(f.startsWith("4.A")){fourA=true;if(fourB||fourC||fiveAthree){System.err.println("already set to another 4 type protein");}}
			if(f.startsWith("4.B")){fourB=true;if(fourA||fourC||fiveAthree){System.err.println("already set to another 4 type protein");}}
			if(f.startsWith("4.C")){fourC=true;if(fourB||fourA||fiveAthree){System.err.println("already set to another 4 type protein");}}
			if(f.startsWith("5.A.3")){fiveAthree=true;}
		}

		if(
				(fourA && !this.transportMetabolites.get(metaboliteID).getKeggMiriam().equals("urn:miriam:kegg.compound:C00074"))
				|| (fourB && !this.transportMetabolites.get(metaboliteID).getChEBIMiriam().equals("urn:miriam:obo.chebi:CHEBI:15422")) 
				|| (fourC && !this.transportMetabolites.get(metaboliteID).getChEBIMiriam().equals("urn:miriam:obo.chebi:CHEBI:15422") && 
						!this.transportMetabolites.get(metaboliteID).getChEBIMiriam().equals("urn:miriam:obo.chebi:CHEBI:15346")) 
				|| (fiveAthree && !this.transportMetabolites.get(metaboliteID).getKeggMiriam().equals("urn:miriam:kegg.compound:C00390") && 
						!this.transportMetabolites.get(metaboliteID).getKeggMiriam().equals("urn:miriam:kegg.compound:C00399"))
				) {

			return true;
		}
		return false;
	}

	/**
	 * @param list
	 * @return
	 */
	private boolean is5A3(List<String> list) {

		for(String f : list) {

			if(f.startsWith("5.A.3")) {

				return true;
			}
		}
		return false;
	}

	/**
	 * @param map
	 * @return
	 */
	private Map<String, MetabolitesOntology> replaceIDForChebis(Map<String, MetabolitesOntology> map) {

		try {

			Map<String, MetabolitesOntology> returnMap = new HashMap<String, MetabolitesOntology>();

			for(MetabolitesOntology metabolitesOntology : map.values()) {

				String original_parent_chebi_id = metabolitesOntology.getOriginal_parent_metabolite_id();
				if(this.chebiMiriam.containsKey(original_parent_chebi_id)) {

					original_parent_chebi_id  = ExternalRefSource.CHEBI.getSourceId(this.chebiMiriam.get(original_parent_chebi_id));
				}

				String metabolite_chebi_id = metabolitesOntology.getMetabolite_id();
				if(this.chebiMiriam.containsKey(metabolite_chebi_id)) {

					metabolite_chebi_id  = ExternalRefSource.CHEBI.getSourceId(this.chebiMiriam.get(metabolite_chebi_id));
				}

				String upper_parent_chebi_id  = metabolitesOntology.getUpper_parent_metabolite_id();
				if(this.chebiMiriam.containsKey(upper_parent_chebi_id )) {

					upper_parent_chebi_id =  ExternalRefSource.CHEBI.getSourceId(this.chebiMiriam.get(upper_parent_chebi_id));
				}

				metabolitesOntology.setMetabolite_id(metabolite_chebi_id);
				metabolitesOntology.setUpper_parent_metabolite_id(upper_parent_chebi_id);
				metabolitesOntology.setOriginal_parent_metabolite_id(original_parent_chebi_id);
				returnMap.put(metabolite_chebi_id, metabolitesOntology);
			}

			return returnMap;
		}
		catch (Exception e) {

			//TODO if chebi exists verify chebi and KEGG, if kegg does not exist in other metabolite add kegg id and create new record 

			System.out.println("Map\t"+ map);
			System.out.println();
			e.printStackTrace();
			System.out.println();
			return null;
		}
	}

	/**
	 * @param first
	 * @param second
	 * @return
	 */
	private boolean hasAtLeastOne(Set<String> first, Set<String> second) {

		for(String value:first)
			if(second.contains(value))
				return true;
		return false;
	}

	/**
	 * @param originalTransportReaction
	 * @param locus_tag
	 * @param geneID 
	 * @return
	 */
	private Set<TransportReaction> getTransportReactionFromOntology(TransportReaction originalTransportReaction, String locus_tag, String geneID) {

		Set<TransportReaction> transportReactionSetResult = new HashSet<TransportReaction>();

		synchronized (this.ontologyReactions) {

			if(this.ontologyReactions.containsKey(originalTransportReaction.getReactionID())) {

				transportReactionSetResult = this.ontologyReactions.get(originalTransportReaction.getReactionID());

				for(TransportReaction tr : transportReactionSetResult) {
					
					boolean sameID = tr.getReactionID().equalsIgnoreCase(originalTransportReaction.getReactionID());
					boolean sameMetabolites = compareReactionMetabolites(tr,originalTransportReaction);
					boolean sameTransportType = tr.getTransportType().equals(originalTransportReaction.getTransportType());
					
					boolean original = false;

					if(sameID || (sameMetabolites && sameTransportType))
						original = originalTransportReaction.getOriginalReaction().get(locus_tag);

					if(!tr.getGeneral_equation().containsKey(locus_tag) || original)
							tr.getGeneral_equation().put(locus_tag, originalTransportReaction.getGeneral_equation().get(locus_tag));
					
					if(!tr.getGeneral_equation().containsKey(locus_tag) || original)
							tr.getOriginalReactionID().put(locus_tag, originalTransportReaction.getReactionID());
					
					// if ontology reaction contains at least one metabolite bellow threshold (symports and reacting metabolties have been removed) it means that this reactions should be considered original
					
					for(String metabolite : tr.getMetabolites().keySet())
						if(this.rejectedGenesMetabolites.containsKey(geneID) && this.rejectedGenesMetabolites.get(geneID).contains(metabolite))
							original = true;
					
					if(!tr.getOriginalReaction().containsKey(locus_tag) || original)
						tr.getOriginalReaction().put(locus_tag, original);

					String surrogate_gene = null;

					synchronized (this.reactionsMetabolitesOntology) {

						if(this.reactionsMetabolitesOntology.containsKey(tr.getReactionID())) {

							for(String gene : this.reactionsMetabolitesOntology.get(tr.getReactionID()).keySet()){

								Map<String, MetabolitesOntology> ont = this.reactionsMetabolitesOntology.get(tr.getReactionID()).get(gene);

								for(MetabolitesOntology met : ont.values())
									if(met.getOriginalReactionID().equalsIgnoreCase(originalTransportReaction.getReactionID()))
										surrogate_gene = gene;
							}

							if(surrogate_gene != null) {

								Map<String, MetabolitesOntology> ont = this.reactionsMetabolitesOntology.get(tr.getReactionID()).get(surrogate_gene);
								Map<String, Map<String, MetabolitesOntology>> genes_ont = new HashMap<String, Map<String, MetabolitesOntology>>();
								genes_ont.put(locus_tag, ont);
								this.reactionsMetabolitesOntology.put(tr.getReactionID(), genes_ont);
							}
						}
					}
				}
			}
			else {

				List<String> metabolitesIDs = this.getOrderedMetabolitesID(originalTransportReaction);

				TransportReaction originalTransportReactionObject =  new TransportReaction(originalTransportReaction.getReactionID(),
						originalTransportReaction.getTransportType(), originalTransportReaction.isReversibility(), true, locus_tag, originalTransportReaction.getReactionID());

				originalTransportReactionObject.setProtein_family_IDs(originalTransportReaction.getProtein_family_IDs());
				originalTransportReactionObject.setGeneral_equation(originalTransportReaction.getGeneral_equation());
				transportReactionSetResult.add(originalTransportReactionObject);	

				for(int metabolite_index=0; metabolite_index<metabolitesIDs.size(); metabolite_index++) {

					String metaboliteID = metabolitesIDs.get(metabolite_index);
					List<String> metabolites = new ArrayList<String>();
					metabolites.add(0,metaboliteID);

					Dijkstra<String, String> alg = null;

					if(this.transportMetabolites.get(metaboliteID).getKeggMiriam().equalsIgnoreCase("urn:miriam:kegg.compound:C00010") && 
							originalTransportReaction.getMetaboliteDirection().get(metaboliteID).contains("reactant")) {

						// remove CoenzymeA from ontology when used as reactant
					}
					else {

						if(this.metabolites_ontology.containsKey(metaboliteID) && !metaboliteID.equals(ExternalRefSource.CHEBI.getSourceId("urn:miriam:obo.chebi:CHEBI:15346"))) {

							alg = new Dijkstra<String, String>(graph, metaboliteID);
							alg.run();
							metabolites.addAll(this.metabolites_ontology.get(metaboliteID));
						}
					}

					transportReactionSetResult = this.addMetabolitesToReaction(transportReactionSetResult, originalTransportReaction.getMetaboliteStoichiometry().get(metaboliteID),
							originalTransportReaction.getMetaboliteDirection().get(metaboliteID), metabolites, alg, locus_tag, originalTransportReaction.getReactionID());
				}

				//remove initial empty object
				transportReactionSetResult.remove(originalTransportReactionObject);

				//remove first reaction created because is same as original
				int token=0;
				String reaction_to_change_ID=originalTransportReaction.getReactionID();
				while(token<metabolitesIDs.size()) {

					reaction_to_change_ID=reaction_to_change_ID.concat("_0");
					token++;
				}

				for(TransportReaction transportReaction:transportReactionSetResult) {

					if(transportReaction.getReactionID().equals(reaction_to_change_ID)) {

						transportReaction.setReactionID(originalTransportReaction.getReactionID());
						transportReaction.addOriginalReaction(locus_tag, true);
						break;
					}
				}

				for(TransportReaction transportReaction : new HashSet<TransportReaction>(transportReactionSetResult)) {
					
					String reactionID = this.existsReaction(transportReaction);

					if(reactionID!=null) {
						
						transportReactionSetResult.remove(transportReaction);

						synchronized (this.transportReactionsList) {
							
							boolean original = transportReaction.getOriginalReaction().get(locus_tag);
							if(!original && this.transportReactionsList.get(reactionID).getOriginalReaction().containsKey(locus_tag) && this.transportReactionsList.get(reactionID).getOriginalReaction().get(locus_tag))
								original=true;

							this.transportReactionsList.get(reactionID).getGeneral_equation().put(locus_tag, originalTransportReaction.getGeneral_equation().get(locus_tag));
							this.transportReactionsList.get(reactionID).getOriginalReaction().put(locus_tag, original);
							this.transportReactionsList.get(reactionID).getOriginalReactionID().put(locus_tag, originalTransportReaction.getReactionID());
							
						}

						synchronized (this.reactionsMetabolitesOntology) {

							if(!transportReaction.getOriginalReaction().get(locus_tag)) {

								Map<String, Map<String, MetabolitesOntology>>  genes_ont = this.reactionsMetabolitesOntology.get(transportReaction.getReactionID());

								if(genes_ont==null) {

									//TODO debug
								}
								else {

									if(this.reactionsMetabolitesOntology.containsKey(reactionID))
										this.reactionsMetabolitesOntology.get(reactionID).putAll(genes_ont);
									else
										this.reactionsMetabolitesOntology.put(reactionID, genes_ont);
								}
							}
							transportReactionSetResult.add(this.transportReactionsList.get(reactionID));
						}
					}
				}

				this.ontologyReactions.put(originalTransportReaction.getReactionID(),transportReactionSetResult);
			}
		}

		return transportReactionSetResult;
	}

	/**
	 * @param transportReaction1
	 * @param transportReaction2
	 * @return
	 */
	private static boolean compareReactionMetabolites(TransportReaction transportReaction1, TransportReaction transportReaction2) {

		Set<String> mets1 = new HashSet<>(transportReaction1.getMetabolites().keySet()), mets1_clone = new HashSet<>(transportReaction1.getMetabolites().keySet());
		Set<String> mets2 = new HashSet<>(transportReaction2.getMetabolites().keySet()), mets2_clone = new HashSet<>(transportReaction1.getMetabolites().keySet());

		mets1.removeAll(mets2_clone);
		mets2.removeAll(mets1_clone);

		if(mets1.isEmpty() && mets2.isEmpty()) {

			//System.out.println(transportReaction1.getReactionID()+" "+transportReaction1.getTransportType()+" same as "+transportReaction2.getReactionID()+" "+transportReaction2.getTransportType());
			return true;
		}

		return false;
	}

	/**
	 * @param originalTransportReaction
	 * @return
	 */
	private List<String> getOrderedMetabolitesID(TransportReaction originalTransportReaction) {

		List<String> products = new ArrayList<String>();
		List<String> result = new ArrayList<String>();

		for(String id: originalTransportReaction.getMetabolites().keySet()) {

			List<String> directions = originalTransportReaction.getMetaboliteDirection().get(id);

			if(directions.contains("product")) {

				products.add(id);
			}
			else {

				result.add(id);
			}
		}
		result.addAll(products);
		return result;
	}

	/**
	 * @param transportReactionsSet
	 * @param stoichiometry
	 * @param direction
	 * @param metabolites
	 * @param alg
	 * @param locus_tag
	 * @param originalReactionID
	 * @return
	 */
	private Set<TransportReaction> addMetabolitesToReaction(Set<TransportReaction> transportReactionsSet, List<Double> stoichiometry, List<String> direction, List<String> metabolites, Dijkstra<String, String> alg, String locus_tag, String originalReactionID) {

		Set<TransportReaction> transportReactionsResultSet = new HashSet<TransportReaction>();
		Set<String> existingReactionsID = new HashSet<String>();

		for(TransportReaction transportReaction : transportReactionsSet) {

			int counter=0;
			for(int metaboliteID_index=0;metaboliteID_index<metabolites.size();metaboliteID_index++) {

				String metaboliteID=metabolites.get(metaboliteID_index);
				TransportReaction transportReactionClone = transportReaction.clone(transportReaction.getReactionID().concat("_"+counter++), locus_tag, originalReactionID, false);
				existingReactionsID.add(transportReactionClone.getReactionID());

				synchronized(reactionsMetabolitesOntology) {

					Map<String, MetabolitesOntology> metabolitesLink = new HashMap<String, MetabolitesOntology>();
					Map<String, Map<String, MetabolitesOntology>> res = new HashMap<String, Map<String,MetabolitesOntology>>();

					if(reactionsMetabolitesOntology.containsKey(transportReaction.getReactionID())) {

						res.putAll(reactionsMetabolitesOntology.get(transportReaction.getReactionID()));

						if(reactionsMetabolitesOntology.get(transportReaction.getReactionID()).containsKey(locus_tag))
							metabolitesLink.putAll(reactionsMetabolitesOntology.get(transportReaction.getReactionID()).get(locus_tag));
					}

					if(alg!= null && !alg.getSource().equalsIgnoreCase(metaboliteID)) {

						int generation = alg.getShortestPath(metaboliteID).size();

						String upperParentMetaboliteID = alg.getShortestPath(metaboliteID).get(alg.getShortestPath(metaboliteID).size()-2);

						if(generation <= this.generations) {
						
							MetabolitesOntology metOnt = new MetabolitesOntology(metaboliteID, alg.getSource(), upperParentMetaboliteID, generation, originalReactionID, transportReactionClone.getReactionID());
							metabolitesLink.put(metaboliteID, metOnt);
						}
						else {
							
							logger.debug("generation {}\treaction {}\toriginal reaction {}", generation, transportReactionClone.getReactionID(), originalReactionID);
						}
					}

					//					if(alg == null && this.metabolites_ontology.containsKey(metabolites.get(0))) {
					//
					//						System.out.println(locus_tag + "\t"+metabolites.get(0) +"\t"+metaboliteID);
					//						System.out.println();
					//					}

					if(!metabolitesLink.isEmpty()) {

						res.put(locus_tag, metabolitesLink);
						this.reactionsMetabolitesOntology.put(transportReactionClone.getReactionID(), res);
					}
				}

				for(int i=0 ; i<stoichiometry.size(); i++) {

					if(transportReaction.getProtein_family_IDs().contains("4.C.1.1.#")) {

						if(direction.get(i).equals("product") 
								&& !transportMetabolites.get(metaboliteID).getChEBIMiriam().equals("urn:miriam:obo.chebi:CHEBI:16027")
								&& !transportMetabolites.get(metaboliteID).getChEBIMiriam().equals("urn:miriam:obo.chebi:CHEBI:29888")
								&& !transportMetabolites.get(metaboliteID).getChEBIMiriam().equals("urn:miriam:obo.chebi:CHEBI:18361")
								) {

							Set<String> metaboliteFunctionalParent=null;

							if(this.metaboliteFunctionalParent_map.containsKey(metaboliteID)) {

								metaboliteFunctionalParent=this.metaboliteFunctionalParent_map.get(metaboliteID);
							}
							else {

								String metabolite_chebi_id = ExternalRefSource.CHEBI.getSourceId(this.transportMetabolites.get(metaboliteID).getChEBIMiriam());
								if(metabolite_chebi_id!=null && !metabolite_chebi_id.equalsIgnoreCase("null")) {

									metaboliteFunctionalParent = ChebiAPIInterface.getFunctionalParents(ExternalRefSource.CHEBI.getSourceId(this.transportMetabolites.get(metaboliteID).getChEBIMiriam()),0);
									this.metaboliteFunctionalParent_map.put(metaboliteID, metaboliteFunctionalParent);
								}
							}

							if(metaboliteFunctionalParent==null || metaboliteFunctionalParent.isEmpty()) {

								//eliminate non related metabolites
							}
							else {

								boolean deleteReaction=true;

								for(String previousMetabolites:transportReaction.getMetabolites().keySet()) {

									String  metabolite = transportReaction.getMetabolites().get(previousMetabolites).getChEBIMiriam();

									if(!ExternalRefSource.CHEBI.getSourceId("urn:miriam:obo.chebi:CHEBI:15346").equals(ExternalRefSource.CHEBI.getSourceId(transportReaction.getMetabolites().get(previousMetabolites).getChEBIMiriam()))
											&& metabolite!= null && !metabolite.equalsIgnoreCase("null") &&  metaboliteFunctionalParent.contains(ExternalRefSource.CHEBI.getSourceId(metabolite))) {

										transportReactionClone.addMetabolite(metaboliteID, this.transportMetabolites.get(metaboliteID), stoichiometry.get(i), direction.get(i));
										transportReactionsResultSet.add(transportReactionClone);
										deleteReaction=false;
									}
								}

								if(deleteReaction) {

									transportReactionsResultSet.remove(transportReactionClone);
									existingReactionsID.remove(transportReactionClone.getReactionID());
								}
							}
						}
						else {

							transportReactionClone.addMetabolite(metaboliteID, this.transportMetabolites.get(metaboliteID), stoichiometry.get(i), direction.get(i));
							transportReactionsResultSet.add(transportReactionClone);
						}
					}
					else {

						transportReactionClone.addMetabolite(metaboliteID, this.transportMetabolites.get(metaboliteID), stoichiometry.get(i), direction.get(i));
						transportReactionsResultSet.add(transportReactionClone);
					}
				}
			}
		}

		//existingReactionsID.addAll(this.reactionsContainer.keySet());
		//reactionsMetabolitesOntology.keySet().retainAll(existingReactionsID);

		return transportReactionsResultSet;
	}

	/**
	 * @param transportReaction
	 * @return
	 */
	private String existsReaction(TransportReaction transportReaction) {

		if(this.reactionsToBeReplaced.containsKey(transportReaction.getReactionID())) {

			return this.reactionsToBeReplaced.get(transportReaction.getReactionID());
		}
		
		for(String reactionID : this.transportReactionsList.keySet()) {

			TransportReaction existingTransportReaction = this.transportReactionsList.get(reactionID);

			if(existingTransportReaction.isReversibility()==transportReaction.isReversibility()) {

				boolean same_directions=false, inverse_directions=false, same_stoichiometries=false;
				List<String> metabolites1 = new ArrayList<String>(transportReaction.getMetabolites().keySet());
				List<String> metabolites2 = new ArrayList<String>(existingTransportReaction.getMetabolites().keySet());
				boolean same_metabolites = this.compareLists(metabolites1, metabolites2);

				if(same_metabolites) {

					boolean isSameDirection=true;
					boolean isInverseDirection=true;

					for(String metaboliteID:existingTransportReaction.getMetabolites().keySet()) {

						List<String> direction1 = new ArrayList<String>(transportReaction.getMetaboliteDirection().get(metaboliteID));
						List<String> direction2 = new ArrayList<String>(existingTransportReaction.getMetaboliteDirection().get(metaboliteID));
						same_directions = this.compareLists(direction1, direction2);

						if(same_directions && isSameDirection) {

							isInverseDirection=false;
							same_stoichiometries = this.compareLists(new ArrayList<Double>(transportReaction.getMetaboliteStoichiometry().get(metaboliteID)),
									new ArrayList<Double>(existingTransportReaction.getMetaboliteStoichiometry().get(metaboliteID)));

							if(!same_stoichiometries) {

								synchronized(this.transportReactionsList) {

									this.transportReactionsList.put(transportReaction.getReactionID(),transportReaction);
								}
								return null;
							}
						}
						else {

							if(transportReaction.isReversibility() && isInverseDirection) {

								List<String> newDirections = new ArrayList<String>();

								for(String direction:direction1) {

									if(direction.equalsIgnoreCase("in"))
										newDirections.add("out");
									else if(direction.equalsIgnoreCase("out"))
										newDirections.add("in");
								}

								inverse_directions=this.compareLists(newDirections, direction2);

								if(inverse_directions) {

									isSameDirection=false;
									same_stoichiometries = this.compareLists(new ArrayList<Double>(transportReaction.getMetaboliteStoichiometry().get(metaboliteID)),
											new ArrayList<Double>(existingTransportReaction.getMetaboliteStoichiometry().get(metaboliteID)));

									if(!same_stoichiometries) {

										synchronized(this.transportReactionsList) {

											this.transportReactionsList.put(transportReaction.getReactionID(),transportReaction);
										}
										return null;
									}
								}
							}
							else {

								synchronized(this.transportReactionsList) {

									this.transportReactionsList.put(transportReaction.getReactionID(),transportReaction);
								}
								return null;
							}
						}
					}
				}
				if(same_metabolites && (same_directions||inverse_directions) && same_stoichiometries) {

					synchronized(this.reactionsToBeReplaced) {

						this.reactionsToBeReplaced.put(transportReaction.getReactionID(), existingTransportReaction.getReactionID());
					}

					return existingTransportReaction.getReactionID();
				}
			}
		}

		synchronized(this.transportReactionsList) {

			this.transportReactionsList.put(transportReaction.getReactionID(),transportReaction);
		}
		return null;
	}


	/**
	 * @param reaction
	 */
//	private void mapReactionToMetabolites(TransportReaction transportReaction) {
//
//		synchronized(this.metaboliteReactions) {
//
//			for(String metabolite : transportReaction.getMetabolites().keySet()) {
//
//				Set<String> reactions = new HashSet<>();
//
//				if(this.metaboliteReactions.containsKey(metabolite))
//					reactions = metaboliteReactions.get(metabolite);
//
//				reactions.add(transportReaction.getReactionID());
//				metaboliteReactions.put(metabolite, reactions);
//			}
//		}
//	}

	/**
	 * @param directions
	 * @param selectedMetabolites
	 * @param protein_family_IDs_list
	 * @return
	 */
	private Set<String> removeReactantsAndProducts(Map<String, List<String>> directions, Set<String> selectedMetabolites, List<String> protein_family_IDs_list) {

		Set<String> filteredMetabolites = new HashSet<String>(selectedMetabolites);
		Set<String> reactantsAndProducts = new HashSet<String>();
		boolean hasVectorialReactions=false;
		boolean fourA = false, fourB = false, fourC = false, fiveAthree = false;

		for(String f : protein_family_IDs_list) {

			if(f.startsWith("4.A")){fourA=true;if(fourB||fourC){System.err.println("already set to another 4 type protein");}}
			if(f.startsWith("4.B")){fourB=true;if(fourA||fourC){System.err.println("already set to another 4 type protein");}}
			if(f.startsWith("4.C")){fourC=true;if(fourB||fourA){System.err.println("already set to another 4 type protein");}}
			if(f.startsWith("5.A.3")){fiveAthree=true;}
		}

		for(String key:directions.keySet()) {

			if(directions.get(key).size()==1 && (directions.get(key).get(0).equals("in")||directions.get(key).get(0).equals("out"))) {

				hasVectorialReactions=true;
			}

			if(directions.get(key).size()==1 && (directions.get(key).get(0).equals("reactant")||directions.get(key).get(0).equals("product"))) {


				if(!fourA && !fourB && !fourC && !fiveAthree ) {

					reactantsAndProducts.add(key);
				}
				else if((fourA && !this.transportMetabolites.get(key).getKeggMiriam().equals("urn:miriam:kegg.compound:C00074")) 
						||(fourB && !this.transportMetabolites.get(key).getChEBIMiriam().equals("urn:miriam:obo.chebi:CHEBI:15422"))
						|| (fourC && !this.transportMetabolites.get(key).getChEBIMiriam().equals("urn:miriam:obo.chebi:CHEBI:15422") && 
								!this.transportMetabolites.get(key).getChEBIMiriam().equals("urn:miriam:obo.chebi:CHEBI:15346"))
						||(fiveAthree && !this.transportMetabolites.get(key).getKeggMiriam().equals("urn:miriam:kegg.compound:C00390") && 
								!this.transportMetabolites.get(key).getKeggMiriam().equals("urn:miriam:kegg.compound:C00399"))) {

					reactantsAndProducts.add(key);
				}
			}
		}

		if(hasVectorialReactions) {

			filteredMetabolites.removeAll(reactantsAndProducts);
		}

		return filteredMetabolites;
	}

	/**
	 * @param originalTransportReaction
	 * @return
	 */
	private List<String> getOriginalBifunctionalMetabolitesList(TransportReaction originalTransportReaction) {

		List<String> originalTransportedMetabolitesList = new ArrayList<String>();

		Map<String,List<String>> metaboliteDirections = originalTransportReaction.getMetaboliteDirection();

		for(String met:metaboliteDirections.keySet()) {

			if(metaboliteDirections.get(met).size()>1) {

				//System.out.println(originalTransportReaction.getReactionID()+"\t"+met);
				originalTransportedMetabolitesList.add(met);
			}
		}
		return originalTransportedMetabolitesList;
	}

	/**
	 * @param list1
	 * @param list2
	 * @return
	 */
	private boolean compareLists(List<?> list1, List<?> list2){

		if(list1.size()==list2.size())
		{
			for(int i =0; i<list1.size();i++)
			{
				//				if(!list1.get(i).equals(list2.get(i)))
				//				{
				//					return false;
				//				}
				list2.remove(list1.get(i));
			}

			if(list2.size()==0)
			{
				return true;
			}
		}
		return false;

	}

	/**
	 * 
	 * 
	 * @param reactionCI
	 * @param geneID
	 * @param proteinID
	 * @param chebi_ontology_byGene 
	 */
	public void addGenetoTransportReaction(TransportReactionCI transportReactionCI, String locus_tag, ProteinFamiliesSet proteinID) {

		transportReactionCI.addGene(locus_tag);

		proteinID.calculateTCfamily_score();

		if(proteinID.getTc_families_above_half()==null || proteinID.getTc_families_above_half().isEmpty())
			transportReactionCI.addProtein(proteinID.getMax_score_family());
		else
			for(String tc_family:proteinID.getTc_families_above_half().keySet()) 
				transportReactionCI.addProtein(tc_family);

		if(!this.genesContainer.containsKey(locus_tag))
			this.addGeneCI(locus_tag);

		this.genesContainer.get(locus_tag).addReactionId(transportReactionCI.getId());
	}

	/**
	 * @param id
	 * @param name
	 * @param reversible
	 * @param reactants
	 * @param products
	 * @param geneID
	 * @param proteinID
	 * @param originalReaction
	 * @param chebi_ontology
	 * @param general_equation_byGene
	 * @param transportType
	 * @param reversibilityConfirmed
	 * @param originalReactionID
	 * @return
	 */
	public String addTransportReaction(String id, String name, boolean reversible, Map<String,StoichiometryValueCI> reactants, 
			Map<String,StoichiometryValueCI> products, String geneID, ProteinFamiliesSet proteinID, 
			Map<String, Boolean> originalReaction, Map<String, Map<String, MetabolitesOntology>> chebi_ontology, Map<String, Map<String, String>> general_equation_byGene, 
			String transportType, Map<String, Boolean> reversibilityConfirmed, Map<String, String> originalReactionID) {

		TransportReactionCI transportReactionCI;

		if(this.reactionsContainer.containsKey(id)) {

			transportReactionCI=this.reactionsContainer.get(id);
		}
		else {

			transportReactionCI = new TransportReactionCI(id, name, reversible, reactants, products);
			transportReactionCI.setType(ReactionTypeEnum.Transport);
			transportReactionCI.setIsOriginalReaction_byGene(originalReaction);
			transportReactionCI.setChebi_ontology_byGene(chebi_ontology);
			transportReactionCI.setGeneral_equation(general_equation_byGene);
			transportReactionCI.setTransportType(transportType);
			transportReactionCI.setReversibilityConfirmed_byGene(reversibilityConfirmed);
			transportReactionCI.setOriginalReactionID_byGene(originalReactionID);
		}

		List<String> mets = new ArrayList<>(transportReactionCI.getMetaboliteSetIds());
		List<String> transportReactions = new ArrayList<>();

		for (int i=0; i< mets.size(); i++) {

			String met = mets.get(i);
			transportReactions = new ArrayList<>();
			if(metabolitesContainer.containsKey(met))
				transportReactions.addAll(metabolitesContainer.get(met).getReactionsId());
			transportReactions.remove(id);
			
			if(keggMiriam.containsKey(met) && keggMiriam.get(met)!= null && !keggMiriam.get(met).equalsIgnoreCase("null") && !keggMiriam.get(met).isEmpty()) {			
				
				String keggID = ExternalRefSource.KEGG_CPD.getSourceId(keggMiriam.get(met));
				if(!keggID.equalsIgnoreCase("C00080") && !keggID.equalsIgnoreCase("C00002") && !keggID.equalsIgnoreCase("C00008") && !keggID.equalsIgnoreCase("C00009")) 
				i = mets.size();
			}
		}

		for (int i=0; i< transportReactions.size(); i++) {

			String tid = transportReactions.get(i);

			if(!reactionsContainer.containsKey(tid))
				tid = existingReactions.get(id);

			TransportReactionCI trci = reactionsContainer.get(tid);

			if(trci.hasSameStoichiometry(transportReactionCI, true)) {

				this.existingReactions.put(transportReactionCI.getId(), trci.getId());
				transportReactionCI = trci;
				i =  transportReactions.size();
			}
		}

		transportReactionCI.addGene(geneID);
		proteinID.calculateTCfamily_score();

		if(proteinID.getTc_families_above_half()==null || proteinID.getTc_families_above_half().isEmpty())
			transportReactionCI.addProtein(proteinID.getMax_score_family());
		else
			for(String tc_family:proteinID.getTc_families_above_half().keySet()) 
				transportReactionCI.addProtein(tc_family);

		synchronized(this.reactionsContainer) {

			this.reactionsContainer.put(id, transportReactionCI);
		}

		return transportReactionCI.getId();
	}

	/**
	 * @param metaboliteID
	 * @param name
	 * @param formula
	 */
	public MetaboliteCI addMetaboliteCI(String metaboliteID, String name, String formula) {

		MetaboliteCI metaboliteCI;

		if(this.metabolitesContainer.containsKey(metaboliteID))
			metaboliteCI=this.metabolitesContainer.get(metaboliteID);
		else
			metaboliteCI= new MetaboliteCI(metaboliteID, name);


		if(formula!=null)
			metaboliteCI.setFormula(formula);

		return metaboliteCI;
	}

	/**
	 * @param geneID
	 * @param locus_tag
	 */
	public void addGeneCI(String locus_tag) {

		if(!this.genesContainer.containsKey(locus_tag)) {

			GeneCI geneCI = new GeneCI(locus_tag, locus_tag);
			this.genesContainer.put(locus_tag, geneCI);
		}
	}

	/**
	 * @param counter the counter to set
	 */
	public void setCounter(AtomicInteger counter) {
		this.counter = counter;
	}

	/**
	 * @return the counter
	 */
	public AtomicInteger getCounter() {
		return counter;
	}

	/**
	 * @param cancel the cancel to set
	 */
	public void setCancel(AtomicBoolean cancel) {
		this.cancel = cancel;
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
}
