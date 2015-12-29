package models

case class Container(id : String, name : String, image : String, ports : Map[Int,Int], active : Boolean) {
  
}