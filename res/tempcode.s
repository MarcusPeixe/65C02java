.org $0600
screen = $0200
res:
 jsr main
stp

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
main:
.i   = $00
.max =  63
 lda #<.msg
 sta printf.fstr
 lda #>.msg
 sta printf.fstr + 1

 .loop1:
  ldx .i
  cpx #.max
  bpl .skip1

  lda res,x
  sta printf.args

  inx
  stx .i
  
  jsr printf
  bra .loop1
 .skip1:

rts

.msg:
.string "$%b "
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
printf:
.fstr      =   $fe
.buffer    = $1000
.bufferlen = $0100
.args      = .buffer + .bufferlen
.out_i     =   $fb
.str_i     =   $fc
.arg_i     =   $fd

 stz .arg_i
 stz .str_i

 .loop1:
  ldy .str_i
  lda (.fstr),y
  beq .end

  cmp #'%'
  beq .tohex

  iny
  sty .str_i

  ldy .out_i
  sta screen,y
  iny
  sty .out_i
 bne .loop1

 .tohex:
  iny
  lda (.fstr),y
  iny
  sty .str_i
  cmp #'b'
  bne .skip_b
   jsr .tobyte
   inc .arg_i
   bra .loop1
  .skip_b:
  cmp #'w'
  bne .loop1
   inc .arg_i
   jsr .tobyte
   dec .arg_i
   jsr .tobyte
   inc .arg_i
   inc .arg_i
  bra .loop1
 .end:
rts

.tobyte:
 ldx .arg_i
 ldy .out_i

 lda .args,x
 lsr lsr lsr lsr
 tax
 lda .hex,x

 sta screen,y
 iny

 ldx .arg_i
 lda .args,x
 and #%0000_1111
 tax
 lda .hex,x

 sta screen,y
 iny

 sty .out_i
rts

.hex:
.text "0123456789ABCDEF"
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

.org $fffa
.word 0, res, 0