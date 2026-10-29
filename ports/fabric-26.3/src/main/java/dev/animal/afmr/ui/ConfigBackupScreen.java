package dev.animal.afmr.ui;

import dev.animal.afmr.Afmr;
import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.MenuColor;
import dev.animal.afmr.library.ConfigBackup;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Whole-install transfer, separate from additive playlist sharing. */
final class ConfigBackupScreen extends MusicScreen {
    private FlatButton export,open,apply;
    private ConfigBackup.Data shown;
    private int unavailable;
    ConfigBackupScreen(Screen parent,ClientRuntime runtime){super("Full configuration",parent,runtime);}
    @Override protected void init(){
        super.init();int half=(contentWidth-6)/2;
        export=button("Export full backup...",left,48,half,()->NativePicker.open(runtime,NativePicker.Kind.BACKUP_EXPORT,""));
        var exportFullBackupHelpText=Component.translatable("afmr.copy.configbackup.export_full_backup");
        if(!exportFullBackupHelpText.getString().isBlank())export.setTooltip(Tooltip.create(exportFullBackupHelpText));
        open=button("Import full backup...",left+half+6,48,half,()->NativePicker.open(runtime,NativePicker.Kind.BACKUP_IMPORT,""));
        var importFullBackupHelpText=Component.translatable("afmr.copy.configbackup.import_full_backup");
        if(!importFullBackupHelpText.getString().isBlank())open.setTooltip(Tooltip.create(importFullBackupHelpText));
        apply=button("Replace on restart",left,bodyBottom()-29,half,()->runtime.applyBackupImport(runtime.pendingBackup()));
        var replaceOnRestartHelpText=Component.translatable("afmr.copy.configbackup.replace_on_restart");
        if(!replaceOnRestartHelpText.getString().isBlank())apply.setTooltip(Tooltip.create(replaceOnRestartHelpText));
        button("Back",left+half+6,bodyBottom()-29,half,this::onClose);updateControls();
    }
    private void updateControls(){
        boolean ready=runtime.backupAvailable()&&!runtime.backupQueued();
        export.active=ready&&runtime.pendingBackup()==null;open.active=export.active;
        apply.active=ready&&runtime.pendingBackup()!=null;
    }
    @Override public void tick(){updateControls();}
    @Override protected void drawBody(GuiGraphicsExtractor g,int x,int y,float delta){
        var data=runtime.pendingBackup();
        if(data!=shown){shown=data;unavailable=data==null?0:Afmr.unavailableBackupKeys(data.keys());}
        if(data!=null){
            var lib=data.library();int playlists=lib.playlists.size();for(var p:lib.profiles.inactive)playlists+=p.playlists.size();
            line(g,(lib.profiles.inactive.size()+1)+" profiles / "+playlists+" playlists",85,MenuColor.TEXT,0xFFEBEEF3);
            line(g,lib.tracks.size()+" songs / "+lib.folders.size()+" folders",101,MenuColor.TEXT,0xFFEBEEF3);
            line(g,data.historyCount()+" history entries / "+data.keys().size()+" keys",117,MenuColor.TEXT,0xFFEBEEF3);
            boolean menu=data.files().containsKey("menu-background.png")||data.files().containsKey("menu-background.gif");
            boolean widget=data.files().containsKey("widget-background.png")||data.files().containsKey("widget-background.gif");
            line(g,"Images: "+(menu?(widget?"menu and widget":"menu"):widget?"widget":"none"),133,MenuColor.TEXT,0xFFEBEEF3);
            if(unavailable>0)g.text(menuFont,fit(unavailable+" unsupported shortcuts will be unbound",contentWidth),left,149,0xFFFF7777);
            g.text(menuFont,fit("Replaces all AFMR settings and library data.",contentWidth),left,bodyBottom()-64,0xFFFF7777);
        }
        line(g,runtime.backupQueued()?"Restart Minecraft to apply the backup.":runtime.taskStatus(),bodyBottom()-47,MenuColor.MUTED_TEXT,0xFFABB5C2);
    }
    private void line(GuiGraphicsExtractor g,String text,int y,MenuColor role,int rgb){g.text(menuFont,fit(text,contentWidth),left,y,MenuAppearance.color(role,rgb));}
    @Override public void onClose(){
        if(runtime.scanning())return;
        var pending=runtime.pendingBackup();if(pending!=null)runtime.cancelBackupImport(pending);
        super.onClose();
    }
}
