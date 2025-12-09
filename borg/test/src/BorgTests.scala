package borg

import chisel3.{test => _, _}
import chisel3.simulator.EphemeralSimulator._
import utest._

object BorgTests extends TestSuite {
  val tests = Tests {
    test("borg") {
      println("bla test bla")
      simulate(new Borg) { borg =>
        // TODO
      }
    }
  }
}

