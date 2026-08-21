package tool;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * {@link ClsProperties} クラスのプロパティ管理およびユーティリティ機能の単体テストクラスです。
 */
public class ClsPropertiesTest {

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
	 * 文字列プロパティの取得・設定および削除動作をテストします。
	 */
	@Test
	public void testStringValue() {
		ClsProperties prop = new ClsProperties();
		assertEquals("default", prop.getValue("unknownKey", "default"));

		prop.setValue("myKey", "myValue");
		assertEquals("myValue", prop.getValue("myKey", "default"));

		prop.setValue("nullKey", "null");
		assertNull(prop.getValue("nullKey", "default"));

		prop.setValue("myKey", (String) null);
		assertEquals("default", prop.getValue("myKey", "default"));
	}

	/**
	 * 真偽値プロパティの取得および設定動作をテストします。
	 */
	@Test
	public void testBooleanValue() {
		ClsProperties prop = new ClsProperties();
		assertTrue(prop.getValue("unknownBool", true));
		assertFalse(prop.getValue("unknownBool", false));

		prop.setValue("flagTrue", true);
		assertTrue(prop.getValue("flagTrue", false));

		prop.setValue("flagFalse", false);
		assertFalse(prop.getValue("flagFalse", true));
	}

	/**
	 * 整数プロパティの取得、設定および不正値のフォールバックをテストします。
	 */
	@Test
	public void testIntValue() {
		ClsProperties prop = new ClsProperties();
		assertEquals(8000, prop.getValue(ClsProperties.PORT, ClsProperties.DEFAULT_PORT));

		prop.setValue(ClsProperties.PORT, 9090);
		assertEquals(9090, prop.getValue(ClsProperties.PORT, ClsProperties.DEFAULT_PORT));

		prop.setValue("invalidInt", "abc");
		assertEquals(1234, prop.getValue("invalidInt", 1234));
	}

	/**
	 * 長整数プロパティの取得、設定および不正値のフォールバックをテストします。
	 */
	@Test
	public void testLongValue() {
		ClsProperties prop = new ClsProperties();
		assertEquals(100L, prop.getValue("longKey", 100L));

		prop.setValue("longKey", 9999999999L);
		assertEquals(9999999999L, prop.getValue("longKey", 0L));

		prop.setValue("invalidLong", "xyz");
		assertEquals(50L, prop.getValue("invalidLong", 50L));
	}

	/**
	 * 倍精度浮動小数点プロパティの取得およびオブジェクト型のフォールバックをテストします。
	 */
	@Test
	public void testDoubleValue() {
		ClsProperties prop = new ClsProperties();
		assertEquals(3.14, prop.getValue("doubleKey", 3.14), 0.001);

		prop.setValue("doubleKey", "2.718");
		assertEquals(2.718, prop.getValue("doubleKey", 0.0), 0.001);

		Double nullDefault = null;
		assertNull(prop.getValue("nonExistent", nullDefault));
		assertEquals(Double.valueOf(1.5), prop.getValue("nonExistent", Double.valueOf(1.5)));
	}

	/**
	 * 文字セットプロパティの取得、設定および不正名称時のフォールバックをテストします。
	 */
	@Test
	public void testCharsetValue() {
		ClsProperties prop = new ClsProperties();
		assertEquals(StandardCharsets.UTF_8, prop.getValue("charsetKey", StandardCharsets.UTF_8));

		prop.setValue("charsetKey", "ISO-8859-1");
		assertEquals(Charset.forName("ISO-8859-1"), prop.getValue("charsetKey", StandardCharsets.UTF_8));

		prop.setValue("invalidCharset", "invalid_charset_name_123");
		assertEquals(StandardCharsets.UTF_8, prop.getValue("invalidCharset", StandardCharsets.UTF_8));
	}

	/**
	 * JST日時フォーマットユーティリティの動作をテストします。
	 */
	@Test
	public void testFormatJstTime() {
		ClsProperties prop = new ClsProperties();
		// 0 ms -> UTC: 1970/01/01 00:00:00, JST(+9): 1970/01/01 09:00:00
		String formatted = prop.formatJstTime(0L, "yyyy/MM/dd HH:mm:ss");
		assertEquals("1970/01/01 09:00:00", formatted);

		String formattedInt = prop.formatJstTime(0, "yyyy/MM/dd HH:mm:ss");
		assertEquals("1970/01/01 09:00:00", formattedInt);
	}

	/**
	 * 文字列トリムユーティリティの動作をテストします。
	 */
	@Test
	public void testTrim() {
		ClsProperties prop = new ClsProperties();
		assertNull(prop.trim(null));
		assertNull(prop.trim("   "));
		assertEquals("hello", prop.trim("  hello  "));
	}

	/**
	 * 数値文字列判定ユーティリティの動作をテストします。
	 */
	@Test
	public void testIsNumber() {
		ClsProperties prop = new ClsProperties();
		assertTrue(prop.isNumber("12345"));
		assertTrue(prop.isNumber("-50"));
		assertFalse(prop.isNumber("abc"));
		assertFalse(prop.isNumber(null));
		assertFalse(prop.isNumber(""));
	}

	/**
	 * 終了フラグ設定およびスリープの中断動作をテストします。
	 */
	@Test
	public void testTerminateAndSleep() {
		ClsProperties prop = new ClsProperties();
		assertFalse(prop.getIsTerminate());

		prop.setIsTerminate(true);
		assertTrue(prop.getIsTerminate());

		// sleep should exit immediately if terminated
		long start = System.currentTimeMillis();
		prop.sleep(1000);
		long elapsed = System.currentTimeMillis() - start;
		assertTrue(elapsed < 300);
	}

	/**
	 * ホスト名取得ユーティリティの動作をテストします。
	 */
	@Test
	public void testGetHostName() {
		ClsProperties prop = new ClsProperties();
		String host = prop.getHostName();
		assertNotNull(host);
		assertFalse(host.isEmpty());
	}

	/**
	 * プロパティ一覧出力メソッドの呼び出しをテストします。
	 */
	@Test
	public void testList() {
		ClsProperties prop = new ClsProperties();
		prop.setValue("k1", "v1");
		prop.setValue("k2", "v2");
		prop.list(); // should not throw exception
	}

}
