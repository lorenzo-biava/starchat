package com.getjenny.starchat.routing.auth

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 19/11/17.
  */

import com.getjenny.starchat.entities._

import scala.concurrent.Future

trait UserService {
  def create(user: User): Future[IndexDocumentResult]
  def update(id: String, user: UserUpdate): Future[UpdateDocumentResult]
  def delete(id: String): Future[DeleteDocumentResult]
  def read(id: String): Future[User]
  def genUser(id: String, user: UserUpdate, authenticator: StarchatAuthenticator): Future[User]
}