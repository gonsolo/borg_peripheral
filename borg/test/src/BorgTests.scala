package borg

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import utest._

object BorgTests extends TestSuite {

    val tests = Tests {
        utest.test("borg_integer_addition") {
            simulate(new Borg) { borg =>
                // Address constants (matching the Module)
                val ADDR_A      = 0
                val ADDR_B      = 4
                val ADDR_RESULT = 8

                // Initial State: Idle (Write/Read lines high/inactive)
                borg.io.data_write_n.poke("b11".U)
                borg.io.data_read_n.poke("b11".U)
                borg.clock.step(1)

                // 1. Write Value 10 to Operand A
                val valA = 10
                borg.io.address.poke(ADDR_A.U)
                borg.io.data_in.poke(valA.U)
                borg.io.data_write_n.poke("b10".U) 
                borg.clock.step(1)

                // 2. Write Value 32 to Operand B
                val valB = 32
                borg.io.address.poke(ADDR_B.U)
                borg.io.data_in.poke(valB.U)
                // data_write_n remains "b10"
                borg.clock.step(1)

                // 3. De-assert write and wait for RegNext(A+B) to propagate
                borg.io.data_write_n.poke("b11".U)
                borg.clock.step(1)

                // 4. Read from Address 8 (The Result)
                borg.io.address.poke(ADDR_RESULT.U)
                borg.io.data_read_n.poke("b10".U) 
                
                // Peek values immediately (since data_out is driven by MuxLookup)
                val actualSum = borg.io.data_out.peek().litValue
                val isReady   = borg.io.data_ready.peek().litToBoolean
                
                println(s"A: $valA, B: $valB, Sum Output: $actualSum, Ready: $isReady")

                // 5. Assertions
                utest.assert(isReady == true)
                utest.assert(actualSum == (valA + valB))

                // 6. Optional: Verify Operand A is still readable
                borg.io.address.poke(ADDR_A.U)
                val readA = borg.io.data_out.peek().litValue
                utest.assert(readA == valA)
            }
        }
    }
}
