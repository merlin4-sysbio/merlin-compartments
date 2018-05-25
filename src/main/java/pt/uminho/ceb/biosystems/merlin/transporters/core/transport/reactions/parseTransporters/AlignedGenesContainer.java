/**
 * 
 */
package pt.uminho.ceb.biosystems.merlin.transporters.core.transport.reactions.parseTransporters;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ODias
 *
 */
public class AlignedGenesContainer {

	private String locusTag;
	private List<AlignmentResult> alignmentResult;
	
	/**
	 * 
	 */
	public AlignedGenesContainer() {
		
		this.alignmentResult = new ArrayList<AlignmentResult>();
	}

	/**
	 * @param locusTag
	 */
	public AlignedGenesContainer(String locusTag) {
		
		super();
		this.locusTag = locusTag;
		this.alignmentResult = new ArrayList<AlignmentResult>();
	}
	
	/**
	 * @param alignmentResult
	 */
	public void addAlignmentResult (AlignmentResult alignmentResult) {
		
		this.alignmentResult.add(alignmentResult);
	}

	/**
	 * @return the locusTag
	 */
	public String getLocusTag() {
		return locusTag;
	}

	/**
	 * @param locusTag the locusTag to set
	 */
	public void setLocusTag(String locusTag) {
		this.locusTag = locusTag;
	}

	/**
	 * @return the alignment
	 */
	public List<AlignmentResult> getAlignmentResult() {
		return alignmentResult;
	}

	/**
	 * @param alignmentResult the alignment to set
	 */
	public void setAlignmentResult(List<AlignmentResult> alignmentResult) {
		this.alignmentResult = alignmentResult;
	}

	@Override
	public String toString() {
		return "AlignedGenesContainer [locusTag=" + locusTag
				+ ", alignmentResult=" + alignmentResult + "]";
	}

}
