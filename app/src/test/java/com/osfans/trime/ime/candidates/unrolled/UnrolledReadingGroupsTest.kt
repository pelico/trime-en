/*
 * SPDX-FileCopyrightText: 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.ime.candidates.unrolled

import com.osfans.trime.core.CandidateProto
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

/**
 * Unit tests for the unrolled candidate panel's reading groups:
 * [buildUnrolledReadingGroups] groups candidates by pinyin reading, keeps each
 * candidate's global index intact, and orders groups by each reading's best
 * (earliest) raw candidate rather than alphabetically.
 */
class UnrolledReadingGroupsTest :
    BehaviorSpec({
        val c = { text: String, comment: String -> CandidateProto(text, comment, "") }

        Given("buildUnrolledReadingGroups") {
            When("readings interleave in the raw menu") {
                Then("groups follow each reading's first occurrence, keeping raw order inside") {
                    buildUnrolledReadingGroups(
                        listOf(c("快", "kuai"), c("菊", "ju"), c("块", "kuai"), c("局", "ju")),
                        offset = 0,
                    ) shouldBe
                        listOf(
                            UnrolledReadingGroup(
                                "kuai",
                                listOf(c("快", "kuai") to 0, c("块", "kuai") to 2),
                            ),
                            UnrolledReadingGroup(
                                "ju",
                                listOf(c("菊", "ju") to 1, c("局", "ju") to 3),
                            ),
                        )
                }
            }

            When("the best candidate of a later alphabetical reading ranks first") {
                Then("kuai leads over ju even though ju sorts earlier") {
                    val groups =
                        buildUnrolledReadingGroups(
                            listOf(c("菊", "ju"), c("居", "ju"), c("快", "kuai")),
                            offset = 7,
                        )
                    groups.map { it.label } shouldBe listOf("ju", "kuai")
                    groups[1].candidates shouldBe listOf(c("快", "kuai") to 9)
                }
            }

            When("some candidates have no reading (english/predict/custom)") {
                Then("they form a headerless first group, preserving their original order") {
                    buildUnrolledReadingGroups(
                        listOf(c("hello", ""), c("好", "hao"), c("hi", "")),
                        offset = 0,
                    ) shouldBe
                        listOf(
                            UnrolledReadingGroup(null, listOf(c("hello", "") to 0, c("hi", "") to 2)),
                            UnrolledReadingGroup("hao", listOf(c("好", "hao") to 1)),
                        )
                }
            }

            When("comments contain surrounding whitespace") {
                Then("they are trimmed for both grouping and header labels") {
                    buildUnrolledReadingGroups(
                        listOf(c("你", " ni"), c("拟", "ni ")),
                        offset = 0,
                    ) shouldBe
                        listOf(
                            UnrolledReadingGroup("ni", listOf(c("你", " ni") to 0, c("拟", "ni ") to 1)),
                        )
                }
            }

            When("the menu is empty") {
                Then("the group list is empty") {
                    buildUnrolledReadingGroups(emptyList(), offset = 0) shouldBe emptyList()
                }
            }
        }
    })
