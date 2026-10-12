# 将棋アプリ (ShogiApp)

Kotlin + Jetpack Compose で作った Android 向け将棋アプリです。

## Android Studio で開く

1. このリポジトリを clone する(Android Studio の **File > New > Project from Version Control** でも可)
2. **File > Open** で clone したフォルダ(リポジトリのルート)を開く
3. Gradle Sync が終わったら、エミュレータか実機を選んで ▶ Run

※ Android Studio から AGP / ライブラリの更新を提案された場合は、そのままアップデートして構いません。

## できること

- 平手の初期配置から対局
- 2人対局 / CPU 対局(CPU は後手・駒得ベースのかんたんな思考)
- 駒をタップすると移動できるマスをハイライト
- 持ち駒を打つ
- 成り・不成の選択(行き所のない駒は自動で成り)
- 反則手の除外: 自玉を取られる手、二歩、行き所のない駒、打ち歩詰め
- 王手表示、詰み判定、千日手(同一局面4回)
- 待った(CPU 戦では自分の手番まで戻る)

## 構成

```
app/src/main/java/com/tktkcompany/shogi/
├── MainActivity.kt          画面の起動
├── game/                    将棋のルール(Android に依存しない純粋な Kotlin)
│   ├── Piece.kt             駒・手番・マス・指し手
│   ├── Position.kt          局面、合法手生成、王手・詰み判定、SFEN 入出力
│   ├── Game.kt              対局の進行、待った、終局判定
│   └── Cpu.kt               CPU の思考
└── ui/
    ├── ShogiViewModel.kt    画面の状態管理
    ├── ShogiScreen.kt       盤・駒台・ボタンの UI
    └── theme/Theme.kt       色
```

ルール部分の単体テストは `app/src/test/` にあり、Android Studio で
`PositionTest` を右クリック > Run、またはターミナルで `./gradlew test` で実行できます。

## 今後のアイデア

- CPU を強くする(探索の深さ・評価関数の改善)
- 棋譜の表示・保存(KIF 形式)
- 持ち時間・対局時計
- 駒の画像化、効果音
- 駒落ち、詰将棋モード
