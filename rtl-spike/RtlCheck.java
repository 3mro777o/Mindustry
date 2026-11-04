import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.graphics.g2d.Font.*;
import arc.util.Align;

public class RtlCheck{
    static FontData data; static Font font;
    static String layout(String s, float width, boolean wrap, int align){
        GlyphLayout l = new GlyphLayout();
        l.setText(font, s, 0, s.length(), Color.white, width, align, wrap, null);
        StringBuilder sb = new StringBuilder();
        float lastY = Float.NaN;
        for(GlyphLayout.GlyphRun r : l.runs){
            if(r.y != lastY){ if(sb.length()>0) sb.append(" | "); sb.append("x=").append((int)r.x).append(": "); lastY = r.y; }
            sb.append("{"+r.color+"}"); for(Glyph g : r.glyphs) sb.append((char)g.id);
        }
        return sb.toString();
    }
    public static void main(String[] a){
        data = new FontData(){
            @Override public Glyph getGlyph(char ch){
                Glyph g = super.getGlyph(ch);
                if(g == null){ g = new Glyph(); g.id = ch; g.xadvance = 10; g.width = 10; setGlyph(ch, g); }
                return g;
            }
        };
        font = new Font(data, new TextureRegion(), true){};
        data.markupEnabled = true;
        data.spaceXadvance = 10;
        data.down = -20;
        // Shaping only
        StringBuilder sh = new StringBuilder(); ArabicShaper.shape("مرحبا بالعالم", 0, 13, false, sh); System.out.println("shape: " + sh);
        System.out.println("1: " + layout("مرحبا بالعالم 123", 0, false, Align.left));
        System.out.println("2: " + layout("ابدأ (Play) الآن", 0, false, Align.left));
        System.out.println("3: " + layout("[red]العب[] الآن و[blue]ل[]ا", 0, false, Align.left));
        System.out.println("4: " + layout("هذا نص طويل جدا يجب أن ينطوي", 100, true, Align.left));
        System.out.println("5: " + layout("hello world", 0, false, Align.left));
        System.out.println("7: " + layout("[accent]ابن: [][lightgray]1[]x\nحفار", 0, false, Align.left));
        System.out.println("8: " + layout("ابن:\n[lightgray]1[]x\nحفار", 0, false, Align.left));
        System.out.println("6: " + layout("[red]اختر عليه.", 0, false, Align.left));
    }
}
