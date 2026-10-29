package dev.animal.afmr.probe;
import dev.animal.afmr.audio.PcmDecoder;
import java.nio.file.*;
import java.util.*;
import javax.sound.sampled.AudioFormat;
/** Exercise provider discovery through the actual packaged mod's class loader. */
final class DecoderChecks {
 static void run() throws Exception {
  var results=new ArrayList<String>();
  for(String name:List.of("test.mp3","test.ogg","test.flac","test.wav","test24.flac","test24.wav","testfloat.wav")){
   try(var pcm=PcmDecoder.open(Path.of("../src/test/resources/audio",name))){
    var format=pcm.getFormat();var bytes=pcm.readNBytes(4096);
    if(bytes.length==0||format.getSampleSizeInBits()!=16||!format.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED))throw new AssertionError(name+" did not produce signed 16-bit PCM");
    results.add("PASS packaged decoder "+name);
   }
  }
  Files.write(Path.of("packaged-codec-results.txt"),results);
 }
}
