package com.getjenny.starchat.routing.auth

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 22/11/17.
  */

import scala.concurrent.Future
import com.getjenny.starchat.entities.{User, IndexDocumentResult, DeleteDocumentResult, UpdateDocumentResult, UserUpdate}

trait UserService {
  def create(user: User): Future[IndexDocumentResult]
  def update(id: String, user: UserUpdate): Future[UpdateDocumentResult]
  def delete(id: String): Future[DeleteDocumentResult]
  def read(id: String): Future[User]
  def genUser(id: String, user: UserUpdate, authenticator: StarChatAuthenticator): Future[User]
}