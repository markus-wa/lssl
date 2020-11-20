; SPIR-V
; Version: 1.0
; Generator: lsslc; v0.1.0-SNAPSHOT
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
%_float_32 = OpTypeFloat 32
%v4float = OpTypeVector %_float_32 4
%_ptr_Output_v4float = OpTypePointer Output %v4float
%FragColor = OpVariable %_ptr_Output_v4float Output
%Inputs = OpTypeStruct %v4float
%_ptr_Uniform_Inputs = OpTypePointer Uniform %Inputs
%inputs = OpVariable %_ptr_Uniform_Inputs Uniform
%fn_void = OpTypeFunction %void
%_ptr_Uniform_v4float = OpTypePointer Uniform %v4float
%int = OpTypeInt 32 1
%int_0 = OpConstant %int 0
%main = OpFunction %void None %fn_void
%_main_entrypoint = OpLabel
%_x_0 = OpAccessChain %_ptr_Uniform_v4float %inputs %int_0
%_x_1 = OpLoad %v4float %_x_0
OpStore %FragColor %_x_1
OpReturn
OpFunctionEnd
