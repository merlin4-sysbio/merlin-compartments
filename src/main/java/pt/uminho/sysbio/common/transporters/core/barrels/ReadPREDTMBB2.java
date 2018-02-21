package pt.uminho.sysbio.common.transporters.core.barrels;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import pt.uminho.sysbio.common.transporters.core.utils.RetrieveRemoteResults;

public class ReadPREDTMBB2 {
	
	public static Map<String, Integer> readResults(String link, Map<String, String> identifiers){

		boolean go = false;
		
		Map<String, Integer> results = new HashMap<>();

		try {
			while(!go){

				go = true;

				BufferedReader in = RetrieveRemoteResults.retrieveDataFromURL(link);

				if(in != null){
					
					String html;
					Document doc;
					String text;

					int entry = -1, length = -1, sp = -1, score = -1, ompdbFamily = -1, tmNumber = -1, reliability = -1, image = -1;

					String entryRes = "";
					int tmNumberRes = 0;

					boolean header = true;
					int i = 0, lastCol = 0;

					while ((html = in.readLine()) != null){

						doc = Jsoup.parse(html);
						text = doc.body().text().trim();
						
						if(text.contains("This page will be automatically reloaded")){
							go = false;
						}

						else{

							if(header){

								if(text.equalsIgnoreCase("Entry")){
									entry = i;
									i++;
								}
								else if(text.equalsIgnoreCase("Length")){
									length = i;
									i++;
								}
								else if(text.equalsIgnoreCase("SP")){
									sp = i;
									i++;
								}
								else if(text.contains("β-score")){
									score = i;
									i++;
								}
								else if(text.equalsIgnoreCase("OMPdb family")){
									ompdbFamily = i;
									i++;
								}
								else if(text.equalsIgnoreCase("#TM")){
									tmNumber = i;
									i++;
								}
								else if(text.equalsIgnoreCase("Reliability")){
									reliability = i;
									i++;
								}
								else if(text.equalsIgnoreCase("Image")){
									image = i;

									lastCol = i;
									i = -2;
									header = false;
									
//									System.out.println(entry + " " + length + " " + sp + " " + score + " " + ompdbFamily + " " + tmNumber + " " + reliability + " " + image );
								}

							}
							else{

								if (i < (lastCol)){

									if(i == entry){
										entryRes = identifiers.get(text.replaceAll("[^A-Za-z0-9]", ""));
									}
									else if(i == tmNumber){
										
										if(text.contains("--"))
											tmNumberRes = 0;
										else
											tmNumberRes = Integer.parseInt(text);
									}

									i++;
								}
								else{
									
									results.put(entryRes, tmNumberRes);
									i = -2;
								}
							}

						}

					}

					if(!go){
						System.out.println("Waiting...");
						TimeUnit.MINUTES.sleep(1);
					}
					
				}
				
				in.close();

			}
			
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
		return results;
	}

}
