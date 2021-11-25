Entity Specification
====

## 概要

## 共通

### Type

#### Entityの種類

| 種類名 | 意味 | 値 | 略称 |
|--|--|--|--|
| Ambulance Team | 救急隊 | `TacticsAmbulance` | AT |
| Fire Brigade | 消防隊 | `TacticsFire` | FB |
| Police Force | 土木隊 | `TacticsPolice` | PF |
| Civilian | 市民 | `Civilian` | CV |
| Building | 建物 | `Building` | BD |
| Refuge | 避難所 | `Refuge` | RF |
| Ambulance Centre | 救急本部 | `ControlAmbulance` | CA |
| Fire Station | 消防本部 | `ControlFire` | CF |
| Police Office | 土木本部 | `ControlPolice` | CP |
| Gas Station | ガソリンスタンド | `GasStation` | GS |
| Road | 道路 | `Road` | RD |
| Hydrant | 給水栓 | `Hydrant` | HD |
| Blockade | ガレキ | `Blockade` | BK |

#### Commandの種類

| 種類名 | 意味 | 値 | 略称 |
|--|--|--|--|
| Move | 移動 | Move | Move |
| Rest | 待機 | Rest | Rest |
| Load | 移送開始 | Load | Load |
| Unload | 移送終了 | Unload | Unload |
| Rescue | 埋没救助 | Rescue | Rescue |
| Extinguish | 消火活動 | Extinguish | EXT |
| Clear | ガレキ除去 | Clear | Clear |
| Clear (Legacy) | ガレキ除去(レガシー) | LClear | LClear |
| Radio | 無線伝達 | Radio | Radio |
| Voice | 口頭伝達 | Voice | Voice |
| Subscribe | 全体伝達 | Subscribe | Subs |
| Tell | 全体伝達 | Tell | Tell |


### Record

| プロパティ名 | 想定型 | 意味 |
|--|--|--|
| time | `Integer` | そのレコードのステップ番号 |
| score | `Float` | このステップでのスコア |
| world | `Entity[]` | すべてのEntityの情報 |
| changes | `Entity[]` | 変更されたEntityの差分情報 |
| commands | `Command[]` | 実行された行動や通信の情報 |
| map | `MapInfo` | そのシミュレーションで使われる地図情報 |
| config | `String[String]` | そのシミュレーションで使われるコンフィグ |

### Entity

| プロパティ名 | 想定型 | 含まれる`type` | 意味 |
|--|--|--|--|
| id | `Integer` | すべて | EntityのID |
| type | `String` | すべて | Entityの種類|
| x | `Integer` | AT,FB,PF,CV,BK | EntityのX座標 |
| y | `Integer` | AT,FB,PF,CV,BK | EntityのY座標 |
| position | `Integer` | AT,FB,PF,CV,BK | Entityの居るAreaのEntity ID |
| damage | `Integer` | AT,FB,PF,CV | Entityが受けているダメージ |
| buried | `Integer` | AT,FB,PF,CV | Entityの埋没度 |
| hp | `Integer` | AT,FB,PF,CV | EntityのHP(体力) |
| history | `Point[]` | AT,FB,PF,CV | Entityの移動経路(座標のリスト) |
| travel | `Integer` | AT,FB,PF,CV | Entityの移動距離 |
| board | `Integer` | AT | 積載しているEntityのID(未実装) |
| water | `Integer` | FB | 積載している水の総量 |
| blockades | `Integer[]` | RD,HD | Entityの上にあるBlockadesのIDリスト |
| temp | `Integer` | RF,CA,CF,CP,BD,GS | 建物の熱量 |
| broken | `Integer` | RF,CA,CF,CP,BD,GS | 建物の倒壊度 |
| fiery | `Integer` | RF,CA,CF,CP,BD,GS | 建物の燃焼度 |
| repairCost | `Integer` | BK | Blockadeの修繕コスト |
| apexes | `Point[]` | BK | Blockadeの頂点情報(形状情報) |


### Map
#### MapInfo

| プロパティ名 | 想定型 | 意味 |
|--|--|--|
| width | `Integer` | 地図の幅 |
| height | `Integer` | 地図の高さ |
| entities | `AreaInfo` | 地形(建物・道路)情報 |

#### AreaInfo

