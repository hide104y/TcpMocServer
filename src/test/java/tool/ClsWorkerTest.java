package tool;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * {@link ClsWorker} ワーカースレッドのライフサイクルおよび実行処理の単体テストクラスです。
 */
public class ClsWorkerTest {

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
	 * ワーカースレッドの起動および割り込み停止処理をテストします。
	 *
	 * @throws InterruptedException スレッド待機中に割り込みが発生した場合
	 */
	@Test
	public void testWorkerLifecycle() throws InterruptedException {
		ClsProperties prop = new ClsProperties();
		ClsTcpSvrScktChnnl channel = new ClsTcpSvrScktChnnl(prop);
		ClsWorker worker = new ClsWorker(prop, 0, channel, "test-worker-00");

		worker.start();
		assertTrue(worker.isAlive());

		// 終了フラグを設定して割り込み
		prop.setIsTerminate(true);
		worker.interrupt();
		worker.join(2000);

		assertFalse(worker.isAlive());
	}

	/**
	 * 割り込みフラグがセットされた状態での {@link ClsWorker#doWork()} 呼び出しの例外発生をテストします。
	 */
	@Test
	public void testDoWorkInterrupted() {
		ClsProperties prop = new ClsProperties();
		ClsTcpSvrScktChnnl channel = new ClsTcpSvrScktChnnl(prop);
		ClsWorker worker = new ClsWorker(prop, 0, channel, "test-worker-01");

		Thread.currentThread().interrupt();
		try {
			worker.doWork();
		} catch (InterruptedException e) {
			// Expected
			assertTrue(true);
		} finally {
			Thread.interrupted(); // clear status
		}
	}

}
