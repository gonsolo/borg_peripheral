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
        val ui_in          = Input(UInt(8.W))
        val uo_out         = Output(UInt(8.W))
        val user_interrupt = Output(Bool())
    })

    val rf = RegInit(VecInit(Seq.fill(3)(0.U(32.W))))

    val ADDR_A   = 0.U(6.W)
    val ADDR_B   = 4.U(6.W)
    val ADDR_ADD = 8.U(6.W)
    val ADDR_MUL = 12.U(6.W)
    val ADDR_C   = 16.U(6.W)

    val is_writing = io.data_write_n === "b10".U
    when(is_writing) {
        when(io.address === ADDR_A) {
            rf(0) := io.data_in
        } .elsewhen(io.address === ADDR_B) {
            rf(1) := io.data_in
        } .elsewhen(io.address === ADDR_C) {
            rf(2) := io.data_in
        }
    }

    val recA = recFNFromFN(8, 24, rf(0))
    val recB = recFNFromFN(8, 24, rf(1))

    val f_add = Module(new AddRecFN(8, 24))
    f_add.io.subOp := false.B
    f_add.io.a := recA
    f_add.io.b := recB
    f_add.io.roundingMode := 0.U
    f_add.io.detectTininess := 1.U

    val f_mul = Module(new MulRecFN(8, 24))
    f_mul.io.a := recA
    f_mul.io.b := recB
    f_mul.io.roundingMode := 0.U
    f_mul.io.detectTininess := 1.U

    val add_result = RegNext(fNFromRecFN(8, 24, f_add.io.out))
    val mul_result = RegNext(fNFromRecFN(8, 24, f_mul.io.out))

    io.data_out := MuxLookup(io.address, 0.U)(Seq(
        ADDR_A   -> rf(0),
        ADDR_B   -> rf(1),
        ADDR_C   -> rf(2),
        ADDR_ADD -> add_result,
        ADDR_MUL -> mul_result
    ))

    io.data_ready := true.B
    io.uo_out := 0.U
    io.user_interrupt := false.B
}