| プロパティ名 | 想定型 | 意味 |
|--|--|--|
| id | `Integer` | この地形情報を持つEntityのID |
| type | `String` | この地形情報の示す種類 |
| x | `Integer` | 地図上でのX座標 |
| y | `Integer` | 地図上でのY座標 |
| edges | `Edge[]` | この地形の外形の辺リスト |
| neighbours | `Integer[]` | この地形に隣り合う地形のEntityIDリスト |


### Command

| プロパティ名 | 想定型 | 含まれる`type` | 意味 |
|--|--|--|--|
| id | `Integer` | すべて |  この行動を起こしたEntityのID |
| type | `String` | すべて | この行動の種類 |
| path | `Integer[]` | Move,Clear | 移動経路 |
| x | `Integer` | Move,Clear | 移動先のX座標 |
| y | `Integer` | Move,Clear | 移動先のY座標 |
| channel | `Integer` | Radio,Tell | 通信チャンネル |
| target | `Integer` | Load,Rescue,Extinguish,Clear | 行動対象のEntityID |
| water | `Integer` | Extinguish | 消火に使う水の量 |
| messageSize | `Integer` | Radio,Voice,Tell | 通信のバイト数 |


## Communication Record

### 基本形

```json
{
    "status": "要求に対する返答コード",
    "message": "サーバからの伝達事項",
    "data": "要求に対する返答"
}
```

### Recordの要求

#### コマンド

- `/viewer/record`

#### 返答

```json
{
    "record": "レコードのリスト",
    "reconnected": "Viewerが今までとは違うProviderに再接続されたかを示す"
}
```

#### Viewerの追加

#### コマンド

- `/viewer/open`

#### 返答

```json
{
    "viewerId": "ViewerのID"
}
```

### Providerの展開 (Kernelへの接続)

#### コマンド

- `/provider/open`

#### 返答

```json
{
    "providerId": "ProviderのID",
    "status": "Providerの現在の状態",
    "online": "常にTrue",
    "host": "Kernelのホスト名",
    "port": "Kernelへの接続ポート番号",
    "timeStep": "現在のステップ数",
    "maxTimeStep": "最大ステップ数",
    "chainedProviderId": "次に自動接続されるProviderのID"
}
```

### Providerの展開 (LogFileの読込)

#### コマンド

- `/provider/log/open`

#### 返答

```json
{
    "providerId": "ProviderのID",
    "status": "Providerの現在の状態",
    "online": "常にFalse",
    "IDString": "このProviderを一意に表す文字列",
    "timeStep": "現在のステップ数",
    "maxTimeStep": "最大ステップ数"
}
```

### Providerの切断

#### コマンド

- `/provider/close`

#### 返答

```json
{
    "providerId": "ProviderのID"
}
```

### ViewerとProviderの接続,切断

#### コマンド

- `/viewer/connect`
- `/viewer/disconnect`

#### 返答

```json
{
    "viewerId": "ViewerのID",
    "providerId": "ProviderのID"
}
```

### Viewer一覧の取得

#### コマンド

- `/viewer/list`

#### 返答

```json
[{
    "viewerId": "ViewerのID",
    "timeStep": "最後に送信したレコードのステップ数",
    "timeStamp": "最後に要求を受け取った時間",
    "providerId": "接続されているProviderのID",
    "status": "現在のViewerの状態"
}]
```

### Provider一覧の取得

#### コマンド

- `/provider/list`

#### 返答

`Providerの展開 (Kernelへの接続)`,`Providerの展開 (LogFileの読込)`の返答
どちらかを含むリスト

### ログファイルリストの取得

#### コマンド

- `/provider/log/list`

#### 返答

```json
[
    "ファイル名1",
    "ファイル名2",
    ...,
    "ファイル名n"
]
```

### ログファイルのディレクトリパス一覧の取得

#### コマンド

- `/provider/log/path`

#### 返答

```json
[
    "ディレクトリ名1",
    "ディレクトリ名2",
    ...,
    "ディレクトリ名n"
]
```


## Viewer Log

#### ファイルの形式

ファイルはすべてMsgPackで直列化されている．

```json
{
    "version": "ログファイル・フォーマットのバージョン",
    "maxTimeStep": "ログに含まれるステップ数",
    "mapName": "Configに示されるマップ名",
    "map": "地図情報->`MapInfo`",
    "config": "シミュレーションのコンフィグ",
    "log": "レコード一覧"
}
```

#### logの形式

```json
{
    "1": "1ステップ目のレコード -> Record",
    "2": "2ステップ目のレコード -> Record",
    ...,
    "300": "300ステップ目のレコード -> Record"
}
```



