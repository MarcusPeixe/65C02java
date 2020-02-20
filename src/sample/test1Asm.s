 .org $0600

reset:
 lda #'F'

.loop:
 sta $0200,X
 sta $0300,X
 sta $0400,X
 sta $0500,X
 
 inx
 bne .loop
 
 wai

irq:
 inc a

 .org $FFFA
 .word irq, reset, irq