/**
 * 
 */
package pt.uminho.sysbio.common.transporters.core.compartments;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * @author ODias
 *
 */
public abstract interface PSortInterface {
	
	
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
	public Map<String,GeneCompartments> getBestCompartmentsByGene(double threshold);

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

}
