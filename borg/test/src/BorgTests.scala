// SPDX-FileCopyrightText: © 2025-2026 Andreas Wendleder
// SPDX-License-Identifier: CERN-OHL-S-2.0

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
    // 1. Load operands into registers 0 and 1
    writeAddr(borg, 0, floatToBits(a))
    writeAddr(borg, 4, floatToBits(b))

    // 2. Setup Addition Instruction
    // rs1 = reg0, rs2 = reg1, funct7 = 0x00 (Add)
    val add_instr = (0x00 << 25) | (1 << 20) | (0 << 15)
    writeAddr(borg, 60, BigInt(add_instr))

    // 3. Wait for Hardware Pipeline
    while (!borg.io.data_ready.peek().litToBoolean) {
      borg.clock.step(1)
    }
    
    // 4. Read result from math_result register (addr 8)
    val addActual = readAddr(borg, 8)
    val expectedSum = a + b

    // 5. Report results to console
    println(
      f"Check: $a%8.2f + $b%8.2f -> Actual: $addActual%8.2f (Exp: $expectedSum%8.2f)"
    )

    utest.assert(math.abs(addActual - expectedSum) < epsilon)
  }

  val tests = Tests {
    utest.test("programmable_operand_batch_test") {
      // Load data from JSON
      val projectRoot =
        sys.env.get("PROJECT_ROOT").map(os.Path(_)).getOrElse(os.pwd)
      val jsonFile = projectRoot / "data" / "test_cases.json"
      val data = ujson.read(os.read(jsonFile))

      val epsilon = data("epsilon").num.toFloat
      val pairs =
        data("pairs").arr.map(p => (p(0).num.toFloat, p(1).num.toFloat))

      simulate(new Borg) { borg =>
        println("\n--- Starting Programmable Adder Batch ---")
        pairs.foreach { case (a, b) =>
          runBasicMathTest(borg, a, b, epsilon)
        }
        println("--- All Tests Passed ---\n")
      }
    }
  }
}
