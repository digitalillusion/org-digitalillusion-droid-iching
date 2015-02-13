package org.digitalillusion.droid.iching.utils.lists;

import java.io.Serializable;
import java.util.Date;

/**
 * An entry of the history list
 *
 * @author digitalillusion
 */
public class HistoryEntry implements Serializable {

  private static final long serialVersionUID = 5369219005455317857L;

  /**
   * The user question *
   */
  private String question;
  /**
   * The currently generated hexagram *
   */
  private int[] hex;
  /**
   * The changing line index *
   */
  private int changing;
  /**
   * The hexagram transformed from the currently generated one *
   */
  private int[] tHex;
  /**
   * The date when the question was posed *
   */
  private Date date;

  public int getChanging() {
    return changing;
  }

  public void setChanging(int changing) {
    this.changing = changing;
  }

  public Date getDate() {
    return date;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  public int[] getHex() {
    return hex;
  }

  public void setHex(int[] hex) {
    this.hex = hex;
  }

  public String getQuestion() {
    return question;
  }

  public void setQuestion(String question) {
    this.question = question;
  }

  public int[] getTHex() {
    return tHex;
  }

  public void setTHex(int[] tHex) {
    this.tHex = tHex;
  }
}
