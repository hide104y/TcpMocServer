package tool;

/**
 * リクエストキューの管理およびワーカースレッドの統括を行うクラスです。
 * <p>
 * 固定長リングバッファ構造のブロッキングキューを内包し、
 * サーバースレッドからのリクエスト追加（{@link #putRequest(ClsRequest)}）と
 * ワーカースレッドからのリクエスト取得（{@link #takeRequest()}）のスレッド同期を制御します。
 * </p>
 * 
 * <pre>{@code
 * ClsProperties prop = new ClsProperties();
 * ClsTcpSvrScktChnnl channel = new ClsTcpSvrScktChnnl(prop);
 * channel.startWorkers();
 * channel.putRequest(request);
 * }</pre>
 */
public class ClsTcpSvrScktChnnl {

	/** ワーカースレッド数の最小値 */
	private static final int MIN_WORKER_THREADS = 1;
	/** ワーカースレッド数の最大値 */
	private static final int MAX_WORKER_THREADS = 99;
	/** 新規リクエスト受入上限閾値（キュー滞留件数） */
	private static final int ACCEPT_THRESHOLD = 90;

	/** クラス名 */
	private final String className = ClsTcpSvrScktChnnl.class.getName();
	/** プロパティ設定オブジェクト */
	private volatile ClsProperties prop = null;
	/** キューの末尾インデックス（書き込み位置） */
	private int tail = 0;
	/** キューの先頭インデックス（読み出し位置） */
	private int head = 0;
	/** 現在キューに格納されているリクエスト数 */
	private int count = 0;

	/** リクエスト格納配列（リングバッファ） */
	private final ClsRequest[] requests;
	/** ワーカースレッド配列 */
	private final ClsWorker[] workers;

	/**
	 * 指定されたプロパティを元に {@code ClsTcpSvrScktChnnl} インスタンスおよびワーカースレッドを構築します。
	 * 
	 * <pre>{@code
	 * ClsTcpSvrScktChnnl channel = new ClsTcpSvrScktChnnl(prop);
	 * }</pre>
	 * 
	 * @param prop プロパティ設定オブジェクト
	 */
	public ClsTcpSvrScktChnnl(ClsProperties prop) {
		this.prop = prop;
		this.requests = new ClsRequest[ClsProperties.DEFAULT_MAX_QUEUE];
		int threads = prop != null ? prop.getValue(ClsProperties.WORKER_THREADS, MIN_WORKER_THREADS) : MIN_WORKER_THREADS;
		String name = prop != null ? prop.getValue(ClsProperties.WORKER_NAME, ClsProperties.DEFAULT_WORKER) : ClsProperties.DEFAULT_WORKER;
		if (threads < MIN_WORKER_THREADS) {
			threads = MIN_WORKER_THREADS;
		}
		if (threads > MAX_WORKER_THREADS) {
			threads = MAX_WORKER_THREADS;
		}
		this.workers = new ClsWorker[threads];
		for (int i = 0; i < this.workers.length; i++) {
			this.workers[i] = new ClsWorker(prop, i, this, name + "-" + String.format("%02d", i));
		}
	}

	/**
	 * 現在キューに滞留しているリクエスト数を取得します。
	 * 
	 * <pre>{@code
	 * int pendingCount = channel.getCount();
	 * }</pre>
	 * 
	 * @return 滞留リクエスト数
	 */
	public synchronized int getCount() {
		return count;
	}

	/**
	 * キューが保持可能な最大リクエスト数を取得します。
	 * 
	 * <pre>{@code
	 * int max = channel.getMaxRequests();
	 * }</pre>
	 * 
	 * @return 最大キュー保持数 (100)
	 */
	public int getMaxRequests() {
		return ClsProperties.DEFAULT_MAX_QUEUE;
	}

	/**
	 * 新しいリクエストを受け入れ可能かどうかを判定します。
	 * <p>
	 * 滞留件数が 90 件未満の場合に {@code true} を返します。
	 * </p>
	 * 
	 * <pre>{@code
	 * if (channel.canAccept()) {
	 *     channel.putRequest(request);
	 * }
	 * }</pre>
	 * 
	 * @return 受入可能な場合は {@code true}、満杯に近い場合は {@code false}
	 */
	public synchronized boolean canAccept() {
		return count < ACCEPT_THRESHOLD;
	}

	/**
	 * すべてのワーカースレッドを開始します。
	 * 
	 * <pre>{@code
	 * channel.startWorkers();
	 * }</pre>
	 */
	public void startWorkers() {
		for (ClsWorker worker : workers) {
			if (worker != null) {
				worker.start();
			}
		}
	}

	/**
	 * すべてのワーカースレッドに対して割り込みを通知し、停止を要求します。
	 * 
	 * <pre>{@code
	 * channel.stopWorkers();
	 * }</pre>
	 */
	public void stopWorkers() {
		for (ClsWorker worker : workers) {
			if (worker != null) {
				worker.interrupt();
			}
		}
	}

	/**
	 * リクエストをキューの末尾に追加します。
	 * <p>
	 * キューが満杯の場合は空きが生じるまで呼び出し元スレッドをブロック（待機）させます。
	 * </p>
	 * 
	 * <pre>{@code
	 * channel.putRequest(request);
	 * }</pre>
	 * 
	 * @param request 追加するリクエストオブジェクト
	 */
	public synchronized void putRequest(ClsRequest request) {
		if (request == null) {
			return;
		}
		while (count >= requests.length) {
			try {
				wait();
			} catch (InterruptedException e) {
				System.out.println("[" + className + "] InterruptedException in putRequest");
				Thread.currentThread().interrupt();
				return;
			}
		}
		requests[tail] = request;
		tail = (tail + 1) % requests.length;
		count++;
		notifyAll();
	}

	/**
	 * キューの先頭からリクエストを1件取り出します。
	 * <p>
	 * キューが空の場合はリクエストが追加されるまで呼び出し元スレッドをブロック（待機）させます。
	 * </p>
	 * 
	 * <pre>{@code
	 * ClsRequest req = channel.takeRequest();
	 * }</pre>
	 * 
	 * @return 取り出されたリクエストオブジェクト。割り込み終了時は {@code null}
	 */
	public synchronized ClsRequest takeRequest() {
		while (count <= 0) {
			try {
				wait();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return null;
			}
		}
		ClsRequest request = requests[head];
		requests[head] = null;
		head = (head + 1) % requests.length;
		count--;
		notifyAll();
		return request;
	}

}
