ViewerManager
===

What is this
---

RoboCupRescue Simulationのサーバ/ログファイルから取り出した
シミュレーションのデータを変換し，
MsgPack/JSONでシリアライズして出力するサーバです．

Requirements
---

- Oracle Java SE 8 Runtime


Directory
---

web/libraryは必須

```
ViewerManager
 + ViewerManager.jar
 + manager.cfg
 + web
 |  + ...
 + library
 |  + ...
```

How to use
---

### 起動方法
#### 1. 通常起動

```bash
java -jar ViewerManager.jar -c manager.cfg
```

#### 2. manager.cfgがない場合

```bash
java -jar ViewerManager.jar -d # manager.cfgが生成される
java -jar ViewerManager.jar -c manager.cfg
```

### 利用方法

1. Managerを立ち上げる
2. Kernel/Logへ接続する
    1. Webインターフェイスから要求
    2. HTTP POSTコマンドで要求
3. ViewerからManagerへ接続する
    1. Viewerからの要求に応じてレコードが返答される．

### manager.cfg (config)について

Managerの動作について設定をおこないます．

| 要素名 | 初期値 | 意味 |
|--|--|--|
| `http.path.static` | `/web/static` | Webサーバの静的ファイルのパス |
| http.port | 8080 | Webサーバの待受ポート番号 |
| rrs.viewer.default.port | 7000 | RRS Kernelへの接続ポート番号 |
| rrs.viewer.wait.count | 60 | RRS Kernelへの接続リトライ回数 |
| rrs.viewer.wiat.ms | 1000 | RRS Kernelへの接続リトライ待ち時間(ミリ秒) |
| viewer.manager.log.dir | logs | Viewer Log 出力ディレクトリ(未実装) |
| viewermanager.logger.level | INFO | ログ出力レベル (log4j, DEBUG/INFO/WARN/ERROR) |
| viewermanager.logger.main | viewer.main.MainLogger | ログ出力用クラス名 |

`#`以降はコメントです．

### 各種名称

ViewerManager内で用いられる特殊用語一覧

| 名称 | 説明 |
|--|--|
| ViewerManager | プログラム本体，及び機能のこと |
| Entity | RRSのシミュレーション状況を表現するデータ群 |
| Command | RRSのシミュレーションでの行動を示すデータ群 |
| Record | 1ステップに与えられる更新情報をまとめたもの |
| Viewer | 接続される表示プログラム，及びEntityを受け取る経路 |
| Provider | Entityを提供する機能を持ったクラス群 |
| Kernel | RRSのシミュレータ |
| Log | RRSのログ・ファイル |
| Viewer Log | Entityに変換されたデータを保存したログファイル |
| Viewer ID | Viewerに割り当てられるID |
| Provider ID | Providerに割り当てられるID |
| Connection | ViewerとProviderの間のリンクを作ること |
| Disconnect | ViewerとProviderの間のリンクを切断すること |


### 詳細情報

[EntityのJSONでの表現方法](./doc/entity_spec.md)
[Viewer開発者向け](./doc/develop_viewer.md )
[Manager開発者向け](./doc/develop_manager.md)
[質疑応答](./doc/faq.md)





