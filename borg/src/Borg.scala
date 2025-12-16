// Copyright Andreas Wendleder 2025
// CERN-OHL-S-2.0

package borg

import chisel3._
import chisel3.util.MuxLookup

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

    val operand_A_bits = RegInit(0.U(32.W))
    val operand_B_bits = RegInit(0.U(32.W))

    val ADDR_A = 0.U(6.W)
    val ADDR_B = 4.U(6.W)
    val ADDR_RESULT = 8.U(6.W)

    val is_write = io.data_write_n === "b10".U

    when (is_write) {
        when (io.address === ADDR_A) {
            operand_A_bits := io.data_in
        }
        .elsewhen (io.address === ADDR_B) {
            operand_B_bits := io.data_in
        }
    }

    val recFN_A = recFNFromFN(Globals.expWidth, Globals.sigWidth, operand_A_bits)
    val recFN_B = recFNFromFN(Globals.expWidth, Globals.sigWidth, operand_B_bits)
    
    val float_adder = Module(new AddRecFN(Globals.expWidth, Globals.sigWidth))

    float_adder.io.a := recFN_A
    float_adder.io.b := recFN_B
    float_adder.io.subOp := false.B 
    float_adder.io.roundingMode := borg.consts.round_near_even
    float_adder.io.detectTininess := borg.consts.tininess_afterRounding

    val result_recFN = float_adder.io.out
    val result_bits = fNFromRecFN(Globals.expWidth, Globals.sigWidth, result_recFN)

    io.data_out := MuxLookup(io.address, 0.U(32.W)) (Seq(
      ADDR_A      -> operand_A_bits,
      ADDR_B      -> operand_B_bits,
      ADDR_RESULT -> result_bits
    ))

    io.data_ready := true.B
    io.uo_out := DontCare
    io.user_interrupt := false.B
}
