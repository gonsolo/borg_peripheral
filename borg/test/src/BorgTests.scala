package borg

import chisel3._
import chisel3.experimental.BundleLiterals._
import chisel3.simulator.EphemeralSimulator._
import java.lang.Float
import utest._

object BorgTests extends TestSuite {

    def floatToUInt(f: Float): UInt = Float.floatToRawIntBits(f).toLong.U(32.W)
    def uintToFloat(i: Long): Float = java.lang.Float.intBitsToFloat(i.toInt)

    val tests = Tests {
        utest.test("borg_float_add") {
            simulate(new Borg) { borg =>
                borg.io.data_write_n.poke("b11".U)
                borg.io.data_read_n.poke("b11".U)
                borg.io.address.poke(0.U)
                borg.clock.step(1)

                val A_float = 123.5f
                val A_bits = floatToUInt(A_float)
                borg.io.address.poke(0.U)
                borg.io.data_in.poke(A_bits)
                borg.io.data_write_n.poke("b10".U)
                borg.clock.step(1)

                val B_float = 456.75f
                val B_bits = floatToUInt(B_float)
                borg.io.address.poke(4.U)
                borg.io.data_in.poke(B_bits)
                borg.io.data_write_n.poke("b10".U)
                borg.clock.step(1)

                borg.io.data_write_n.poke("b11".U)

                val expected_sum = A_float + B_float

                borg.io.address.poke(8.U)
                borg.clock.step(1)

                val actual_sum_bits = borg.io.data_out.peek().litValue.toLong
                val actual_sum = uintToFloat(actual_sum_bits)

                println(s"Operand A: $A_float, Operand B: $B_float")
                println(s"Expected Sum: $expected_sum, Actual Sum: $actual_sum")

                val epsilon = 0.000001f
                utest.assert((actual_sum - expected_sum).abs < epsilon)
            }
        }
    }
}
