package tool;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * モックTCPサーバーアプリケーションのエントリポイントおよびライフサイクル管理クラスです。
 * <p>
 * コマンドライン引数を解析してポート番号やログ詳細レベルを設定し、
 * TCPサーバー（{@link ClsTcpServer}）の起動・停止およびシャットダウンフックの登録を行います。
 * </p>
 * 
 * <pre>{@code
 * // コマンドラインからの起動
 * java -jar TcpMocServer.jar -p 8080 -v
 * 
 * // Javaコードからの呼び出し例
 * TcpMocServer.main(new String[]{"-p", "8080"});
 * }</pre>
 */
public class TcpMocServer {

	/** シャットダウンスリープ時間 (500ミリ秒) */
	private static final int SHUTDOWN_SLEEP_MS = 500;
	/** メインポーリングスリープ時間 (1000ミリ秒) */
	private static final int MAIN_POLL_SLEEP_MS = 1000;

	/**
	 * アプリケーションのメインエントリポイントです。
	 * 
	 * <pre>{@code
	 * TcpMocServer.main(new String[]{"-p", "8000", "-v"});
	 * }</pre>
	 * 
	 * @param args コマンドライン引数配列
	 */
	public static void main(String[] args) {
		new TcpMocServer(args, true);
	}

	/** クラス名 */
	private final String className = TcpMocServer.class.getName();
	/** プロパティ設定オブジェクト */
	private ClsProperties prop = null;
	/** TCPサーバーインスタンス */
	private ClsTcpServer tcpServer = null;
	/** シャットダウンフックスレッド */
	private Thread shutdownHook = null;
	/** 起動時刻（エポックミリ秒） */
	private final long startTimeMs = System.currentTimeMillis();
	/** 終了時刻（エポックミリ秒） */
	private long endTimeMs = 0;
	/** JVM強制終了フラグ */
	private boolean killJvm = false;
	/** キャンセルフラグ */
	private boolean isCancel = false;
	/** シャットダウンフック実行中フラグ */
	private boolean runningHook = false;
	/** コマンドライン引数マップ */
	private final Map<String, String> argsMap = new LinkedHashMap<>();

	/**
	 * 指定されたコマンドライン引数および終了制御フラグで {@code TcpMocServer} を構築・実行します。
	 * 
	 * <pre>{@code
	 * TcpMocServer server = new TcpMocServer(args, false);
	 * }</pre>
	 * 
	 * @param args コマンドライン引数配列
	 * @param killJvm 終了時にJVMごと強制停止（{@link System#exit(int)} または {@link Runtime#halt(int)}）するかどうか
	 */
	public TcpMocServer(String[] args, boolean killJvm) {
		exec(args, killJvm);
	}

