// Copyright Andreas Wendleder 2025
// CERN-OHL-S-2.0

package borg

import chisel3._
import chisel3.util.{MuxCase, Cat}
import hardfloat._

import chisel3._
import chisel3.util._

class Borg extends Module {
    val io = IO(new Bundle {
        val ui_in = Input(UInt(8.W))
        val uo_out = Output(UInt(8.W))
        val address = Input(UInt(6.W))
        val data_in = Input(UInt(32.W))
        val data_write_n = Input(UInt(2.W))
        val data_read_n = Input(UInt(2.W))
        val data_out = Output(UInt(32.W))
        val data_ready = Output(Bool())
        val user_interrupt = Output(Bool())
    })

    val operand_A = RegInit(0.U(32.W))
    val operand_B = RegInit(0.U(32.W))

    val ADDR_A = 0.U(6.W)
    val ADDR_B = 4.U(6.W)
    val ADDR_RESULT = 8.U(6.W)

    val is_write = io.data_write_n === "b10".U

    when (is_write) {
        when (io.address === ADDR_A) {
            operand_A := io.data_in
        }
        .elsewhen (io.address === ADDR_B) {
            operand_B := io.data_in
        }
    }

    val sum = operand_A + operand_B

    io.data_out := MuxLookup(io.address, 0.U) (Seq(
      ADDR_A      -> operand_A,
      ADDR_B      -> operand_B,
      ADDR_RESULT -> sum
    ))

    io.data_ready := true.B

    io.uo_out := DontCare
    io.user_interrupt := false.B
}

