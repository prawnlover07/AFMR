package dev.animal.afmr.library;

import dev.animal.afmr.audio.*;
import dev.animal.afmr.core.Library;
import dev.animal.afmr.core.TrackLoudness;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** One explicit batch. Homogeneous lanes own their decoder/meter/buffer and disjoint result slots. */
public final class LoudnessScan implements AutoCloseable {
    private final int laneCount=Math.max(1,Math.min(2,Runtime.getRuntime().availableProcessors()-2));
    private final ExecutorService workers=Executors.newFixedThreadPool(laneCount,r->{
        var t=new Thread(r,"AFMR loudness");t.setDaemon(true);t.setPriority(Thread.MIN_PRIORITY);return t;
    });
    private Job current;
    public enum Source { SCANNED, CACHED, TAGS, FAILED }
    public record Request(String id,String path,String title,TrackLoudness cached) {}
    public record Row(Request track,Source source,double requestedDb,double appliedDb,TrackLoudness measurement,String detail) {
        public boolean limited(){return source!=Source.FAILED&&appliedDb<requestedDb-0.005;}
    }
    public record Progress(String title,double seconds) {}
    public record Report(String playlist,List<Row> rows,int total,boolean cancelled) {
        public long count(Source source){return rows.stream().filter(r->r.source()==source).count();}
    }
    /** Captured only at the explicit UI action, using playlist assignments and the track index. */
    public static List<Request> requests(Library library,Library.Playlist playlist) {
        List<Request> result=new ArrayList<>();
        for(String id:playlist.tracks.keySet()){
            var t=library.track(id);if(t!=null&&!t.vanilla)result.add(new Request(t.id,t.path,t.title,t.loudness));
        }
        return List.copyOf(result);
    }
    public Job start(String playlist,List<Request> requests,boolean allowed) {
        if(current!=null&&current.report()==null)throw new IllegalStateException("A loudness scan is already running");
        Job job=new Job(playlist,List.copyOf(requests),laneCount,allowed);current=job;
        for(int lane=0;lane<laneCount;lane++){int index=lane;workers.execute(()->job.runLane(index));}
        return job;
    }
    @Override public void close(){
        if(current!=null)current.cancel();workers.shutdown();
        try{workers.awaitTermination(2,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}
    }

    public static final class Job {
        private final String playlist;
        private final List<Request> requests;
        private final Row[] rows;
        private final AtomicInteger next=new AtomicInteger(),completed=new AtomicInteger(),remaining;
        private final AtomicReferenceArray<Progress> progress;
        private volatile boolean cancelled,allowed;
        private volatile Report report;
        private Job(String playlist,List<Request> requests,int lanes,boolean allowed){
            this.playlist=playlist;this.requests=requests;rows=new Row[requests.size()];remaining=new AtomicInteger(lanes);
            progress=new AtomicReferenceArray<>(lanes);this.allowed=allowed;
        }
        public String playlist(){return playlist;}
        public int total(){return requests.size();}
        public int completed(){return completed.get();}
        public int lanes(){return progress.length();}
        public Progress progress(int lane){return progress.get(lane);}
        public Report report(){return report;}
        public void cancel(){cancelled=true;}
        public boolean cancelled(){return cancelled;}
        public void setAllowed(boolean value){allowed=value;}
        public boolean allowed(){return allowed;}
        private void checkpoint(){
            while(true){
                if(cancelled||Thread.currentThread().isInterrupted())throw new CancellationException();
                if(allowed)return;
                try{Thread.sleep(25);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new CancellationException();}
            }
        }
        private void runLane(int lane){
            // One allocation per lane per explicit batch, independent of song length or library size.
            LoudnessMeter meter=new LoudnessMeter();byte[] buffer=new byte[16384];
            try{
                while(true){
                    checkpoint();int index=next.getAndIncrement();if(index>=requests.size())break;
                    Request request=requests.get(index);progress.set(lane,new Progress(request.title(),0));
                    try{rows[index]=analyze(request,lane,meter,buffer);}
                    catch(CancellationException e){throw e;}
                    catch(Exception e){rows[index]=new Row(request,Source.FAILED,0,0,null,e.getMessage()==null?e.getClass().getSimpleName():e.getMessage());}
                    completed.incrementAndGet();
                }
            }catch(CancellationException ignored){cancelled=true;}
            finally{
                progress.set(lane,null);
                // The last lane combines only after all disjoint writes have been published.
                if(remaining.decrementAndGet()==0){
                    List<Row> done=new ArrayList<>(completed.get());for(Row row:rows)if(row!=null)done.add(row);
                    report=new Report(playlist,List.copyOf(done),rows.length,cancelled);
                }
            }
        }
        private Row analyze(Request request,int lane,LoudnessMeter meter,byte[] buffer) throws Exception {
            Path path=Path.of(request.path());var before=Files.readAttributes(path,BasicFileAttributes.class);
            if(!before.isRegularFile())throw new IOException("File missing or unavailable");
            ReplayGain.Gain gain=ReplayGain.read(path);Source source=Source.TAGS;TrackLoudness measurement=null;
            checkpoint();
            if(gain==null){
                TrackLoudness cached=request.cached();
                if(cached!=null&&cached.matches(before.size(),before.lastModifiedTime().toString())){
                    source=Source.CACHED;gain=new ReplayGain.Gain(cached.gainDb(),cached.peak());
                }else{
                    source=Source.SCANNED;
                    try(var pcm=PcmDecoder.open(path)){
                        meter.reset(pcm.getFormat());long lastUpdate=0;
                        while(true){
                            checkpoint();int n=pcm.read(buffer,0,buffer.length);if(n<0)break;if(n==0)continue;
                            meter.add(buffer,n);long now=System.nanoTime();
                            if(now-lastUpdate>=100_000_000L){lastUpdate=now;progress.set(lane,new Progress(request.title(),meter.frames()/pcm.getFormat().getSampleRate()));}
                        }
                        var measured=meter.finish();gain=measured.gain();
                        measurement=new TrackLoudness(before.size(),before.lastModifiedTime().toString(),gain.db(),gain.peak());
                    }
                }
            }
            checkpoint();var after=Files.readAttributes(path,BasicFileAttributes.class);
            if(before.size()!=after.size()||!before.lastModifiedTime().equals(after.lastModifiedTime()))
                throw new IOException("File changed during analysis; run normalization again");
            String detail=gain.peak()>0?"":"No peak tag: boosts capped at 0 dB";
            return new Row(request,source,gain.db(),gain.effectiveDb(),measurement,detail);
        }
    }
}
