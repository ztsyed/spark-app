package org.apache.spark.examples.streaming;


import java.io.IOException;
import java.util.Properties;
import org.apache.spark.streaming.receiver.Receiver;
import org.apache.spark.storage.StorageLevel;
import org.nats.*;

public class NatsClient extends Receiver<String> {
	static Connection conn =null;
	String natsURL = null;
	public NatsClient(String url) {
		super(StorageLevel.MEMORY_AND_DISK_2());
		natsURL=url;
		System.out.println("NatsClient Initialized for "+natsURL);
	}
	public void onStart() {
    // Start the thread that receives data over a connection
    new Thread()  {
      @Override public void run() {
        Connect(natsURL);
        Subscribe("foo");
      }
    }.start();
  }

  public void onStop() {
    // There is nothing much to do as the thread calling receive()
    // is designed to stop by itself isStopped() returns false
    Close();
  }

	public void Connect(String natsURL) {
		try {
			Properties opts = new Properties();
			opts.put("servers", natsURL);
			conn= Connection.connect(opts);
			System.out.println("Connected >> " + conn.isConnected());
			// conn.close();
		} catch (Exception e) {
			System.out.println("Error ");
		}
	}

	public void Close() {
		if(conn!=null && conn.isConnected()){
			try {
				conn.flush();
				conn.close(true);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void Subscribe(String chan_name) {
		// Simple Subscriber
		if(conn!=null && conn.isConnected())
			try {
				conn.subscribe(chan_name, new MsgHandler() {
					public void execute(String msg) {
						System.out.println("Received a message: " + msg);
						if(!isStopped()){
							store(msg);
						}
					}
				});
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

	public void Publish(String chan_name, final String msg) {
		System.out.println("Publish called:"+msg + " " +conn.isConnected());
		if (conn!=null && conn.isConnected()) {
			try {
				conn.publish(chan_name, msg, new MsgHandler() {
					public void execute() {
						System.out.println("Message Sent!["+msg+"]");
					}
				});
			} catch (Exception e) {
			}
		}else{
			System.out.println("No Connection");
		}
	}
}