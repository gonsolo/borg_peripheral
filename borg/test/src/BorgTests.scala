package borg

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import java.lang.Float
import utest._

object BorgTests extends TestSuite {

    // Helper to convert float to Chisel UInt
    def floatToUInt(f: Float): UInt = {
        val bits = java.lang.Float.floatToRawIntBits(f)
        (bits.toLong & 0xFFFFFFFFL).U(32.W)
    }
    
    // Helper to convert BigInt back to float
    def uintToFloat(i: BigInt): Float = java.lang.Float.intBitsToFloat(i.toInt)

    val tests = Tests {
        // Use utest.test explicitly to avoid collision with chisel3.test
        utest.test("borg_simple_increment") {
            simulate(new Borg) { borg =>
                // Initial State: Idle
                borg.io.data_write_n.poke("b11".U)
                borg.io.data_read_n.poke("b11".U)
                borg.clock.step(1)

                // 1. Write Value 0x42 to Address 0
                val inputVal = 0x42
                borg.io.address.poke(0.U)
                borg.io.data_in.poke(inputVal.U)
                borg.io.data_write_n.poke("b10".U) // 32-bit write
                borg.clock.step(1)

                // 2. De-assert write
                borg.io.data_write_n.poke("b11".U)
                borg.clock.step(1)

                // 3. Start Read from Address 0
                // We need to see the result of: 
                // result_to_read = RegNext(reg_value + 1.U)
                borg.io.address.poke(0.U)
                borg.io.data_read_n.poke("b10".U) 
                
                // Step to let the read register and RegNext propagate
                borg.clock.step(1)

                // 4. Check results using utest.assert
                val actualOut = borg.io.data_out.peek().litValue
                val isReady = borg.io.data_ready.peek().litToBoolean
                
                println(s"Input: $inputVal, Output: $actualOut, Ready: $isReady")

                utest.assert(isReady == true)
                utest.assert(actualOut == (inputVal + 1))
            }
        }
    }
}
