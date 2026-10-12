# 将棋アプリ プライバシーポリシー

制定日：2026年10月12日

tktkcompany（以下「開発者」といいます）は、Android アプリ「将棋」（以下「本アプリ」といいます）における利用者の情報の取り扱いについて、以下のとおり定めます。

## 1. 取得する情報と利用目的

### (1) オンライン対戦を利用する場合

本アプリはオンライン対戦のために Google LLC が提供する Firebase（Firebase Authentication および Firebase Realtime Database）を使用します。オンライン対戦を利用すると、次の情報が Firebase のサーバーに送信・保存されます。

| 情報 | 利用目的 |
| --- | --- |
| 匿名のユーザーID（Firebase が自動で発行するもの） | 対局の参加者を区別するため |
| 部屋番号、部屋の作成日時、対局の状態 | 対局相手と部屋を共有するため |
| 指し手、投了の情報 | 対局を進行するため |
| 感想戦で表示している局面 | 対局相手と感想戦の盤面を共有するため |
| チャットで送信したメッセージと送信日時 | 対局相手とのチャットのため |

氏名、メールアドレス、電話番号などの登録は不要です。匿名のユーザーIDから、開発者が利用者個人を特定することはありません。

チャットのメッセージは対局相手に表示されます。個人を特定できる情報（氏名、住所、連絡先など）は書き込まないでください。

### (2) 広告の表示

本アプリは Google LLC が提供する広告配信サービス Google AdMob を利用しています。AdMob は、広告の配信・効果測定・不正防止のために、広告ID、端末の情報（機種、OS のバージョンなど）、IP アドレス、広告の表示・タップの状況などを取得する場合があります。これらの情報は Google のプライバシーポリシーに従って取り扱われます。

- Google のプライバシーポリシー：<https://policies.google.com/privacy>
- Google による広告でのデータ利用について：<https://policies.google.com/technologies/partner-sites>

広告ID は、端末の「設定 > プライバシー > 広告」（端末により表記が異なります）から削除・リセットでき、パーソナライズド広告を無効にできます。

### (3) 端末内にのみ保存する情報

効果音・振動のオン/オフの設定は、利用者の端末内にのみ保存し、外部には送信しません。

CPU 対局、2人対局、および感想戦での AI 解析（最善手の表示）は端末内で処理され、対局内容が外部に送信されることはありません。

## 2. 第三者への提供

開発者は、次の場合を除き、取得した情報を第三者に提供しません。

- 利用者の同意がある場合
- 法令に基づく場合
- 本ポリシーに記載したサービス（Firebase、Google AdMob）の利用に伴い、その提供元に情報が送信される場合

## 3. 外国にある第三者への提供

Firebase および Google AdMob の提供元である Google LLC は米国の事業者であり、情報は米国その他の国にあるサーバーで処理・保存される場合があります。Google LLC の情報の取り扱いについては、前記の Google のプライバシーポリシーをご確認ください。

## 4. 情報の保存期間と削除

オンライン対戦の部屋の情報（指し手、感想戦の局面、チャットを含む）は、現在、自動では削除していません。削除を希望される場合は、下記のお問い合わせ先までご連絡ください。対象の部屋番号をお知らせいただければ、確認のうえ削除します。

本アプリをアンインストールすると、端末内に保存した設定は削除されます。

## 5. 安全管理

開発者は、Firebase のセキュリティルールにより、対局データの書き込みを対局の参加者に限定するなど、情報の漏えい・改ざんを防ぐための措置を講じています。

## 6. お子さまの利用について

本アプリは13歳未満のお子さまを主な対象としたものではありません。13歳未満のお子さまがオンライン対戦のチャットを利用する場合は、保護者の方の同意のうえでご利用ください。

## 7. プライバシーポリシーの変更

開発者は、必要に応じて本ポリシーを変更することがあります。変更後のポリシーは、このページに掲載した時点から効力を生じます。

## 8. お問い合わせ

本ポリシーおよび本アプリの情報の取り扱いに関するお問い合わせは、次のページからお願いします。

<https://github.com/tktktakatsuka/ShogiApp/issues>

---

## Privacy Policy (English summary)

Effective date: October 12, 2026

- **Online play:** The app uses Google Firebase (Authentication and Realtime Database). When you play online, an anonymous user ID, room code, moves, resignation, the shared post-game review position, and chat messages are stored on Firebase servers to run the game with your opponent. No name, email address, or phone number is required. Chat messages are shown to your opponent; please do not share personal information in chat.
- **Ads:** The app shows ads through Google AdMob, which may collect the advertising ID, device information, IP address, and ad interaction data. See Google's privacy policy: <https://policies.google.com/privacy>. You can reset or delete your advertising ID in your device settings.
- **On-device only:** Sound settings, CPU and two-player games, and AI analysis stay on your device and are not sent anywhere.
- **Sharing:** Data is not shared with third parties except Google (as the provider of Firebase and AdMob), with your consent, or where required by law. Google may process data on servers in the United States and other countries.
- **Retention and deletion:** Online room data is not deleted automatically at this time. To request deletion, contact us with your room code.
- **Children:** The app is not directed at children under 13.
- **Contact:** <https://github.com/tktktakatsuka/ShogiApp/issues>
