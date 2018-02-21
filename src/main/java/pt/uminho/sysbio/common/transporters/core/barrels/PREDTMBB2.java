package pt.uminho.sysbio.common.transporters.core.barrels;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.biojava.nbio.core.sequence.template.AbstractSequence;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

public class PREDTMBB2 {

	private final static String LINK = "http://195.251.108.230/PRED-TMBB2/";
	private final static int LIMIT = 5;

	/**
	 * Method to access PRED-TMBB2, perform the submission and retrieve the results link.
	 * 
	 * @param sequences
	 * @return
	 * @throws InterruptedException 
	 */
	public static String getLink(Map<String, AbstractSequence<?>> sequences) throws InterruptedException{

		String query = getCorrectFormatFasta(sequences);

		WebDriver driver = new HtmlUnitDriver();

		// And now use this to visit Google
		driver.get(LINK);

		// Find the text input element by its name
		WebElement element = driver.findElement(By.name("sequence"));			//text area
		element.sendKeys(query);

		element = driver.findElement(By.xpath("/html/body/form/input[2]"));		//check prediction for batch
		element.click();

		element = driver.findElement(By.xpath("/html/body/form/input[3]"));		//uncheck Signal peptide predictions
		element.click();

		element = driver.findElement(By.xpath("/html/body/form/input[7]"));		//run prediction
		element.click();

		//		TimeUnit.SECONDS.sleep();		see if necessary
		
		boolean go = false;
		String currentUrl = null;
		int errorCounter = 0;
		
		while(!go && errorCounter < LIMIT){
			
			try{

				currentUrl = driver.getCurrentUrl();
				
				go = true;
			}
			catch(Exception e){
				
				errorCounter ++;
				TimeUnit.SECONDS.sleep(10);
			}
		}

		return currentUrl;
	}

	/**
	 * Method to put the query in fasta format for submission
	 * 
	 * @param sequences
	 * @return
	 */
	public static String getCorrectFormatFasta(Map<String, AbstractSequence<?>> sequences){

		String query = "";

		for(String sequence : sequences.keySet()){

			query = query.concat(">").concat(sequence).concat("\n").concat(sequences.get(sequence).toString()).concat("\n");

		}

		return query;
	}

}
