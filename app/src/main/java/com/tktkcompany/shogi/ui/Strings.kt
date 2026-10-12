package com.tktkcompany.shogi.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tktkcompany.shogi.R
import com.tktkcompany.shogi.UiText
import com.tktkcompany.shogi.game.CpuLevel
import com.tktkcompany.shogi.game.EndReason
import com.tktkcompany.shogi.game.Evaluation
import com.tktkcompany.shogi.game.GameResult
import com.tktkcompany.shogi.game.Player

/** 端末の言語の文字列にする。 */
@Composable
fun UiText.resolve(): String = stringResource(id, *args)

/** 「先手」「後手」。 */
@get:StringRes
val Player.label: Int get() = if (this == Player.SENTE) R.string.player_sente else R.string.player_gote

/** 駒台に表示するプレイヤー名。 */
@Composable
fun UiState.nameOf(player: Player): String = when {
    mode == GameMode.VS_HUMAN -> stringResource(player.label)
    isMe(player) -> stringResource(R.string.you)
    mode == GameMode.VS_CPU -> stringResource(R.string.cpu_with_level, stringResource(cpuLevel.label))
    else -> stringResource(R.string.opponent)
}

/** CPU の強さの名前(「入門」「中級」など)。 */
@get:StringRes
val CpuLevel.label: Int
    get() = when (this) {
        CpuLevel.INTRO -> R.string.cpu_level_INTRO
        CpuLevel.NOVICE -> R.string.cpu_level_NOVICE
        CpuLevel.INTERMEDIATE -> R.string.cpu_level_INTERMEDIATE
        CpuLevel.ADVANCED -> R.string.cpu_level_ADVANCED
        CpuLevel.EXPERT -> R.string.cpu_level_EXPERT
    }

/** CPU の強さの目安(「ふだん将棋を指す方に」など)。 */
@get:StringRes
val CpuLevel.description: Int
    get() = when (this) {
        CpuLevel.INTRO -> R.string.cpu_level_INTRO_desc
        CpuLevel.NOVICE -> R.string.cpu_level_NOVICE_desc
        CpuLevel.INTERMEDIATE -> R.string.cpu_level_INTERMEDIATE_desc
        CpuLevel.ADVANCED -> R.string.cpu_level_ADVANCED_desc
        CpuLevel.EXPERT -> R.string.cpu_level_EXPERT_desc
    }

/** 終局の理由と結果(例: "詰み。先手の勝ち")。 */
@Composable
fun GameResult.message(): String {
    if (winner == null || reason == EndReason.SENNICHITE) return stringResource(R.string.result_sennichite)
    val winnerName = stringResource(winner.label)
    return when (reason) {
        EndReason.RESIGN -> stringResource(R.string.result_resign, stringResource(winner.opponent.label), winnerName)
        else -> stringResource(R.string.result_checkmate, winnerName)
    }
}

/** 先手から見た形勢の説明(例: "+350 先手有利"、"後手の勝ち(7手詰)")。 */
@Composable
fun Evaluation.describe(): String = when (this) {
    is Evaluation.Mate -> {
        val winnerName = stringResource(winner.label)
        if (moves != null) stringResource(R.string.eval_mate_in, winnerName, moves)
        else stringResource(R.string.eval_mate, winnerName)
    }
    is Evaluation.Score -> {
        val side = stringResource(leader.label)
        val label = when (level) {
            Evaluation.Level.EVEN -> stringResource(R.string.eval_even)
            Evaluation.Level.ADVANTAGE -> stringResource(R.string.eval_advantage, side)
            Evaluation.Level.CLEAR_ADVANTAGE -> stringResource(R.string.eval_clear_advantage, side)
            Evaluation.Level.WINNING -> stringResource(R.string.eval_winning, side)
        }
        "${if (cp > 0) "+" else ""}$cp $label"
    }
}
