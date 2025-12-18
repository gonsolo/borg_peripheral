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

    // 1. Registers for inputs
    val operand_A = RegInit(0.U(32.W))
    val operand_B = RegInit(0.U(32.W))

    // 2. Address Constants
    val ADDR_A      = 0.U(6.W)
    val ADDR_B      = 4.U(6.W)
    val ADDR_RESULT = 8.U(6.W)

    // 3. Write Logic
    val is_writing = io.data_write_n === "b10".U
    when(is_writing) {
        when(io.address === ADDR_A) {
            operand_A := io.data_in
        } .elsewhen(io.address === ADDR_B) {
            operand_B := io.data_in
        }
    }

    // 4. Arithmetic Logic (Integer Addition)
    // We register the result to ensure better timing, similar to your first working example
    val sum_result = RegNext(operand_A + operand_B)

    // 5. Read Logic (Bus Decoding)
    io.data_out := MuxLookup(io.address, 0.U)(Seq(
        ADDR_A      -> operand_A,
        ADDR_B      -> operand_B,
        ADDR_RESULT -> sum_result
    ))

    // 6. Handshake / Defaults
    // data_ready is true when writing, or always true if you don't need wait states
    io.data_ready := true.B
    io.uo_out := 0.U
    io.user_interrupt := false.B
}
