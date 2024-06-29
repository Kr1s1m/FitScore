package com.fitscore

import cats.effect.*
import com.comcast.ip4s.*
import com.fitscore.core.*
import com.fitscore.http.*
import doobie.util.ExecutionContexts
import doobie.hikari.HikariTransactor
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.CORS

object Application extends IOApp.Simple {
  def makePostgres = for {
    ec <- ExecutionContexts.fixedThreadPool[IO](32)
    transactor <- HikariTransactor.newHikariTransactor[IO](
      "org.postgresql.Driver",
      "jdbc:postgresql://localhost:5444/",
      "docker",
      "docker",
      ec
    )
  } yield transactor

  def makeServer = for {
    postgres <- makePostgres
    accounts <- AccountsLive.resource[IO](postgres)
    accountsApi   <- AccountRoutes.resource[IO](accounts)
    server <- EmberServerBuilder
      .default[IO]
      .withHost(host"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp(CORS(accountsApi.routes.orNotFound))
      .build
  } yield server

  override def run: IO[Unit] =
    makeServer.use(_ => IO.println("FitScore Server ready.") *> IO.never)
}
