/**
 * 
 */
package pt.uminho.sysbio.common.transporters.core.compartments;

import java.io.File;
import java.sql.SQLException;
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
	 * @param project_id
	 * @throws Exception
	 */
	public void loadCompartmentsInformation() throws Exception;
	
	/**
	 * @param threshold
	 * @param project_id
	 * @return
	 */
	public Map<String,GeneCompartments> getBestCompartmentsByGene(double threshold) throws SQLException;

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

}
