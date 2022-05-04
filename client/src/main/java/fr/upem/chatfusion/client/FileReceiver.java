package fr.upem.chatfusion.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public record FileReceiver(int transferID, String srcNickname, String dstNickname, String filename) {



    public boolean createFile() {
        var file = new File(dstNickname+"-"+filename);
        try {
            if (file.createNewFile()) {
                System.out.println("file created");
                return true;
            }
        } catch (IOException e) {
            return false;
        }
        return false;
    }


    public boolean writeChunk(byte[] chunk) {
        try {
            var fos= new FileOutputStream(dstNickname+"-"+filename, true);
            fos.write(chunk);
            fos.close();
            return true;
        } catch (FileNotFoundException e) {
            System.out.println("file not found ! ");
            return false;
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }



}
