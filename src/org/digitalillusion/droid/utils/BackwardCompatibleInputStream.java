package org.digitalillusion.droid.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

class BackwardCompatibleInputStream extends ObjectInputStream {

    public BackwardCompatibleInputStream(InputStream in) throws IOException {
        super(in);
    }

    @Override
    protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
        ObjectStreamClass resultClassDescriptor = super.readClassDescriptor();

        if (resultClassDescriptor.getName().equals("org.digitalillusion.droid.iching.HistoryEntry")) {
            resultClassDescriptor = ObjectStreamClass.lookup(org.digitalillusion.droid.utils.lists.HistoryEntry.class);
        }

        return resultClassDescriptor;
    }
}
