package models

import scala.xml.Elem
import play.api.libs.json.JsValue

case class Task(id : String, status : String, message : String = "") {
  
	
}

object TaskFactory {

  def apply(xml : Elem) : Task = {
    xml.head.label match {
      case "Error" => Task("undefined","error",xml.head.attribute("message").get.text)
      case "Task" => Task(xml.head.attribute("id").get.text.split(":").last,
        xml.head.attribute("status").get.text,
	     (xml \ "Error").map(node => node.attribute("message").get.text).mkString("\n")
	      )
      case _ => Task("undefined","error","unknow error")
    }
    
  }
}