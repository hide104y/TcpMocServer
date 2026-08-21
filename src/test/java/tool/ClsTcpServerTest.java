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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * {@link ClsTcpServer} クラスのサーバー起動・停止およびクライアント接続受付の単体テストクラスです。
 */
public class ClsTcpServerTest {

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
	 * 利用可能なローカルポート番号を検索して取得します。
	 *
	 * @return 空きポート番号
	 * @throws IOException ポート取得に失敗した場合
	 */
	private int findAvailablePort() throws IOException {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		}
	}

	/**
	 * サーバーの起動、クライアント接続、レスポンス受信および停止処理をテストします。
	 *
	 * @throws Exception テスト実行中にエラーが発生した場合
	 */
	@Test
	public void testServerStartAndStop() throws Exception {
		int port = findAvailablePort();
		ClsProperties prop = new ClsProperties();
		prop.setValue(ClsProperties.PORT, port);
		prop.setValue(ClsProperties.WORKER_THREADS, 2);

		ClsTcpServer server = new ClsTcpServer(prop, "TestServer");
		server.start();

		// サーバーの起動待ち
		Thread.sleep(500);
		assertTrue(server.isRunning());

		// クライアントからの接続テスト
		try (Socket client = new Socket("127.0.0.1", port);
			 PrintWriter out = new PrintWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8), true);
			 BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8))) {

			out.println("GET / HTTP/1.1");
			out.println("Host: localhost");
			out.println("");

			String line = in.readLine();
			assertNotNull(line);
			assertTrue(line.contains("HTTP/1.1 200 OK"));
		}

		// 終了処理
		prop.setIsTerminate(true);
		server.terminate();
		server.join(3000);

		assertFalse(server.isRunning());
	}

}
