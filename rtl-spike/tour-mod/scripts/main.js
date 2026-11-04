// RTL UI tour: opens every screen, takes a screenshot of each, then exits.
const ui = Vars.ui, control = Vars.control, logic = Vars.logic;
const tfHolder = {};
const out = Core.settings.getDataDirectory().child("rtlshots");
out.mkdirs();
const log = out.child("log.txt");
log.writeString("start\n", false);
function note(s){ log.writeString(s + "\n", true); }

let pending = null;
Events.run(Trigger.uiDrawEnd, () => {
    if(pending == null) return;
    const name = pending;
    pending = null;
    try{
        const w = Core.graphics.getWidth(), h = Core.graphics.getHeight();
        const pix = Packages.arc.util.ScreenUtils.getFrameBufferPixmap(0, 0, w, h, true);
        Packages.arc.graphics.PixmapIO.writePng(out.child(name + ".png"), pix);
        pix.dispose();
        note("shot " + name);
    }catch(e){ note("shotfail " + name + " " + e); }
});

function hideAll(){
    try{
        const dialogs = Core.scene.root.getChildren().copy();
        dialogs.each(d => { if(d instanceof Packages.arc.scene.ui.Dialog) d.hide(null); });
    }catch(e){ note("hideAll " + e); }
}

function settingsTab(i){
    ui.settings.show();
    Packages.arc.util.Reflect.invoke(Packages.mindustry.ui.dialogs.SettingsMenuDialog, ui.settings, "visible",
        [new java.lang.Integer(i)], [java.lang.Integer.TYPE]);
}

// [name, action, waitSeconds]
const steps = [
    ["01_menu", () => {}, 3],
    ["02_planet", () => ui.planet.show(), 3],
    ["03_settings", () => ui.settings.show(), 1.5],
    ["04_settings_game", () => settingsTab(0), 1.5],
    ["05_settings_graphics", () => settingsTab(1), 1.5],
    ["06_settings_sound", () => settingsTab(2), 1.5],
    ["07_controls", () => ui.controls.show(), 1.5],
    ["08_language", () => ui.language.show(), 1.5],
    ["09_database", () => ui.database.show(), 2],
    ["10_info_duo", () => ui.content.show(Blocks.duo), 1.5],
    ["11_info_dagger", () => ui.content.show(UnitTypes.dagger), 1.5],
    ["12_info_copper", () => ui.content.show(Items.copper), 1.5],
    ["13_research", () => ui.research.show(), 2],
    ["14_schematics", () => ui.schematics.show(), 1.5],
    ["15_mods", () => ui.mods.show(), 2],
    ["16_about", () => ui.about.show(), 1.5],
    ["17_load", () => ui.load.show(), 1.5],
    ["18_join", () => ui.join.show(), 2],
    ["19_custom", () => ui.custom.show(), 2],
    ["20_maps", () => ui.maps.show(), 1.5],
    ["21_confirm", () => ui.showConfirm("@confirm", "@settings.clear.confirm", () => {}), 1],
    ["22_info", () => ui.showInfo("@campaign.serpulo"), 1],
    ["23_logic", () => ui.logic.show("print \"hello\"", null, false, s => {}), 2],
    ["24_textfields", () => {
        const d = new Packages.mindustry.ui.dialogs.BaseDialog("اختبار خانات الكتابة");
        const f = d.cont.field("مرحبا بالعالم 123 لا", s => {}).width(420).get();
        d.cont.row();
        d.cont.field("Hello world", s => {}).width(420);
        d.cont.row();
        d.cont.area("هذا نص طويل في خانة متعددة الأسطر\nسطر ثان مع English و 456", s => {}).size(420, 140);
        d.addCloseButton();
        d.show();
        Core.scene.setKeyboardFocus(f);
        f.setCursorPosition(f.getText().length);
        tfHolder.f = f;
    }, 1.5],
    ["25_textfield_typing", () => {
        const f = tfHolder.f;
        f.appendText(" لالا");
        f.setCursorPosition(f.getText().length);
        f.selectAll();
    }, 1.5],
    ["26_objective_labels", () => {
        const d = new Packages.mindustry.ui.dialogs.BaseDialog("objectives");
        const texts = [
            Core.bundle.format("objective.build", 1, Blocks.plasmaBore.emoji(), Blocks.plasmaBore.localizedName),
            Core.bundle.format("objective.item", 0, 60, Items.beryllium.emoji(), Items.beryllium.localizedName),
            Core.bundle.format("objective.buildunit", 3, UnitTypes.stell.emoji(), UnitTypes.stell.localizedName),
        ];
        texts.forEach(t => { d.cont.table(Tex.buttonEdge3, tb => tb.labelWrap(t).width(170).pad(8)).width(200).pad(6); d.cont.row(); });
        d.addCloseButton();
        d.show();
    }, 1.5],
    ["30_game_serpulo", () => { ui.logic.hide(); hideAll(); control.playSector(SectorPresets.groundZero.sector); }, 8],
    ["31_place_conveyor", () => { control.input.block = Blocks.conveyor; }, 1.5],
    ["32_place_duo", () => { control.input.block = Blocks.duo; }, 1.5],
    ["33_paused", () => { control.input.block = null; ui.paused.show(); }, 1.5],
    ["34_research_ingame", () => ui.research.show(), 2],
    ["35_chat", () => { ui.chatfrag.toggle(); }, 1],
    ["35b_chat_messages", () => {
        ui.chatfrag.addMessage("[accent]لاعب[]: مرحبا بكم في السيرفر 123");
        ui.chatfrag.addMessage("[coral]Player[]: hello everyone");
        ui.chatfrag.addMessage("[accent]أحمد[]: لنبني ناقلات (conveyors) إلى النواة");
    }, 1],
    ["36_toast", () => { ui.chatfrag.toggle(); ui.hudfrag.showToast("@sector.capture"); ui.announce("@sector.curcapture"); }, 1],
    ["37_gameover", () => ui.restart.show(Team.crux), 2],
    ["40_game_erekir", () => { logic.reset(); control.playSector(SectorPresets.onset.sector); }, 8],
    ["41_place_erekir", () => { control.input.block = Blocks.turbineCondenser; }, 1.5],
    ["42_core_info", () => { control.input.block = Blocks.coreBastion; }, 1.5],
];

let t = 0;
// wall-clock timer (game Time stops while paused), executed on the main thread
function later(sec, fn){ Packages.arc.util.Timer.schedule(() => Core.app.post(fn), sec); }
Events.on(ClientLoadEvent, () => {
    note("loaded, locale=" + java.util.Locale.getDefault());
    steps.forEach(s => {
        const name = s[0], action = s[1], wait = s[2];
        later(t, () => {
            if(!name.startsWith("3") && !name.startsWith("4") && name != "01_menu" && name != "25_textfield_typing") hideAll();
            if(name == "34_research_ingame" || name == "35_chat" || name == "37_gameover" || name == "40_game_erekir") hideAll();
            try{ action(); note("ok " + name); }catch(e){ note("fail " + name + " " + e); }
        });
        later(t + wait, () => { pending = name; });
        t += wait + 0.5;
    });
    later(t + 1, () => { note("done"); Core.app.exit(); });
});
