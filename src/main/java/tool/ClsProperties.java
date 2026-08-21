package tool;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * プロパティおよび設定値の保持・管理を行うクラスです。
 * <p>
 * アプリケーション実行時の各種パラメータ（ポート番号、ログレベル、タイムゾーン等）を
 * キー・バリュー形式で保持し、各種プリミティブ型やオブジェクトへの型変換機能を提供します。
 * </p>
 * 
 * <pre>{@code
 * ClsProperties prop = new ClsProperties();
 * prop.setValue(ClsProperties.PORT, 8080);
 * int port = prop.getValue(ClsProperties.PORT, 8000);
 * }</pre>
 */
public class ClsProperties {

	/** デバッグレベル定数 (-1) */
	public static final int LVL_DEBUG = -1;
	/** 情報レベル定数 (0) */
	public static final int LVL_INFO = 0;
	/** 警告レベル定数 (10) */
	public static final int LVL_WARN = 10;
	/** エラーレベル定数 (20) */
	public static final int LVL_ERROR = 20;
	/** 致命的エラーレベル定数 (30) */
	public static final int LVL_FATAL = 30;

	/** デフォルトタイムゾーン ("Asia/Tokyo") */
	public static final String DEFAULT_TIMEZONE = "Asia/Tokyo";
	/** デフォルトホスト名 ("localhost") */
	public static final String DEFAULT_HOSTNAME = "localhost";
	/** デフォルト冗長出力レベル (0) */
	public static final int DEFAULT_VERBOSE = 0;
	/** デフォルトポート番号 (8000) */
	public static final int DEFAULT_PORT = 8000;
	/** デフォルト最大キュー保持数 (100) */
	public static final int DEFAULT_MAX_QUEUE = 100;
	/** デフォルトワーカースレッド名プレフィックス ("worker") */
	public static final String DEFAULT_WORKER = "worker";

	/** ワーカースレッド名キー ("WorkerName") */
	public static final String WORKER_NAME = "WorkerName";
	/** タイムゾーンキー ("TimeZone") */
	public static final String TIMEZONE = "TimeZone";
	/** 冗長出力レベルキー ("Verbose") */
	public static final String VERBOSE = "Verbose";
	/** ポート番号キー ("Port") */
	public static final String PORT = "Port";
	/** 開始日時ミリ秒キー ("StartTimeMiliSec") */
	public static final String START_TIME_MS = "StartTimeMiliSec";
	/** トレースログ出力キー ("IsTraceLog") */
	public static final String IS_TRACE_LOG = "IsTraceLog";
	/** キー未存在時警告出力キー ("IsWarnIfKeyNotFound") */
	public static final String WARN_KEY_MISS = "IsWarnIfKeyNotFound";
	/** ホスト名キー ("HostName") */
	public static final String HOSTNAME = "HostName";
	/** ワーカースレッド数キー ("WorkerThreads") */
	public static final String WORKER_THREADS = "WorkerThreads";

	/** スリープ刻み時間の最大値 (ミリ秒) */
	private static final int MAX_SLEEP_STEP_MS = 500;
	/** 外部プロセスの最大待機時間 (秒) */
	private static final int PROCESS_TIMEOUT_SEC = 3;

	/** クラス名 */
	private final String className = ClsProperties.class.getName();
	/** プロパティマップ（スレッドセーフ） */
	private final Map<String, String> propMap = Collections.synchronizedMap(new LinkedHashMap<>());
	/** キー未存在時の警告フラグ */
	private boolean warnKeyMiss = false;
	/** 終了フラグ */
	private volatile boolean isTerminated = false;

	/**
	 * 新しい {@code ClsProperties} インスタンスを構築します。
	 * 
	 * <pre>{@code
	 * ClsProperties prop = new ClsProperties();
	 * }</pre>
	 */
	public ClsProperties() {
	}

	/**
	 * アプリケーションの終了フラグを設定します。
	 * 
	 * <pre>{@code
	 * prop.setIsTerminate(true);
	 * }</pre>
	 * 
	 * @param terminate 終了する場合は {@code true}、それ以外は {@code false}
	 */
	public void setIsTerminate(boolean terminate) {
		this.isTerminated = terminate;
	}

