package org.digitalillusion.droid.iching.changinglines;

import org.digitalillusion.droid.iching.utils.Consts;

/**
 * Proxy class for the changing lines evaluators
 * @author digitalillusion
 */
public abstract class ChangingLinesEvaluator {
	
	public static ChangingLinesEvaluator produce(Integer evalType) {
		if (evalType == Consts.EVALUATOR_MASTERYIN) {
			return new MasterYinEvaluator();
		} else if (evalType == Consts.EVALUATOR_NAJING) {
			return new NanjingEvaluator();
		}
		return new ChangingLinesEvaluator() {
			@Override
			public int evaluate(int[] hex, int[] tHex) {
				int changingCount = 0;
				for (int i = 0; i < 6; i++) {
					if (isChangingLine(hex[i])) {
						changingCount++;
					}
					tHex[i] = getChangedLine(hex[i]);
				}
				if (changingCount == 0) {
					return ICHING_APPLY_CAST;
				}
				return ICHING_APPLY_MANUAL;
			}			
		};
	}
	
	/**
	 * @param lineVal The line to evaluate
	 * @return True if the line is changing, false otherwise
	 */
	public static boolean isChangingLine(int lineVal) {
		return lineVal == Consts.ICHING_OLD_YANG || lineVal == Consts.ICHING_OLD_YIN;
	}
	
	/**
	 * @param lineVal The line to evaluate
	 * @return The changed line if the line is changing, the same line otherwise
	 */
	public static int getChangedLine(int lineVal) {
		if (lineVal == Consts.ICHING_OLD_YANG) {
			return Consts.ICHING_YOUNG_YIN;
		} else if (lineVal == Consts.ICHING_OLD_YIN) {
			return Consts.ICHING_YOUNG_YANG;
		} else {
			return lineVal;
		}
	}

	/** Flag to use when cast hexagram applies **/
	public static final int ICHING_APPLY_CAST = -1;
	
	/** Flag to use when transformed hexagram applies **/
	public static final int ICHING_APPLY_TRANSFORMED = -2;
	
	/** Flag to use when both cast and transformed hexagrams apply **/
	public static final int ICHING_APPLY_BOTH = -3;
	
	/** Flag to use when nothing in the reading applies (unused) **/
	public static final int ICHING_APPLY_NONE = -4;
	
	/** Flag to use when the reading is manual **/
	public static final int ICHING_APPLY_MANUAL = -5;
	
	/**
	 * Evaluate the cast and transformed hexagrams to find the most important
	 * changing line
	 * 
	 * @param hex The cast hexagram
	 * @param tHex The transformed hexagrams
	 * @return The position of the most important changing line
	 */
	public abstract int evaluate(int[] hex, int[] tHex);
}
