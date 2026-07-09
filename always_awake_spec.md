# Black Screen Keep Awake

Version: 1.0

## 概要

Xperiaをテザリング専用機として運用するためのAndroidアプリ。

Game EnhancerのHSパワーコントロールと併用し、
画面を消灯させずにOLEDの黒表示によって消費電力を抑えることを目的とする。

本アプリはシンプルさと安定性を最優先とし、
不要な権限・設定変更は行わない。

---

# 対象環境

- Android 12以降
- Xperia
- Kotlin
- Jetpack Compose
- Material3

---

# コンセプト

本アプリは

「画面を黒くしてスリープしない」

それだけを行う。

システム設定は極力変更しない。

---

# 起動時の動作

Activity待機画面を表示.ボタンでActivityを開始する

# Activity開始時

Activity時に以下を実施する。

## 黒画面表示

画面全体を黒で表示する。

```
Color.Black
```

OLED端末では黒画素はほぼ発光しないため、
省電力化を期待する。

---

## Keep Screen On

画面をスリープさせない。

```
FLAG_KEEP_SCREEN_ON
```

を利用する。

---

## システムバー非表示

以下を非表示にする。

- Status Bar
- Navigation Bar

Immersive Modeを使用すること。

スワイプ時のみ一時表示は許可する。

---

## システム設定は変更しない

以下は一切変更しない。

- 画面輝度
- 自動輝度
- 回転設定
- 画面タイムアウト
- その他システム設定

WRITE_SETTINGS権限は不要。

---

# 終了条件

以下のいずれかで終了する。

## 1.

ホームへ戻る

または

アプリ切替

または

アプリ終了

→ Keep Screen On解除

---

## 2.

黒画面をダブルタップ

→ Activity終了

---

# 終了時の動作

- Keep Screen On解除
- Immersive Mode解除
- 通常Activity終了

システム設定の復元処理は不要
（変更していないため）

Activity待機画面に戻る

---

# 画面UI

UIは極力存在しない。

背景色のみ。

```
Black
```

ボタン

テキスト

アイコン

メニュー

一切不要。

---

# ダブルタップ

画面全体で判定する。

ダブルタップすると

```
finish()
```

を実行する。

---

# 給電判定

可能であれば実装する。

## 動作

充電中

↓

Keep Screen On有効

給電解除

↓

Keep Screen On解除

※実装が複雑になる場合は省略してよい。

---

# テザリング

テザリングのON/OFFは実装しない。

理由

Androidでは一般アプリからWi-Fiテザリングを操作できないため。

設定画面を開く機能も不要。

---

# 権限

基本的に権限不要。

WRITE_SETTINGS不要

Accessibility不要

Device Owner不要

Root不要

---

# パフォーマンス

本アプリは

CPU使用率

メモリ使用量

描画負荷

を極限まで小さくすること。

アニメーション不要。

リスト不要。

Compose再描画も最小限。

---

# ライフサイクル

Activityが停止したら

Keep Screen Onを解除する。

Activity再開時は

Keep Screen Onを有効にする。

ライフサイクルに従って正しく管理すること。

---

# アプリ名

仮名称

Black Screen Keep Awake

変更しやすいよう定数化すること。

---

# 将来拡張（今回は実装不要）

- テザリング状態表示
- 充電状態表示
- 起動時自動開始
- クイック設定タイル
- 通知から終了
- Always On Displayとの連携
- Game Enhancer向け最適化

---

# 実装方針

シンプル・堅牢・最小構成を優先する。

不要な機能は実装しない。

Composeらしい実装を行い、
Androidのベストプラクティスに従うこと。

コードは可読性を重視し、
適切なコメントを付与すること。

```
Project Goal

「黒画面を表示し続け、
画面をスリープさせないだけの
軽量Androidアプリ」
```
