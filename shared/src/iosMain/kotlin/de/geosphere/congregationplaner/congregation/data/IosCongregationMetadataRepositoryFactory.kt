package de.geosphere.congregationplaner.congregation.data

import de.geosphere.congregationplaner.congregation.domain.repository.CongregationMetadataRepository

actual fun createCongregationMetadataRepository(): CongregationMetadataRepository =
    unavailableCongregationMetadataRepository("iOS")
