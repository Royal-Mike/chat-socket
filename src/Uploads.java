import javax.xml.crypto.Data;
import java.io.*;
import java.net.Socket;

public class Uploads {
    private int count;
    public Uploads() throws IOException {
        BufferedReader br = new BufferedReader(new FileReader("./data/uploads/$data.txt"));
        String line = br.readLine();
        count = Integer.parseInt(line);
        br.close();
    }
    public String saveFile(Socket s, String ext) throws IOException {
        count++;
        InputStream is = s.getInputStream();
        FileOutputStream fos = new FileOutputStream("./data/uploads/" + count + "." + ext);

        byte[] buffer = new byte[8196];
        int bytes;
        while ((bytes = is.read(buffer)) != -1) {
            fos.write(buffer, 0, bytes);
            if (bytes < 8196) break;
        }

        fos.close();

        BufferedWriter bw = new BufferedWriter(new FileWriter("./data/uploads/$data.txt"));
        bw.write(String.valueOf(count));
        bw.close();

        return count + "." + ext;
    }
    static class UploadFileThread extends Thread {
        private final File file;
        private DataOutputStream dos;
        public UploadFileThread(File f, DataOutputStream d) {
            file = f;
            dos = d;
        }
        public void run() {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                byte[] buffer = new byte[8196];
                int bytes;
                while ((bytes = fis.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytes);
                }
                dos.flush();
            } catch (IOException e) {
                System.out.println("UploadFileException");
            } finally {
                try {
                    assert fis != null;
                    fis.close();
                } catch (IOException e) {
                    System.out.println("UploadIOException");
                }
            }
        }
    }
    public void downloadFile(String filename, DataOutputStream dos) {
        File file = new File("./data/uploads/" + filename);
        UploadFileThread uft = new UploadFileThread(file, dos);
        uft.start();
    }
}
