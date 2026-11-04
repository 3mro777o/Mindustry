import arc.graphics.g2d.*;
import arc.graphics.g2d.Font.*;
import arc.scene.ui.*;
import arc.scene.ui.TextField.*;
public class CaretCheck{
    static class TF extends TextField{
        TF(TextFieldStyle s){ super("", s); }
        String dump(){
            StringBuilder sb = new StringBuilder("rtl=" + rtlText + " base=" + (rtlBase ? "R" : "L") + " carets:");
            for(int i = 0; i < caretPositions.size; i++) sb.append(" ").append(i).append(":").append((int)caretPositions.get(i));
            return sb.toString();
        }
        boolean fwd(boolean right){ return arrowForward(right); }
    }
    public static void main(String[] a){
        arc.Core.app = new arc.mock.MockApplication(); arc.Core.input = new arc.mock.MockInput(); arc.Core.graphics = new arc.mock.MockGraphics();
        FontData data = new FontData(){
            @Override public Glyph getGlyph(char ch){
                Glyph g = super.getGlyph(ch);
                if(g == null){ g = new Glyph(); g.id = ch; boolean m = ArabicShaper.isMark(ch); g.xadvance = m ? 0 : 10; g.width = m ? 4 : 10; setGlyph(ch, g); }
                return g;
            }
            @Override public boolean hasGlyph(char ch){ return true; }
        };
        Font font = new Font(data, new TextureRegion(), true){};
        data.spaceXadvance = 10; data.down = -20;
        TextFieldStyle st = new TextFieldStyle(); st.font = font; st.fontColor = arc.graphics.Color.white;
        TF tf = new TF(st);
        for(String s : new String[]{"أنا قادم الآن", "abc", "سلام", "لا بأس", "ابنِ", "مرحبا hi 12", "hi مرحبا"}){
            tf.setText(s);
            System.out.println("[" + s + "] " + tf.dump() + "  leftArrowForward=" + tf.fwd(false));
        }
    }
}
