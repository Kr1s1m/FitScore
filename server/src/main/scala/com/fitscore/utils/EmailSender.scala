package com.fitscore.utils

import java.util.Properties
import javax.mail._
import javax.mail.internet._

class EmailSender(smtpHost: String, smtpPort: Int, username: String, password: String) {
  private val properties = new Properties()
  properties.put("mail.smtp.auth", "true")
  properties.put("mail.smtp.starttls.enable", "true")
  properties.put("mail.smtp.host", smtpHost)
  properties.put("mail.smtp.port", smtpPort.toString)

  private val session = Session.getInstance(properties, new Authenticator() {
    override protected def getPasswordAuthentication = new PasswordAuthentication(username, password)
  })

  def sendEmail(to: String, subject: String, content: String): Unit = {
    val message = new MimeMessage(session)
    message.setFrom(new InternetAddress(username))
    message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to).asInstanceOf[Array[Address]])
    message.setSubject(subject)
    message.setText(content)
    Transport.send(message)
  }
}

//TODO: Idea for freemaker template renderer...Maybe in its own file in utils package
//import freemarker.template.{Configuration, TemplateExceptionHandler}
//import java.io.StringWriter
//
//class EmailTemplateService {
//  private val cfg = new Configuration(Configuration.VERSION_2_3_31)
//  cfg.setClassForTemplateLoading(this.getClass, "/templates")
//  cfg.setDefaultEncoding("UTF-8")
//  cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER)
//
//  def renderTemplate(templateName: String, dataModel: Map[String, AnyRef]): String = {
//    val template = cfg.getTemplate(templateName)
//    val out = new StringWriter()
//    template.process(dataModel, out)
//    out.toString
//  }
//}