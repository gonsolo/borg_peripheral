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
        utest.test("borg_float_addition") {
            simulate(new Borg) { borg =>
                // Address constants
                val ADDR_A      = 0
                val ADDR_B      = 4
                val ADDR_RESULT = 8

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

                // 4. Read from Address 8 (The Result)
                borg.io.address.poke(ADDR_RESULT.U)
                borg.io.data_read_n.poke("b10".U) 
                
                val rawOut    = borg.io.data_out.peek().litValue
                val actualSum = bitsToFloat(rawOut)
                val isReady   = borg.io.data_ready.peek().litToBoolean
                
                val expectedSum = valA + valB
                println(s"A: $valA, B: $valB, Sum Output: $actualSum, Bits: ${rawOut.toString(16)}")

                // 5. Assertions
                utest.assert(isReady == true)
                // Using a tolerance check for floats is usually safer, 
                // though for these exact values (1.5, 2.75) bits should be identical.
                utest.assert(actualSum == expectedSum)

                // 6. Verify Operand A bits are still intact
                borg.io.address.poke(ADDR_A.U)
                val readA = bitsToFloat(borg.io.data_out.peek().litValue)
                utest.assert(readA == valA)
            }
        }
    }
}
