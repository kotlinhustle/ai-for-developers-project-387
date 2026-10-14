package com.example.backend.meetingtypes

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MeetingTypeRepository : JpaRepository<MeetingTypeEntity, UUID>
