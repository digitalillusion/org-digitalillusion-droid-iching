package org.digitalillusion.droid.iching.changinglines;

import org.digitalillusion.droid.iching.utils.Consts;

/**
 * Implementor of the Master Yin evaluator
 * @author digitalillusion
 */
public class MasterYinEvaluator extends ChangingLinesEvaluator {

	@Override
	public int evaluate(int[] hex, int[] tHex) {
		// Master Yin's rules
		int changingCount = 0;
		int changing = -1;
		for (int i = 0; i < 6; i++) {
			if (isChangingLine(hex[i])) {
				changingCount++;
			}
			tHex[i] = getChangedLine(hex[i]);
		}
		
		switch (changingCount) {
			case 0 :
				// Read only the Cast Hexagram
				changing = ICHING_APPLY_CAST; 
				break;
			case 1 :
				// Consult this changing line
				for (int i = 0; i < 6; i++) {
					if (isChangingLine(hex[i])) {
						changing = i;
						break;
					}
				}
				break;
			case 2 :
				// If both lines are Six or Nine, the Upper Line applies.
				// If one is Six and the other Nine, the Six prevails.
				int sum = 0;
				for (int i = 0; i < 6; i++) {
					if (isChangingLine(hex[i])) {
						sum += hex[i];
					}
				}
				if ((sum == Consts.ICHING_OLD_YANG*2) || (sum == Consts.ICHING_OLD_YIN*2)) {
					boolean taken = false;
					for (int i = 5; i >= 0; i--) {
						if (!taken && isChangingLine(hex[i])) {
							taken = true;
							changing = i;
						}
					}	
				} else { 
					for (int i = 5; i >= 0; i--) {
						if (isChangingLine(hex[i]) && hex[i] == Consts.ICHING_OLD_YIN) {
							changing = i;
						}
					}
				}
				break;
			case 3 :
				// The middle line counts
				boolean dropped = false;
				for (int i = 0; i < 6; i++) {
					if (isChangingLine(hex[i])) {
						if (dropped) {
							changing = i;
						} else {
							dropped = true;
						}
					}
				}
				break;
			case 4 :
				// Read the upper NON-changing line
				boolean taken = false;
				for (int i = 5; i >= 0; i--) {
					if (taken || isChangingLine(hex[i])) {
					} else {
						taken = true;
						changing = i;
						tHex[i] = getChangedLine(hex[i]);
					}
				}
				break;
			case 5 :
				// Read the only NON-changing line
				for (int i = 5; i >= 0; i--) {
					if (!isChangingLine(hex[i])) {
						changing = i;
					}
				}
				break;
			case 6 :
				// Only the Transformed Hexagram applies.
				// With two exceptions: If all lines are Six or all lines are Nine, 
				// read both Cast Hexagram and Transformed Hexagram.
				changing = ICHING_APPLY_TRANSFORMED;
				sum = 0;
				for (int i = 0; i < 6; i++) {
					sum += hex[i];
				}
				if(sum == Consts.ICHING_OLD_YANG*6 || sum == Consts.ICHING_OLD_YIN*6) {
					changing = ICHING_APPLY_BOTH;
				}
				break;
		}
		return changing;
	}

}
