package pt.uminho.ceb.biosystems.merlin.compartments.services;

import java.io.BufferedReader;
import java.io.File;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import pt.uminho.ceb.biosystems.merlin.compartments.datatype.AnnotationCompartmentsGenes;
import pt.uminho.ceb.biosystems.merlin.compartments.datatype.AnnotationCompartmentsWolfPsort;
import pt.uminho.ceb.biosystems.merlin.compartments.interfaces.ICompartmentsServices;
import pt.uminho.ceb.biosystems.merlin.compartments.processes.CompartmentsInitializationProcesses;
import pt.uminho.ceb.biosystems.merlin.compartments.utils.RetrieveRemoteResults;
import pt.uminho.ceb.biosystems.merlin.core.interfaces.ICompartmentResult;

public class ComparmentsImportWolfPsortServices implements ICompartmentsServices{

	private int normalization = 32;
	private AtomicBoolean cancel;

	/**
	 * @param conn
	 * @param genomeCode
	 * @param project_id
	 */
	public ComparmentsImportWolfPsortServices() {

		this.cancel = new AtomicBoolean(false);
	}

	/**
	 * @param type
	 * @param genome_file_path
	 * @param out
	 * @return
	 */
	public static boolean getCompartments(String type, String genome_file_path, String out) throws Exception {

		String[] args = new String[3];
		args[0]=genome_file_path;
		args[1]=out;
		args[2]=type;
		return true;
	}

	/**
	 * @return
	 * @throws Exception 
	 */
	public Map<String, ICompartmentResult> readWoLFPSORTFile(BufferedReader in) throws Exception {

		Map<String, ICompartmentResult> compartmentLists = new HashMap<>();

		String inputLine;
		
		while ((inputLine = in.readLine()) != null && !this.cancel.get()) {
			
			Document doc = Jsoup.parse(inputLine);
			String str = doc.body().text();
			
			if(!str.isEmpty()){

				String[] line = str.split(" details ");
				
				String locusTag = line[0].trim();
				
				String[] comp = line[1].split(", ");
				
				AnnotationCompartmentsWolfPsort WoLFPSORTResult = new AnnotationCompartmentsWolfPsort(locusTag);
				
				for(int i = 0; i<comp.length; i++){
					
					String[] score = comp[i].split(": ");
					
					if(score.length == 2){
						
						String[] value = score[1].split("\\s+");
						WoLFPSORTResult.addCompartment(score[0], Double.valueOf(value[0].trim()));
					}
				}
				compartmentLists.put(locusTag, WoLFPSORTResult);
			}
		}
		
		in.close();
		return compartmentLists;
	}

	
	
	
	/* (non-Javadoc)
	 * @see compartments.CompartmentsInterface#getBestCompartmentsForGene(double)
	 */
	public Map<Integer, AnnotationCompartmentsGenes> getBestCompartmentsByGene(double threshold, Statement statement) throws SQLException  {

		return CompartmentsInitializationProcesses.getBestCompartmenForGene(threshold, this.normalization, statement);
	}

	@Override
	public AtomicBoolean isCancel() {
		return this.cancel;
	}

	@Override
	public void setCancel(AtomicBoolean cancel) {
		this.cancel = cancel;
	}


	@Override
	public boolean isEukaryote() {

		return true;
	}

	@Override
	public Map<String, ICompartmentResult> addGeneInformation(File outFile) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Map<String, ICompartmentResult> addGeneInformation(String link) {
		
		Map<String, ICompartmentResult> results = null;
		
		try {
			
			BufferedReader data = RetrieveRemoteResults.retrieveDataFromURL(link);
			results = this.readWoLFPSORTFile(data);
			
		} catch (Exception e) {
			e.printStackTrace();
		}	
		
		return results;
	}
	
	public void loadCompartmentsInformation(Map<String, ICompartmentResult> results, Statement statement)
			throws Exception {

		for(ICompartmentResult woLFPSORT_Result : results.values()) {

			CompartmentsInitializationProcesses.loadData(woLFPSORT_Result.getGeneID(), woLFPSORT_Result.getCompartments(), statement);
		}
		
	}

	@Override
	public boolean getCompartments(String string) throws Exception {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setPlant(boolean typePlant) {
		// TODO Auto-generated method stub
		
	}

}
