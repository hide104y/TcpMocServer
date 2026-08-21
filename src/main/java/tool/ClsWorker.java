package tool;

/**
 * リクエストキューからリクエストを取り出して非同期に実行するワーカースレッドクラスです。
 * <p>
 * {@link ClsTcpSvrScktChnnl} からリクエスト（{@link ClsRequest}）を取得し、
 * 各リクエストの処理を実行します。終了フラグまたは割り込みによって安全に停止します。
 * </p>
 * 
 * <pre>{@code
 * ClsWorker worker = new ClsWorker(prop, 0, channel, "worker-00");
 * worker.start();
 * }</pre>
 */
public class ClsWorker extends Thread {

	/** クラス名 */
	private final String className = ClsWorker.class.getName();
	/** プロパティ設定オブジェクト */
	private volatile ClsProperties prop = null;
	/** ソケットチャネル管理オブジェクト */
	private final ClsTcpSvrScktChnnl channel;
	/** スレッド固有の識別番号 */
	private final int threadNo;

	/**
	 * 指定されたパラメータでワーカースレッドインスタンスを構築します。
	 * 
	 * <pre>{@code
	 * ClsWorker worker = new ClsWorker(prop, 0, channel, "worker-00");
	 * }</pre>
	 * 
	 * @param prop プロパティ設定オブジェクト
	 * @param threadNo スレッド番号
	 * @param channel リクエストキューを管理するソケットチャネル
	 * @param name スレッド名
	 */
	public ClsWorker(ClsProperties prop, int threadNo, ClsTcpSvrScktChnnl channel, String name) {
		super(name);
		this.prop = prop;
		this.threadNo = threadNo;
		this.channel = channel;
	}

	/**
	 * ワーカースレッドのメインループを実行します。
	 * <p>
	 * アプリケーションの終了フラグが立つか割り込みが発生するまで、{@link #doWork()} を繰り返し実行します。
	 * </p>
	 * 
	 * <pre>{@code
	 * worker.start();
	 * }</pre>
	 */
	@Override
	public void run() {
		while (prop != null && !prop.getIsTerminate()) {
			try {
				doWork();
			} catch (InterruptedException e) {
				if (prop != null) {
					prop.setIsTerminate(true);
				}
				Thread.currentThread().interrupt();
				break;
			}
		}
		System.out.println("[" + className + "] BYE");
	}

	/**
	 * リクエストキューからリクエストを1件取り出し、処理を実行します。
	 * 
	 * <pre>{@code
	 * worker.doWork();
	 * }</pre>
	 * 
	 * @throws InterruptedException スレッド待機中に割り込みが発生した場合
	 */
	public void doWork() throws InterruptedException {
		if (channel == null) {
			return;
		}
		ClsRequest request = channel.takeRequest();
		if (Thread.currentThread().isInterrupted()) {
			System.out.println("[" + className + "] isInterrupted = true");
			throw new InterruptedException();
		}
		if (request != null && prop != null && !prop.getIsTerminate()) {
			request.execute(threadNo);
		}
	}

}
