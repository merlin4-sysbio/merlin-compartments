package pt.uminho.ceb.biosystems.merlin.transporters.core.compartments;

import java.io.BufferedReader;
import java.io.File;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class WoLFPSORT implements CompartmentsInterface{

	private int normalization = 32;
	private AtomicBoolean cancel;

	/**
	 * @param conn
	 * @param genomeCode
	 * @param project_id
	 */
	public WoLFPSORT() {

		this.cancel = new AtomicBoolean(false);
	}

	//	/**
	//	 * @param msqlmt
	//	 * @throws SQLException
	//	 */
	//	public WoLFPSORT(Connection conn) throws SQLException  {
	//
	//		this.loadCompartments = new LoadCompartments(conn);
	//		this.cancel = new AtomicBoolean(false);
	//	}

	/**
	 * @param type
	 * @param genome_file_path
	 * @param out
	 * @return
	 */
	public static boolean getCompartments(String type, String genome_file_path, String out) throws Exception {

		String[] args = new String[3];
		args[0]=genome_file_path;
		//args[1]=this.tempPath+this.genomeCode+".out";
		args[1]=out;
		args[2]=type;
//		for(String a : args)
//			System.out.print(a+" ");
//
//		try {
//			WoLFPsort.main(args);
//		} catch (Exception e) {
//			System.err.println("Exception caught");
//			throw e;
//		} catch (Error er) {
//			System.err.println("Error caught");
//			throw er;
//		}
		return true;
	}

	/**
	 * @return
	 * @throws Exception 
	 */
	public Map<String, CompartmentResult> readWoLFPSORTFile(BufferedReader in) throws Exception {

		Map<String, CompartmentResult> compartmentLists = new HashMap<>();

		String inputLine;
		
		while ((inputLine = in.readLine()) != null && !this.cancel.get()) {
			
			Document doc = Jsoup.parse(inputLine);
			String str = doc.body().text();
			
			if(!str.isEmpty()){

				String[] line = str.split(" details ");
				
				String locusTag = line[0].trim();
				
				String[] comp = line[1].split(", ");
				
				WoLFPSORT_Result WoLFPSORTResult = new WoLFPSORT_Result(locusTag);
				
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

	
	
	
	

//	/**
//	 * @return
//	 * @throws Exception 
//	 */
//	public List<WoLFPSORT_Result> readWoLFPSORTFile(BufferedReader in) throws Exception {
//
//		List<WoLFPSORT_Result> compartmentLists = new ArrayList<WoLFPSORT_Result>();
//		Map<String,Integer> locus_tags = new HashMap<String, Integer>();
//
//		try {
//
//			String str;
//			int index=0;
//
//			while ((str = in.readLine()) != null && !this.cancel.get()) {
//				System.out.println(str);
//
//				StringTokenizer st = new StringTokenizer(str,",");
//				int i = 0;
//				String[] results = new String[st.countTokens()];
//	
//				while(st.hasMoreTokens()) {
//	
//					results[i] = st.nextToken();
//					i++;
//				}
//	
//				StringTokenizer id_result_tokenizer = new StringTokenizer(results[0]," ");
//				String id = id_result_tokenizer.nextToken();
//				String result = id_result_tokenizer.nextToken();
//				double score = Double.valueOf(id_result_tokenizer.nextToken());
//				WoLFPSORT_Result woLFPSORT_Result = new WoLFPSORT_Result(id);
//	
//				locus_tags.put(id, index);
//	
//				woLFPSORT_Result.addCompartment(result, score);
//	
//				for(int j = 1; j < results.length; j++) {
//	
//					id_result_tokenizer = new StringTokenizer(results[j]," ");
//					id = id_result_tokenizer.nextToken();
//					result = id_result_tokenizer.nextToken();
//					woLFPSORT_Result.addCompartment(id,  Double.valueOf(result));
//				}
//				compartmentLists.add(index,woLFPSORT_Result);
//				index++;
//				}
//			in.close();
//
//			if(!this.cancel.get()) {
//
//				List<WoLFPSORT_Result> compartmentResults = new ArrayList<WoLFPSORT_Result>();
//				for(String locus_tag:locus_tags.keySet()) {
//
//					WoLFPSORT_Result woLFPSORT_result = compartmentLists.get(locus_tags.get(locus_tag));
//					woLFPSORT_result.setGeneID(locus_tag);
//					compartmentResults.add(woLFPSORT_result);
//				}
//				
//				for(int i = 0 ; i< compartmentResults.size(); i++){
//					System.out.println(compartmentResults.get(i));
//				}
//				return compartmentResults;
//			}
//		}
//		catch (IOException e) {
//
//			System.out.println("WoLFPSORT output file not Found!\nPlease Run WoLFPSORT and try again!");
//			e.printStackTrace();
//			throw new Exception(e.getMessage());
//		}
//		catch (Exception e) {
//
//			System.err.println("NCBI error!");
//			e.printStackTrace();
//			throw new Exception(e.getMessage());
//		}
//		return null;
//	}

//	/**
//	 * @return
//	 * @throws Exception 
//	 */
//	public List<WoLFPSORT_Result> addGeneInformation(boolean silico) throws Exception {
//
//		List<WoLFPSORT_Result> compartmentLists = new ArrayList<WoLFPSORT_Result>();
//		Map<String,Integer> locus_tags = new HashMap<String, Integer>();
//
//		try {
//
//			BufferedReader in = new BufferedReader(new FileReader(this.tempPath+this.genomeCode+".out"));
//			String str;
//			int index=0;
//
//			while ((str = in.readLine()) != null && !this.cancel.get()) {
//
//				if(str.startsWith("#")) {
//
//					this.normalization = Integer.parseInt(str.split(":")[1].trim());
//				}
//				else {
//
//					StringTokenizer st = new StringTokenizer(str,",");
//					int i = 0;
//					String[] results = new String[st.countTokens()];
//
//					while(st.hasMoreTokens()) {
//
//						results[i] = st.nextToken();
//						i++;
//					}
//
//					StringTokenizer id_result_tokenizer = new StringTokenizer(results[0]," ");
//					String id = id_result_tokenizer.nextToken();
//					String result = id_result_tokenizer.nextToken();
//					double score = Double.valueOf(id_result_tokenizer.nextToken());
//					WoLFPSORT_Result woLFPSORT_Result = new WoLFPSORT_Result(id);
//
//					locus_tags.put(id, index);
//
//					woLFPSORT_Result.addCompartment(result, score);
//
//					for(int j = 1; j < results.length; j++) {
//
//						id_result_tokenizer = new StringTokenizer(results[j]," ");
//						id = id_result_tokenizer.nextToken();
//						result = id_result_tokenizer.nextToken();
//						woLFPSORT_Result.addCompartment(id,  Double.valueOf(result));
//					}
//					compartmentLists.add(index,woLFPSORT_Result);
//					index++;
//				}
//			}
//			in.close();
//
//			if(!this.cancel.get()) {
//
//				List<WoLFPSORT_Result> compartmentResults = new ArrayList<WoLFPSORT_Result>();
//				for(String locus_tag:locus_tags.keySet()) {
//
//					WoLFPSORT_Result woLFPSORT_result = compartmentLists.get(locus_tags.get(locus_tag));
//					woLFPSORT_result.setGeneID(locus_tag);
//					compartmentResults.add(woLFPSORT_result);
//				}
//
//				return compartmentResults;
//			}
//		}
//		catch (IOException e) {
//
//			System.out.println("WoLFPSORT output file not Found!\nPlease Run WoLFPSORT and try again!");
//			e.printStackTrace();
//			throw new Exception(e.getMessage());
//		}
//		catch (Exception e) {
//
//			System.err.println("NCBI error!");
//			e.printStackTrace();
//			throw new Exception(e.getMessage());
//		}
//		return null;
//	}

	/* (non-Javadoc)
	 * @see compartments.CompartmentsInterface#getBestCompartmentsForGene(double)
	 */
	public Map<String,GeneCompartments> getBestCompartmentsByGene(double threshold, int projectID,  Statement statement) throws SQLException  {

		return LoadCompartments.getBestCompartmenForGene(threshold, this.normalization, projectID, statement);
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
	public Map<String, CompartmentResult> addGeneInformation(File outFile) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Map<String, CompartmentResult> addGeneInformation(String link) {
		
		Map<String, CompartmentResult> results = null;
		
		try {
			
			BufferedReader data = RemoteCompartmentsResults.retrieveDataFromURL(link);
			results = this.readWoLFPSORTFile(data);
			
		} catch (Exception e) {
			e.printStackTrace();
		}	
		
		return results;
	}
	
	public void loadCompartmentsInformation(Map<String, CompartmentResult> results, int projectID, Statement statement)
			throws Exception {

		for(CompartmentResult woLFPSORT_Result : results.values()) {

			LoadCompartments.loadData(woLFPSORT_Result.getGeneID(), woLFPSORT_Result.getCompartments(), projectID, statement);
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
