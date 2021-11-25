package viewermanager.manager;


import java.io.IOException;

public interface Manager {

    /**
     * Process Viewer's request of record.
     * Return {@code ServerResponse} includes records requested.
     * If records requested is not available, included records must be listed empty.
     *
     * ViewerからのRecord要求を処理する.
     * 要求されたレコードが入った {@code ServerResponse} を返す．
     * 要求新規追加についての処理されたレコードが存在しない場合，空のリストを利用する．
     *
     * @param request Request from Viewer. Viewerからの要求.
     * @return {@code ServerResponse} includes records. レコードを含む {@code ServerResponse}.
     */
    ServerResponse requestRecords(ServerRequest request);

    /**
     * Return the list of Information includes Viewer connected.
     * If no Viewer connected, Return empty list.
     *
     * 現在接続されているすべてのViewerの情報を返す．
     * 1つのViewerも接続されていない場合，空のリストを返す．
     *
     * @return {@code ServerResponse} includes list of all Viewers.
     *  すべてのViewerの情報を含んだ {@code ServerResponse}.
     */
    ServerResponse requestViewerList();

    /**
     * Return the list of Information includes Provider connected.
     * If no Provider connected, Return empty list.
     *
     * 現在接続されているすべてのProviderの情報を返す．
     * 1つのProviderも接続されていない場合，空のリストを返す．
     *
     * @return {@code ServerResponse} includes list of all Provider. すべてのProviderの情報を含んだ {@code ServerResponse}.
     */
    ServerResponse requestProviderList();

    /**
     * Process request for new open Viewer, and assign viewerId.
     * If the parameter {@code manual} is {@code false}, Manager assigns opened Provider to incoming Viewer automatically.
     * {@code manual} が {@code false}の時は，すでに開かれているProviderに自動的に接続する．
     * Otherwise, Manager only process open, and assign ID.
     * In this case, {@code ServerResponse} returned may contains no ProviderId.
     *
     * 新しく開かれたViewerについての処理を行い，ViewerIDを割り当てる．
     * その他の場合，開いてIDを割り当てるのみおこなう．
     * この場合，返却される{@code ServerResponse}はProviderIDを含んでいなくても構わない.
     *
     * @param manual is the 'manual connection' to Provider. Providerへの手動接続をおこなうかどうか．
     * @return {@code ServerResponse} includes information of opened Viewer's Id, and Provider's Id if it's connected.
     * @return 割り当てられたViewerのIDを含む {@code ServerResponse}. Providerに接続された場合はProviderのIDを含む.
     */
    ServerResponse requestOpenViewer(boolean manual);

    /**
     * Connect specified viewer and provider.
     *
     * 指定されたViewerとProviderを接続する.
     *
     * @param viewerId ID of Viewer connecting. 接続するViewerのID.
     * @param providerId ID of Provider connecting. 接続するProviderのID.
     * @return {@code ServerResponse} contains a information of connection. 接続の情報を含む{@code ServerResponse}.
     */
    ServerResponse requestConnection(int viewerId, int providerId);

    /**
     * Disconnect specified viewer from connected provider
     *
     * 指定されたViewerをProviderから切断する.
     *
     * @param viewerId ID of Viewer disconnecting. 切断するViewerのID.
     * @return {@code ServerResponse} contains a information of connection disconnected.
     *  切断された接続の情報を含む {@code ServerResponse}.
     */
    ServerResponse requestDisconnect(int viewerId);

    /**
     * Open new Provider and connect to Kernel.
     * If the {@code port} is {@code null}, use default port (may be 7000).
     *
     * 新しいProviderを開き，Kernelに接続する.
     * {@code port}が{@code null}のときは，標準ポート(7000)を使う．
     *
     * @param host hostname of kernel. カーネルのホスト名.
     * @param port port number of kernel. カーネルのポート番号.
     * @return {@code ServerResponse} contains a information of opened Provider.
     *   開かれたProviderの情報を持った{@code ServerResponse}.
     */
    ServerResponse requestOpenProvider(String host, Integer port) throws IOException;

    /**
     * Close opened Provider.
     *
     * Providerを閉じる．
     *
     * @param providerId ID of Provider closing. 閉じるProviderのID.
     * @return {@code ServerResponse} contains a closed ProviderId.
     *  閉じられたProviderIdを含む{@code ServerResponse}.
     * */
    ServerResponse requestCloseProvider(int providerId) throws IOException;

    /**
     * Open new Provider, and connect to Log File.
     *
     * 新しいProviderを開き，ログファイルに接続する.
     *
     * @param path the path of LogFile opening. 開くログファイルのパス.
     * @reutrn {@code ServerResponse} contains a opened Provider's information.
     *  開かれたProviderの情報を含む{@code ServerResponse}.
     * */
    ServerResponse requestOpenLogProvider(String path) throws IOException;

    /**
     * Get list of Log files under the registered directory.
     *
     * 登録されたディレクトリ内に存在するログファイルの一覧を得る.
     *
     * @return {@code ServerResponse} contains a list of log files paths.
     *  ログファイルのパスの一覧を含んだ{@code ServerResponse}.
     */
    ServerResponse requestLogList();

    /**
     * Get list of directories of Log Files Path registered.
     *
     * 登録されたログファイルのディレクトリの一覧を得る.
     *
     * @return {@code ServerResponse} contains a list of log directory paths.
     *  ログファイルのディレクトリ一覧を含んだ{@code ServerResponse}.
     * */
    ServerResponse requestLogPaths();

    /**
     * Register directory that may contains log file.
     *
     * ログファイルを含むディレクトリを登録する.
     *
     * @param path path of log directory. ログのディレクトリ.
     * @return . TODO: determine this specifies;;
     * */
    ServerResponse requestAddLogPath(String path) throws IOException;

    /**
     * Finish the Manager.
     *
     * Managerを終了させる.
     *
     * @return ACCEPTED.
     * */
    ServerResponse requestCloseServer();

    /**
     * Start the Manager.
     *
     * Managerを起動する.
     * */
    void run();

    /**
     * Stop all Provider, and Release all Resources.
     *
     * すべてのProviderを停止させ，リソースを開放する.
     * */
    void shutdown();

}
