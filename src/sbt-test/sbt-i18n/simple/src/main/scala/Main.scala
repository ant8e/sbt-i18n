package simple

/**
  * A simple class and objects to write tests against.
  */
class Main {
  val default = "the function returned"
  def method = default + " " + Main.function
}

object Main {

  val constant = 1
  def function = 2*constant
//  val b = org.example.i18n.Bundle

  def main(args: Array[String]): Unit = {
    println(new Main().default)
  }
}
