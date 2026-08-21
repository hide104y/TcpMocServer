package tool;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * TCPサーバーのリスニングソケットを管理し、接続要求を受け付けるサーバースレッドクラスです。
 * <p>
 * 指定されたポートで {@link ServerSocket} をバインドし、クライアントからの接続を受け付けて
 * リクエストキュー（{@link ClsTcpSvrScktChnnl}）へ渡します。
 * </p>
 * 
 * <pre>{@code
 * ClsProperties prop = new ClsProperties();
 * ClsTcpServer server = new ClsTcpServer(prop, "MyTcpServer");
 * server.start();
 * // ...
 * server.terminate();
 * }</pre>
 */
public class ClsTcpServer extends Thread {

	/** キュー満杯時のポーリング待機時間 (ミリ秒) */
	private static final int DEFAULT_POLL_INTERVAL_MS = 50;

	/** クラス名 */
	private final String className = ClsTcpServer.class.getName();
	/** プロパティ設定オブジェクト */
	private volatile ClsProperties prop = null;
	/** ソケットチャネル管理オブジェクト */
	private ClsTcpSvrScktChnnl channel = null;
	/** サーバーソケット */
	private ServerSocket serverSocket = null;
	/** 実行中フラグ */
	private volatile boolean running = false;

	/**
	 * 指定されたプロパティとスレッド名を持つ {@code ClsTcpServer} インスタンスを構築します。
	 * 
	 * <pre>{@code
	 * ClsTcpServer server = new ClsTcpServer(prop, "TcpServerThread");
	 * }</pre>
	 * 
	 * @param prop プロパティ設定オブジェクト
	 * @param name スレッド名
	 */
	public ClsTcpServer(ClsProperties prop, String name) {
		super(name);
		this.prop = prop;
	}

	/**
	 * サーバーが稼働中であるかを取得します。
	 * 
	 * <pre>{@code
	 * boolean active = server.isRunning();
	 * }</pre>
	 * 
	 * @return 稼働中の場合は {@code true}、停止している場合は {@code false}
	 */
	public boolean isRunning() {
		return running;
	}

	/**
	 * サーバーソケットのバインドおよび接続受付ループを実行します。
	 * <p>
	 * 終了フラグが設定されるまで {@link #doWork()} を繰り返し呼び出します。
	 * </p>
	 * 
	 * <pre>{@code
	 * server.start();
	 * }</pre>
	 */
	@Override
	public void run() {
		running = true;
		try {
			int port = prop != null ? prop.getValue(ClsProperties.PORT, ClsProperties.DEFAULT_PORT) : ClsProperties.DEFAULT_PORT;
			serverSocket = new ServerSocket(port);
			System.out.println("[" + className + "] ServerSocket created at " + serverSocket.getLocalSocketAddress());
			System.out.println("[" + className + "] listening on port " + serverSocket.getLocalPort());
			channel = new ClsTcpSvrScktChnnl(prop);
			channel.startWorkers();
			while (prop != null && !prop.getIsTerminate()) {
				doWork();
			}
		} catch (InterruptedException e) {
			if (prop != null) {
				prop.setIsTerminate(true);
			}
			Thread.currentThread().interrupt();
		} catch (IOException e) {
			System.out.println("[" + className + "] IOEXCEPTION : " + e.getMessage());
		} finally {
			terminate();
		}
	}

	/**
	 * クライアント接続を待ち受け、リクエストキューに登録します。
	 * 
	 * <pre>{@code
	 * server.doWork();
	 * }</pre>
	 * 
	 * @throws InterruptedException スレッド待機中に割り込みが発生した場合
	 */
	public void doWork() throws InterruptedException {
		if (Thread.currentThread().isInterrupted()) {
			if (prop != null) {
				prop.setIsTerminate(true);
			}
			throw new InterruptedException();
		}
		if (channel != null && channel.canAccept()) {
			Socket client = listenSocket();
			if (client != null) {
				ClsRequest request = new ClsRequest(prop, client);
				channel.putRequest(request);
			}
		} else {
			if (prop != null) {
				prop.sleep(DEFAULT_POLL_INTERVAL_MS);
			} else {
				Thread.sleep(DEFAULT_POLL_INTERVAL_MS);
			}
		}
	}

	/**
	 * サーバーソケットでクライアントからの接続要求を受け付けます。
	 * 
	 * <pre>{@code
	 * Socket client = server.listenSocket();
	 * }</pre>
	 * 
	 * @return 接続されたクライアントソケット。受付失敗または中断時は {@code null}
	 */
	public Socket listenSocket() {
		Socket client = null;
		try {
			if (serverSocket != null && !serverSocket.isClosed()) {
				client = serverSocket.accept();
			}
		} catch (SocketException e) {
			System.out.println("[" + className + "] SocketException -> serverSocket closed or interrupted");
		} catch (IOException e) {
			System.out.println("[" + className + "] EXCEPTION : " + e.getMessage());
		}
		return client;
	}

	/**
	 * サーバーソケットおよびワーカースレッドを停止・破棄します。
	 * 
	 * <pre>{@code
	 * server.terminate();
	 * }</pre>
	 */
	public void terminate() {
		if (!running) {
			return;
		}
		running = false;
		if (channel != null) {
			System.out.println("[" + className + "] Call channel.stopWorkers()");
			channel.stopWorkers();
		}
		if (serverSocket != null && !serverSocket.isClosed()) {
			try {
				System.out.println("[" + className + "] Call serverSocket.close()");
				serverSocket.close();
			} catch (IOException e) {
				System.out.println("[" + className + "] EXCEPTION : " + e.getMessage());
			}
		}
		System.out.println("[" + className + "] BYE");
	}

}
