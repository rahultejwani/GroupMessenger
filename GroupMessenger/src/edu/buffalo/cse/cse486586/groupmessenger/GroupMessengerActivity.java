package edu.buffalo.cse.cse486586.groupmessenger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.TextView;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * 
 *
 */
public class GroupMessengerActivity extends Activity {

	private  ContentResolver mContentResolver;
	private  Uri mUri; 
	static final String KEY = "key";
	static final String VALUE = "value";
	static final String TAG = GroupMessengerActivity.class.getSimpleName();
	static final String[] ports = {"11108","11112","11116","11120","11124"};
	static final int SERVER_PORT = 10000;
	static final String requestMarker ="#requestSequence";
	static int GlobalSequence = 0;
	static int LocalSequence = 0;
	private String myPort;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_group_messenger);
		final TextView tv = (TextView) findViewById(R.id.textView1);
		final EditText editText = (EditText) findViewById(R.id.editText1);
		tv.setMovementMethod(new ScrollingMovementMethod());
		TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
		String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
		myPort = String.valueOf((Integer.parseInt(portStr) * 2));

		try {

			ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
			new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
		} catch (IOException e) {

			Log.e("first time", "Can't create a ServerSocket");
			Log.e("error detail",e.getMessage());
			return;
		}





		editText.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
						(keyCode == KeyEvent.KEYCODE_ENTER)) {

					String msg = editText.getText().toString() + "\n";
					editText.setText(""); 
					//					String sequence;
					//					sequence = requestMarker;
					MessageBean mb = new MessageBean(msg, requestMarker, myPort);
					new ClientMulticastTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, mb);
					return true;
				}   
				return false;
			}


		});
		findViewById(R.id.button1).setOnClickListener(
				new OnPTestClickListener(tv, getContentResolver()));

		findViewById(R.id.button4).setOnClickListener(
				new OnClickListener() {

					@Override
					public void onClick(View v) {

						String msg = editText.getText().toString() + "\n";
						editText.setText(""); 
						MessageBean mb = new MessageBean(msg, requestMarker, myPort);
						new ClientMulticastTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, mb);


					}
				});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
		return true;
	}





	private class ServerTask extends AsyncTask<ServerSocket, MessageBean, Void> {
		ArrayList<MessageBean> buffer = new ArrayList<MessageBean>();
		@Override
		protected Void doInBackground(ServerSocket... sockets) {
			ServerSocket serverSocket = sockets[0];



			while(true)
			{

				try {
					Socket client = serverSocket.accept();
					ObjectInputStream Din = new ObjectInputStream(new BufferedInputStream(client.getInputStream()));
					MessageBean mb = (MessageBean) Din.readObject();
					Log.i("server recieved text ", mb.getText());
					Log.i("server recieved Sequence", mb.getSequence());
					Log.i("server recieved port", mb.getPort());
					publishProgress(mb);

				} catch (IOException e) {
					Log.e("ServerTask", "Can't get data from client");
					Log.e("Error detail",e.toString());
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}

		}

		protected void onProgressUpdate(MessageBean... message) {
			/*
			 * The following code displays what is received in doInBackground().
			 */

			String strReceived = message[0].getText().trim();
			String seqNum = message[0].getSequence().trim();


			/*
			 * Sequencer Specific task 
			 */
			if(seqNum.equals(requestMarker))
			{
				MessageBean mb = message[0];
				mb.setSequence(String.valueOf(GlobalSequence));
				new ClientMulticastTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,mb);
				GlobalSequence++;
			}
			/*
			 * generic server
			 */
			else 
			{

				TextView remoteTextView = (TextView) findViewById(R.id.textView1);
				remoteTextView.append(strReceived + "\t\n");
				if (Integer.parseInt(message[0].getSequence()) == LocalSequence )
				{

					try {

						mContentResolver = getContentResolver();
						mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger.provider");
						ContentValues cv =  new ContentValues();
						cv.put(KEY, seqNum);
						cv.put(VALUE, strReceived);
						mContentResolver.insert(mUri, cv);
						LocalSequence++;

					} catch (Exception e) {
						Log.e("", "File write failed");
					}
				}
				else if(checkinBuffer(message[0]))
				{
					try {

						mContentResolver = getContentResolver();
						mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger.provider");
						ContentValues cv =  new ContentValues();
						cv.put(KEY, seqNum);
						cv.put(VALUE, strReceived);
						mContentResolver.insert(mUri, cv);
						buffer.remove(message[0]);
						LocalSequence++;
					} catch (Exception e) {
						Log.e("", "File write failed");
					}
				}

				else
				{
					buffer.add(message[0]);
				}

			}


		}
		private Uri buildUri(String scheme, String authority) {
			Uri.Builder uriBuilder = new Uri.Builder();
			uriBuilder.authority(authority);
			uriBuilder.scheme(scheme);
			return uriBuilder.build();
		}
		private boolean checkinBuffer(MessageBean message)
		{
			String inSequence = message.getSequence();
			for (MessageBean mb : buffer) {
				if(mb.getSequence().equals(inSequence))
					return true;
			}
			return false;
		}
	}
	private class ClientMulticastTask extends AsyncTask<MessageBean, Void, Void> {


		@Override
		protected Void doInBackground(MessageBean... msgs) {
			try {
				String seqNum = msgs[0].getSequence();
				if(seqNum.equals(requestMarker))
				{
					Socket sequencer = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
							Integer.parseInt(ports[0]));
					ObjectOutputStream outs = new ObjectOutputStream(new BufferedOutputStream(sequencer.getOutputStream()));
					outs.writeObject(msgs[0]);
					outs.flush();
					sequencer.close();
				}
				else {
					multicast(msgs[0]);
				}
			} catch (UnknownHostException e) {
				Log.e(TAG, "ClientTask UnknownHostException");
			} catch (IOException e) {
				Log.e(TAG, "ClientTask socket IOException");
				Log.e("Error Detail",e.toString());
			}

			return null;
		}

		public void multicast(MessageBean mb) throws NumberFormatException, UnknownHostException, IOException
		{
			for (String port : ports) {
				Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
						Integer.parseInt(port));
				ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
				out.writeObject(mb);
				out.flush();
				out.close();
				socket.close();
			}
		}
	}



}
