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

## できること

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

## 構成

```
app/src/main/java/com/tktkcompany/shogi/
├── MainActivity.kt          画面の起動
├── game/                    将棋のルール(Android に依存しない純粋な Kotlin)
│   ├── Piece.kt             駒・手番・マス・指し手
│   ├── Position.kt          局面、合法手生成、王手・詰み判定、SFEN 入出力
│   ├── Game.kt              対局の進行、待った、終局判定
│   ├── Usi.kt               指し手の文字列表記(USI 形式)
│   └── Cpu.kt               CPU の思考
├── online/
│   ├── Room.kt              対局部屋の状態とサーバーの指し手からの局面復元
│   ├── OnlineRepository.kt  サーバーとのやりとりのインターフェース
│   └── FirebaseOnlineRepository.kt  Firebase Realtime Database での実装
└── ui/
    ├── ShogiViewModel.kt    画面の状態管理
    ├── ShogiApp.kt          ホーム画面・オンライン対戦のロビー画面
    ├── GameScreen.kt        盤・駒台・ボタンの対局画面
    └── theme/Theme.kt       色
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
