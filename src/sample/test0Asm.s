INITC = %00001001 ;$09
MASK1 = %00000100 ;$04
MASK2 = %00000011 ;$03
SCR = $0200
; SCR_LO = $00
; SCR_HI = $02

colour = $00
var_y = $01
var_x = $02
ptr = $0403
; ptr_lo = $03
; ptr_hi = $04

 .org $0600

reset:
 lda #INITC
 sta colour

.loop_1:
 lda #1
 sta var_y

.loop_2:
 lda #1
 sta var_x

.loop_3:
 lda #$00
 sta >ptr
 lda var_y
 sta <ptr
 ldx #5

.tloop_1:
 asl <ptr
 rol >ptr
 dex

 bne .tloop_1

 lda <ptr
 ora var_x
 sta <ptr

 clc
 ; lda <ptr
 ; adc #SCR_LO
 ; sta <ptr
 lda >ptr
 adc #>SCR
 sta >ptr

 lda colour
 sta (<ptr,x)

 lda colour
 eor #MASK1
 sta colour

 inc var_x
 lda var_x
 cmp #31
 bmi .loop_3

 lda colour
 eor #MASK1
 sta colour

 inc var_y
 lda var_y
 cmp #31
 bmi .loop_2

 lda colour
 eor #MASK2
 sta colour

 ; brk
 jmp .loop_1

 .org $FFFA
 .word $0000, reset, $0000

 ; #define MASK1   0b0000'0100
 ; #define MASK2   0b0000'0011
 ; #define INITC   0b0000'1001
 ; #define SCREEN  0200
 ;
 ; uint8_t colour = INITC;
 ;
 ; while (1) {
 ;   for (uint8_t y = 0; y < 32; y++) {
 ;     for (uint8_t x = 0; x < 32; x++) {
 ;       uint8_t *ptr = y << 5 | x + SCREEN;
 ;         *ptr = colour;
 ;         colour ^= MASK1;
 ;       }
 ;     colour ^= MASK1;
 ;   }
 ;   colour ^= MASK2;
 ; }
 ;