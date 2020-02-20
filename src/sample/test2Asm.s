 .org $0600

res:
 lda #4
 sta draw_x
 lda #1
 sta draw_y

res_loop1:
 cli
 jsr cls
 lda #<pixelart2
 sta <draw_spr
 lda #>pixelart2
 sta >draw_spr
 jsr draw
 inc draw_x
 inc draw_y
 ; lda draw_y
 ; cmp #30
 brk
 ; bmi res_loop1
 brk

nmi:
irq:
 cli
 ; tsx
 ; lda $0101,x
 pla
 and #$10
 bne irq_break
 pla
 pla
 jmp res_loop1

irq_break:
 cli
 pla
 pla

irq_break_loop1:
 jmp irq_break_loop1

; nmi:
; pla
; pla
; pla
; jmp res_loop1

; Params
draw_x = $00
draw_y = $01
draw_spr = $0302
; Vars
draw_index = $04
draw_i = $05
draw_j = $06
draw_x2 = $07
draw_y2 = $08
draw_ptr = $0A09
draw_colour = $0B
draw:

 lda #0 ; index = 0
 sta draw_index
 
 lda #0 ; i = 0
 sta draw_i

draw_loop1:
 lda draw_i ; i < 16
 cmp #16
 bpl draw_loop1_break
 
 lda #0 ; j = 0
 sta draw_j

draw_loop2:
 lda draw_j ; j < 16
 cmp #16
 bpl draw_loop2_break

 clc ; x2 = x + j
 lda draw_x
 adc draw_j
 cmp #32
 bpl draw_loop2_continue
 sta draw_x2
 
 clc ; y2 = y + i
 lda draw_y
 adc draw_i
 cmp #32
 bpl draw_loop1_break
 sta draw_y2
 
 lda draw_y2 ; ptr = y2 << 5 | x2 + 0x0200
 sta <draw_ptr
 lda #0
 sta >draw_ptr
 ldx #5

draw_shift1:
 asl <draw_ptr
 rol >draw_ptr
 dex
 bne draw_shift1

 lda <draw_ptr
 ora draw_x2
 sta <draw_ptr
 clc
 lda >draw_ptr
 adc #$02
 sta >draw_ptr
 
 lda (<draw_spr,x) ; *ptr = *spr++
 sta (<draw_ptr,x)
 
draw_loop2_continue:
 
 clc
 lda <draw_spr
 adc #1
 sta <draw_spr
 lda >draw_spr
 adc #0
 sta >draw_spr
 
 inc draw_j ; j++
 jmp draw_loop2

draw_loop2_break:
 
 inc draw_i ; i++
 jmp draw_loop1

draw_loop1_break:
 rts

cls:
 lda #0
 ldx #0

cls_loop:
 sta $0200,x
 sta $0300,x
 sta $0400,x
 sta $0500,x
 inx
 bne cls_loop

 rts

; void draw(uint8_t x, uint8_t y, uint16_t spr) {
;  for (uint8_t i = 0; i < 16; i++) {
;   for (uint8_t j = 0; j < 16; j++) {
;    uint8_t x2 = x + j, y2 = y + i;
;    uint16_t ptr = y2 << 5 | x2 + 0x0200;
;    // uint8_t colour = *(spr);
;    // *ptr = colour;
;    *ptr = *spr++;
;   }
;  }
; }


