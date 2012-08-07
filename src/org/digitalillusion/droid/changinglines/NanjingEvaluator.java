package org.digitalillusion.droid.changinglines;

/**
 * Implementor of the Nanjing evaluator
 * @author digitalillusion
 */
public class NanjingEvaluator extends ChangingLinesEvaluator {

	@Override
	public int evaluate(int[] hex, int[] tHex) {
		int sum = 0;
		int changingCount = 0;
		for(int i = 0; i < 6; i++) {
			sum += hex[i];
			if (isChangingLine(hex[i])) {
				changingCount++;
			}
			tHex[i] = getChangedLine(hex[i]);
		}
		
		if (changingCount == 0) {
			return ICHING_APPLY_CAST;
		} else {
			int changing = (54 - sum)%5;
			
			if (!isChangingLine(hex[changing]) && changingCount == 3) {
				return ICHING_APPLY_NONE;
			} else {
				return changing;
			}
		}
	}

}
