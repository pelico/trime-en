// SPDX-FileCopyrightText: 2026 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.ime.candidates.unrolled.window

import android.view.Gravity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.Slide
import androidx.transition.Transition
import com.osfans.trime.ime.candidates.unrolled.UnrolledCandidateLayout
import com.osfans.trime.ime.candidates.unrolled.UnrolledCandidateViewAdapter
import com.osfans.trime.ime.window.BoardWindow
import org.kodein.di.DI

/** 按读音分组展示候选的展开面板：纵向排列各读音行，行内候选横向滑动。 */
class GroupedUnrolledCandidateWindow(di: DI) : BaseUnrolledCandidateWindow(di) {
    override fun exitAnimation(nextWindow: BoardWindow): Transition = Slide().apply {
        slideEdge = Gravity.TOP
    }

    override val adapter by lazy {
        UnrolledCandidateViewAdapter(scope, rime) { holder ->
            bindCandidateUiViewHolder(holder)
        }
    }

    override val layoutManager by lazy {
        LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
    }

    override fun onCreateCandidateLayout(): UnrolledCandidateLayout = UnrolledCandidateLayout(context, scope).apply {
        recyclerView.apply {
            adapter = this@GroupedUnrolledCandidateWindow.adapter
            layoutManager = this@GroupedUnrolledCandidateWindow.layoutManager
        }
    }
}
