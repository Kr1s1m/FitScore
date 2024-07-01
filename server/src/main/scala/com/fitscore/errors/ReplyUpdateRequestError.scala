package com.fitscore.errors

enum ReplyUpdateRequestError:
  case EmptyReplyBody
  case ReplyResourceNotFound(rowsChanged: Int)