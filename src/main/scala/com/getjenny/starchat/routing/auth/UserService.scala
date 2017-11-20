package com.getjenny.starchat.routing.auth

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 19/11/17.
  */

import com.getjenny.starchat.entities.{DeleteDocumentResult, IndexDocumentResult, UpdateDocumentResult, User}

import scala.concurrent.Future

trait UserService {
  def create_user(user: User): Future[IndexDocumentResult]
  def update_user(user: User): Future[UpdateDocumentResult]
  def delete_user(id: String): Future[DeleteDocumentResult]
  def get_user(id: String): Future[User]
  def generate_salt(): Future[String]
}