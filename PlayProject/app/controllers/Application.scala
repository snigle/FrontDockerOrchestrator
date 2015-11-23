package controllers

import play.api.Play.current
import play.api.libs.ws._
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Application extends Controller {
  var cookie : String =""

  def index = Action.async { implicit request => {
     getLogin.onComplete(x => cookie = x.get )
     Future(Ok(cookie))
  }

  }



  def getLogin = WS.url("https://vcloud-director-http-2.ccr.eisti.fr/api/login").withHeaders("Accept" -> "application/*+xml;version=5.1").withAuth("user1@icc-02", "eisti0002", WSAuthScheme.BASIC).get().map{
      response => {
        response.cookies.mkString
      }
    }


  }









