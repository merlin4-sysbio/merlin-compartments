/**
 * 
 */
package pt.uminho.ceb.biosystems.merlin.transporters.core.compartments;

import java.io.File;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * @author ODias
 *
 */
public abstract interface CompartmentsInterface {
	
	/**
	 * @return
	 */
	public boolean isEukaryote();
	
	/**
	 * @return
	 */
	public void setPlant(boolean typePlant);
	
	/**
	 * @param project_id
	 * @throws Exception
	 */
	public void loadCompartmentsInformation(Map<String, CompartmentResult> results, int projectID, Statement statement) throws Exception;
	
	/**
	 * @param threshold
	 * @param project_id
	 * @return
	 */
	public Map<String,GeneCompartments> getBestCompartmentsByGene(double threshold, int projectID, Statement statement) throws SQLException;

	/**
	 * @param string
	 * @return 
	 * @throws Exception 
	 */
	public boolean getCompartments(String string) throws Exception;

	/**
	 * @return
	 */
	public AtomicBoolean isCancel();

	/**
	 * @param cancel
	 */
	public void setCancel(AtomicBoolean cancel);

	/**
	 * @param outFile
	 * @return
	 * @throws Exception 
	 */
	public Map<String, CompartmentResult> addGeneInformation(File outFile) throws Exception;
	
	/**
	 * @param link
	 * @return
	 * @throws Exception 
	 */
	public Map<String, CompartmentResult> addGeneInformation(String link) throws Exception;

}
