package com.tktkcompany.shogi.engine

import com.tktkcompany.shogi.game.Analysis
import com.tktkcompany.shogi.game.CpuLevel
import com.tktkcompany.shogi.game.Position
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.io.Writer

/**
 * USI プロトコルで動く将棋エンジン(やねうら王)を別プロセスとして起動し、局面を検討させる。
 * 感想戦の検討([analyze])と CPU の指し手([think])の両方に使う。
 * エンジンは最初に使うときに起動し、[close] まで使い回す。
 */
class UsiEngine(private val executable: File, private val workDir: File) : Closeable {
    private val mutex = Mutex()

    /** エンジンへの書き込みと [searching] を守るロック。 */
    private val lock = Any()
    private var process: Process? = null
    private var reader: BufferedReader? = null
    private var writer: Writer? = null
    private var searching = false

    /** エンジンの実行ファイルがアプリに入っているか。 */
    val isAvailable: Boolean get() = executable.exists()

    /**
     * [position] を最大 [millis] ミリ秒検討する。読みが深まるたびに [onUpdate] を(別スレッドから)呼ぶ。
     * 途中でやめるときは、コルーチンをキャンセルしてから [stop] を呼ぶ。
     * @throws IOException エンジンを起動できない、または異常終了した
     */
    suspend fun analyze(position: Position, millis: Long, onUpdate: (Analysis) -> Unit) {
        search(position, goCommand = "go infinite", multiPv = 1, timeLimitMillis = millis) { line ->
            Analysis.parseInfo(line)?.let(onUpdate)
        }
    }

    /**
     * CPU の指し手を考える。[level] の読む量で探索し、候補手(MultiPV の順)を返す。
     * 指す手は [CpuLevel.pick] で選ぶ。
     * @throws IOException エンジンを起動できない、または異常終了した
     */
    suspend fun think(position: Position, level: CpuLevel): List<Analysis> {
        val latest = sortedMapOf<Int, Analysis>()
        search(position, goCommand = level.goCommand, multiPv = level.multiPv, timeLimitMillis = null) { line ->
            val analysis = Analysis.parseInfo(line) ?: return@search
            latest[multiPvIndex(line)] = analysis
        }
        return latest.values.toList()
    }

    /** "info ... multipv 2 ..." の候補手の番号(なければ 1)。 */
    private fun multiPvIndex(line: String): Int {
        val tokens = line.split(' ')
        val i = tokens.indexOf("multipv")
        return if (i >= 0) tokens.getOrNull(i + 1)?.toIntOrNull() ?: 1 else 1
    }

    /**
     * 探索を1回行い、bestmove が返るまでエンジンの出力を1行ずつ [onLine] に渡す。
     * [timeLimitMillis] を過ぎたら打ち切らせる(null なら go コマンドの条件で終わるのを待つ)。
     */
    private suspend fun search(
        position: Position,
        goCommand: String,
        multiPv: Int,
        timeLimitMillis: Long?,
        onLine: (String) -> Unit,
    ) {
        val job = currentCoroutineContext().job
        mutex.withLock {
            // 探索中のエンジンを放置しないよう、始めたら bestmove が返るまで必ず読み切る
            withContext(Dispatchers.IO + NonCancellable) {
                try {
                    if (process == null) start()
                    synchronized(lock) {
                        if (!job.isActive) return@withContext
                        send("setoption name MultiPV value $multiPv")
                        send("position sfen ${position.toSfen()} 1")
                        send(goCommand)
                        searching = true
                    }
                    val timer = timeLimitMillis?.let { millis ->
                        launch {
                            delay(millis)
                            stop()
                        }
                    }
                    try {
                        while (true) {
                            val line = readLine()
                            if (line.startsWith("bestmove")) break
                            onLine(line)
                        }
                    } finally {
                        synchronized(lock) { searching = false }
                        timer?.cancel()
                    }
                } catch (e: IOException) {
                    shutdown()
                    throw e
                }
            }
        }
    }

    /** 検討中なら打ち切らせる。 */
    fun stop() {
        synchronized(lock) {
            if (!searching) return
            try {
                send("stop")
            } catch (e: IOException) {
                // エンジンが落ちている。読み取り側が検知して後始末する
            }
        }
    }

    private fun start() {
        val p = ProcessBuilder(executable.absolutePath)
            .directory(workDir)
            .redirectErrorStream(true)
            .start()
        process = p
        reader = p.inputStream.bufferedReader()
        writer = p.outputStream.writer()
        send("usi")
        waitFor("usiok")
        val threads = (Runtime.getRuntime().availableProcessors() / 2).coerceIn(1, 4)
        val options = mapOf(
            "Threads" to threads,
            "USI_Hash" to 128,
            "BookFile" to "no_book",
            // 水匠5は評価値のスケールが 24 のときに最も強い
            "FV_SCALE" to 24,
            // 検討向けの出力(読み筋を省略せず、仮の評価値を出さない)
            "ConsiderationMode" to true,
            "PvInterval" to 200,
        )
        for ((name, value) in options) send("setoption name $name value $value")
        send("isready")
        waitFor("readyok")
    }

    private fun waitFor(expected: String) {
        while (readLine() != expected) Unit
    }

    private fun readLine(): String =
        reader?.readLine()?.trim() ?: throw IOException("エンジンが終了しました")

    private fun send(command: String) {
        synchronized(lock) {
            val w = writer ?: throw IOException("エンジンが起動していません")
            w.write(command + "\n")
            w.flush()
        }
    }

    private fun shutdown() {
        synchronized(lock) {
            searching = false
            try {
                writer?.apply { write("quit\n"); flush() }
            } catch (e: IOException) {
                // すでに終了している
            }
            process?.destroy()
            process = null
            reader = null
            writer = null
        }
    }

    override fun close() = shutdown()
}
