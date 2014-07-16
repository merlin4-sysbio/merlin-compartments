/**
 * 
 */
package pt.uminho.sysbio.common.transporters.core.compartments;

import java.io.File;
import java.util.Map;

/**
 * @author ODias
 *
 */
public class RunPSortMain {

	/**
	 * 
	 */
	public RunPSortMain() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {

		PSort3 pSort3 =new PSort3(true, -1);

		Map<String, PSort3_result> res = pSort3.addGeneInformation(new File("D:/My Dropbox/WORK/Projecto_PEM/reu/PSort/psortb-results_extr2.txt"));

		for(PSort3_result p : res.values()) {

			System.out.println(p.getGeneID());
		}
	}

}
