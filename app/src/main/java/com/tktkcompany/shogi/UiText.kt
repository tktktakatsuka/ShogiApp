package com.tktkcompany.shogi

import androidx.annotation.StringRes

/**
 * 画面に出す文言。文字列リソースの ID と埋め込む値を持ち、表示するときに端末の言語の文字列にする。
 */
class UiText(@StringRes val id: Int, vararg val args: Any)
