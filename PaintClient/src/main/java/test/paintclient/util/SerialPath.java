package test.paintclient.util;

import android.graphics.Path;

import java.io.Serializable;

/**
 * Created by elvislee on 7/30/13.
 */
public class SerialPath extends Path implements Serializable {
    public SerialPath() {
        super();
    }

    public SerialPath(SerialPath path) {
        super(path);
    }
}
