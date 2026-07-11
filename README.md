# Always Awake

スリープせず**OLEDの黒表示** によって消費電力を抑えながらデバイスを起動状態に保つ Android アプリ。「画面を黒くしてスリープしない」ことだけを、システム設定を一切変更せずに行う。

---

## 動作要件

### OS

| 項目 | 値 |
|---|---|
| 最小 | Android 12 (API 31) |
| 対象 | Android 16 (API 36) |
| ビルド | SDK 37 / JDK 17+（Java互換: 11） |

### ハードウェア

必須要件はなく、Android 12 以降であれば動作する。ただし本アプリの省電力効果は表示中の黒画素が発光しないディスプレイに依存するため、**OLED / AMOLED**（黒画素が完全消灯）で最大の効果を発揮する。**MiniLED** 等のローカルディミング対応パネルでも一定の効果が期待できる。通常の液晶（バックライト常時点灯）では消費電力の削減効果はほとんど得られない。

追加権限は一切不要（`WRITE_SETTINGS` / Accessibility / Device Owner / Root いずれも不要）。

---

## 導入方法（Android Studio・デバッグモード）

```
1. リポジトリを取得
   git clone <repository-url>

2. Android Studio で開く
   File > Open > AlwaysAwake フォルダを選択
   → Gradle Sync が自動実行される

3. デバイスを接続
   実機: USB接続 + 開発者オプションで「USBデバッグ」を有効化
   仮想: Device Manager から API 31+ のエミュレータを作成・起動

4. 実行
   ツールバーの ▶ Run 'app'（Shift+F10）
   → debug ビルドが端末にインストールされ起動する
```

コマンドラインからビルドする場合は `./gradlew assembleDebug`（生成物は `app/build/outputs/apk/debug/`）。

---

## 使用技術

| 分類 | 技術 |
|---|---|
| 言語 | Kotlin |
| UI | Jetpack Compose |
| デザイン | Material 3 |
| 画面点灯 | `WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON` |
| 全画面表示 | `WindowInsetsControllerCompat`（Immersive Mode） |
| 電源監視 | `BroadcastReceiver`（`ACTION_POWER_DISCONNECTED` / `ACTION_BATTERY_CHANGED`） |
| 描画 | Compose `Canvas` + ハーモノグラフ曲線 |
| アニメーション | `Animatable` + `tween`（フェード） |

アーキテクチャは 2 つの `Activity` に分離している。設定 UI と点灯ロジックを分けることで、黒画面側を最小構成に保っている。

```
MainActivity (待機画面)      AwakeActivity (黒画面)
  ├ 設定トグル                   ├ 全面黒 + KEEP_SCREEN_ON
  ├ スタートボタン                 ├ Immersive Mode
  └ 終了理由トースト               ├ 電源監視 (任意)
                              └ Awake インジケータ (任意)
```

---

## 動作フロー

```mermaid
    A[アプリ起動] --> B
    B[MainActivity 待機画面] -->(Start)--> C
    C[AwakeActivity 黒画面] -->(ダブルタップ)--> B
    C -->(ホーム・他アプリ遷移/アプリ終了)--> B
    C -->(電源が外れた ※pluggedOnlyオプション有効時) --> D
    D[終了理由をトーストで表示] --> B
```

待機画面で 2 つのオプションを設定し、`Start` で黒画面へ遷移する。フラグは `Intent` の extra で受け渡す。

```kotlin
val intent = Intent(context, AwakeActivity::class.java)
    .putExtra(AwakeActivity.EXTRA_CHARGE_ONLY, chargeOnly)
    .putExtra(AwakeActivity.EXTRA_SHOW_AWAKE_ICON, showAwakeIcon)
launcher.launch(intent)
```

### 点灯のライフサイクル管理

画面を点けっぱなしにするフラグは、Activity が前面にある間だけ有効にし、離脱した瞬間に確実に解除する。
```kotlin
override fun onResume() { window.addFlags(FLAG_KEEP_SCREEN_ON) }   // 点灯維持を開始
override fun onPause()  { window.clearFlags(FLAG_KEEP_SCREEN_ON) } // 解除
override fun onStop()   { if (!isChangingConfigurations) finish() } // 完全離脱時は終了
```

`onStop` で `finish()` するのは、黒画面がバックグラウンドに残り「次回起動時に真っ暗なまま復帰する」事態を防ぐため。

### 電源接続オプション（任意）

「電源接続中のみスリープしない」が有効な場合、バッテリー駆動を検知して自動終了し、真っ暗な画面のままバッテリーを消耗する事象を防ぐ。


### Awake インジケータ（任意）

OLED では黒画面がスリープと見分けられないため、「起動中」を示す白い曲線アート（ハーモノグラフ）を重ねて表示する。**焼き付き防止**のため、一定周期でフェードアウト → 消灯中に位置・形をランダムに再生成 → フェードインを繰り返す。点灯ピクセルを抑えるため、線は 1 本のスカスカな軌跡としている。

```kotlin
while (true) {
    alpha.animateTo(1f, tween(ICON_FADE_MS))   
    delay(ICON_HOLD_MS)                         
    alpha.animateTo(0f, tween(ICON_FADE_MS))    
    seed = Random.nextLong()                     
}
```

---

## 設計方針

シンプル・堅牢・最小構成を最優先する。
