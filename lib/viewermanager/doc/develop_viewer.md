
For Developer of Viewer
---

### ViewerManagerとの通信について

Viewerの接続，通信はすべてHTTPを用いておこなわれる．
使用するメソッドは`GET`，`POST`であり，指定を外れた場合はエラーが返される．

HTTPリクエストのパラメータはすべて`param1=a&param2=b`の形で受け付けられる．
これは`GET`，`POST`に関わらず同様である．

また，ViewerManagerからのレスポンスはすべてJSONで表現される．

#### ViewerManagerへの接続

ViewerManagerへの接続(Viewerの登録)は，
`GET /viewer/open`を利用する．

受け付けられた場合`viewerId`が返却されるため，
以降このIDを使って通信をおこなう．

```example
GET http://host:8080/viewer/open

> {"viewerId": 1}
```

#### Providerへの接続(Connectionの確立)
接続したViewerがどのProviderから情報を得るかを決定するため，
`POST /viewer/connect`を使いConnectionの確立をおこなう．
パラメータとして，自らの`viewerId`と接続先の`providerId`を指定する．

```example
POST http://host:8080/viewer/connect?viewerId=1&providerId=2
```

##### 自動接続
`GET /viewer/open`に`auto=true`のパラメータを渡すと，
現在開かれているProviderから自動的に選択され，接続される．

```exmaple
GET http://host:8080/viewer/open?auto=true
```

##### Providerの取得

現在開かれているProviderを取得するためには
`GET /provider/list`を利用する．

#### Recordの取得

ViewerManagerからReceordを取得するためには
`GET /viewer/record`を利用する．
パラメータとして`viewerId`を指定する．

```example
GET http://host:8080/viewer/record?viewerId=1
```

利用できるパラメータとして`count`, `time`が存在する．
`count`は取得する最大個数を設定する．
`time`は取得するステップ数を設定でき，`count`と共に指定しある時間から指定個数取得できる．


### レコードのフォーマット

ひとつの`Record`には次の情報が含まれている．

全ての要素が含まれているわけではなく，最小限の提供がおこなわれる．
例えば`world`や`map`は最初のステップでのみ与えられる．
逆に，差分情報である`changes`は最初のステップでは与えられない．

`changes`にはそのステップで得られる，Entityについての差分情報が入っている．
例えばあるエージェントAがエリア1からエリア2に移動し，その他の変更点がない場合，
得られる`changes`は
```json
{"changes": {100: {"id": 9, "area": 2}}}
```
となる．

#### Entityについて

Entityは，RRSのシミュレーションにおいて表現される建物や救急隊，市民を示す．
RRS上では`StandardEntity`を継承したそれぞれのオブジェクトで表される．

ViewerManagerはRRSから受け取った`StandardEntity`を独自のEntityに変換する．
ViewerManagerから提供されるEntityはすべて差分であり，
前回までの結果に上書きすることで正しいデータが得られるはずである．

Entityの詳細については，独自パラメータである`type`を参照することで
知ることができる．
`type`は`Building`や`TacticsAmbulance`など，Entityの種類を示す．
そのEntityに含まれている情報はこの`type`によってある程度断定できる．
もしそのパラメータが存在しない場合，情報としても与えられない．
静的な言語であれば`null`，動的な言語であれば`undefined`などになるはずである．

Entityに含まれる情報に関しては後述する．

#### Commandについて

Commandは，RRSのシミュレーションにおける行動を示すデータである．
RRS上では`AKMove`や`AKExtinguish`などのオブジェクトで与えられる．

ViewerManagerではこれらを`Command`に変換する．
Entityと同様に，`type`によって種類を断定することができる．

Commandに含まれる情報に関しては後述する．


### 通信フォーマット

HTTPを介した通信では，レスポンスはすべてJSON形式で送信される．


#### Recordの要求をした場合

ひとつの`Record`はMsgPackで直列化されている．
レコード要求に対するレスポンスはJSONの配列を用いて返されるが，
その中身はMsgPackのデータである．
バイナリの送信のため，データはBase64でエンコードされている．

そのため，データを取り出すためには

1. JSONから配列の1要素を取り出す
2. 取り出したデータをBase64でデコードする
3. デコードして得られたバイナリをMsgPackでunpackする

という手順が必要である．


