package homework4.generic

import cats.effect.IO
import cats.syntax.all.*
import homework4.generic.Concurrent.Callback

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

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
    def recover(pf: PartialFunction[Throwable, A]): F[A] = fa.recoverWith(pf.andThen(a => pure(a)))
    def recoverWith(pf: PartialFunction[Throwable, F[A]]): F[A] =
      fa.handleErrorWith: e =>
        pf.lift(e).getOrElse(raiseError(e))
    def redeemWith[B](recover: Throwable => F[B], f: A => F[B]): F[B] =
      val fSuccess = fa.flatMap(a => pure(Success(a): Try[A]))
      val fTry = fSuccess.handleErrorWith(e => pure(Failure(e)))

      fTry.flatMap:
        case Success(a) => f(a)
        case Failure(e) => recover(e)

    // similar to redeemWith, but using a Try
    def transformWith[B](f: Try[A] => F[B]): F[B] = redeemWith(e => f(Failure(e)), a => f(Success(a)))

  def parMap2[A, B, R](fa: F[A], fb: F[B])(f: (A, B) => R): F[R] = parProduct(fa, fb).map(f.tupled)

  extension [A](fas: List[F[A]])
    def parSequence: F[List[A]] =
      fas.foldRight(pure(List.empty[A]))((next, acc) => parMap2(next, acc)(_ :: _))

  extension [A](as: List[A])
    def parTraverse[B](f: A => F[B]): F[List[B]] =
      as.map(f).parSequence

  def fromFuture[A](fa: => Future[A]): F[A] = async((callback, ec) => fa.onComplete(callback)(ec))

  // define everything else you might need here...

  def fromTry[A](maybeValue: Try[A]): F[A] = maybeValue match
    case Success(a) => pure(a)
    case Failure(e) => raiseError(e)

object Concurrent:
  type Callback[-A] = Try[A] => Unit

  def apply[F[_]](using f: Concurrent[F]): Concurrent[F] = f

  given Concurrent[IO] with
    def pure[A](a: A): IO[A] = a.pure[IO]

    def raiseError[A](e: Throwable): IO[A] = e.raiseError

    def delay[A](a: => A): IO[A] = IO(a)

    def evalOn[A](a: => A, ec: ExecutionContext): IO[A] = IO(a).evalOn(ec)

    def parProduct[A, B](fa: IO[A], fb: IO[B]): IO[(A, B)] = (fa, fb).parTupled

    extension [A](fa: IO[A])
      def flatMap[B](f: A => IO[B]): IO[B] = fa.flatMap(f)
      def handleErrorWith(f: Throwable => IO[A]): IO[A] = fa.handleErrorWith(f)

    def async[A](initiateAction: (Callback[A], ExecutionContext) => Unit): IO[A] =
      IO.executionContext.flatMap: ec =>
        IO.async_ : callback =>
          initiateAction(result => callback(result.toEither), ec)

  given (using ec: ExecutionContext): Concurrent[Future] with
    def pure[A](a: A): Future[A] = Future.successful(a)

    def raiseError[A](e: Throwable): Future[A] = Future.failed(e)

    def delay[A](a: => A): Future[A] = Future(a)

    def evalOn[A](a: => A, ec: ExecutionContext): Future[A] = Future(a)(ec)

    def parProduct[A, B](fa: Future[A], fb: Future[B]): Future[(A, B)] = fa zip fb

    extension [A](fa: Future[A])
      def flatMap[B](f: A => Future[B]): Future[B] = fa.flatMap(f)
      def handleErrorWith(f: Throwable => Future[A]): Future[A] =
        fa.recoverWith:
          case e => f(e)

    def async[A](initiateAction: (Callback[A], ExecutionContext) => Unit): Future[A] =
      val promise = Promise[A]()

      initiateAction(promise.complete, ec)

      promise.future