pixelart1:
 .byte $80, $81, $82, $83, $84, $85, $86, $87
 .byte $88, $89, $8A, $8B, $8C, $8D, $8E, $8F
 .byte $90, $91, $92, $93, $94, $95, $96, $97
 .byte $98, $99, $9A, $9B, $9C, $9D, $9E, $9F
 .byte $A0, $A1, $A2, $A3, $A4, $A5, $A6, $A7
 .byte $A8, $A9, $AA, $AB, $AC, $AD, $AE, $AF
 .byte $B0, $B1, $B2, $B3, $B4, $B5, $B6, $B7
 .byte $B8, $B9, $BA, $BB, $BC, $BD, $BE, $BF

 .byte $80, $81, $82, $83, $84, $85, $86, $87
 .byte $88, $89, $8A, $8B, $8C, $8D, $8E, $8F
 .byte $90, $91, $92, $93, $94, $95, $96, $97
 .byte $98, $99, $9A, $9B, $9C, $9D, $9E, $9F
 .byte $A0, $A1, $A2, $A3, $A4, $A5, $A6, $A7
 .byte $A8, $A9, $AA, $AB, $AC, $AD, $AE, $AF
 .byte $B0, $B1, $B2, $B3, $B4, $B5, $B6, $B7
 .byte $B8, $B9, $BA, $BB, $BC, $BD, $BE, $BF

 .byte $C0, $C1, $C2, $C3, $C4, $C5, $C6, $C7
 .byte $C8, $C9, $CA, $CB, $CC, $CD, $CE, $CF
 .byte $D0, $D1, $D2, $D3, $D4, $D5, $D6, $D7
 .byte $D8, $D9, $DA, $DB, $DC, $DD, $DE, $DF
 .byte $E0, $E1, $E2, $E3, $E4, $E5, $E6, $E7
 .byte $E8, $E9, $EA, $EB, $EC, $ED, $EE, $EF
 .byte $F0, $F1, $F2, $F3, $F4, $F5, $F6, $F7
 .byte $F8, $F9, $FA, $FB, $FC, $FD, $FE, $FF

 .byte $C0, $C1, $C2, $C3, $C4, $C5, $C6, $C7
 .byte $C8, $C9, $CA, $CB, $CC, $CD, $CE, $CF
 .byte $D0, $D1, $D2, $D3, $D4, $D5, $D6, $D7
 .byte $D8, $D9, $DA, $DB, $DC, $DD, $DE, $DF
 .byte $E0, $E1, $E2, $E3, $E4, $E5, $E6, $E7
 .byte $E8, $E9, $EA, $EB, $EC, $ED, $EE, $EF
 .byte $F0, $F1, $F2, $F3, $F4, $F5, $F6, $F7
 .byte $F8, $F9, $FA, $FB, $FC, $FD, $FE, $FF

pixelart2:
 .byte $90, $90, $90, $90, $90, $90, $90, $90
 .byte $90, $90, $90, $90, $90, $90, $90, $90
 .byte $90, $90, $90, $90, $90, $90, $97, $BF
 .byte $BF, $97, $90, $90, $90, $90, $90, $90
 .byte $90, $90, $90, $90, $90, $97, $BF, $BF
 .byte $97, $97, $97, $90, $90, $90, $90, $90
 .byte $90, $90, $90, $90, $90, $97, $BF, $BF
 .byte $97, $97, $97, $90, $90, $90, $90, $90

 .byte $A0, $A0, $A0, $A0, $A0, $97, $97, $BF
 .byte $97, $97, $97, $A0, $A0, $A0, $A0, $A0
 .byte $A0, $A0, $A0, $A0, $A0, $A0, $BF, $AB
 .byte $AB, $AB, $A0, $A0, $A0, $A0, $A0, $A0
 .byte $A0, $A0, $A0, $A0, $A0, $A0, $AB, $BF
 .byte $AB, $AB, $A0, $A0, $A0, $A0, $A0, $A0
 .byte $A0, $A0, $A0, $A0, $A0, $A0, $AB, $AB
 .byte $AB, $AB, $A0, $A0, $A0, $A0, $A0, $A0

 .byte $B0, $B0, $B0, $B0, $B0, $B0, $BF, $AB
 .byte $AB, $AB, $B0, $B0, $B0, $B0, $B0, $B0
 .byte $B0, $B0, $B0, $AB, $AB, $AB, $AB, $AB
 .byte $AB, $AB, $AB, $AB, $AB, $B0, $B0, $B0
 .byte $B0, $B0, $AB, $AB, $AB, $AB, $AB, $AB
 .byte $AB, $AB, $AB, $AB, $AB, $AB, $B0, $B0
 .byte $B0, $B0, $AB, $AB, $AB, $AB, $AB, $AB
 .byte $AB, $AB, $AB, $AB, $AB, $AB, $B0, $B0

 .byte $B5, $B5, $AB, $AB, $AB, $AB, $AB, $B5
 .byte $B5, $AB, $AB, $AB, $AB, $AB, $B5, $B5
 .byte $B5, $B5, $B5, $AB, $AB, $AB, $B5, $B5
 .byte $B5, $B5, $AB, $AB, $AB, $B5, $B5, $B5
 .byte $B5, $B5, $B5, $B5, $B5, $B5, $B5, $B5
 .byte $B5, $B5, $B5, $B5, $B5, $B5, $B5, $B5
 .byte $B5, $B5, $B5, $B5, $B5, $B5, $B5, $B5
 .byte $B5, $B5, $B5, $B5, $B5, $B5, $B5, $B5

 .org $FFFA
 .word nmi, res, irq