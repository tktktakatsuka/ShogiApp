package com.tktkcompany.shogi.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * オンライン対戦の招待リンク。
 *
 * 招待リンクは GitHub Pages の docs/join.html(https://…/join.html?room=123456)を開く。
 * そのページの「アプリで開く」から shogiapp://join?room=123456 でアプリが起動し、部屋に参加する。
 * アプリが入っていなければ Google Play のページが開く。
 */
object Invite {
    private const val PAGE = "https://tktktakatsuka.github.io/ShogiApp/join.html"
    private const val LINE_PACKAGE = "jp.naver.line.android"

    /** 招待リンク。LINE の中で開いたときは、アプリを起動できるよう外部のブラウザで開かせる。 */
    fun link(code: String): String = "$PAGE?room=$code&openExternalBrowser=1"

    /** アプリを起動したリンクが招待リンクなら、その部屋番号。 */
    fun roomCode(uri: Uri?): String? {
        if (uri == null || uri.scheme != "shogiapp" || uri.host != "join") return null
        return uri.getQueryParameter("room")?.takeIf { it.length == 6 && it.all(Char::isDigit) }
    }

    /** LINE で送る。LINE が入っていなければ、ほかのアプリを選ぶ画面にする。 */
    fun sendWithLine(context: Context, message: String) {
        val intent = textIntent(message).setPackage(LINE_PACKAGE)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            sendWithChooser(context, message)
        }
    }

    fun sendWithChooser(context: Context, message: String) {
        context.startActivity(Intent.createChooser(textIntent(message), null))
    }

    private fun textIntent(message: String) = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, message)
}
