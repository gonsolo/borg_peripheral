package borg

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import utest._

object BorgTests extends TestSuite {

  def floatToBits(f: Float): BigInt = {
    BigInt(java.lang.Float.floatToRawIntBits(f)) & 0xffffffffL
  }

  def bitsToFloat(b: BigInt): Float = {
    java.lang.Float.intBitsToFloat(b.toInt)
  }

  val tests = Tests {
    utest.test("borg_float_addition_multiplication_batch") {
      simulate(new Borg) { borg =>
        val ADDR_A = 0
        val ADDR_B = 4
        val ADDR_ADD = 8
        val ADDR_MUL = 12
        val EPSILON = 1e-6f

        // Define the same test pairs as the cocotb test
        val testPairs = Seq(
          (10.0f, 20.0f),
          (0.1f, 0.2f),
          (-5.5f, 2.25f),
          (100.0f, 0.0f),
          (0.0123f, 0.0456f) // 1.23e-2, 4.56e-2
        )

        // Initialize state
        borg.io.data_write_n.poke("b11".U)
        borg.io.data_read_n.poke("b11".U)
        borg.clock.step(1)

        testPairs.foreach { case (valA, valB) =>
          // 1. Write Operand A
          borg.io.address.poke(ADDR_A.U)
          borg.io.data_in.poke(floatToBits(valA).U)
          borg.io.data_write_n.poke("b10".U)
          borg.clock.step(1)

          // 2. Write Operand B
          borg.io.address.poke(ADDR_B.U)
          borg.io.data_in.poke(floatToBits(valB).U)
          borg.clock.step(1)

          // 3. De-assert write and let logic settle
          borg.io.data_write_n.poke("b11".U)
          borg.clock.step(1)

          // 4. Check Addition
          borg.io.address.poke(ADDR_ADD.U)
          borg.io.data_read_n.poke("b10".U)

          val addActual = bitsToFloat(borg.io.data_out.peek().litValue)
          val expectedSum = valA + valB

          println(s"Testing Add: $valA + $valB = $addActual")
          utest.assert(math.abs(addActual - expectedSum) < EPSILON)

          // 5. Check Multiplication
          borg.io.address.poke(ADDR_MUL.U)

          val mulActual = bitsToFloat(borg.io.data_out.peek().litValue)
          val expectedMul = valA * valB

          println(s"Testing Mul: $valA * $valB = $mulActual")
          utest.assert(math.abs(mulActual - expectedMul) < EPSILON)

          borg.clock.step(1)
        }
      }
    }
  }
}
