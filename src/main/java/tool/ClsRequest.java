package tool;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

/**
 * クライアントからのTCPソケット接続に対するリクエスト受信およびHTTP応答処理を行うクラスです。
 * <p>
 * クライアントソケットからHTTPリクエストヘッダーを行単位で読み込み、
 * 接続情報（クライアントIP/ポート、サーバーホスト名/ポート）を含むHTTP/1.1 200 OKレスポンスを返却します。
 * </p>
 * 
 * <pre>{@code
 * ClsRequest request = new ClsRequest(prop, clientSocket);
 * request.execute(1);
 * }</pre>
 */
public class ClsRequest {

	/** デフォルトのアイドルタイムアウト (60000ミリ秒) */
	private static final int DEFAULT_IDLE_TIMEOUT_MS = 60000;
	/** アイドルタイムアウト設定キー */
	private static final String KEY_IDLE_TIMEOUT = "IdleTimeout";

	/** クラス名 */
	private final String className = ClsRequest.class.getName();
	/** プロパティ設定オブジェクト */
	private volatile ClsProperties prop = null;
	/** クライアントソケット接続 */
	private final Socket clientConn;

	/**
	 * 新しい {@code ClsRequest} インスタンスを構築します。
	 * 
	 * <pre>{@code
	 * ClsRequest request = new ClsRequest(prop, clientSocket);
	 * }</pre>
	 * 
	 * @param prop プロパティ設定オブジェクト
	 * @param clientConn クライアントとの通信用ソケット
	 */
	public ClsRequest(ClsProperties prop, Socket clientConn) {
		this.prop = prop;
		this.clientConn = clientConn;
	}

	/**
	 * クライアントからのリクエストを受信・処理し、HTTPレスポンスを返却します。
	 * <p>
	 * 通信完了後または例外発生時に、ソケットおよびストリームリソースは自動的にクローズされます。
	 * </p>
	 * 
	 * <pre>{@code
	 * request.execute(0);
	 * }</pre>
	 * 
	 * @param threadNo 処理を実行しているワーカースレッド番号
	 */
	public void execute(int threadNo) {
		if (clientConn == null) {
			return;
		}
		String clientIp = "";
		int clientPort = 0;
		try (Socket socket = this.clientConn;
			 BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
			 BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

			if (socket.getInetAddress() != null) {
				clientIp = socket.getInetAddress().getHostAddress();
			}
			clientPort = socket.getPort();
			System.out.println("[" + className + "] Connect from " + clientIp + ":" + clientPort);

			int timeout = prop != null ? prop.getValue(KEY_IDLE_TIMEOUT, DEFAULT_IDLE_TIMEOUT_MS) : DEFAULT_IDLE_TIMEOUT_MS;
			socket.setSoTimeout(timeout);

			String input = br.readLine();
			while (input != null && !input.isEmpty()) {
				System.out.println("[" + className + "] REPLY " + threadNo + "> " + input);
				input = br.readLine();
			}

			String host = prop != null ? prop.getValue(ClsProperties.HOSTNAME, ClsProperties.DEFAULT_HOSTNAME) : ClsProperties.DEFAULT_HOSTNAME;
			String port = prop != null ? String.valueOf(prop.getValue(ClsProperties.PORT, ClsProperties.DEFAULT_PORT)) : String.valueOf(ClsProperties.DEFAULT_PORT);

			String body = "<H1>OK : CONNECTED : " + clientIp + ":" + clientPort + " -> " + host + ":" + port + "</H1>";
			byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);

			bw.write("HTTP/1.1 200 OK\r\n");
			bw.write("Content-Type: text/html; charset=UTF-8\r\n");
			bw.write("Content-Length: " + bodyBytes.length + "\r\n");
			bw.write("Connection: close\r\n");
			bw.write("\r\n");
			bw.write(body);
			bw.flush();

		} catch (SocketTimeoutException e) {
			System.out.println("[" + className + "] SocketTimeoutException : " + e.getMessage());
		} catch (IOException e) {
			System.out.println("[" + className + "] REQUEST EXCEPTION : " + e.getMessage());
		} finally {
			System.out.println("[" + className + "] Disconnect from " + clientIp + ":" + clientPort);
		}
	}

}
