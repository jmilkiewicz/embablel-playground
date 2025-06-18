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
import com.embabel.agent.api.annotation.fromForm
import com.embabel.agent.api.annotation.using
import com.embabel.agent.api.common.OperationContext
import com.embabel.agent.api.common.createObject
import com.embabel.agent.api.common.createObjectIfPossible
import com.embabel.agent.domain.io.UserInput
import com.embabel.agent.domain.library.HasContent
import com.embabel.common.ai.model.LlmOptions
import com.embabel.common.ai.model.ModelSelectionCriteria.Companion.Auto
import com.embabel.common.core.types.Timestamped
import com.fasterxml.jackson.annotation.JsonClassDescription
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import org.springframework.context.annotation.Profile
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


@JsonClassDescription("Name and Latitude and Longitude of a location")
data class Location(
    @get:JsonPropertyDescription("Latitude")
    val lat: Double,
    @get:JsonPropertyDescription("Longitude")
    val lon: Double,

    @get:JsonPropertyDescription("location name")
    val name: String
)

data class LocationName(
    val name: String
)

data class WeatherReport(
    val temperature: Int,
    val windPower: Int,
    val otherInfo: String,
) : HasContent, Timestamped {

    override val timestamp: Instant
        get() = Instant.now()

    override val content: String
        get() = """
            # Expected 
            ${otherInfo}

            # with max temperature of 
            ${temperature}
            
            # and wind power of 
            ${windPower}                       
        """.trimIndent()
}


data class OutFitSuggestion(
    val location: Location,
    val suggestion: String,
) : HasContent, Timestamped {

    override val timestamp: Instant
        get() = Instant.now()

    override val content: String
        get() = """
            # Outfit Suggestion for ${location.name}
            ${suggestion} 

        ${
            timestamp.atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy"))
        }
        """.trimIndent()
}


@Agent(
    description = "Help selecting the best outfit for a person visiting a location",
    name = "What to put on"
)
@Profile("!test")
class WhatToPutOnAgent(
) {
    @Action
    fun getLocation(userInput: UserInput): Location? =
        using(
            LlmOptions(criteria = Auto)
        ).createObjectIfPossible(
            """
             Create Location with lat and longitude from this user input including name extracted from user input 
             ${userInput.content}
        """.trimIndent()
        )

    @Action(cost = 100.0)
    fun askForLocation(): LocationName =
        fromForm("Where are you heading ?")


    @Action
    fun whereIs(locationName: LocationName): Location =
        using(
            LlmOptions(criteria = Auto)
        ).createObject(
            """
             I am heading to ${locationName.name}. What is a name, lat and longitude of ${locationName.name} ? 
        """.trimIndent()
        )

    @Action
    fun getWeather(location: Location): WeatherReport {
        println(location)

        return WeatherReport(
            temperature = 15,
            windPower = 1,
            otherInfo = "sunny but chilly"
        )
    }


    @AchievesGoal("suggestion what to put on when visiting a location")
    @Action
    fun suggestWhatToPut(
        location: Location,
        weatherReport: WeatherReport,
        context: OperationContext
    ): OutFitSuggestion {
        val suggestion = context.promptRunner(
            LlmOptions(criteria = Auto)
        ).generateText(
            """
            I am visiting ${location.name} today. 
            Suggest what to put on when visiting it, given the weather report.
            LIMIT to outfit suggestions only
                       
            # Weather report
            ${weatherReport.content}
            
        """.trimIndent()
        )
        return OutFitSuggestion(
            location = location,
            suggestion = suggestion,
        )
    }

}