	/**
	 * アプリケーションが終了状態であるかを取得します。
	 * 
	 * <pre>{@code
	 * if (prop.getIsTerminate()) {
	 *     // 終了処理
	 * }
	 * }</pre>
	 * 
	 * @return 終了状態の場合は {@code true}、実行中の場合は {@code false}
	 */
	public boolean getIsTerminate() {
		return isTerminated;
	}

	/**
	 * 指定されたキーに対応する文字列プロパティ値を取得します。
	 * 
	 * <pre>{@code
	 * String host = prop.getValue(ClsProperties.HOSTNAME, "localhost");
	 * }</pre>
	 * 
	 * @param key プロパティキー
	 * @param defaultValue キーが存在しない場合のデフォルト値
	 * @return プロパティ値。キーが存在しない場合はデフォルト値
	 */
	public String getValue(String key, String defaultValue) {
		String val = defaultValue != null ? defaultValue : "";
		if (key != null && !key.isEmpty()) {
			if (propMap.containsKey(key)) {
				val = propMap.get(key);
			} else if (warnKeyMiss) {
				System.out.println("[" + className + "] ★★★ NOT FOUND KEY ★★★ : " + key);
			}
		}
		if ("null".equalsIgnoreCase(val)) {
			val = null;
		}
		return val;
	}

	/**
	 * 指定されたキーに対応する真偽値プロパティ値を取得します。
	 * 
	 * <pre>{@code
	 * boolean isTrace = prop.getValue(ClsProperties.IS_TRACE_LOG, false);
	 * }</pre>
	 * 
	 * @param key プロパティキー
	 * @param defaultValue キーが存在しない場合または解析失敗時のデフォルト値
	 * @return 真偽値
	 */
	public boolean getValue(String key, boolean defaultValue) {
		String strVal = getValue(key, String.valueOf(defaultValue));
		if ("true".equalsIgnoreCase(strVal)) {
			return true;
		} else if ("false".equalsIgnoreCase(strVal)) {
			return false;
		}
		return defaultValue;
	}

