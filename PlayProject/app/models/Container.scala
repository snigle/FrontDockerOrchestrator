package models

case class Container(id : String, name : String, image : String, ports : Seq[Int], active : Boolean) {
  
}