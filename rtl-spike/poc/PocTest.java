import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.graphics.g2d.Font.*;
import arc.util.Align;
import java.util.*;

public class PocTest{
    static final String ALPHA = "مرحبالعسلامأإآؤئءةىيكفقشصضطظغخذثجدزوتنهـ";
    static String pick(Random r, String[] parts, int n){
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < n; i++) sb.append(parts[r.nextInt(parts.length)]);
        return sb.toString();
    }
    public static void main(String[] a) throws Exception{
        Random r = new Random(42);
        // 1. Bidi vs java.text.Bidi (test only)
        String[] bidiParts = {"م","ر","ح","ب","ا","a","b","Z"," "," ","1","2","٣","٤","۵",".",",",":","-","+","%","$","#","(",")","[","]","{","}","!","/","'","\"","«","»","\t"};
        SimpleBidi sb = new SimpleBidi();
        int bidiFail = 0, bidiTotal = 0; List<String> fails = new ArrayList<>();
        for(int t = 0; t < 200000; t++){
            String s = pick(r, bidiParts, 1 + r.nextInt(25));
            boolean rtl = r.nextBoolean();
            char[] c = sb.begin(s.length()); s.getChars(0, s.length(), c, 0);
            sb.resolve(rtl); sb.reorder();
            java.text.Bidi jb = new java.text.Bidi(s, rtl ? java.text.Bidi.DIRECTION_RIGHT_TO_LEFT : java.text.Bidi.DIRECTION_LEFT_TO_RIGHT);
            byte[] lv = new byte[s.length()]; Integer[] ord = new Integer[s.length()];
            for(int i = 0; i < s.length(); i++){ lv[i] = (byte)jb.getLevelAt(i); ord[i] = i; }
            java.text.Bidi.reorderVisually(lv, 0, ord, 0, s.length());
            boolean ok = true;
            for(int i = 0; i < s.length(); i++) if(ord[i] != sb.order[i]) ok = false;
            bidiTotal++;
            if(!ok){ bidiFail++; if(fails.size() < 12){
                StringBuilder exp = new StringBuilder(), got = new StringBuilder();
                for(int i = 0; i < s.length(); i++){ exp.append(s.charAt(ord[i])); got.append(s.charAt(sb.order[i])); }
                fails.add((rtl?"RTL ":"LTR ") + "[" + s + "] java=[" + exp + "] ours=[" + got + "]");
            }}
        }
        System.out.println("bidi visual order mismatches: " + bidiFail + " / " + bidiTotal);
        for(String f : fails) System.out.println("  " + f);

        // 2. Shaper vs old prototype shaper
        String[] shapeParts = {"م","ل","ا","أ","إ","آ","ب","ة","ي","ى","و","ر","ع","ـ"," ","a","1","ً","َ","ّ","[red]","[]","[[","لا","لأ"};
        int shapeFail = 0; StringBuilder out = new StringBuilder();
        for(int t = 0; t < 200000; t++){
            String s = pick(r, shapeParts, 1 + r.nextInt(15));
            boolean markup = r.nextBoolean();
            String exp = old.OldShaper.shape(s, 0, s.length(), markup).toString();
            ArabicShaper.shape(s, 0, s.length(), markup, out);
            StringBuilder noMarks = new StringBuilder();
            for(int k = 0; k < out.length(); k++) if(!ArabicShaper.isMark(out.charAt(k))) noMarks.append(out.charAt(k));
            if(!exp.equals(noMarks.toString())){ if(shapeFail++ < 8) System.out.println("  shape diff markup=" + markup + " [" + s + "] old=[" + exp + "] new=[" + out + "]"); }
        }
        System.out.println("shaper mismatches vs prototype (ignoring kept marks): " + shapeFail + " / 200000");

        // 3. Allocation check on full layout
        FontData data = new FontData(){
            @Override public Glyph getGlyph(char ch){
                Glyph g = super.getGlyph(ch);
                if(g == null){ g = new Glyph(); g.id = ch; g.xadvance = 10; g.width = 10; setGlyph(ch, g); }
                return g;
            }
        };
        Font font = new Font(data, new TextureRegion(), true){};
        data.markupEnabled = true; data.spaceXadvance = 10; data.down = -20;
        String[] texts = {"مرحبا بالعالم 123", "ابدأ (Play) الآن", "[red]العب[] الآن و[blue]ل[]ا", "هذا نص طويل جدا يجب أن ينطوي على عدة أسطر هنا", "[accent]ابن: [][lightgray]1[]x\nحفار", "الطاقة: 45% (+12/ث) Mindustry v7"};
        GlyphLayout l = new GlyphLayout();
        com.sun.management.ThreadMXBean mx = (com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();
        for(String rtlOnly : new String[]{"arabic", "english"}){
            String[] use = rtlOnly.equals("arabic") ? texts : new String[]{"Hello world 123", "[red]Play[] now (v7)"};
            for(int i = 0; i < 20000; i++){ String s = use[i % use.length]; l.setText(font, s, 0, s.length(), Color.white, 120, Align.left, true, null); }
            long before = mx.getThreadAllocatedBytes(Thread.currentThread().getId());
            int N = 100000;
            for(int i = 0; i < N; i++){ String s = use[i % use.length]; l.setText(font, s, 0, s.length(), Color.white, 120, Align.left, true, null); }
            long after = mx.getThreadAllocatedBytes(Thread.currentThread().getId());
            System.out.println(rtlOnly + ": bytes allocated per layout after warmup = " + ((after - before) / (double)N));
        }
    }
}
