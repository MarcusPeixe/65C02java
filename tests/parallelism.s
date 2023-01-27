;*
Set Periodic IRQs to something greater
than zero and Speed to something
greater than 10. Each coloured strip
is a different "thread".
*;

default_status	=	%0010_0000
default_flags	=	%01_000
flipped_flags	=	%01_000

screen		=	$0400

; ======================== 

	.org	$0800
res:
; brk brk
	lda	#<scheduler.processes
	sta	scheduler.process_index
	lda	#>scheduler.processes
	sta	scheduler.process_index + 1

	wai

; ========================

task1:
.addr		=	screen + $000
.fill		=	%001 | default_flags 
	lda	#.fill

.setup:
	ldx	#0

.loop:
	sta	.addr,x
	inx
	cpx	#$80
	bne	.loop
	
	eor	#flipped_flags 
	bra	.setup

; ========================

task2:
.addr		=	screen + $080
.fill		=	%011 | default_flags 
	lda	#.fill

.setup:
	ldx	#0

.loop:
	sta	.addr,x
	inx
	cpx	#$80
	bne	.loop
	
	eor	#flipped_flags 
	bra	.setup

; ========================

task3:
.addr		=	screen + $100
.fill		=	%010 | default_flags 
	lda	#.fill

.setup:
	ldx	#0

.loop:
	sta	.addr,x
	inx
	cpx	#$80
	bne	.loop
	
	eor	#flipped_flags 
	bra	.setup

; ========================

task4:
.addr		=	screen + $180
.fill		=	%110 | default_flags 
	lda	#.fill

.setup:
	ldx	#0

.loop:
	sta	.addr,x
	inx
	cpx	#$80
	bne	.loop
	
	eor	#flipped_flags 
	bra	.setup

; ========================

task5:
.addr		=	screen + $200
.fill		=	%100 | default_flags 
	lda	#.fill

.setup:
	ldx	#0

.loop:
	sta	.addr,x
	inx
	cpx	#$80
	bne	.loop
	
	eor	#flipped_flags 
	bra	.setup

; ========================

task6:
.addr		=	screen + $280
.fill		=	%101 | default_flags 
	lda	#.fill

.setup:
	ldx	#0

.loop:
	sta	.addr,x
	inx
	cpx	#$80
	bne	.loop
	
	eor	#flipped_flags 
	bra	.setup

; ========================

task7:
.addr		=	screen + $300
.fill		=	%000 | default_flags 
	lda	#.fill

.setup:
	ldx	#0

.loop:
	sta	.addr,x
	inx
	cpx	#$80
	bne	.loop
	
	eor	#flipped_flags 
	bra	.setup

; ========================

task8:
.addr		=	screen + $380
.fill		=	%111 | default_flags 
	lda	#.fill

.setup:
	ldx	#0

.loop:
	sta	.addr,x
	inx
	cpx	#$80
	bne	.loop
	
	eor	#flipped_flags 
	bra	.setup

; ========================

irq: scheduler:
.sizeof_process		=	6
.processes_length 	=	8
.process_index		=	$00
	
	phy
	phx
	pha

; STORE CURRENT PROCESS

	ldy	#0
.freeze_current:
	lda	$0200 - .sizeof_process,y
	sta	(.process_index),y
	iny
	cpy	#.sizeof_process
	bne	.freeze_current

; INCREMENT PROCESS INDEX

	clc
	lda	.process_index
	adc	#<.sizeof_process
	sta	.process_index
	lda	.process_index + 1
	adc	#>.sizeof_process
	sta	.process_index + 1

; IF END THEN GO BACK

	lda	.process_index
	cmp	#<(.processes
		+ .processes_length
		* .sizeof_process)
	bne	.skip

	lda	.process_index + 1
	cmp	#>(.processes
		+ .processes_length
		* .sizeof_process)
	bne	.skip

; GO TO START OF PROCESS ARRAY

	lda	#<.processes
	sta	.process_index
	lda	#>.processes
	sta	.process_index + 1

.skip:

; LOAD NEXT PROCESS

	ldy	#0
.load_new:
	lda	(.process_index),y
	sta	$0200 - .sizeof_process,y
	iny
	cpy	#.sizeof_process
	bne	.load_new

	pla
	plx
	ply

;	wai
	rti

; ========================

	.org	$1000
.processes:

	.byte	0, 0, 0, default_status 
	.word	task1

	.byte	0, 0, 0, default_status 
	.word	task2

	.byte	0, 0, 0, default_status 
	.word	task3

	.byte	0, 0, 0, default_status 
	.word	task4

	.byte	0, 0, 0, default_status 
	.word	task5

	.byte	0, 0, 0, default_status 
	.word	task6

	.byte	0, 0, 0, default_status 
	.word	task7

	.byte	0, 0, 0, default_status 
	.word	task8

; ========================

	.org	$fffa
	.word	0, res, irq