	/**
	 * コマンドライン引数を解析し、TCPサーバーを起動してメインループに入ります。
	 * 
	 * <pre>{@code
	 * exec(new String[]{"-p", "8000"}, false);
	 * }</pre>
	 * 
	 * @param args コマンドライン引数配列
	 * @param killJvm 終了時にJVMを停止するかどうか
	 */
	private void exec(String[] args, boolean killJvm) {
		int returnCode = ClsProperties.LVL_INFO;
		int portNum = 0;
		boolean showUsage = false;
		this.killJvm = killJvm;
		prop = new ClsProperties();
		argsMap.put(ClsProperties.START_TIME_MS, String.valueOf(startTimeMs));

		// 引数処理
		for (int i = 0; i < args.length; ++i) {
			String arg = args[i];
			String argLower = arg.toLowerCase();
			if ("-h".equals(arg) || "--help".equals(argLower) || "-help".equals(argLower) || "-?".equals(arg) || "/?".equals(arg)) {
				showUsage = true;
			} else if ("-v".equals(argLower) || "--verbose".equals(argLower) || "-verbose".equals(argLower)) {
				argsMap.put(ClsProperties.VERBOSE, "1");
				if (args.length > (i + 1) && !args[i + 1].isEmpty() && !args[i + 1].startsWith("-")) {
					try {
						portNum = Integer.parseInt(args[i + 1]);
						argsMap.put(ClsProperties.PORT, String.valueOf(portNum));
					} catch (NumberFormatException e) {
						// ignore: ポート番号が数値でない場合はスキップ
					}
				}
			} else if ("--vv".equals(argLower) || "-vv".equals(argLower)) {
				argsMap.put(ClsProperties.VERBOSE, "2");
				if (args.length > (i + 1) && !args[i + 1].isEmpty() && !args[i + 1].startsWith("-")) {
					try {
						portNum = Integer.parseInt(args[i + 1]);
						argsMap.put(ClsProperties.PORT, String.valueOf(portNum));
					} catch (NumberFormatException e) {
						// ignore: ポート番号が数値でない場合はスキップ
					}
				}
			} else if ("--vvv".equals(argLower) || "-vvv".equals(argLower)) {
				argsMap.put(ClsProperties.VERBOSE, "3");
				if (args.length > (i + 1) && !args[i + 1].isEmpty() && !args[i + 1].startsWith("-")) {
					try {
						portNum = Integer.parseInt(args[i + 1]);
						argsMap.put(ClsProperties.PORT, String.valueOf(portNum));
					} catch (NumberFormatException e) {
						// ignore: ポート番号が数値でない場合はスキップ
					}
				}
			} else if ("-p".equals(argLower) || "--port".equals(argLower)) {
				if (args.length > (i + 1) && !args[i + 1].isEmpty() && !args[i + 1].startsWith("-")) {
					try {
						portNum = Integer.parseInt(args[i + 1]);
						argsMap.put(ClsProperties.PORT, String.valueOf(portNum));
					} catch (NumberFormatException e) {
						System.err.println("Invalid port number: " + args[i + 1]);
					}
				}
			}
		}

		// プロパティ設定
		for (String key : argsMap.keySet()) {
			prop.setValue(key, argsMap.get(key));
		}
		argsMap.clear();
		prop.setValue(ClsProperties.HOSTNAME, prop.getHostName());

		// USAGEチェック
		if (showUsage) {
			showUsage(ClsProperties.LVL_WARN);
			return;
		}

		// 開始メッセージ
		if (prop.getValue(ClsProperties.VERBOSE, ClsProperties.DEFAULT_VERBOSE) > 0) {
			System.out.println("#===<<< [" + className + "] START : " + prop.formatJstTime(startTimeMs, "yyyy/MM/dd HH:mm:ss") + ">>>===");
		}

		// DEBUG
		if (prop.getValue(ClsProperties.VERBOSE, ClsProperties.DEFAULT_VERBOSE) > 1) {
			System.out.println("############################################################");
			System.out.println("# HOSTNAME    : " + prop.getValue(ClsProperties.HOSTNAME, ClsProperties.DEFAULT_HOSTNAME));
			System.out.println("# LISTEN PORT : " + prop.getValue(ClsProperties.PORT, ClsProperties.DEFAULT_PORT));
			System.out.println("############################################################");
		}

		// TCP SERVER
		if (!prop.getIsTerminate()) {
			tcpServer = new ClsTcpServer(prop, "_tcpServer");
		}

		// シグナル受信スレッドの生成
		runningHook = true;
		final Thread mainThread = Thread.currentThread();
		shutdownHook = new Thread(() -> {
			System.out.println("[" + className + "] START : shutdownHook.run()");
			System.out.flush();
			if (prop != null) {
				prop.setIsTerminate(true);
			}
			if (tcpServer != null) {
				tcpServer.interrupt();
				tcpServer.terminate();
			}
			mainThread.interrupt();
			if (prop != null) {
				prop.sleep(SHUTDOWN_SLEEP_MS);
			}
			System.out.println("[" + className + "] END : shutdownHook.run()");
			System.out.flush();
			terminate(ClsProperties.LVL_WARN);
		}, "Thread-ShutdownHook");

		// シグナル受信スレッドの登録
		try {
			Runtime.getRuntime().addShutdownHook(shutdownHook);
		} catch (IllegalStateException e) {
			// ignore: すでにシャットダウン中の場合は無視
		}

		// TCPサーバの起動
		if (!prop.getIsTerminate() && tcpServer != null) {
			tcpServer.start();
		}

		// 終了待ちループ
		while (!prop.getIsTerminate()) {
			if (Thread.interrupted()) {
				prop.setIsTerminate(true);
				break;
			}
			prop.sleep(MAIN_POLL_SLEEP_MS);
		}

		// 終了処理
		if (tcpServer != null) {
			tcpServer.interrupt();
			tcpServer.terminate();
		}
		prop.sleep(SHUTDOWN_SLEEP_MS);

		// END
		terminate(returnCode);
	}

	/**
	 * アプリケーションの終了処理を行い、必要に応じてJVMを停止します。
	 * 
	 * <pre>{@code
	 * terminate(ClsProperties.LVL_INFO);
	 * }</pre>
	 * 
	 * @param returnCode 終了コード
	 */
	private void terminate(int returnCode) {
		// 終了メッセージ出力
		if (prop != null && prop.getValue(ClsProperties.VERBOSE, ClsProperties.DEFAULT_VERBOSE) > 0) {
			endTimeMs = System.currentTimeMillis();
			double elapsedSec = (double) (endTimeMs - startTimeMs) / 1000.0;
			System.out.println("#===<<< [" + className + "] EXIT (" + returnCode + ") : " + prop.formatJstTime(endTimeMs, "yyyy/MM/dd HH:mm:ss") + " : " + elapsedSec + " sec>>>===");
		}

		// コマンドラインから起動した場合はJVMごと強制終了
		if (killJvm && !isCancel) {
			if (runningHook) {
				// シャットダウンフックの登録後: Runtime.halt()で即座にJVMを停止
				Runtime.getRuntime().halt(returnCode);
			} else {
				// シャットダウンフック登録前
				System.exit(returnCode);
			}
		}
	}

	/**
	 * コマンドラインの利用方法（Usage）を標準出力に表示し、終了します。
	 * 
	 * <pre>{@code
	 * showUsage(ClsProperties.LVL_WARN);
	 * }</pre>
	 * 
	 * @param returnCode 表示後の終了コード
	 */
	private void showUsage(int returnCode) {
		System.out.println("");
		System.out.println("Usage:   java -jar TcpMocServer.jar [option...]");
		System.out.println("");
		System.out.println("Basic options:");
		System.out.println("  -p port                      LISTEN PORT       (Val = " + (prop != null ? prop.getValue(ClsProperties.PORT, ClsProperties.DEFAULT_PORT) : ClsProperties.DEFAULT_PORT) + ")");
		System.out.println("");
		System.out.println("Help options:");
		System.out.println("  -h                         SHOW THIS HELP MESSAGE");
		System.out.println("  -v|--vv|--vvv              SHOW VERBOSE");
		System.out.println("");
		System.out.println("exit code: NORMAL=0 / WARN=10 / ERROR=20 or HTTPCODE(200以外)");
		System.out.println("");
		terminate(returnCode);
	}

}
