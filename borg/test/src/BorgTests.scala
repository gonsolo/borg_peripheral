package borg

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import utest._
import upickle.default._
import os._

object BorgTests extends TestSuite {

  /** * Helper to convert Scala Float (32-bit) to BigInt bit pattern (IEEE 754)
   */
  def floatToBits(f: Float): BigInt = {
    BigInt(java.lang.Float.floatToRawIntBits(f)) & 0xffffffffL
  }

  /** * Helper to convert bit pattern back to Scala Float (32-bit)
   */
  def bitsToFloat(b: BigInt): Float = {
    java.lang.Float.intBitsToFloat(b.toInt)
  }

  val tests = Tests {
    utest.test("borg_float_addition_multiplication_batch") {
      
      // 1. Resolve the project root path.
      val projectRoot = sys.env.get("PROJECT_ROOT") match {
        case Some(path) => os.Path(path)
        case None       => os.pwd 
      }
      
      val jsonFile = projectRoot / "data" / "test_cases.json"
      
      // 2. Load and parse the shared JSON data
      if (!os.exists(jsonFile)) {
        throw new Exception(s"Shared test vectors not found at: $jsonFile")
      }

      val data = ujson.read(os.read(jsonFile))
      val epsilon = data("epsilon").num.toFloat
      
      // Map JSON numbers explicitly to Scala Floats (32-bit)
      val testPairs = data("pairs").arr.map { pair =>
        (pair(0).num.toFloat, pair(1).num.toFloat)
      }.toSeq

      // 3. Run the Hardware Simulation
      simulate(new Borg) { borg =>
        val ADDR_A   = 0
        val ADDR_B   = 4
        val ADDR_ADD = 8
        val ADDR_MUL = 12

        // Initialize State
        borg.io.data_write_n.poke("b11".U)
        borg.io.data_read_n.poke("b11".U)
        borg.clock.step(1)

        testPairs.foreach { case (valA, valB) =>
          val a32: Float = valA
          val b32: Float = valB

          // Step A: Write Operand A
          borg.io.address.poke(ADDR_A.U)
          borg.io.data_in.poke(floatToBits(a32).U)
          borg.io.data_write_n.poke("b10".U) 
          borg.clock.step(1)

          // Step B: Write Operand B
          borg.io.address.poke(ADDR_B.U)
          borg.io.data_in.poke(floatToBits(b32).U)
          borg.clock.step(1)

          // Step C: De-assert write
          borg.io.data_write_n.poke("b11".U)
          borg.clock.step(1)

          // Step D: Verify Addition Result
          borg.io.address.poke(ADDR_ADD.U)
          borg.io.data_read_n.poke("b10".U) 
          
          val addActual = bitsToFloat(borg.io.data_out.peek().litValue)
          val expectedSum: Float = a32 + b32
          
          println(s"Checking Add: $a32 + $b32 = $addActual (Expected: $expectedSum)")
          // Using explicit utest.assert to avoid Chisel collision
          utest.assert(math.abs(addActual - expectedSum) < epsilon)

          // Step E: Verify Multiplication Result
          borg.io.address.poke(ADDR_MUL.U)
          
          val mulActual = bitsToFloat(borg.io.data_out.peek().litValue)
          val expectedMul: Float = a32 * b32

          println(s"Checking Mul: $a32 * $b32 = $mulActual (Expected: $expectedMul)")
          // Using explicit utest.assert to avoid Chisel collision
          utest.assert(math.abs(mulActual - expectedMul) < epsilon)

          borg.clock.step(1)
        }
      }
    }
  }
}
