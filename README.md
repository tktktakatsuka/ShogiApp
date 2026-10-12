# 将棋アプリ (ShogiApp)

Kotlin + Jetpack Compose で作った Android 向け将棋アプリです。

## Android Studio で開く

1. このリポジトリを clone する(Android Studio の **File > New > Project from Version Control** でも可)
2. **File > Open** で clone したフォルダ(リポジトリのルート)を開く
3. Gradle Sync が終わったら、エミュレータか実機を選んで ▶ Run

※ Android Studio から AGP / ライブラリの更新を提案された場合は、そのままアップデートして構いません。

## オンライン対戦の設定(Firebase)

オンライン対戦には Firebase(無料の Spark プランで可)を使います。
設定しなくてもビルド・CPU 対局・2人対局はでき、その場合はメニューの「オンライン対戦」が押せなくなります。

1. [Firebase コンソール](https://console.firebase.google.com/) でプロジェクトを作る(100マス計算アプリと同じプロジェクトでも可)
2. **Authentication** > ログイン方法 で **匿名** を有効にする
3. **Realtime Database** > データベースを作成(場所はどこでも可。ロックモードで開始)
4. Realtime Database の **ルール** タブに、このリポジトリの `database.rules.json` の中身を貼り付けて公開
5. プロジェクトの設定 > マイアプリ で **Android アプリを追加**。パッケージ名は `com.tktkcompany.shogi`
6. ダウンロードした `google-services.json` を **`app/` フォルダ直下** に置く
   (必ず手順3の後にダウンロードしてください。データベースの URL が含まれている必要があります)
7. Android Studio で Gradle Sync して実行

### 遊び方

- ひとりが「部屋を作る」→ 6桁の部屋番号が表示される(作った人が先手)
- もうひとりが「部屋に参加する」で番号を入力すると対局開始(後手側は盤が反対向きに表示されます)
- 対局中に退出すると投了扱いになります
- 終局後は「感想戦」で、2人で同じ盤面を見ながらチャットできます(盤面の操作も相手と共有されます)

> 感想戦の機能を追加したときに `database.rules.json` を更新しています。
> 以前ルールを貼り付けた場合は、Firebase のルールを最新の内容に貼り替えて公開してください。

## 感想戦のAI(最善手の表示)の設定

感想戦の「AIの最善手」ボタンは、将棋エンジン「やねうら王」と評価関数「水匠5」を使います。
エンジンは1つ約60MBあるためリポジトリには入れていません。設定しなくてもビルドでき、その場合はボタンが表示されません。

1. Android Studio の **SDK Manager > SDK Tools** で **NDK (Side by side)** を入れる
2. リポジトリのルートで次を実行する(初回は約130MBをダウンロードします)

   ```
   powershell -ExecutionPolicy Bypass -File scripts\build-engine.ps1
   ```

3. `app/src/main/jniLibs/<ABI>/libyaneuraou.so` ができるので、アプリをビルドし直す

- 既定では実機用(arm64-v8a)とエミュレータ用(x86_64)の両方を作ります。APK を小さくしたいときは
  `-Abi arm64-v8a` を付けて実機用だけにしてください(1つにつき APK が約35MB増えます)
- 最善手は自分の画面にだけ表示されます(オンラインの感想戦でも相手には見えません)
- 表示中は局面を動かすたびに約3秒考えます。検討中はメモリを数百MB使います

### ライセンスについて

やねうら王と、水匠5 を同梱した配布物はどちらも **GPLv3** です。エンジンを入れた APK を他の人に配布する場合は、
GPLv3 に従ってエンジンのソースコードを入手できるようにする必要があります
(`scripts/build-engine.ps1` に、使っているソースのコミットと評価関数の入手元を記録しています)。

- やねうら王: https://github.com/yaneurao/YaneuraOu
- 水匠5(同梱版): https://github.com/mizar/YaneuraOu/releases/tag/v7.6.3

## 広告(AdMob)の設定

ホーム画面と対局画面の一番下にバナー広告を表示します(感想戦中は表示しません)。
設定しなくてもビルドでき、その場合は Google のテスト広告(「Test Ad」と表示されるもの)が出ます。

1. [AdMob](https://admob.google.com/) でアプリを追加し、**バナー**の広告ユニットを作る
2. `local.properties`(リポジトリには入らないファイル)に次の2行を足す

   ```
   admobAppId=ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy
   admobBannerId=ca-app-pub-xxxxxxxxxxxxxxxx/zzzzzzzzzz
   ```

3. **release** ビルドを作ると本物の広告になる

- debug ビルド(Android Studio の ▶ Run)では、設定していても必ずテスト広告になります。
  開発中に本物の広告を表示・タップすると AdMob の規約違反になり、アカウントが停止されることがあるためです
- EU などの利用者にも公開する場合は、広告の同意確認(UMP SDK)の追加が必要です

## できること

- 和風デザイン: 木目調の五角形の駒(駒の種類で大きさが違う)、榧風の盤・星・筋と段の数字、和紙風の背景、明朝体
- 演出: 駒の移動アニメーション、駒を打つときの拡大→着地、「パチッ」という効果音と振動(オン/オフ可)、「王手」の表示、選んだ持ち駒が浮き上がる
- 対局情報: プレイヤー名、手番を示す光るマーク、直前の手の棋譜表記(▲７六歩 など)、持ち駒の枚数バッジ
- 平手の初期配置から対局
- 2人対局 / CPU 対局(CPU は後手・駒得ベースのかんたんな思考)
- オンライン対戦(部屋番号で相手と対局)
- 駒をタップすると移動できるマスをハイライト
- 持ち駒を打つ
- 成り・不成の選択(行き所のない駒は自動で成り)
- 反則手の除外: 自玉を取られる手、二歩、行き所のない駒、打ち歩詰め
- 王手表示、詰み判定、千日手(同一局面4回)
- 待った(CPU 戦では自分の手番まで戻る。オンライン対戦では不可)
- 投了
- 感想戦: 終局後に棋譜をたどる・別の手(変化)を試す。オンラインでは相手と盤面を共有しながらチャット(定型文ボタン付き)
- 感想戦でAIの最善手を表示(盤上の矢印・形勢・読み筋。要設定)
- 日本語と英語に対応(端末の言語で切り替わる。日本語以外では英語で表示し、棋譜は ▲P-7f のような英語式になる。駒は漢字のまま)

### 言語を追加するには

1. `app/src/main/res/values/strings.xml`(英語)をコピーして `values-<言語コード>/strings.xml` を作り、翻訳する
2. `app/src/main/res/xml/locales_config.xml` にその言語を追加する

## 構成

```
app/src/main/java/com/tktkcompany/shogi/
├── MainActivity.kt          画面の起動
├── UiText.kt                画面に出す文言(文字列リソースの ID と埋め込む値)
├── game/                    将棋のルール(Android に依存しない純粋な Kotlin)
│   ├── Piece.kt             駒・手番・マス・指し手
│   ├── Position.kt          局面、合法手生成、王手・詰み判定、SFEN 入出力
│   ├── Game.kt              対局の進行、待った、終局判定
│   ├── Usi.kt               指し手の文字列表記(USI 形式)
│   ├── Kifu.kt              棋譜表記(日本式の ▲７六歩、英語式の ▲P-7f)
│   ├── Review.kt            感想戦(本譜をたどる・変化を試す)
│   ├── Analysis.kt          検討エンジンの読み(評価値・読み筋)
│   └── Cpu.kt               CPU の思考
├── engine/
│   └── UsiEngine.kt         将棋エンジン(やねうら王)を起動して局面を検討させる
├── online/
│   ├── Room.kt              対局部屋の状態とサーバーの指し手からの局面復元
│   ├── OnlineRepository.kt  サーバーとのやりとりのインターフェース
│   └── FirebaseOnlineRepository.kt  Firebase Realtime Database での実装
└── ui/
    ├── ShogiViewModel.kt    画面の状態管理
    ├── ShogiApp.kt          ホーム画面・オンライン対戦のロビー画面
    ├── GameScreen.kt        対局画面(盤・駒台・ボタン・ダイアログ・王手の演出)
    ├── ReviewPanel.kt       感想戦の操作ボタン・棋譜一覧・チャット欄
    ├── Strings.kt           プレイヤー名・終局結果・形勢を端末の言語の文字列にする
    ├── SoundPlayer.kt       効果音(res/raw/koma.wav)
    ├── components/          盤・駒・駒台・和紙の背景・ボタン・バナー広告の部品
    └── theme/Theme.kt       和の配色と明朝体
```

ルール部分の単体テストは `app/src/test/` にあり、Android Studio で
`PositionTest` を右クリック > Run、またはターミナルで `./gradlew test` で実行できます。

## 今後のアイデア

- オンライン: ランダムマッチング、対局中の切断・再接続への対応、古い部屋の自動削除
- CPU を強くする(探索の深さ・評価関数の改善)
- 棋譜の表示・保存(KIF 形式)
- 持ち時間・対局時計
- 駒の画像化、効果音
- 駒落ち、詰将棋モード
