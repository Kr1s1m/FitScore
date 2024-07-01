package com.fitscore.errors

enum PostUpdateRequestError:
  case EmptyPostTitle
  case EmptyPostBody
  case PostCreatedTimeElapsed
  
  case PostResourceNotFound(rowsChanged: Int)
  