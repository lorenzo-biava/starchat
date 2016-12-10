package com.getjenny.starchat.entities

/**
  * Created by Angelo Leto <angelo.leto@elegans.io> on 02/07/16.
  */

case class DeleteDocumentResult(index: String,
                                dtype: String,
                                id: String,
                                version: Long,
                                found: Boolean
                               )
