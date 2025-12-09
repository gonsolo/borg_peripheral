package borg

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import utest._

import utest._
import chisel3._
import chisel3.experimental.BundleLiterals._

object BorgTests extends TestSuite {
    val tests = Tests {
        utest.test("borg_integer_add") {
            simulate(new Borg) { borg =>
                borg.io.data_write_n.poke("b11".U)
                borg.io.data_read_n.poke("b11".U)
                borg.io.address.poke(0.U)
                borg.clock.step(1)

                val A = 123.U
                borg.io.address.poke(0.U)
                borg.io.data_in.poke(A)
                borg.io.data_write_n.poke("b10".U)
                borg.clock.step(1)

                val B = 456.U
                borg.io.address.poke(4.U)
                borg.io.data_in.poke(B)
                borg.io.data_write_n.poke("b10".U)
                borg.clock.step(1)

                borg.io.data_write_n.poke("b11".U)

                val expected_sum = 123 + 456

                borg.io.address.poke(8.U)
                borg.clock.step(1)

                val actual_sum = borg.io.data_out.peek().litValue.toInt

                println(s"Operand A: 123, Operand B: 456")
                println(s"Expected Sum: $expected_sum, Actual Sum: $actual_sum")

                utest.assert(actual_sum == expected_sum)
            }
        }
    }
}

