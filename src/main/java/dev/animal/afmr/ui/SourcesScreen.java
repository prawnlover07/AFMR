package dev.animal.afmr.ui;
import dev.animal.afmr.ClientRuntime;
import net.minecraft.client.gui.screens.Screen;
final class SourcesScreen extends MusicScreen {
    private int page,lastCount;
    SourcesScreen(Screen parent,ClientRuntime runtime){super("Linked music folders",parent,runtime);}
    @Override protected void init(){
        super.init();button("Add folder...",left,47,112,()->NativePicker.open(runtime,NativePicker.Kind.FOLDER,"library"));button("Add files...",left+118,47,100,()->NativePicker.open(runtime,NativePicker.Kind.FILES,"library"));button("Rescan all",left+224,47,100,runtime::rescan);
        var unique=new java.util.LinkedHashMap<String,dev.animal.afmr.core.Library.Folder>();runtime.library().folders.forEach(f->unique.putIfAbsent(f.path,f));
        int size=pageSize(78,72);var list=new java.util.ArrayList<>(unique.values());page=Math.min(page,Math.max(0,(list.size()-1)/size));lastCount=runtime.library().folders.size();
        for(int i=page*size;i<Math.min(list.size(),(page+1)*size);i++){var f=list.get(i);button(fit(f.path,contentWidth-12),left,78+(i-page*size)*22,contentWidth,()->minecraft.gui.setScreen(new FolderScreen(this,runtime,f)));}
        pages(page,size,list.size(),height-53,p->{page=p;rebuildWidgets();});button("Back",left+contentWidth-104,height-29,104,this::onClose);
    }
    @Override public void tick(){if(lastCount!=runtime.library().folders.size())rebuildWidgets();}
}
