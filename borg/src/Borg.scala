package borg

import chisel3._
import chisel3.util.MuxLookup

class Borg extends Module {
    val io = IO(new Bundle {
        val address      = Input(UInt(6.W))
        val data_in      = Input(UInt(32.W))
        val data_write_n = Input(UInt(2.W))
        val data_read_n  = Input(UInt(2.W))
        val data_out     = Output(UInt(32.W))
        val data_ready   = Output(Bool())
        // Unused TinyQV signals
        val ui_in        = Input(UInt(8.W))
        val uo_out       = Output(UInt(8.W))
        val user_interrupt = Output(Bool())
    })

    // 1. Registers for input bits (Standard IEEE-754)
    val operand_A_bits = RegInit(0.U(32.W))
    val operand_B_bits = RegInit(0.U(32.W))

    // 2. Address Constants
    val ADDR_A      = 0.U(6.W)
    val ADDR_B      = 4.U(6.W)
    val ADDR_RESULT = 8.U(6.W)

    // 3. Write Logic
    val is_writing = io.data_write_n === "b10".U
    when(is_writing) {
        when(io.address === ADDR_A) {
            operand_A_bits := io.data_in
        } .elsewhen(io.address === ADDR_B) {
            operand_B_bits := io.data_in
        }
    }

    // 4. HardFloat Conversion & Addition
    // Convert standard 32-bit Float bits to Recoded format (33 bits)
    // expWidth = 8, sigWidth = 24 for Float32
    val recA = recFNFromFN(8, 24, operand_A_bits)
    val recB = recFNFromFN(8, 24, operand_B_bits)

    val f_add = Module(new AddRecFN(8, 24))
    f_add.io.subOp := false.B                          // False = Add
    f_add.io.a := recA
    f_add.io.b := recB
    f_add.io.roundingMode := 0.U                       // round_near_even
    f_add.io.detectTininess := 1.U                     // tininess_afterRounding

    // 5. Convert result back to standard IEEE-754 bits
    // We wrap this in RegNext to improve timing for the bus read
    val result_bits = RegNext(fNFromRecFN(8, 24, f_add.io.out))

    // 6. Read Logic
    io.data_out := MuxLookup(io.address, 0.U)(Seq(
        ADDR_A      -> operand_A_bits,
        ADDR_B      -> operand_B_bits,
        ADDR_RESULT -> result_bits
    ))

    // 7. Handshake / Defaults
    io.data_ready := true.B
    io.uo_out := 0.U
    io.user_interrupt := false.B
}
