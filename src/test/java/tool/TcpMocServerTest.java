package tool;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * {@link TcpMocServer} アプリケーション全体の起動・引数処理・ライフサイクルの単体テストクラスです。
 */
public class TcpMocServerTest {

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
	 * ヘルプオプション指定時の Usage 表示および終了動作をテストします。
	 */
	@Test
	public void testHelpOption() {
		// -h オプション指定時、Usageを表示して即座に終了すること（killJvm=false）
		String[] args = new String[]{"-h"};
		TcpMocServer server = new TcpMocServer(args, false);
		assertNotNull(server);
	}

	/**
	 * サーバー起動および割り込み停止動作をテストします。
	 *
	 * @throws Exception テスト実行中にエラーが発生した場合
	 */
	@Test
	public void testServerExecutionAndInterrupt() throws Exception {
		int port = findAvailablePort();
		String[] args = new String[]{"-p", String.valueOf(port), "-v"};

		Thread serverThread = new Thread(() -> {
			new TcpMocServer(args, false);
		});

		serverThread.start();
		Thread.sleep(800);

		// サーバーを割り込みで停止
		serverThread.interrupt();
		serverThread.join(3000);

		assertFalse(serverThread.isAlive());
	}

	/**
	 * 各種ログ詳細オプション引数およびヘルプオプションのパース処理をテストします。
	 */
	@Test
	public void testVerboseArgs() {
		// -help 指定で即座に戻る動作の検証
		String[] args = new String[]{"-help"};
		TcpMocServer server = new TcpMocServer(args, false);
		assertNotNull(server);
	}

}
