//// Copyright Andreas Wendleder 2025
//// CERN-OHL-S-2.0
//
//package hardfloat
//
//import chisel3.RawModule
//import circt.stage.ChiselStage
//
//object Main extends App {
//
//    val FP32_EXP_W = 8
//    val FP32_SIG_W = 24
//    val ROUND_OPTIONS = 0
//
//    def emitModule(module: => RawModule) = {
//        ChiselStage.emitSystemVerilogFile(
//            gen = module,
//            args = Array("--target-dir", "src"),
//            firtoolOpts = Array("--lowering-options=disallowLocalVariables")
//        )
//    }
//
//    emitModule(new AddRecFN(expWidth = FP32_EXP_W, sigWidth = FP32_SIG_W))
//    emitModule(new AddRawFN(expWidth = FP32_EXP_W, sigWidth = FP32_SIG_W))
//    emitModule(new RoundRawFNToRecFN(
//        expWidth = FP32_EXP_W,
//        sigWidth = FP32_SIG_W,
//        options = ROUND_OPTIONS
//    ))
//    emitModule(new FNFromRecFNWrapper(expWidth = FP32_EXP_W, sigWidth = FP32_SIG_W))
//}
