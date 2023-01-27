.org $0800
scr = $0400 ; pointer

reset:
.num = $04

; init vars and pointers
  stz .num

  lda #<scr
  sta decimal.copy_to
  lda #>scr
  sta decimal.copy_to + 1

.loop:
  lda .num
  sta decimal.num
  jsr decimal

; add offset
  lda #4
  ;inc
  
  adc decimal.copy_to
  sta decimal.copy_to
  lda #0
  adc decimal.copy_to + 1
  sta decimal.copy_to + 1

; loop through all numbers 0-255
  inc .num
  bne .loop

stp

decimal:
; args
  .num = $00     ; 1 byte
  .copy_to = $01 ; 3 bytes

  .mod10 = $03

; stack start
  lda #0
  pha

; init vars
.divide:
  ldx #8
  stz .mod10
  clc

.div_loop:
; rotate
  rol .num
  rol .mod10

; subtract
  sec
  lda .mod10
  sbc #10
  bcc .skip_mod10

; replace mod10
  sta .mod10

; iterate loop
.skip_mod10:
  dex
  bne .div_loop

; push digit to the stack
  rol .num

  lda .mod10
  clc
  adc #'0'
  pha

; next digit
  lda .num
  bne .divide

; print
  ldy #0
.print_loop:
  pla
  beq .end
  sta (.copy_to),y

  iny
  bra .print_loop
.end:

; return character count
  tya
  rts

.org $fffa
.word reset, reset, reset