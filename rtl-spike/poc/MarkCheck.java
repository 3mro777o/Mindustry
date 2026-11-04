import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.graphics.g2d.Font.*;
import arc.util.Align;
public class MarkCheck{
    public static void main(String[] a){
        FontData data = new FontData(){
            @Override public Glyph getGlyph(char ch){
                Glyph g = super.getGlyph(ch);
                if(g == null){ g = new Glyph(); g.id = ch; boolean m = ArabicShaper.isMark(ch); g.xadvance = m ? 0 : 10; g.width = m ? 4 : 10; setGlyph(ch, g); }
                return g;
            }
        };
        Font font = new Font(data, new TextureRegion(), true){};
        data.markupEnabled = true; data.spaceXadvance = 10; data.down = -20;
        for(String s : new String[]{"ابنِ", "مُحَمَّد", "[accent]ابنِ: [][lightgray]1[]x", "Build ابنِ now", "لَا"}){
            GlyphLayout l = new GlyphLayout();
            l.setText(font, s, 0, s.length(), Color.white, 0, Align.left, false, null);
            StringBuilder sb = new StringBuilder(s + "  ->  ");
            for(GlyphLayout.GlyphRun r : l.runs){
                float x = r.x;
                for(int i = 0; i < r.glyphs.size; i++){ x += r.xAdvances.get(i); Glyph g = r.glyphs.get(i); sb.append(String.format("%s@%.0f ", ArabicShaper.isMark((char)g.id) ? "◌" + (char)g.id : String.valueOf((char)g.id), x)); }
                sb.append("| ");
            }
            System.out.println(sb + " width=" + l.width);
        }
    }
}
