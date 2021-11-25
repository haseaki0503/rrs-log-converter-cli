For Developer of Manager
---

### 全体の見取り図


追加情報
---

### 逆引きクラス名
| Method | Parameter | 動作 |
|--|--|--|
| `GET /server/interfaces` | `[]` | すべての経路情報を返す |
| `GET /server/errors` | `["count"]` | 今までに発生したエラーを返す |
| `POST /server/shutdown` | `[]` | サーバをシャットダウンする |
| `GET /viewer/open` | `["auto"]` | 新しいViewerを登録する |
| `POST /viewer/connect` | `["viewerId", "providerId"]` | ViewerとProviderを接続する |
| `POST /viewer/disconnect` | `["viewerId"]` | ViewerをProviderから切断する |
| `GET /viewer/record` | `["viewerId", "count", "time"]` | 割り当てられたProviderからRecordを取得する |
| `GET /viewer/list` | `[]` | 接続されたViewerの一覧を取得する |
| `POST /provider/open` | `["host", "port"]` | Kernelへ接続しProviderを作成する |
| `POST /provider/close` | `["providerId"]` | Providerを閉じる |
| `POST /provider/log/open` | `["path"]` | ログファイルを開く |
| `GET /provider/log/list` | `[]` | 開くことのできるログファイルの一覧を取得する |
| `POST /provider/log/path/add` | `["path"]` | ログファイルを検索するディレクトリを追加する |
| `GET /provider/log/path` | `[]` | ログファイルを検索するディレクトリの一覧を取得する |
| `GET /provider/list` | `[]` | Providerの一覧を取得する |

