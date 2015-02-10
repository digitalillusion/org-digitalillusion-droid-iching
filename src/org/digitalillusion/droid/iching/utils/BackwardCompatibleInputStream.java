package org.digitalillusion.droid.iching.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

/**
 * Maps serialized HistoryEntry of version prior to 1.2 to current version
 *
 * @author digitalillusion
 */
class BackwardCompatibleInputStream extends ObjectInputStream {

  public BackwardCompatibleInputStream(InputStream in) throws IOException {
    super(in);
  }

  @Override
  protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
    ObjectStreamClass resultClassDescriptor = super.readClassDescriptor();

    if (resultClassDescriptor.getName().equals("org.digitalillusion.droid.iching.HistoryEntry") ||
        resultClassDescriptor.getName().equals("org.digitalillusion.droid.utils.lists.HistoryEntry")) {
      resultClassDescriptor = ObjectStreamClass.lookup(org.digitalillusion.droid.iching.utils.lists.HistoryEntry.class);
    }

    return resultClassDescriptor;
  }
}
