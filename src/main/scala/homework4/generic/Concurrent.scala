package homework4.generic

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import homework4.generic.Concurrent.Callback

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}
import cats.syntax.all.*

trait Concurrent[F[_]]:
  // Base operations:
  def pure[A](a: A): F[A]
  def raiseError[A](e: Throwable): F[A]

  def delay[A](a: => A): F[A]
  def evalOn[A](a: => A, ec: ExecutionContext): F[A]

  def parProduct[A, B](fa: F[A], fb: F[B]): F[(A, B)]

  extension [A](fa: F[A])
    def flatMap[B](f: A => F[B]): F[B]
    def handleErrorWith(f: Throwable => F[A]): F[A]

    def >>[B](next: F[B]): F[B] = fa.flatMap(_ => next)
    def >>=[B](f: A => F[B]): F[B] = fa.flatMap(f)

  def async[A](initiateAction: (Callback[A], ExecutionContext) => Unit): F[A]

  // Derived operations:
  extension [A](fa: F[A])
    def map[B](f: A => B): F[B] = fa.flatMap(a => pure(f(a)))
    def recover(pf: PartialFunction[Throwable, A]): F[A] = ???
    def recoverWith(pf: PartialFunction[Throwable, F[A]]): F[A] = ???
    def redeemWith[B](recover: Throwable => F[B], f: A => F[B]): F[B] = ???

    // similar to redeemWith, but using a Try
    def transformWith[B](f: Try[A] => F[B]): F[B] = redeemWith(e => f(Failure(e)), a => f(Success(a)))

  def parMap2[A, B, R](fa: F[A], fb: F[B])(f: (A, B) => R): F[R] = parProduct(fa, fb).map(f.tupled)

  extension [A](fas: List[F[A]]) def parSequence: F[List[A]] = ???

  def fromFuture[A](fa: => Future[A]): F[A] = ???

  // define everything else you might need here...

object Concurrent:
  type Callback[-A] = Try[A] => Unit

  def apply[F[_]](using f: Concurrent[F]): Concurrent[F] = f

  given Concurrent[IO] with
    def pure[A](a: A): IO[A] = ???

    def raiseError[A](e: Throwable): IO[A] = ???

    def delay[A](a: => A): IO[A] = ???

    def evalOn[A](a: => A, ec: ExecutionContext): IO[A] = ???

    def parProduct[A, B](fa: IO[A], fb: IO[B]): IO[(A, B)] = ???

    extension [A](fa: IO[A])
      def flatMap[B](f: A => IO[B]): IO[B] = ???
      def handleErrorWith(f: Throwable => IO[A]): IO[A] = ???

    def async[A](initiateAction: (Callback[A], ExecutionContext) => Unit): IO[A] =
      IO.executionContext.flatMap: ec =>
        IO.async_ : callback =>
          initiateAction(result => callback(result.toEither), ec)

  given (using ec: ExecutionContext): Concurrent[Future] with
    def pure[A](a: A): Future[A] = ???

    def raiseError[A](e: Throwable): Future[A] = ???

    def delay[A](a: => A): Future[A] = ???

    def evalOn[A](a: => A, ec: ExecutionContext): Future[A] = ???

    def parProduct[A, B](fa: Future[A], fb: Future[B]): Future[(A, B)] = ???

    extension [A](fa: Future[A])
      def flatMap[B](f: A => Future[B]): Future[B] = ???
      def handleErrorWith(f: Throwable => Future[A]): Future[A] = ???

    def async[A](initiateAction: (Callback[A], ExecutionContext) => Unit): Future[A] =
      val promise = Promise[A]()

      initiateAction(promise.complete, ec)

      promise.future
