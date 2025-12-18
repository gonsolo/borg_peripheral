package borg

import chisel3._
import chisel3.util._

class Borg extends Module {
    val io = IO(new Bundle {
        val address      = Input(UInt(6.W))
        val data_in      = Input(UInt(32.W))
        val data_write_n = Input(UInt(2.W))
        val data_read_n  = Input(UInt(2.W))
        val data_out     = Output(UInt(32.W))
        val data_ready   = Output(Bool())
        // Unused TinyQV signals
        val ui_in          = Input(UInt(8.W))
        val uo_out         = Output(UInt(8.W))
        val user_interrupt = Output(Bool())
    })

    // Internal Register
    val reg_value = RegInit(0.U(32.W))

    // Bus Decoding
    val is_writing = io.data_write_n === "b10".U // 32-bit write
    val is_reading = io.data_read_n === "b10".U  // 32-bit read
    
    // Logic: Write to register
    when(is_writing && io.address === 0.U) {
        reg_value := io.data_in
    }

    // Output Logic: Read the register + 1
    // We register the result to ensure there is a clear timing path
    val result_to_read = RegNext(reg_value + 1.U)
    
    // Handshake: Read takes 1 cycle (matches RegNext)
    val read_ready = RegNext(is_reading)
    io.data_ready := is_writing || read_ready

    io.data_out := result_to_read
    
    // Default outputs
    io.uo_out := 0.U
    io.user_interrupt := false.B
}
