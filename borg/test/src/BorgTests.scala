package borg

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import utest._
import ujson._
import os._

object BorgTests extends TestSuite {

  def floatToBits(f: Float): BigInt = {
    BigInt(java.lang.Float.floatToRawIntBits(f)) & 0xffffffffL
  }

  def bitsToFloat(b: BigInt): Float = {
    java.lang.Float.intBitsToFloat(b.toInt)
  }

  def writeAddr(borg: Borg, addr: Int, bits: BigInt): Unit = {
    borg.io.address.poke(addr.U)
    borg.io.data_in.poke(bits.U)
    borg.io.data_write_n.poke("b10".U)
    borg.clock.step(1)
    borg.io.data_write_n.poke("b11".U)
    borg.clock.step(1)
  }

  def readAddr(borg: Borg, addr: Int): Float = {
    borg.io.address.poke(addr.U)
    val res = bitsToFloat(borg.io.data_out.peek().litValue)
    borg.clock.step(1)
    res
  }

  def runBasicMathTest(borg: Borg, a: Float, b: Float, epsilon: Float): Unit = {
    // 1. Write operands to RF(0) and RF(1)
    writeAddr(borg, 0, floatToBits(a))
    writeAddr(borg, 4, floatToBits(b))

    // 2. Program the chip: Set rs1=0 (bits 15-19) and rs2=1 (bits 20-24)
    // This tells the hardware: "Use RF(0) and RF(1) for the next math op"
    val instr = (1 << 20) | (0 << 15)
    writeAddr(borg, 60, BigInt(instr))

    // 3. Read results from the "Live" FPU addresses
    val addActual = readAddr(borg, 8)
    val mulActual = readAddr(borg, 12)

    val expectedSum = a + b
    val expectedMul = a * b

    // 4. Report results to console
    println(f"Check: $a%8.2f op $b%8.2f -> Add: $addActual%8.2f (Exp: $expectedSum%8.2f), Mul: $mulActual%8.2f (Exp: $expectedMul%8.2f)")

    utest.assert(math.abs(addActual - expectedSum) < epsilon)
    utest.assert(math.abs(mulActual - expectedMul) < epsilon)
  }

  val tests = Tests {
    utest.test("programmable_operand_batch_test") {
      // Load data from JSON
      val projectRoot = sys.env.get("PROJECT_ROOT").map(os.Path(_)).getOrElse(os.pwd)
      val jsonFile = projectRoot / "data" / "test_cases.json"
      val data = ujson.read(os.read(jsonFile))
      
      val epsilon = data("epsilon").num.toFloat
      val pairs = data("pairs").arr.map(p => (p(0).num.toFloat, p(1).num.toFloat))

      simulate(new Borg) { borg =>
        println("\n--- Starting Programmable Math Batch ---")
        pairs.foreach { case (a, b) =>
          runBasicMathTest(borg, a, b, epsilon)
        }
        println("--- All Tests Passed ---\n")
      }
    }
  }
}
