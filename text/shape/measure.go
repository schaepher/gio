// SPDX-License-Identifier: Unlicense OR MIT

/*
Package shape implements text layout and shaping.
*/
package shape

import (
	"math"
	"unicode"
	"unicode/utf8"

	"gioui.org/f32"
	"gioui.org/op"
	"gioui.org/op/paint"
	"gioui.org/text"
	"golang.org/x/image/font"
	"golang.org/x/image/font/sfnt"
	"golang.org/x/image/math/fixed"
)

// Family is an implementation of text.Family. It caches
// layouts and paths.
// A Family must specify at least the Regular font to be useful.
type Family struct {
	Regular *sfnt.Font
	Italic  *sfnt.Font
	Bold    *sfnt.Font

	layoutCache layoutCache
	pathCache   pathCache
	buf         sfnt.Buffer
}

// for returns a font for the given face.
func (f *Family) fontFor(face text.Face) *sfnt.Font {
	var font *sfnt.Font
	switch {
	case face.Style == text.Italic:
		font = f.Italic
	case face.Weight >= 600:
		font = f.Bold
	}
	if font == nil {
		font = f.Regular
	}
	return font
}

func (f *Family) Layout(face text.Face, size float32, str string, opts text.LayoutOptions) *text.Layout {
	fnt := f.fontFor(face)
	ppem := fixed.Int26_6(size * 64)
	lk := layoutKey{
		f:    fnt,
		ppem: ppem,
		str:  str,
		opts: opts,
	}
	if l, ok := f.layoutCache.Get(lk); ok {
		return l
	}
	l := layoutText(&f.buf, ppem, str, &opentype{Font: fnt, Hinting: font.HintingFull}, opts)
	f.layoutCache.Put(lk, l)
	return l
}

func (f *Family) Shape(face text.Face, size float32, str text.String) op.MacroOp {
	fnt := f.fontFor(face)
	ppem := fixed.Int26_6(size * 64)
	pk := pathKey{
		f:    fnt,
		ppem: ppem,
		str:  str.String,
	}
	if p, ok := f.pathCache.Get(pk); ok {
		return p
	}
	p := textPath(&f.buf, ppem, &opentype{Font: fnt, Hinting: font.HintingFull}, str)
	f.pathCache.Put(pk, p)
	return p
}

func layoutText(buf *sfnt.Buffer, ppem fixed.Int26_6, str string, f *opentype, opts text.LayoutOptions) *text.Layout {
	m := f.Metrics(buf, ppem)
	lineTmpl := text.Line{
		Ascent: m.Ascent,
		// m.Height is equal to m.Ascent + m.Descent + linegap.
		// Compute the descent including the linegap.
		Descent: m.Height - m.Ascent,
		Bounds:  f.Bounds(buf, ppem),
	}
	var lines []text.Line
	maxDotX := fixed.Int26_6(math.MaxInt32)
	maxDotX = fixed.I(opts.MaxWidth)
	type state struct {
		r     rune
		advs  []fixed.Int26_6
		adv   fixed.Int26_6
		x     fixed.Int26_6
		idx   int
		valid bool
	}
	var prev, word state
	endLine := func() {
		line := lineTmpl
		line.Text.Advances = prev.advs
		line.Text.String = str[:prev.idx]
		line.Width = prev.x + prev.adv
		line.Bounds.Max.X += prev.x
		lines = append(lines, line)
		str = str[prev.idx:]
		prev = state{}
		word = state{}
	}
	for prev.idx < len(str) {
		c, s := utf8.DecodeRuneInString(str[prev.idx:])
		nl := c == '\n'
		if opts.SingleLine && nl {
			nl = false
			c = ' '
			s = 1
		}
		a, ok := f.GlyphAdvance(buf, ppem, c)
		if !ok {
			prev.idx += s
			continue
		}
		next := state{
			r:     c,
			advs:  prev.advs,
			idx:   prev.idx + s,
			x:     prev.x + prev.adv,
			valid: true,
		}
		if nl {
			// The newline is zero width; use the previous
			// character for line measurements.
			prev.advs = append(prev.advs, 0)
			prev.idx = next.idx
			endLine()
			continue
		}
		next.adv = a
		var k fixed.Int26_6
		if prev.valid {
			k = f.Kern(buf, ppem, prev.r, next.r)
		}
		// Break the line if we're out of space.
		if prev.idx > 0 && next.x+next.adv+k >= maxDotX {
			// If the line contains no word breaks, break off the last rune.
			if word.idx == 0 {
				word = prev
			}
			next.x -= word.x + word.adv
			next.idx -= word.idx
			next.advs = next.advs[len(word.advs):]
			prev = word
			endLine()
		} else {
			next.adv += k
		}
		next.advs = append(next.advs, next.adv)
		if unicode.IsSpace(next.r) {
			word = next
		}
		prev = next
	}
	endLine()
	return &text.Layout{Lines: lines}
}

func textPath(buf *sfnt.Buffer, ppem fixed.Int26_6, f *opentype, str text.String) op.MacroOp {
	var lastPos f32.Point
	var builder paint.Path
	ops := new(op.Ops)
	var x fixed.Int26_6
	var advIdx int
	var m op.MacroOp
	m.Record(ops)
	builder.Begin(ops)
	for _, r := range str.String {
		if !unicode.IsSpace(r) {
			segs, ok := f.LoadGlyph(buf, ppem, r)
			if !ok {
				continue
			}
			// Move to glyph position.
			pos := f32.Point{
				X: float32(x) / 64,
			}
			builder.Move(pos.Sub(lastPos))
			lastPos = pos
			var lastArg f32.Point
			// Convert sfnt.Segments to relative segments.
			for _, fseg := range segs {
				nargs := 1
				switch fseg.Op {
				case sfnt.SegmentOpQuadTo:
					nargs = 2
				case sfnt.SegmentOpCubeTo:
					nargs = 3
				}
				var args [3]f32.Point
				for i := 0; i < nargs; i++ {
					a := f32.Point{
						X: float32(fseg.Args[i].X) / 64,
						Y: float32(fseg.Args[i].Y) / 64,
					}
					args[i] = a.Sub(lastArg)
					if i == nargs-1 {
						lastArg = a
					}
				}
				switch fseg.Op {
				case sfnt.SegmentOpMoveTo:
					builder.Move(args[0])
				case sfnt.SegmentOpLineTo:
					builder.Line(args[0])
				case sfnt.SegmentOpQuadTo:
					builder.Quad(args[0], args[1])
				case sfnt.SegmentOpCubeTo:
					builder.Cube(args[0], args[1], args[2])
				default:
					panic("unsupported segment op")
				}
			}
			lastPos = lastPos.Add(lastArg)
		}
		x += str.Advances[advIdx]
		advIdx++
	}
	builder.End()
	m.Stop()
	return m
}