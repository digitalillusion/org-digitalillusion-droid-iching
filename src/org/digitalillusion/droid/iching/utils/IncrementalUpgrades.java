package org.digitalillusion.droid.iching.utils;

import org.digitalillusion.droid.iching.IChingActivityRenderer;
import org.digitalillusion.droid.iching.connection.RemoteResolver;

/**
 * Allows to perform a one time initialisation upon application upgrade
 *
 * @author digitalillusion
 */

public class IncrementalUpgrades {

  private final IChingActivityRenderer activity;

  public IncrementalUpgrades(IChingActivityRenderer activity) {
    this.activity = activity;
  }

  public void onAppUpdated(int buildVersion) {
    if (buildVersion < 67) {
      onUpdateTo67();
    }
  }

  private void onUpdateTo67() {
    activity.getHexSectionDataSource().deleteHexSection("32", Consts.DICTIONARY_ALTERVISTA, RemoteResolver.ICHING_REMOTE_SECTION_DESC, Consts.LANGUAGE_PT);
  }
}
