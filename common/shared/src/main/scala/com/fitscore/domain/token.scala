package com.fitscore.domain

import java.time.Instant
import java.util.UUID

object token:

  case class VerificationToken(
                                tokenId: UUID,
                                userId: UUID,
                                token: String,
                                createdAt: Instant,
                                expiresAt: Instant
                              )