# Commonly Used Instructions in Assembly Language
## Data Movement Instructions
### MOV
MOV is used to move data from one location to another. It can move data between registers, memory locations, and immediate data.

Syntax:
```assembly
MOV destination, source
```
Example:
```assembly
MOV AX, 05H
MOV BX, 06H
MOV CX, AX
MOV DX, BX
```
### XCHG
XCHG is used to exchange the contents of two registers or memory locations.

Syntax:
```assembly
XCHG destination, source
```
Example:
```assembly
XCHG AX, BX
XCHG CX, DX
```
### PUSH
PUSH is used to push data onto the stack.

Syntax:
```assembly
PUSH source
```
Example:
```assembly
PUSH AX
PUSH BX
```
### POP
POP is used to pop data from the stack.

Syntax:
```assembly
POP destination
```
Example:
```assembly
POP AX
POP BX
```
## Arithmetic Instructions
### ADD
ADD is used to add two operands.

Syntax:
```assembly
ADD destination, source
```
Example:
```assembly
ADD AX, BX
ADD CX, DX
```
### SUB
SUB is used to subtract two operands.

Syntax:
```assembly
SUB destination, source
```
Example:
```assembly
SUB AX, BX
SUB CX, DX
```
### INC
INC is used to increment the operand by 1.

Syntax:
```assembly
INC destination
```
Example:
```assembly
INC AX
INC CX
```
### DEC
DEC is used to decrement the operand by 1.

Syntax:
```assembly
DEC destination
```
Example:
```assembly
DEC AX
DEC CX
```
### MUL
MUL is used to multiply two operands.

Syntax:
```assembly
MUL source
```
Example:
```assembly
MUL AX
MUL CX
```
### DIV
DIV is used to divide two operands.

Syntax:
```assembly
DIV source
```
Example:
```assembly
DIV AX
DIV CX
```
## Logical Instructions
### AND
AND is used to perform a logical AND operation between two operands.

Syntax:
```assembly
AND destination, source
```
Example:
```assembly
AND AX, BX
AND CX, DX
```
### OR
OR is used to perform a logical OR operation between two operands.

Syntax:
```assembly
OR destination, source
```
Example:
```assembly
OR AX, BX
OR CX, DX
```
### XOR
XOR is used to perform a logical XOR operation between two operands.

Syntax:
```assembly
XOR destination, source
```
Example:
```assembly
XOR AX, BX
XOR CX, DX
```
### NOT
NOT is used to perform a logical NOT operation on an operand.

Syntax:
```assembly
NOT destination
```
Example:
```assembly
NOT AX
NOT CX
```
## Branching Instructions
### JMP
JMP is used to jump to a specified location.

Syntax:
```assembly
JMP destination
```
Example:
```assembly
JMP label
```
### JZ
JZ is used to jump to a specified location if the zero flag is set.

Syntax:
```assembly
JZ destination
```
Example:
```assembly
JZ label
```
### JNZ
JNZ is used to jump to a specified location if the zero flag is not set.

Syntax:
```assembly
JNZ destination
```
Example:
```assembly
JNZ label
```
### JC
JC is used to jump to a specified location if the carry flag is set.

Syntax:
```assembly
JC destination
```
Example:
```assembly
JC label
```
### JNC
JNC is used to jump to a specified location if the carry flag is not set.

Syntax:
```assembly
JNC destination
```
Example:
```assembly
JNC label
```
### JA
JA is used to jump to a specified location if the zero flag and the carry flag are not set.

Syntax:
```assembly
JA destination
```
Example:
```assembly
JA label
```