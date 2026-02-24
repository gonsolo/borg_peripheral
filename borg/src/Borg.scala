// SPDX-FileCopyrightText: © 2025-2026 Andreas Wendleder
// SPDX-License-Identifier: CERN-OHL-S-2.0

package borg

import chisel3.*
import chisel3.util.*

/** BorgIO defines the interface for the shading processor. It uses
  * memory-mapped I/O for register and instruction memory access.
  */
class BorgIO extends Bundle {
  val address = Input(
    UInt(6.W)
  ) // 64-word address space (byte-addressed internally by shifting)
  val data_in = Input(UInt(32.W))
  val data_write_n = Input(UInt(2.W)) // 0b10 for write
  val data_read_n = Input(UInt(2.W))
  val data_out = Output(UInt(32.W))
  val data_ready = Output(Bool())
  val ui_in = Input(UInt(8.W))
  val uo_out = Output(UInt(8.W))
  val user_interrupt = Output(Bool())
}

/** Borg is a minimal shading processor with instruction memory and a program
  * counter. It executes floating-point addition instructions in a 4-cycle
  * pipeline.
  */
class Borg extends Module {
  val io = IO(new BorgIO)

  // --- Storage ---
  // registerFile: 4 general-purpose 32-bit registers for floating-point data
  val registerFile = Reg(Vec(4, UInt(32.W)))

  // instructionMemory: 8 words of instruction memory to store the shader program
  val instructionMemory = Reg(Vec(8, UInt(32.W)))

  // programCounter: Points to the current instruction in instructionMemory
  val programCounter = RegInit(0.U(3.W))

  // currentInstruction: The instruction currently being decoded/executed
  val currentInstruction = RegInit(0.U(32.W))

  // running: Status flag indicating if the processor is currently executing a program
  val running = RegInit(false.B)

  // --- Pipeline Control ---
  // busy_counter: Tracks the 4-cycle execution stage of the current instruction
  val busy_counter = RegInit(0.U(3.W))
  val is_busy = busy_counter > 0.U

  val is_writing = io.data_write_n === "b10".U

  // --- Memory-Mapped Write Logic ---
  when(is_writing) {
    when(io.address < 16.U) {
      // 0x00 - 0x0C: Register File (rf0, rf1, rf2, rf3)
      registerFile(io.address(3, 2)) := io.data_in
    }.elsewhen(io.address >= 32.U && io.address < 64.U) {
      when(io.address === 60.U) {
        // 0x3C (60): Control Register
        // Bit 0 = Start execution
        // Bit 1 = Reset PC and stop
        when(io.data_in(0)) { running := true.B }
        when(io.data_in(1)) { programCounter := 0.U; running := false.B }
      }.otherwise {
        // 0x20 - 0x38: Instruction Memory (8 slots)
        instructionMemory(io.address(4, 2)) := io.data_in
      }
    }
  }

  // --- Fetch & Execute State Machine ---
  when(running && !is_busy) {
    // Fetch Stage: Load next instruction from memory
    currentInstruction := instructionMemory(programCounter)
    busy_counter := 4.U
  }.elsewhen(is_busy) {
    // Execution Stages: Counting down 4 cycles
    busy_counter := busy_counter - 1.U

    when(busy_counter === 1.U) {
      // End of execution: Increment PC
      programCounter := programCounter + 1.U

      // Stop execution if the next instruction is all zeros (HALT)
      // Note: This creates an "Implicit Halt" at the end of the program
      when(instructionMemory(programCounter + 1.U) === 0.U) {
        running := false.B
      }
    }
  }

  // --- Instruction Decoding (RISC-V inspired) ---
  val rs2_idx = currentInstruction(24, 20)(1, 0)
  val rs1_idx = currentInstruction(19, 15)(1, 0)
  val rd_idx = currentInstruction(11, 7)(1, 0)

  // Floating Point Operands
  val recA = recFNFromFN(8, 24, registerFile(rs1_idx))
  val recB = recFNFromFN(8, 24, registerFile(rs2_idx))

  // --- Arithmetic Logic Unit: Floating Point Adder ---
  val f_add = Module(new AddRecFN(8, 24))
  f_add.io.subOp := false.B
  f_add.io.a := recA
  f_add.io.b := recB
  f_add.io.roundingMode := 0.U
  f_add.io.detectTininess := 1.U

  // --- Multi-Stage Pipeline ---
  val stage1_math_rec = Reg(UInt(33.W))

  // Stage 1: Capture (Cycle 2 of 4)
  when(busy_counter === 2.U) {
    stage1_math_rec := f_add.io.out
  }

  // Stage 2: Writeback (Cycle 1 of 4)
  when(busy_counter === 1.U) {
    registerFile(rd_idx) := fNFromRecFN(8, 24, stage1_math_rec)
  }

  // --- Memory-Mapped Read Logic ---
  io.data_out := MuxLookup(io.address, 0.U)(
    Seq(
      0.U -> registerFile(0),
      4.U -> registerFile(1),
      8.U -> registerFile(2),
      12.U -> registerFile(3),
      16.U -> Cat(
        0.U(30.W),
        !running,
        0.U(1.W)
      ), // Status Register: [Halted, _]
      60.U -> currentInstruction
    )
  )
  // Memory bus reads from peripheral registers take 1 cycle.
  // We rely on software polling `status[1]` (Halted) to avoid reading `res` while busy.
  io.data_ready := true.B

  io.uo_out := 0.U
  io.user_interrupt := false.B
}
