package edu.yu.cs.com3800.stage4;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

import edu.yu.cs.com3800.*;

// leader class
public class RoundRobinLeader extends Thread implements LoggingServer {

	private InetSocketAddress myAddress;
	private LinkedBlockingQueue<InetSocketAddress> workers;
	private boolean shutdown;
	private Logger logger;
	private ThreadPoolExecutor tpe = (ThreadPoolExecutor) Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

	public RoundRobinLeader(LinkedBlockingQueue<Message> inMessage, LinkedBlockingQueue<Message> outMessage, InetSocketAddress myAddress, LinkedBlockingQueue<InetSocketAddress> queue) {
		this.myAddress = myAddress;
		this.workers = queue;
		this.shutdown = false;
		this.logger = initializeLogging(RoundRobinLeader.class.getCanonicalName() + "-myAddress-" + this.myAddress.getPort());
	}
	
	@Override
	public void run() {
		// Socket socket = new Socket("localhost", this.myAddress.getPort()+2);
		try {
			ServerSocket serverSocket = new ServerSocket(this.myAddress.getPort()+2);

			while (!this.shutdown) {
				Socket socket = serverSocket.accept();
				logger.info("socket connection made...");

				Thread thread = new Thread(() -> {
					try {
						byte[] returnedData = null;
						returnedData = Util.readAllBytesFromNetwork(socket.getInputStream());
						Message message = new Message(returnedData);
						logger.info("message received from GatewayServer: " + message.toString());
						InetSocketAddress workerAddress = workers.poll();
						// if worker is observer (ie GatewayPeerServerImpl), then pull another message and offer again (ie skip observer) - dont send work to observer
						Socket soc;
						try {
							soc = new Socket(workerAddress.getHostName(), workerAddress.getPort()+2);
							workers.add(workerAddress);
						}
						catch (Exception e) {
							workerAddress = workers.poll();
							workers.add(workerAddress);
							soc = new Socket(workerAddress.getHostName(), workerAddress.getPort()+2);
						}
						OutputStream os = soc.getOutputStream(); // sends to worker w tcp
						os.write(message.getNetworkPayload());

						byte[] workerData = Util.readAllBytesFromNetwork(soc.getInputStream()); // data response from worker
						// System.out.println(new String(workerData)); //todo fix this
						OutputStream oStream = socket.getOutputStream(); // sends to RRLeader
						oStream.write(workerData);
						logger.info("message sent to worker");

						soc.close();
					} catch (IOException e) {
						logger.severe("ERROR! contents failed to be processed by leader");
						e.printStackTrace();
					}
				});

				thread.setName("RRL: " + this.myAddress + " thread on connection: " + socket.getPort());
				thread.setDaemon(true);
				tpe.execute(thread);
				logger.info("Thread: " + thread.getId() + " - " + thread.getName() + " executed.");
			}
			serverSocket.close();

		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	void shutdown() {
		this.interrupt();
		this.shutdown = true;
		tpe.shutdown();
		this.logger.info("Leader Server and Threads Shutdown");
	}

}