package dev.animal.afmr.ui;
import dev.animal.afmr.ClientRuntime;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
final class SourcesScreen extends MusicScreen {
    private int page,lastCount;
    private dev.animal.afmr.library.LibraryHealth.Report health;
    SourcesScreen(Screen parent,ClientRuntime runtime){super("Linked music folders",parent,runtime);}
    @Override protected void init(){
        super.init();health=runtime.sourceHealth();int third=(contentWidth-8)/3;
        button("Add folder...",left,47,third,()->NativePicker.open(runtime,NativePicker.Kind.FOLDER,"library"));button("Add files...",left+third+4,47,third,()->NativePicker.open(runtime,NativePicker.Kind.FILES,"library"));button("Rescan all",left+2*(third+4),47,third,runtime::rescan);
        var s=runtime.library().settings;int half=(contentWidth-4)/2;
        var rescanOnLaunchHelpControl=toggle("Rescan on launch",left,73,half,()->s.rescanOnLaunch,()->{s.rescanOnLaunch=!s.rescanOnLaunch;runtime.save();rebuildWidgets();});
        var rescanOnLaunchHelpText=Component.translatable("afmr.copy.sources.rescan_on_launch");
        if(!rescanOnLaunchHelpText.getString().isBlank())rescanOnLaunchHelpControl.setTooltip(Tooltip.create(rescanOnLaunchHelpText));
        var repair=button("Repair / missing"+(health==null?"":" ("+health.issues().size()+")"),left+half+4,73,contentWidth-half-4,()->minecraft.gui.setScreen(new LibraryRepairScreen(this,runtime)));
        if(health!=null&&!health.issues().isEmpty())repair.setLabelTextColor(0xFFFF9A91);
        var unique=new java.util.LinkedHashMap<String,dev.animal.afmr.core.Library.Folder>();runtime.library().folders.forEach(f->unique.putIfAbsent(f.path,f));
        int size=pageSize(104,72);var list=new java.util.ArrayList<>(unique.values());page=Math.min(page,Math.max(0,(list.size()-1)/size));lastCount=runtime.library().folders.size();
        for(int i=page*size;i<Math.min(list.size(),(page+1)*size);i++){var f=list.get(i);button(fit(f.path,contentWidth-12),left,104+(i-page*size)*22,contentWidth,()->minecraft.gui.setScreen(new FolderScreen(this,runtime,f)));}
        pages(page,size,list.size(),bodyBottom()-53,p->{page=p;rebuildWidgets();});button("Back",left+contentWidth-104,bodyBottom()-29,104,this::onClose);
    }
    @Override public void tick(){if(lastCount!=runtime.library().folders.size()||health!=runtime.sourceHealth())rebuildWidgets();}
}
