package de.geosphere.congregationplaner.talk.presentation.directory

import de.geosphere.congregationplaner.talk.domain.model.ExternalCircuit
import de.geosphere.congregationplaner.talk.domain.model.ExternalCongregation
import de.geosphere.congregationplaner.talk.domain.model.ExternalDirectory
import de.geosphere.congregationplaner.talk.domain.model.VisitingSpeaker

/** Beispieldaten für die Previews des Verzeichnisses. */
internal val previewSpeaker = VisitingSpeaker(
    id = "s1",
    congregationId = "g1",
    lastName = "Beispiel",
    firstName = "Tim",
    mobilePhone = "0170 1234",
    landlinePhone = "08092 1234",
    email = "tim@example.org",
    talkNumbers = listOf(12, 45),
    ranking = 3,
)

internal val previewDirectory = ExternalDirectory(
    circuits = listOf(ExternalCircuit("c1", "Kreis Nord"), ExternalCircuit("c2", "Kreis Süd")),
    congregations = listOf(ExternalCongregation("g1", "c1", "Musterstadt"), ExternalCongregation("g2", "c2", "Zell")),
    speakers = listOf(
        previewSpeaker,
        VisitingSpeaker(id = "s2", congregationId = "g2", lastName = "Muster", firstName = "Eva"),
    ),
)

internal val previewDirectoryState = ExternalDirectoryUiState(
    directory = previewDirectory,
    editor = ExternalDirectoryEditor.Speaker(previewSpeaker, "12, 45", previewSpeaker),
    isLoading = false,
    canView = true,
    canPlan = true,
)
