import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.graphics.g2d.Font.*;
import arc.util.Align;
public class AllocEach{
    public static void main(String[] a){
        FontData data = new FontData(){
            @Override public Glyph getGlyph(char ch){
                Glyph g = super.getGlyph(ch);
                if(g == null){ g = new Glyph(); g.id = ch; g.xadvance = 10; g.width = 10; setGlyph(ch, g); }
                return g;
            }
        };
        Font font = new Font(data, new TextureRegion(), true){};
        data.markupEnabled = true; data.spaceXadvance = 10; data.down = -20;
        String[] texts = {"Hello world", "[red]Play[] now", "مرحبا بالعالم 123", "ابدأ (Play) الآن", "[red]العب[] الآن و[blue]ل[]ا", "هذا نص طويل جدا يجب أن ينطوي على عدة أسطر هنا", "[accent]ابن: [][lightgray]1[]x\nحفار", "الطاقة: 45% (+12/ث) Mindustry v7", "ا", "مُحَمَّدٌ يَبْنِي قَاعِدَةً. ابنِ الآن"};
        GlyphLayout l = new GlyphLayout();
        com.sun.management.ThreadMXBean mx = (com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();
        long id = Thread.currentThread().getId();
        for(String s : texts){
            for(boolean wrap : new boolean[]{false, true}){
                for(int i = 0; i < 20000; i++) l.setText(font, s, 0, s.length(), Color.white, 120, Align.left, wrap, null);
                long b = mx.getThreadAllocatedBytes(id);
                for(int i = 0; i < 50000; i++) l.setText(font, s, 0, s.length(), Color.white, 120, Align.left, wrap, null);
                System.out.printf("%6.1f B  wrap=%s  %s%n", (mx.getThreadAllocatedBytes(id) - b) / 50000.0, wrap, s);
            }
        }
    }
}
