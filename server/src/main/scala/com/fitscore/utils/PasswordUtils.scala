package com.fitscore.utils

import org.mindrot.jbcrypt.BCrypt

object PasswordUtils:
  def hash(password: String): String = BCrypt.hashpw(password, BCrypt.gensalt())
  def check(password: String, hash: String): Boolean = BCrypt.checkpw(password, hash)
