package tool;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/**
 * {@link ClsRequest} クライアントリクエスト処理の単体テストクラスです。
 */
public class ClsRequestTest {

	/** テスト用作業ディレクトリパス */
	private final Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "UnitTest", "TcpMocServer", this.getClass().getSimpleName());

	/**
	 * 各テスト実行前の初期化処理を行います。
	 *
	 * @throws IOException ディレクトリ作成に失敗した場合
	 */
	@Before
	public void setUp() throws IOException {
		if (Files.exists(tempDir)) {
			deleteDir(tempDir.toFile());
		}
		Files.createDirectories(tempDir);
	}

	/**
	 * 各テスト実行後のクリーンアップ処理を行います。
	 */
	@After
	public void tearDown() {
		if (Files.exists(tempDir)) {
			deleteDir(tempDir.toFile());
		}
	}

	/**
	 * 指定されたディレクトリおよび配下のファイルを再帰的に削除します。
	 *
	 * @param dir 削除対象ディレクトリ
	 */
	private void deleteDir(File dir) {
		File[] files = dir.listFiles();
		if (files != null) {
			for (File f : files) {
				if (f.isDirectory()) {
					deleteDir(f);
				} else {
					f.delete();
				}
			}
		}
		dir.delete();
	}

	/**
	 * ソケット接続を介したHTTPリクエスト受信およびレスポンス生成をテストします。
	 *
	 * @throws Exception テスト実行中にエラーが発生した場合
	 */
	@Test
	public void testRequestExecution() throws Exception {
		ClsProperties prop = new ClsProperties();
		prop.setValue("IdleTimeout", 5000);

		try (ServerSocket serverSocket = new ServerSocket(0)) {
			int port = serverSocket.getLocalPort();
			final StringBuilder responseBuilder = new StringBuilder();

			// サーバー側で接続を受け付けて ClsRequest を実行するスレッド
			Thread serverThread = new Thread(() -> {
				try {
					Socket clientConn = serverSocket.accept();
					ClsRequest request = new ClsRequest(prop, clientConn);
					request.execute(1);
				} catch (IOException e) {
					// ignore: テスト中の切断等
				}
			});
			serverThread.start();

			// クライアント側からリクエストを送信してレスポンスを受信
			try (Socket client = new Socket("127.0.0.1", port);
				 PrintWriter out = new PrintWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8), true);
				 BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8))) {

				out.println("GET / HTTP/1.1");
				out.println("Host: localhost");
				out.println(""); // 空行でリクエストヘッダー終了

				String line;
				while ((line = in.readLine()) != null) {
					responseBuilder.append(line).append("\n");
				}
			}

			serverThread.join(3000);

			String response = responseBuilder.toString();
			assertTrue(response.contains("HTTP/1.1 200 OK"));
			assertTrue(response.contains("Content-Type: text/html; charset=UTF-8"));
			assertTrue(response.contains("OK : CONNECTED"));
		}
	}

	/**
	 * nullソケットを渡した場合に安全に復帰することをテストします。
	 */
	@Test
	public void testNullSocket() {
		ClsProperties prop = new ClsProperties();
		ClsRequest request = new ClsRequest(prop, null);
		// nullソケットでも例外なく処理が終了すること
		request.execute(0);
	}

}
