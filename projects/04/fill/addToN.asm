@sum
M=0;
@i
M=1;

(LOOP)
@i
D=M;
@R0
D=D-M
@END
D;JGT

@i
D=M
@sum
M=D+M
@i
M=M+1
@sum
D=M
@R1
M=D
@LOOP
0;JMP



(END)
@END
0;JMP