package test.paintclient;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;


import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class MainActivity extends Activity {
    private final String TAG = "PaintClient";
    private CanvasView view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        view = new CanvasView(this);
        setContentView(view);
    }

    private static final int COLOR_MENU_ID = Menu.FIRST;
    private static final int DRAW_LINE_ID = Menu.FIRST + 1;
    private static final int DRAW_CIRCLE_ID = Menu.FIRST + 2;
    private static final int DRAW_RECTANGLE_ID = Menu.FIRST + 3;
    private static final int CLEAR_CANVAS_ID = Menu.FIRST + 4;
    private static final int CONNECT_ID = Menu.FIRST + 5;

    public static final int CONNECT_REQUEST = 0;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.main, menu);
        menu.add(0, COLOR_MENU_ID, 0, "Pick color");
        menu.add(0, DRAW_LINE_ID, 0, "Draw line");
        menu.add(0, DRAW_CIRCLE_ID, 0, "Draw circle");
        menu.add(0, DRAW_RECTANGLE_ID, 0, "Draw rectangle");
        menu.add(0, CLEAR_CANVAS_ID, 0, "Clear canvas");
        menu.add(0, CONNECT_ID, 0, "Connect to server");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case COLOR_MENU_ID:
                view.showColorPicker();
                return true;
            case DRAW_LINE_ID:
                view.setDrawType(CanvasView.TYPE_LINE);
                view.setPaintStyle(Paint.Style.STROKE);
                Toast.makeText(this, "Draw line", Toast.LENGTH_SHORT).show();
                break;

            case DRAW_CIRCLE_ID:
                view.setDrawType(CanvasView.TYPE_CIRCLE);
                view.setPaintStyle(Paint.Style.FILL);
                Toast.makeText(this, "Draw circle", Toast.LENGTH_SHORT).show();
                break;

            case DRAW_RECTANGLE_ID:
                view.setDrawType(CanvasView.TYPE_RECT);
                view.setPaintStyle(Paint.Style.FILL);
                Toast.makeText(this, "Draw rectangle", Toast.LENGTH_SHORT).show();
                break;
            case CLEAR_CANVAS_ID:
                view.eraseCanvas();
                break;
            case CONNECT_ID:
                connect();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void connect() {
        Intent intent = new Intent(this, ConnectionActivity.class);
        startActivityForResult(intent, CONNECT_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null)
            return;

        Bundle bundle = data.getExtras();
        String IP = bundle.getString("IP");
        int port = bundle.getInt("port");
        Toast.makeText(MainActivity.this, "Connecting...", Toast.LENGTH_SHORT).show();
        Thread thread = new SocketThread(IP, port, getApplicationContext());
        thread.start();
    }

    class SocketThread extends Thread {
        private Socket server;
        private String mIP;
        private int mPort;
        private Context mContext;
        public SocketThread(String IP, int port, Context context) {
            mIP = IP;
            mPort = port;
            mContext = context;
        }

        @Override
        public void run() {
            try {
                server=new Socket(mIP, mPort);

            } catch (IOException e) {
//                view.post(new Runnable() {
//                    public void run() {
//                        Toast.makeText(mContext, "Connect fail", Toast.LENGTH_SHORT).show();
//                    }
//                });
                e.printStackTrace();
            }

            PrintWriter out = null;
            BufferedReader in = null;

            try {
                out = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(server.getOutputStream())), true);

                in = new BufferedReader(
                        new InputStreamReader(server.getInputStream()));

            } catch (IOException e) {
                Log.i("SocketThread", "get stream fail");
                e.printStackTrace();
                return;
            } catch (NullPointerException e) {
                view.post(new Runnable() {
                    public void run() {
                        Toast.makeText(mContext, "Server Connect fail", Toast.LENGTH_SHORT).show();
                    }
                });

                return;
            }

            view.post(new Runnable() {
                public void run() {
                    Toast.makeText(mContext, "Server Connected", Toast.LENGTH_SHORT).show();
                }
            });
            view.setWriter(out);

            while(true)
            {
                Gson gson = new Gson();
                String jsonStr = null;
                try {
                    jsonStr = in.readLine();

                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
                final PathTrace trace = gson.fromJson(jsonStr, PathTrace.class);

                // UI thread draw path from server
                view.post(new Runnable() {
                        public void run() {
                            view.addServerPath(trace);
                        }
                    });
            }
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}

