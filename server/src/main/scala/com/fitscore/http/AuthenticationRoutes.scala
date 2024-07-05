package com.fitscore.http

import cats.*
import cats.effect.*
import cats.syntax.all.*
import com.fitscore.core.*
import com.fitscore.domain.account.*
import io.circe.generic.auto.*
import org.http4s.*
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits.*
import org.http4s.server.Router
import cats.data.{NonEmptyChain, Validated}
import cats.data.Validated.*
import com.fitscore.errors.RegistrationRequestError
import com.fitscore.errors.RegistrationRequestError.*
import com.fitscore.utils.Date
import com.fitscore.validation.AccountValidator
import com.fitscore.core.AccountsRoles
import com.fitscore.domain.enums.AccessType
import com.fitscore.domain.enums.AccessType.*

import java.util.UUID


import com.fitscore.utils.EmailSender

class AuthenticationRoutes[F[_]: Concurrent] private (
                                                       accounts: Accounts[F],
                                                       accountsRoles: AccountsRoles[F] ,
                                                       verificationTokens: VerificationTokens[F]
                                                     ) extends Http4sDsl[F]:
  private val prefix = "/authentication"


  //TODO: move this to a new routes file/class related to new service class Authentication?
  //POST /accounts/register { registrationRequest }
//  private val registerAccountRoute: HttpRoutes[F] = HttpRoutes.of[F] {
//    case request @ POST -> Root / "register" =>
//      request.as[RegistrationRequest].flatMap( regReq =>
//        AccountValidator.register(regReq).fold(
//          //TODO: chained errors from the validation should be turned into strings with some functionality showErrors?
//          errors => BadRequest(s"${errors.toString}"),
//          account =>
//            for
//              emailNotExists <- accounts.notExistsEmail(account.email)
//              usernameNotExists <- accounts.notExistsUsername(account.username)
//              response <- (emailNotExists, usernameNotExists).mapN((_,_) => true).fold(
//                errors => BadRequest(s"${errors.toString}"),
//                _ =>
//                  accounts.create(account).flatMap( accountId =>
//                    for
//                      roleId   <- accountsRoles.spawn(User)
//                      _        <- accountsRoles.assign(accountId, roleId)
//                      r        <- Created(accountId)
//                    yield r
//                  )
//              )
//            yield response
//        )
//      )
//  }
//
//  //TODO: move this to a new routes file/class Authentication routes related to new core class Authentication
//  private val loginRoute: HttpRoutes[F] = HttpRoutes.of[F] {
//    case request@POST -> Root / "login" =>
//      request.as[LoginRequest].flatMap(logReq =>
//        for
//          email <- accounts.existsByEmail(logReq.email)
//          matchingPassword <- accounts.existsMatchingPassword(logReq.email,logReq.password)
//          response <- (email,matchingPassword).mapN((a, _) => a).fold(
//            errors => BadRequest(s"${errors.toString}"),
//            account =>
//              val sessionId = UUID.randomUUID().toString
//              Response(Status.Ok)
//                .withEntity(LoginResponse(account.username, sessionId))
//                .addCookie(ResponseCookie("sessionId", sessionId))
//                .pure[F]
//          )
//        yield response
//      )
//  }

//TODO: Ideas for verify and send verification token
//import org.http4s.HttpRoutes
//import org.http4s.dsl.io._
//import org.http4s.circe.CirceEntityCodec._
//
//case class SendVerificationRequest(userId: UUID, email: String, username: String)
//case class VerifyTokenRequest(token: String)
//
//class EmailVerificationRoutes(emailVerificationService: EmailVerificationService) {
//  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {
//    case req @ POST -> Root / "send-verification" =>
//      for {
//        sendRequest <- req.as[SendVerificationRequest]
//        _ <- emailVerificationService.sendVerificationEmail(sendRequest.userId, sendRequest.email, sendRequest.username)
//        resp <- Ok("Verification email sent")
//      } yield resp
//
//    case req @ POST -> Root / "verify-token" =>
//      for {
//        verifyRequest <- req.as[VerifyTokenRequest]
//        verified <- emailVerificationService.verifyToken(verifyRequest.token)
//        resp <- if (verified) Ok("Email verified") else BadRequest("Invalid or expired token")
//      } yield resp
//  }
//}

