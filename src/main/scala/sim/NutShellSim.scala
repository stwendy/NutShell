/**************************************************************************************
* Copyright (c) 2020 Institute of Computing Technology, CAS
* Copyright (c) 2020 University of Chinese Academy of Sciences
* 
* NutShell is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2. 
* You may obtain a copy of Mulan PSL v2 at:
*             http://license.coscl.org.cn/MulanPSL2 
* 
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER 
* EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR 
* FIT FOR A PARTICULAR PURPOSE.  
*
* See the Mulan PSL v2 for more details.  
***************************************************************************************/

package sim

import system._
import nutcore.NutCoreConfig

import chisel3._
import chisel3.util._
import chisel3.util.experimental.BoringUtils

import bus.axi4._
import device.AXI4RAM

class DiffTestIO extends Bundle {
  val r = Output(Vec(32, UInt(64.W)))
  val commit = Output(Bool())
  val isMultiCommit = Output(Bool())
  val thisPC = Output(UInt(64.W))
  val thisINST = Output(UInt(32.W))
  val isMMIO = Output(Bool())
  val isRVC = Output(Bool())
  val isRVC2 = Output(Bool())
  val intrNO = Output(UInt(64.W))
  
  val priviledgeMode = Output(UInt(2.W))
  val mstatus = Output(UInt(64.W))
  val sstatus = Output(UInt(64.W))
  val mepc = Output(UInt(64.W))
  val sepc = Output(UInt(64.W))
  val mcause = Output(UInt(64.W))
  val scause = Output(UInt(64.W))
}

class TrapIO extends Bundle {
  val valid = Output(Bool())
  val code = Output(UInt(3.W))
  val pc = Output(UInt(64.W))
  val cycleCnt = Output(UInt(64.W))
  val instrCnt = Output(UInt(64.W))
}

class NutShellSimTop extends Module {
  val io = IO(new Bundle{
    val difftest = new DiffTestIO
    val difftestCtrl = new DiffTestCtrlIO
    val trap = new TrapIO
  })

  lazy val config = NutCoreConfig(FPGAPlatform = false)
  val soc = Module(new NutShell()(config))
  val mem = Module(new AXI4RAM(memByte = 128 * 1024 * 1024, useBlackBox = true))
  // Be careful with the commit checking of emu.
  // A large delay will make emu incorrectly report getting stuck.
  val memdelay = Module(new AXI4Delayer(0))
  val mmio = Module(new SimMMIO)

  soc.io.frontend <> mmio.io.dma

  memdelay.io.in <> soc.io.mem
  mem.io.in <> memdelay.io.out

  mmio.io.rw <> soc.io.mmio

  soc.io.meip := mmio.io.meip

  val difftest = WireInit(0.U.asTypeOf(new DiffTestIO))
  BoringUtils.addSink(difftest.commit, "difftestCommit")
  BoringUtils.addSink(difftest.isMultiCommit, "difftestMultiCommit")
  BoringUtils.addSink(difftest.thisPC, "difftestThisPC")
  BoringUtils.addSink(difftest.thisINST, "difftestThisINST")
  BoringUtils.addSink(difftest.isMMIO, "difftestIsMMIO")
  BoringUtils.addSink(difftest.isRVC, "difftestIsRVC")
  BoringUtils.addSink(difftest.isRVC2, "difftestIsRVC2")
  BoringUtils.addSink(difftest.intrNO, "difftestIntrNO")
  BoringUtils.addSink(difftest.r, "difftestRegs")
  BoringUtils.addSink(difftest.priviledgeMode, "difftestMode")
  BoringUtils.addSink(difftest.mstatus, "difftestMstatus")
  BoringUtils.addSink(difftest.sstatus, "difftestSstatus") 
  BoringUtils.addSink(difftest.mepc, "difftestMepc")
  BoringUtils.addSink(difftest.sepc, "difftestSepc")
  BoringUtils.addSink(difftest.mcause, "difftestMcause")
  BoringUtils.addSink(difftest.scause, "difftestScause")
  io.difftest := difftest
  io.difftestCtrl <> mmio.io.difftestCtrl

  val trap = WireInit(0.U.asTypeOf(new TrapIO))
  BoringUtils.addSink(trap.valid, "trapValid")
  BoringUtils.addSink(trap.code, "trapCode")
  BoringUtils.addSink(trap.pc, "trapPC")
  BoringUtils.addSink(trap.cycleCnt, "simCycleCnt")
  BoringUtils.addSink(trap.instrCnt, "simInstrCnt")
  io.trap := trap
}
