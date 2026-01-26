package borg

import chisel3._
import chisel3.util.MuxLookup

class Borg extends Module {
  val io = IO(new Bundle {
    val address = Input(UInt(6.W))
    val data_in = Input(UInt(32.W))
    val data_write_n = Input(UInt(2.W))
    val data_read_n = Input(UInt(2.W))
    val data_out = Output(UInt(32.W))
    val data_ready = Output(Bool())
    val ui_in = Input(UInt(8.W))
    val uo_out = Output(UInt(8.W))
    val user_interrupt = Output(Bool())
  })

  val rf = RegInit(VecInit(Seq.fill(3)(0.U(32.W))))
  val instr = RegInit(0.U(32.W))

  // Instruction Fields (RISC-V Standard)
  val funct7 = instr(31, 25)
  val rs2_idx = instr(24, 20) % 3.U
  val rs1_idx = instr(19, 15) % 3.U

  val is_writing = io.data_write_n === "b10".U
  when(is_writing) {
    when(io.address === 0.U) { rf(0) := io.data_in }
      .elsewhen(io.address === 4.U) { rf(1) := io.data_in }
      .elsewhen(io.address === 16.U) { rf(2) := io.data_in }
      .elsewhen(io.address === 60.U) { instr := io.data_in }
  }

  val recA = recFNFromFN(8, 24, rf(rs1_idx))
  val recB = recFNFromFN(8, 24, rf(rs2_idx))

  // Math Units
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

  // Unified Execution Selection
  // If funct7 is 0x08 (8 decimal), select Multiply. Otherwise, Add.
  val result_bits = Mux(
    funct7 === 0x08.U,
    fNFromRecFN(8, 24, f_mul.io.out),
    fNFromRecFN(8, 24, f_add.io.out)
  )

  io.data_out := MuxLookup(io.address, 0.U)(
    Seq(
      0.U -> rf(0),
      4.U -> rf(1),
      16.U -> rf(2),
      8.U -> result_bits, // Address 12 is now retired
      60.U -> instr
    )
  )

  io.data_ready := true.B
  io.uo_out := 0.U
  io.user_interrupt := false.B
}