//TODO: Abstract Ideas from java for overall design of routes
//@RestController
//@CrossOrigin(origins = "http://localhost:4200")
//@RequestMapping("api/v1/authentication")
//public class AuthenticationController {
//
//  @Autowired
//  AccountService accountService;
//
//  @Autowired
//  MailService mailService;
//
//  @Autowired
//  AuthenticationManager authenticationManager;
//
//  @Autowired
//  JwtUtils jwtUtils;
//
//  @PostMapping("/register")
//  public ResponseEntity<?> registerAccount(@Valid @RequestBody RegistrationRequest registrationRequest) {
//    try {
//      Account account = accountService.registerNewAccount(registrationRequest);
//      final String token = UUID.randomUUID().toString();
//      accountService.createVerificationTokenForAccount(account, token);
//      mailService.sendVerificationToken(token, account);
//
//    } catch (AccountAlreadyExistAuthenticationException e) {
//      return new ResponseEntity<>(new ApiResponse(false, e.getMessage()), HttpStatus.BAD_REQUEST);
//    }
//
//    return ResponseEntity.ok().body(new ApiResponse(true, "User registered successfully"));
//  }
//
//  @PostMapping("/login")
//  public ResponseEntity<?> authenticateAccount(@Valid @RequestBody LoginRequest loginRequest) {
//
//    try{
//      Authentication authentication = authenticationManager.authenticate(
//        new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));
//
//      SecurityContextHolder.getContext().setAuthentication(authentication);
//      AccountDetails accountDetails = (AccountDetails) authentication.getPrincipal();
//
//      String jwt = jwtUtils.generateJwtToken(authentication);
//
//      AccountInfo accountInfo = new AccountInfo(
//        accountDetails.getId(),
//        accountDetails.getUsername(),
//        accountDetails.getEmail(),
//        accountDetails.getRoles()
//      );
//
//
//      return ResponseEntity.ok(new JwtResponse(jwt, accountInfo));
//
//    }catch (AuthenticationException ae){
//      return new ResponseEntity<>(new ApiResponse(false, "Login failed: " + ae.getMessage()), HttpStatus.BAD_REQUEST);
//    }
//
//  }
//  @PostMapping("/token/verify")
//  public ResponseEntity<?> confirmRegistration(@NotEmpty @RequestBody String token) {
//    System.out.println(token);
//    final String result = accountService.validateVerificationToken(token);
//    return ResponseEntity.ok().body(new ApiResponse(true, result));
//  }
//
//  @PostMapping("/token/resend")
//  @ResponseBody
//  public ResponseEntity<?> resendRegistrationToken(@NotEmpty @RequestBody String expiredToken) {
//    if (!accountService.resendVerificationToken(expiredToken)) {
//      return new ResponseEntity<>(new ApiResponse(false, "Token not found!"), HttpStatus.BAD_REQUEST);
//    }
//    return ResponseEntity.ok().body(new ApiResponse(true, "Successfully resent token"));
//  }
//}

//TODO: abstract ideas from Java for creating a token for account, resending token, validating token logic

//@Override
//public void createVerificationTokenForAccount(Account account, String token) {
//  final VerificationToken myToken = new VerificationToken(token, account);
//  verificationTokenRepository.save(myToken);
//}
//
//@Override
//@Transactional
//public boolean resendVerificationToken(String existingVerificationToken) {
//
//  Optional < VerificationToken > maybeToken = verificationTokenRepository.findByToken(existingVerificationToken);
//  if (maybeToken.isPresent()) {
//    VerificationToken verificationToken = maybeToken.get();
//
//    verificationToken.updateToken(UUID.randomUUID().toString());
//    verificationToken = verificationTokenRepository.save(verificationToken);
//    mailService.sendVerificationToken(verificationToken.getToken(), verificationToken.getAccount());
//    return true;
//  }
//  return false;
//}
//
//@Override
//public String validateVerificationToken(String token) {
//
//  Optional < VerificationToken > verificationToken = verificationTokenRepository.findByToken(token);
//
//  if (!verificationToken.isPresent()) {
//    return "INVALID";
//  }
//
//  final Account account = verificationToken.get().getAccount();
//  if (verificationToken.get().getExpiryDate().isBefore(LocalDateTime.now())) {
//    return "EXPIRED";
//  }
//
//  account.setEnabled(true);
//  account.setDateCreated(LocalDateTime.now());
//
//  verificationTokenRepository.delete(verificationToken.get());
//  accountRepository.save(account);
//
//  return "VALID";
//}