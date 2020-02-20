ptr = $0100 ; HI-LO zp addresses
 .org $0600

res:
 ldx #0
 stz <ptr
 lda #$02
 sta >ptr

cls:
 stz $0200,x
 stz $0300,x
 stz $0400,x
 stz $0500,x
 inx
 bne cls

new_key:
 lda #'|'
 sta (<ptr)

.keypress_listener:
 lda $ff
 beq .keypress_listener

 stz $ff

 cmp #"\r" ; newline for some reason
 beq newline

 cmp #'\010' ; backspace
 beq backspace

 stA (<ptr)

 inc <ptr
 bne 2
 inc >ptr

 bra new_key

newline:
 lda #' '
 sta (<ptr)

 inc <ptr
 bne 2
 inc >ptr

 lda <ptr
 bit #%00011111
 beq new_key

 bra newline

backspace:
 lda #' '
 sta (<ptr)

 lda <ptr
 bne 2
 dec >ptr
 dec <ptr

 bra new_key

 .org $fffa
 .word 0, res, 0