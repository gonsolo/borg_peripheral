// SPDX-FileCopyrightText: © 2025-2026 Andreas Wendleder
// SPDX-License-Identifier: CERN-OHL-S-2.0

package borg

import chisel3.*
import chisel3.util.MuxLookup

class BorgIO extends Bundle {
  val address = Input(UInt(6.W))
  val data_in = Input(UInt(32.W))
  val data_write_n = Input(UInt(2.W))
  val data_read_n = Input(UInt(2.W))
  val data_out = Output(UInt(32.W))
  val data_ready = Output(Bool())
  val ui_in = Input(UInt(8.W))
  val uo_out = Output(UInt(8.W))
  val user_interrupt = Output(Bool())
}

class Borg extends Module {
  val io = IO(new BorgIO)

  val rf = Reg(Vec(3, UInt(32.W)))
  val instr = RegInit(0.U(32.W))

  val busy_counter = RegInit(0.U(3.W))
  val is_busy = busy_counter > 0.U

  val is_writing = io.data_write_n === "b10".U
  val writing_instr = is_writing && io.address === 60.U

  when(writing_instr) {
    busy_counter := 4.U // Start 4-cycle countdown
    instr := io.data_in
  }.elsewhen(is_busy) {
    busy_counter := busy_counter - 1.U
  }

  val funct7 = instr(31, 25)
  val rs2_idx = instr(24, 20) % 3.U
  val rs1_idx = instr(19, 15) % 3.U

  when(is_writing && !writing_instr) {
    when(io.address === 0.U) { rf(0) := io.data_in }
      .elsewhen(io.address === 4.U) { rf(1) := io.data_in }
      .elsewhen(io.address === 16.U) { rf(2) := io.data_in }
  }

  val recA = recFNFromFN(8, 24, rf(rs1_idx))
  val recB = recFNFromFN(8, 24, rf(rs2_idx))

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

  // --- Optimization 1: Multi-Stage Pipeline ---
  // Intermediate register to hold the Recoded result (33 bits)
  val stage1_math_rec = Reg(UInt(33.W))
  val math_result_reg = RegInit(0.U(32.W))

  // Stage 1: Selection (Cycle 2 of 4)
  // This captures the output of Add/Mul before the expensive fNFromRecFN conversion
  when(busy_counter === 2.U) {
    stage1_math_rec := Mux(funct7 === 0x08.U, f_mul.io.out, f_add.io.out)
  }

  // Stage 2: Final Conversion (Cycle 1 of 4)
  // This captures the final IEEE-754 bits
  when(busy_counter === 1.U) {
    math_result_reg := fNFromRecFN(8, 24, stage1_math_rec)
  }

  io.data_out := MuxLookup(io.address, 0.U)(
    Seq(
      0.U -> rf(0),
      4.U -> rf(1),
      16.U -> rf(2),
      8.U -> math_result_reg,
      60.U -> instr
    )
  )

  io.data_ready := !is_busy && !writing_instr

  io.uo_out := 0.U
  io.user_interrupt := false.B
}