	/**
	 * 指定されたキーに対応する整数プロパティ値を取得します。
	 * 
	 * <pre>{@code
	 * int port = prop.getValue(ClsProperties.PORT, 8000);
	 * }</pre>
	 * 
	 * @param key プロパティキー
	 * @param defaultValue キーが存在しない場合または解析失敗時のデフォルト値
	 * @return 整数値
	 */
	public int getValue(String key, int defaultValue) {
		String strVal = getValue(key, String.valueOf(defaultValue));
		try {
			return Integer.parseInt(strVal);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	/**
	 * 指定されたキーに対応する長整数プロパティ値を取得します。
	 * 
	 * <pre>{@code
	 * long startTime = prop.getValue(ClsProperties.START_TIME_MS, 0L);
	 * }</pre>
	 * 
	 * @param key プロパティキー
	 * @param defaultValue キーが存在しない場合または解析失敗時のデフォルト値
	 * @return 長整数値
	 */
	public long getValue(String key, long defaultValue) {
		String strVal = getValue(key, String.valueOf(defaultValue));
		try {
			return Long.parseLong(strVal);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	/**
	 * 指定されたキーに対応する倍精度浮動小数点プロパティ値を取得します。
	 * 
	 * <pre>{@code
	 * double rate = prop.getValue("Rate", 1.0);
	 * }</pre>
	 * 
	 * @param key プロパティキー
	 * @param defaultValue キーが存在しない場合または解析失敗時のデフォルト値
	 * @return 倍精度浮動小数点数値
	 */
	public double getValue(String key, double defaultValue) {
		String strVal = getValue(key, String.valueOf(defaultValue));
		try {
			return Double.parseDouble(strVal);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	/**
	 * 指定されたキーに対応する {@link Double} オブジェクトプロパティ値を取得します。
	 * 
	 * <pre>{@code
	 * Double rate = prop.getValue("Rate", (Double) null);
	 * }</pre>
	 * 
	 * @param key プロパティキー
	 * @param defaultValue キーが存在しない場合のデフォルトオブジェクト
	 * @return {@link Double} 値
	 */
	public Double getValue(String key, Double defaultValue) {
		if (defaultValue == null) {
			String strVal = getValue(key, "");
			if (strVal == null || strVal.isEmpty()) {
				return null;
			}
			try {
				return Double.parseDouble(strVal);
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return getValue(key, defaultValue.doubleValue());
	}

	/**
	 * 指定されたキーに対応する文字セット ({@link Charset}) プロパティ値を取得します。
	 * 
	 * <pre>{@code
	 * Charset cs = prop.getValue("Encoding", StandardCharsets.UTF_8);
	 * }</pre>
	 * 
	 * @param key プロパティキー
	 * @param defaultValue キーが存在しない場合または不正な文字セット名時のデフォルト値
	 * @return {@link Charset} オブジェクト
	 */
	public Charset getValue(String key, Charset defaultValue) {
		String strVal = getValue(key, "");
		if (strVal != null && !strVal.isEmpty()) {
			try {
				return Charset.forName(strVal);
			} catch (Exception e) {
				return defaultValue;
			}
		}
		return defaultValue;
	}

	/**
	 * 真偽値のプロパティ値を設定します。
	 * 
	 * <pre>{@code
	 * prop.setValue("IsDebug", true);
	 * }</pre>
	 * 
	 * @param key プロパティキー
	 * @param value 設定する真偽値
	 */
	public void setValue(String key, boolean value) {
		propMap.put(key, value ? "true" : "false");
	}

	/**
	 * 整数値のプロパティ値を設定します。
	 * 
	 * <pre>{@code
	 * prop.setValue(ClsProperties.PORT, 8080);
	 * }</pre>
	 * 
	 * @param key プロパティキー
	 * @param value 設定する整数値
	 */
	public void setValue(String key, int value) {
		propMap.put(key, String.valueOf(value));
	}

	/**
	 * 長整数値のプロパティ値を設定します。
	 * 
	 * <pre>{@code
	 * prop.setValue("TimeoutMs", 5000L);
	 * }</pre>
	 * 
	 * @param key プロパティキー
	 * @param value 設定する長整数値
	 */
	public void setValue(String key, long value) {
		propMap.put(key, String.valueOf(value));
	}

	/**
	 * 文字列のプロパティ値を設定します。値が null の場合は該当キーを削除します。
	 * 
	 * <pre>{@code
	 * prop.setValue(ClsProperties.HOSTNAME, "127.0.0.1");
	 * }</pre>
	 * 
	 * @param key プロパティキー
	 * @param value 設定する文字列（null の場合は削除）
	 */
	public void setValue(String key, String value) {
		if (value != null) {
			propMap.put(key, value);
		} else {
			propMap.remove(key);
		}
	}

	/**
	 * 保持している全プロパティキーおよび値を標準出力に一覧表示します。
	 * 
	 * <pre>{@code
	 * prop.list();
	 * }</pre>
	 */
	public void list() {
		synchronized (propMap) {
			for (Map.Entry<String, String> entry : propMap.entrySet()) {
				System.out.println("# " + entry.getKey() + " = " + entry.getValue());
			}
		}
	}

	/**
	 * UNIXエポックミリ秒を日本標準時 (JST) の指定書式文字列へ変換します。
	 * 
	 * <pre>{@code
	 * String jst = prop.formatJstTime(System.currentTimeMillis(), "yyyy/MM/dd HH:mm:ss");
	 * }</pre>
	 * 
	 * @param millis UNIXエポックからのミリ秒
	 * @param format 日時フォーマットパターン（例: "yyyy/MM/dd HH:mm:ss"）
	 * @return フォーマット済み日時文字列
	 */
	public String formatJstTime(long millis, String format) {
		Date dt = new Date(millis);
		String tzName = getValue(ClsProperties.TIMEZONE, ClsProperties.DEFAULT_TIMEZONE);
		TimeZone tz = TimeZone.getTimeZone(tzName);
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		sdf.setTimeZone(tz);
		return sdf.format(dt);
	}

	/**
	 * UNIX秒（エポック秒）を日本標準時 (JST) の指定書式文字列へ変換します。
	 * 
	 * <pre>{@code
	 * String jst = prop.formatJstTime(1609459200, "yyyy/MM/dd HH:mm:ss");
	 * }</pre>
	 * 
	 * @param unixTime UNIXエポックからの秒数
	 * @param format 日時フォーマットパターン
	 * @return フォーマット済み日時文字列
	 */
	public String formatJstTime(int unixTime, String format) {
		long millis = unixTime * 1000L;
		return formatJstTime(millis, format);
	}

	/**
	 * 文字列の両端の空白を除去し、空文字列の場合は null を返却します。
	 * 
	 * <pre>{@code
	 * String trimmed = prop.trim("  test  "); // "test"
	 * String nullStr = prop.trim("   ");      // null
	 * }</pre>
	 * 
	 * @param str トリム対象の文字列
	 * @return トリム後の文字列。入力が null または空文字列の場合は null
	 */
	public String trim(String str) {
		if (str != null) {
			String trimmed = str.trim();
			return trimmed.isEmpty() ? null : trimmed;
		}
		return null;
	}

	/**
	 * 指定された文字列が整数値として解釈可能かどうかを判定します。
	 * 
	 * <pre>{@code
	 * boolean valid = prop.isNumber("12345"); // true
	 * boolean invalid = prop.isNumber("abc"); // false
	 * }</pre>
	 * 
	 * @param str 判定対象の文字列
	 * @return 整数値として解析可能な場合は {@code true}、それ以外は {@code false}
	 */
	public boolean isNumber(String str) {
		if (str == null || str.isEmpty()) {
			return false;
		}
		try {
			Integer.parseInt(str);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	/**
	 * 指定ミリ秒間スリープします。終了フラグが立った場合は早期に中断します。
	 * 
	 * <pre>{@code
	 * prop.sleep(1000);
	 * }</pre>
	 * 
	 * @param sleepMillis スリープ時間（ミリ秒）
	 */
	public void sleep(int sleepMillis) {
		if (sleepMillis <= 0) {
			return;
		}
		int loopMillis = sleepMillis;
		int maxLoops = 1;
		if (sleepMillis > MAX_SLEEP_STEP_MS) {
			loopMillis = MAX_SLEEP_STEP_MS;
			maxLoops = sleepMillis / MAX_SLEEP_STEP_MS;
		}
		for (int i = 0; i < maxLoops; i++) {
			if (isTerminated) {
				break;
			}
			try {
				Thread.sleep(loopMillis);
			} catch (InterruptedException e) {
				isTerminated = true;
				Thread.currentThread().interrupt();
				break;
			}
		}
	}

	/**
	 * 実行環境のローカルホスト名を取得します。
	 * 
	 * <pre>{@code
	 * String host = prop.getHostName();
	 * }</pre>
	 * 
	 * @return ホスト名文字列
	 */
	public String getHostName() {
		try {
			String host = InetAddress.getLocalHost().getHostName();
			if (host != null && !host.isEmpty()) {
				return host.trim().split("[\\s\\.]")[0];
			}
		} catch (Exception e) {
			// ignore: ホスト名取得失敗時は環境変数やコマンドから取得を試みる
		}
		String envHost = System.getenv("COMPUTERNAME");
		if (envHost == null || envHost.isEmpty()) {
			envHost = System.getenv("HOSTNAME");
		}
		if (envHost != null && !envHost.isEmpty()) {
			return envHost.trim().split("[\\s\\.]")[0];
		}
		String cmdHost = execRead("hostname");
		if (cmdHost != null && !cmdHost.isEmpty()) {
			return cmdHost.trim().split("[\\s\\.]")[0];
		}
		return DEFAULT_HOSTNAME;
	}

	/**
	 * 指定された外部コマンドを実行し、標準出力を文字列として読み込みます。
	 * 
	 * <pre>{@code
	 * String out = prop.execRead("hostname");
	 * }</pre>
	 * 
	 * @param cmd 実行する外部コマンド文字列
	 * @return 外部コマンドの標準出力結果（トリム済み）
	 */
	public String execRead(String cmd) {
		String result = "";
		Process process = null;
		try {
			process = new ProcessBuilder(cmd.split("\\s+")).start();
			try (Scanner s = new Scanner(process.getInputStream(), StandardCharsets.UTF_8.name()).useDelimiter("\\A")) {
				result = s.hasNext() ? s.next() : "";
			}
			process.waitFor(PROCESS_TIMEOUT_SEC, TimeUnit.SECONDS);
		} catch (IOException | InterruptedException e) {
			// ignore: コマンド実行失敗時は空文字を返却
		} finally {
			if (process != null) {
				process.destroy();
			}
		}
		return result.trim();
	}

}
