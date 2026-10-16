/*
 * SPDX-FileCopyrightText: 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.ime.candidates.unrolled

import com.osfans.trime.core.CandidateProto
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

/**
 * Unit tests for the unrolled candidate panel's pinyin grouping:
 * [buildUnrolledDisplayItems] turns a candidate menu into display items with
 * per-reading headers while keeping every candidate's global index intact.
 */
class UnrolledDisplayItemsTest :
    BehaviorSpec({
        val c = { text: String, comment: String -> CandidateProto(text, comment, "") }

        Given("buildUnrolledDisplayItems") {
            When("candidates carry different readings") {
                Then("groups are alphabetical with headers; order within a group is preserved") {
                    val items =
                        buildUnrolledDisplayItems(
                            listOf(c("拼", "pin"), c("请", "qing"), c("品", "pin"), c("清", "qing")),
                            offset = 3,
                        )
                    items shouldBe
                        listOf(
                            UnrolledDisplayItem.Header("pin"),
                            UnrolledDisplayItem.Candidate(c("拼", "pin"), 3),
                            UnrolledDisplayItem.Candidate(c("品", "pin"), 5),
                            UnrolledDisplayItem.Header("qing"),
                            UnrolledDisplayItem.Candidate(c("请", "qing"), 4),
                            UnrolledDisplayItem.Candidate(c("清", "qing"), 6),
                        )
                }
            }

            When("some candidates have no reading (english/predict/custom)") {
                Then("they come first without a header, preserving their original order") {
                    val items =
                        buildUnrolledDisplayItems(
                            listOf(c("hello", ""), c("好", "hao"), c("hi", "")),
                            offset = 0,
                        )
                    items shouldBe
                        listOf(
                            UnrolledDisplayItem.Candidate(c("hello", ""), 0),
                            UnrolledDisplayItem.Candidate(c("hi", ""), 2),
                            UnrolledDisplayItem.Header("hao"),
                            UnrolledDisplayItem.Candidate(c("好", "hao"), 1),
                        )
                }
            }

            When("comments contain surrounding whitespace") {
                Then("they are trimmed for both grouping and header labels") {
                    val items =
                        buildUnrolledDisplayItems(
                            listOf(c("你", " ni"), c("拟", "ni ")),
                            offset = 0,
                        )
                    items shouldBe
                        listOf(
                            UnrolledDisplayItem.Header("ni"),
                            UnrolledDisplayItem.Candidate(c("你", " ni"), 0),
                            UnrolledDisplayItem.Candidate(c("拟", "ni "), 1),
                        )
                }
            }

            When("all candidates share one reading") {
                Then("exactly one header is emitted") {
                    val items =
                        buildUnrolledDisplayItems(
                            listOf(c("发", "fa"), c("法", "fa"), c("罚", "fa")),
                            offset = 10,
                        )
                    items shouldBe
                        listOf(
                            UnrolledDisplayItem.Header("fa"),
                            UnrolledDisplayItem.Candidate(c("发", "fa"), 10),
                            UnrolledDisplayItem.Candidate(c("法", "fa"), 11),
                            UnrolledDisplayItem.Candidate(c("罚", "fa"), 12),
                        )
                }
            }

            When("the menu is empty") {
                Then("the display list is empty") {
                    buildUnrolledDisplayItems(emptyList(), offset = 0) shouldBe emptyList()
                }
            }
        }
    })
