package tool;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * {@link ClsTcpSvrScktChnnl} クラスのリクエストキュー同期およびワーカー制御の単体テストクラスです。
 */
public class ClsTcpSvrScktChnnlTest {

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
	 * リクエストの追加と取り出しによるキューの件数および順序整合性をテストします。
	 */
	@Test
	public void testChannelQueue() {
		ClsProperties prop = new ClsProperties();
		prop.setValue(ClsProperties.WORKER_THREADS, 2);
		ClsTcpSvrScktChnnl channel = new ClsTcpSvrScktChnnl(prop);

		assertEquals(0, channel.getCount());
		assertEquals(ClsProperties.DEFAULT_MAX_QUEUE, channel.getMaxRequests());
		assertTrue(channel.canAccept());

		ClsRequest req1 = new ClsRequest(prop, null);
		ClsRequest req2 = new ClsRequest(prop, null);

		channel.putRequest(req1);
		assertEquals(1, channel.getCount());

		channel.putRequest(req2);
		assertEquals(2, channel.getCount());

		ClsRequest taken1 = channel.takeRequest();
		assertSame(req1, taken1);
		assertEquals(1, channel.getCount());

		ClsRequest taken2 = channel.takeRequest();
		assertSame(req2, taken2);
		assertEquals(0, channel.getCount());
	}

	/**
	 * ワーカースレッドの開始および停止処理をテストします。
	 */
	@Test
	public void testStartAndStopWorkers() {
		ClsProperties prop = new ClsProperties();
		prop.setValue(ClsProperties.WORKER_THREADS, 2);
		ClsTcpSvrScktChnnl channel = new ClsTcpSvrScktChnnl(prop);

		channel.startWorkers();
		prop.setIsTerminate(true);
		channel.stopWorkers();
	}

}
