package org.digitalillusion.droid.iching.changinglines;

import org.digitalillusion.droid.iching.utils.Consts;

/**
 * Proxy class for the changing lines evaluators
 *
 * @author digitalillusion
 */
public abstract class ChangingLinesEvaluator {

  /**
   * Flag to use when cast hexagram applies *
   */
  public static final int ICHING_APPLY_CAST = -1;

  /**
   * Flag to use when transformed hexagram applies *
   */
  public static final int ICHING_APPLY_TRANSFORMED = -2;

  /**
   * Flag to use when both cast and transformed hexagrams apply *
   */
  public static final int ICHING_APPLY_BOTH = -3;

  /**
   * Flag to use when nothing in the reading applies (unused) *
   */
  public static final int ICHING_APPLY_NONE = -4;

  /**
   * Flag to use when the reading is manual *
   */
  public static final int ICHING_APPLY_MANUAL = -5;

  /**
   * SORTED Subset of the hexagrams set when all lines changing have a particular meaning *
   */
  public static final Integer[] ICHING_ALL_LINES_DESC = new Integer[]{1, 2};

  /**
   * SORTED Subsets of the hexagrams set when line 0 to 5 is governing *
   */
  public static final Integer[][] ICHING_GOVERNING_LINE = {
      new Integer[]{3, 17, 19, 24, 25, 51},
      new Integer[]{2, 4, 11, 13, 19, 22, 28, 29, 30, 32, 36, 37, 38, 42, 44, 47, 53, 58, 62, 63},
      new Integer[]{15},
      new Integer[]{16, 28, 31, 34, 45},
      new Integer[]{1, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 17, 18, 20, 21, 25, 26, 27, 29, 30, 31, 33, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 64},
      new Integer[]{20, 22, 23, 26, 27, 50, 52}
  };

  /**
   * SORTED Subsets of the hexagrams set when line 0 to 5 is governing *
   */
  public static final Integer[][] ICHING_CONSTITUENT_LINE = {
      new Integer[]{33, 42, 44, 46, 57},
      new Integer[]{12, 33, 59},
      new Integer[]{10, 41, 54, 58, 61},
      new Integer[]{9, 42, 57, 59, 61},
      new Integer[]{},
      new Integer[]{36, 41, 43, 54, 58},
  };
  public static final String READ_CHANGING_SELECT_LINE = "read_changing_select_line";

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

  /**
   * @param lineVal The line to evaluate
   * @return The changing value of the given line
   */
  public static int getChangingLineOf(int lineVal) {
    if (lineVal == Consts.ICHING_YOUNG_YANG) {
      return Consts.ICHING_OLD_YANG;
    } else if (lineVal == Consts.ICHING_YOUNG_YIN) {
      return Consts.ICHING_OLD_YIN;
    } else {
      return lineVal;
    }
  }

  /**
   * @param lineVal The line to evaluate
   * @return True if the line is changing, false otherwise
   */
  public static boolean isChangingLine(int lineVal) {
    return lineVal == Consts.ICHING_OLD_YANG || lineVal == Consts.ICHING_OLD_YIN;
  }

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
        for (int i = 0; i < Consts.HEX_LINES_COUNT; i++) {
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
   * Evaluate the cast and transformed hexagrams to find the most important
   * changing line
   *
   * @param hex  The cast hexagram
   * @param tHex The transformed hexagrams
   * @return The position of the most important changing line
   */
  public abstract int evaluate(int[] hex, int[] tHex);
}
