; SPIR-V
; Version: 1.0
; Generator: Khronos Glslang Reference Front End; 10
; Bound: 18
; Schema: 0
OpCapability Shader
%1 = OpExtInstImport "GLSL.std.450"
OpMemoryModel Logical GLSL450
OpEntryPoint Fragment %main "main" %FragColor
OpExecutionMode %main OriginUpperLeft
OpSource GLSL 460
OpName %main "main"
OpName %FragColor "FragColor"
OpName %Inputs "Inputs"
OpMemberName %Inputs 0 "color"
OpName %inputs "inputs"
OpDecorate %FragColor Location 0
OpMemberDecorate %Inputs 0 Offset 0
OpDecorate %Inputs Block
OpDecorate %inputs DescriptorSet 0
OpDecorate %inputs Binding 0
%void = OpTypeVoid
%3 = OpTypeFunction %void
%float = OpTypeFloat 32
%v4float = OpTypeVector %float 4
%_ptr_Output_v4float = OpTypePointer Output %v4float
%FragColor = OpVariable %_ptr_Output_v4float Output
%Inputs = OpTypeStruct %v4float
%_ptr_Uniform_Inputs = OpTypePointer Uniform %Inputs
%inputs = OpVariable %_ptr_Uniform_Inputs Uniform
%int = OpTypeInt 32 1
%int_0 = OpConstant %int 0
%_ptr_Uniform_v4float = OpTypePointer Uniform %v4float
%main = OpFunction %void None %3
%5 = OpLabel
%16 = OpAccessChain %_ptr_Uniform_v4float %inputs %int_0
%17 = OpLoad %v4float %16
OpStore %FragColor %17
OpReturn
OpFunctionEnd
