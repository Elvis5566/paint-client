package test.paintclient;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class ConnectionActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.connection, menu);
        return true;
    }

    public void onConnect(View view) {
        EditText editIP = (EditText) findViewById(R.id.editIP);
        EditText editPort = (EditText) findViewById(R.id.editPort);
        String IP = editIP.getText().toString();

        try {
            int port = Integer.parseInt(editPort.getText().toString());
            Intent intent = new Intent();
            intent.putExtra("IP", IP);
            intent.putExtra("port", port);
            this.setResult(RESULT_OK, intent);
            this.finish();
        } catch (NumberFormatException e) {
            return;
        }

    }


}
