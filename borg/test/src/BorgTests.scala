package borg

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import utest._
import upickle.default._
import os._

object BorgTests extends TestSuite {

  def floatToBits(f: Float): BigInt = {
    BigInt(java.lang.Float.floatToRawIntBits(f)) & 0xffffffffL
  }

  def bitsToFloat(b: BigInt): Float = {
    java.lang.Float.intBitsToFloat(b.toInt)
  }

  class BorgDriver(borg: Borg) {
    def writeReg(regIdx: Int, value: Float): Unit = {
      borg.io.address.poke((regIdx * 4).U)
      borg.io.data_in.poke(floatToBits(value).U)
      borg.io.data_write_n.poke("b10".U) // Assert Write Enable
      borg.clock.step(1)
      borg.io.data_write_n.poke("b11".U) // De-assert
      borg.clock.step(1)
    }

    def readAddr(addr: Int): Float = {
      borg.io.address.poke(addr.U)
      borg.io.data_read_n.poke("b10".U) // Assert Read Enable
      // Peek the data currently on the bus
      val result = bitsToFloat(borg.io.data_out.peek().litValue)
      borg.clock.step(1)
      borg.io.data_read_n.poke("b11".U)
      result
    }

    def reset(): Unit = {
      borg.io.data_write_n.poke("b11".U)
      borg.io.data_read_n.poke("b11".U)
      borg.clock.step(1)
    }
  }

  val tests = Tests {
    utest.test("borg_vulkan_style_math_batch") {
      
      val projectRoot = sys.env.get("PROJECT_ROOT") match {
        case Some(path) => os.Path(path)
        case None       => os.pwd 
      }
      
      val jsonFile = projectRoot / "data" / "test_cases.json"
      if (!os.exists(jsonFile)) {
        throw new Exception(s"Shared test vectors not found at: $jsonFile")
      }

      val data = ujson.read(os.read(jsonFile))
      val epsilon = data("epsilon").num.toFloat
      
      val testPairs = data("pairs").arr.map { pair =>
        (pair(0).num.toFloat, pair(1).num.toFloat)
      }.toSeq

      // 2. Run the Hardware Simulation
      simulate(new Borg) { borg =>
        val driver = new BorgDriver(borg)
        driver.reset()

        testPairs.foreach { case (valA, valB) =>
          runBasicMathTest(driver, valA, valB, epsilon)
        }
      }
    }
  }

  def runBasicMathTest(driver: BorgDriver, a: Float, b: Float, epsilon: Float): Unit = {
    // Phase 1: Upload Data (Like Descriptor Set updates)
    driver.writeReg(0, a) // ADDR 0
    driver.writeReg(1, b) // ADDR 4

    // Phase 2: Read Results (Calculated in the background by HardFloat)
    // ADDR 8 is Addition, ADDR 12 is Multiplication
    val addActual = driver.readAddr(8)
    val mulActual = driver.readAddr(12)

    val expectedSum: Float = a + b
    val expectedMul: Float = a * b

    println(s"Checking: $a & $b -> Add: $addActual (Exp: $expectedSum), Mul: $mulActual (Exp: $expectedMul)")

    utest.assert(math.abs(addActual - expectedSum) < epsilon)
    utest.assert(math.abs(mulActual - expectedMul) < epsilon)
  }
}
