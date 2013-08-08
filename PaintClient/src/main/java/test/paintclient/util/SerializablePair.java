package test.paintclient.util;

import android.graphics.Paint;
import android.graphics.Path;
import android.util.Pair;

import java.io.Serializable;

/**
 * Created by elvislee on 7/30/13.
 */


public class SerializablePair implements Serializable {
    public SerialPath path;
    public SerialPaint paint;
    public SerializablePair(SerialPath path, SerialPaint paint) {
        this.path = path;
        this.paint = paint;
    }
}
