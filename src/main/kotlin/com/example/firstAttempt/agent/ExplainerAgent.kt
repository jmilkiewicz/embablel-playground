/*
 * Copyright 2024-2025 Embabel Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.firstAttempt.agent

import com.embabel.agent.api.annotation.AchievesGoal
import com.embabel.agent.api.annotation.Action
import com.embabel.agent.api.annotation.Agent
import com.embabel.agent.api.annotation.using
import com.embabel.agent.api.common.OperationContext
import com.embabel.agent.api.common.create
import com.embabel.agent.domain.io.UserInput
import com.embabel.agent.domain.library.HasContent
import com.embabel.agent.prompt.persona.Persona
import com.embabel.common.ai.model.LlmOptions
import com.embabel.common.ai.model.ModelSelectionCriteria.Companion.Auto
import com.embabel.common.core.types.Timestamped
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

val Explainer = Persona(
    name = "Albert Ein",
    persona = "An expert in the field of universe",
    voice = "Professional but helpful",
    objective = "Explain concept in a straight-forward and amusing way.",
)

val Critiquer = Persona(
    name = "Mr Satan",
    persona = "Pure devil",
    voice = "Nasty but intriguing",
    objective = "Always critiques and nitpicking",
)

data class Explanation(
    val text: String,
)

data class ExplanationReviewed(
    val explanation: Explanation,
    val review: String,
    val critiquer: Persona,
) : HasContent, Timestamped {

    override val timestamp: Instant
        get() = Instant.now()

    override val content: String
        get() = """
            # Explanation
            ${explanation.text}

            # Review
            $review

            # Critiquer
            ${critiquer.name}, ${
            timestamp.atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy"))
        }
        """.trimIndent()
}

@Agent(
    description = "Explains a complicated concept to a user but have the explanation critiqued by 2nd eyes",
)
@Profile("!test")
class ExplainerAgent(
    @Value("\${storyWordCount:100}") private val storyWordCount: Int,
    @Value("\${reviewWordCount:100}") private val reviewWordCount: Int,
) {

    @Action
    fun explainConcept(userInput: UserInput): Explanation =
        using(
            LlmOptions(criteria = Auto)
                .withTemperature(.9), // Higher temperature for more creative output
        ).withPromptContributor(Explainer)
            .create(
                """
            Explain the concept of ${userInput.content} in $storyWordCount words or less.
            The explanation shall be engaging and imaginative.                        
            
        """.trimIndent()
            )

    @AchievesGoal("The creative but informative explanation of a concept")
    @Action
    fun reviewExplanation(userInput: UserInput, explanation: Explanation, context: OperationContext): ExplanationReviewed {
        val review = context.promptRunner(
            LlmOptions(criteria = Auto)
        ).withPromptContributor(Critiquer)
            .generateText(
                """
            You will be given a short explanation of ${userInput.content} to review.
            Review it in $reviewWordCount words or less.
            Consider whether or not the explanation is engaging, imaginative, and well-written.
            Also consider whether the explanation is appropriate given the original user input.

            # Explanation
            ${explanation.text}
            
        """.trimIndent()
            )
        return ExplanationReviewed(
            explanation = explanation,
            review = review,
            critiquer = Critiquer,
        )
    }

}
