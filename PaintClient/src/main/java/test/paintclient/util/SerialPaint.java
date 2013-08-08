package test.paintclient.util;

import android.graphics.Paint;

import java.io.Serializable;

/**
 * Created by elvislee on 7/30/13.
 */
public class SerialPaint extends Paint implements Serializable {
    public SerialPaint(int flags) {
        super(flags);
    }
    public SerialPaint() {
        super();
    }
}
