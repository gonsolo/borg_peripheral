package borg

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import utest._

object BorgTests extends TestSuite {

    // Helper to convert Scala Float to UInt bit pattern
    def floatToBits(f: Float): BigInt = {
        BigInt(java.lang.Float.floatToRawIntBits(f)) & 0xFFFFFFFFL
    }

    // Helper to convert bit pattern back to Scala Float
    def bitsToFloat(b: BigInt): Float = {
        java.lang.Float.intBitsToFloat(b.toInt)
    }

    val tests = Tests {
        utest.test("borg_float_addition_multiplication") {
            simulate(new Borg) { borg =>
                // Address constants
                val ADDR_A      = 0
                val ADDR_B      = 4
                val ADDR_ADD    = 8
                val ADDR_MUL    = 12

                // Initial State
                borg.io.data_write_n.poke("b11".U)
                borg.io.data_read_n.poke("b11".U)
                borg.clock.step(1)

                // 1. Write Float 1.5 to Operand A
                val valA = 1.5f
                borg.io.address.poke(ADDR_A.U)
                borg.io.data_in.poke(floatToBits(valA).U)
                borg.io.data_write_n.poke("b10".U) 
                borg.clock.step(1)

                // 2. Write Float 2.75 to Operand B
                val valB = 2.75f
                borg.io.address.poke(ADDR_B.U)
                borg.io.data_in.poke(floatToBits(valB).U)
                borg.clock.step(1)

                // 3. De-assert write and wait for RegNext(result) to propagate
                borg.io.data_write_n.poke("b11".U)
                borg.clock.step(1)

                borg.io.address.poke(ADDR_ADD.U)
                borg.io.data_read_n.poke("b10".U) 
                
                val add_rawOut    = borg.io.data_out.peek().litValue
                val add_actualSum = bitsToFloat(add_rawOut)
                val add_isReady   = borg.io.data_ready.peek().litToBoolean
                
                val add_expectedSum = valA + valB
                println(s"A: $valA, B: $valB, Sum Output: $add_actualSum, Bits: ${add_rawOut.toString(16)}")

                utest.assert(add_isReady == true)
                utest.assert(add_actualSum == add_expectedSum)

                borg.io.address.poke(ADDR_MUL.U)
                borg.io.data_read_n.poke("b10".U) 
                
                val mul_rawOut    = borg.io.data_out.peek().litValue
                val mul_actualMul = bitsToFloat(mul_rawOut)
                val mul_isReady   = borg.io.data_ready.peek().litToBoolean
                
                val mul_expectedMul = valA * valB
                println(s"A: $valA, B: $valB, Mul Output: $mul_actualMul, Bits: ${mul_rawOut.toString(16)}")

                utest.assert(mul_isReady == true)
                utest.assert(mul_actualMul == mul_expectedMul)

                borg.io.address.poke(ADDR_A.U)
                val readA = bitsToFloat(borg.io.data_out.peek().litValue)
                utest.assert(readA == valA)
            }
        }
    }
}
